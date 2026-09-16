package FDA_Automation_Script.FDA_Automation_Script.tests;

import FDA_Automation_Script.FDA_Automation_Script.base.BaseClass;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDACartPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAHomePage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDALoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAOrderHistoryPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAPaymentPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAPDPPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDASuccessPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklLoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklOrderDetailPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklOrdersPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklReturnPage;
import FDA_Automation_Script.FDA_Automation_Script.utils.ApiUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ReturnApiUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import io.restassured.response.Response;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TC_FBO_021_Test extends BaseClass {

    private static final String TC_NAME = "TC_FBO_021";
    private static final String SKU_1   = "7080901020316";
    private static final String SKU_2   = "7080901020305";

    // --- Page objects ---
    private FDAHomePage           fdaHomePage;
    private FDALoginPage          fdaLoginPage;
    private FDAPDPPage            fdaPdpPage;
    private FDACartPage           fdaCartPage;
    private FDAPaymentPage        fdaPaymentPage;
    private FDASuccessPage        fdaSuccessPage;
    private FDAOrderHistoryPage   fdaOrderHistoryPage;
    private MiraklLoginPage       miraklLoginPage;
    private MiraklOrdersPage      miraklOrdersPage;
    private MiraklOrderDetailPage miraklOrderDetailPage;
    private MiraklReturnPage      miraklReturnPage;

    // --- Dynamic data ---
    private String orderId;
    private String orderTotal;

    private static class ShipmentInfo {
        String shipmentRef;
        String tplShipmentId;
        String carrierName;
        String trackingNumber;
        String deliveryPartner;
        String miraklDetailUrl;
    }

    private final List<ShipmentInfo> shipments = new ArrayList<>();

    private static final By MIRAKL_ORDER_STATUS_LOCATOR =
        By.xpath("(//span[contains(@class,'_6e782__sc-s4ukbq-0') and not(ancestor::a)])[1]");

    @BeforeClass
    public void initPageObjects() {
        fdaHomePage           = new FDAHomePage(driver);
        fdaLoginPage          = new FDALoginPage(driver);
        fdaPdpPage            = new FDAPDPPage(driver);
        fdaCartPage           = new FDACartPage(driver);
        fdaPaymentPage        = new FDAPaymentPage(driver);
        fdaSuccessPage        = new FDASuccessPage(driver);
        fdaOrderHistoryPage   = new FDAOrderHistoryPage(driver);
        miraklLoginPage       = new MiraklLoginPage(driver);
        miraklOrdersPage      = new MiraklOrdersPage(driver);
        miraklOrderDetailPage = new MiraklOrderDetailPage(driver);
        miraklReturnPage      = new MiraklReturnPage(driver);
        LoggerUtility.info("All page objects initialized for TC_FBO_021");
    }

    @Test(testName = TC_NAME, groups = {"FBO"},
          description = "Verify 2-SKU 3P seller order: 2 shipments → full fulfillment → return → compliance → full refund → Closed")
    public void tc_fbo_021_dual_product_return_refund() throws InterruptedException {

        String urlShipmentA = "";
        String urlShipmentB = "";

        // ============================================================
        // PHASE 1: FDA — Login, Add 2 Products, Cart, Payment, Order
        // ============================================================

        // Suite @BeforeSuite already logged in to FDA. Ensure focus is on the FDA tab.
        switchToFDATab();
        LoggerUtility.info("===== PHASE 1: FDA Order Placement =====");
        LoggerUtility.info("TC_FBO_021: Starting — FDA and Mirakl sessions active from suite setup");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Pre-test: Clearing leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ---- Product 1 (SKU_1) ----
        LoggerUtility.info("Step 10: Searching SKU 1: " + SKU_1);
        fdaHomePage.enterSearchQuery(SKU_1);
        fdaHomePage.pressSearchEnter();
        Assert.assertTrue(fdaPdpPage.isDisplayed(), "PDP not displayed for SKU 1: " + SKU_1);
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(), "Add to cart not enabled for SKU 1: " + SKU_1);
        Assert.assertEquals(fdaPdpPage.getQuantity(), "1", "SKU 1 PDP quantity should be 1");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("Step 11: Adding Product 1 to cart");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product 1 added to cart");
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ---- Product 2 (SKU_2) ----
        LoggerUtility.info("Step 12: Searching SKU 2: " + SKU_2);
        fdaHomePage.enterSearchQuery(SKU_2);
        fdaHomePage.pressSearchEnter();
        Assert.assertTrue(fdaPdpPage.isDisplayed(), "PDP not displayed for SKU 2: " + SKU_2);
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(), "Add to cart not enabled for SKU 2: " + SKU_2);
        Assert.assertEquals(fdaPdpPage.getQuantity(), "1", "SKU 2 PDP quantity should be 1");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("Step 13: Adding Product 2 to cart");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product 2 added to cart");

        // ---- Cart validation — 2 products ----
        LoggerUtility.info("Step 14: Verifying cart with 2 products");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());
        int itemCount = fdaCartPage.getItemCount();
        // Retry if second item hasn't appeared yet (Magento sections API may lag)
        for (int r = 0; r < 5 && itemCount < 2; r++) {
            LoggerUtility.info("Cart shows " + itemCount + " item(s) — refreshing (retry " + (r + 1) + "/5)");
            driver.navigate().refresh();
            WaitUtility.fluentWait(driver, By.cssSelector("input[data-role='cart-item-qty']"));
            itemCount = fdaCartPage.getItemCount();
        }
        LoggerUtility.info("Cart item count: " + itemCount);
        Assert.assertEquals(itemCount, 2, "Cart should contain exactly 2 products");
        List<String> quantities = fdaCartPage.getAllQuantities();
        LoggerUtility.info("Cart quantities: " + quantities);
        Assert.assertEquals(quantities.size(), 2, "Should have 2 quantity inputs in cart");
        Assert.assertEquals(quantities.get(0), "1", "Product 1 cart quantity should be 1");
        Assert.assertEquals(quantities.get(1), "1", "Product 2 cart quantity should be 1");
        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(), "Order total should be displayed in cart");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ---- Checkout ----
        LoggerUtility.info("Step 15: Proceeding to payment");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();
        LoggerUtility.info("Step 16: Clicking Siguiente on shipping page");
        fdaPaymentPage.clickNextButton();
        LoggerUtility.info("Step 17: Selecting Credit Card and entering details");
        fdaPaymentPage.selectCreditCardOption();
        fdaPaymentPage.enterCardNumber(config.getFdaCardNumber());
        fdaPaymentPage.enterExpiry(config.getFdaCardExpiry());
        fdaPaymentPage.enterCvv(config.getFdaCardCvv());
        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Completar pago button text: " + payBtnText);
        Assert.assertTrue(
            payBtnText.contains("Completar pago") || payBtnText.contains("MXN") || payBtnText.contains("$"),
            "Pay button should show amount. Actual: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 18: Completing payment");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(), "Order success page not displayed after payment");
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 19: Order ID = " + orderId);
        Assert.assertFalse(orderId.isEmpty(), "Order ID should be present on success page");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ---- Order history ----
        LoggerUtility.info("Step 20: Checking order history");
        fdaHomePage.clickProfileIcon();
        fdaHomePage.clickMyOrdersLink();
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(), "Order history page not displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
            "Order ID " + orderId + " not found in order history");
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("FDA order status: " + fdaStatus);
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
            "FDA status should be 'Creada' or 'Pendiente'. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 2: MIRAKL — Login, Verify 2 Shipments (WEB-A + WEB-B), Accept Both
        // ============================================================

        LoggerUtility.info("===== PHASE 2: Mirakl — Verify and Accept Both Shipments =====");
        // Switch to Mirakl tab — session active from suite setup
        LoggerUtility.info("Step 21: Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();

        String miraklSearchTerm = orderId + "WEB";
        String shipmentRefA     = orderId + "WEB-A";
        String shipmentRefB     = orderId + "WEB-B";

        LoggerUtility.info("Step 22: Searching Mirakl for order: " + miraklSearchTerm);
        boolean orderFoundInMirakl = false;
        for (int attempt = 1; attempt <= 5; attempt++) {
            LoggerUtility.info("Mirakl search attempt " + attempt + "/5 for: " + miraklSearchTerm);
            miraklOrdersPage.searchOrder(miraklSearchTerm);
            if (miraklOrdersPage.hasSearchResults(miraklSearchTerm)) {
                orderFoundInMirakl = true;
                LoggerUtility.info("Order found in Mirakl on attempt " + attempt);
                break;
            }
            LoggerUtility.info("Order not in Mirakl yet — attempt " + attempt + "/5");
            if (attempt < 5) {
                LoggerUtility.info("Waiting 60 seconds before next attempt...");
                Thread.sleep(60_000);
                miraklOrdersPage.clickOrdersMenu();
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(orderFoundInMirakl,
            "Order " + miraklSearchTerm + " did not appear in Mirakl within 5 minutes");

        // Verify both WEB-A and WEB-B present
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefA),
            "Mirakl shipment WEB-A not found: " + shipmentRefA);
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefB),
            "Mirakl shipment WEB-B not found: " + shipmentRefB);
        LoggerUtility.info("Two shipments verified — WEB-A and WEB-B present in Mirakl");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ---- Accept WEB-A ----
        LoggerUtility.info("Step 23: Accepting shipment WEB-A");
        miraklOrderDetailPage.clickOrderInList(shipmentRefA);
        String statusA = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-A status before accept: " + statusA);
        Assert.assertEquals(statusA, "Pending acceptance",
            "WEB-A should be 'Pending acceptance'. Actual: " + statusA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        statusA = miraklOrderDetailPage.getOrderStatus();
        Assert.assertEquals(statusA, "Awaiting shipment",
            "WEB-A should be 'Awaiting shipment' after accept. Actual: " + statusA);
        urlShipmentA = driver.getCurrentUrl();
        LoggerUtility.info("WEB-A accepted. URL: " + urlShipmentA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Navigate back to All Orders to open WEB-B
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        miraklOrdersPage.searchOrder(miraklSearchTerm);

        // ---- Accept WEB-B ----
        LoggerUtility.info("Step 24: Accepting shipment WEB-B");
        miraklOrderDetailPage.clickOrderInList(shipmentRefB);
        String statusB = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-B status before accept: " + statusB);
        Assert.assertEquals(statusB, "Pending acceptance",
            "WEB-B should be 'Pending acceptance'. Actual: " + statusB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        statusB = miraklOrderDetailPage.getOrderStatus();
        Assert.assertEquals(statusB, "Awaiting shipment",
            "WEB-B should be 'Awaiting shipment' after accept. Actual: " + statusB);
        urlShipmentB = driver.getCurrentUrl();
        LoggerUtility.info("WEB-B accepted. URL: " + urlShipmentB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 52 — Wait 2 minutes for Kibo/3PL connector to generate shipment labels
        LoggerUtility.info("Step 52: Waiting 2 minutes for 3PL connector to process...");
        Thread.sleep(120_000);
        LoggerUtility.info("2-minute wait complete");

        // ============================================================
        // PHASE 3: KIBO API — Auth + Collect Shipment Data for Both Shipments
        // ============================================================

        LoggerUtility.info("===== PHASE 3: Kibo API — Shipment Data =====");
        LoggerUtility.info("========== Kibo Authentication ==========");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(), "Kibo access token should not be empty");

        String externalId = orderId + "WEB";
        LoggerUtility.info("========== Find Kibo Order ID ==========");
        LoggerUtility.info("Searching externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(), "Kibo Order ID not found for: " + externalId);
        LoggerUtility.info("Kibo Order ID: " + kiboOrderId);

        LoggerUtility.info("========== Polling Kibo Shipments (2 expected) ==========");
        boolean allShipmentsReady = false;
        for (int attempt = 1; attempt <= 20; attempt++) {
            LoggerUtility.info("Kibo shipment poll attempt " + attempt + "/20");
            Response shipmentsResp = ApiUtility.kiboGetShipmentsByOrderId(kiboToken, kiboOrderId);
            Assert.assertEquals(shipmentsResp.getStatusCode(), 200,
                "Kibo Get Shipments should return 200. Actual: " + shipmentsResp.getStatusCode());

            List<Map<String, Object>> shipmentList = shipmentsResp.jsonPath().getList("items");
            if (shipmentList == null || shipmentList.isEmpty()) {
                shipmentList = shipmentsResp.jsonPath().getList("_embedded.shipments");
            }

            if (shipmentList != null && shipmentList.size() >= 2) {
                List<ShipmentInfo> ready = new ArrayList<>();
                boolean allPopulated = true;
                for (Map<String, Object> s : shipmentList) {
                    ShipmentInfo info = new ShipmentInfo();
                    info.tplShipmentId   = extractCustomField(s, "3pl_shipmentId");
                    info.carrierName     = extractCustomField(s, "carrierName");
                    info.trackingNumber  = extractCustomField(s, "tracking_number");
                    info.deliveryPartner = extractCustomField(s, "deliveryPartner");
                    if (info.tplShipmentId.isEmpty() || info.deliveryPartner.isEmpty()) {
                        allPopulated = false;
                        LoggerUtility.info("3PL data not ready — tplShipmentId='" + info.tplShipmentId
                            + "' deliveryPartner='" + info.deliveryPartner + "'");
                    } else {
                        ready.add(info);
                    }
                }
                if (allPopulated && ready.size() >= 2) {
                    ready.get(0).shipmentRef     = shipmentRefA;
                    ready.get(0).miraklDetailUrl = urlShipmentA;
                    ready.get(1).shipmentRef     = shipmentRefB;
                    ready.get(1).miraklDetailUrl = urlShipmentB;
                    shipments.addAll(ready);
                    allShipmentsReady = true;
                    LoggerUtility.info("All " + shipments.size() + " shipments have 3PL data on attempt " + attempt);
                    break;
                }
            } else {
                LoggerUtility.info("Kibo returned fewer than 2 shipments — waiting 30s (attempt " + attempt + ")");
            }
            if (attempt < 20) Thread.sleep(30_000);
        }
        Assert.assertTrue(allShipmentsReady, "Not all shipment 3PL data populated within 10 minutes");
        Assert.assertEquals(shipments.size(), 2, "Expected exactly 2 shipments from Kibo");

        for (int i = 0; i < shipments.size(); i++) {
            ShipmentInfo info = shipments.get(i);
            LoggerUtility.info("--- Shipment " + (i + 1) + " [" + info.shipmentRef + "] ---");
            LoggerUtility.info("  Delivery Partner : " + info.deliveryPartner);
            LoggerUtility.info("  3PL Shipment ID  : " + info.tplShipmentId);
            LoggerUtility.info("  Carrier          : " + info.carrierName);
            LoggerUtility.info("  Tracking Number  : " + info.trackingNumber);
        }
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: Carrier Webhook + Status Validation (loop per shipment)
        // ============================================================

        LoggerUtility.info("===== PHASE 4: Carrier Webhooks — Both Shipments to Received =====");
        for (ShipmentInfo info : shipments) {
            LoggerUtility.info("========== Processing Shipment: " + info.shipmentRef + " ==========");
            LoggerUtility.info("Delivery Partner: " + info.deliveryPartner);
            switchToMiraklTab();
            driver.get(info.miraklDetailUrl);
            WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);

            if (info.deliveryPartner.toLowerCase().contains("envioclick")) {
                runEnvioclickFlow(info);
            } else if (info.deliveryPartner.toLowerCase().contains("skydropx")) {
                runSkydropxFlow(info);
            } else {
                Assert.fail("Unknown delivery partner for " + info.shipmentRef
                    + ": " + info.deliveryPartner + ". Expected 'Envioclick' or 'Skydropx'.");
            }
        }
        LoggerUtility.info("Both shipments received. Proceeding to return flow.");

        // ============================================================
        // PHASE 5: Return — Get order_line_id for WEB-A + Create Return via API
        // ============================================================

        LoggerUtility.info("===== PHASE 5: Return — API Flow =====");

        String orderCommercialId = orderId + "WEB";
        LoggerUtility.info("========== Get Mirakl Order Line ID ==========");
        LoggerUtility.info("Getting order_line_id for commercial order: " + orderCommercialId);
        String orderLineId = ReturnApiUtility.getMiraklOrderLineId(orderCommercialId);
        Assert.assertFalse(orderLineId.isEmpty(),
            "order_line_id should not be empty for commercial order: " + orderCommercialId);
        LoggerUtility.info("Order Line ID : " + orderLineId);
        LoggerUtility.info("========== Create Return ==========");
        LoggerUtility.info("order_commercial_id : " + orderCommercialId);
        LoggerUtility.info("order_line_id       : " + orderLineId);
        Response returnResponse = ReturnApiUtility.postReturn(orderCommercialId, orderLineId);
        Assert.assertTrue(
            returnResponse.getStatusCode() == 200 || returnResponse.getStatusCode() == 201,
            "Return service should return 200 or 201. Actual: " + returnResponse.getStatusCode()
            + " | Body: " + returnResponse.getBody().asString());
        LoggerUtility.info("Return created — HTTP " + returnResponse.getStatusCode());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 6: Mirakl Return UI — Steps 68-78
        // Navigate to Orders → search WEB-A → then return actions on order detail
        // ============================================================

        LoggerUtility.info("===== PHASE 6: Mirakl Return UI =====");
        switchToMiraklTab();

        // Navigate to All Orders and search for WEB-A — return actions live on order detail page
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        LoggerUtility.info("Searching for WEB-A order in Mirakl: " + shipmentRefA);
        miraklOrdersPage.searchOrder(shipmentRefA);
        miraklOrderDetailPage.clickOrderInList(shipmentRefA);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 68 — Click "Mark as received"
        LoggerUtility.info("Step 68: Clicking Mark as received");
        miraklReturnPage.clickMarkAsReceived();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 69 — Confirm popup "Mark as received"
        LoggerUtility.info("Step 69: Confirming Mark as received on popup");
        miraklReturnPage.confirmMarkAsReceivedPopup();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 70 — Click "Check compliance"
        LoggerUtility.info("Step 70: Clicking Check compliance");
        miraklReturnPage.clickCheckCompliance();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 71 — Click "Save"
        LoggerUtility.info("Step 71: Clicking Save");
        miraklReturnPage.clickSave();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 72 — Click "Refund" dropdown
        LoggerUtility.info("Step 72: Clicking Refund dropdown");
        miraklReturnPage.clickRefundDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 73 — Select "Full refund" from dropdown
        LoggerUtility.info("Step 73: Selecting Full refund from dropdown");
        miraklReturnPage.selectFullRefundFromDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 74 — Click "Select" dropdown for reason
        LoggerUtility.info("Step 74: Clicking Select dropdown for refund reason");
        miraklReturnPage.clickSelectReasonDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 75 — Select "Item returned"
        LoggerUtility.info("Step 75: Selecting Item returned");
        miraklReturnPage.selectItemReturned();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 76 — Click "Confirm" on refund popup
        LoggerUtility.info("Step 76: Clicking Confirm on refund popup");
        miraklReturnPage.confirmRefundPopup();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 77 — Wait 1 minute for refund to process, then refresh
        LoggerUtility.info("Step 77: Waiting 60 seconds for shipment closure to process...");
        Thread.sleep(60_000);
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 7: Step 78 — Verify Shipment 1 (WEB-A) Status = Closed
        // ============================================================

        LoggerUtility.info("===== PHASE 7: Step 78 — Verify Shipment 1 (WEB-A) = Closed =====");
        String closedStatus = waitForMiraklStatus("Closed", 6);
        LoggerUtility.info("Step 78: Shipment 1 (WEB-A) status after full refund: " + closedStatus);
        Assert.assertEquals(closedStatus, "Closed",
            "Shipment 1 (WEB-A) should be 'Closed' after full refund. Actual: " + closedStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        LoggerUtility.info("TC_FBO_021 PASSED — Shipment 1 (WEB-A) = Closed");
    }

    // ----------------------------------------------------------------
    // Envioclick flow for a single shipment (En tránsito → Shipped, Entregado → Received)
    // ----------------------------------------------------------------
    private void runEnvioclickFlow(ShipmentInfo info) {
        LoggerUtility.info("Calling Envioclick API — En tránsito for: " + info.shipmentRef);
        Response enTransito = ApiUtility.postEnvioclickEnTransito(
            info.tplShipmentId, info.trackingNumber, orderId, info.carrierName);
        Assert.assertEquals(enTransito.getStatusCode(), 200,
            "Envioclick En tránsito should return 200 for " + info.shipmentRef
            + ". Actual: " + enTransito.getStatusCode());
        LoggerUtility.info("API Response = " + enTransito.getStatusCode());

        String status = waitForMiraklStatusOneOf(new String[]{"Shipped", "3PL delivery"}, 20);
        LoggerUtility.info("Mirakl status after En tránsito [" + info.shipmentRef + "]: " + status);
        Assert.assertTrue(status.equals("Shipped") || status.equals("3PL delivery"),
            "Mirakl should be 'Shipped' or '3PL delivery' after En tránsito ["
            + info.shipmentRef + "]. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Calling Envioclick API — Entregado for: " + info.shipmentRef);
        Response entregado = ApiUtility.postEnvioclickEntregado(
            info.tplShipmentId, info.trackingNumber, orderId, info.carrierName);
        Assert.assertEquals(entregado.getStatusCode(), 200,
            "Envioclick Entregado should return 200 for " + info.shipmentRef
            + ". Actual: " + entregado.getStatusCode());
        LoggerUtility.info("API Response = " + entregado.getStatusCode());

        status = waitForMiraklStatus("Received", 30);
        LoggerUtility.info("Mirakl status after Entregado [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Received",
            "Mirakl should be 'Received' after Entregado [" + info.shipmentRef + "]. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("Envioclick flow complete — " + info.shipmentRef + " = Received");
    }

    // ----------------------------------------------------------------
    // Skydropx flow for a single shipment (Picked_up → Shipped, Delivered → Received)
    // ----------------------------------------------------------------
    private void runSkydropxFlow(ShipmentInfo info) {
        LoggerUtility.info("Calling Skydropx API — Picked_up for: " + info.shipmentRef);
        Response pickedUp = ApiUtility.postSkydropxPickedUp(info.tplShipmentId);
        Assert.assertEquals(pickedUp.getStatusCode(), 200,
            "Skydropx Picked_up should return 200 for " + info.shipmentRef
            + ". Actual: " + pickedUp.getStatusCode());
        LoggerUtility.info("API Response = " + pickedUp.getStatusCode());

        String status = waitForMiraklStatus("Shipped", 60);
        LoggerUtility.info("Mirakl status after Picked_up [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Shipped",
            "Mirakl should be 'Shipped' after Skydropx Picked_up [" + info.shipmentRef + "]. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Calling Skydropx API — Delivered for: " + info.shipmentRef);
        Response delivered = ApiUtility.postSkydropxDelivered(info.tplShipmentId);
        Assert.assertEquals(delivered.getStatusCode(), 200,
            "Skydropx Delivered should return 200 for " + info.shipmentRef
            + ". Actual: " + delivered.getStatusCode());
        LoggerUtility.info("API Response = " + delivered.getStatusCode());

        status = waitForMiraklStatus("Received", 60);
        LoggerUtility.info("Mirakl status after Delivered [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Received",
            "Mirakl should be 'Received' after Skydropx Delivered [" + info.shipmentRef + "]. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("Skydropx flow complete — " + info.shipmentRef + " = Received");
    }

    // ----------------------------------------------------------------
    // Poll Mirakl status — refresh + check up to maxRetries times
    // ----------------------------------------------------------------
    private String waitForMiraklStatus(String expected, int maxRetries) {
        String status = "";
        for (int i = 1; i <= maxRetries; i++) {
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            status = miraklOrderDetailPage.getOrderStatus();
            LoggerUtility.info("Mirakl status check " + i + "/" + maxRetries + ": " + status);
            if (expected.equals(status)) break;
        }
        return status;
    }

    private String waitForMiraklStatusOneOf(String[] expected, int maxRetries) {
        Set<String> expectedSet = new HashSet<>(Arrays.asList(expected));
        String status = "";
        for (int i = 1; i <= maxRetries; i++) {
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            status = miraklOrderDetailPage.getOrderStatus();
            LoggerUtility.info("Mirakl status check " + i + "/" + maxRetries + ": " + status);
            if (expectedSet.contains(status)) break;
        }
        return status;
    }

    // ----------------------------------------------------------------
    // Extract named custom field from a Kibo shipment item map
    // ----------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private String extractCustomField(Map<String, Object> item, String fieldKey) {
        Object dataRaw = item.get("data");
        if (dataRaw instanceof Map) {
            Object val = ((Map<String, Object>) dataRaw).get(fieldKey);
            if (val != null && !val.toString().isBlank()) return val.toString().trim();
        }
        Object pkgsRaw = item.get("packages");
        if (pkgsRaw instanceof List) {
            for (Object pkg : (List<?>) pkgsRaw) {
                if (pkg instanceof Map) {
                    Object pkgData = ((Map<String, Object>) pkg).get("data");
                    if (pkgData instanceof Map) {
                        Object val = ((Map<String, Object>) pkgData).get(fieldKey);
                        if (val != null && !val.toString().isBlank()) return val.toString().trim();
                    }
                }
            }
        }
        return "";
    }

    // ----------------------------------------------------------------
    // Handle checkout opening in new window/tab after clickProceedToPayment
    // ----------------------------------------------------------------
    private void switchToCheckoutWindow() {
        Set<String> handles = driver.getWindowHandles();
        LoggerUtility.info("Window handles after clickProceedToPayment: " + handles.size());
        if (!handles.contains(fdaTabHandle)) {
            fdaTabHandle = handles.iterator().next();
            driver.switchTo().window(fdaTabHandle);
            LoggerUtility.info("Original FDA window closed — switched to checkout: " + fdaTabHandle);
        } else if (handles.size() > 1) {
            for (String h : handles) {
                if (!h.equals(fdaTabHandle) && !h.equals(miraklTabHandle)) {
                    driver.switchTo().window(h);
                    fdaTabHandle = h;
                    LoggerUtility.info("Checkout in new window — switched to: " + fdaTabHandle);
                    break;
                }
            }
        }
    }

}
// @AfterMethod navigateToHomePage() is inherited from BaseClass
