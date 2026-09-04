package FDA_Automation_Script.FDA_Automation_Script.tests;

import FDA_Automation_Script.FDA_Automation_Script.base.BaseClass;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDACartPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAHomePage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDALoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAOrderHistoryPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAPaymentPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAPDPPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDASuccessPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklOrderDetailPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklOrdersPage;
import FDA_Automation_Script.FDA_Automation_Script.utils.ApiUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import io.restassured.response.Response;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TC_FBO_003_Test extends BaseClass {

    private static final String TC_NAME        = "TC_FBO_003";

    // TC-specific test data (different FDA user and SKU from the suite default)
    private static final String TC_FDA_USER    = "mgowda@kognivera.com";
    private static final String TC_FDA_PASS    = "Mithun@12345";
    private static final String TC_SKU         = "78078094274";
    private static final String TC_CARD_NUMBER = "5454545454545454";
    private static final String TC_CARD_EXPIRY = "03/30";
    private static final String TC_CARD_CVV    = "737";
    private static final int    TC_QUANTITY    = 2;

    // --- Page Objects ---
    private FDAHomePage           fdaHomePage;
    private FDALoginPage          fdaLoginPage;
    private FDAPDPPage            fdaPdpPage;
    private FDACartPage           fdaCartPage;
    private FDAPaymentPage        fdaPaymentPage;
    private FDASuccessPage        fdaSuccessPage;
    private FDAOrderHistoryPage   fdaOrderHistoryPage;
    private MiraklOrdersPage      miraklOrdersPage;
    private MiraklOrderDetailPage miraklOrderDetailPage;

    // --- Dynamic Test Data (captured once, reused everywhere) ---
    private String orderId;
    private String orderTotal;
    private String tplShipmentId;
    private String carrierName;
    private String trackingNumber;
    private String deliveryPartner;

    private static final By MIRAKL_ORDER_STATUS_LOCATOR =
        By.xpath("(//span[contains(@class,'_6e782__sc-s4ukbq-0') and not(ancestor::a)])[1]");

    @BeforeClass
    public void initAndLogin() {
        fdaHomePage          = new FDAHomePage(driver);
        fdaLoginPage         = new FDALoginPage(driver);
        fdaPdpPage           = new FDAPDPPage(driver);
        fdaCartPage          = new FDACartPage(driver);
        fdaPaymentPage       = new FDAPaymentPage(driver);
        fdaSuccessPage       = new FDASuccessPage(driver);
        fdaOrderHistoryPage  = new FDAOrderHistoryPage(driver);
        miraklOrdersPage     = new MiraklOrdersPage(driver);
        miraklOrderDetailPage = new MiraklOrderDetailPage(driver);
        LoggerUtility.info("TC_FBO_003: All page objects initialized");

        // TC_FBO_003 uses a different FDA account — logout the current session and re-login
        LoggerUtility.info("TC_FBO_003: Switching FDA session to: " + TC_FDA_USER);
        switchToFDATab();
        fdaHomePage.navigateTo(config.getFdaUrl());
        fdaHomePage.logout();
        fdaHomePage.navigateTo(config.getFdaUrl());
        fdaHomePage.clickProfileIcon();
        fdaHomePage.clickLoginLink();
        fdaLoginPage.login(TC_FDA_USER, TC_FDA_PASS);
        LoggerUtility.info("TC_FBO_003: FDA login as " + TC_FDA_USER + " successful");
    }

    @AfterClass(alwaysRun = true)
    public void restoreOriginalFdaSession() {
        // Re-login as the suite-default FDA user so subsequent tests are unaffected
        LoggerUtility.info("TC_FBO_003 @AfterClass: Restoring original FDA session");
        try {
            switchToFDATab();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.logout();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.clickProfileIcon();
            fdaHomePage.clickLoginLink();
            fdaLoginPage.login(config.getFdaUsername(), config.getFdaPassword());
            LoggerUtility.info("TC_FBO_003 @AfterClass: FDA session restored as " + config.getFdaUsername());
        } catch (Exception e) {
            LoggerUtility.error("TC_FBO_003 @AfterClass: Failed to restore FDA session: " + e.getMessage());
        }
    }

    @Test(testName = TC_NAME,
          description = "Verify 3P seller order: 1 product quantity 2 → full fulfillment lifecycle (FDA → Mirakl → Kibo → Delivery)")
    public void tc_fbo_003_single_product_qty2_full_fulfillment() throws InterruptedException {

        LoggerUtility.info("TC_FBO_003 execution started");

        // ============================================================
        // PHASE 1: FDA — Search, PDP, Qty=2, Cart, Payment, Order
        // ============================================================

        // Step 1: FDA Home Page
        switchToFDATab();
        LoggerUtility.info("Step 1: Verifying FDA Home Page");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test: clear leftover cart items
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // Steps 2-4: Search SKU
        LoggerUtility.info("Step 2: Clicking search field — ¿Qué estás buscando?");
        LoggerUtility.info("Step 3: Entering SKU: " + TC_SKU);
        fdaHomePage.enterSearchQuery(TC_SKU);
        LoggerUtility.info("Step 4: Pressing Enter to search");
        fdaHomePage.pressSearchEnter();
        LoggerUtility.info("SKU " + TC_SKU + " searched successfully");

        // Steps 5-6: PDP validation
        LoggerUtility.info("Step 5: Verifying Product Details Page is displayed");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "PDP not displayed after searching SKU: " + TC_SKU);

        LoggerUtility.info("Step 6: Verifying Agregar al carrito button is enabled");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "Add to cart button is not enabled on PDP for SKU: " + TC_SKU);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 7: Increase quantity from 1 to 2
        LoggerUtility.info("Step 7: Clicking plus button to increase quantity from 1 to " + TC_QUANTITY);
        fdaPdpPage.increaseQuantity();

        // Step 8: Verify quantity is 2
        String pdpQty = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 8: Product quantity after increment: " + pdpQty);
        Assert.assertEquals(pdpQty, String.valueOf(TC_QUANTITY),
            "Product quantity on PDP should be " + TC_QUANTITY + " after clicking plus. Actual: " + pdpQty);
        LoggerUtility.info("Product quantity changed from 1 to " + TC_QUANTITY);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 9: Add to cart
        LoggerUtility.info("Step 9: Clicking Agregar al carrito");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product added to cart with quantity " + TC_QUANTITY);

        // Steps 10-14: Cart page
        LoggerUtility.info("Step 10: Opening Mi carrito (cart page)");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        // Step 11: Verify exactly 1 product in cart
        int itemCount = fdaCartPage.getItemCount();
        LoggerUtility.info("Step 11: Cart item count: " + itemCount);
        Assert.assertEquals(itemCount, 1,
            "Cart should contain exactly 1 product (qty=" + TC_QUANTITY + "). Actual item count: " + itemCount);
        LoggerUtility.info("Cart contains 1 product with quantity " + TC_QUANTITY);

        // Step 12: Verify product name is present
        String cartProductName = fdaCartPage.getProductName();
        LoggerUtility.info("Step 12: Cart product name: " + cartProductName);
        Assert.assertFalse(cartProductName.isEmpty(),
            "Product name should be displayed in cart");

        // Step 13: Verify quantity is 2
        String cartQty = fdaCartPage.getQuantity();
        LoggerUtility.info("Step 13: Cart product quantity: " + cartQty);
        Assert.assertEquals(cartQty, String.valueOf(TC_QUANTITY),
            "Cart product quantity should be " + TC_QUANTITY + ". Actual: " + cartQty);

        // Step 14: Verify order total contains expected amount
        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 14: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(),
            "Order total should be displayed in cart");
        Assert.assertTrue(orderTotal.contains("800"),
            "Order total should reflect MXN$800.00. Actual: " + orderTotal);
        LoggerUtility.info("Order total = " + orderTotal);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 15: Proceed to payment
        LoggerUtility.info("Step 15: Clicking Proceed to payment");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();

        // Step 16: Siguiente on shipping page
        LoggerUtility.info("Step 16: Clicking Siguiente on shipping page");
        fdaPaymentPage.clickNextButton();

        // Step 17: Select credit card
        LoggerUtility.info("Step 17: Selecting Pago con Tarjeta Crédito/Débito");
        fdaPaymentPage.selectCreditCardOption();

        // Steps 18-19: Card number
        LoggerUtility.info("Step 18: Clicking Número de tarjeta field");
        LoggerUtility.info("Step 19: Entering card number");
        fdaPaymentPage.enterCardNumber(TC_CARD_NUMBER);

        // Steps 20-21: Expiry
        LoggerUtility.info("Step 20: Clicking Fecha de expiración field");
        LoggerUtility.info("Step 21: Entering expiry: " + TC_CARD_EXPIRY);
        fdaPaymentPage.enterExpiry(TC_CARD_EXPIRY);

        // Steps 22-23: CVV
        LoggerUtility.info("Step 22: Clicking Código de seguridad field");
        LoggerUtility.info("Step 23: Entering CVV");
        fdaPaymentPage.enterCvv(TC_CARD_CVV);

        // Step 24: Verify Completar pago button shows total
        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Step 24: Completar pago button text: " + payBtnText);
        Assert.assertTrue(
            payBtnText.contains("Completar pago") || payBtnText.contains("MXN") || payBtnText.contains("$"),
            "Completar pago button should display the payment total. Actual: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 25: Complete payment
        LoggerUtility.info("Step 25: Clicking Completar pago button");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();

        // Step 26: Verify success page
        LoggerUtility.info("Step 26: Verifying order success page");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(),
            "Order success page not displayed after payment");

        // Step 27: Capture order ID
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 27: Order ID = " + orderId);
        Assert.assertFalse(orderId.isEmpty(),
            "Order ID should be present on success page");
        LoggerUtility.info("Order created successfully — Order ID = " + orderId);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 28: Click Mi cuenta
        LoggerUtility.info("Step 28: Clicking Mi cuenta profile icon");
        fdaHomePage.clickProfileIcon();

        // Step 29: Click Mis pedidos
        LoggerUtility.info("Step 29: Clicking Mis pedidos link");
        fdaHomePage.clickMyOrdersLink();

        // Step 30: Verify order history page
        LoggerUtility.info("Step 30: Verifying Mis pedidos page is displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(),
            "Mis pedidos page not displayed after navigation");

        // Step 31: Verify order ID in history
        LoggerUtility.info("Step 31: Verifying Order ID " + orderId + " is present in order history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
            "Order ID " + orderId + " not found in order history");

        // Step 32: Verify order status
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 32: FDA order status: " + fdaStatus);
        Assert.assertTrue(
            fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
            "FDA order status should be 'Creada' or 'Pendiente'. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 2: MIRAKL — Find WEB-A Shipment, Accept
        // ============================================================

        // Step 33: Switch to Mirakl tab
        LoggerUtility.info("Step 33: Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 34-35: Navigate to All Orders
        LoggerUtility.info("Step 34: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 35: Clicking All Orders");
        miraklOrdersPage.clickAllOrders();

        // Steps 36-38: Search for order and verify WEB-A shipment
        String miraklSearchTerm = orderId + "WEB";
        String shipmentRef      = orderId + "WEB-A";

        LoggerUtility.info("Step 36: Clicking Mirakl search field");
        LoggerUtility.info("Step 37: Searching for order: " + miraklSearchTerm);
        boolean orderFoundInMirakl = false;
        for (int attempt = 1; attempt <= 5; attempt++) {
            LoggerUtility.info("Mirakl search attempt " + attempt + "/5 for: " + miraklSearchTerm);
            miraklOrdersPage.searchOrder(miraklSearchTerm);
            if (miraklOrdersPage.hasSearchResults(miraklSearchTerm)) {
                orderFoundInMirakl = true;
                LoggerUtility.info("Mirakl order found on attempt " + attempt);
                break;
            }
            LoggerUtility.info("Order not in Mirakl yet (attempt " + attempt + "/5)");
            if (attempt < 5) {
                LoggerUtility.info("Waiting 60 seconds before next attempt...");
                Thread.sleep(60_000);
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(orderFoundInMirakl,
            "Order " + miraklSearchTerm + " did not appear in Mirakl within 5 minutes");

        // Step 38: Verify exactly 1 shipment with reference WEB-A
        LoggerUtility.info("Step 38: Verifying WEB-A shipment exists — shipmentRef: " + shipmentRef);
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRef),
            "Expected shipment reference " + shipmentRef + " not found in Mirakl");
        LoggerUtility.info("WEB-A shipment identified: " + shipmentRef);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 39: Open order → verify Pending acceptance → Accept
        LoggerUtility.info("Step 39: Opening Mirakl order: " + shipmentRef);
        miraklOrderDetailPage.clickOrderInList(shipmentRef);

        String miraklStatus = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("Mirakl status before acceptance: " + miraklStatus);
        Assert.assertEquals(miraklStatus, "Pending acceptance",
            "Mirakl status should be 'Pending acceptance'. Actual: " + miraklStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 39: Accepting order in Mirakl");
        miraklOrderDetailPage.clickAcceptButton();
        // Step 40: Wait for status update using Fluent Wait via refreshAndWait
        LoggerUtility.info("Step 40: Refreshing Mirakl order detail — waiting for Awaiting shipment status");
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        miraklStatus = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("Mirakl status after acceptance: " + miraklStatus);
        Assert.assertEquals(miraklStatus, "Awaiting shipment",
            "Mirakl status should be 'Awaiting shipment' after acceptance. Actual: " + miraklStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: KIBO API — Authenticate + Get Shipment Details
        // ============================================================

        LoggerUtility.info("========== Order Accepted ==========");

        // Step 41: Kibo authentication
        LoggerUtility.info("Step 41: ========== Kibo Authentication ==========");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(),
            "Kibo access token should not be empty — authentication failed");
        LoggerUtility.info("Kibo authentication successful");

        // Step 42: Find Kibo order ID by externalId
        String externalId = orderId + "WEB";
        LoggerUtility.info("Step 42: ========== Find Kibo Order ID ==========");
        LoggerUtility.info("Searching externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(),
            "Kibo Order ID not found for externalId: " + externalId);
        LoggerUtility.info("Kibo Order ID: " + kiboOrderId);

        // Step 43: Poll Kibo shipments until 3PL data is populated
        LoggerUtility.info("Step 43: ========== Get Shipment Details ==========");
        LoggerUtility.info("Polling Kibo shipments (30s interval, up to 10 min)...");
        boolean shipmentDataReady = false;
        for (int attempt = 1; attempt <= 20; attempt++) {
            LoggerUtility.info("Kibo shipment poll attempt " + attempt + "/20");
            Response shipmentsResp = ApiUtility.kiboGetShipmentsByOrderId(kiboToken, kiboOrderId);
            Assert.assertEquals(shipmentsResp.getStatusCode(), 200,
                "Kibo Get Shipments should return 200. Actual: " + shipmentsResp.getStatusCode());

            List<Map<String, Object>> shipmentList = shipmentsResp.jsonPath().getList("items");
            if (shipmentList == null || shipmentList.isEmpty()) {
                shipmentList = shipmentsResp.jsonPath().getList("_embedded.shipments");
            }

            if (shipmentList != null && !shipmentList.isEmpty()) {
                Map<String, Object> shipment = shipmentList.get(0);
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
            "3PL data (deliveryPartner, 3pl_shipmentId) not populated in Kibo within 10 minutes");

        LoggerUtility.info("Delivery Partner  : " + deliveryPartner);
        LoggerUtility.info("3pl_shipmentId    : " + tplShipmentId);
        LoggerUtility.info("Carrier Name      : " + carrierName);
        LoggerUtility.info("Tracking Number   : " + trackingNumber);

        Assert.assertFalse(deliveryPartner.isEmpty(),
            "deliveryPartner missing from Kibo shipment data");
        Assert.assertFalse(tplShipmentId.isEmpty(),
            "3pl_shipmentId missing from Kibo shipment data");
        Assert.assertFalse(carrierName.isEmpty(),
            "carrierName missing from Kibo shipment data");
        if (deliveryPartner.toLowerCase().contains("envioclick")) {
            Assert.assertFalse(trackingNumber.isEmpty(),
                "tracking_number required for Envioclick flow but missing from Kibo shipment data");
        }

        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("Shipment generated — Carrier = " + carrierName + " | 3PL Shipment ID = " + tplShipmentId);

        // ============================================================
        // PHASE 4: Webhook + Status Validation (Envioclick OR Skydropx)
        // ============================================================

        LoggerUtility.info("Delivery partner is: " + deliveryPartner);

        if (deliveryPartner.toLowerCase().contains("envioclick")) {
            runEnvioclickFlow();
        } else if (deliveryPartner.toLowerCase().contains("skydropx")) {
            runSkydropxFlow();
        } else {
            Assert.fail("Unknown delivery partner: " + deliveryPartner
                + ". Expected 'Envioclick' or 'Skydropx'.");
        }

        LoggerUtility.info("TC_FBO_003 execution completed successfully");
    }

    // ----------------------------------------------------------------
    // Envioclick flow: En tránsito → Shipped | Entregado → Received
    // ----------------------------------------------------------------
    private void runEnvioclickFlow() {
        // Step 45: En tránsito
        LoggerUtility.info("Step 45: Calling Envioclick API — En tránsito");
        LoggerUtility.info("EnvioClick API Request — carrier: " + carrierName
            + " | idOrder (3pl_shipmentId): " + tplShipmentId
            + " | trackingCode: " + trackingNumber
            + " | myShipmentReference: " + orderId + "WEB");
        Response enTransitoResp = ApiUtility.postEnvioclickEnTransito(
            tplShipmentId, trackingNumber, orderId, carrierName);
        Assert.assertEquals(enTransitoResp.getStatusCode(), 200,
            "Envioclick En tránsito API should return 200. Actual: " + enTransitoResp.getStatusCode());
        LoggerUtility.info("EnvioClick API Response = " + enTransitoResp.getStatusCode());

        // Step 46: Refresh Mirakl → Shipped or 3PL delivery
        LoggerUtility.info("Step 46: Switching to Mirakl — refreshing to check Shipped status");
        switchToMiraklTab();
        String status = waitForMiraklStatusOneOf(new String[]{"Shipped", "3PL delivery"}, 60);
        LoggerUtility.info("Mirakl status after En tránsito: " + status);
        Assert.assertTrue(status.equals("Shipped") || status.equals("3PL delivery"),
            "Mirakl status should be 'Shipped' or '3PL delivery' after En tránsito. Actual: " + status);
        LoggerUtility.info("Mirakl status = " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 47: Entregado
        LoggerUtility.info("Step 47: Calling Envioclick API — Entregado");
        LoggerUtility.info("EnvioClick API Request — carrier: " + carrierName
            + " | idOrder (3pl_shipmentId): " + tplShipmentId
            + " | trackingCode: " + trackingNumber
            + " | myShipmentReference: " + orderId + "WEB");
        Response entregadoResp = ApiUtility.postEnvioclickEntregado(
            tplShipmentId, trackingNumber, orderId, carrierName);
        Assert.assertEquals(entregadoResp.getStatusCode(), 200,
            "Envioclick Entregado API should return 200. Actual: " + entregadoResp.getStatusCode());
        LoggerUtility.info("EnvioClick API Response = " + entregadoResp.getStatusCode());

        // Step 48: Refresh Mirakl → Received
        LoggerUtility.info("Step 48: Refreshing Mirakl — checking Received status");
        status = waitForMiraklStatus("Received", 60);
        LoggerUtility.info("Mirakl status after Entregado: " + status);
        Assert.assertEquals(status, "Received",
            "Mirakl status should be 'Received' after Entregado. Actual: " + status);
        LoggerUtility.info("Mirakl status = Received");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        LoggerUtility.info("Envioclick flow complete — Mirakl Status = Received");
    }

    // ----------------------------------------------------------------
    // Skydropx flow: Picked_up → Shipped | Delivered → Received
    // ----------------------------------------------------------------
    private void runSkydropxFlow() {
        // Step 49: Picked_up
        LoggerUtility.info("Step 49: Calling Skydropx API — Picked_up");
        LoggerUtility.info("Skydropx API Request — id (3pl_shipmentId): " + tplShipmentId);
        Response pickedUpResp = ApiUtility.postSkydropxPickedUp(tplShipmentId);
        Assert.assertEquals(pickedUpResp.getStatusCode(), 200,
            "Skydropx Picked_up API should return 200. Actual: " + pickedUpResp.getStatusCode());
        LoggerUtility.info("Skydropx API Response = " + pickedUpResp.getStatusCode());

        // Step 50: Refresh Mirakl → Shipped
        LoggerUtility.info("Step 50: Switching to Mirakl — refreshing to check Shipped status");
        switchToMiraklTab();
        String status = waitForMiraklStatus("Shipped", 60);
        LoggerUtility.info("Mirakl status after Picked_up: " + status);
        Assert.assertEquals(status, "Shipped",
            "Mirakl status should be 'Shipped' after Skydropx Picked_up. Actual: " + status);
        LoggerUtility.info("Mirakl status = Shipped");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 51: Delivered
        LoggerUtility.info("Step 51: Calling Skydropx API — Delivered");
        LoggerUtility.info("Skydropx API Request — id (3pl_shipmentId): " + tplShipmentId);
        Response deliveredResp = ApiUtility.postSkydropxDelivered(tplShipmentId);
        Assert.assertEquals(deliveredResp.getStatusCode(), 200,
            "Skydropx Delivered API should return 200. Actual: " + deliveredResp.getStatusCode());
        LoggerUtility.info("Skydropx API Response = " + deliveredResp.getStatusCode());

        // Step 52: Refresh Mirakl → Received
        LoggerUtility.info("Step 52: Refreshing Mirakl — checking Received status");
        status = waitForMiraklStatus("Received", 60);
        LoggerUtility.info("Mirakl status after Delivered: " + status);
        Assert.assertEquals(status, "Received",
            "Mirakl status should be 'Received' after Skydropx Delivered. Actual: " + status);
        LoggerUtility.info("Mirakl status = Received");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        LoggerUtility.info("Skydropx flow complete — Mirakl Status = Received");
    }

    // ----------------------------------------------------------------
    // Poll helper: refresh + check status up to maxRetries times
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
    // Extract a named custom field from a Kibo shipment item map.
    // Searches item.data, then item.packages[*].data.
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
        java.util.Set<String> handles = driver.getWindowHandles();
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
                    LoggerUtility.info("Checkout opened in new window — switched to: " + fdaTabHandle);
                    break;
                }
            }
        }
    }
}
// @AfterMethod navigateToHomePage() is inherited from BaseClass
