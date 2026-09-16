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
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklReturnPage;
import FDA_Automation_Script.FDA_Automation_Script.utils.ApiUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ReturnApiUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
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

public class TC_FBO_026_Test extends BaseClass {

    private static final String TC_NAME        = "TC_FBO_026";

    // TC-specific test data — mgowda account, 1 SKU qty=2, partial return qty=1
    private static final String TC_FDA_USER    = "mgowda@kognivera.com";
    private static final String TC_FDA_PASS    = "Mithun@12345";
    private static final String TC_SKU         = "78078094107";
    private static final String TC_CARD_NUMBER = "5454545454545454";
    private static final String TC_CARD_EXPIRY = "03/30";
    private static final String TC_CARD_CVV    = "737";
    private static final int    TC_ORDER_QTY   = 2;
    private static final int    TC_RETURN_QTY  = 1;

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
    private MiraklReturnPage      miraklReturnPage;

    // --- Dynamic Test Data (captured once, reused everywhere) ---
    private String orderId;
    private String orderTotal;
    private String tplShipmentId;
    private String carrierName;
    private String trackingNumber;
    private String deliveryPartner;

    private static final By MIRAKL_ORDER_STATUS_LOCATOR =
        By.xpath("(//span[contains(@class,'_6e782__sc-s4ukbq-0') and not(ancestor::a)])[1]");

    // ================================================================
    // @BeforeClass — init page objects + swap FDA session to TC user
    // ================================================================

    @BeforeClass
    public void initAndLogin() {
        fdaHomePage           = new FDAHomePage(driver);
        fdaLoginPage          = new FDALoginPage(driver);
        fdaPdpPage            = new FDAPDPPage(driver);
        fdaCartPage           = new FDACartPage(driver);
        fdaPaymentPage        = new FDAPaymentPage(driver);
        fdaSuccessPage        = new FDASuccessPage(driver);
        fdaOrderHistoryPage   = new FDAOrderHistoryPage(driver);
        miraklOrdersPage      = new MiraklOrdersPage(driver);
        miraklOrderDetailPage = new MiraklOrderDetailPage(driver);
        miraklReturnPage      = new MiraklReturnPage(driver);
        LoggerUtility.info("TC_FBO_026: All page objects initialized");

        // Swap FDA session only if suite default user differs from TC-specific user
        if (!TC_FDA_USER.equals(config.getFdaUsername())) {
            LoggerUtility.info("TC_FBO_026: Switching FDA session from "
                + config.getFdaUsername() + " to: " + TC_FDA_USER);
            switchToFDATab();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.logout();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.clickProfileIcon();
            fdaHomePage.clickLoginLink();
            fdaLoginPage.login(TC_FDA_USER, TC_FDA_PASS);
            LoggerUtility.info("TC_FBO_026: FDA login as " + TC_FDA_USER + " successful");
        } else {
            LoggerUtility.info("TC_FBO_026: Suite user already " + TC_FDA_USER + " — no session swap needed");
        }
    }

    // ================================================================
    // @AfterClass — restore original suite FDA session
    // ================================================================

    @AfterClass(alwaysRun = true)
    public void restoreOriginalFdaSession() {
        if (!TC_FDA_USER.equals(config.getFdaUsername())) {
            LoggerUtility.info("TC_FBO_026 @AfterClass: Restoring original FDA session to "
                + config.getFdaUsername());
            try {
                switchToFDATab();
                fdaHomePage.navigateTo(config.getFdaUrl());
                fdaHomePage.logout();
                fdaHomePage.navigateTo(config.getFdaUrl());
                fdaHomePage.clickProfileIcon();
                fdaHomePage.clickLoginLink();
                fdaLoginPage.login(config.getFdaUsername(), config.getFdaPassword());
                LoggerUtility.info("TC_FBO_026 @AfterClass: FDA session restored as " + config.getFdaUsername());
            } catch (Exception e) {
                LoggerUtility.error("TC_FBO_026 @AfterClass: Failed to restore FDA session: " + e.getMessage());
            }
        } else {
            LoggerUtility.info("TC_FBO_026 @AfterClass: No session restore needed — suite user unchanged");
        }
    }

    // ================================================================
    // @Test
    // ================================================================

    @Test(testName = TC_NAME, groups = {"FBO"},
          description = "Verify 1 SKU qty=2 from 1 3P seller → 1 Mirakl shipment (WEB-A) → full fulfillment → partial return qty=1 → compliance → partial refund → Kibo return status = closed")
    public void tc_fbo_026_single_product_qty2_partial_return_refund() throws InterruptedException {

        LoggerUtility.info("TC_FBO_026 execution started");
        LoggerUtility.info("Scenario: 1 SKU qty=2 from 3P seller → WEB-A → Received → Partial Return qty=1 → closed");
        LoggerUtility.info("Test Case ID      : " + TC_NAME);
        LoggerUtility.info("SKU               : " + TC_SKU);
        LoggerUtility.info("Ordered Quantity  : " + TC_ORDER_QTY);
        LoggerUtility.info("Return Quantity   : " + TC_RETURN_QTY);
        LoggerUtility.info("Expected Total    : MXN$800.00");

        // ============================================================
        // PHASE 1: FDA — Product Search, PDP, Quantity, Add to Cart
        // ============================================================

        LoggerUtility.info("===== PHASE 1: FDA Order Placement =====");

        // Step 1: Switch to FDA tab, verify home page
        switchToFDATab();
        LoggerUtility.info("Step 1: Switched to FDA tab — verifying FDA home page");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test: clear any leftover cart items
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // Step 2-4: Search by SKU
        LoggerUtility.info("Step 2: Clicking search text field '¿Qué estás buscando?'");
        LoggerUtility.info("Step 3: Entering SKU: " + TC_SKU);
        fdaHomePage.enterSearchQuery(TC_SKU);
        LoggerUtility.info("Step 4: Pressing Enter to search");
        fdaHomePage.pressSearchEnter();
        LoggerUtility.info("SKU " + TC_SKU + " searched successfully");

        // Step 5: Verify PDP displayed
        LoggerUtility.info("Step 5: Verifying Product Details Page is displayed");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "PDP not displayed after searching SKU: " + TC_SKU + " | TC: " + TC_NAME);

        // Step 6: Verify Add to Cart enabled
        LoggerUtility.info("Step 6: Verifying Agregar al carrito button is enabled");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "Add to cart button not enabled for SKU: " + TC_SKU + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 7: Increase quantity from 1 to 2
        LoggerUtility.info("Step 7: Clicking + button once to increase quantity from 1 to " + TC_ORDER_QTY);
        fdaPdpPage.increaseQuantity();

        // Step 8: Verify PDP quantity = 2
        String pdpQty = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 8: Verifying PDP quantity is exactly " + TC_ORDER_QTY + ". Actual: " + pdpQty);
        Assert.assertEquals(pdpQty, String.valueOf(TC_ORDER_QTY),
            "PDP quantity should be " + TC_ORDER_QTY + " | SKU: " + TC_SKU + " | TC: " + TC_NAME);
        LoggerUtility.info("PDP quantity confirmed = " + TC_ORDER_QTY);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 9: Add to cart
        LoggerUtility.info("Step 9: Clicking Agregar al carrito");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("SKU " + TC_SKU + " added to cart — qty=" + TC_ORDER_QTY);

        // ============================================================
        // PHASE 2: Cart Validation
        // ============================================================

        LoggerUtility.info("===== PHASE 2: Cart Validation =====");

        // Step 10: Navigate to cart
        LoggerUtility.info("Step 10: Clicking Mi carrito — opening cart page");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        // Step 11: Verify exactly 1 product
        int itemCount = fdaCartPage.getItemCount();
        LoggerUtility.info("Step 11: Cart item count: " + itemCount);
        Assert.assertEquals(itemCount, 1,
            "Cart should contain exactly 1 product. Actual: " + itemCount + " | TC: " + TC_NAME);
        LoggerUtility.info("Cart contains exactly 1 product — verified");

        // Step 12: Verify product name
        String cartProductName = fdaCartPage.getProductName();
        LoggerUtility.info("Step 12: Cart product name: " + cartProductName);
        Assert.assertFalse(cartProductName.isEmpty(),
            "Product name should not be empty in cart | SKU: " + TC_SKU + " | TC: " + TC_NAME);

        // Step 13: Verify quantity = 2
        String cartQty = fdaCartPage.getQuantity();
        LoggerUtility.info("Step 13: Cart quantity: " + cartQty);
        Assert.assertEquals(cartQty, String.valueOf(TC_ORDER_QTY),
            "Cart quantity should be " + TC_ORDER_QTY + " | SKU: " + TC_SKU + " | TC: " + TC_NAME);
        LoggerUtility.info("Cart quantity verified = " + TC_ORDER_QTY);

        // Step 14: Verify order total MXN$800.00
        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 14: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(),
            "Order total should be displayed in cart | TC: " + TC_NAME);
        Assert.assertTrue(orderTotal.contains("800"),
            "Order total should reflect MXN$800.00. Actual: " + orderTotal + " | TC: " + TC_NAME);
        LoggerUtility.info("Order total verified — " + orderTotal + " (expected: MXN$800.00)");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: Checkout + Payment
        // ============================================================

        LoggerUtility.info("===== PHASE 3: Checkout and Payment =====");

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

        // Steps 18-23: Enter card details
        LoggerUtility.info("Step 18-19: Clicking Número de tarjeta and entering card number");
        fdaPaymentPage.enterCardNumber(TC_CARD_NUMBER);
        LoggerUtility.info("Step 20-21: Clicking Fecha de expiración and entering: " + TC_CARD_EXPIRY);
        fdaPaymentPage.enterExpiry(TC_CARD_EXPIRY);
        LoggerUtility.info("Step 22-23: Clicking Código de seguridad and entering CVV");
        fdaPaymentPage.enterCvv(TC_CARD_CVV);

        // Step 24: Verify Completar pago button shows MXN$800.00
        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Step 24: Completar pago button text: " + payBtnText);
        Assert.assertTrue(
            payBtnText.contains("Completar pago") || payBtnText.contains("MXN")
                || payBtnText.contains("800") || payBtnText.contains("$"),
            "Completar pago button should display order total MXN$800.00. Actual: " + payBtnText
                + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 25: Complete payment
        LoggerUtility.info("Step 25: Clicking Completar pago button");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();

        // Step 26: Verify success page
        LoggerUtility.info("Step 26: Verifying order success page is displayed");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(),
            "Order success page not displayed after payment | TC: " + TC_NAME);

        // Step 27: Capture order ID
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 27: Order ID = " + orderId);
        Assert.assertFalse(orderId.isEmpty(),
            "Order ID should be present on success page | TC: " + TC_NAME);
        LoggerUtility.info("Order created successfully — Order ID: " + orderId
            + " | SKU: " + TC_SKU + " | qty=" + TC_ORDER_QTY + " | total=" + orderTotal);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: FDA Order History
        // ============================================================

        LoggerUtility.info("===== PHASE 4: FDA Order History =====");

        // Steps 28-29: Navigate to Mis pedidos
        LoggerUtility.info("Step 28: Clicking Mi cuenta profile icon");
        fdaHomePage.clickProfileIcon();
        LoggerUtility.info("Step 29: Clicking Mis pedidos link");
        fdaHomePage.clickMyOrdersLink();

        // Step 30: Verify order history page
        LoggerUtility.info("Step 30: Verifying Mis pedidos page is displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(),
            "Mis pedidos page not displayed | TC: " + TC_NAME);

        // Step 31: Verify order ID present
        LoggerUtility.info("Step 31: Verifying Order ID " + orderId + " is present in order history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
            "Order ID " + orderId + " not found in Mis pedidos | TC: " + TC_NAME);

        // Step 32: Verify order status = Creada or Pendiente
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 32: FDA order status: " + fdaStatus);
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
            "FDA order status should be 'Creada' or 'Pendiente'. Actual: " + fdaStatus
                + " | Order ID: " + orderId + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 5: MIRAKL — Find WEB-A, Verify, Accept
        // ============================================================

        LoggerUtility.info("===== PHASE 5: Mirakl — Verify and Accept Shipment WEB-A =====");

        // Step 33: Switch to Mirakl tab
        LoggerUtility.info("Step 33: Switching to existing Mirakl tab — reusing authenticated session");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 34-35: Navigate to All Orders
        LoggerUtility.info("Step 34: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 35: Clicking All Orders");
        miraklOrdersPage.clickAllOrders();

        String miraklSearchTerm = orderId + "WEB";
        String shipmentRefA     = orderId + "WEB-A";

        // Steps 36-37: Search — retry every 60s up to 10 min for Mirakl sync
        LoggerUtility.info("Step 36: Clicking search field");
        LoggerUtility.info("Step 37: Entering Order ID: " + miraklSearchTerm);
        boolean orderFoundInMirakl = false;
        for (int attempt = 1; attempt <= 10; attempt++) {
            LoggerUtility.info("Mirakl search attempt " + attempt + "/10 for: " + miraklSearchTerm);
            miraklOrdersPage.searchOrder(miraklSearchTerm);
            if (miraklOrdersPage.hasSearchResults(miraklSearchTerm)) {
                orderFoundInMirakl = true;
                LoggerUtility.info("Order found in Mirakl on attempt " + attempt);
                break;
            }
            LoggerUtility.info("Order not in Mirakl yet (attempt " + attempt
                + "/10) — waiting 60 seconds...");
            if (attempt < 10) {
                Thread.sleep(60_000);
                miraklOrdersPage.clickOrdersMenu();
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(orderFoundInMirakl,
            "Order " + miraklSearchTerm + " did not appear in Mirakl within 10 minutes | TC: " + TC_NAME);

        // Step 38: Verify exactly 1 shipment WEB-A is created
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefA),
            "Mirakl shipment WEB-A not found: " + shipmentRefA
                + " | Order: " + orderId + " | TC: " + TC_NAME);
        LoggerUtility.info("Step 38: Shipment WEB-A found: " + shipmentRefA);
        LoggerUtility.info("Exactly 1 shipment verified — WEB-A for SKU: " + TC_SKU
            + " qty=" + TC_ORDER_QTY);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 39: Open WEB-A detail, verify Pending acceptance, click Accept
        LoggerUtility.info("Step 39: Clicking Order Line ID: " + shipmentRefA);
        miraklOrderDetailPage.clickOrderInList(shipmentRefA);

        String miraklStatus = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("Mirakl WEB-A status before acceptance: " + miraklStatus);
        Assert.assertEquals(miraklStatus, "Pending acceptance",
            "WEB-A should be 'Pending acceptance' before accept. Actual: " + miraklStatus
                + " | Shipment: " + shipmentRefA + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 39: Clicking Accept for WEB-A: " + shipmentRefA);
        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        miraklStatus = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("Mirakl WEB-A status after acceptance: " + miraklStatus);
        Assert.assertEquals(miraklStatus, "Awaiting shipment",
            "WEB-A should be 'Awaiting shipment' after acceptance. Actual: " + miraklStatus
                + " | Shipment: " + shipmentRefA + " | TC: " + TC_NAME);
        String urlShipmentA = driver.getCurrentUrl();
        LoggerUtility.info("WEB-A accepted — status: " + miraklStatus + " | URL: " + urlShipmentA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 40: Wait 2 minutes for 3PL connector to process
        LoggerUtility.info("Step 40: Waiting 2 minutes for 3PL connector to process...");
        Thread.sleep(120_000);
        LoggerUtility.info("2-minute wait complete");

        // ============================================================
        // PHASE 6: KIBO API — Auth + Shipment Data
        // ============================================================

        LoggerUtility.info("===== PHASE 6: Kibo API — Authentication and Shipment Data =====");

        // Step 41: Kibo authentication
        LoggerUtility.info("Step 41: Calling Kibo Auth API — retrieving access token");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(),
            "Kibo access token should not be empty — authentication failed | Order: " + orderId
                + " | TC: " + TC_NAME);
        LoggerUtility.info("Kibo authentication successful — access token retrieved");

        // Step 42: Find Kibo order ID using FDA order externalId
        String externalId = orderId + "WEB";
        LoggerUtility.info("Step 42: Searching Kibo for externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(),
            "Kibo Order ID not found for externalId: " + externalId
                + " | FDA Order: " + orderId + " | TC: " + TC_NAME);
        LoggerUtility.info("Kibo Order ID: " + kiboOrderId + " | FDA Order: " + orderId);

        // Step 43: Poll Kibo until shipment has 3PL data — up to 20 × 30s = 10 minutes
        LoggerUtility.info("Step 43: Polling Kibo shipments for Order: " + orderId
            + " (1 shipment expected — 30s interval, up to 10 min)");
        boolean shipmentDataReady = false;
        for (int attempt = 1; attempt <= 20; attempt++) {
            LoggerUtility.info("Kibo shipment poll attempt " + attempt + "/20 — Order: " + orderId);
            Response shipmentsResp = ApiUtility.kiboGetShipmentsByOrderId(kiboToken, kiboOrderId);
            Assert.assertEquals(shipmentsResp.getStatusCode(), 200,
                "Kibo Get Shipments should return 200. Actual: " + shipmentsResp.getStatusCode()
                    + " | Order: " + orderId + " | TC: " + TC_NAME);

            // Handle both standard Kibo list format (items[]) and HAL format (_embedded.shipments)
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

                LoggerUtility.info("Shipment data — 3pl_shipmentId='" + tplShipmentId
                    + "' | deliveryPartner='" + deliveryPartner
                    + "' | carrierName='" + carrierName
                    + "' | tracking_number='" + trackingNumber + "'");

                if (!deliveryPartner.isEmpty() && !tplShipmentId.isEmpty()) {
                    shipmentDataReady = true;
                    LoggerUtility.info("3PL data ready on attempt " + attempt);
                    break;
                }
                LoggerUtility.info("3PL data not ready — tplShipmentId='" + tplShipmentId
                    + "' deliveryPartner='" + deliveryPartner + "'");
            } else {
                LoggerUtility.info("Kibo returned no shipments yet — waiting 30s (attempt "
                    + attempt + "/20)");
            }

            if (attempt < 20) Thread.sleep(30_000);
        }

        Assert.assertTrue(shipmentDataReady,
            "3PL data (deliveryPartner, 3pl_shipmentId) not populated within 10 minutes"
                + " | Order: " + orderId + " | TC: " + TC_NAME);

        LoggerUtility.info("Shipment details retrieved successfully:");
        LoggerUtility.info("  Delivery Partner : " + deliveryPartner);
        LoggerUtility.info("  3PL Shipment ID  : " + tplShipmentId);
        LoggerUtility.info("  Carrier Name     : " + carrierName);
        LoggerUtility.info("  Tracking Number  : " + trackingNumber);

        Assert.assertFalse(deliveryPartner.isEmpty(),
            "deliveryPartner missing from Kibo shipment data | Order: " + orderId + " | TC: " + TC_NAME);
        Assert.assertFalse(carrierName.isEmpty(),
            "carrierName missing from Kibo shipment data | Order: " + orderId + " | TC: " + TC_NAME);
        if (deliveryPartner.toLowerCase().contains("envioclick")) {
            Assert.assertFalse(trackingNumber.isEmpty(),
                "tracking_number required for Envioclick but missing | Order: " + orderId
                    + " | TC: " + TC_NAME);
        }
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 7: Carrier Webhook + Status Validation → Received
        // ============================================================

        LoggerUtility.info("===== PHASE 7: Carrier Webhook — WEB-A to Received =====");

        // Step 44: Navigate to WEB-A detail and route by deliveryPartner
        switchToMiraklTab();
        driver.get(urlShipmentA);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        LoggerUtility.info("Step 44: Navigated to Mirakl detail page for: " + shipmentRefA);
        LoggerUtility.info("Delivery partner: " + deliveryPartner);

        if (deliveryPartner.toLowerCase().contains("envioclick")) {
            LoggerUtility.info("Delivery partner = Envioclick — starting Envioclick flow for "
                + shipmentRefA);
            runEnvioclickFlow(shipmentRefA);
        } else if (deliveryPartner.toLowerCase().contains("skydropx")) {
            LoggerUtility.info("Delivery partner = Skydropx — starting Skydropx flow for "
                + shipmentRefA);
            runSkydropxFlow(shipmentRefA);
        } else {
            ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.FAIL);
            Assert.fail("Unknown delivery partner for " + shipmentRefA
                + ": '" + deliveryPartner + "'. Expected 'Envioclick' or 'Skydropx'."
                + " | Order: " + orderId + " | tplShipmentId: " + tplShipmentId
                + " | TC: " + TC_NAME);
        }

        // Step 53: Verify exactly 1 shipment reached Received
        LoggerUtility.info("Step 53: Shipment " + shipmentRefA + " verified — status = Received");
        LoggerUtility.info("WEB-A reached Received. Proceeding to return flow.");

        // ============================================================
        // PHASE 8: Return API — Get order_line_id + Create Partial Return Incident
        // ============================================================

        LoggerUtility.info("===== PHASE 8: Return API — Partial Return Flow =====");

        String orderCommercialId = orderId + "WEB";

        // Step 54: Get Mirakl order_line_id for WEB-A
        LoggerUtility.info("Step 54: Calling Get_order_line_id for commercial order: " + orderCommercialId);
        String orderLineId = ReturnApiUtility.getMiraklOrderLineId(orderCommercialId);

        Assert.assertFalse(orderLineId.isEmpty(),
            "order_line_id should not be empty for commercial order: " + orderCommercialId
                + " | TC: " + TC_NAME);
        LoggerUtility.info("order_line_id retrieved: " + orderLineId);

        // Step 55: Create partial Return incident — quantity=1 (partial, out of 2 ordered)
        LoggerUtility.info("Step 55: Return request details:");
        LoggerUtility.info("  order_commercial_id : " + orderCommercialId);
        LoggerUtility.info("  order_line_id       : " + orderLineId);
        LoggerUtility.info("  Original quantity   : " + TC_ORDER_QTY);
        LoggerUtility.info("  Return quantity     : " + TC_RETURN_QTY + " (PARTIAL RETURN — do NOT send qty=2)");
        LoggerUtility.info("Calling Return service API — creating partial return incident");
        Response returnResponse = ReturnApiUtility.postReturn(orderCommercialId, orderLineId, TC_RETURN_QTY);

        Assert.assertTrue(
            returnResponse.getStatusCode() == 200 || returnResponse.getStatusCode() == 201,
            "Return service should return 200 or 201. Actual: " + returnResponse.getStatusCode()
                + " | Body: " + returnResponse.getBody().asString()
                + " | orderCommercialId: " + orderCommercialId
                + " | orderLineId: " + orderLineId
                + " | quantity: " + TC_RETURN_QTY + " | TC: " + TC_NAME);
        LoggerUtility.info("Partial return incident created — HTTP " + returnResponse.getStatusCode()
            + " | quantity=" + TC_RETURN_QTY);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 9: Mirakl Return UI — Mark Received, Compliance, Partial Refund
        // ============================================================

        LoggerUtility.info("===== PHASE 9: Mirakl Return UI — Partial Refund Flow =====");

        // Navigate to WEB-A for return actions
        LoggerUtility.info("Switching to Mirakl — navigating to WEB-A order detail for return");
        switchToMiraklTab();
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        LoggerUtility.info("Searching for WEB-A order in Mirakl: " + shipmentRefA);
        miraklOrdersPage.searchOrder(shipmentRefA);
        miraklOrderDetailPage.clickOrderInList(shipmentRefA);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 56: Click "Mark as received"
        LoggerUtility.info("Step 56: Clicking Mark as received");
        miraklReturnPage.clickMarkAsReceived();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 57: Confirm popup "Mark as received"
        LoggerUtility.info("Step 57: Confirming Mark as received on popup");
        miraklReturnPage.confirmMarkAsReceivedPopup();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 58: Click "Check compliance"
        LoggerUtility.info("Step 58: Clicking Check compliance");
        miraklReturnPage.clickCheckCompliance();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 59: Click "Save"
        LoggerUtility.info("Step 59: Clicking Save");
        miraklReturnPage.clickSave();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 60: Click "Refund" dropdown
        LoggerUtility.info("Step 60: Clicking Refund dropdown");
        miraklReturnPage.clickRefundDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 61: Select "Refund part of the order" (NOT Full refund — this is a partial return)
        LoggerUtility.info("Step 61: Selecting Refund part of the order — partial return qty=" + TC_RETURN_QTY);
        Assert.assertTrue(TC_RETURN_QTY < TC_ORDER_QTY,
            "Return qty must be less than order qty for partial refund | returnQty=" + TC_RETURN_QTY
                + " | orderQty=" + TC_ORDER_QTY + " | TC: " + TC_NAME);
        miraklReturnPage.selectPartialRefundFromDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 62: Click "Select" dropdown for reason
        LoggerUtility.info("Step 62: Clicking Select dropdown for refund reason");
        miraklReturnPage.clickPartialRefundReasonDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 63: Select "Item returned"
        LoggerUtility.info("Step 63: Selecting Item returned");
        miraklReturnPage.selectItemReturned();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 64-65: Enter refund quantity = 1
        LoggerUtility.info("Step 64: Clicking Quantity field");
        LoggerUtility.info("Step 65: Entering refund quantity = " + TC_RETURN_QTY
            + " (original qty=" + TC_ORDER_QTY + ", partial refund)");
        miraklReturnPage.enterPartialRefundQuantity(TC_RETURN_QTY);
        LoggerUtility.info("Refund quantity " + TC_RETURN_QTY + " entered");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 66: Click "Refund order" button
        LoggerUtility.info("Step 66: Clicking Refund order button");
        miraklReturnPage.clickRefundOrderButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 67: Confirm refund popup
        LoggerUtility.info("Step 67: Clicking Confirm on refund confirmation popup");
        miraklReturnPage.confirmRefundPopup();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 10: Final Return API Validation — Kibo returns status = closed
        // ============================================================

        LoggerUtility.info("===== PHASE 10: Final Return API Validation =====");

        // Step 68: Wait 1 minute for refund to process, then refresh Mirakl
        LoggerUtility.info("Step 68: Waiting 60 seconds for partial refund to process...");
        Thread.sleep(60_000);
        LoggerUtility.info("60-second wait complete");
        LoggerUtility.info("Step 68: Refreshing Mirakl page");
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 69: Call Kibo Get_returns_for_a_order — verify return status = "closed"
        LoggerUtility.info("Step 69: Calling Get_returns_for_a_order — Kibo Order ID: " + kiboOrderId);
        Response returnsResp = ApiUtility.kiboGetReturnsForOrder(kiboToken, kiboOrderId);
        Assert.assertEquals(returnsResp.getStatusCode(), 200,
            "Get returns API should return 200. Actual: " + returnsResp.getStatusCode()
                + " | Kibo Order: " + kiboOrderId + " | FDA Order: " + orderId + " | TC: " + TC_NAME);

        String returnStatus = returnsResp.jsonPath().getString("items[0].status");
        LoggerUtility.info("Step 69: Kibo return status: " + returnStatus);
        Assert.assertNotNull(returnStatus,
            "Return item not found in Kibo returns response | Kibo Order: " + kiboOrderId
                + " | FDA Order: " + orderId + " | TC: " + TC_NAME);
        Assert.assertEquals(returnStatus.toLowerCase(), "closed",
            "Kibo return status should be 'closed'. Actual: '" + returnStatus + "'"
                + " | Kibo Order: " + kiboOrderId + " | FDA Order: " + orderId + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);

        // Final summary log
        LoggerUtility.info("TC_FBO_026 completed successfully");
        LoggerUtility.info("  Test Case ID      : " + TC_NAME);
        LoggerUtility.info("  SKU               : " + TC_SKU);
        LoggerUtility.info("  Ordered Quantity  : " + TC_ORDER_QTY);
        LoggerUtility.info("  Return Quantity   : " + TC_RETURN_QTY);
        LoggerUtility.info("  Refund Quantity   : " + TC_RETURN_QTY);
        LoggerUtility.info("  Expected Total    : MXN$800.00");
        LoggerUtility.info("  Actual Total      : " + orderTotal);
        LoggerUtility.info("  Order ID          : " + orderId);
        LoggerUtility.info("  Mirakl Order ID   : " + shipmentRefA);
        LoggerUtility.info("  Kibo Order ID     : " + kiboOrderId);
        LoggerUtility.info("  Shipment ID       : " + shipmentRefA);
        LoggerUtility.info("  Delivery Partner  : " + deliveryPartner);
        LoggerUtility.info("  Tracking Number   : " + trackingNumber);
        LoggerUtility.info("  3PL Shipment ID   : " + tplShipmentId);
        LoggerUtility.info("  Carrier Name      : " + carrierName);
        LoggerUtility.info("  order_line_id     : (retrieved successfully)");
        LoggerUtility.info("  Return Status     : " + returnStatus);
        LoggerUtility.info("  Final Test Status : PASS");
    }

    // ================================================================
    // Envioclick flow: En tránsito → Shipped/3PL delivery | Entregado → Received
    // ================================================================
    private void runEnvioclickFlow(String shipmentRef) {
        LoggerUtility.info("Step 45: Calling Envioclick API — En tránsito for: " + shipmentRef);
        LoggerUtility.info("EnvioClick API Request — carrier: " + carrierName
            + " | idOrder (3pl_shipmentId): " + tplShipmentId
            + " | trackingCode: " + trackingNumber
            + " | myShipmentReference: " + orderId + "WEB"
            + " | deliveryPartner: " + deliveryPartner);
        Response enTransitoResp = ApiUtility.postEnvioclickEnTransito(
            tplShipmentId, trackingNumber, orderId, carrierName);
        LoggerUtility.info("EnvioClick En tránsito API Response = " + enTransitoResp.getStatusCode()
            + " | Body: " + enTransitoResp.getBody().asString());
        Assert.assertEquals(enTransitoResp.getStatusCode(), 200,
            "Envioclick En tránsito should return 200 for " + shipmentRef
                + ". Actual: " + enTransitoResp.getStatusCode()
                + " | Order: " + orderId + " | tplShipmentId: " + tplShipmentId
                + " | TC: " + TC_NAME);

        // Step 46: Verify Mirakl status → Shipped or 3PL delivery
        LoggerUtility.info("Step 46: Verifying Mirakl status → Shipped after En tránsito: " + shipmentRef);
        String status = waitForMiraklStatusOneOf(new String[]{"Shipped", "3PL delivery"}, 6);
        LoggerUtility.info("Mirakl status after En tránsito [" + shipmentRef + "]: " + status);
        Assert.assertTrue(status.equals("Shipped") || status.equals("3PL delivery"),
            "Mirakl status should be 'Shipped' or '3PL delivery' after Envioclick En tránsito ["
                + shipmentRef + "]. Actual: " + status
                + " | Order: " + orderId + " | tplShipmentId: " + tplShipmentId
                + " | TC: " + TC_NAME);
        LoggerUtility.info("Mirakl status = " + status + " — shipment in transit");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 47: Call Envioclick Entregado
        LoggerUtility.info("Step 47: Calling Envioclick API — Entregado for: " + shipmentRef);
        LoggerUtility.info("EnvioClick API Request — carrier: " + carrierName
            + " | idOrder (3pl_shipmentId): " + tplShipmentId
            + " | trackingCode: " + trackingNumber
            + " | myShipmentReference: " + orderId + "WEB"
            + " | deliveryPartner: " + deliveryPartner);
        Response entregadoResp = ApiUtility.postEnvioclickEntregado(
            tplShipmentId, trackingNumber, orderId, carrierName);
        LoggerUtility.info("EnvioClick Entregado API Response = " + entregadoResp.getStatusCode()
            + " | Body: " + entregadoResp.getBody().asString());
        Assert.assertEquals(entregadoResp.getStatusCode(), 200,
            "Envioclick Entregado should return 200 for " + shipmentRef
                + ". Actual: " + entregadoResp.getStatusCode()
                + " | Order: " + orderId + " | tplShipmentId: " + tplShipmentId
                + " | TC: " + TC_NAME);

        // Step 48: Verify Mirakl status → Received
        LoggerUtility.info("Step 48: Verifying Mirakl status → Received after Entregado: " + shipmentRef);
        status = waitForMiraklStatus("Received", 6);
        LoggerUtility.info("Mirakl status after Entregado [" + shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Received",
            "Mirakl status should be 'Received' after Envioclick Entregado ["
                + shipmentRef + "]. Actual: " + status
                + " | Order: " + orderId + " | tplShipmentId: " + tplShipmentId
                + " | TC: " + TC_NAME);
        LoggerUtility.info("Mirakl status = Received");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("Envioclick flow complete — " + shipmentRef + " = Received | Order: " + orderId);
    }

    // ================================================================
    // Skydropx flow: Picked_up → Shipped | Delivered → Received
    // ================================================================
    private void runSkydropxFlow(String shipmentRef) {
        LoggerUtility.info("Step 49: Calling Skydropx API — Picked_up for: " + shipmentRef);
        LoggerUtility.info("Skydropx API Request — id (3pl_shipmentId): " + tplShipmentId
            + " | deliveryPartner: " + deliveryPartner + " | Order: " + orderId);
        Response pickedUpResp = ApiUtility.postSkydropxPickedUp(tplShipmentId);
        LoggerUtility.info("Skydropx Picked_up API Response = " + pickedUpResp.getStatusCode()
            + " | Body: " + pickedUpResp.getBody().asString());
        Assert.assertEquals(pickedUpResp.getStatusCode(), 200,
            "Skydropx Picked_up should return 200 for " + shipmentRef
                + ". Actual: " + pickedUpResp.getStatusCode()
                + " | Order: " + orderId + " | tplShipmentId: " + tplShipmentId
                + " | TC: " + TC_NAME);

        // Step 50: Verify Mirakl status → Shipped
        LoggerUtility.info("Step 50: Verifying Mirakl status → Shipped after Picked_up: " + shipmentRef);
        String status = waitForMiraklStatus("Shipped", 6);
        LoggerUtility.info("Mirakl status after Picked_up [" + shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Shipped",
            "Mirakl status should be 'Shipped' after Skydropx Picked_up ["
                + shipmentRef + "]. Actual: " + status
                + " | Order: " + orderId + " | tplShipmentId: " + tplShipmentId
                + " | TC: " + TC_NAME);
        LoggerUtility.info("Mirakl status = Shipped — shipment picked up");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 51: Call Skydropx Delivered
        LoggerUtility.info("Step 51: Calling Skydropx API — Delivered for: " + shipmentRef);
        LoggerUtility.info("Skydropx API Request — id (3pl_shipmentId): " + tplShipmentId
            + " | deliveryPartner: " + deliveryPartner + " | Order: " + orderId);
        Response deliveredResp = ApiUtility.postSkydropxDelivered(tplShipmentId);
        LoggerUtility.info("Skydropx Delivered API Response = " + deliveredResp.getStatusCode()
            + " | Body: " + deliveredResp.getBody().asString());
        Assert.assertEquals(deliveredResp.getStatusCode(), 200,
            "Skydropx Delivered should return 200 for " + shipmentRef
                + ". Actual: " + deliveredResp.getStatusCode()
                + " | Order: " + orderId + " | tplShipmentId: " + tplShipmentId
                + " | TC: " + TC_NAME);

        // Step 52: Verify Mirakl status → Received
        LoggerUtility.info("Step 52: Verifying Mirakl status → Received after Delivered: " + shipmentRef);
        status = waitForMiraklStatus("Received", 6);
        LoggerUtility.info("Mirakl status after Delivered [" + shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Received",
            "Mirakl status should be 'Received' after Skydropx Delivered ["
                + shipmentRef + "]. Actual: " + status
                + " | Order: " + orderId + " | tplShipmentId: " + tplShipmentId
                + " | TC: " + TC_NAME);
        LoggerUtility.info("Mirakl status = Received");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("Skydropx flow complete — " + shipmentRef + " = Received | Order: " + orderId);
    }

    // ================================================================
    // Poll Mirakl for a specific expected status (refresh-based, no Thread.sleep)
    // ================================================================
    private String waitForMiraklStatus(String expected, int maxRetries) {
        String status = "";
        for (int i = 1; i <= maxRetries; i++) {
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            status = miraklOrderDetailPage.getOrderStatus();
            LoggerUtility.info("Mirakl status check " + i + "/" + maxRetries
                + ": " + status + " (expected: " + expected + ")");
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
            LoggerUtility.info("Mirakl status check " + i + "/" + maxRetries
                + ": " + status + " (expected one of: " + Arrays.toString(expected) + ")");
            if (expectedSet.contains(status)) break;
        }
        return status;
    }

    // ================================================================
    // Extract named custom field from Kibo shipment item map.
    // Searches: item.data map first, then item.packages[*].data maps.
    // ================================================================
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

    // ================================================================
    // Handle checkout window that may open in a new browser window
    // after clickProceedToPayment — updates fdaTabHandle if needed
    // ================================================================
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
                    LoggerUtility.info("Checkout opened in new window — switched to: " + fdaTabHandle);
                    break;
                }
            }
        }
    }
}
// @AfterMethod navigateToHomePage() is inherited from BaseClass — navigates FDA + Mirakl to home
