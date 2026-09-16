package FDA_Automation_Script.FDA_Automation_Script.pages.fda;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * FDA storefront search-results grid. Existing tests only ever searched by exact
 * SKU (which commonly redirects straight to the matching PDP), so no results-grid
 * verification page existed before. TC_E2E_009 also searches by product name,
 * which can return a multi-item grid instead of a direct PDP redirect.
 */
public class FDASearchResultsPage extends BasePage {

    // TODO: Verify locators against actual FDA storefront search-results DOM
    private static final By RESULTS_GRID = By.cssSelector(".products-grid, .search.results, .products.list.items");
    private static final By RESULT_ITEM_LINKS = By.cssSelector(
        ".products-grid .product-item-link, .search.results .product-item-link");
    // Same product-name heading locator as FDAPDPPage.PRODUCT_NAME — duplicated here (separate
    // page object) as the reliable "are we already on a PDP" signal. Confirmed via a real run
    // (2026-09-15): an exact-match Product ID search redirected straight to the correct PDP, but
    // the previous "URL has .html AND results grid not displayed" heuristic still reported a grid,
    // because RESULTS_GRID's generic CSS classes also match a related/upsell-products section that
    // is part of the PDP itself — so the check never realized navigation had already succeeded.
    private static final By PDP_MARKER = By.xpath("//h1//span[@class='base' and @itemprop='name']");
    // TODO: Verify against actual DOM — standard Magento/Luma theme price markup guessed
    // (data-price-type='finalPrice'), with a plain ".price" class fallback.
    private static final By RESULT_ITEM_PRICE_RELATIVE = By.xpath(
        "ancestor::li[contains(@class,'product-item')][1]"
        + "//span[@data-price-type='finalPrice']//span[contains(@class,'price')] | "
        + "ancestor::li[contains(@class,'product-item')][1]//span[contains(@class,'price')]");

    public FDASearchResultsPage(WebDriver driver) {
        super(driver);
    }

    /**
     * True if either (a) the search navigated directly to a PDP (typical for an
     * exact-match SKU search), or (b) a results grid is present with an item whose
     * text matches expectedText (SKU or product name).
     */
    public boolean isProductPresent(String expectedText) {
        if (isDisplayed(PDP_MARKER)) {
            LoggerUtility.info("Search navigated directly to a product page for query: " + expectedText);
            return true;
        }
        List<WebElement> items = driver.findElements(RESULT_ITEM_LINKS);
        for (WebElement item : items) {
            if (item.getText().trim().toLowerCase().contains(expectedText.toLowerCase())) {
                LoggerUtility.info("Search result match found for: " + expectedText);
                return true;
            }
        }
        LoggerUtility.warn("No search result match found for: " + expectedText
            + " among " + items.size() + " result item(s)");
        return false;
    }

    /** True if a results grid is actually showing (as opposed to a direct PDP redirect). */
    public boolean isResultsGridDisplayed() {
        return !isDisplayed(PDP_MARKER);
    }

    /**
     * Scrolls to the top of the PLP. Per explicit instruction (2026-09-15): call this once on
     * landing on the results grid, before locating/reading any item card — a fresh, known scroll
     * position rather than whatever position the page happened to load/settle at.
     */
    public void scrollToTop() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
        LoggerUtility.info("Scrolled PLP to top");
    }

    /**
     * Reads the price shown on the results-grid (PLP) card for the item matching expectedText.
     * Only meaningful when a results grid is actually present — check isResultsGridDisplayed()
     * first, since an exact-match search can redirect straight to the PDP with no grid at all.
     */
    public String getResultItemPrice(String expectedText) {
        List<WebElement> items = driver.findElements(RESULT_ITEM_LINKS);
        for (WebElement item : items) {
            if (item.getText().trim().toLowerCase().contains(expectedText.toLowerCase())) {
                // Per explicit instruction (2026-09-15): scroll the matching card into view before
                // reading its price — this grid can lazy-render/lazy-price cards that are still
                // off-screen. Centered (not top-aligned) so the card doesn't end up sitting under
                // the storefront's sticky header — a top-aligned scroll here left the card there
                // for a later openMatchingResult() click, which then got intercepted by the header
                // (confirmed via a real run, 2026-09-16).
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", item);
                String price = item.findElement(RESULT_ITEM_PRICE_RELATIVE).getText().trim();
                LoggerUtility.info("FDA search result (PLP) price for '" + expectedText + "': " + price);
                return price;
            }
        }
        throw new NoSuchElementException("No search result item matched (for price lookup): " + expectedText);
    }

    /** Clicks the results-grid item matching expectedText to open its PDP. No-op if already on a PDP. */
    public void openMatchingResult(String expectedText) {
        if (isDisplayed(PDP_MARKER)) {
            LoggerUtility.info("Already on a product page — no results-grid click needed");
            return;
        }
        List<WebElement> items = driver.findElements(RESULT_ITEM_LINKS);
        for (WebElement item : items) {
            if (item.getText().trim().toLowerCase().contains(expectedText.toLowerCase())) {
                // Center the card in the viewport before clicking — a top-aligned scroll (or
                // whatever position the page happened to be left at by an earlier step) can leave
                // the card sitting under the storefront's sticky header, which intercepts the
                // click (confirmed via a real run, 2026-09-16).
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", item);
                try {
                    item.click();
                } catch (org.openqa.selenium.ElementClickInterceptedException e) {
                    LoggerUtility.warn("Search result click intercepted, retrying via JS click: " + e.getMessage());
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", item);
                }
                LoggerUtility.info("Clicked search result matching: " + expectedText);
                return;
            }
        }
        throw new NoSuchElementException("No search result item matched: " + expectedText);
    }
}
