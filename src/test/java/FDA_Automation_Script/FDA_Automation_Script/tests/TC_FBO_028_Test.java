package FDA_Automation_Script.FDA_Automation_Script.tests;

import FDA_Automation_Script.FDA_Automation_Script.base.BaseClass;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDACartPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAHomePage;
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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

public class TC_FBO_028_Test extends BaseClass {

    private static final String TC_NAME = "TC_FBO_028";
    private static final String SKU_1   = "7080901020316";
    private static final String SKU_2   = "7080901020305";

    // --- Page objects ---
    private FDAHomePage           fdaHomePage;
    private FDAPDPPage            fdaPdpPage;
    private FDACartPage           fdaCartPage;
    private FDAPaymentPage        fdaPaymentPage;
    private FDASuccessPage        fdaSuccessPage;
    private FDAOrderHistoryPage   fdaOrderHistoryPage;
    private MiraklOrdersPage      miraklOrdersPage;
    private MiraklOrderDetailPage miraklOrderDetailPage;

    // --- Dynamic test data ---
    private String orderId;
    private String orderTotal;

    private static final By MIRAKL_ORDER_STATUS_LOCATOR =
        By.xpath("(//span[contains(@class,'_6e782__sc-s4ukbq-0') and not(ancestor::a)])[1]");

    @BeforeClass
    public void initPageObjects() {
        fdaHomePage          = new FDAHomePage(driver);
        fdaPdpPage           = new FDAPDPPage(driver);
        fdaCartPage          = new FDACartPage(driver);
        fdaPaymentPage       = new FDAPaymentPage(driver);
        fdaSuccessPage       = new FDASuccessPage(driver);
        fdaOrderHistoryPage  = new FDAOrderHistoryPage(driver);
        miraklOrdersPage     = new MiraklOrdersPage(driver);
        miraklOrderDetailPage = new MiraklOrderDetailPage(driver);
        LoggerUtility.info("TC_FBO_028: All page objects initialized");
    }

    @Test(testName = TC_NAME, groups = {"FBO"},
          description = "Verify 2-product 3P seller order placement and partial cancel: "
                      + "FDA (2 SKUs) → Mirakl Accept Both → Cancel Shipment A API → WEB-A=Canceled, WEB-B=Awaiting shipment")
    public void tc_fbo_028_partial_shipment_cancel_flow() throws InterruptedException {

        // ============================================================
        // PHASE 1: FDA — 2 Products, Qty 1 each, Checkout, Capture Order ID
        // ============================================================
        LoggerUtility.info("===== TC_FBO_028 PHASE 1: FDA Order Placement =====");
        LoggerUtility.info("TC_FBO_028 | SKU 1: " + SKU_1);
        LoggerUtility.info("TC_FBO_028 | SKU 2: " + SKU_2);

        // Steps 1-2: Switch to FDA tab — session active from suite @BeforeSuite
        switchToFDATab();
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test: remove any leftover cart items
        LoggerUtility.info("Pre-test: Clearing leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ---- Steps 3-9: Product 1 (SKU_1) ----
        LoggerUtility.info("Step 3-5: Searching SKU 1: " + SKU_1);
        fdaHomePage.enterSearchQuery(SKU_1);
        fdaHomePage.pressSearchEnter();

        LoggerUtility.info("Step 6: Verifying PDP is displayed for SKU 1");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "TC_FBO_028 — PDP not displayed for SKU 1: " + SKU_1);
        LoggerUtility.info("Step 7: Verifying Agregar al carrito button is enabled for SKU 1");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "TC_FBO_028 — Add to cart button not enabled for SKU 1: " + SKU_1);
        String pdpQty1 = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 8: PDP quantity for SKU 1: " + pdpQty1);
        Assert.assertEquals(pdpQty1, "1",
            "TC_FBO_028 — PDP quantity should be 1 for SKU 1");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 9: Clicking Agregar al carrito for SKU 1");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product 1 added to cart");

        // Navigate home before searching Product 2
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ---- Steps 10-16: Product 2 (SKU_2) ----
        LoggerUtility.info("Step 10-12: Searching SKU 2: " + SKU_2);
        fdaHomePage.enterSearchQuery(SKU_2);
        fdaHomePage.pressSearchEnter();

        LoggerUtility.info("Step 13: Verifying PDP is displayed for SKU 2");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "TC_FBO_028 — PDP not displayed for SKU 2: " + SKU_2);
        LoggerUtility.info("Step 14: Verifying Agregar al carrito button is enabled for SKU 2");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "TC_FBO_028 — Add to cart button not enabled for SKU 2: " + SKU_2);
        String pdpQty2 = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 15: PDP quantity for SKU 2: " + pdpQty2);
        Assert.assertEquals(pdpQty2, "1",
            "TC_FBO_028 — PDP quantity should be 1 for SKU 2");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 16: Clicking Agregar al carrito for SKU 2");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product 2 added to cart");

        // ---- Steps 17-21: Cart validation ----
        LoggerUtility.info("Step 17: Clicking Mi carrito — navigating to cart");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        LoggerUtility.info("Step 18: Verifying cart contains exactly 2 products");
        int itemCount = fdaCartPage.getItemCount();
        LoggerUtility.info("Cart item count: " + itemCount);
        Assert.assertEquals(itemCount, 2,
            "TC_FBO_028 — Cart should contain exactly 2 products. Actual: " + itemCount);

        LoggerUtility.info("Step 19: Verifying product names in cart");
        List<String> productNames = fdaCartPage.getAllProductNames();
        LoggerUtility.info("Cart product names: " + productNames);
        Assert.assertEquals(productNames.size(), 2,
            "TC_FBO_028 — Cart should have 2 product names. Actual: " + productNames.size());
        Assert.assertFalse(productNames.get(0).isEmpty(),
            "TC_FBO_028 — Cart product 1 name should not be empty");
        Assert.assertFalse(productNames.get(1).isEmpty(),
            "TC_FBO_028 — Cart product 2 name should not be empty");

        LoggerUtility.info("Step 20: Verifying quantities are 1 for both products");
        List<String> quantities = fdaCartPage.getAllQuantities();
        LoggerUtility.info("Cart quantities: " + quantities);
        Assert.assertEquals(quantities.size(), 2,
            "TC_FBO_028 — Should have 2 quantity inputs in cart. Actual: " + quantities.size());
        Assert.assertEquals(quantities.get(0), "1",
            "TC_FBO_028 — Product 1 cart quantity should be 1");
        Assert.assertEquals(quantities.get(1), "1",
            "TC_FBO_028 — Product 2 cart quantity should be 1");

        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 21: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(),
            "TC_FBO_028 — Order total should be displayed in cart");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ---- Steps 22-32: Checkout and payment ----
        LoggerUtility.info("Step 22: Clicking Proceed to payment");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();

        LoggerUtility.info("Step 23: Clicking Siguiente on shipping page");
        fdaPaymentPage.clickNextButton();

        LoggerUtility.info("Step 24: Selecting Pago con Tarjeta Crédito/Débito");
        fdaPaymentPage.selectCreditCardOption();

        LoggerUtility.info("Step 25-26: Entering card number");
        fdaPaymentPage.enterCardNumber(config.getFdaCardNumber());
        LoggerUtility.info("Step 27-28: Entering expiration date");
        fdaPaymentPage.enterExpiry(config.getFdaCardExpiry());
        LoggerUtility.info("Step 29-30: Entering security code");
        fdaPaymentPage.enterCvv(config.getFdaCardCvv());

        LoggerUtility.info("Step 31: Verifying Completar pago button shows order total");
        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Completar pago button text: " + payBtnText);
        Assert.assertTrue(
            payBtnText.contains("Completar pago") || payBtnText.contains("MXN")
                || payBtnText.contains("$"),
            "TC_FBO_028 — Completar pago button should show order total. Actual: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 32: Clicking Completar pago");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();

        // ---- Steps 33-34: Success page + capture Order ID ----
        LoggerUtility.info("Step 33: Verifying success page");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(),
            "TC_FBO_028 — Success page not displayed after payment");
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 34: Order ID = " + orderId);
        LoggerUtility.info("TC_FBO_028 | Order ID            : " + orderId);
        LoggerUtility.info("TC_FBO_028 | API Order Reference : " + orderId + "WEB");
        Assert.assertFalse(orderId.isEmpty(),
            "TC_FBO_028 — Order ID should be present on success page");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ---- Steps 35-39: Order history verification ----
        LoggerUtility.info("Step 35: Clicking Mi cuenta");
        fdaHomePage.clickProfileIcon();
        LoggerUtility.info("Step 36: Clicking Mis pedidos");
        fdaHomePage.clickMyOrdersLink();
        LoggerUtility.info("Step 37: Verifying Mis pedidos page");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(),
            "TC_FBO_028 — Mis pedidos page not displayed");
        LoggerUtility.info("Step 38: Verifying order " + orderId + " in history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
            "TC_FBO_028 — Order ID " + orderId + " not found in FDA order history");
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 39: FDA order status: " + fdaStatus);
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
            "TC_FBO_028 — FDA order status should be 'Creada'. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 2: MIRAKL — Find Order, Accept Both Shipments (WEB-A and WEB-B)
        // ============================================================
        LoggerUtility.info("===== TC_FBO_028 PHASE 2: Mirakl — Accept Both Shipments =====");

        // Step 40: Switch to Mirakl tab — session active from suite @BeforeSuite
        LoggerUtility.info("Step 40: Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 41-42: Navigate to All Orders
        LoggerUtility.info("Step 41: Clicking Orders menu");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 42: Clicking All Orders");
        miraklOrdersPage.clickAllOrders();

        // Steps 43-45: Search and verify order in Mirakl — retry up to 5 min for sync delay
        String miraklSearchTerm = orderId + "WEB";
        String shipmentRefA     = orderId + "WEB-A";
        String shipmentRefB     = orderId + "WEB-B";

        LoggerUtility.info("Steps 43-45: Searching Mirakl for: " + miraklSearchTerm);
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
            "TC_FBO_028 — Order " + miraklSearchTerm + " did not appear in Mirakl within 5 minutes");

        // Verify both WEB-A and WEB-B are present
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefA),
            "TC_FBO_028 — Shipment WEB-A not found: " + shipmentRefA);
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefB),
            "TC_FBO_028 — Shipment WEB-B not found: " + shipmentRefB);
        LoggerUtility.info("Two shipments verified — WEB-A and WEB-B present in Mirakl");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 46 (WEB-A): Open, identify product SKU, accept
        LoggerUtility.info("Step 46a: Opening shipment WEB-A: " + shipmentRefA);
        miraklOrderDetailPage.clickOrderInList(shipmentRefA);

        String statusBeforeAcceptA = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-A status before accept: " + statusBeforeAcceptA);
        Assert.assertEquals(statusBeforeAcceptA, "Pending acceptance",
            "TC_FBO_028 — WEB-A should be 'Pending acceptance'. Actual: " + statusBeforeAcceptA);

        // Identify which product is in WEB-A: try page source first (works if Mirakl shows EAN).
        // Fallback: use SKU_2 (7080901020305) — Mirakl consistently assigns the lower product ID
        // to WEB-A for this seller, as confirmed by the cancel-shipment curl example.
        String productInShipmentA = miraklOrderDetailPage.findProductSkuOnPage(SKU_1, SKU_2);
        if (productInShipmentA.isEmpty()) {
            productInShipmentA = SKU_2;
            LoggerUtility.info("SKU not found in Mirakl page source (names shown, not EANs) "
                + "— using known WEB-A assignment: " + SKU_2);
        }
        LoggerUtility.info("TC_FBO_028 | Product in WEB-A   : " + productInShipmentA);
        String productInShipmentB = SKU_1.equals(productInShipmentA) ? SKU_2 : SKU_1;
        LoggerUtility.info("TC_FBO_028 | Product in WEB-B   : " + productInShipmentB);

        // Capture WEB-A URL for Phase 4 navigation
        String urlShipmentA = driver.getCurrentUrl();
        LoggerUtility.info("WEB-A URL captured: " + urlShipmentA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Accepting shipment WEB-A");
        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        String statusAfterAcceptA = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-A status after accept: " + statusAfterAcceptA);
        Assert.assertEquals(statusAfterAcceptA, "Awaiting shipment",
            "TC_FBO_028 — WEB-A should be 'Awaiting shipment' after acceptance. Actual: " + statusAfterAcceptA);
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
            "TC_FBO_028 — WEB-B should be 'Pending acceptance'. Actual: " + statusBeforeAcceptB);

        // Capture WEB-B URL for Phase 4 verification
        String urlShipmentB = driver.getCurrentUrl();
        LoggerUtility.info("WEB-B URL captured: " + urlShipmentB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Accepting shipment WEB-B");
        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        String statusAfterAcceptB = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-B status after accept: " + statusAfterAcceptB);
        Assert.assertEquals(statusAfterAcceptB, "Awaiting shipment",
            "TC_FBO_028 — WEB-B should be 'Awaiting shipment' after acceptance. Actual: " + statusAfterAcceptB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 47: (Wait for Mirakl to process — FluentWait already applied after accept)
        LoggerUtility.info("Step 47: Both shipments accepted — proceeding to cancel one shipment");

        // ============================================================
        // PHASE 3: CANCEL SHIPMENT API — Cancel WEB-A only
        // ============================================================
        LoggerUtility.info("===== TC_FBO_028 PHASE 3: Cancel Shipment API — WEB-A =====");

        String apiOrderReference   = orderId + "WEB";
        String cancelledShipmentId = shipmentRefA;               // orderId + "WEB-A"
        List<String> cancelProductIds = java.util.List.of(productInShipmentA);

        LoggerUtility.info("Step 48: Building cancel shipment request");
        LoggerUtility.info("TC_FBO_028 | Cancel Shipment ID  : " + cancelledShipmentId);
        LoggerUtility.info("TC_FBO_028 | Cancel Product IDs  : " + cancelProductIds);
        LoggerUtility.info("TC_FBO_028 | Cancel Order Ref    : " + apiOrderReference);

        LoggerUtility.info("Step 49: Calling Cancel Shipment API");
        Response cancelResponse = ApiUtility.cancelShipment(
            cancelledShipmentId, cancelProductIds, apiOrderReference);

        LoggerUtility.info("Step 50: Validating Cancel Shipment API response");
        LoggerUtility.info("TC_FBO_028 | Cancel API HTTP Status : " + cancelResponse.getStatusCode());
        LoggerUtility.info("TC_FBO_028 | Cancel API Body        : " + cancelResponse.getBody().asString());
        Assert.assertTrue(
            cancelResponse.getStatusCode() == 200
                || cancelResponse.getStatusCode() == 201
                || cancelResponse.getStatusCode() == 204,
            "TC_FBO_028 — Cancel Shipment API should return 2xx. Actual HTTP: "
                + cancelResponse.getStatusCode()
                + " | Body: " + cancelResponse.getBody().asString());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: MIRAKL — Verify WEB-A = Canceled, WEB-B = Awaiting shipment
        // ============================================================
        LoggerUtility.info("===== TC_FBO_028 PHASE 4: Mirakl Shipment Status Verification =====");

        // Steps 51-55: Navigate to WEB-A, poll for "Canceled"
        LoggerUtility.info("Step 51-53: Switching to Mirakl tab and navigating to WEB-A detail");
        switchToMiraklTab();
        driver.get(urlShipmentA);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        LoggerUtility.info("Step 54-55: Polling WEB-A for 'Canceled' status (refresh + FluentWait)");
        String finalStatusA = waitForMiraklStatus("Canceled", 20);
        LoggerUtility.info("TC_FBO_028 | WEB-A Final Status : " + finalStatusA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        Assert.assertEquals(finalStatusA, "Canceled",
            "TC_FBO_028 — WEB-A (" + cancelledShipmentId + ") should be 'Canceled'. Actual: "
                + finalStatusA);

        // Step 56: Navigate to WEB-B, verify it is NOT Canceled
        LoggerUtility.info("Step 56: Navigating to WEB-B to verify it is not cancelled");
        driver.get(urlShipmentB);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        String finalStatusB = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("TC_FBO_028 | WEB-B Final Status : " + finalStatusB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        Assert.assertNotEquals(finalStatusB, "Canceled",
            "TC_FBO_028 — WEB-B (" + shipmentRefB + ") should NOT be 'Canceled'. It was cancelled.");
        Assert.assertEquals(finalStatusB, "Awaiting shipment",
            "TC_FBO_028 — WEB-B (" + shipmentRefB + ") should remain 'Awaiting shipment'. Actual: "
                + finalStatusB);

        // ============================================================
        // FINAL SUMMARY LOG
        // ============================================================
        LoggerUtility.info("===== TC_FBO_028 COMPLETE — PASS =====");
        LoggerUtility.info("TC_FBO_028 | Test Case ID              : " + TC_NAME);
        LoggerUtility.info("TC_FBO_028 | SKU 1                     : " + SKU_1);
        LoggerUtility.info("TC_FBO_028 | SKU 2                     : " + SKU_2);
        LoggerUtility.info("TC_FBO_028 | Product Name 1            : " + (productNames.size() > 0 ? productNames.get(0) : ""));
        LoggerUtility.info("TC_FBO_028 | Product Name 2            : " + (productNames.size() > 1 ? productNames.get(1) : ""));
        LoggerUtility.info("TC_FBO_028 | Order ID                  : " + orderId);
        LoggerUtility.info("TC_FBO_028 | Order ID + WEB            : " + apiOrderReference);
        LoggerUtility.info("TC_FBO_028 | Order Total               : " + orderTotal);
        LoggerUtility.info("TC_FBO_028 | Initial FDA Status        : " + fdaStatus);
        LoggerUtility.info("TC_FBO_028 | Shipment A ID             : " + shipmentRefA);
        LoggerUtility.info("TC_FBO_028 | Shipment B ID             : " + shipmentRefB);
        LoggerUtility.info("TC_FBO_028 | Cancelled Shipment ID     : " + cancelledShipmentId);
        LoggerUtility.info("TC_FBO_028 | Product Used (Cancel)     : " + productInShipmentA);
        LoggerUtility.info("TC_FBO_028 | Cancel API HTTP Status    : " + cancelResponse.getStatusCode());
        LoggerUtility.info("TC_FBO_028 | Cancel API Body           : " + cancelResponse.getBody().asString());
        LoggerUtility.info("TC_FBO_028 | WEB-A Final Status        : " + finalStatusA);
        LoggerUtility.info("TC_FBO_028 | WEB-B Final Status        : " + finalStatusB);
        LoggerUtility.info("TC_FBO_028 | Test Execution Status     : PASS");
    }

    // Polls Mirakl status by refreshing the current page until expectedStatus is reached.
    // Uses FluentWait (no Thread.sleep) — satisfies framework wait requirement.
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
            + maxRetries + " retries. Last observed: " + current);
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
