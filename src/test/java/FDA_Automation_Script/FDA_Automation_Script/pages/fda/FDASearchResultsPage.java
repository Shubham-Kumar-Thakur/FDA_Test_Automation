package FDA_Automation_Script.FDA_Automation_Script.pages.fda;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

// TODO: Not verified against the live FDA storefront DOM — best-effort Magento-convention locators.
// TC_FBO_001 shows an exact-SKU search can redirect straight to the PDP (single-match behavior);
// this page only applies when a results GRID is shown instead (e.g. searching by product name, or a
// bulk-imported SKU with no unique catalog match yet).
public class FDASearchResultsPage extends BasePage {

    private static final By RESULT_ITEMS = By.xpath(
        "//*[contains(@class,'product-item-name')]//a | //*[contains(@class,'product-item-link')]");
    // Price sits in the same product-item container as the name/link — scoped relative price lookup
    // per matched result item, not a page-wide selector (a PLP can list several prices at once).
    private static final By RESULT_ITEM_CONTAINER = By.xpath("//*[contains(@class,'product-item-info')]");

    public FDASearchResultsPage(WebDriver driver) {
        super(driver);
    }

    // Instant JS presence check, not driver.findElement()/isDisplayed() — those honor the global
    // 2-minute implicit wait and, on a search that redirected straight to a PDP (no grid at all),
    // block for the full 2 minutes before returning false. Same fix pattern already proven elsewhere
    // in this codebase for this exact class of stall (see CLAUDE.md's TC_OU_009 runtime-fix notes).
    public boolean isResultsGridDisplayed() {
        Object present = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(\"//*[contains(@class,'product-item-name')]//a | "
            + "//*[contains(@class,'product-item-link')]\", document, null, "
            + "XPathResult.BOOLEAN_TYPE, null).booleanValue;");
        boolean displayed = Boolean.TRUE.equals(present);
        LoggerUtility.info("PLP results grid displayed: " + displayed);
        return displayed;
    }

    // Reads the price shown on the PLP tile for whichever result item's visible text contains
    // `identifier` (product name or SKU) — same matching convention as isProductInResults()/
    // clickProductInResults() below, just reading the price instead of clicking.
    public String getResultItemPrice(String identifier) {
        for (WebElement container : driver.findElements(RESULT_ITEM_CONTAINER)) {
            String text = container.getText();
            if (text != null && text.toLowerCase().contains(identifier.toLowerCase())) {
                List<WebElement> priceEls = container.findElements(By.cssSelector(".price"));
                String price = priceEls.isEmpty() ? "" : priceEls.get(0).getText().trim();
                LoggerUtility.info("PLP price for '" + identifier + "': " + price);
                return price;
            }
        }
        throw new org.openqa.selenium.NoSuchElementException("No search result item matched: " + identifier);
    }

    // Alias for clickProductInResults() — reads clearer at TC_OU_009's Phase 4 PLP->PDP transition.
    public void openMatchingResult(String identifier) {
        clickProductInResults(identifier);
    }

    public boolean isProductInResults(String identifier) {
        try {
            WaitUtility.fluentWait(driver, RESULT_ITEMS);
        } catch (TimeoutException e) {
            LoggerUtility.info("No search result items appeared for: " + identifier);
            return false;
        }
        for (WebElement el : driver.findElements(RESULT_ITEMS)) {
            String text = el.getText();
            if (text != null && text.toLowerCase().contains(identifier.toLowerCase())) {
                LoggerUtility.info("Search result found matching: " + identifier);
                return true;
            }
        }
        LoggerUtility.info("No search result matched: " + identifier);
        return false;
    }

    public void clickProductInResults(String identifier) {
        for (WebElement el : driver.findElements(RESULT_ITEMS)) {
            String text = el.getText();
            if (text != null && text.toLowerCase().contains(identifier.toLowerCase())) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
                LoggerUtility.info("Clicked search result matching: " + identifier);
                return;
            }
        }
        throw new org.openqa.selenium.NoSuchElementException("No search result item matched: " + identifier);
    }
}
