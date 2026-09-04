package FDA_Automation_Script.FDA_Automation_Script.pages.fda;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class FDAOrderHistoryPage extends BasePage {

    // TODO: Verify locators against actual Mis pedidos page DOM
	private static final By PAGE_HEADING      = By.cssSelector(".page-title span, h1.page-title");
    private static final By ORDER_TABLE_ROWS  = By.cssSelector("table.orders-recent tr.odd, table.orders-recent tr.even, .orders-history table tr");

    public FDAOrderHistoryPage(WebDriver driver) {
        super(driver);
    }

    public boolean isOrderHistoryDisplayed() {
        try {
            WaitUtility.fluentWait(driver, PAGE_HEADING);
            LoggerUtility.info("Mis pedidos page displayed: true");
            return true;
        } catch (Exception e) {
            LoggerUtility.info("Mis pedidos page displayed: false");
            return false;
        }
    }

    public boolean isOrderPresent(String orderId) {
        boolean present = isDisplayed(By.xpath("//table//td[contains(text(),'" + orderId + "')] | //a[contains(text(),'" + orderId + "')]"));
        LoggerUtility.info("Order ID " + orderId + " present in order history: " + present);
        return present;
    }

    public String getOrderStatus(String orderId) {
        // Locate the row containing orderId and get the status cell
        String status = getText(By.xpath(
                "//table//td[contains(text(),'" + orderId + "')]/following-sibling::td[@class[contains(.,'status')]] | " +
                "//tr[.//a[contains(text(),'" + orderId + "')]]//td[@class[contains(.,'status')]]"));
        LoggerUtility.info("FDA order status for " + orderId + ": " + status);
        return status;
    }
}
