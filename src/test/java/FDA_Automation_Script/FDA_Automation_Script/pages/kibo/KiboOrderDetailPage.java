package FDA_Automation_Script.FDA_Automation_Script.pages.kibo;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class KiboOrderDetailPage extends BasePage {

    // TODO: Verify locators against actual Kibo order detail page DOM
	private static final By ORDER_STATUS_BADGE   = By.xpath("//span[@id='taco-navigate-to-parent']/..//span[@data-role='nav-header-pill']");
    private static final By PAYMENTS_TAB         = By.xpath("//li[normalize-space()='Payments']");
    private static final By PAYMENT_STATUS_BADGE = By.xpath("//div[@class='title' and text()='Transaction History']/..//div[@class='details']");
    private static final By SHIPMENTS_TAB        = By.xpath("//li[@class='taco-link-button active x-item-selected']");
    private static final By CUSTOM_DATA_TAB      = By.xpath("//li[normalize-space()='Custom Data']");
    private static final By THREEpl_shipmentId   = By.xpath("//div[normalize-space()='3pl_shipmentId']/../following-sibling::td[1]/div");
    private static final By CarrierName          = By.xpath("//div[normalize-space()='carrierName']/../following-sibling::td[1]/div");
    private static final By tracking_number      = By.xpath("//div[normalize-space()='tracking_number']/../following-sibling::td[1]/div");
    private static final By labelUrl             = By.xpath("//div[normalize-space()='labelUrl']/../following-sibling::td[1]/div");
    private static final By tracking_url         = By.xpath("//div[normalize-space()='tracking_url']/../following-sibling::td[1]/div");
    private static final By DeliveryPartner      = By.xpath("//div[normalize-space()='deliveryPartner']/../following-sibling::td[1]/div");

    // Custom Data field locators -- values are displayed as key/value pairs
    // TODO: Adjust row XPath based on actual key label structure in Kibo custom data panel
    private static final String CUSTOM_ROW_XPATH = "//tr[.//td[normalize-space()='%s']]/td[2] | //div[.//span[normalize-space()='%s']]/following-sibling::div | //*[@data-key='%s']";

    public KiboOrderDetailPage(WebDriver driver) {
        super(driver);
    }

    public String getOrderStatus() {
        String status = getText(ORDER_STATUS_BADGE);
        LoggerUtility.info("Kibo Status = " + status);
        return status;
    }

    public void clickPaymentsTab() {
        LoggerUtility.info("Clicking Payments tab");
        click(PAYMENTS_TAB);
    }

    public String getPaymentStatus() {
        String status = getText(PAYMENT_STATUS_BADGE);
        LoggerUtility.info("Payment Status = " + status);
        return status;
    }

    public void clickShipmentsTab() {
        LoggerUtility.info("Clicking Shipments tab");
        click(SHIPMENTS_TAB);
    }

    public void clickCustomDataTab() {
        LoggerUtility.info("Clicking Custom Data tab");
        click(CUSTOM_DATA_TAB);
    }

    public void clickOrderInSearchResults(String orderId) {
        LoggerUtility.info("Clicking order in search results: " + orderId);
        click(By.xpath("//a[contains(text(),'" + orderId + "')] | //td[contains(text(),'" + orderId + "')]"));
    }

    private String getCustomDataValue(String key) {
        By locator = By.xpath(String.format(CUSTOM_ROW_XPATH, key, key, key));
        String value = getText(locator).trim();
        LoggerUtility.info(key + " = " + value);
        return value;
    }

    public boolean verify3plShipmentId() {
        boolean present = isDisplayed(By.xpath(String.format(CUSTOM_ROW_XPATH, "3pl_shipmentId", "3pl_shipmentId", "3pl_shipmentId")));
        LoggerUtility.info("3pl_shipmentId value available: " + present);
        return present;
    }

    public String get3plShipmentId() {
        String val = getCustomDataValue("3pl_shipmentId");
        LoggerUtility.info("Shipment ID = " + val);
        return val;
    }

    public boolean verifyCarrierName() {
        boolean present = isDisplayed(By.xpath(String.format(CUSTOM_ROW_XPATH, "carrierName", "carrierName", "carrierName")));
        LoggerUtility.info("carrierName value available: " + present);
        return present;
    }

    public String getCarrierName() {
        String val = getCustomDataValue("carrierName");
        LoggerUtility.info("Carrier = " + val);
        return val;
    }

    public boolean verifyTrackingNumber() {
        boolean present = isDisplayed(By.xpath(String.format(CUSTOM_ROW_XPATH, "tracking_number", "tracking_number", "tracking_number")));
        LoggerUtility.info("tracking_number value available: " + present);
        return present;
    }

    public String getTrackingNumber() {
        String val = getCustomDataValue("tracking_number");
        LoggerUtility.info("tracking_number = " + val);
        return val;
    }

    public boolean verifyLabelUrl() {
        boolean present = isDisplayed(By.xpath(String.format(CUSTOM_ROW_XPATH, "labelUrl", "labelUrl", "labelUrl")));
        LoggerUtility.info("labelUrl value available: " + present);
        return present;
    }

    public String getLabelUrl() {
        String val = getCustomDataValue("labelUrl");
        LoggerUtility.info("Label URL = " + val);
        return val;
    }

    public boolean verifyTrackingUrl() {
        boolean present = isDisplayed(By.xpath(String.format(CUSTOM_ROW_XPATH, "tracking_url", "tracking_url", "tracking_url")));
        LoggerUtility.info("tracking_url value available: " + present);
        return present;
    }

    public String getTrackingUrl() {
        String val = getCustomDataValue("tracking_url");
        LoggerUtility.info("Tracking URL = " + val);
        return val;
    }

    public boolean verifyDeliveryPartner() {
        boolean present = isDisplayed(By.xpath(String.format(CUSTOM_ROW_XPATH, "deliveryPartner", "deliveryPartner", "deliveryPartner")));
        LoggerUtility.info("deliveryPartner value available: " + present);
        return present;
    }

    public String getDeliveryPartner() {
        String val = getCustomDataValue("deliveryPartner");
        LoggerUtility.info("Delivery Partner = " + val);
        return val;
    }
}
