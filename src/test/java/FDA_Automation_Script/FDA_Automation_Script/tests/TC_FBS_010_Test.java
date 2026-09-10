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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * TC_FBS_010 — FBS (Fulfilled By Store) order lifecycle, 2 products (qty=2 each) from 1 3P
 * seller, PayPal payment:
 * FDA (PayPal) -> Mirakl Accept -> Kibo (verify delivery type = FBS)
 * -> Mirakl Invoice upload -> Mirakl DHL tracking -> Mark as Shipped -> Custom field "Entregado" -> Received.
 *
 * Combines TC_FBS_004's 2-product/qty=2-each cart flow (both products from the same 3P seller,
 * so this produces a single Mirakl shipment — no WEB-A/WEB-B split, confirmed via TC_FBS_004's
 * live run) with TC_FBS_007/008/009's PayPal payment flow. Step numbers in the log/comments below
 * are 1:1 with the 77-step manual test case; the PayPal payment section is exactly one step
 * longer than TC_FBS_004's Credit Card section (the extra "PayPal Pagar" click), so everything
 * from "Proceed to payment" onward is shifted +1 vs TC_FBS_004's step numbers.
 *
 * The manual case's Step 35 ("verify order total for both products qty=2 each is displayed on the
 * Completar pago button") describes the Credit Card button-text check TC_FBS_004 performs — no
 * equivalent button exists on PayPal's review page in the live-confirmed working flow
 * (TC_FBS_007/008/009), so this logs the cart total captured earlier instead of asserting on a
 * PayPal-side element that was never observed to exist.
 *
 * Belongs to the "FBS" TestNG group. BaseClass logs in to FDA once via @BeforeGroups("FBS") using
 * fbs.username/fbs.password before the first FBS test runs, and logs out once via @AfterGroups("FBS")
 * after the last one — no per-test-class login/logout here. Mirakl uses the shared session from
 * @BeforeSuite (mirakl.username/mirakl.password already matches the account this TC requires).
 */
public class TC_FBS_010_Test extends BaseClass {

    private static final String TC_NAME = "TC_FBS_010";

    // Fixed business constants for the FBS flow (not account/test data — not sourced from config)
    private static final String TC_CARRIER       = "DHL";
    private static final String TC_DOCUMENT_TYPE = "Invoice";
    private static final String TC_DELIVERY_TYPE = "FBS";
    private static final int    TC_QUANTITY      = 2;

    // PayPal sandbox test data — same sandbox account already used by TC_FBO_007/TC_FBS_007/008/009
    private static final String TC_PAYPAL_EMAIL = "sb-1dwgi10803338@personal.example.com";
    private static final String TC_PAYPAL_PASS  = "3+UEw1Q!";

    // --- Page Objects ---
    private FDAHomePage           fdaHomePage;
    private FDALoginPage          fdaLoginPage;
    private FDAPDPPage            fdaPdpPage;
    private FDACartPage           fdaCartPage;
    private FDAPaymentPage        fdaPaymentPage;
    private FDAPayPalPage         fdaPayPalPage;
    private FDASuccessPage        fdaSuccessPage;
    private FDAOrderHistoryPage   fdaOrderHistoryPage;
    private MiraklLoginPage       miraklLoginPage;
    private MiraklOrdersPage      miraklOrdersPage;
    private MiraklOrderDetailPage miraklOrderDetailPage;

    // --- Dynamic Test Data (captured once, reused everywhere) ---
    private String orderId;
    private String orderTotal;
    private String trackingNumber;
    private String paypalWindowHandle;

    private static final By MIRAKL_ORDER_STATUS_LOCATOR =
        By.xpath("(//span[contains(@class,'_6e782__sc-s4ukbq-0') and not(ancestor::a)])[1]");

    @BeforeClass
    public void initPageObjects() {
        fdaHomePage           = new FDAHomePage(driver);
        fdaLoginPage          = new FDALoginPage(driver);
        fdaPdpPage            = new FDAPDPPage(driver);
        fdaCartPage           = new FDACartPage(driver);
        fdaPaymentPage        = new FDAPaymentPage(driver);
        fdaPayPalPage         = new FDAPayPalPage(driver);
        fdaSuccessPage        = new FDASuccessPage(driver);
        fdaOrderHistoryPage   = new FDAOrderHistoryPage(driver);
        miraklLoginPage       = new MiraklLoginPage(driver);
        miraklOrdersPage      = new MiraklOrdersPage(driver);
        miraklOrderDetailPage = new MiraklOrderDetailPage(driver);
        LoggerUtility.info("All page objects initialized for TC_FBS_010");
    }

    @Test(testName = TC_NAME, groups = {"FBS"},
          description = "Verify FBS order placement (2 products qty=2 each, 1 3P seller, PayPal payment) and full fulfilment lifecycle: FDA -> Mirakl Accept -> Kibo (FBS) -> Documents/Tracking -> Shipped -> Received")
    public void tc_fbs_010_place_order_2products_qty2_paypal_fbs_full_fulfillment() throws InterruptedException {

        // Step 56's delivery-type check must not abort the run: the flow needs to keep going
        // through Documents/Tracking/Mark as Shipped/Entregado/Received even if Kibo hasn't
        // surfaced deliveryType=FBS yet. Recorded here and asserted only at the end.
        SoftAssert softAssert = new SoftAssert();

        // ============================================================
        // PHASE 1: FDA — Search, PDP, Qty=2 each, Cart (2 products), PayPal Payment, Order
        // ============================================================

        switchToFDATab();
        LoggerUtility.info("TC_FBS_010: Starting — FDA session active from @BeforeGroups(FBS), Mirakl session active from suite setup");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test cleanup: remove any cart items left over from previous runs (not a spec step)
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        String sku1 = config.getFbs010Sku1();
        String sku2 = config.getFbs010Sku2();

        // Steps 1-3: Search first product
        LoggerUtility.info("Step 1: Clicking ¿Qué estás buscando? search field");
        LoggerUtility.info("Step 2: Entering first product SKU: " + sku1);
        fdaHomePage.enterSearchQuery(sku1);
        LoggerUtility.info("Step 3: Pressing Enter on the keyboard");
        fdaHomePage.pressSearchEnter();

        // Steps 4-6: PDP validations for product 1
        LoggerUtility.info("Step 4: Verifying PDP for first product is displayed");
        Assert.assertTrue(fdaPdpPage.isDisplayed(), "PDP not displayed for first product SKU: " + sku1);
        LoggerUtility.info("Step 5: Verifying Agregar al carrito button is enabled");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(), "Add to cart button is not enabled on PDP for SKU: " + sku1);
        LoggerUtility.info("Step 6: Verifying first product quantity is 1");
        Assert.assertEquals(fdaPdpPage.getQuantity(), "1", "Product quantity on PDP should be 1 for SKU: " + sku1);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 7-8: Increase first product quantity from 1 to 2
        LoggerUtility.info("Step 7: Clicking plus button to increase first product quantity from 1 to " + TC_QUANTITY);
        fdaPdpPage.increaseQuantity();
        String pdpQty1 = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 8: Verifying first product quantity is " + TC_QUANTITY + ". Actual: " + pdpQty1);
        Assert.assertEquals(pdpQty1, String.valueOf(TC_QUANTITY),
                "First product quantity on PDP should be " + TC_QUANTITY + " after clicking plus. Actual: " + pdpQty1);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 9: Add first product to cart
        LoggerUtility.info("Step 9: Clicking Agregar al carrito for first product");
        fdaPdpPage.clickAddToCart();

        // Navigate home before searching the second product
        fdaHomePage.navigateTo(config.getFdaUrl());

        // Steps 10-12: Search second product
        LoggerUtility.info("Step 10: Clicking ¿Qué estás buscando? search field");
        LoggerUtility.info("Step 11: Entering second product SKU: " + sku2);
        fdaHomePage.enterSearchQuery(sku2);
        LoggerUtility.info("Step 12: Pressing Enter on the keyboard");
        fdaHomePage.pressSearchEnter();

        // Steps 13-15: PDP validations for product 2
        LoggerUtility.info("Step 13: Verifying PDP for second product is displayed");
        Assert.assertTrue(fdaPdpPage.isDisplayed(), "PDP not displayed for second product SKU: " + sku2);
        LoggerUtility.info("Step 14: Verifying Agregar al carrito button is enabled");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(), "Add to cart button is not enabled on PDP for SKU: " + sku2);
        LoggerUtility.info("Step 15: Verifying second product quantity is 1");
        Assert.assertEquals(fdaPdpPage.getQuantity(), "1", "Product quantity on PDP should be 1 for SKU: " + sku2);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 16-17: Increase second product quantity from 1 to 2
        LoggerUtility.info("Step 16: Clicking plus button to increase second product quantity from 1 to " + TC_QUANTITY);
        fdaPdpPage.increaseQuantity();
        String pdpQty2 = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 17: Verifying second product quantity is " + TC_QUANTITY + ". Actual: " + pdpQty2);
        Assert.assertEquals(pdpQty2, String.valueOf(TC_QUANTITY),
                "Second product quantity on PDP should be " + TC_QUANTITY + " after clicking plus. Actual: " + pdpQty2);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 18: Add second product to cart
        LoggerUtility.info("Step 18: Clicking Agregar al carrito for second product");
        fdaPdpPage.clickAddToCart();

        // Step 19: Open cart
        LoggerUtility.info("Step 19: Clicking Mi carrito cart icon");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        // Steps 20-23: Verify both products present with qty 2 each
        List<String> cartProductNames = fdaCartPage.getAllProductNames();
        LoggerUtility.info("Step 20-21: Cart product names: " + cartProductNames);
        Assert.assertEquals(cartProductNames.size(), 2, "Cart should contain exactly 2 product names");
        Assert.assertFalse(cartProductNames.get(0).isEmpty(), "First product name should be displayed in cart");
        Assert.assertFalse(cartProductNames.get(1).isEmpty(), "Second product name should be displayed in cart");

        List<String> cartQuantities = fdaCartPage.getAllQuantities();
        LoggerUtility.info("Step 22-23: Cart quantities: " + cartQuantities);
        Assert.assertEquals(cartQuantities.size(), 2, "Cart should contain exactly 2 quantity inputs");
        Assert.assertEquals(cartQuantities.get(0), String.valueOf(TC_QUANTITY), "First product cart quantity should be " + TC_QUANTITY);
        Assert.assertEquals(cartQuantities.get(1), String.valueOf(TC_QUANTITY), "Second product cart quantity should be " + TC_QUANTITY);

        // Step 24: Order total for both products with quantity 2 each
        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 24: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(), "Order total should be displayed in cart");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 25: Proceed to payment
        LoggerUtility.info("Step 25: Clicking Proceed to payment button");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();

        // Step 26: Siguiente on shipping page
        LoggerUtility.info("Step 26: Clicking Siguiente button on the Shipping page");
        fdaPaymentPage.clickNextButton();

        // Step 27: Select PayPal radio button
        LoggerUtility.info("Step 27: Selecting PayPal radio button on the Payment page");
        fdaPaymentPage.selectPayPalOption();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Capture existing window handles before PayPal popup opens
        Set<String> handlesBeforePayPal = new HashSet<>(driver.getWindowHandles());
        LoggerUtility.info("Window handles before PayPal click: " + handlesBeforePayPal.size());

        // Step 28: Click PayPal Pagar button
        LoggerUtility.info("Step 28: Clicking PayPal Pagar button");
        fdaPaymentPage.clickPayPalButton();

        // Switch to PayPal popup window
        LoggerUtility.info("Detecting and switching to PayPal popup window");
        switchToPayPalWindow(handlesBeforePayPal);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        fdaPayPalPage.waitForPageLoad();

        // Steps 29-32: Email + Password login — PayPal remembers a logged-in session within the
        // same browser (confirmed via a live run, 2026-09-09): the 2nd+ PayPal checkout in one
        // browser session skips straight to the "Pagar con" funding-source screen with no
        // email/password fields at all, so these steps only run if PayPal actually shows the
        // login form.
        if (fdaPayPalPage.isLoginScreenDisplayed()) {
            LoggerUtility.info("Step 29: Clicking Email address or cell phone number field");
            fdaPayPalPage.clickEmailField();
            LoggerUtility.info("Step 30: Entering valid PayPal email (value masked in logs)");
            fdaPayPalPage.enterEmail(TC_PAYPAL_EMAIL);
            fdaPayPalPage.clickNextButton();
            ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

            LoggerUtility.info("Step 31: Clicking Password field");
            fdaPayPalPage.clickPasswordField();
            if (TC_PAYPAL_PASS == null || TC_PAYPAL_PASS.isBlank()) {
                ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.FAIL);
                Assert.fail(TC_NAME + ": PayPal password is BLANK in the supplied test data. "
                        + "The test cannot proceed past PayPal login without a valid credential. "
                        + "Update the TC_PAYPAL_PASS constant in " + TC_NAME + "_Test.java.");
            }
            LoggerUtility.info("Step 32: Entering valid PayPal password (value masked in logs)");
            fdaPayPalPage.enterPassword(TC_PAYPAL_PASS);
            fdaPayPalPage.clickLoginButton();
            ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        } else {
            LoggerUtility.info("Steps 29-32: PayPal already recognized this browser session — "
                    + "skipping email/password login, proceeding directly to funding-source selection");
        }

        // Step 33: Select Visa radio button under "Pay with" section (if present — PayPal sandbox
        // does not always show a funding-source picker, so this is best-effort like the other
        // optional payment-method UI elements elsewhere in this codebase)
        LoggerUtility.info("Step 33: Selecting Visa radio button under Pay with section");
        fdaPayPalPage.selectVisaOption();

        // Step 34: Full purchase button
        LoggerUtility.info("Step 34: Clicking Full purchase button");
        fdaPayPalPage.clickFullPurchaseButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 35 (manual case): verify order total for both products qty=2 each is shown on the
        // "Completar pago" button — that button/text only exists on the Credit Card path
        // (TC_FBS_004); the live-confirmed PayPal flow (TC_FBS_007/008/009) has no equivalent
        // element on PayPal's review page, so this logs the cart total captured in Step 24
        // instead of asserting on an element that was never observed to exist for PayPal orders.
        LoggerUtility.info("Step 35: Order total for both products qty=" + TC_QUANTITY + " each (captured in cart): " + orderTotal
                + " — PayPal review page has no equivalent 'Completar pago (MXN$...)' button to re-verify this against");

        // Step 36: Click Compra completa — this is the button that actually finalizes the
        // PayPal-side authorization (same button TC_FBO_007/TC_FBS_007/008/009 click); it closes
        // the popup and returns control to FDA's checkout page with the order already completed.
        // The FDA/Adyen "Completar pago" button used by the Credit Card path does not exist once
        // PayPal is the selected payment method, so it must not be clicked here (confirmed via a
        // live TC_FBS_007 run — clicking it threw NoSuchElementException).
        LoggerUtility.info("Step 36: Clicking Compra completa button on PayPal review page");
        fdaPayPalPage.clickCompletePayment();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Wait for PayPal popup to close and switch back to the FDA checkout window
        LoggerUtility.info("Waiting for PayPal window to close and switching back to FDA");
        waitForPayPalWindowClose();

        // Steps 37-38: Success page → get order ID
        LoggerUtility.info("Step 37: Verifying order success page is displayed");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(), "Order success page not displayed after PayPal payment");
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 38: Order ID = " + orderId);
        Assert.assertFalse(orderId.isEmpty(), "Order ID should be present on success page");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 39-43: Order history
        LoggerUtility.info("Step 39: Clicking Mi cuenta profile icon");
        fdaHomePage.clickProfileIcon();
        LoggerUtility.info("Step 40: Clicking Mis pedidos link");
        fdaHomePage.clickMyOrdersLink();
        LoggerUtility.info("Step 41: Verifying Mis pedidos page is displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(), "Mis pedidos page not displayed");
        LoggerUtility.info("Step 42: Verifying Order ID " + orderId + " is present in order history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
                "Order ID " + orderId + " not found in order history");
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 43: FDA order status: " + fdaStatus + " (expected 'Creada')");
        // PayPal orders can land on "Procesando" instead of "Creada"/"Pendiente" — same tolerance
        // TC_FBS_007/008/009's PayPal flow already applies; the Credit Card FBS tests don't see
        // this status because payment finalizes synchronously on the FDA page for them.
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente") || fdaStatus.equals("Procesando"),
                "Order status should be 'Creada', 'Pendiente', or 'Procesando' in FDA order history. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 2: MIRAKL — Search Order, Pending Acceptance, Accept
        // ============================================================

        LoggerUtility.info("Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 44-45: Navigate to All Orders
        LoggerUtility.info("Step 44: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 45: Clicking All Orders link");
        miraklOrdersPage.clickAllOrders();

        // Steps 46-48: Search for order in Mirakl — retry every 60s up to 5 minutes for sync delay.
        // 1 3P seller = 1 shipment, so search directly with the "WEB-A" shipment ref rather than
        // the generic "orderId+WEB" term (Kibo's externalId lookup below stays as plain "WEB" —
        // Kibo's externalOrderId field never carries the Mirakl "-A" shipment suffix).
        String miraklSearchTerm = orderId + "WEB-A";
        LoggerUtility.info("Step 46-47: Clicking Search field and entering Order ID: " + miraklSearchTerm);
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
        String miraklListStatus = miraklOrdersPage.getFirstOrderStatus();
        LoggerUtility.info("Step 48: Mirakl Status = " + miraklListStatus + " (expected 'Pending acceptance')");
        // Same auto-accept tolerance already established for TC_FBS_001-009: this shop's orders
        // can already be auto-accepted by the time the 5-minute Mirakl sync window elapses.
        Assert.assertTrue(miraklListStatus.equals("Pending acceptance") || miraklListStatus.equals("Awaiting shipment"),
                "Mirakl order status should be 'Pending acceptance' (or already 'Awaiting shipment' if auto-accepted). Actual: " + miraklListStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 49-50: Click into detail → verify total
        LoggerUtility.info("Step 49: Clicking on the Order ID in the search result list");
        miraklOrderDetailPage.clickOrderInList(miraklSearchTerm);
        String miraklTotal = miraklOrderDetailPage.getOrderTotal();
        LoggerUtility.info("Step 50: Mirakl order total: " + miraklTotal + " | FDA order total: " + orderTotal);
        Assert.assertFalse(miraklTotal.isEmpty(), "Mirakl order total should be displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 51-52: Accept order → Awaiting shipment (skip the click if already auto-accepted)
        String miraklStatus;
        if (miraklOrderDetailPage.isAcceptButtonPresent(5)) {
            LoggerUtility.info("Step 51: Clicking Accept button on the Order Details page in Mirakl");
            miraklOrderDetailPage.clickAcceptButton();
            LoggerUtility.info("Refreshing Mirakl order details page after acceptance");
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            miraklStatus = miraklOrderDetailPage.getOrderStatus();
        } else {
            LoggerUtility.info("Step 51: Accept button not present — order was already auto-accepted");
            miraklStatus = miraklOrderDetailPage.getOrderStatus();
        }
        LoggerUtility.info("Step 52: Mirakl Status = " + miraklStatus + " (expected 'Awaiting shipment')");
        Assert.assertEquals(miraklStatus, "Awaiting shipment",
                "Mirakl order status should be 'Awaiting shipment' after acceptance. Actual: " + miraklStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: KIBO API — Authenticate + Verify Delivery Type = FBS
        // ============================================================

        LoggerUtility.info("Step 53: ========== Kibo_Auth ==========");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(), "Kibo authentication failed — access token is empty");

        String externalId = orderId + "WEB";
        LoggerUtility.info("Step 54: ========== Get_all_orders_in_Kibo ==========");
        LoggerUtility.info("Searching externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(), "Kibo Order ID not found for externalId: " + externalId);
        LoggerUtility.info("Kibo Order ID : " + kiboOrderId);

        LoggerUtility.info("Step 55: ========== Get_Shipment_Details ==========");
        String deliveryType = "";
        for (int attempt = 1; attempt <= 5; attempt++) {
            LoggerUtility.info("Kibo shipment data poll attempt " + attempt + "/5");
            Response shipmentsResp = ApiUtility.kiboGetShipmentsByOrderId(kiboToken, kiboOrderId);
            Assert.assertEquals(shipmentsResp.getStatusCode(), 200,
                    "Kibo Get Shipment Details should return 200. Actual: " + shipmentsResp.getStatusCode());

            List<Map<String, Object>> shipmentList = shipmentsResp.jsonPath().getList("items");
            if (shipmentList == null || shipmentList.isEmpty()) {
                shipmentList = shipmentsResp.jsonPath().getList("_embedded.shipments");
            }
            if (shipmentList != null && !shipmentList.isEmpty()) {
                deliveryType = extractCustomField(shipmentList.get(0), "deliveryType");
                if (!deliveryType.isEmpty()) break;
            }
            LoggerUtility.info("Delivery type not yet available. Waiting 15s...");
            if (attempt < 5) Thread.sleep(15_000);
        }
        // Step 56: Verify delivery type is FBS (soft assert — must not stop the run before
        // the Mirakl documents/tracking/shipped/received steps complete; failure is reported
        // by softAssert.assertAll() at the end).
        LoggerUtility.info("Step 56: Kibo delivery type = " + deliveryType + " (expected 'FBS')");
        softAssert.assertEquals(deliveryType.toUpperCase(), TC_DELIVERY_TYPE,
                "Kibo shipment delivery type should be 'FBS'. Actual: " + deliveryType);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: MIRAKL — Documents, Tracking, Mark as Shipped
        // ============================================================

        // Steps 57-60: More actions -> Documents -> Add -> upload popup
        LoggerUtility.info("Step 57: Clicking More actions dropdown");
        miraklOrderDetailPage.clickMoreActionsDropdown();
        LoggerUtility.info("Step 58: Clicking Documents option under More actions");
        miraklOrderDetailPage.clickDocumentsOption();
        LoggerUtility.info("Step 59: Clicking the blue Add button in the Order documents section");
        miraklOrderDetailPage.clickAddDocumentButton();
        LoggerUtility.info("Step 60: Verifying Upload an order document popup is displayed");
        Assert.assertTrue(miraklOrderDetailPage.isUploadDocumentPopupDisplayed(),
                "Upload an order document popup should be displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 61-64: Select Invoice type, upload file, confirm
        LoggerUtility.info("Step 61-62: Clicking Document type dropdown and selecting " + TC_DOCUMENT_TYPE);
        miraklOrderDetailPage.selectDocumentType(TC_DOCUMENT_TYPE);
        LoggerUtility.info("Step 63: Selecting the invoice PDF file: " + config.getFbs010InvoiceFilePath());
        miraklOrderDetailPage.uploadDocumentFile(config.getFbs010InvoiceFilePath());
        LoggerUtility.info("Step 64: Clicking Confirm button");
        miraklOrderDetailPage.clickConfirmUploadButton();
        Assert.assertTrue(miraklOrderDetailPage.isDocumentUploadedMessageDisplayed(),
                "'The document has been uploaded.' confirmation message should be displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // (Fix — not a numbered step in the manual test case) Clicking More actions > Documents
        // navigates away from the Order Detail page to a separate "Order documents" page.
        // "Add tracking information" and "Mark as Shipped" only exist on the Order Detail page,
        // so we must navigate back the same way we got here the first time before Step 65 can
        // find anything (same fix already applied in TC_FBS_001-009).
        LoggerUtility.info("Navigating back to Mirakl Order Detail page after document upload");
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        miraklOrdersPage.searchOrder(miraklSearchTerm);
        miraklOrderDetailPage.clickOrderInList(miraklSearchTerm);
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 65-70: Add tracking information (DHL + tracking number)
        LoggerUtility.info("Step 65: Clicking Add tracking information link");
        miraklOrderDetailPage.clickAddTrackingInformationLink();
        LoggerUtility.info("Step 66-67: Clicking Select a carrier dropdown and selecting " + TC_CARRIER);
        miraklOrderDetailPage.selectCarrier(TC_CARRIER);
        trackingNumber = generateEightDigitTrackingNumber();
        LoggerUtility.info("Step 68-69: Clicking Tracking number field and entering: " + trackingNumber);
        miraklOrderDetailPage.enterTrackingNumber(trackingNumber);
        LoggerUtility.info("Step 70: Clicking Add button");
        miraklOrderDetailPage.clickAddTrackingButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 71: Verify the selected Carrier Name and Tracking Number are displayed
        LoggerUtility.info("Step 71: Clicking View tracking information link and verifying Carrier/Tracking Number");
        miraklOrderDetailPage.clickViewTrackingInformationLink();
        String displayedCarrier = miraklOrderDetailPage.getTrackingDialogCarrierName();
        Assert.assertTrue(displayedCarrier.toUpperCase().contains(TC_CARRIER),
                "Displayed carrier should be DHL. Actual: " + displayedCarrier);
        String displayedTracking = miraklOrderDetailPage.getTrackingDialogTrackingNumber();
        Assert.assertEquals(displayedTracking, trackingNumber,
                "Displayed tracking number should match entered value. Actual: " + displayedTracking);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        miraklOrderDetailPage.closeTrackingInfoDialog();

        // Step 72: Mark as Shipped
        LoggerUtility.info("Step 72: Clicking Mark as Shipped button");
        miraklOrderDetailPage.clickMarkAsShippedButton();
        String shippedStatus = waitForMiraklStatus("Shipped", 6);
        LoggerUtility.info("Mirakl status after Mark as Shipped: " + shippedStatus);
        Assert.assertEquals(shippedStatus, "Shipped",
                "Mirakl order status should be 'Shipped'. Actual: " + shippedStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 5: MIRAKL — Custom field "Entregado" → Received
        // ============================================================

        LoggerUtility.info("Step 73: Clicking More actions dropdown");
        miraklOrderDetailPage.clickMoreActionsDropdown();
        LoggerUtility.info("Step 74: Clicking Custom field button");
        miraklOrderDetailPage.clickCustomFieldButton();
        LoggerUtility.info("Step 75: Clicking Entregado option");
        miraklOrderDetailPage.clickEntregadoOption();
        miraklOrderDetailPage.clickEntregadoValueDropdown();
        LoggerUtility.info("Selecting 'Yes' from Entregado dropdown");
        miraklOrderDetailPage.selectEntregadoYes();
        LoggerUtility.info("Step 76: Clicking Confirm button");
        miraklOrderDetailPage.clickCustomFieldConfirmButton();

        // Wait 30s for the Entregado -> Received transition to propagate on the backend before
        // polling (same fix already applied in TC_FBS_001-009 after live runs showed 10s/no-sleep
        // isn't reliably enough time even though the click sequence completes with no errors).
        LoggerUtility.info("Waiting 30s for Entregado update to propagate");
        Thread.sleep(30_000);

        // Step 77: Verify status changes from Shipped to Received
        String finalStatus = waitForMiraklStatus("Received", 20);
        LoggerUtility.info("Step 77: Mirakl Status = " + finalStatus + " (expected 'Received')");
        Assert.assertEquals(finalStatus, "Received",
                "Mirakl order status should change from 'Shipped' to 'Received'. Actual: " + finalStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);

        // ============================================================
        // PHASE 6: FDA — Re-verify order in Mis pedidos after Mirakl Received
        // ============================================================

        LoggerUtility.info("Switching to FDA tab to verify order in Mis pedidos after Received");
        switchToFDATab();
        fdaHomePage.navigateTo(config.getFdaUrl());
        fdaHomePage.clickProfileIcon();
        fdaHomePage.clickMyOrdersLink();
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(), "Mis pedidos page not displayed after Received");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
                "Order ID " + orderId + " not found in FDA order history after Received");
        String finalFdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Final FDA order status after Mirakl Received: " + finalFdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("TC_FBS_010 execution completed successfully");

        // All 77 steps have run; now surface any soft-assertion failures (e.g. Step 56 delivery
        // type) so the test still fails correctly if something was wrong.
        softAssert.assertAll();
    }

    // ----------------------------------------------------------------
    // Poll helper: refresh + read status up to maxRetries times. Adds a 10s sleep between
    // retries (same fix TC_FBO_030/TC_FBS_007/008/009 already apply for slow post-acceptance
    // status transitions) — a live TC_FBS_007 run showed the Entregado -> Received transition
    // can outlast the ~80s of back-to-back refreshes that 20 retries alone give you.
    // ----------------------------------------------------------------
    private String waitForMiraklStatus(String expected, int maxRetries) {
        String status = "";
        for (int i = 1; i <= maxRetries; i++) {
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            status = miraklOrderDetailPage.getOrderStatus();
            LoggerUtility.info("Mirakl status check attempt " + i + "/" + maxRetries + ": " + status);
            if (expected.equals(status)) break;
            if (i < maxRetries) {
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return status;
    }

    // ----------------------------------------------------------------
    // Extract a named custom field from a Kibo shipment item map.
    // Searches: shipment.data, shipment.packages[*].data, then shipment.items[*].data —
    // the FDA_Custom_STH_WF_V1.0 workflow puts deliveryType on the line item's data map
    // (shipment.items[*].data.deliveryType), not on the shipment or its packages. With 2 line
    // items in this shipment, the loop below checks each one in turn.
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
        Object lineItemsRaw = item.get("items");
        if (lineItemsRaw instanceof List) {
            for (Object lineItem : (List<?>) lineItemsRaw) {
                if (lineItem instanceof Map) {
                    Object lineItemData = ((Map<String, Object>) lineItem).get("data");
                    if (lineItemData instanceof Map) {
                        Object val = ((Map<String, Object>) lineItemData).get(fieldKey);
                        if (val != null && !val.toString().isBlank()) return val.toString().trim();
                    }
                }
            }
        }
        return "";
    }

    // ----------------------------------------------------------------
    // Generates a random 8-digit tracking number (manual step 69 requirement)
    // ----------------------------------------------------------------
    private String generateEightDigitTrackingNumber() {
        int number = 10_000_000 + new Random().nextInt(90_000_000);
        return String.valueOf(number);
    }

    // ----------------------------------------------------------------
    // Handle checkout opening in new window/tab after clickProceedToPayment.
    // Polls up to 20s for the window-handle set to settle before deciding which window to
    // switch to — a single immediate read here is a race condition that caused TC_FBS_004 to
    // get stuck operating on a stale, soon-to-close tab (see CLAUDE.md).
    // ----------------------------------------------------------------
    private void switchToCheckoutWindow() {
        String newHandle = null;
        Set<String> handles = driver.getWindowHandles();
        for (int attempt = 1; attempt <= 20; attempt++) {
            handles = driver.getWindowHandles();
            for (String h : handles) {
                if (!h.equals(fdaTabHandle) && !h.equals(miraklTabHandle)) {
                    newHandle = h;
                    break;
                }
            }
            if (newHandle != null || !handles.contains(fdaTabHandle)) break;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        LoggerUtility.info("Window handles after clickProceedToPayment (settled): " + handles.size());
        if (newHandle != null) {
            driver.switchTo().window(newHandle);
            fdaTabHandle = newHandle;
            LoggerUtility.info("Checkout opened in new window — switched to: " + fdaTabHandle);
        } else if (!handles.contains(fdaTabHandle)) {
            fdaTabHandle = handles.iterator().next();
            driver.switchTo().window(fdaTabHandle);
            LoggerUtility.info("Original FDA window closed — switched to checkout window: " + fdaTabHandle);
        } else {
            driver.switchTo().window(fdaTabHandle);
            LoggerUtility.info("Checkout stayed in original FDA window: " + fdaTabHandle);
        }
    }

    // ----------------------------------------------------------------
    // Detect the new PayPal popup window and switch to it.
    // handlesBeforePayPal = set of window handles captured before clicking PayPal button.
    // ----------------------------------------------------------------
    private void switchToPayPalWindow(Set<String> handlesBeforePayPal) {
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(d -> {
                    for (String h : d.getWindowHandles()) {
                        if (!handlesBeforePayPal.contains(h)) return true;
                    }
                    return false;
                });

        for (String handle : driver.getWindowHandles()) {
            if (!handlesBeforePayPal.contains(handle)) {
                paypalWindowHandle = handle;
                driver.switchTo().window(paypalWindowHandle);
                LoggerUtility.info("Switched to PayPal popup window. Handle: " + paypalWindowHandle);
                LoggerUtility.info("PayPal URL: " + driver.getCurrentUrl());
                return;
            }
        }
        Assert.fail(TC_NAME + ": PayPal popup window did not open within 15 seconds after clicking PayPal button");
    }

    // ----------------------------------------------------------------
    // Wait for PayPal popup to close, then switch back to the FDA checkout window.
    // ----------------------------------------------------------------
    private void waitForPayPalWindowClose() {
        if (paypalWindowHandle != null) {
            try {
                new WebDriverWait(driver, Duration.ofSeconds(60))
                        .until(ExpectedConditions.not(
                                d -> d.getWindowHandles().contains(paypalWindowHandle)));
                LoggerUtility.info("PayPal popup window closed");
            } catch (Exception e) {
                LoggerUtility.warn(TC_NAME + ": PayPal window close wait timed out — " + e.getMessage());
            }
        }
        driver.switchTo().window(fdaTabHandle);
        LoggerUtility.info("Switched back to FDA checkout/success window: " + fdaTabHandle);
    }
}
// @AfterMethod navigateToHomePage() is inherited from BaseClass
