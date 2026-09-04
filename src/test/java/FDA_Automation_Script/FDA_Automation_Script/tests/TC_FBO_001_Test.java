package FDA_Automation_Script.FDA_Automation_Script.tests;

import FDA_Automation_Script.FDA_Automation_Script.base.BaseClass;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.*;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklLoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklOrderDetailPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklOrdersPage;
import FDA_Automation_Script.FDA_Automation_Script.utils.ApiUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import io.restassured.response.Response;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TC_FBO_001_Test extends BaseClass {

    private static final String TC_NAME = "TC_FBO_001";

    // --- Page Objects ---
    private FDAHomePage fdaHomePage;
    private FDALoginPage fdaLoginPage;
    private FDAPDPPage fdaPdpPage;
    private FDACartPage fdaCartPage;
    private FDAPaymentPage fdaPaymentPage;
    private FDASuccessPage fdaSuccessPage;
    private FDAOrderHistoryPage fdaOrderHistoryPage;
    private MiraklLoginPage miraklLoginPage;
    private MiraklOrdersPage miraklOrdersPage;
    private MiraklOrderDetailPage miraklOrderDetailPage;

    // --- Dynamic Test Data (captured once, reused everywhere) ---
    private String orderId;
    private String orderTotal;
    private String tplShipmentId;
    private String carrierName;
    private String trackingNumber;
    private String deliveryPartner;

    private static final By MIRAKL_ORDER_STATUS_LOCATOR = By.xpath("(//span[contains(@class,'_6e782__sc-s4ukbq-0') and not(ancestor::a)])[1]");

    @BeforeClass
    public void initPageObjects() {
        fdaHomePage          = new FDAHomePage(driver);
        fdaLoginPage         = new FDALoginPage(driver);
        fdaPdpPage           = new FDAPDPPage(driver);
        fdaCartPage          = new FDACartPage(driver);
        fdaPaymentPage       = new FDAPaymentPage(driver);
        fdaSuccessPage       = new FDASuccessPage(driver);
        fdaOrderHistoryPage  = new FDAOrderHistoryPage(driver);
        miraklLoginPage      = new MiraklLoginPage(driver);
        miraklOrdersPage     = new MiraklOrdersPage(driver);
        miraklOrderDetailPage = new MiraklOrderDetailPage(driver);
        LoggerUtility.info("All page objects initialized for TC_FBO_001");
    }

    @Test(testName = TC_NAME,
          description = "Verify 3P seller order placement and full fulfilment lifecycle: FDA -> Mirakl -> Shipment -> Delivery")
    public void tc_fbo_001_place_order_3p_seller_full_fulfillment() throws InterruptedException {

        // ============================================================
        // PHASE 1: FDA — Login, Search, PDP, Cart, Payment, Order
        // ============================================================

        // Suite @BeforeSuite already logged in to FDA. Ensure focus is on the FDA tab.
        switchToFDATab();
        LoggerUtility.info("TC_FBO_001: Starting — FDA and Mirakl sessions active from suite setup");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test cleanup: remove any cart items left over from previous runs
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // Step 10-12: Search by SKU
        LoggerUtility.info("Step 10: Clicking search field");
        fdaHomePage.enterSearchQuery(config.getFdaSku());
        LoggerUtility.info("Step 11-12: Entering SKU and pressing Enter");
        fdaHomePage.pressSearchEnter();

        // Step 13-15: PDP validations
        LoggerUtility.info("Step 13: Verifying Product Details Page is displayed");
        Assert.assertTrue(fdaPdpPage.isDisplayed(), "PDP not displayed after search for SKU: " + config.getFdaSku());
        LoggerUtility.info("Step 14: Verifying Agregar al carrito button is enabled");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(), "Add to cart button is not enabled on PDP");
        LoggerUtility.info("Step 15: Verifying product quantity is 1");
        Assert.assertEquals(fdaPdpPage.getQuantity(), "1", "Product quantity on PDP should be 1");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 16: Add to cart
        LoggerUtility.info("Step 16: Clicking Agregar al carrito");
        fdaPdpPage.clickAddToCart();

        // Step 17-20: Cart page
        LoggerUtility.info("Step 17: Navigating to cart page");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());
        String cartProductName = fdaCartPage.getProductName();
        LoggerUtility.info("Step 18: Cart product name: " + cartProductName);
        Assert.assertFalse(cartProductName.isEmpty(), "Product name should be displayed in cart");
        String cartQty = fdaCartPage.getQuantity();
        LoggerUtility.info("Step 19: Cart quantity: " + cartQty);
        Assert.assertEquals(cartQty, "1", "Cart quantity should be 1");
        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 20: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(), "Order total should be displayed in cart");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 21-22: Proceed to payment → shipping page
        LoggerUtility.info("Step 21: Clicking Proceed to payment");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();
        LoggerUtility.info("Step 22: Clicking Siguiente on shipping page");
        fdaPaymentPage.clickNextButton();

        // Step 23-29: Payment page
        LoggerUtility.info("Step 23: Selecting Credit/Debit Card Payment");
        fdaPaymentPage.selectCreditCardOption();
        LoggerUtility.info("Step 24-25: Entering card number");
        fdaPaymentPage.enterCardNumber(config.getFdaCardNumber());
        LoggerUtility.info("Step 26-27: Entering expiration date");
        fdaPaymentPage.enterExpiry(config.getFdaCardExpiry());
        LoggerUtility.info("Step 27-28: Entering security code");
        fdaPaymentPage.enterCvv(config.getFdaCardCvv());
        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Step 29: Completar pago button text: " + payBtnText);
        Assert.assertTrue(payBtnText.contains("Completar pago") || payBtnText.contains("MXN")
                || payBtnText.contains("Pago de contado") || payBtnText.contains("$"),
                "Completar pago button should display order total. Actual text: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 30-32: Complete payment → success page → get order ID
        LoggerUtility.info("Step 30: Clicking Completar pago button");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();
        LoggerUtility.info("Step 31: Verifying success page");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(), "Order success page not displayed after payment");
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 32: Order ID = " + orderId);
        Assert.assertFalse(orderId.isEmpty(), "Order ID should be present on success page");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 33-37: Order history
        LoggerUtility.info("Step 33: Clicking Mi cuenta profile icon");
        fdaHomePage.clickProfileIcon();
        LoggerUtility.info("Step 34: Clicking Mis pedidos link");
        fdaHomePage.clickMyOrdersLink();
        LoggerUtility.info("Step 35: Verifying Mis pedidos page");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(), "Mis pedidos page not displayed");
        LoggerUtility.info("Step 36: Verifying order ID in history: " + orderId);
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
                "Order ID " + orderId + " not found in order history");
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 37: FDA order status: " + fdaStatus);
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
                "Order status should be 'Creada' or 'Pendiente' in FDA order history. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 2: MIRAKL — Login, Search Order, Pending Acceptance, Accept
        // ============================================================

        // Switch to Mirakl tab — session active from suite setup
        LoggerUtility.info("Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 46-47: Navigate to All Orders
        LoggerUtility.info("Step 46: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 47: Clicking All Orders");
        miraklOrdersPage.clickAllOrders();

        // Step 48-50: Search for order in Mirakl — retry every 60s up to 5 minutes for sync delay
        String miraklSearchTerm = orderId + "WEB";
        LoggerUtility.info("Step 48-49: Searching for order in Mirakl: " + miraklSearchTerm);
        boolean orderFoundInMirakl = false;
        for (int attempt = 1; attempt <= 5; attempt++) {
            LoggerUtility.info("Mirakl search attempt " + attempt + "/5 for: " + miraklSearchTerm);
            miraklOrdersPage.searchOrder(miraklSearchTerm);
            if (miraklOrdersPage.hasSearchResults(miraklSearchTerm)) {
                orderFoundInMirakl = true;
                LoggerUtility.info("Order found in Mirakl on attempt " + attempt);
                break;
            }
            LoggerUtility.info("Order not in Mirakl yet (attempt " + attempt + "/5)");
            if (attempt < 5) {
                LoggerUtility.info("Waiting 60 seconds before next search attempt...");
                Thread.sleep(60_000);
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(orderFoundInMirakl, "Order " + miraklSearchTerm + " did not appear in Mirakl within 5 minutes");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 51-53: Click into detail → verify total and status
        LoggerUtility.info("Step 51: Clicking on order in Mirakl list");
        miraklOrderDetailPage.clickOrderInList(orderId);
        String miraklTotal = miraklOrderDetailPage.getOrderTotal();
        LoggerUtility.info("Step 52: Mirakl order total: " + miraklTotal + " | FDA order total: " + orderTotal);
        Assert.assertFalse(miraklTotal.isEmpty(), "Mirakl order total should be displayed");
        String miraklStatus = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("Step 53: Mirakl Status = " + miraklStatus);
        Assert.assertEquals(miraklStatus, "Pending acceptance",
                "Mirakl order status should be 'Pending acceptance'. Actual: " + miraklStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 54: Accept order → Awaiting shipment
        LoggerUtility.info("Step 54: Accepting the order in Mirakl");
        miraklOrderDetailPage.clickAcceptButton();
        LoggerUtility.info("Step 54a: Refreshing Mirakl order details page after acceptance");
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        miraklStatus = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("Step 55: Mirakl Status = " + miraklStatus);
        Assert.assertEquals(miraklStatus, "Awaiting shipment",
                "Mirakl order status should be 'Awaiting shipment' after acceptance. Actual: " + miraklStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: KIBO API — Authenticate + Get Shipment Details
        // ============================================================

        LoggerUtility.info("========== Order Accepted ==========");

        // Step Kibo-1: Kibo Authentication — generate access token
        LoggerUtility.info("========== Kibo Authentication ==========");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(),
                "Kibo authentication failed — access token is empty");

        // Step Kibo-2: Get All Orders — find Kibo order ID matching FDA externalId
        String externalId = orderId + "WEB";
        LoggerUtility.info("========== Get Orders ==========");
        LoggerUtility.info("Searching externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(),
                "Kibo Order ID not found for externalId: " + externalId);
        LoggerUtility.info("Kibo Order ID : " + kiboOrderId);

        // Step Kibo-3: Get Shipment Details — poll until 3PL connector populates data
        // Uses GET /api/commerce/shipments?filter=orderId=={kiboOrderId} (same as Postman collection)
        LoggerUtility.info("========== Get Shipment Details ==========");
        LoggerUtility.info("Polling Kibo Get Shipment Details (30s interval, up to 10 min)...");
        boolean shipmentDataReady = false;
        for (int attempt = 1; attempt <= 20; attempt++) {
            LoggerUtility.info("Kibo shipment data poll attempt " + attempt + "/20");
            Response shipmentsResp = ApiUtility.kiboGetShipmentsByOrderId(kiboToken, kiboOrderId);
            Assert.assertEquals(shipmentsResp.getStatusCode(), 200,
                    "Kibo Get Shipment Details should return 200. Actual: " + shipmentsResp.getStatusCode());

            // Handle both standard Kibo list format (items[]) and HAL format (_embedded.shipments)
            java.util.List<java.util.Map<String, Object>> shipmentList =
                    shipmentsResp.jsonPath().getList("items");
            if (shipmentList == null || shipmentList.isEmpty()) {
                shipmentList = shipmentsResp.jsonPath().getList("_embedded.shipments");
            }

            if (shipmentList != null && !shipmentList.isEmpty()) {
                java.util.Map<String, Object> shipment = shipmentList.get(0);
                deliveryPartner = extractCustomField(shipment, "deliveryPartner");
                tplShipmentId   = extractCustomField(shipment, "3pl_shipmentId");
                carrierName     = extractCustomField(shipment, "carrierName");
                trackingNumber  = extractCustomField(shipment, "tracking_number");

                if (!deliveryPartner.isEmpty() && !tplShipmentId.isEmpty()) {
                    shipmentDataReady = true;
                    LoggerUtility.info("Shipment 3PL data ready on attempt " + attempt);
                    break;
                }
            }
            LoggerUtility.info("3PL data not ready — deliveryPartner='" + deliveryPartner
                    + "', tplShipmentId='" + tplShipmentId + "'. Waiting 30s...");
            if (attempt < 20) Thread.sleep(30_000);
        }
        Assert.assertTrue(shipmentDataReady,
                "3PL data (deliveryPartner, 3pl_shipmentId) did not appear in Kibo shipment within 10 minutes");

        LoggerUtility.info("Delivery Partner : " + deliveryPartner);
        LoggerUtility.info("Shipment ID      : " + tplShipmentId);
        LoggerUtility.info("Carrier          : " + carrierName);
        LoggerUtility.info("Tracking Number  : " + trackingNumber);

        Assert.assertFalse(carrierName.isEmpty(),
                "carrierName missing from Kibo shipment data");
        if (deliveryPartner.toLowerCase().contains("envioclick")) {
            Assert.assertFalse(trackingNumber.isEmpty(),
                    "tracking_number required for Envioclick flow but missing from Kibo shipment data");
        }

        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("Shipment Generated — Carrier = " + carrierName + " | Shipment ID = " + tplShipmentId);

        // ============================================================
        // PHASE 4: Webhook + Status Validation (Envioclick OR Skydropx)
        // ============================================================

        LoggerUtility.info("Step 62: Delivery partner is: " + deliveryPartner);

        if (deliveryPartner.toLowerCase().contains("envioclick")) {
            runEnvioclickFlow();
        } else if (deliveryPartner.toLowerCase().contains("skydropx")) {
            runSkydropxFlow();
        } else {
            Assert.fail("Unknown delivery partner: " + deliveryPartner + ". Expected 'Envioclick' or 'Skydropx'.");
        }

        LoggerUtility.info("INFO  Test Passed");
    }

    // ----------------------------------------------------------------
    // Envioclick flow
    // ----------------------------------------------------------------
    private void runEnvioclickFlow() {
        // En tránsito
        LoggerUtility.info("Step 63: Calling Envioclick API — En tránsito");
        Response enTransitoResponse = ApiUtility.postEnvioclickEnTransito(tplShipmentId, trackingNumber, orderId, carrierName);
        Assert.assertEquals(enTransitoResponse.getStatusCode(), 200,
                "Envioclick En tránsito API should return 200. Actual: " + enTransitoResponse.getStatusCode());
        LoggerUtility.info("API Response = " + enTransitoResponse.getStatusCode());

        // Refresh Mirakl → Shipped or "3PL delivery" (Envioclick may produce either)
        LoggerUtility.info("Step 64: Refreshing Mirakl order details page");
        String status = waitForMiraklStatusOneOf(new String[]{"Shipped", "3PL delivery"}, 6);
        LoggerUtility.info("Step 65: Mirakl Status = " + status);
        Assert.assertTrue(status.equals("Shipped") || status.equals("3PL delivery"),
                "Mirakl status should be 'Shipped' or '3PL delivery' after En tránsito. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Entregado
        LoggerUtility.info("Step 66: Calling Envioclick API — Entregado");
        Response entregadoResponse = ApiUtility.postEnvioclickEntregado(tplShipmentId, trackingNumber, orderId, carrierName);
        Assert.assertEquals(entregadoResponse.getStatusCode(), 200,
                "Envioclick Entregado API should return 200. Actual: " + entregadoResponse.getStatusCode());
        LoggerUtility.info("API Response = " + entregadoResponse.getStatusCode());

        // Refresh Mirakl → Received (retry up to 6 times for async processing)
        LoggerUtility.info("Step 67: Refreshing Mirakl order details page");
        status = waitForMiraklStatus("Received", 6);
        LoggerUtility.info("Step 68: Mirakl Status = " + status);
        Assert.assertEquals(status, "Received",
                "Mirakl status should be 'Received' after Entregado. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        LoggerUtility.info("Envioclick flow complete — Mirakl Status = Received");
    }

    // ----------------------------------------------------------------
    // Skydropx flow
    // ----------------------------------------------------------------
    private void runSkydropxFlow() {
        // Picked_up
        LoggerUtility.info("Step 63: Calling Skydropx API — Picked_up");
        Response pickedUpResponse = ApiUtility.postSkydropxPickedUp(tplShipmentId);
        Assert.assertEquals(pickedUpResponse.getStatusCode(), 200,
                "Skydropx Picked_up API should return 200. Actual: " + pickedUpResponse.getStatusCode());
        LoggerUtility.info("API Response = " + pickedUpResponse.getStatusCode());

        // Refresh Mirakl → Shipped (retry up to 6 times for async processing)
        LoggerUtility.info("Step 64: Refreshing Mirakl order details page");
        String status = waitForMiraklStatus("Shipped", 6);
        LoggerUtility.info("Step 65: Mirakl Status = " + status);
        Assert.assertEquals(status, "Shipped",
                "Mirakl status should be 'Shipped' after Skydropx Picked_up. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Delivered
        LoggerUtility.info("Step 66: Calling Skydropx API — Delivered");
        Response deliveredResponse = ApiUtility.postSkydropxDelivered(tplShipmentId);
        Assert.assertEquals(deliveredResponse.getStatusCode(), 200,
                "Skydropx Delivered API should return 200. Actual: " + deliveredResponse.getStatusCode());
        LoggerUtility.info("API Response = " + deliveredResponse.getStatusCode());

        // Refresh Mirakl → Received (retry up to 6 times for async processing)
        LoggerUtility.info("Step 67: Refreshing Mirakl order details page");
        status = waitForMiraklStatus("Received", 6);
        LoggerUtility.info("Step 68: Mirakl Status = " + status);
        Assert.assertEquals(status, "Received",
                "Mirakl status should be 'Received' after Skydropx Delivered. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        LoggerUtility.info("Skydropx flow complete — Mirakl Status = Received");
    }

    // ----------------------------------------------------------------
    // Poll helper: refresh + read status up to maxRetries times
    // ----------------------------------------------------------------
    private String waitForMiraklStatus(String expected, int maxRetries) {
        String status = "";
        for (int i = 1; i <= maxRetries; i++) {
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            status = miraklOrderDetailPage.getOrderStatus();
            LoggerUtility.info("Mirakl status check attempt " + i + "/" + maxRetries + ": " + status);
            if (expected.equals(status)) break;
        }
        return status;
    }

    // Poll for any one of the expected statuses (stops on first match)
    private String waitForMiraklStatusOneOf(String[] expected, int maxRetries) {
        java.util.Set<String> expectedSet = new java.util.HashSet<>(java.util.Arrays.asList(expected));
        String status = "";
        for (int i = 1; i <= maxRetries; i++) {
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            status = miraklOrderDetailPage.getOrderStatus();
            LoggerUtility.info("Mirakl status check attempt " + i + "/" + maxRetries + ": " + status);
            if (expectedSet.contains(status)) break;
        }
        return status;
    }

    // ----------------------------------------------------------------
    // Extract a named custom field from a Kibo shipment item map.
    // Searches: item.data map, then item.packages[*].data maps.
    // ----------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private String extractCustomField(java.util.Map<String, Object> item, String fieldKey) {
        Object dataRaw = item.get("data");
        if (dataRaw instanceof java.util.Map) {
            Object val = ((java.util.Map<String, Object>) dataRaw).get(fieldKey);
            if (val != null && !val.toString().isBlank()) return val.toString().trim();
        }
        Object pkgsRaw = item.get("packages");
        if (pkgsRaw instanceof java.util.List) {
            for (Object pkg : (java.util.List<?>) pkgsRaw) {
                if (pkg instanceof java.util.Map) {
                    Object pkgData = ((java.util.Map<String, Object>) pkg).get("data");
                    if (pkgData instanceof java.util.Map) {
                        Object val = ((java.util.Map<String, Object>) pkgData).get(fieldKey);
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
        java.util.Set<String> handles = driver.getWindowHandles();
        LoggerUtility.info("Window handles after clickProceedToPayment: " + handles.size());
        if (!handles.contains(fdaTabHandle)) {
            fdaTabHandle = handles.iterator().next();
            driver.switchTo().window(fdaTabHandle);
            LoggerUtility.info("Original FDA window closed — switched to checkout window: " + fdaTabHandle);
        } else if (handles.size() > 1) {
            for (String h : handles) {
                if (!h.equals(fdaTabHandle) && !h.equals(miraklTabHandle)) {
                    driver.switchTo().window(h);
                    fdaTabHandle = h;
                    LoggerUtility.info("Checkout opened in new window — switched to: " + fdaTabHandle);
                    break;
                }
            }
        }
    }

}
// @AfterMethod navigateToHomePage() is inherited from BaseClass
