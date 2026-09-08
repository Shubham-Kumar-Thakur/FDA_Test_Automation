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
import FDA_Automation_Script.FDA_Automation_Script.utils.ApiUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
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

public class TC_FBO_002_Test extends BaseClass {

    private static final String TC_NAME = "TC_FBO_002";

    // Two distinct 3P-seller SKUs for this test
    private static final String SKU_1 = "7080901020316";
    private static final String SKU_2 = "7080901020305";

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

    // --- Dynamic data ---
    private String orderId;
    private String orderTotal;

    // Per-shipment data collected from Kibo and Mirakl
    private static class ShipmentInfo {
        String shipmentRef;      // orderId + "WEB-A" or "WEB-B"
        String tplShipmentId;    // 3pl_shipmentId from Kibo
        String carrierName;
        String trackingNumber;
        String deliveryPartner;
        String miraklDetailUrl;  // Mirakl detail page URL captured after acceptance
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
        LoggerUtility.info("All page objects initialized for TC_FBO_002");
    }

    @Test(testName = TC_NAME, groups = {"FBO"},
          description = "Verify dual-product 3P seller order: 2 SKUs → 2 Mirakl shipments (WEB-A, WEB-B) → full fulfillment lifecycle")
    public void tc_fbo_002_dual_product_3p_seller_full_fulfillment() throws InterruptedException {

        // Mirakl detail URLs captured during acceptance phase, reused in carrier webhook phase
        String urlShipmentA = "";
        String urlShipmentB = "";

        // ============================================================
        // PHASE 1: FDA — Login, Add 2 Products, Cart, Payment, Order
        // ============================================================

        // Suite @BeforeSuite already logged in to FDA. Ensure focus is on the FDA tab.
        switchToFDATab();
        LoggerUtility.info("TC_FBO_002: Starting — FDA and Mirakl sessions active from suite setup");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test cleanup: remove cart items from any prior run
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ---- Product 1 (SKU_1) ----
        LoggerUtility.info("Step 10: Searching SKU 1: " + SKU_1);
        fdaHomePage.enterSearchQuery(SKU_1);
        fdaHomePage.pressSearchEnter();

        LoggerUtility.info("Step 11: Verifying PDP for Product 1");
        Assert.assertTrue(fdaPdpPage.isDisplayed(), "PDP not displayed for SKU 1: " + SKU_1);
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(), "Add to cart button not enabled for SKU 1");
        Assert.assertEquals(fdaPdpPage.getQuantity(), "1", "SKU 1 PDP quantity should be 1");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("Step 12: Adding Product 1 to cart");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product 1 added to cart");

        // Navigate home before searching second product
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ---- Product 2 (SKU_2) ----
        LoggerUtility.info("Step 13: Searching SKU 2: " + SKU_2);
        fdaHomePage.enterSearchQuery(SKU_2);
        fdaHomePage.pressSearchEnter();

        LoggerUtility.info("Step 14: Verifying PDP for Product 2");
        Assert.assertTrue(fdaPdpPage.isDisplayed(), "PDP not displayed for SKU 2: " + SKU_2);
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(), "Add to cart button not enabled for SKU 2");
        Assert.assertEquals(fdaPdpPage.getQuantity(), "1", "SKU 2 PDP quantity should be 1");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("Step 15: Adding Product 2 to cart");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product 2 added to cart");

        // ---- Cart validation — 2 products ----
        LoggerUtility.info("Step 16: Opening cart to verify 2 products");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        int itemCount = fdaCartPage.getItemCount();
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
        LoggerUtility.info("Step 17: Proceeding to payment");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();
        LoggerUtility.info("Step 18: Clicking Siguiente on shipping page");
        fdaPaymentPage.clickNextButton();

        LoggerUtility.info("Step 19: Selecting Credit Card");
        fdaPaymentPage.selectCreditCardOption();
        LoggerUtility.info("Step 19a: Entering card number");
        fdaPaymentPage.enterCardNumber(config.getFdaCardNumber());
        LoggerUtility.info("Step 19b: Entering expiry date");
        fdaPaymentPage.enterExpiry(config.getFdaCardExpiry());
        LoggerUtility.info("Step 19c: Entering CVV");
        fdaPaymentPage.enterCvv(config.getFdaCardCvv());

        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Step 20: Completar pago button text: " + payBtnText);
        Assert.assertTrue(
                payBtnText.contains("Completar pago") || payBtnText.contains("MXN") || payBtnText.contains("$"),
                "Pay button should show amount. Actual: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 21: Completing payment");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();

        LoggerUtility.info("Step 22: Verifying success page");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(), "Order success page not displayed after payment");
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 23: Order ID = " + orderId);
        Assert.assertFalse(orderId.isEmpty(), "Order ID should be present on success page");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 24: Navigating to Order History");
        fdaHomePage.clickProfileIcon();
        fdaHomePage.clickMyOrdersLink();
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(), "Order history page not displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
                "Order ID " + orderId + " not found in order history");
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 25-26: FDA order status: " + fdaStatus);
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
                "FDA order status should be 'Creada' or 'Pendiente'. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 2: MIRAKL — Login, Verify 2 Shipments (WEB-A + WEB-B), Accept Both
        // ============================================================

        // Switch to Mirakl tab — session active from suite setup
        LoggerUtility.info("Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 29: Navigating to All Orders in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();

        String miraklSearchTerm = orderId + "WEB";
        String shipmentRefA     = orderId + "WEB-A";
        String shipmentRefB     = orderId + "WEB-B";

        LoggerUtility.info("Step 30: Searching Mirakl for order: " + miraklSearchTerm);
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

        // Verify both WEB-A and WEB-B shipments are present
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefA),
                "Mirakl shipment WEB-A not found: " + shipmentRefA);
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefB),
                "Mirakl shipment WEB-B not found: " + shipmentRefB);
        LoggerUtility.info("Two shipments verified — WEB-A and WEB-B present in Mirakl");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ---- Accept WEB-A ----
        LoggerUtility.info("Step 31: Opening shipment WEB-A");
        miraklOrderDetailPage.clickOrderInList(shipmentRefA);
        String statusA = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-A status before accept: " + statusA);
        Assert.assertEquals(statusA, "Pending acceptance",
                "WEB-A should be 'Pending acceptance'. Actual: " + statusA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 32: Accepting shipment WEB-A");
        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        statusA = miraklOrderDetailPage.getOrderStatus();
        Assert.assertEquals(statusA, "Awaiting shipment",
                "WEB-A should be 'Awaiting shipment' after accept. Actual: " + statusA);
        urlShipmentA = driver.getCurrentUrl();
        LoggerUtility.info("WEB-A accepted. URL captured: " + urlShipmentA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Navigate back to All Orders list to open WEB-B
        LoggerUtility.info("Navigating back to All Orders to open WEB-B");
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        miraklOrdersPage.searchOrder(miraklSearchTerm);

        // ---- Accept WEB-B ----
        LoggerUtility.info("Step 33: Opening shipment WEB-B");
        miraklOrderDetailPage.clickOrderInList(shipmentRefB);
        String statusB = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-B status before accept: " + statusB);
        Assert.assertEquals(statusB, "Pending acceptance",
                "WEB-B should be 'Pending acceptance'. Actual: " + statusB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 34: Accepting shipment WEB-B");
        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        statusB = miraklOrderDetailPage.getOrderStatus();
        Assert.assertEquals(statusB, "Awaiting shipment",
                "WEB-B should be 'Awaiting shipment' after accept. Actual: " + statusB);
        urlShipmentB = driver.getCurrentUrl();
        LoggerUtility.info("WEB-B accepted. URL captured: " + urlShipmentB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: KIBO API — Auth + Collect Shipment Data for ALL Shipments
        // ============================================================

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
                        LoggerUtility.info("Shipment 3PL data not ready — tplShipmentId='"
                                + info.tplShipmentId + "' deliveryPartner='" + info.deliveryPartner + "'");
                    } else {
                        ready.add(info);
                    }
                }

                if (allPopulated && ready.size() >= 2) {
                    // Assign WEB-A / WEB-B by index — first Kibo shipment maps to WEB-A
                    ready.get(0).shipmentRef     = shipmentRefA;
                    ready.get(0).miraklDetailUrl = urlShipmentA;
                    ready.get(1).shipmentRef     = shipmentRefB;
                    ready.get(1).miraklDetailUrl = urlShipmentB;
                    shipments.addAll(ready);
                    allShipmentsReady = true;
                    LoggerUtility.info("All " + shipments.size() + " shipments have 3PL data — attempt " + attempt);
                    break;
                }
            } else {
                LoggerUtility.info("Kibo returned fewer than 2 shipments — waiting 30s (attempt " + attempt + ")");
            }

            if (attempt < 20) Thread.sleep(30_000);
        }

        Assert.assertTrue(allShipmentsReady,
                "Not all shipment 3PL data populated within 10 minutes");
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

        for (ShipmentInfo info : shipments) {
            LoggerUtility.info("========== Processing Shipment: " + info.shipmentRef + " ==========");
            LoggerUtility.info("Delivery Partner: " + info.deliveryPartner);

            // Navigate to this shipment's Mirakl detail page
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

        LoggerUtility.info("INFO  TC_FBO_002 Test Passed — both shipments fully received");
    }

    // ----------------------------------------------------------------
    // Envioclick flow for a single shipment
    // ----------------------------------------------------------------
    private void runEnvioclickFlow(ShipmentInfo info) {
        LoggerUtility.info("Step 63: Calling Envioclick API — En tránsito for: " + info.shipmentRef);
        Response enTransito = ApiUtility.postEnvioclickEnTransito(
                info.tplShipmentId, info.trackingNumber, orderId, info.carrierName);
        Assert.assertEquals(enTransito.getStatusCode(), 200,
                "Envioclick En tránsito should return 200 for " + info.shipmentRef
                + ". Actual: " + enTransito.getStatusCode());
        LoggerUtility.info("API Response = " + enTransito.getStatusCode());

        LoggerUtility.info("Step 64: Refreshing Mirakl — checking Shipped status for: " + info.shipmentRef);
        String status = waitForMiraklStatusOneOf(new String[]{"Shipped", "3PL delivery"}, 6);
        LoggerUtility.info("Mirakl status after En tránsito [" + info.shipmentRef + "]: " + status);
        Assert.assertTrue(status.equals("Shipped") || status.equals("3PL delivery"),
                "Mirakl status should be 'Shipped' or '3PL delivery' after En tránsito ["
                + info.shipmentRef + "]. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 66: Calling Envioclick API — Entregado for: " + info.shipmentRef);
        Response entregado = ApiUtility.postEnvioclickEntregado(
                info.tplShipmentId, info.trackingNumber, orderId, info.carrierName);
        Assert.assertEquals(entregado.getStatusCode(), 200,
                "Envioclick Entregado should return 200 for " + info.shipmentRef
                + ". Actual: " + entregado.getStatusCode());
        LoggerUtility.info("API Response = " + entregado.getStatusCode());

        LoggerUtility.info("Step 67: Refreshing Mirakl — checking Received status for: " + info.shipmentRef);
        status = waitForMiraklStatus("Received", 6);
        LoggerUtility.info("Mirakl status after Entregado [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Received",
                "Mirakl status should be 'Received' after Entregado ["
                + info.shipmentRef + "]. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        LoggerUtility.info("Envioclick flow complete — " + info.shipmentRef + " = Received");
    }

    // ----------------------------------------------------------------
    // Skydropx flow for a single shipment
    // ----------------------------------------------------------------
    private void runSkydropxFlow(ShipmentInfo info) {
        LoggerUtility.info("Step 63: Calling Skydropx API — Picked_up for: " + info.shipmentRef);
        Response pickedUp = ApiUtility.postSkydropxPickedUp(info.tplShipmentId);
        Assert.assertEquals(pickedUp.getStatusCode(), 200,
                "Skydropx Picked_up should return 200 for " + info.shipmentRef
                + ". Actual: " + pickedUp.getStatusCode());
        LoggerUtility.info("API Response = " + pickedUp.getStatusCode());

        LoggerUtility.info("Step 64: Refreshing Mirakl — checking Shipped status for: " + info.shipmentRef);
        String status = waitForMiraklStatus("Shipped", 6);
        LoggerUtility.info("Mirakl status after Picked_up [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Shipped",
                "Mirakl status should be 'Shipped' after Skydropx Picked_up ["
                + info.shipmentRef + "]. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 66: Calling Skydropx API — Delivered for: " + info.shipmentRef);
        Response delivered = ApiUtility.postSkydropxDelivered(info.tplShipmentId);
        Assert.assertEquals(delivered.getStatusCode(), 200,
                "Skydropx Delivered should return 200 for " + info.shipmentRef
                + ". Actual: " + delivered.getStatusCode());
        LoggerUtility.info("API Response = " + delivered.getStatusCode());

        LoggerUtility.info("Step 67: Refreshing Mirakl — checking Received status for: " + info.shipmentRef);
        status = waitForMiraklStatus("Received", 6);
        LoggerUtility.info("Mirakl status after Delivered [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Received",
                "Mirakl status should be 'Received' after Skydropx Delivered ["
                + info.shipmentRef + "]. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
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
    // Extract named custom field from a Kibo shipment item map.
    // Searches: item.data map, then item.packages[*].data maps.
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
