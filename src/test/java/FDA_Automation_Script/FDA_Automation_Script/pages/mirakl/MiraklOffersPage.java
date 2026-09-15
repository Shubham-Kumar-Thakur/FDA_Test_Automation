package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Mirakl "Price and stock -> Offers" screen. Used by both Seller and
 * Operator backoffice views (Operator additionally uses filterByShop()).
 */
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
    private static final By OFFER_SKU_DROPDOWN_OPTION = By.xpath(
        "//li[normalize-space()='Offer SKU'] | //div[@role='option' and normalize-space()='Offer SKU'] | "
        + "//*[@role='menuitem' and normalize-space()='Offer SKU']");
    private static final By PRODUCT_ID_DROPDOWN_OPTION = By.xpath(
        "//li[normalize-space()='Product ID'] | //div[@role='option' and normalize-space()='Product ID'] | "
        + "//*[@role='menuitem' and normalize-space()='Product ID']");
    private static final By SKU_SEARCH_FIELD = By.xpath(
        "//button[contains(normalize-space(.),'Product ID') or contains(normalize-space(.),'Offer SKU')]"
        + "/following::input[1]");

    // Operator's Shop filter (task step explicitly calls out "Apply filter and select shop").
    // Two earlier guesses (a <span> with text "Filter", then a <button> wrapping that same text)
    // both turned out wrong: a real page-source dump (2026-09-10, after both previous locators
    // failed to match live) showed there is no single generic "Filter" button at all — each filter
    // category has its own dedicated toggle button identified by a stable data-testid, e.g.
    // data-testid="roma-toolbar-filter-shops" (visible label "Para tienda" — the UI is
    // Spanish-locale, not literally "Filter"). Matched directly on that attribute instead of any
    // visible text, since it's far less likely to change than button text/nesting.
    private static final By FILTER_BUTTON = By.cssSelector("button[data-testid='roma-toolbar-filter-shops']");
    // Several earlier guesses for this field were all wrong. A real page-source dump (2026-09-10),
    // captured once FILTER_BUTTON's click genuinely expanded the panel (confirmed via
    // aria-expanded="true"), showed the actual search input has a stable id: id="shops-option-filter"
    // / name="shops-option-filter", labeled "Buscar una cuenta tienda" (Spanish for "Search a shop
    // account"). Matched directly on that id instead of any label/placeholder text, with the old
    // guesses kept as a fallback.
    private static final By SHOP_FILTER_FIELD = By.xpath(
        "//input[@id='shops-option-filter' or @name='shops-option-filter'] | "
        + "//div[text()='Shop']/following::input[1] | "
        + "//label[contains(normalize-space(.),'Shop')]/following::input[1] | "
        + "//input[contains(@placeholder,'Shop') or contains(@aria-label,'Shop')]");
    private static final By APPLY_FILTERS_BUTTON = By.xpath(
        "//button[contains(normalize-space(.),'Apply')]");
    private static final By RESULTS_ROW = By.xpath("//table//tbody/tr[1]");
    // All result rows (not just the first) — used to confirm a Product ID search has actually
    // narrowed the grid to a single unique match, rather than trusting whatever the unfiltered/
    // still-paginated grid happens to show in row 1 while the search's AJAX response is still in
    // flight.
    private static final By RESULTS_ROWS = By.xpath("//table//tbody/tr");
    private static final By RESULTS_ROW_PRICE = By.xpath(
        "//table//tbody/tr[1]//td[contains(@class,'price') or contains(.,'$') or contains(.,'MXN')]");
    private static final By RESULTS_ROW_PRODUCT = By.xpath("//table//tbody/tr[1]//td[2]");
    // Confirmed against live Offers page screenshot (2026-09-07): column order is
    // checkbox | Product | Offer SKU | Status | Default price | Quantity | Brand | ...
    private static final By RESULTS_ROW_OFFER_SKU = By.xpath("//table//tbody/tr[1]//td[3]");
    private static final By RESULTS_ROW_STATUS = By.xpath("//table//tbody/tr[1]//td[4]");

    // Confirmed against live Offers page screenshot (2026-09-07): tabs "Offers" / "Pending offers"
    // sit together in the main content area. "Pending offers" is a unique string on the page, so
    // OFFERS_TAB is anchored as the nearest preceding "Offers" text to it — this avoids ambiguity
    // with the left-nav "Offers" submenu link (OFFERS_LINK above), which also matches literal
    // text "Offers" but lives earlier in the DOM (sidebar), further from "Pending offers".
    private static final By PENDING_OFFERS_TAB = By.xpath("//*[normalize-space()='Pending offers']");
    private static final By OFFERS_TAB = By.xpath(
        "//*[normalize-space()='Pending offers']/preceding::*[normalize-space()='Offers'][1]");

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

    public void searchBySku(String sku) {
        LoggerUtility.info("Searching Mirakl Offers for SKU: " + sku);
        WaitUtility.fluentWaitForClickable(driver, SEARCH_TYPE_DROPDOWN);
        // Idempotent: this page is (re)visited multiple times across phases (e.g. Phase 1 then
        // Phase 3 after import). If the dropdown already shows "Offer SKU" from a prior call,
        // re-clicking it/the option again risks toggling it back off (behaved like a toggle, not
        // a single-select, in a real run — Phase 3's re-search silently reverted to 0 results).
        String currentMode = getText(SEARCH_TYPE_DROPDOWN);
        if (!"Offer SKU".equalsIgnoreCase(currentMode.trim())) {
            LoggerUtility.info("Switching search-type dropdown from '" + currentMode + "' to 'Offer SKU'");
            jsClick(SEARCH_TYPE_DROPDOWN);
            WaitUtility.fluentWaitForClickable(driver, OFFER_SKU_DROPDOWN_OPTION);
            jsClick(OFFER_SKU_DROPDOWN_OPTION);
        } else {
            LoggerUtility.info("Search-type dropdown already set to 'Offer SKU' — no change needed");
        }
        WaitUtility.fluentWaitForClickable(driver, SKU_SEARCH_FIELD);
        type(SKU_SEARCH_FIELD, sku);
        pressEnter(SKU_SEARCH_FIELD);
    }

    // Same search field as searchBySku(), but switches the dropdown to "Product ID" mode instead
    // of "Offer SKU" — used when the test needs to search by the Product ID (EAN/UPC) column
    // rather than the Seller-internal Offer SKU.
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

    public void filterByShop(String shopName) {
        LoggerUtility.info("Applying Mirakl Offers Shop filter: " + shopName);
        openFilterPanelAndSet(SHOP_FILTER_FIELD, shopName);
    }

    // Opens the "Filter" panel, types into the given field, and applies — pressing Enter first
    // (common pattern for these panels) then falling back to an explicit "Apply" button if the
    // panel is still open, since the exact apply mechanism isn't yet confirmed against the DOM.
    // Confirmed via a real Operator run (2026-09-10): fluentWaitForClickable requires the element
    // to be both visible and enabled (ExpectedConditions.elementToBeClickable), which timed out
    // after 2 minutes on this exact, DOM-confirmed span — same "not truly visible to Selenium"
    // pattern as the other Operator controls in this file. jsClick doesn't need visibility, so the
    // wait is dropped in favor of the same direct-click pattern already used for
    // PRICES_AND_STOCKS_MENU/OFFERS_LINK; the global 2-minute implicit wait still covers presence.
    private void openFilterPanelAndSet(By fieldLocator, String value) {
        // Confirmed via a real Operator run (2026-09-10): a raw jsClick(FILTER_BUTTON) with the
        // global 2-minute implicit wait active blocks for the full 2 minutes before throwing
        // NoSuchElementException when the button is genuinely absent — same class of issue already
        // fixed for the "Offers" submenu link in clickOffersLinkIfPresent(). Use the same short
        // 5-second implicit wait + short retry pattern so a genuine absence fails in seconds, not
        // minutes, and dump the live page source on final failure so the real cause (button missing
        // vs. a locator that no longer matches) can be diagnosed from actual DOM instead of guessing.
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        try {
            boolean clicked = false;
            for (int attempt = 1; attempt <= 3 && !clicked; attempt++) {
                if (isDisplayed(FILTER_BUTTON)) {
                    try {
                        // Confirmed via a real run (2026-09-10): jsClick's raw JS element.click()
                        // only dispatches the "click" DOM event, not mousedown/mouseup — the
                        // diagnostic dump showed aria-expanded stayed "false" after the click, i.e.
                        // the toggle never fired. This dropdown is a React component that (like many
                        // accordion/menu toggles) likely reacts to a fuller mouse event sequence.
                        // Use a genuine Selenium click() instead, which dispatches real native mouse
                        // events — safe here since isDisplayed() just confirmed this element is
                        // actually visible/interactable (unlike the other Operator controls in this
                        // file, which needed jsClick specifically because they weren't).
                        click(FILTER_BUTTON);
                        clicked = true;
                    } catch (StaleElementReferenceException | org.openqa.selenium.NoSuchElementException e) {
                        LoggerUtility.info("Filter button went stale/disappeared right after being "
                            + "found (attempt " + attempt + "/3) — retrying");
                    }
                } else {
                    LoggerUtility.info("Filter button not present yet (attempt " + attempt
                        + "/3) — retrying");
                }
                if (!clicked && attempt < 3) {
                    try {
                        Thread.sleep(2_000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            if (!clicked) {
                try {
                    java.nio.file.Files.writeString(
                        java.nio.file.Path.of("test-output/logs/operator_offers_filter_button_missing.html"),
                        driver.getPageSource());
                    LoggerUtility.info("Diagnostic: dumped page source to "
                        + "test-output/logs/operator_offers_filter_button_missing.html");
                } catch (Exception dumpFailure) {
                    LoggerUtility.error("Diagnostic page-source dump failed: " + dumpFailure.getMessage());
                }
                throw new org.openqa.selenium.NoSuchElementException(
                    "Filter button not found after 3 short retries — see diagnostic page-source dump");
            }
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        }
        try {
            WaitUtility.fluentWaitForClickable(driver, fieldLocator);
        } catch (org.openqa.selenium.TimeoutException e) {
            // Diagnostic: the Shop filter button locator was only just fixed from real DOM evidence
            // (2026-09-10) — the field locator that should appear after expanding it (SHOP_FILTER_FIELD)
            // is still an unconfirmed guess. Dump the expanded panel's real DOM if this ever fails so
            // the field locator can be fixed the same way, instead of guessing again.
            try {
                java.nio.file.Files.writeString(
                    java.nio.file.Path.of("test-output/logs/operator_offers_shop_field_missing.html"),
                    driver.getPageSource());
                LoggerUtility.info("Diagnostic: dumped page source to "
                    + "test-output/logs/operator_offers_shop_field_missing.html");
            } catch (Exception dumpFailure) {
                LoggerUtility.error("Diagnostic page-source dump failed: " + dumpFailure.getMessage());
            }
            throw e;
        }
        type(fieldLocator, value);
        pressEnter(fieldLocator);
        if (isDisplayed(APPLY_FILTERS_BUTTON)) {
            click(APPLY_FILTERS_BUTTON);
        }
    }

    public boolean hasResults() {
        return isDisplayed(RESULTS_ROW);
    }

    // Confirmed via a real run (2026-09-10): hasResults() only checks that row 1 exists — right
    // after a Product ID search fires, the grid can still be showing its previous unfiltered/
    // paginated state (many rows) while the search's AJAX response is in flight, and row 1 in that
    // state can be a completely unrelated offer. A Product ID search that has actually taken effect
    // narrows the grid to exactly one row (a unique match), so callers should wait for a count of 1
    // rather than trusting "any row present" before reading row 1's data.
    public int getResultsCount() {
        return driver.findElements(RESULTS_ROWS).size();
    }

    // Genuine explicit wait (WebDriverWait poll) for the grid to narrow to exactly one row after a
    // search — replaces the previous fixed-attempt Thread.sleep(2_000) settle loops duplicated
    // across TC_E2E_009's phases, per explicit instruction to wait on the grid row rather than sleep.
    public boolean waitForSingleResult(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                .pollingEvery(Duration.ofSeconds(1))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> getResultsCount() == 1);
            return true;
        } catch (TimeoutException e) {
            LoggerUtility.warn("Offers grid did not narrow to a single row within " + timeout.getSeconds()
                + "s (last seen count: " + getResultsCount() + ")");
            return false;
        }
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

    // Confirmed via a real run (2026-09-09): right after a search, the grid can briefly still show
    // the previous search's row (or a loading spinner) — reading price/name at that instant
    // silently returns stale/wrong data instead of throwing. Callers should check this against
    // the offer's known Offer SKU before trusting getFirstResultPrice()/getFirstResultProductName().
    public String getFirstResultOfferSku() {
        String sku = getText(RESULTS_ROW_OFFER_SKU);
        LoggerUtility.info("Mirakl Offers — displayed Offer SKU: " + sku);
        return sku;
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

    public String getFirstResultStatus() {
        String status = getText(RESULTS_ROW_STATUS);
        LoggerUtility.info("Mirakl Offers — displayed status: " + status);
        return status;
    }

    public void clickOffersTab() {
        LoggerUtility.info("Clicking 'Offers' tab");
        jsClick(OFFERS_TAB);
    }

    public void clickPendingOffersTab() {
        LoggerUtility.info("Clicking 'Pending offers' tab");
        jsClick(PENDING_OFFERS_TAB);
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
