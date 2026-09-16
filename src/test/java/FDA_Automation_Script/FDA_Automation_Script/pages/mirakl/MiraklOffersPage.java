package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

/** Mirakl "Price and stock -> Offers" screen, used by TC_E2E_009's Seller flow. */
public class MiraklOffersPage extends BasePage {

    // Confirmed against live Seller dashboard left-nav (2026-09-07 real run screenshot): menu is a
    // <span> reading "Price and stock" (singular). Confirmed against a live Operator dashboard
    // (2026-09-10, DOM inspection) that the Operator's version isn't a span with that text at all
    // — it's a <button id="priceAndStock">. Two locators guessed at matching the Operator's
    // visible label text both failed (a plain-text theory and an iframe theory), because the
    // element simply isn't a text-matched span there — it's identified by id. Matched via "|" so
    // one locator serves both roles without needing to branch in navigateToOffers().
    private static final By PRICES_AND_STOCKS_MENU = By.xpath(
        "//span[normalize-space()='Price and stock'] | //button[@id='priceAndStock']");
    // Confirmed against live Seller dashboard (2026-09-07): "Price and stock" expands to
    // Offers / Promotions / File imports, and this text-based locator matched fine there. On the
    // Operator dashboard it always found zero matches — not because Operator "auto-navigates"
    // straight to Offers as originally assumed, but because the Operator UI is entirely
    // Spanish-locale (confirmed via a real page-source dump, 2026-09-10): the actual link's visible
    // text is "Ofertas", not "Offers", so the English-text locator could never match. That link does
    // have a stable id="offersSearch" regardless of locale — matched on that instead, with the old
    // English-text guess kept as a fallback for the Seller side (English in the runs observed so
    // far). Confirmed via a real run (2026-09-10): a bare //*[@id='offersSearch'] (any element type)
    // caused a stale-element loop on the Seller dashboard, where that id apparently also matches
    // some other, non-interactive element in a different DOM state. The real dump showed the actual
    // Operator link is specifically <a role="menuitem" id="offersSearch">, so require that role too
    // to avoid matching an unrelated element that merely shares the id.
    private static final By OFFERS_LINK = By.xpath(
        "//*[@id='offersSearch' and @role='menuitem'] | //span[normalize-space()='Offers'] | "
        + "//span[text()='Offers']");

    // Confirmed working (2026-09-07 real run): the search-type dropdown defaults to "Product ID"
    // — per explicit instruction, click it and select "Offer SKU" before typing the SKU, rather
    // than searching in "Product ID" mode. Anchored to match either current label, since the
    // button's own text changes from "Product ID" to "Offer SKU" once switched.
    private static final By SEARCH_TYPE_DROPDOWN = By.xpath(
        "//button[contains(normalize-space(.),'Product ID') or contains(normalize-space(.),'Offer SKU')]");
    private static final By PRODUCT_ID_DROPDOWN_OPTION = By.xpath(
        "//li[normalize-space()='Product ID'] | //div[@role='option' and normalize-space()='Product ID'] | "
        + "//*[@role='menuitem' and normalize-space()='Product ID']");
    private static final By SKU_SEARCH_FIELD = By.xpath(
        "//button[contains(normalize-space(.),'Product ID') or contains(normalize-space(.),'Offer SKU')]"
        + "/following::input[1]");

    // All result rows (not just the first) — used to confirm a Product ID search has actually
    // narrowed the grid to a single unique match, rather than trusting whatever the unfiltered/
    // still-paginated grid happens to show in row 1 while the search's AJAX response is still in
    // flight.
    private static final By RESULTS_ROWS = By.xpath("//table//tbody/tr");
    private static final By RESULTS_ROW_PRICE = By.xpath(
        "//table//tbody/tr[1]//td[contains(@class,'price') or contains(.,'$') or contains(.,'MXN')]");
    private static final By RESULTS_ROW_PRODUCT = By.xpath("//table//tbody/tr[1]//td[2]");

    // Confirmed against live Seller dashboard (2026-09-07 real run screenshot): heading reads
    // "Welcome {SellerName}" (e.g. "Welcome TEST_SELLERRR") — getShopName() strips the prefix.
    private static final By SHOP_NAME_HEADER = By.xpath(
        "//*[self::h1 or self::h2][contains(normalize-space(.),'Welcome')]");

    public MiraklOffersPage(WebDriver driver) {
        super(driver);
    }

    public void navigateToOffers() {
        LoggerUtility.info("Navigating Mirakl: Price and stock -> Offers");
        // Confirmed via a real run (2026-09-10): TC_E2E_009's Phase 3 polling loop calls this
        // immediately after driver.navigate().refresh(), which can race the reload — jsClick's own
        // findElement() succeeds, but the element goes stale before the follow-up executeScript
        // round-trip completes, throwing StaleElementReferenceException. fluentWait polls until the
        // menu is genuinely visible/stable (and already ignores StaleElementReferenceException
        // while polling), so wait for it before the click instead of clicking blind.
        //
        // Confirmed via a real run (2026-09-15): clickOffersLinkIfPresent()'s "link disappeared ->
        // assume already navigated" fallback is not always true — the submenu can simply collapse
        // (click-outside/animation race) without ever navigating, leaving neither the submenu open
        // nor the Offers grid reached. Trusting that assumption blindly left searchByProductId()
        // waiting 2 minutes for a search dropdown that would never appear. Verify the Offers view
        // was actually reached before returning, and retry the whole click sequence once if not.
        for (int attempt = 1; attempt <= 2; attempt++) {
            WaitUtility.fluentWait(driver, PRICES_AND_STOCKS_MENU);
            jsClick(PRICES_AND_STOCKS_MENU);
            clickOffersLinkIfPresent();
            if (isOffersViewReached()) {
                return;
            }
            LoggerUtility.info("Offers view not confirmed after navigation attempt " + attempt
                + "/2" + (attempt < 2 ? " — retrying" : " — giving up, letting the caller's own wait fail loudly"));
        }
    }

    // Cheap, fast presence check (short implicit wait, not the full 2-minute one) for whether the
    // Offers search dropdown actually rendered — the real signal that navigateToOffers() succeeded,
    // as opposed to assuming success just because the submenu link disappeared.
    private boolean isOffersViewReached() {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        try {
            return !driver.findElements(SEARCH_TYPE_DROPDOWN).isEmpty();
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        }
    }

    // Confirmed via a real Operator run (2026-09-10): even the user-inspected exact locator for
    // OFFERS_LINK found zero matches after the full 2-minute implicit wait on the Operator
    // dashboard, right after clicking button#priceAndStock. The very first Operator screenshot
    // shared for this investigation already showed "Offers" pre-highlighted/active directly under
    // "Prices and stocks" — so clicking that button likely navigates straight to its default
    // Offers sub-view there, with no separate "Offers" submenu item to click. On the Seller
    // dashboard the click is genuinely required (submenu only expands, doesn't auto-navigate).
    // A short probe distinguishes the two without branching per-role in navigateToOffers().
    private void clickOffersLinkIfPresent() {
        // Confirmed via two real runs (2026-09-10): the "Offers" submenu link can vanish from the
        // DOM between being detected and being clicked, right after clicking PRICES_AND_STOCKS_MENU
        // triggers the submenu's expand animation/navigation. This surfaces as either
        // StaleElementReferenceException (jsClick's own findElement() found a now-detached
        // reference) or NoSuchElementException (the element is gone outright before findElement()
        // runs) — and if the global 2-minute implicit wait is active at that moment, an
        // "already gone" NoSuchElementException doesn't fail fast: WebDriver silently retries
        // internally for the full 2 minutes before finally throwing. Keep the short 5-second
        // implicit wait active for this entire presence-check-and-click sequence, including
        // retries, so a genuinely-gone element is treated as "already navigated" (matching this
        // method's own original fallback) instead of blocking. Restore the global 2-minute wait
        // exactly once, in the finally, no matter which path is taken.
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        try {
            for (int attempt = 1; attempt <= 2; attempt++) {
                if (!isDisplayed(OFFERS_LINK)) {
                    LoggerUtility.info("'Offers' submenu link not present (attempt " + attempt
                        + "/2) — assuming it already navigated straight to the Offers view");
                    return;
                }
                try {
                    // Confirmed via a real run (2026-09-10): OFFERS_LINK now also matches
                    // id="offersSearch" on the Operator dashboard, a role="menuitem" element in the
                    // same React app where jsClick's raw JS element.click() was proven not to
                    // trigger a mousedown-based toggle on another menu-style control
                    // (FILTER_BUTTON). Use a genuine Selenium click() here too — isDisplayed() just
                    // confirmed this element is visible/interactable, so the native click is safe.
                    click(OFFERS_LINK);
                    return;
                } catch (StaleElementReferenceException | org.openqa.selenium.NoSuchElementException e) {
                    LoggerUtility.info("'Offers' submenu link went stale/disappeared right after the "
                        + "menu click (attempt " + attempt + "/2) — re-probing");
                }
            }
            LoggerUtility.info("'Offers' submenu link never settled after retrying — assuming it "
                + "already navigated straight to the Offers view");
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        }
    }

    public void searchByProductId(String productId) {
        LoggerUtility.info("Searching Mirakl Offers for Product ID: " + productId);
        WaitUtility.fluentWaitForClickable(driver, SEARCH_TYPE_DROPDOWN);
        String currentMode = getText(SEARCH_TYPE_DROPDOWN);
        if (!"Product ID".equalsIgnoreCase(currentMode.trim())) {
            LoggerUtility.info("Switching search-type dropdown from '" + currentMode + "' to 'Product ID'");
            jsClick(SEARCH_TYPE_DROPDOWN);
            WaitUtility.fluentWaitForClickable(driver, PRODUCT_ID_DROPDOWN_OPTION);
            jsClick(PRODUCT_ID_DROPDOWN_OPTION);
        } else {
            LoggerUtility.info("Search-type dropdown already set to 'Product ID' — no change needed");
        }
        WaitUtility.fluentWaitForClickable(driver, SKU_SEARCH_FIELD);
        type(SKU_SEARCH_FIELD, productId);
        pressEnter(SKU_SEARCH_FIELD);
    }

    // Confirmed via a real run (2026-09-10): trusting "any row present" isn't enough — right
    // after a Product ID search fires, the grid can still be showing its previous unfiltered/
    // paginated state (many rows) while the search's AJAX response is in flight, and row 1 in that
    // state can be a completely unrelated offer. A Product ID search that has actually taken effect
    // narrows the grid to exactly one row (a unique match), so callers should wait for a count of 1
    // rather than trusting "any row present" before reading row 1's data.
    public int getResultsCount() {
        return driver.findElements(RESULTS_ROWS).size();
    }

    public String getFirstResultPrice() {
        String rawPrice = getText(RESULTS_ROW_PRICE);
        // Confirmed against a real run (2026-09-07): right after a price update, Mirakl briefly
        // shows both the old and new price stacked in the same cell (e.g. "MX$80.00\nMX$100.00").
        // Take the last line — the current/final price — rather than the whole (concatenated,
        // unparseable) cell text.
        String[] lines = rawPrice.split("\\r?\\n");
        String price = lines[lines.length - 1].trim();
        LoggerUtility.info("Mirakl Offers — displayed price: " + price
            + (lines.length > 1 ? " (raw cell text: " + rawPrice.replace("\n", " | ") + ")" : ""));
        return price;
    }

    public String getFirstResultProductName() {
        // Confirmed via a real run (2026-09-15): this cell's full text also includes a second
        // line — a category/brand tag (e.g. "Bebé") shown directly under the product name in the
        // grid (visible in the Offers screenshot) — which getText() picks up too. Only the first
        // line is the actual product name; the FDA storefront PDP title doesn't carry that tag, so
        // comparing the raw multi-line text against it always mismatched.
        String rawName = getText(RESULTS_ROW_PRODUCT);
        String name = rawName.split("\\r?\\n", 2)[0].trim();
        LoggerUtility.info("Mirakl Offers — displayed product name: " + name
            + (rawName.contains("\n") ? " (raw cell text: " + rawName.replace("\n", " | ") + ")" : ""));
        return name;
    }

    public String getShopName() {
        String raw = getText(SHOP_NAME_HEADER);
        String shopName = raw.replaceFirst("(?i)^welcome\\s+", "")
            .replaceAll("(?i)\\s*(open|closed)\\s*$", "")
            .trim();
        LoggerUtility.info("Mirakl — current Seller/Shop name: " + shopName + " (raw heading: " + raw + ")");
        return shopName;
    }
}
