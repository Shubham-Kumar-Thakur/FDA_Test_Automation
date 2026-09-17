package FDA_Automation_Script.FDA_Automation_Script.pages.fda;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

// TODO: Not verified against the live FDA storefront DOM — best-effort Magento-convention locators.
// TC_FBO_001 shows an exact-SKU search can redirect straight to the PDP (single-match behavior);
// this page only applies when a results GRID is shown instead (e.g. searching by product name, or a
// bulk-imported SKU with no unique catalog match yet).
public class FDASearchResultsPage extends BasePage {

    private static final By RESULT_ITEMS = By.xpath(
        "//*[contains(@class,'product-item-name')]//a | //*[contains(@class,'product-item-link')]");

    public FDASearchResultsPage(WebDriver driver) {
        super(driver);
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
