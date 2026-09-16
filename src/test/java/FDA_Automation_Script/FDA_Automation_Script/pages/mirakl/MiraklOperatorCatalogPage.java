package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Mirakl Operator "Catalog -> Product Imports" screen — filter by Seller, review products with
 * Status "New", accept them into the "FDA Catalog", and track status New -> Pending -> Published.
 * New page object for TC_E2E_009's product-import flow; DOM not yet verified against a live run.
 */
public class MiraklOperatorCatalogPage extends BasePage {

    private static final By CATALOG_MENU = By.xpath("//span[normalize-space()='Catalog']");
    // TODO: Verify submenu label against actual Operator DOM
    private static final By PRODUCT_IMPORTS_LINK = By.xpath("//span[normalize-space()='Product Imports']");

    // TODO: Verify Seller filter locator — likely a dropdown/search similar to the Offers page's
    // Shop filter used elsewhere in this framework.
    private static final By SELLER_FILTER_BUTTON = By.xpath("//button[contains(normalize-space(.),'Seller')]");
    private static final By SELLER_FILTER_INPUT = By.xpath(
        "//button[contains(normalize-space(.),'Seller')]/following::input[1]");

    private static final By RESULTS_ROW = By.xpath("//table//tbody/tr[1]");
    private static final By RESULTS_ROW_STATUS = By.xpath("//table//tbody/tr[1]//td[contains(.,'New') or "
        + "contains(.,'Pending') or contains(.,'Published')]");
    // TODO: Verify "select all" checkbox locator
    private static final By SELECT_ALL_CHECKBOX = By.xpath("//table//thead//input[@type='checkbox']");
    private static final By MORE_ACTIONS_BUTTON = By.xpath("//button[contains(normalize-space(.),'More Actions')]");
    private static final By EDIT_CATALOGS_OPTION = By.xpath(
        "//li[normalize-space()='Edit Catalogs'] | //div[@role='option' and normalize-space()='Edit Catalogs']");
    private static final By FDA_CATALOG_CHECKBOX = By.xpath(
        "//*[contains(normalize-space(.),'FDA Catalog')]//input[@type='checkbox'] | "
        + "//input[@type='checkbox' and following-sibling::*[contains(normalize-space(.),'FDA Catalog')]]");
    private static final By CONFIRM_BUTTON = By.xpath("//button[normalize-space()='Confirm']");
    private static final By ACCEPT_BUTTON = By.xpath("//button[normalize-space()='Accept']");
    // "Accept Products" confirmation popup — TODO: Verify exact popup title/confirm-button locator
    private static final By ACCEPT_PRODUCTS_POPUP = By.xpath(
        "//*[contains(normalize-space(.),'Accept Products')]");
    private static final By POPUP_CONFIRM_BUTTON = By.xpath(
        "//div[contains(@class,'modal') or @role='dialog']//button[normalize-space()='Confirm']");

    public MiraklOperatorCatalogPage(WebDriver driver) {
        super(driver);
    }

    public void navigateToProductImports() {
        LoggerUtility.info("Navigating Mirakl Operator: Catalog -> Product Imports");
        jsClick(CATALOG_MENU);
        jsClick(PRODUCT_IMPORTS_LINK);
    }

    public void filterBySeller(String sellerName) {
        LoggerUtility.info("Filtering Product Imports by Seller: " + sellerName);
        WaitUtility.fluentWaitForClickable(driver, SELLER_FILTER_BUTTON);
        click(SELLER_FILTER_BUTTON);
        WaitUtility.fluentWaitForClickable(driver, SELLER_FILTER_INPUT);
        type(SELLER_FILTER_INPUT, sellerName);
        pressEnter(SELLER_FILTER_INPUT);
    }

    public boolean hasResults() {
        return isDisplayed(RESULTS_ROW);
    }

    public String getFirstResultStatus() {
        String status = getText(RESULTS_ROW_STATUS);
        LoggerUtility.info("Mirakl Operator Product Imports — first result status: " + status);
        return status;
    }

    public void selectAllProducts() {
        LoggerUtility.info("Selecting all products (select-all checkbox)");
        click(SELECT_ALL_CHECKBOX);
    }

    public void clickMoreActions() {
        LoggerUtility.info("Clicking More Actions");
        click(MORE_ACTIONS_BUTTON);
    }

    public void selectEditCatalogs() {
        LoggerUtility.info("Selecting 'Edit Catalogs' from More Actions menu");
        WaitUtility.fluentWaitForClickable(driver, EDIT_CATALOGS_OPTION);
        jsClick(EDIT_CATALOGS_OPTION);
    }

    public void selectFdaCatalogCheckbox() {
        LoggerUtility.info("Selecting 'FDA Catalog' checkbox in catalog-selection popup");
        WaitUtility.fluentWaitForClickable(driver, FDA_CATALOG_CHECKBOX);
        jsClick(FDA_CATALOG_CHECKBOX);
    }

    public void clickConfirm() {
        LoggerUtility.info("Clicking Confirm");
        click(CONFIRM_BUTTON);
    }

    public void clickAccept() {
        LoggerUtility.info("Clicking Accept button");
        click(ACCEPT_BUTTON);
    }

    public boolean isAcceptProductsPopupDisplayed() {
        return isDisplayed(ACCEPT_PRODUCTS_POPUP);
    }

    public void confirmAcceptPopup() {
        LoggerUtility.info("Confirming 'Accept Products' popup");
        click(POPUP_CONFIRM_BUTTON);
    }
}
