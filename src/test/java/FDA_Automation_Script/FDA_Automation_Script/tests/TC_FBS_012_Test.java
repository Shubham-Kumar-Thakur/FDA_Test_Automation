package FDA_Automation_Script.FDA_Automation_Script.tests;

import FDA_Automation_Script.FDA_Automation_Script.base.BaseClass;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.*;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklLoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklOrderDetailPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklOrdersPage;
import FDA_Automation_Script.FDA_Automation_Script.utils.ApiUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
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
 * TC_FBS_012 — FBS (Fulfilled By Store) order lifecycle, 2 products (qty=2 each) from
 * 2 DIFFERENT 3P sellers, PayPal payment.
 *
 * Combines TC_FBS_006's dual-shipment flow (2 different sellers → 2 Mirakl shipments, WEB-A +
 * WEB-B, each independently run through Accept -> Kibo deliveryType check -> Documents ->
 * Tracking -> Mark as Shipped -> Entregado -> Received; PDP quantity increased 1->2 for both
 * products) with TC_FBS_007-011's PayPal payment flow. Different-seller splitting is structural
 * in Mirakl (confirmed across every 2-different-seller FBS/FBO test in this codebase), not a
 * quantity-driven assumption.
 *
 * NOTE on SKUs: same discrepancy as TC_FBS_011 — the manual test case's supplied SKU pair
 * (32982398137/32982398136) is the confirmed SAME-SELLER pairing already used by
 * TC_FBS_002/004/008/010, which would land as a single shipment, contradicting the case's
 * "two 3P Seller" title. Per the same resolution already confirmed for TC_FBS_011, this TC uses
 * TC_FBS_006's known different-seller pairing instead (32982398137 + 4892828010) so it actually
 * exercises the dual-shipment path.
 *
 * Step numbers in the log/comments below are 1:1 with the 77-step manual test case; the PayPal
 * payment section is exactly one step longer than TC_FBS_006's Credit Card section (extra
 * "PayPal Pagar" click), so everything from "Proceed to payment" onward is shifted +1 vs
 * TC_FBS_006's step numbers. The Mirakl/Kibo/Documents/Tracking/Shipped/Entregado phase (run
 * independently per shipment via runFbsFulfillmentFlow) is copied from TC_FBS_006 essentially
 * verbatim, including its generic-search-once-per-attempt fix and the 10s inter-retry sleep in
 * waitForMiraklStatus (see TC_FBS_007's Entregado->Received timing fix).
 *
 * Belongs to the "FBS" TestNG group. BaseClass logs in to FDA once via @BeforeGroups("FBS") using
 * fbs.username/fbs.password before the first FBS test runs, and logs out once via @AfterGroups("FBS")
 * after the last one — no per-test-class login/logout here. Mirakl uses the shared session from
 * @BeforeSuite (mirakl.username/mirakl.password already matches the account this TC requires).
 */
public class TC_FBS_012_Test extends BaseClass {

    private static final String TC_NAME = "TC_FBS_012";

    // Fixed business constants for the FBS flow (not account/test data — not sourced from config)
    private static final String TC_CARRIER       = "DHL";
    private static final String TC_DOCUMENT_TYPE = "Invoice";
    private static final String TC_DELIVERY_TYPE = "FBS";
    private static final int    TC_QUANTITY      = 2;

    // PayPal sandbox test data — same sandbox account already used by TC_FBO_007/TC_FBS_007-011
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
        LoggerUtility.info("All page objects initialized for TC_FBS_012");
    }

    @Test(testName = TC_NAME, groups = {"FBS"},
          description = "Verify FBS order placement (2 products qty=2 each, 2 different 3P sellers, PayPal payment) and full fulfilment lifecycle for BOTH resulting Mirakl shipments (WEB-A + WEB-B): FDA -> Mirakl Accept -> Kibo (FBS) -> Documents/Tracking -> Shipped -> Received")
    public void tc_fbs_012_place_order_2products_diff_sellers_qty2_paypal_fbs_full_fulfillment() throws InterruptedException {

        // Kibo deliveryType checks (one per shipment entry) must not abort the run: the flow needs
        // to keep going through Documents/Tracking/Mark as Shipped/Entregado/Received for both
        // shipments even if Kibo hasn't surfaced deliveryType=FBS yet. Recorded here and asserted
        // only at the end.
        SoftAssert softAssert = new SoftAssert();

        // ============================================================
        // PHASE 1: FDA — Search, PDP, Qty=2 each, Cart (2 products, 2 different sellers), PayPal Payment, Order
        // ============================================================

        switchToFDATab();
        LoggerUtility.info("TC_FBS_012: Starting — FDA session active from @BeforeGroups(FBS), Mirakl session active from suite setup");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test cleanup: remove any cart items left over from previous runs (not a spec step)
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        String sku1 = config.getFbs012Sku1();
        String sku2 = config.getFbs012Sku2();

        // Steps 1-3: Search first product (Seller A)
        LoggerUtility.info("Step 1: Clicking ¿Qué estás buscando? search field");
        LoggerUtility.info("Step 2: Entering first product SKU: " + sku1 + " (Seller A)");
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

        // Steps 10-12: Search second product (Seller B — different 3P seller)
        LoggerUtility.info("Step 10: Clicking ¿Qué estás buscando? search field");
        LoggerUtility.info("Step 11: Entering second product SKU: " + sku2 + " (Seller B — different 3P seller)");
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
        // (TC_FBS_006); the live-confirmed PayPal flow (TC_FBS_007-011) has no equivalent element
        // on PayPal's review page, so this logs the cart total captured in Step 24 instead of
        // asserting on an element that was never observed to exist for PayPal orders.
        LoggerUtility.info("Step 35: Order total for both products qty=" + TC_QUANTITY + " each (captured in cart): " + orderTotal
                + " — PayPal review page has no equivalent 'Completar pago (MXN$...)' button to re-verify this against");

        // Step 36: Click Compra completa — this is the button that actually finalizes the
        // PayPal-side authorization (same button TC_FBO_007/TC_FBS_007-011 click); it closes the
        // popup and returns control to FDA's checkout page with the order already completed. The
        // FDA/Adyen "Completar pago" button used by the Credit Card path does not exist once
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
        // TC_FBS_007-011's PayPal flow already applies; the Credit Card FBS tests don't see this
        // status because payment finalizes synchronously on the FDA page for them.
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente") || fdaStatus.equals("Procesando"),
                "Order status should be 'Creada', 'Pendiente', or 'Procesando' in FDA order history. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 2: MIRAKL — Verify WEB-A (Seller A) + WEB-B (Seller B), Accept Both
        // ============================================================
        // Different sellers → 2 Mirakl shipments, same as TC_FBS_006/TC_FBO_006. The manual test
        // case's remaining steps (44-77) are applied independently to each shipment below.

        LoggerUtility.info("Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();

        String shipmentRefA = orderId + "WEB-A";
        String shipmentRefB = orderId + "WEB-B";
        String shipmentRefGeneric = orderId + "WEB";

        // Search for the order — retry every 60s up to 10 minutes for Mirakl sync delay.
        // Search ONCE per attempt with the generic "orderId+WEB" term (it matches both "...WEB-A"
        // and "...WEB-B" as a substring), then check the single resulting table for both suffixes.
        // Searching directly with each specific shipment ref back-to-back in the same attempt (no
        // generic search in between) was confirmed via live run (TC_FBS_006) to leave the Mirakl
        // search field holding both terms concatenated together, which never matches any row.
        boolean shipmentAFound = false;
        boolean shipmentBFound = false;
        for (int attempt = 1; attempt <= 10; attempt++) {
            LoggerUtility.info("Mirakl search attempt " + attempt + "/10 for generic ref: " + shipmentRefGeneric);
            miraklOrdersPage.searchOrder(shipmentRefGeneric);
            shipmentAFound = miraklOrdersPage.hasSearchResults(shipmentRefA);
            shipmentBFound = miraklOrdersPage.hasSearchResults(shipmentRefB);
            LoggerUtility.info("Attempt " + attempt + " — WEB-A present: " + shipmentAFound + ", WEB-B present: " + shipmentBFound);
            if (shipmentAFound && shipmentBFound) {
                LoggerUtility.info("Both WEB-A and WEB-B confirmed present in Mirakl on attempt " + attempt);
                break;
            }
            if (attempt < 10) {
                LoggerUtility.info("Waiting 60 seconds before next search attempt...");
                Thread.sleep(60_000);
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(shipmentAFound, "Mirakl shipment WEB-A (Seller A) not found: " + shipmentRefA);
        Assert.assertTrue(shipmentBFound, "Mirakl shipment WEB-B (Seller B) not found: " + shipmentRefB);
        LoggerUtility.info("Both shipments verified — WEB-A (Seller A) and WEB-B (Seller B) present in Mirakl");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ---- Open + Accept WEB-A ----
        LoggerUtility.info("Opening shipment WEB-A: " + shipmentRefA);
        miraklOrderDetailPage.clickOrderInList(shipmentRefA);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        String urlShipmentA = acceptShipment(shipmentRefA);
        LoggerUtility.info("WEB-A accepted — URL: " + urlShipmentA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ---- Open + Accept WEB-B — navigate back to the order list and re-search (same
        // technique TC_FBS_005/006/TC_FBO_006 use for 2-different-seller orders; unlike
        // same-seller splits, seller-split shipment IDs are not guaranteed to differ only in the
        // WEB-A/WEB-B suffix of a shared URL, so URL substitution is not used here) ----
        LoggerUtility.info("Navigating back to Mirakl order list to open WEB-B: " + shipmentRefB);
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        miraklOrdersPage.searchOrder(shipmentRefB);
        miraklOrderDetailPage.clickOrderInList(shipmentRefB);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        String urlShipmentB = acceptShipment(shipmentRefB);
        LoggerUtility.info("WEB-B accepted — URL: " + urlShipmentB);
        LoggerUtility.info("Both shipments (WEB-A + WEB-B) accepted successfully");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: KIBO API — Authenticate + Verify Delivery Type = FBS on both shipment entries
        // ============================================================

        LoggerUtility.info("========== Kibo_Auth ==========");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(), "Kibo authentication failed — access token is empty");

        String externalId = orderId + "WEB";
        LoggerUtility.info("========== Get_all_orders_in_Kibo ==========");
        LoggerUtility.info("Searching externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(), "Kibo Order ID not found for externalId: " + externalId);
        LoggerUtility.info("Kibo Order ID : " + kiboOrderId);

        LoggerUtility.info("========== Get_Shipment_Details ==========");
        List<Map<String, Object>> shipmentList = null;
        for (int attempt = 1; attempt <= 5; attempt++) {
            LoggerUtility.info("Kibo shipment data poll attempt " + attempt + "/5");
            Response shipmentsResp = ApiUtility.kiboGetShipmentsByOrderId(kiboToken, kiboOrderId);
            Assert.assertEquals(shipmentsResp.getStatusCode(), 200,
                    "Kibo Get Shipment Details should return 200. Actual: " + shipmentsResp.getStatusCode());

            List<Map<String, Object>> polled = shipmentsResp.jsonPath().getList("items");
            if (polled == null || polled.isEmpty()) {
                polled = shipmentsResp.jsonPath().getList("_embedded.shipments");
            }
            if (polled != null && polled.size() >= 2) {
                boolean allPopulated = true;
                for (Map<String, Object> s : polled) {
                    if (extractCustomField(s, "deliveryType").isEmpty()) {
                        allPopulated = false;
                        break;
                    }
                }
                if (allPopulated) {
                    shipmentList = polled;
                    LoggerUtility.info("Both Kibo shipment entries have deliveryType populated — attempt " + attempt);
                    break;
                }
            }
            LoggerUtility.info("Kibo shipment data not fully ready. Waiting 15s...");
            if (attempt < 5) Thread.sleep(15_000);
        }
        // Verify delivery type is FBS on each returned shipment entry (soft assert — must not stop
        // the run before the Mirakl documents/tracking/shipped/received steps complete for either
        // shipment; failures are reported by softAssert.assertAll() at the end).
        if (shipmentList != null) {
            for (int i = 0; i < shipmentList.size(); i++) {
                String deliveryType = extractCustomField(shipmentList.get(i), "deliveryType");
                LoggerUtility.info("Kibo shipment entry " + (i + 1) + " delivery type = " + deliveryType + " (expected 'FBS')");
                softAssert.assertEquals(deliveryType.toUpperCase(), TC_DELIVERY_TYPE,
                        "Kibo shipment entry " + (i + 1) + " delivery type should be 'FBS'. Actual: " + deliveryType);
            }
        } else {
            softAssert.fail("Kibo did not return 2 populated shipment entries for order " + kiboOrderId + " within 5 attempts");
        }
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: MIRAKL — Documents, Tracking, Mark as Shipped, Entregado → Received
        // for BOTH shipments independently
        // ============================================================

        runFbsFulfillmentFlow(shipmentRefA, urlShipmentA);
        runFbsFulfillmentFlow(shipmentRefB, urlShipmentB);

        // ============================================================
        // PHASE 5: FDA — Re-verify order in Mis pedidos after both shipments Received
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

        LoggerUtility.info("TC_FBS_012 execution completed successfully — WEB-A and WEB-B both Received");

        // Now surface any soft-assertion failures (e.g. Kibo delivery type) so the test still
        // fails correctly if something was wrong.
        softAssert.assertAll();
    }

    // ----------------------------------------------------------------
    // Accept the currently-open Mirakl shipment (tolerates auto-accept, same pattern as
    // TC_FBS_002-011) and return its detail page URL for later direct navigation.
    // ----------------------------------------------------------------
    private String acceptShipment(String shipmentRef) {
        String status;
        if (miraklOrderDetailPage.isAcceptButtonPresent(5)) {
            LoggerUtility.info("Clicking Accept button for " + shipmentRef);
            miraklOrderDetailPage.clickAcceptButton();
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            status = miraklOrderDetailPage.getOrderStatus();
        } else {
            LoggerUtility.info("Accept button not present for " + shipmentRef + " — already auto-accepted");
            status = miraklOrderDetailPage.getOrderStatus();
        }
        Assert.assertEquals(status, "Awaiting shipment",
                "Mirakl status should be 'Awaiting shipment' after acceptance [" + shipmentRef + "]. Actual: " + status);
        return driver.getCurrentUrl();
    }

    // ----------------------------------------------------------------
    // Documents upload -> Tracking -> Mark as Shipped -> Custom field Entregado -> Received,
    // run independently for one Mirakl shipment. Each call generates its own random tracking
    // number; the same invoice PDF (fbs012.invoice.file.path) is uploaded for both shipments.
    // ----------------------------------------------------------------
    private void runFbsFulfillmentFlow(String shipmentRef, String miraklDetailUrl) throws InterruptedException {
        LoggerUtility.info("========== FBS Fulfillment: " + shipmentRef + " ==========");
        switchToMiraklTab();
        driver.get(miraklDetailUrl);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // More actions -> Documents -> Add -> upload popup
        LoggerUtility.info("Clicking More actions dropdown [" + shipmentRef + "]");
        miraklOrderDetailPage.clickMoreActionsDropdown();
        LoggerUtility.info("Clicking Documents option under More actions [" + shipmentRef + "]");
        miraklOrderDetailPage.clickDocumentsOption();
        LoggerUtility.info("Clicking the blue Add button in the Order documents section [" + shipmentRef + "]");
        miraklOrderDetailPage.clickAddDocumentButton();
        Assert.assertTrue(miraklOrderDetailPage.isUploadDocumentPopupDisplayed(),
                "Upload an order document popup should be displayed [" + shipmentRef + "]");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Select Invoice type, upload file, confirm
        LoggerUtility.info("Selecting document type " + TC_DOCUMENT_TYPE + " [" + shipmentRef + "]");
        miraklOrderDetailPage.selectDocumentType(TC_DOCUMENT_TYPE);
        LoggerUtility.info("Uploading invoice PDF: " + config.getFbs012InvoiceFilePath() + " [" + shipmentRef + "]");
        miraklOrderDetailPage.uploadDocumentFile(config.getFbs012InvoiceFilePath());
        miraklOrderDetailPage.clickConfirmUploadButton();
        Assert.assertTrue(miraklOrderDetailPage.isDocumentUploadedMessageDisplayed(),
                "'The document has been uploaded.' confirmation message should be displayed [" + shipmentRef + "]");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Clicking More actions > Documents navigates the browser away from the Order Detail page
        // to a separate "Order documents" page. Return via the captured detail URL rather than
        // re-searching through the orders list.
        LoggerUtility.info("Navigating back to Mirakl Order Detail page after document upload [" + shipmentRef + "]");
        driver.get(miraklDetailUrl);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Add tracking information (DHL + independently generated tracking number)
        LoggerUtility.info("Clicking Add tracking information link [" + shipmentRef + "]");
        miraklOrderDetailPage.clickAddTrackingInformationLink();
        LoggerUtility.info("Selecting carrier " + TC_CARRIER + " [" + shipmentRef + "]");
        miraklOrderDetailPage.selectCarrier(TC_CARRIER);
        String trackingNumber = generateEightDigitTrackingNumber();
        LoggerUtility.info("Entering tracking number: " + trackingNumber + " [" + shipmentRef + "]");
        miraklOrderDetailPage.enterTrackingNumber(trackingNumber);
        miraklOrderDetailPage.clickAddTrackingButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Verify the selected Carrier Name and Tracking Number are displayed
        LoggerUtility.info("Verifying Carrier/Tracking Number via View tracking information [" + shipmentRef + "]");
        miraklOrderDetailPage.clickViewTrackingInformationLink();
        String displayedCarrier = miraklOrderDetailPage.getTrackingDialogCarrierName();
        Assert.assertTrue(displayedCarrier.toUpperCase().contains(TC_CARRIER),
                "Displayed carrier should be DHL [" + shipmentRef + "]. Actual: " + displayedCarrier);
        String displayedTracking = miraklOrderDetailPage.getTrackingDialogTrackingNumber();
        Assert.assertEquals(displayedTracking, trackingNumber,
                "Displayed tracking number should match entered value [" + shipmentRef + "]. Actual: " + displayedTracking);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        miraklOrderDetailPage.closeTrackingInfoDialog();

        // Mark as Shipped
        LoggerUtility.info("Clicking Mark as Shipped button [" + shipmentRef + "]");
        miraklOrderDetailPage.clickMarkAsShippedButton();
        String shippedStatus = waitForMiraklStatus("Shipped", 6);
        LoggerUtility.info("Mirakl status after Mark as Shipped [" + shipmentRef + "]: " + shippedStatus);
        Assert.assertEquals(shippedStatus, "Shipped",
                "Mirakl order status should be 'Shipped' [" + shipmentRef + "]. Actual: " + shippedStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Custom field "Entregado" → Received
        LoggerUtility.info("Clicking More actions dropdown [" + shipmentRef + "]");
        miraklOrderDetailPage.clickMoreActionsDropdown();
        LoggerUtility.info("Clicking Custom field button [" + shipmentRef + "]");
        miraklOrderDetailPage.clickCustomFieldButton();
        LoggerUtility.info("Clicking Entregado option [" + shipmentRef + "]");
        miraklOrderDetailPage.clickEntregadoOption();
        // Entregado has a Yes/No value dropdown that must be set before Confirm does anything.
        miraklOrderDetailPage.clickEntregadoValueDropdown();
        miraklOrderDetailPage.selectEntregadoYes();
        miraklOrderDetailPage.clickCustomFieldConfirmButton();

        // The Entregado -> Received transition is processed asynchronously on the backend — a
        // live run of TC_FBS_004/006/007 confirmed rapid refreshes with no pre-poll wait isn't
        // reliably enough time even though the click sequence completes with no errors. Give the
        // backend a head start, then poll with more retries.
        LoggerUtility.info("Waiting 30s for Entregado update to propagate before polling [" + shipmentRef + "]...");
        Thread.sleep(30_000);
        String finalStatus = waitForMiraklStatus("Received", 20);
        LoggerUtility.info("Mirakl status after Entregado [" + shipmentRef + "]: " + finalStatus + " (expected 'Received')");
        Assert.assertEquals(finalStatus, "Received",
                "Mirakl order status should change from 'Shipped' to 'Received' [" + shipmentRef + "]. Actual: " + finalStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        LoggerUtility.info(shipmentRef + " fulfillment complete — Received");
    }

    // ----------------------------------------------------------------
    // Poll helper: refresh + read status up to maxRetries times. Adds a 10s sleep between
    // retries (same fix TC_FBO_030/TC_FBS_007-011 already apply for slow post-acceptance status
    // transitions) — a live TC_FBS_007 run showed the Entregado -> Received transition can
    // outlast the ~80s of back-to-back refreshes that 20 retries alone give you.
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
    // (shipment.items[*].data.deliveryType), not on the shipment or its packages.
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
    // Generates a random 8-digit tracking number (manual step requirement) — called once per
    // shipment so WEB-A and WEB-B get independent tracking numbers.
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
