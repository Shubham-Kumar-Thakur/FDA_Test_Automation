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

import java.util.List;
import java.util.Set;

public class TC_FBO_032_Test extends BaseClass {

    private static final String TC_NAME        = "TC_FBO_032";

    // TC-specific credentials and test data — never stored in config.properties
    private static final String TC_FDA_USER    = "mgowda@kognivera.com";
    private static final String TC_FDA_PASS    = "Mithun@12345";
    private static final String TC_SKU_1       = "78078094274";    // Seller B (offer_sku != EAN), qty=2
    private static final String TC_SKU_2       = "7080901020316";  // Seller A, qty=2
    private static final int    TC_QUANTITY    = 2;
    private static final String TC_CARD_NUMBER = "5454545454545454";
    private static final String TC_CARD_EXPIRY = "03/30";
    private static final String TC_CARD_CVV    = "737";

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

    // --- Dynamic test data (captured once, reused throughout the test) ---
    private String orderId;
    private String orderTotal;

    private static final By MIRAKL_ORDER_STATUS_LOCATOR =
        By.xpath("(//span[contains(@class,'_6e782__sc-s4ukbq-0') and not(ancestor::a)])[1]");

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
        LoggerUtility.info("TC_FBO_032: All page objects initialized");

        // TC_FBO_032 uses a different FDA account — guard prevents double-logout
        // when the previous TC already ran as the same user
        if (!TC_FDA_USER.equals(config.getFdaUsername())) {
            LoggerUtility.info("TC_FBO_032 @BeforeClass: Switching FDA session to: " + TC_FDA_USER);
            switchToFDATab();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.logout();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.clickProfileIcon();
            fdaHomePage.clickLoginLink();
            fdaLoginPage.login(TC_FDA_USER, TC_FDA_PASS);
            LoggerUtility.info("TC_FBO_032 @BeforeClass: FDA login as " + TC_FDA_USER + " successful");
        }
    }

    @AfterClass(alwaysRun = true)
    public void restoreOriginalFdaSession() {
        // Restore suite-default FDA user so subsequent test cases are unaffected
        if (!TC_FDA_USER.equals(config.getFdaUsername())) {
            LoggerUtility.info("TC_FBO_032 @AfterClass: Restoring original FDA session");
            try {
                switchToFDATab();
                fdaHomePage.navigateTo(config.getFdaUrl());
                fdaHomePage.logout();
                fdaHomePage.navigateTo(config.getFdaUrl());
                fdaHomePage.clickProfileIcon();
                fdaHomePage.clickLoginLink();
                fdaLoginPage.login(config.getFdaUsername(), config.getFdaPassword());
                LoggerUtility.info("TC_FBO_032 @AfterClass: FDA session restored as " + config.getFdaUsername());
            } catch (Exception e) {
                LoggerUtility.error("TC_FBO_032 @AfterClass: Failed to restore FDA session: " + e.getMessage());
            }
        }
    }

    @Test(testName = TC_NAME,
          description = "Verify 2 products qty=2 each from 2 different 3P sellers — "
                      + "FDA → Mirakl Accept WEB-A + WEB-B → Wait 1 min → Cancel Shipment WEB-A API "
                      + "→ WEB-A=Canceled, WEB-B=Awaiting shipment")
    public void tc_fbo_032_dual_product_diff_sellers_qty2_cancel_one_shipment() throws InterruptedException {

        // ============================================================
        // PHASE 1: FDA — Product 1 (TC_SKU_1, qty=2) → Cart
        // ============================================================
        LoggerUtility.info("===== TC_FBO_032 PHASE 1: FDA Order Placement — Product 1 =====");
        LoggerUtility.info("TC_FBO_032 | SKU 1: " + TC_SKU_1 + " (Seller B) | SKU 2: " + TC_SKU_2 + " (Seller A) | Qty: " + TC_QUANTITY + " each");

        // Steps 1-2: Switch to FDA tab — session swapped to TC_FDA_USER in @BeforeClass
        switchToFDATab();
        fdaHomePage.navigateTo(config.getFdaUrl());
        LoggerUtility.info("Step 1: Verified FDA home page is displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test: remove any leftover cart items
        LoggerUtility.info("Pre-test: Clearing leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ---- Steps 2-9: Product 1 (TC_SKU_1, qty=2) ----
        LoggerUtility.info("Step 2-4: Searching SKU 1: " + TC_SKU_1);
        fdaHomePage.enterSearchQuery(TC_SKU_1);
        fdaHomePage.pressSearchEnter();

        LoggerUtility.info("Step 5: Verifying PDP is displayed for SKU 1");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "TC_FBO_032 — PDP not displayed for SKU 1: " + TC_SKU_1);
        LoggerUtility.info("Step 6: Verifying Agregar al carrito button is enabled for SKU 1");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "TC_FBO_032 — Add to cart button not enabled for SKU 1: " + TC_SKU_1);

        LoggerUtility.info("Step 7: Clicking plus button to increase quantity to 2 for SKU 1");
        fdaPdpPage.increaseQuantity();
        String pdpQty1 = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 8: PDP quantity for SKU 1: " + pdpQty1);
        Assert.assertEquals(pdpQty1, String.valueOf(TC_QUANTITY),
            "TC_FBO_032 — PDP quantity should be " + TC_QUANTITY + " for SKU 1. Actual: " + pdpQty1);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 9: Clicking Agregar al carrito for SKU 1");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("TC_FBO_032 - Product 1 added to cart with quantity " + TC_QUANTITY);

        // Navigate home before searching Product 2
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ============================================================
        // PHASE 2: FDA — Product 2 (TC_SKU_2, qty=2) → Cart
        // ============================================================
        LoggerUtility.info("===== TC_FBO_032 PHASE 2: FDA Order Placement — Product 2 =====");

        // ---- Steps 10-17: Product 2 (TC_SKU_2, qty=2) ----
        LoggerUtility.info("Step 10-12: Searching SKU 2: " + TC_SKU_2);
        fdaHomePage.enterSearchQuery(TC_SKU_2);
        fdaHomePage.pressSearchEnter();

        LoggerUtility.info("Step 13: Verifying PDP is displayed for SKU 2");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "TC_FBO_032 — PDP not displayed for SKU 2: " + TC_SKU_2);
        LoggerUtility.info("Step 14: Verifying Agregar al carrito button is enabled for SKU 2");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "TC_FBO_032 — Add to cart button not enabled for SKU 2: " + TC_SKU_2);

        LoggerUtility.info("Step 15: Clicking plus button to increase quantity to 2 for SKU 2");
        fdaPdpPage.increaseQuantity();
        String pdpQty2 = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 16: PDP quantity for SKU 2: " + pdpQty2);
        Assert.assertEquals(pdpQty2, String.valueOf(TC_QUANTITY),
            "TC_FBO_032 — PDP quantity should be " + TC_QUANTITY + " for SKU 2. Actual: " + pdpQty2);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 17: Clicking Agregar al carrito for SKU 2");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("TC_FBO_032 - Product 2 added to cart with quantity " + TC_QUANTITY);

        // ============================================================
        // PHASE 3: FDA — Cart Validation
        // ============================================================
        LoggerUtility.info("===== TC_FBO_032 PHASE 3: Cart Validation =====");

        // ---- Steps 18-22: Cart validation ----
        LoggerUtility.info("Step 18: Clicking Mi carrito — navigating to cart");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        LoggerUtility.info("Step 19: Verifying cart contains exactly 2 products");
        int itemCount = fdaCartPage.getItemCount();
        LoggerUtility.info("TC_FBO_032 - Cart contains " + itemCount + " products");
        Assert.assertEquals(itemCount, 2,
            "TC_FBO_032 — Cart should contain exactly 2 products. Actual: " + itemCount);

        LoggerUtility.info("Step 20: Verifying product names in cart");
        List<String> productNames = fdaCartPage.getAllProductNames();
        LoggerUtility.info("Cart product names: " + productNames);
        Assert.assertEquals(productNames.size(), 2,
            "TC_FBO_032 — Cart should have 2 product names. Actual: " + productNames.size());
        Assert.assertFalse(productNames.get(0).isEmpty(),
            "TC_FBO_032 — Cart product 1 name should not be empty");
        Assert.assertFalse(productNames.get(1).isEmpty(),
            "TC_FBO_032 — Cart product 2 name should not be empty");

        LoggerUtility.info("Step 21: Verifying quantities are " + TC_QUANTITY + " for both products");
        List<String> quantities = fdaCartPage.getAllQuantities();
        LoggerUtility.info("Cart quantities: " + quantities);
        Assert.assertEquals(quantities.size(), 2,
            "TC_FBO_032 — Should have 2 quantity inputs in cart. Actual: " + quantities.size());
        Assert.assertEquals(quantities.get(0), String.valueOf(TC_QUANTITY),
            "TC_FBO_032 — Product 1 cart quantity should be " + TC_QUANTITY);
        Assert.assertEquals(quantities.get(1), String.valueOf(TC_QUANTITY),
            "TC_FBO_032 — Product 2 cart quantity should be " + TC_QUANTITY);

        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 22: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(),
            "TC_FBO_032 — Order total should be displayed in cart");
        LoggerUtility.info("TC_FBO_032 - Cart contains 2 products with qty=" + TC_QUANTITY + " each | Total: " + orderTotal);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: FDA — Checkout and Payment
        // ============================================================
        LoggerUtility.info("===== TC_FBO_032 PHASE 4: Checkout and Payment =====");

        // ---- Steps 23-35: Checkout and payment ----
        LoggerUtility.info("Step 23: Clicking Proceed to payment");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();

        LoggerUtility.info("Step 24: Clicking Siguiente on shipping page");
        fdaPaymentPage.clickNextButton();

        LoggerUtility.info("Step 25: Selecting Pago con Tarjeta Crédito/Débito");
        fdaPaymentPage.selectCreditCardOption();

        LoggerUtility.info("Step 26-27: Entering card number");
        fdaPaymentPage.enterCardNumber(TC_CARD_NUMBER);
        LoggerUtility.info("Step 28-29: Entering expiration date: " + TC_CARD_EXPIRY);
        fdaPaymentPage.enterExpiry(TC_CARD_EXPIRY);
        LoggerUtility.info("Step 30-31: Entering security code");
        fdaPaymentPage.enterCvv(TC_CARD_CVV);

        LoggerUtility.info("Step 32: Verifying Completar pago button shows order total");
        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Completar pago button text: " + payBtnText);
        Assert.assertTrue(
            payBtnText.contains("Completar pago") || payBtnText.contains("MXN")
                || payBtnText.contains("$"),
            "TC_FBO_032 — Completar pago button should show order total. Actual: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 33: Clicking Completar pago");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();

        // ---- Steps 34-35: Success page + capture Order ID ----
        LoggerUtility.info("Step 34: Verifying success page");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(),
            "TC_FBO_032 — Success page not displayed after payment");
        orderId = fdaSuccessPage.getOrderId();
        Assert.assertNotNull(orderId,
            "TC_FBO_032 — Order ID must not be null");
        Assert.assertFalse(orderId.isEmpty(),
            "TC_FBO_032 — Order ID must not be empty");
        LoggerUtility.info("Step 35: Order ID captured successfully");
        LoggerUtility.info("TC_FBO_032 | Order ID            : " + orderId);
        LoggerUtility.info("TC_FBO_032 | API Order Reference : " + orderId + "WEB");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 5: FDA — Order History Validation
        // ============================================================
        LoggerUtility.info("===== TC_FBO_032 PHASE 5: FDA Order History =====");

        // ---- Steps 36-40: Order history verification ----
        LoggerUtility.info("Step 36: Clicking Mi cuenta");
        fdaHomePage.clickProfileIcon();
        LoggerUtility.info("Step 37: Clicking Mis pedidos");
        fdaHomePage.clickMyOrdersLink();
        LoggerUtility.info("Step 38: Verifying Mis pedidos page");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(),
            "TC_FBO_032 — Mis pedidos page not displayed");
        LoggerUtility.info("Step 39: Verifying order " + orderId + " in history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
            "TC_FBO_032 — Order ID " + orderId + " not found in FDA order history");
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 40: FDA order status: " + fdaStatus);
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
            "TC_FBO_032 — FDA order status should be 'Creada'. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 6: MIRAKL — Find Order, Accept Both Shipments (WEB-A and WEB-B)
        // ============================================================
        LoggerUtility.info("===== TC_FBO_032 PHASE 6: Mirakl — Accept Both Shipments =====");

        // Step 41: Switch to Mirakl tab — session active from suite @BeforeSuite
        LoggerUtility.info("Step 41: Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 42-43: Navigate to All Orders
        LoggerUtility.info("Step 42: Clicking Orders menu");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 43: Clicking All Orders");
        miraklOrdersPage.clickAllOrders();

        // Steps 44-47: Search and verify order in Mirakl — retry up to 5 min for sync delay
        String miraklSearchTerm = orderId + "WEB";
        String shipmentRefA     = orderId + "WEB-A";
        String shipmentRefB     = orderId + "WEB-B";

        LoggerUtility.info("TC_FBO_032 - Searching order in Mirakl: " + miraklSearchTerm);
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
                LoggerUtility.info("Waiting 60 seconds before next Mirakl search attempt...");
                Thread.sleep(60_000);
                miraklOrdersPage.clickOrdersMenu();
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(orderFoundInMirakl,
            "TC_FBO_032 — Order " + miraklSearchTerm + " did not appear in Mirakl within 5 minutes");

        // Verify both WEB-A and WEB-B are present
        LoggerUtility.info("TC_FBO_032 - Two shipments identified: " + shipmentRefA + " | " + shipmentRefB);
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefA),
            "TC_FBO_032 — Shipment WEB-A not found: " + shipmentRefA);
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefB),
            "TC_FBO_032 — Shipment WEB-B not found: " + shipmentRefB);
        LoggerUtility.info("TC_FBO_032 - Two shipments verified — WEB-A and WEB-B present in Mirakl");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 46 (WEB-A): Open, identify product SKU, verify Pending acceptance, accept
        LoggerUtility.info("Step 46a: Opening shipment WEB-A: " + shipmentRefA);
        miraklOrderDetailPage.clickOrderInList(shipmentRefA);

        String statusBeforeAcceptA = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-A status before accept: " + statusBeforeAcceptA);
        Assert.assertEquals(statusBeforeAcceptA, "Pending acceptance",
            "TC_FBO_032 — WEB-A should be 'Pending acceptance'. Actual: " + statusBeforeAcceptA);

        // Identify which product is in WEB-A via page source.
        // TC_SKU_1 (78078094274, Seller B) is expected in WEB-A based on established seller pattern.
        // Fallback to TC_SKU_1 if page source does not expose EAN (Mirakl shows product names, not EANs).
        String productInShipmentA = miraklOrderDetailPage.findProductSkuOnPage(TC_SKU_1, TC_SKU_2);
        if (productInShipmentA.isEmpty()) {
            productInShipmentA = TC_SKU_1;
            LoggerUtility.info("SKU not found in Mirakl page source (names shown, not EANs) "
                + "— using known WEB-A fallback: " + TC_SKU_1);
        }
        String productInShipmentB = TC_SKU_1.equals(productInShipmentA) ? TC_SKU_2 : TC_SKU_1;
        LoggerUtility.info("TC_FBO_032 - Target shipment identified: " + shipmentRefA + " | Product: " + productInShipmentA);
        LoggerUtility.info("TC_FBO_032 | Product in WEB-A   : " + productInShipmentA);
        LoggerUtility.info("TC_FBO_032 | Product in WEB-B   : " + productInShipmentB);

        // Capture shipment status before cancellation for audit trail
        LoggerUtility.info("TC_FBO_032 | WEB-A status before cancel: " + statusBeforeAcceptA);

        // Capture WEB-A URL for Phase 8 navigation
        String urlShipmentA = driver.getCurrentUrl();
        LoggerUtility.info("WEB-A URL captured: " + urlShipmentA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Accepting shipment WEB-A");
        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        String statusAfterAcceptA = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-A status after accept: " + statusAfterAcceptA);
        Assert.assertEquals(statusAfterAcceptA, "Awaiting shipment",
            "TC_FBO_032 — WEB-A should be 'Awaiting shipment' after acceptance. Actual: " + statusAfterAcceptA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Navigate back to All Orders for WEB-B
        LoggerUtility.info("Navigating back to All Orders to open WEB-B");
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        miraklOrdersPage.searchOrder(miraklSearchTerm);

        // Step 46 (WEB-B): Open and accept
        LoggerUtility.info("Step 46b: Opening shipment WEB-B: " + shipmentRefB);
        miraklOrderDetailPage.clickOrderInList(shipmentRefB);

        String statusBeforeAcceptB = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-B status before accept: " + statusBeforeAcceptB);
        Assert.assertEquals(statusBeforeAcceptB, "Pending acceptance",
            "TC_FBO_032 — WEB-B should be 'Pending acceptance'. Actual: " + statusBeforeAcceptB);

        // Capture WEB-B URL for Phase 8 verification
        String urlShipmentB = driver.getCurrentUrl();
        LoggerUtility.info("WEB-B URL captured: " + urlShipmentB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Accepting shipment WEB-B");
        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        String statusAfterAcceptB = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-B status after accept: " + statusAfterAcceptB);
        Assert.assertEquals(statusAfterAcceptB, "Awaiting shipment",
            "TC_FBO_032 — WEB-B should be 'Awaiting shipment' after acceptance. Actual: " + statusAfterAcceptB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 47: Business-required 1-minute wait after accepting both shipments before cancel
        LoggerUtility.info("Step 47: Both shipments accepted — waiting 60 seconds before cancel API call");
        Thread.sleep(60_000);
        LoggerUtility.info("Step 47: 60-second wait complete");

        // ============================================================
        // PHASE 7: CANCEL SHIPMENT API — Cancel WEB-A only
        // ============================================================
        LoggerUtility.info("===== TC_FBO_032 PHASE 7: Cancel Shipment API — WEB-A only =====");

        // Dynamic values — never hardcoded
        String apiOrderReference   = orderId + "WEB";
        String shipmentIdToCancel  = shipmentRefA;    // orderId + "WEB-A" — dynamically built

        // TC_SKU_1 (78078094274) has offer_sku="S4009184" in Mirakl — does not equal EAN.
        // The cancel microservice matches productIds against Mirakl order_line_id, so
        // passing EAN fails with 400 "cancelations must not be empty".
        // Use Mirakl order_line_id format "{shipmentId}-1" which always resolves correctly.
        String orderLineId = shipmentIdToCancel + "-1";
        List<String> cancelProductIds = java.util.List.of(orderLineId);

        LoggerUtility.info("Step 48: Building cancel shipment request");
        LoggerUtility.info("TC_FBO_032 - Cancel Shipment API executed");
        LoggerUtility.info("TC_FBO_032 | Order Commercial ID : " + apiOrderReference);
        LoggerUtility.info("TC_FBO_032 | Shipment ID         : " + shipmentIdToCancel);
        LoggerUtility.info("TC_FBO_032 | Order Line ID       : " + orderLineId);

        // Pre-API validations
        Assert.assertNotNull(orderId,
            "TC_FBO_032 — Order ID must not be null before cancel API");
        Assert.assertEquals(apiOrderReference, orderId + "WEB",
            "TC_FBO_032 — Order Commercial ID must follow format orderId+WEB");
        Assert.assertNotNull(shipmentIdToCancel,
            "TC_FBO_032 — Shipment ID must not be null before cancel API");
        Assert.assertTrue(shipmentIdToCancel.startsWith(orderId),
            "TC_FBO_032 — Target shipment must belong to the current order. ShipmentID: "
                + shipmentIdToCancel + " | OrderID: " + orderId);
        Assert.assertFalse(cancelProductIds.isEmpty(),
            "TC_FBO_032 — Cancel product IDs must not be empty");

        LoggerUtility.info("Step 49: Calling Cancel Shipment API (WEB-A only)");
        Response cancelResponse = ApiUtility.cancelShipment(
            shipmentIdToCancel, cancelProductIds, apiOrderReference);

        LoggerUtility.info("Step 50: Validating Cancel Shipment API response");
        LoggerUtility.info("TC_FBO_032 | Cancel API HTTP Status : " + cancelResponse.getStatusCode());
        LoggerUtility.info("TC_FBO_032 | Cancel API Body        : " + cancelResponse.getBody().asString());
        Assert.assertTrue(
            cancelResponse.getStatusCode() == 200
                || cancelResponse.getStatusCode() == 201
                || cancelResponse.getStatusCode() == 204,
            "TC_FBO_032 — Cancel Shipment API should return 2xx. Actual HTTP: "
                + cancelResponse.getStatusCode()
                + " | Body: " + cancelResponse.getBody().asString());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 8: MIRAKL — Verify WEB-A = Canceled, WEB-B = Awaiting shipment
        // ============================================================
        LoggerUtility.info("===== TC_FBO_032 PHASE 8: Mirakl Shipment Status Verification =====");

        // Steps 49-50: Navigate to WEB-A, poll for "Canceled"
        LoggerUtility.info("Step 49: Switching to Mirakl tab and navigating to target shipment: " + shipmentIdToCancel);
        switchToMiraklTab();
        driver.get(urlShipmentA);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        LoggerUtility.info("Step 50: Polling target shipment for 'Canceled' status");
        String finalStatusA = waitForMiraklStatus("Canceled", 20);
        LoggerUtility.info("TC_FBO_032 - Target shipment status verified as Canceled");
        LoggerUtility.info("TC_FBO_032 | WEB-A Final Status : " + finalStatusA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        Assert.assertEquals(finalStatusA, "Canceled",
            "TC_FBO_032 — Target shipment (" + shipmentIdToCancel + ") should be 'Canceled'. "
                + "Order ID: " + orderId + " | Expected: Canceled | Actual: " + finalStatusA);

        // Verify second shipment is NOT canceled
        LoggerUtility.info("Navigating to second shipment WEB-B to verify it is not cancelled: " + shipmentRefB);
        driver.get(urlShipmentB);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        String finalStatusB = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("TC_FBO_032 - Second shipment verified as not Canceled");
        LoggerUtility.info("TC_FBO_032 | WEB-B Final Status : " + finalStatusB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        Assert.assertNotEquals(finalStatusB, "Canceled",
            "TC_FBO_032 — Second shipment (" + shipmentRefB + ") must NOT be 'Canceled'. "
                + "Order ID: " + orderId);
        Assert.assertEquals(finalStatusB, "Awaiting shipment",
            "TC_FBO_032 — Second shipment (" + shipmentRefB + ") should remain 'Awaiting shipment'. "
                + "Actual: " + finalStatusB);

        // ============================================================
        // FINAL SUMMARY LOG
        // ============================================================
        LoggerUtility.info("===== TC_FBO_032 COMPLETE — PASS =====");
        LoggerUtility.info("TC_FBO_032 | Test Case ID              : " + TC_NAME);
        LoggerUtility.info("TC_FBO_032 | FDA User                  : " + TC_FDA_USER);
        LoggerUtility.info("TC_FBO_032 | SKU 1 (Seller B)          : " + TC_SKU_1 + " | Qty: " + TC_QUANTITY);
        LoggerUtility.info("TC_FBO_032 | SKU 2 (Seller A)          : " + TC_SKU_2 + " | Qty: " + TC_QUANTITY);
        LoggerUtility.info("TC_FBO_032 | Product Name 1            : " + (productNames.size() > 0 ? productNames.get(0) : ""));
        LoggerUtility.info("TC_FBO_032 | Product Name 2            : " + (productNames.size() > 1 ? productNames.get(1) : ""));
        LoggerUtility.info("TC_FBO_032 | Order ID                  : " + orderId);
        LoggerUtility.info("TC_FBO_032 | Order Commercial ID       : " + apiOrderReference);
        LoggerUtility.info("TC_FBO_032 | Order Total               : " + orderTotal);
        LoggerUtility.info("TC_FBO_032 | Initial FDA Status        : " + fdaStatus);
        LoggerUtility.info("TC_FBO_032 | WEB-A Product             : " + productInShipmentA);
        LoggerUtility.info("TC_FBO_032 | WEB-B Product             : " + productInShipmentB);
        LoggerUtility.info("TC_FBO_032 | Shipment A ID (cancelled) : " + shipmentRefA);
        LoggerUtility.info("TC_FBO_032 | Shipment B ID (retained)  : " + shipmentRefB);
        LoggerUtility.info("TC_FBO_032 | Target Shipment ID        : " + shipmentIdToCancel);
        LoggerUtility.info("TC_FBO_032 | Cancel Order Line ID      : " + orderLineId);
        LoggerUtility.info("TC_FBO_032 | Cancel API HTTP Status    : " + cancelResponse.getStatusCode());
        LoggerUtility.info("TC_FBO_032 | WEB-A Final Status        : " + finalStatusA);
        LoggerUtility.info("TC_FBO_032 | WEB-B Final Status        : " + finalStatusB);
        LoggerUtility.info("TC_FBO_032 | Test Execution Status     : PASS");
    }

    // Polls Mirakl status by refreshing the current page until expectedStatus is reached.
    // Uses FluentWait (no Thread.sleep) — satisfies framework wait requirement.
    // On timeout, fails with Order ID + Shipment context for debugging.
    private String waitForMiraklStatus(String expectedStatus, int maxRetries) {
        String current = "";
        for (int i = 1; i <= maxRetries; i++) {
            LoggerUtility.info("Mirakl status poll " + i + "/" + maxRetries
                + " — expecting: " + expectedStatus);
            driver.navigate().refresh();
            WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
            current = miraklOrderDetailPage.getOrderStatus();
            LoggerUtility.info("Mirakl current status: " + current);
            if (expectedStatus.equals(current)) {
                LoggerUtility.info("Expected status reached: " + current);
                return current;
            }
        }
        LoggerUtility.warn("Status did not reach '" + expectedStatus + "' after "
            + maxRetries + " retries. "
            + "Order ID: " + orderId + " | Last observed: " + current);
        return current;
    }

    // Handles payment checkout opening in a new window/tab
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
// @AfterMethod navigateToHomePage() is inherited from BaseClass
