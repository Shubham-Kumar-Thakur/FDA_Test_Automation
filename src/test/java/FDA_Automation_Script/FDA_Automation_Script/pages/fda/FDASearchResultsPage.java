package FDA_Automation_Script.FDA_Automation_Script.pages.fda;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import org.openqa.selenium.By;
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

    public FDASearchResultsPage(WebDriver driver) {
        super(driver);
    }

    /**
     * True if either (a) the search navigated directly to a PDP (typical for an
     * exact-match SKU search), or (b) a results grid is present with an item whose
     * text matches expectedText (SKU or product name).
     */
    public boolean isProductPresent(String expectedText) {
        if (driver.getCurrentUrl().contains(".html") && !isDisplayed(RESULTS_GRID)) {
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

    /** Clicks the results-grid item matching expectedText to open its PDP. No-op if already on a PDP. */
    public void openMatchingResult(String expectedText) {
        if (driver.getCurrentUrl().contains(".html") && !isDisplayed(RESULTS_GRID)) {
            LoggerUtility.info("Already on a product page — no results-grid click needed");
            return;
        }
        List<WebElement> items = driver.findElements(RESULT_ITEM_LINKS);
        for (WebElement item : items) {
            if (item.getText().trim().toLowerCase().contains(expectedText.toLowerCase())) {
                item.click();
                LoggerUtility.info("Clicked search result matching: " + expectedText);
                return;
            }
        }
        throw new NoSuchElementException("No search result item matched: " + expectedText);
    }
}
