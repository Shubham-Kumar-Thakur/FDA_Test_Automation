package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class MiraklOrderDetailPage extends BasePage {

    // TODO: Verify locators against actual Mirakl order detail DOM
    private static final By ORDER_STATUS_BADGE     = By.xpath("(//span[contains(@class,'_6e782__sc-s4ukbq-0') and not(ancestor::a)])[1]");
    private static final By ORDER_TOTAL_LABEL      = By.xpath("//span[text()='Total order amount']/../following-sibling::div/h3");
    private static final By ACCEPT_BUTTON          = By.xpath("//span[contains(text(),'Accept')] |//span[text()='Accept2']");
    // "3PL delivery" label in right sidebar, or carrier name field as fallback
    private static final By DELIVERY_PARTNER_LABEL = By.xpath(
        "//span[text()='3PL delivery']/../following-sibling::div/div/p | " +
        "//*[contains(text(),'Delivery partner')]/following-sibling::*[1] | " +
        "//*[normalize-space()='Carrier' or normalize-space()='Carrier name' or " +
        "normalize-space()='Transporteur' or normalize-space()='Shipping carrier']" +
        "/following-sibling::*[1]");

    // Delivery slip PDF filename — always present once Mirakl generates the slip
    // text format: "delivery-4000270061WEB-A.pdf - 23.17 kB"
    private static final By DELIVERY_SLIP_FILENAME = By.xpath(
        "//*[contains(text(),'delivery-') and contains(text(),'.pdf')]");

    // Carrier & tracking — may appear after 3PL processes the shipment label
    private static final By SHIPMENT_CARRIER_NAME  = By.xpath(
        "//*[normalize-space()='Carrier' or normalize-space()='Carrier name' or " +
        "normalize-space()='Transporteur' or normalize-space()='Shipping carrier']" +
        "/following-sibling::*[1] | " +
        "//*[normalize-space()='Carrier' or normalize-space()='Carrier name' or " +
        "normalize-space()='Transporteur']/parent::*[1]/following-sibling::*[1]");
    private static final By SHIPMENT_TRACKING_NUM  = By.xpath(
        "//*[normalize-space()='Tracking number' or normalize-space()='Tracking code' or " +
        "normalize-space()='Numéro de suivi' or normalize-space()='Tracking link']" +
        "/following-sibling::*[1] | " +
        "//*[normalize-space()='Tracking number' or normalize-space()='Tracking code' or " +
        "normalize-space()='Tracking link']/parent::*[1]/following-sibling::*[1]");

    private static final By DELIVERY_SLIP_LOADING  = By.xpath(
        "//*[contains(text(),'Delivery slip generation can take')]");
    // Ready when the delivery slip PDF filename is visible on the page
    private static final By DELIVERY_SLIP_READY    = By.xpath(
        "//*[contains(text(),'delivery-') and contains(text(),'.pdf')]");

    public MiraklOrderDetailPage(WebDriver driver) {
        super(driver);
    }

    public String getOrderStatus() {
        String status = getText(ORDER_STATUS_BADGE);
        LoggerUtility.info("Mirakl Status = " + status);
        return status;
    }

    public String getOrderTotal() {
        String total = getText(ORDER_TOTAL_LABEL);
        LoggerUtility.info("Mirakl order total: " + total);
        return total;
    }

    public void clickAcceptButton() {
        LoggerUtility.info("Clicking Accept button on Mirakl order detail");
        scrollIntoView(ACCEPT_BUTTON);
        jsClick(ACCEPT_BUTTON);
        // Handle optional confirmation dialog (covers both single-order 'Accept' and split 'Accept2')
        By confirmInDialog = By.xpath(
            "//div[@role='dialog']//span[text()='Accept'] | " +
            "//div[@role='dialog']//span[text()='Accept2'] | " +
            "//div[@role='dialog']//button[normalize-space()='Accept'] | " +
            "//div[@role='dialog']//button[normalize-space()='Accept2'] | " +
            "//div[contains(@class,'modal')]//span[text()='Accept'] | " +
            "//div[contains(@class,'modal')]//span[text()='Accept2']");
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(5));
        try {
            if (!driver.findElements(confirmInDialog).isEmpty()) {
                driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
                LoggerUtility.info("Mirakl Accept confirmation dialog detected — confirming");
                jsClick(confirmInDialog);
            }
        } catch (Exception e) {
            LoggerUtility.info("No confirmation dialog detected");
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
        // Wait for the SPA to update the status after acceptance API call completes
        WaitUtility.fluentWaitForText(driver, ORDER_STATUS_BADGE, "Awaiting shipment");
        LoggerUtility.info("Mirakl order accepted — status updated to Awaiting shipment");
    }

    /**
     * Returns true if the Accept button is present within the given timeout.
     * Used to skip acceptance for auto-accepted split-order shipments.
     */
    public boolean isAcceptButtonPresent(int timeoutSeconds) {
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(timeoutSeconds));
        try {
            return !driver.findElements(ACCEPT_BUTTON).isEmpty();
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
    }

    public String getDeliveryPartner() {
        String partner = getText(DELIVERY_PARTNER_LABEL);
        LoggerUtility.info("Mirakl 3PL delivery: " + partner);
        return partner;
    }

    public boolean isShipmentDataAvailable() {
        // Wait up to 30s for the delivery slip PDF filename to appear on page
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(30));
        try {
            driver.findElement(DELIVERY_SLIP_READY);
            LoggerUtility.info("Shipment/delivery slip data is available in Mirakl");
            return true;
        } catch (Exception e) {
            LoggerUtility.info("Shipment data not yet available in Mirakl");
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
    }

    public void waitForShipmentData() {
        LoggerUtility.info("Waiting for shipment data to appear on Mirakl order detail");
        WaitUtility.fluentWait(driver, DELIVERY_SLIP_FILENAME);
    }

    /**
     * Parses the tplShipmentId from the delivery slip PDF filename.
     * Filename format: "delivery-4000270061WEB-A.pdf - 23.17 kB"
     * Returns:          "4000270061WEB-A"
     */
    public String getShipmentParcelRef() {
        scrollIntoView(DELIVERY_SLIP_FILENAME);
        String text = getText(DELIVERY_SLIP_FILENAME);
        LoggerUtility.info("Delivery slip filename text: " + text);
        // Extract the ref between "delivery-" and ".pdf"
        String ref = text.replaceAll(".*?delivery-(.+?)\\.pdf.*", "$1").trim();
        LoggerUtility.info("Mirakl shipment parcel ref (tplShipmentId): " + ref);
        return ref;
    }

    public String getShipmentCarrierName() {
        // Scroll to bottom to ensure all sections are visible
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        String carrier = getText(SHIPMENT_CARRIER_NAME);
        LoggerUtility.info("Mirakl shipment carrier: " + carrier);
        return carrier;
    }

    public String getShipmentTrackingNumber() {
        String tracking = getText(SHIPMENT_TRACKING_NUM);
        LoggerUtility.info("Mirakl shipment tracking number: " + tracking);
        return tracking;
    }

    /**
     * Checks the page source for each candidate SKU and returns the first match.
     * Used by TC_FBO_028 to identify which product belongs to a specific shipment,
     * so the correct productId is sent to the cancel-shipment API.
     */
    public String findProductSkuOnPage(String... candidateSkus) {
        String pageSource = driver.getPageSource();
        for (String sku : candidateSkus) {
            if (pageSource.contains(sku)) {
                LoggerUtility.info("Found product SKU on Mirakl shipment detail page: " + sku);
                return sku;
            }
        }
        LoggerUtility.warn("No candidate SKU found on Mirakl shipment detail page. Checked: "
                + java.util.Arrays.toString(candidateSkus));
        return "";
    }

    public void clickOrderInList(String orderId) {
        LoggerUtility.info("Clicking order in Mirakl list: " + orderId);
        By targetRow = By.xpath("//tbody/tr[contains(.,'" + orderId + "')]//a[1]");
        try {
            jsClick(targetRow);
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            LoggerUtility.warn("Stale on clickOrderInList, retrying");
            jsClick(targetRow);
        }
    }
}
