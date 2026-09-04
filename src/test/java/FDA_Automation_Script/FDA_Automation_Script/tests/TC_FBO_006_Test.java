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
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import io.restassured.response.Response;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TC_FBO_006_Test extends BaseClass {

    private static final String TC_NAME        = "TC_FBO_006";

    // TC-specific test data — mgowda account, 2 products from 2 different 3P sellers, qty=2 each
    private static final String TC_FDA_USER    = "mgowda@kognivera.com";
    private static final String TC_FDA_PASS    = "Mithun@12345";
    private static final String TC_SKU_1       = "78078094274";   // Seller A
    private static final String TC_SKU_2       = "90099999967";   // Seller B (different 3P seller)
    private static final String TC_CARD_NUMBER = "5454545454545454";
    private static final String TC_CARD_EXPIRY = "03/30";
    private static final String TC_CARD_CVV    = "737";
    private static final int    TC_QUANTITY    = 2;

    // Per-shipment data collected from Kibo + Mirakl detail page URLs
    private static class ShipmentInfo {
        String shipmentRef;
        String tplShipmentId;
        String carrierName;
        String trackingNumber;
        String deliveryPartner;
        String miraklDetailUrl;
    }

    private final List<ShipmentInfo> shipments = new ArrayList<>();

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
        LoggerUtility.info("TC_FBO_006: All page objects initialized");

        // TC_FBO_006 uses a different FDA account — logout the suite session and re-login
        LoggerUtility.info("TC_FBO_006: Switching FDA session to: " + TC_FDA_USER);
        switchToFDATab();
        fdaHomePage.navigateTo(config.getFdaUrl());
        fdaHomePage.logout();
        fdaHomePage.navigateTo(config.getFdaUrl());
        fdaHomePage.clickProfileIcon();
        fdaHomePage.clickLoginLink();
        fdaLoginPage.login(TC_FDA_USER, TC_FDA_PASS);
        LoggerUtility.info("TC_FBO_006: FDA login as " + TC_FDA_USER + " successful");
    }

    // ================================================================
    // @AfterClass — restore original suite FDA session so next TC is unaffected
    // ================================================================

    @AfterClass(alwaysRun = true)
    public void restoreOriginalFdaSession() {
        LoggerUtility.info("TC_FBO_006 @AfterClass: Restoring original FDA session");
        try {
            switchToFDATab();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.logout();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.clickProfileIcon();
            fdaHomePage.clickLoginLink();
            fdaLoginPage.login(config.getFdaUsername(), config.getFdaPassword());
            LoggerUtility.info("TC_FBO_006 @AfterClass: FDA session restored as " + config.getFdaUsername());
        } catch (Exception e) {
            LoggerUtility.error("TC_FBO_006 @AfterClass: Failed to restore FDA session: " + e.getMessage());
        }
    }

    // ================================================================
    // @Test
    // ================================================================

    @Test(testName = TC_NAME,
          description = "Verify 2 products qty=2 each from 2 different 3P sellers → 2 Mirakl shipments (WEB-A + WEB-B) → full fulfillment lifecycle")
    public void tc_fbo_006_dual_product_diff_sellers_qty2_each_full_fulfillment() throws InterruptedException {

        LoggerUtility.info("TC_FBO_006 execution started");
        LoggerUtility.info("Scenario: 2 products × qty=2 each from 2 different 3P sellers → WEB-A + WEB-B → Received");

        // ============================================================
        // PHASE 1: FDA — Product 1 (Seller A, qty=2) → cart
        // ============================================================

        switchToFDATab();
        LoggerUtility.info("Step 1: Switching to FDA tab");
        LoggerUtility.info("Step 2: Verifying FDA Home Page is displayed");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test: clear any leftover cart items from previous tests
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ---- Product 1 (Seller A) ----
        LoggerUtility.info("Step 3: Clicking search text field for Product 1 (Seller A)");
        LoggerUtility.info("Step 4: Entering Product 1 SKU: " + TC_SKU_1);
        fdaHomePage.enterSearchQuery(TC_SKU_1);
        LoggerUtility.info("Step 5: Pressing Enter to search Product 1");
        fdaHomePage.pressSearchEnter();
        LoggerUtility.info("Product 1 SKU " + TC_SKU_1 + " searched successfully");

        LoggerUtility.info("Step 6: Verifying Product Details Page is displayed for Product 1");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "PDP not displayed after searching Product 1 SKU: " + TC_SKU_1);

        LoggerUtility.info("Step 7: Verifying Agregar al carrito button is enabled for Product 1");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "Add to cart button not enabled for Product 1 SKU: " + TC_SKU_1);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 8: Clicking plus button once to increase Product 1 quantity from 1 to " + TC_QUANTITY);
        fdaPdpPage.increaseQuantity();

        String p1PdpQty = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 9: Verifying Product 1 quantity is exactly " + TC_QUANTITY + ". Actual: " + p1PdpQty);
        Assert.assertEquals(p1PdpQty, String.valueOf(TC_QUANTITY),
            "Product 1 quantity on PDP should be " + TC_QUANTITY + ". Actual: " + p1PdpQty);
        LoggerUtility.info("Product 1 quantity confirmed = " + TC_QUANTITY);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 10: Clicking Agregar al carrito for Product 1");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product 1 (SKU: " + TC_SKU_1 + ", Seller A) added to cart — qty=" + TC_QUANTITY);

        // Navigate home before searching Product 2
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ---- Product 2 (Seller B — different 3P seller) ----
        LoggerUtility.info("Step 11: Clicking search text field for Product 2 (Seller B — different 3P seller)");
        LoggerUtility.info("Step 12: Entering Product 2 SKU: " + TC_SKU_2 + " (different 3P seller from Product 1)");
        fdaHomePage.enterSearchQuery(TC_SKU_2);
        LoggerUtility.info("Step 13: Pressing Enter to search Product 2");
        fdaHomePage.pressSearchEnter();
        LoggerUtility.info("Product 2 SKU " + TC_SKU_2 + " searched successfully");

        LoggerUtility.info("Step 14: Verifying Product Details Page is displayed for Product 2");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "PDP not displayed after searching Product 2 SKU: " + TC_SKU_2);

        LoggerUtility.info("Step 15: Verifying Agregar al carrito button is enabled for Product 2");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "Add to cart button not enabled for Product 2 SKU: " + TC_SKU_2);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 16: Clicking plus button once to increase Product 2 quantity from 1 to " + TC_QUANTITY);
        fdaPdpPage.increaseQuantity();

        String p2PdpQty = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 17: Verifying Product 2 quantity is exactly " + TC_QUANTITY + ". Actual: " + p2PdpQty);
        Assert.assertEquals(p2PdpQty, String.valueOf(TC_QUANTITY),
            "Product 2 quantity on PDP should be " + TC_QUANTITY + ". Actual: " + p2PdpQty);
        LoggerUtility.info("Product 2 quantity confirmed = " + TC_QUANTITY);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 18: Clicking Agregar al carrito for Product 2");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product 2 (SKU: " + TC_SKU_2 + ", Seller B) added to cart — qty=" + TC_QUANTITY);

        // ============================================================
        // PHASE 2: Cart Validation
        // ============================================================

        LoggerUtility.info("Step 19: Clicking Mi carrito — opening cart page");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        // Step 20: Verify exactly 2 different products in cart
        int itemCount = fdaCartPage.getItemCount();
        LoggerUtility.info("Step 20: Cart item count: " + itemCount);
        Assert.assertEquals(itemCount, 2,
            "Cart should contain exactly 2 different products (one from each 3P seller). Actual: " + itemCount);
        LoggerUtility.info("Cart contains exactly 2 different products from 2 different 3P sellers — verified");

        // Step 21: Verify product names are present for both products
        List<String> productNames = fdaCartPage.getAllProductNames();
        LoggerUtility.info("Step 21: Cart product names: " + productNames);
        Assert.assertEquals(productNames.size(), 2,
            "Cart should have 2 product name entries. Actual: " + productNames.size());
        Assert.assertFalse(productNames.get(0).isEmpty(),
            "Product 1 name (SKU: " + TC_SKU_1 + ") should not be empty in cart");
        Assert.assertFalse(productNames.get(1).isEmpty(),
            "Product 2 name (SKU: " + TC_SKU_2 + ") should not be empty in cart");
        LoggerUtility.info("Product 1 in cart: " + productNames.get(0));
        LoggerUtility.info("Product 2 in cart: " + productNames.get(1));

        // Step 22: Verify both quantities are 2
        List<String> cartQtys = fdaCartPage.getAllQuantities();
        LoggerUtility.info("Step 22: Cart quantities: " + cartQtys);
        Assert.assertEquals(cartQtys.size(), 2,
            "Cart should have 2 quantity inputs. Actual: " + cartQtys.size());
        Assert.assertEquals(cartQtys.get(0), String.valueOf(TC_QUANTITY),
            "SKU " + TC_SKU_1 + " cart quantity should be " + TC_QUANTITY + ". Actual: " + cartQtys.get(0));
        Assert.assertEquals(cartQtys.get(1), String.valueOf(TC_QUANTITY),
            "SKU " + TC_SKU_2 + " cart quantity should be " + TC_QUANTITY + ". Actual: " + cartQtys.get(1));
        LoggerUtility.info("Product quantities validated — SKU " + TC_SKU_1 + " qty=" + cartQtys.get(0)
            + " | SKU " + TC_SKU_2 + " qty=" + cartQtys.get(1));

        // Step 23: Verify order total is MXN$1,600.00
        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 23: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(),
            "Order total should be displayed in cart");
        Assert.assertTrue(orderTotal.contains("1,600") || orderTotal.contains("1600"),
            "Order total should reflect MXN$1,600.00. Actual: " + orderTotal);
        LoggerUtility.info("Order total verified — " + orderTotal);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: Checkout + Payment
        // ============================================================

        LoggerUtility.info("Step 24: Clicking Proceed to payment");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();

        LoggerUtility.info("Step 25: Clicking Siguiente on shipping page");
        fdaPaymentPage.clickNextButton();

        LoggerUtility.info("Step 26: Selecting Pago con Tarjeta Crédito/Débito");
        fdaPaymentPage.selectCreditCardOption();

        LoggerUtility.info("Step 27-28: Clicking Número de tarjeta and entering card number");
        fdaPaymentPage.enterCardNumber(TC_CARD_NUMBER);
        LoggerUtility.info("Step 29-30: Clicking Fecha de expiración and entering expiry: " + TC_CARD_EXPIRY);
        fdaPaymentPage.enterExpiry(TC_CARD_EXPIRY);
        LoggerUtility.info("Step 31-32: Clicking Código de seguridad and entering CVV");
        fdaPaymentPage.enterCvv(TC_CARD_CVV);

        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Step 33: Completar pago button text: " + payBtnText);
        Assert.assertTrue(
            payBtnText.contains("Completar pago") || payBtnText.contains("MXN")
                || payBtnText.contains("1,600") || payBtnText.contains("$"),
            "Completar pago button should display order total MXN$1,600.00. Actual: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 34: Clicking Completar pago button");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();

        LoggerUtility.info("Step 35: Verifying order success page is displayed");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(),
            "Order success page not displayed after payment — TC_FBO_006");

        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 36: Order ID = " + orderId);
        Assert.assertFalse(orderId.isEmpty(),
            "Order ID should be present on success page — TC_FBO_006");
        LoggerUtility.info("Order created successfully — Order ID = " + orderId
            + " | SKU_1=" + TC_SKU_1 + " | SKU_2=" + TC_SKU_2 + " | qty=2 each");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: FDA Order History
        // ============================================================

        LoggerUtility.info("Step 37: Clicking Mi cuenta profile icon");
        fdaHomePage.clickProfileIcon();
        LoggerUtility.info("Step 38: Clicking Mis pedidos link");
        fdaHomePage.clickMyOrdersLink();

        LoggerUtility.info("Step 39: Verifying Mis pedidos page is displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(),
            "Mis pedidos page not displayed — TC_FBO_006");

        LoggerUtility.info("Step 40: Verifying Order ID " + orderId + " is present in order history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
            "Order ID " + orderId + " not found in Mis pedidos — TC_FBO_006");

        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 41: FDA order status: " + fdaStatus);
        Assert.assertTrue(
            fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
            "FDA order status should be 'Creada' or 'Pendiente'. Actual: " + fdaStatus
                + " | Order ID: " + orderId);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 5: MIRAKL — Verify WEB-A (Seller A) + WEB-B (Seller B), Accept Both
        // ============================================================

        LoggerUtility.info("Step 42: Switching to existing Mirakl tab — reusing authenticated session");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 43: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 44: Clicking All Orders");
        miraklOrdersPage.clickAllOrders();

        String miraklSearchTerm = orderId + "WEB";
        String shipmentRefA     = orderId + "WEB-A";
        String shipmentRefB     = orderId + "WEB-B";

        // Step 45-46: Search for order — retry every 60s up to 5 minutes for Mirakl sync delay
        LoggerUtility.info("Step 45: Clicking search field");
        LoggerUtility.info("Step 46: Entering Order ID: " + miraklSearchTerm);
        boolean orderFoundInMirakl = false;
        for (int attempt = 1; attempt <= 5; attempt++) {
            LoggerUtility.info("Mirakl search attempt " + attempt + "/5 for: " + miraklSearchTerm);
            miraklOrdersPage.searchOrder(miraklSearchTerm);
            if (miraklOrdersPage.hasSearchResults(miraklSearchTerm)) {
                orderFoundInMirakl = true;
                LoggerUtility.info("Order found in Mirakl on attempt " + attempt);
                break;
            }
            LoggerUtility.info("Order not in Mirakl yet (attempt " + attempt + "/5) — waiting 60 seconds...");
            if (attempt < 5) {
                Thread.sleep(60_000);
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(orderFoundInMirakl,
            "Order " + miraklSearchTerm + " did not appear in Mirakl within 5 minutes — TC_FBO_006");

        // Step 47: Verify both shipments (WEB-A Seller A + WEB-B Seller B) are created
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefA),
            "Mirakl shipment WEB-A (Seller A) not found: " + shipmentRefA + " | Order: " + orderId);
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefB),
            "Mirakl shipment WEB-B (Seller B) not found: " + shipmentRefB + " | Order: " + orderId);
        LoggerUtility.info("Step 47: WEB-A shipment found: " + shipmentRefA + " (Seller A — SKU: " + TC_SKU_1 + ")");
        LoggerUtility.info("Step 47: WEB-B shipment found: " + shipmentRefB + " (Seller B — SKU: " + TC_SKU_2 + ")");
        LoggerUtility.info("Both shipments verified — 2 different 3P sellers confirmed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ----------------------------------------------------------------
        // Step 48-49: Accept WEB-A (Seller A)
        // ----------------------------------------------------------------
        LoggerUtility.info("Step 48: Opening WEB-A shipment for acceptance: " + shipmentRefA);
        miraklOrderDetailPage.clickOrderInList(shipmentRefA);

        String statusA = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-A status before acceptance: " + statusA);
        Assert.assertEquals(statusA, "Pending acceptance",
            "WEB-A should be 'Pending acceptance' before accept. Actual: " + statusA
                + " | Shipment: " + shipmentRefA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 49: Clicking Accept for WEB-A: " + shipmentRefA);
        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        statusA = miraklOrderDetailPage.getOrderStatus();
        Assert.assertEquals(statusA, "Awaiting shipment",
            "WEB-A should be 'Awaiting shipment' after acceptance. Actual: " + statusA
                + " | Shipment: " + shipmentRefA);
        String urlShipmentA = driver.getCurrentUrl();
        LoggerUtility.info("WEB-A accepted — status: " + statusA + " | URL: " + urlShipmentA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ----------------------------------------------------------------
        // Step 48-49: Accept WEB-B (Seller B)
        // ----------------------------------------------------------------
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        miraklOrdersPage.searchOrder(miraklSearchTerm);

        LoggerUtility.info("Step 48: Opening WEB-B shipment for acceptance: " + shipmentRefB);
        miraklOrderDetailPage.clickOrderInList(shipmentRefB);

        String statusB = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-B status before acceptance: " + statusB);
        Assert.assertEquals(statusB, "Pending acceptance",
            "WEB-B should be 'Pending acceptance' before accept. Actual: " + statusB
                + " | Shipment: " + shipmentRefB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 49: Clicking Accept for WEB-B: " + shipmentRefB);
        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        statusB = miraklOrderDetailPage.getOrderStatus();
        Assert.assertEquals(statusB, "Awaiting shipment",
            "WEB-B should be 'Awaiting shipment' after acceptance. Actual: " + statusB
                + " | Shipment: " + shipmentRefB);
        String urlShipmentB = driver.getCurrentUrl();
        LoggerUtility.info("WEB-B accepted — status: " + statusB + " | URL: " + urlShipmentB);
        LoggerUtility.info("Both WEB-A (Seller A) and WEB-B (Seller B) shipments accepted successfully");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 6: KIBO API — Auth + Shipment Data for Both Shipments
        // Step 50 wait is satisfied by Kibo polling loop (30s × 20 = 10 min max)
        // ============================================================

        // Step 51: Kibo authentication
        LoggerUtility.info("========== Kibo Authentication ==========");
        LoggerUtility.info("Step 51: Calling Kibo Auth API — retrieving access token");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(),
            "Kibo access token should not be empty — authentication failed | Order: " + orderId);
        LoggerUtility.info("Kibo authentication successful — access token retrieved");

        // Step 52: Find Kibo order ID using FDA order ID as externalId
        String externalId = orderId + "WEB";
        LoggerUtility.info("========== Get All Orders in Kibo ==========");
        LoggerUtility.info("Step 52: Searching Kibo for externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(),
            "Kibo Order ID not found for externalId: " + externalId + " | FDA Order: " + orderId);
        LoggerUtility.info("Kibo Order ID: " + kiboOrderId + " | FDA Order: " + orderId);

        // Step 53: Poll Kibo until both shipments have 3PL data — up to 20 × 30s = 10 minutes
        LoggerUtility.info("========== Get Shipment Details ==========");
        LoggerUtility.info("Step 53: Polling Kibo shipments for Order ID " + orderId
            + " (2 shipments expected — one per seller)");
        boolean allShipmentsReady = false;

        for (int attempt = 1; attempt <= 20; attempt++) {
            LoggerUtility.info("Kibo shipment poll attempt " + attempt + "/20 — Order: " + orderId);
            Response shipmentsResp = ApiUtility.kiboGetShipmentsByOrderId(kiboToken, kiboOrderId);
            Assert.assertEquals(shipmentsResp.getStatusCode(), 200,
                "Kibo Get Shipments should return 200. Actual: " + shipmentsResp.getStatusCode()
                    + " | Order: " + orderId);

            // Handle both standard Kibo list format (items[]) and HAL format (_embedded.shipments)
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

                    LoggerUtility.info("Shipment data — 3pl_shipmentId='" + info.tplShipmentId
                        + "' | deliveryPartner='" + info.deliveryPartner
                        + "' | carrierName='" + info.carrierName
                        + "' | tracking_number='" + info.trackingNumber + "'");

                    if (info.tplShipmentId.isEmpty() || info.deliveryPartner.isEmpty()) {
                        allPopulated = false;
                        LoggerUtility.info("3PL data not ready — tplShipmentId='"
                            + info.tplShipmentId + "' deliveryPartner='" + info.deliveryPartner + "'");
                    } else {
                        ready.add(info);
                    }
                }

                if (allPopulated && ready.size() >= 2) {
                    // For 2-different-seller orders, Kibo index 0 → WEB-B (Seller B processed first),
                    // Kibo index 1 → WEB-A (Seller A processed second). Mirrors TC_FBO_005 mapping.
                    ready.get(0).shipmentRef     = shipmentRefB;
                    ready.get(0).miraklDetailUrl = urlShipmentB;
                    ready.get(1).shipmentRef     = shipmentRefA;
                    ready.get(1).miraklDetailUrl = urlShipmentA;
                    shipments.addAll(ready);
                    allShipmentsReady = true;
                    LoggerUtility.info("All " + shipments.size()
                        + " shipments have 3PL data — attempt " + attempt
                        + " | Order: " + orderId);
                    break;
                }
            } else {
                int count = (shipmentList == null) ? 0 : shipmentList.size();
                LoggerUtility.info("Kibo returned " + count
                    + " shipments (need 2) — waiting 30s (attempt " + attempt + "/20)");
            }

            if (attempt < 20) Thread.sleep(30_000);
        }

        Assert.assertTrue(allShipmentsReady,
            "3PL data not fully populated for both shipments within 10 minutes — Order: " + orderId
                + " | SKU_1=" + TC_SKU_1 + " | SKU_2=" + TC_SKU_2);
        Assert.assertEquals(shipments.size(), 2,
            "Expected exactly 2 Kibo shipments (one per 3P seller). Actual: " + shipments.size()
                + " | Order: " + orderId);

        // Log all retrieved shipment details
        for (int i = 0; i < shipments.size(); i++) {
            ShipmentInfo info = shipments.get(i);
            LoggerUtility.info("--- Shipment " + (i + 1) + " [" + info.shipmentRef + "] ---");
            LoggerUtility.info("  Delivery Partner    : " + info.deliveryPartner);
            LoggerUtility.info("  3PL Shipment ID     : " + info.tplShipmentId);
            LoggerUtility.info("  Carrier Name        : " + info.carrierName);
            LoggerUtility.info("  Tracking Number     : " + info.trackingNumber);
            LoggerUtility.info("  Mirakl Detail URL   : " + info.miraklDetailUrl);
        }
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 7: Process Both Shipments Independently (Steps 54-63)
        // Each shipment's deliveryPartner is checked independently.
        // ============================================================

        for (ShipmentInfo info : shipments) {
            LoggerUtility.info("========== Processing Shipment: " + info.shipmentRef + " ==========");
            LoggerUtility.info("Order ID        : " + orderId);
            LoggerUtility.info("Shipment Ref    : " + info.shipmentRef);
            LoggerUtility.info("Delivery Partner: " + info.deliveryPartner);
            LoggerUtility.info("3PL Shipment ID : " + info.tplShipmentId);
            LoggerUtility.info("Carrier Name    : " + info.carrierName);
            LoggerUtility.info("Tracking Number : " + info.trackingNumber);

            // Navigate to this shipment's Mirakl detail page using the captured URL
            switchToMiraklTab();
            driver.get(info.miraklDetailUrl);
            WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
            LoggerUtility.info("Navigated to Mirakl detail page for: " + info.shipmentRef);

            if (info.deliveryPartner.toLowerCase().contains("envioclick")) {
                LoggerUtility.info("Delivery partner = Envioclick — starting Envioclick flow for " + info.shipmentRef);
                runEnvioclickFlow(info);
            } else if (info.deliveryPartner.toLowerCase().contains("skydropx")) {
                LoggerUtility.info("Delivery partner = Skydropx — starting Skydropx flow for " + info.shipmentRef);
                runSkydropxFlow(info);
            } else {
                ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.FAIL);
                Assert.fail("Unknown delivery partner for " + info.shipmentRef
                    + ": '" + info.deliveryPartner + "'. Expected 'Envioclick' or 'Skydropx'."
                    + " | Order: " + orderId + " | tplShipmentId: " + info.tplShipmentId);
            }
        }

        LoggerUtility.info("TC_FBO_006 completed successfully");
        LoggerUtility.info("Order ID      : " + orderId);
        LoggerUtility.info("Product 1 SKU : " + TC_SKU_1 + " (Seller A) qty=2 — Shipment WEB-A: Received");
        LoggerUtility.info("Product 2 SKU : " + TC_SKU_2 + " (Seller B) qty=2 — Shipment WEB-B: Received");
        LoggerUtility.info("Order Total   : " + orderTotal);
        LoggerUtility.info("Final Test Status: PASS");
    }

    // ================================================================
    // Envioclick flow: En tránsito → Shipped/3PL delivery | Entregado → Received
    // ================================================================
    private void runEnvioclickFlow(ShipmentInfo info) {
        LoggerUtility.info("Step 55: Calling Envioclick API — En tránsito for: " + info.shipmentRef);
        LoggerUtility.info("EnvioClick API Request — carrier: " + info.carrierName
            + " | idOrder (3pl_shipmentId): " + info.tplShipmentId
            + " | trackingCode: " + info.trackingNumber
            + " | myShipmentReference: " + orderId + "WEB"
            + " | deliveryPartner: " + info.deliveryPartner);
        Response enTransitoResp = ApiUtility.postEnvioclickEnTransito(
            info.tplShipmentId, info.trackingNumber, orderId, info.carrierName);
        LoggerUtility.info("EnvioClick En tránsito API Response = " + enTransitoResp.getStatusCode()
            + " | Body: " + enTransitoResp.getBody().asString());
        Assert.assertEquals(enTransitoResp.getStatusCode(), 200,
            "Envioclick En tránsito should return 200 for " + info.shipmentRef
                + ". Actual: " + enTransitoResp.getStatusCode()
                + " | Order: " + orderId + " | tplShipmentId: " + info.tplShipmentId);

        LoggerUtility.info("Step 56: Switching to Mirakl — verifying status → Shipped for: " + info.shipmentRef);
        String status = waitForMiraklStatusOneOf(new String[]{"Shipped", "3PL delivery"}, 6);
        LoggerUtility.info("Mirakl status after En tránsito [" + info.shipmentRef + "]: " + status);
        Assert.assertTrue(status.equals("Shipped") || status.equals("3PL delivery"),
            "Mirakl status should be 'Shipped' or '3PL delivery' after Envioclick En tránsito ["
                + info.shipmentRef + "]. Actual: " + status
                + " | Order: " + orderId + " | tplShipmentId: " + info.tplShipmentId);
        LoggerUtility.info("Mirakl status = " + status + " — Shipment " + info.shipmentRef + " in transit");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 57: Calling Envioclick API — Entregado for: " + info.shipmentRef);
        LoggerUtility.info("EnvioClick API Request — carrier: " + info.carrierName
            + " | idOrder (3pl_shipmentId): " + info.tplShipmentId
            + " | trackingCode: " + info.trackingNumber
            + " | myShipmentReference: " + orderId + "WEB"
            + " | deliveryPartner: " + info.deliveryPartner);
        Response entregadoResp = ApiUtility.postEnvioclickEntregado(
            info.tplShipmentId, info.trackingNumber, orderId, info.carrierName);
        LoggerUtility.info("EnvioClick Entregado API Response = " + entregadoResp.getStatusCode()
            + " | Body: " + entregadoResp.getBody().asString());
        Assert.assertEquals(entregadoResp.getStatusCode(), 200,
            "Envioclick Entregado should return 200 for " + info.shipmentRef
                + ". Actual: " + entregadoResp.getStatusCode()
                + " | Order: " + orderId + " | tplShipmentId: " + info.tplShipmentId);

        LoggerUtility.info("Step 58: Switching to Mirakl — verifying status → Received for: " + info.shipmentRef);
        status = waitForMiraklStatus("Received", 6);
        LoggerUtility.info("Mirakl status after Entregado [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Received",
            "Mirakl status should be 'Received' after Envioclick Entregado ["
                + info.shipmentRef + "]. Actual: " + status
                + " | Order: " + orderId + " | tplShipmentId: " + info.tplShipmentId);
        LoggerUtility.info("Mirakl status = Received");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        LoggerUtility.info("Envioclick flow complete — " + info.shipmentRef + " = Received | Order: " + orderId);
    }

    // ================================================================
    // Skydropx flow: Picked_up → Shipped | Delivered → Received
    // ================================================================
    private void runSkydropxFlow(ShipmentInfo info) {
        LoggerUtility.info("Step 59: Calling Skydropx API — Picked_up for: " + info.shipmentRef);
        LoggerUtility.info("Skydropx API Request — id (3pl_shipmentId): " + info.tplShipmentId
            + " | deliveryPartner: " + info.deliveryPartner
            + " | Order: " + orderId);
        Response pickedUpResp = ApiUtility.postSkydropxPickedUp(info.tplShipmentId);
        LoggerUtility.info("Skydropx Picked_up API Response = " + pickedUpResp.getStatusCode()
            + " | Body: " + pickedUpResp.getBody().asString());
        Assert.assertEquals(pickedUpResp.getStatusCode(), 200,
            "Skydropx Picked_up should return 200 for " + info.shipmentRef
                + ". Actual: " + pickedUpResp.getStatusCode()
                + " | Order: " + orderId + " | tplShipmentId: " + info.tplShipmentId);

        LoggerUtility.info("Step 60: Switching to Mirakl — verifying status → Shipped for: " + info.shipmentRef);
        String status = waitForMiraklStatus("Shipped", 6);
        LoggerUtility.info("Mirakl status after Picked_up [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Shipped",
            "Mirakl status should be 'Shipped' after Skydropx Picked_up ["
                + info.shipmentRef + "]. Actual: " + status
                + " | Order: " + orderId + " | tplShipmentId: " + info.tplShipmentId);
        LoggerUtility.info("Mirakl status = Shipped — Shipment " + info.shipmentRef + " picked up");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 61: Calling Skydropx API — Delivered for: " + info.shipmentRef);
        LoggerUtility.info("Skydropx API Request — id (3pl_shipmentId): " + info.tplShipmentId
            + " | deliveryPartner: " + info.deliveryPartner
            + " | Order: " + orderId);
        Response deliveredResp = ApiUtility.postSkydropxDelivered(info.tplShipmentId);
        LoggerUtility.info("Skydropx Delivered API Response = " + deliveredResp.getStatusCode()
            + " | Body: " + deliveredResp.getBody().asString());
        Assert.assertEquals(deliveredResp.getStatusCode(), 200,
            "Skydropx Delivered should return 200 for " + info.shipmentRef
                + ". Actual: " + deliveredResp.getStatusCode()
                + " | Order: " + orderId + " | tplShipmentId: " + info.tplShipmentId);

        LoggerUtility.info("Step 62: Switching to Mirakl — verifying status → Received for: " + info.shipmentRef);
        status = waitForMiraklStatus("Received", 6);
        LoggerUtility.info("Mirakl status after Delivered [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Received",
            "Mirakl status should be 'Received' after Skydropx Delivered ["
                + info.shipmentRef + "]. Actual: " + status
                + " | Order: " + orderId + " | tplShipmentId: " + info.tplShipmentId);
        LoggerUtility.info("Mirakl status = Received");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        LoggerUtility.info("Skydropx flow complete — " + info.shipmentRef + " = Received | Order: " + orderId);
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
// @AfterMethod navigateToHomePage() is inherited from BaseClass — navigates FDA + Mirakl to home
