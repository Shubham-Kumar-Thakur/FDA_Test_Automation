package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class MiraklOrdersPage extends BasePage {

    // TODO: Verify locators against actual Mirakl navigation DOM
	private static final By ORDERS_MENU    = By.xpath("//span[normalize-space()='Orders']");
    private static final By ALL_ORDERS_LINK = By.xpath("//span[normalize-space()='All orders']");
    private static final By SEARCH_FIELD   = By.xpath("//input[@id='search' and @name='search']");
    private static final By ORDER_STATUS   = By.xpath("//span[text()='Status']/../../../../../following-sibling::tbody/tr/td[3]/div/div/div/span/div");

    public MiraklOrdersPage(WebDriver driver) {
        super(driver);
    }

    public void clickOrdersMenu() {
        LoggerUtility.info("Clicking Orders menu in Mirakl");
        jsClick(ORDERS_MENU);
    }

    public void clickAllOrders() {
        LoggerUtility.info("Clicking All Orders link in Mirakl");
        jsClick(ALL_ORDERS_LINK);
    }

    public void searchOrder(String orderId) {
        LoggerUtility.info("Searching Mirakl for order: " + orderId);
        type(SEARCH_FIELD, orderId);
        pressEnter(SEARCH_FIELD);
    }

    public String getFirstOrderStatus() {
        String status = getText(ORDER_STATUS);
        LoggerUtility.info("Mirakl Status = " + status);
        return status;
    }

    public boolean hasSearchResults(String commercialId) {
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(15));
        try {
            driver.findElement(By.xpath("//tbody/tr[contains(.,'" + commercialId + "')]"));
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
    }
}
