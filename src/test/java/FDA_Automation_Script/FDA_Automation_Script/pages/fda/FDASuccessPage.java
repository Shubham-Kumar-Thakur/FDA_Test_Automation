package FDA_Automation_Script.FDA_Automation_Script.pages.fda;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class FDASuccessPage extends BasePage {

    // TODO: Verify locators against actual success page DOM
    private static final By SUCCESS_HEADING  = By.xpath(
        "//h1//span[contains(text(),'Muchas gracias')] | " +
        "//h1[contains(text(),'Muchas gracias')] | " +
        "//*[contains(@class,'checkout-success')]//*[contains(text(),'gracias')] | " +
        "//*[contains(@class,'page-title')][contains(text(),'gracias')]");
    private static final By ORDER_ID_ELEMENT = By.xpath(
        "//p[text()='N\u00FAmero de pedido ']/strong/a[@class='order-number'] | " +
        "//p[contains(.,'N\u00FAmero de pedido')]//a[@class='order-number'] | " +
        "//a[@class='order-number']");

    public FDASuccessPage(WebDriver driver) {
        super(driver);
    }

    public boolean isSuccessPageDisplayed() {
        try {
            WaitUtility.fluentWait(driver, SUCCESS_HEADING);
            LoggerUtility.info("Success page displayed: true");
            return true;
        } catch (Exception e) {
            LoggerUtility.info("Success page displayed: false — URL: " + driver.getCurrentUrl());
            LoggerUtility.info("Page title: " + driver.getTitle());
            return false;
        }
    }

    public String getOrderId() {
        String orderId = getText(ORDER_ID_ELEMENT);
        // Strip any non-numeric prefix/suffix that the site may add
        orderId = orderId.replaceAll("[^0-9]", "").trim();
        LoggerUtility.info("Order ID = " + orderId);
        return orderId;
    }
}
