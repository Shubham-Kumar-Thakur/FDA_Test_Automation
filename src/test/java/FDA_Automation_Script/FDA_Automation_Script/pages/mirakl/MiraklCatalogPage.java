package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

/**
 * Mirakl Seller "Catalog" menu — Product Imports (import job/product status tracking) and
 * Catalog Management (per-product SKU list, product detail Invalid/Valid data validation,
 * Fragile/MSI field correction). New page object for TC_E2E_009's product-import flow —
 * "Catalog" is a confirmed top-level nav item (seen in real screenshots) but its submenu
 * items and the product-detail edit form are not yet verified against the live DOM.
 */
public class MiraklCatalogPage extends BasePage {

    // Confirmed top-level nav item (2026-09-07 screenshot). Submenu labels below are TODO.
    private static final By CATALOG_MENU = By.xpath("//span[normalize-space()='Catalog']");
    // TODO: Verify submenu labels against actual DOM
    private static final By PRODUCT_IMPORTS_LINK = By.xpath("//span[normalize-space()='Product Imports']");
    private static final By CATALOG_MANAGEMENT_LINK = By.xpath("//span[normalize-space()='Catalog Management']");

    // TODO: Verify search box / results-row locators against actual Catalog Management DOM —
    // reuses the same "search-type dropdown -> input" pattern confirmed working on the Offers page.
    private static final By SKU_SEARCH_TYPE_DROPDOWN = By.xpath(
        "//button[contains(normalize-space(.),'Product ID') or contains(normalize-space(.),'SKU')]");
    private static final By SKU_SEARCH_FIELD = By.xpath(
        "//button[contains(normalize-space(.),'Product ID') or contains(normalize-space(.),'SKU')]"
        + "/following::input[1]");
    private static final By RESULTS_ROW = By.xpath("//table//tbody/tr[1]");
    private static final By RESULTS_ROW_STATUS = By.xpath("//table//tbody/tr[1]//td[contains(.,'Pending') or "
        + "contains(.,'Published') or contains(.,'New') or contains(.,'Active')]");
    private static final By PRODUCT_NAME_LINK = By.xpath("//table//tbody/tr[1]//a");

    // Product detail page (opened after clicking the product name link)
    private static final By INVALID_DATA_BADGE = By.xpath("//*[contains(normalize-space(.),'Invalid data')]");
    private static final By VALID_DATA_BADGE = By.xpath("//*[contains(normalize-space(.),'Valid data')]");
    private static final By EDIT_BUTTON = By.xpath("//button[normalize-space()='Edit']");
    // TODO: Verify Fragile/MSI field locators — likely dropdowns similar to other custom selects
    private static final By FRAGILE_FIELD = By.xpath(
        "//*[normalize-space(.)='Fragile']/following::*[self::select or self::button or self::div][1]");
    private static final By MSI_FIELD = By.xpath(
        "//*[normalize-space(.)='MSI']/following::*[self::select or self::button or self::div][1]");
    private static final By SAVE_BUTTON = By.xpath("//button[normalize-space()='Save']");

    public MiraklCatalogPage(WebDriver driver) {
        super(driver);
    }

    public void navigateToProductImports() {
        LoggerUtility.info("Navigating Mirakl: Catalog -> Product Imports");
        jsClick(CATALOG_MENU);
        jsClick(PRODUCT_IMPORTS_LINK);
    }

    public void navigateToCatalogManagement() {
        LoggerUtility.info("Navigating Mirakl: Catalog -> Catalog Management");
        jsClick(CATALOG_MENU);
        jsClick(CATALOG_MANAGEMENT_LINK);
    }

    public void searchBySku(String sku) {
        LoggerUtility.info("Searching Mirakl Catalog for SKU: " + sku);
        WaitUtility.fluentWaitForClickable(driver, SKU_SEARCH_TYPE_DROPDOWN);
        WaitUtility.fluentWaitForClickable(driver, SKU_SEARCH_FIELD);
        type(SKU_SEARCH_FIELD, sku);
        pressEnter(SKU_SEARCH_FIELD);
    }

    public boolean hasResults() {
        return isDisplayed(RESULTS_ROW);
    }

    /** Refreshes and re-searches up to maxRetries times (30s apart) until the SKU appears. */
    public boolean refreshUntilVisible(String sku, int maxRetries) throws InterruptedException {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            searchBySku(sku);
            if (hasResults()) {
                LoggerUtility.info("SKU " + sku + " visible on attempt " + attempt + "/" + maxRetries);
                return true;
            }
            LoggerUtility.info("SKU " + sku + " not visible yet (attempt " + attempt + "/" + maxRetries
                + ") — waiting 30 seconds...");
            if (attempt < maxRetries) {
                Thread.sleep(30_000);
                driver.navigate().refresh();
            }
        }
        return false;
    }

    public String getFirstResultStatus() {
        String status = getText(RESULTS_ROW_STATUS);
        LoggerUtility.info("Mirakl Catalog — first result status: " + status);
        return status;
    }

    public void clickProductNameLink() {
        LoggerUtility.info("Clicking product name link");
        click(PRODUCT_NAME_LINK);
    }

    /** Fallback source of product name when the Excel template has no "Product Name" column. */
    public String getFirstResultProduct() {
        String name = getText(PRODUCT_NAME_LINK);
        LoggerUtility.info("Mirakl Catalog — first result product name: " + name);
        return name;
    }

    public boolean isInvalidDataDisplayed() {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        try {
            return driver.findElement(INVALID_DATA_BADGE).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        }
    }

    public boolean isValidDataDisplayed() {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        try {
            return driver.findElement(VALID_DATA_BADGE).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        }
    }

    public void clickEdit() {
        LoggerUtility.info("Clicking Edit on product detail page");
        click(EDIT_BUTTON);
    }

    public void setFragile(String value) {
        LoggerUtility.info("Setting Fragile = " + value);
        jsClick(FRAGILE_FIELD);
        By option = By.xpath("//li[normalize-space()='" + value + "'] | //div[@role='option' and normalize-space()='"
            + value + "']");
        jsClick(option);
    }

    public void setMsi(String value) {
        LoggerUtility.info("Setting MSI = " + value);
        jsClick(MSI_FIELD);
        By option = By.xpath("//li[normalize-space()='" + value + "'] | //div[@role='option' and normalize-space()='"
            + value + "']");
        jsClick(option);
    }

    public void clickSave() {
        LoggerUtility.info("Clicking Save on product detail page");
        click(SAVE_BUTTON);
    }
}
