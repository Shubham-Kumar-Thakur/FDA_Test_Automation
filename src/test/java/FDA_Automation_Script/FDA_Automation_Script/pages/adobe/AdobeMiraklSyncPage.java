
package FDA_Automation_Script.FDA_Automation_Script.pages.adobe;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Adobe Admin "Mirakl -> System -> Synchronization" screen — triggers the
 * "MCM Products Asynchronous Import" job that pulls accepted Mirakl catalog products into
 * Magento. New page object for TC_E2E_009; DOM not yet verified against a live run.
 */
public class AdobeMiraklSyncPage extends BasePage {

    // TODO: Verify Admin left-nav locators against actual DOM
    private static final By MIRAKL_MENU = By.xpath("//span[normalize-space()='Mirakl'] | //a[normalize-space()='Mirakl']");
    private static final By SYSTEM_MENU = By.xpath("//span[normalize-space()='System'] | //a[normalize-space()='System']");
    private static final By SYNCHRONIZATION_LINK = By.xpath(
        "//span[normalize-space()='Synchronization'] | //a[normalize-space()='Synchronization']");

    // "MCM Products Asynchronous Import (CM52-CM53-CM54)" row and its "Import to Magento" action
    private static final By PRODUCTS_IMPORT_ROW = By.xpath(
        "//tr[contains(normalize-space(.),'MCM Products Asynchronous Import')]");
    private static final By IMPORT_TO_MAGENTO_BUTTON = By.xpath(
        "//tr[contains(normalize-space(.),'MCM Products Asynchronous Import')]"
        + "//button[contains(normalize-space(.),'Import to Magento')] | "
        + "//tr[contains(normalize-space(.),'MCM Products Asynchronous Import')]"
        + "//a[contains(normalize-space(.),'Import to Magento')]");
    // TODO: Verify success confirmation locator (toast/message) once the real DOM is available
    private static final By IMPORT_TRIGGERED_MESSAGE = By.xpath(
        "//*[contains(normalize-space(.),'success') or contains(normalize-space(.),'triggered') "
        + "or contains(normalize-space(.),'started')]");

    public AdobeMiraklSyncPage(WebDriver driver) {
        super(driver);
    }

    public void navigateToSynchronization() {
        LoggerUtility.info("Navigating Adobe Admin: Mirakl -> System -> Synchronization");
        jsClick(MIRAKL_MENU);
        jsClick(SYSTEM_MENU);
        jsClick(SYNCHRONIZATION_LINK);
    }

    public boolean isProductsImportRowDisplayed() {
        return isDisplayed(PRODUCTS_IMPORT_ROW);
    }

    public void clickImportToMagento() {
        LoggerUtility.info("Clicking 'Import to Magento' for MCM Products Asynchronous Import");
        click(IMPORT_TO_MAGENTO_BUTTON);
    }

    public boolean isImportTriggeredMessageDisplayed() {
        return isDisplayed(IMPORT_TRIGGERED_MESSAGE);
    }
}
