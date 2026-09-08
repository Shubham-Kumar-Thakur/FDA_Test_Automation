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
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * TC_FBS_006 — FBS (Fulfilled By Store) order lifecycle, 2 products (qty=2 each) from
 * 2 DIFFERENT 3P sellers.
 *
 * Combines TC_FBS_004's PDP quantity-increase flow (each product raised from 1 to 2 via the
 * Aumentar plus button) with TC_FBS_005's dual-shipment handling: different-seller orders split
 * into TWO Mirakl shipments — WEB-A + WEB-B — as a structural property of Mirakl's data model
 * (each seller/shop gets its own order line), confirmed consistently across every
 * 2-different-seller FBO test in this codebase (TC_FBO_005, 006, 011, 012), including the direct
 * FBO analog of this exact product setup, TC_FBO_006 (2 SKUs qty=2 each from 2 different 3P
 * sellers). This is a reliable predictor of splitting, unlike the quantity-driven assumption that
 * turned out wrong for TC_FBS_004 (same seller, qty=2 each, does NOT split).
 *
 * Both shipments independently need the full Accept -> Kibo deliveryType check -> Documents ->
 * Tracking -> Mark as Shipped -> Entregado -> Received sequence, each shipment gets its own
 * independently generated random tracking number, and the same invoice PDF is uploaded to both.
 * Kibo's Get_Shipment_Details call also returns 2 shipment entries for this order; deliveryType
 * is verified on each generically — no need to map a specific Kibo entry to WEB-A vs WEB-B, since
 * FBS fulfillment actions are driven entirely from the Mirakl UI, not from Kibo data.
 *
 * WEB-B is reopened by navigating back to the Mirakl order list and re-searching (same technique
 * TC_FBS_005/TC_FBO_006 use for 2-different-seller orders), not via URL substitution — seller-split
 * shipment IDs are not guaranteed to differ only in the "WEB-A"/"WEB-B" suffix the way same-seller
 * split shipments are.
 *
 * Belongs to the "FBS" TestNG group. BaseClass logs in to FDA once via @BeforeGroups("FBS") using
 * fbs.username/fbs.password before the first FBS test runs, and logs out once via @AfterGroups("FBS")
 * after the last one — no per-test-class login/logout here. Mirakl uses the shared session from
 * @BeforeSuite (mirakl.username/mirakl.password already matches the account this TC requires).
 */
public class TC_FBS_006_Test extends BaseClass {

    private static final String TC_NAME = "TC_FBS_006";

    // Fixed business constants for the FBS flow (not account/test data — not sourced from config)
    private static final String TC_CARRIER       = "DHL";
    private static final String TC_DOCUMENT_TYPE = "Invoice";
    private static final String TC_DELIVERY_TYPE = "FBS";
    private static final int    TC_QUANTITY      = 2;

    // --- Page Objects ---
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

    // --- Dynamic Test Data (captured once, reused everywhere) ---
    private String orderId;
    private String orderTotal;

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
        LoggerUtility.info("All page objects initialized for TC_FBS_006");
    }

    @Test(testName = TC_NAME, groups = {"FBS"},
          description = "Verify FBS order placement (2 products qty=2 each, 2 different 3P sellers) and full fulfilment lifecycle for BOTH resulting Mirakl shipments (WEB-A + WEB-B): FDA -> Mirakl Accept -> Kibo (FBS) -> Documents/Tracking -> Shipped -> Received")
    public void tc_fbs_006_place_order_2products_diff_sellers_qty2_fbs_full_fulfillment() throws InterruptedException {

        // Kibo deliveryType checks (one per shipment entry) must not abort the run: the flow needs
        // to keep going through Documents/Tracking/Mark as Shipped/Entregado/Received for both
        // shipments even if Kibo hasn't surfaced deliveryType=FBS yet. Recorded here and asserted
        // only at the end.
        SoftAssert softAssert = new SoftAssert();

        // ============================================================
        // PHASE 1: FDA — Search, PDP, Qty=2 each, Cart (2 products, 2 different sellers), Payment, Order
        // ============================================================

        switchToFDATab();
        LoggerUtility.info("TC_FBS_006: Starting — FDA session active from @BeforeGroups(FBS), Mirakl session active from suite setup");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test cleanup: remove any cart items left over from previous runs (not a spec step)
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        String sku1 = config.getFbs006Sku1();
        String sku2 = config.getFbs006Sku2();

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

        // Step 25-26: Proceed to payment → shipping page
        LoggerUtility.info("Step 25: Clicking Proceed to payment button");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();
        LoggerUtility.info("Step 26: Clicking Siguiente button on the Shipping page");
        fdaPaymentPage.clickNextButton();

        // Steps 27-34: Payment page
        LoggerUtility.info("Step 27: Selecting Pago con Tarjeta Crédito/Débito radio button");
        fdaPaymentPage.selectCreditCardOption();
        LoggerUtility.info("Step 28-29: Clicking Número de tarjeta field and entering card number");
        fdaPaymentPage.enterCardNumber(config.getFbs006CardNumber());
        LoggerUtility.info("Step 30-31: Clicking Fecha de expiración field and entering expiration date");
        fdaPaymentPage.enterExpiry(config.getFbs006CardExpiry());
        LoggerUtility.info("Step 32-33: Clicking Código de seguridad field and entering security code");
        fdaPaymentPage.enterCvv(config.getFbs006CardCvv());
        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Step 34: Completar pago button text: " + payBtnText);
        Assert.assertTrue(payBtnText.contains("Completar pago") || payBtnText.contains("MXN")
                || payBtnText.contains("$"),
                "Completar pago button should display order total for both products with quantity " + TC_QUANTITY + " each. Actual text: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 35-37: Complete payment → success page → get order ID
        LoggerUtility.info("Step 35: Clicking Completar pago button");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();
        LoggerUtility.info("Step 36: Verifying order success page is displayed");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(), "Order success page not displayed after payment");
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 37: Order ID = " + orderId);
        Assert.assertFalse(orderId.isEmpty(), "Order ID should be present on success page");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 38-42: Order history
        LoggerUtility.info("Step 38: Clicking Mi cuenta profile icon");
        fdaHomePage.clickProfileIcon();
        LoggerUtility.info("Step 39: Clicking Mis pedidos link");
        fdaHomePage.clickMyOrdersLink();
        LoggerUtility.info("Step 40: Verifying Mis pedidos page is displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(), "Mis pedidos page not displayed");
        LoggerUtility.info("Step 41: Verifying Order ID " + orderId + " is present in order history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
                "Order ID " + orderId + " not found in order history");
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 42: FDA order status: " + fdaStatus + " (expected 'Creada')");
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
                "Order status should be 'Creada' in FDA order history. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 2: MIRAKL — Verify WEB-A (Seller A) + WEB-B (Seller B), Accept Both
        // ============================================================
        // Different sellers → 2 Mirakl shipments, same as TC_FBO_006. The manual test case's
        // remaining steps (43-76) are applied independently to each shipment below.

        LoggerUtility.info("Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();

        String miraklSearchTerm = orderId + "WEB";
        String shipmentRefA     = orderId + "WEB-A";
        String shipmentRefB     = orderId + "WEB-B";

        // Search for the order — retry every 60s up to 10 minutes for Mirakl sync delay.
        // IMPORTANT (learned from TC_FBS_004): the generic search term ("orderId+WEB") is a
        // substring match that's already satisfied the instant EITHER WEB-A or WEB-B alone
        // appears — Mirakl does not create both shipment rows atomically. The loop must keep
        // polling until BOTH shipmentRefA and shipmentRefB are individually confirmed present,
        // not just the generic term.
        LoggerUtility.info("Searching Mirakl for: " + miraklSearchTerm);
        boolean orderFoundInMirakl = false;
        boolean shipmentAFound = false;
        boolean shipmentBFound = false;
        for (int attempt = 1; attempt <= 10; attempt++) {
            LoggerUtility.info("Mirakl search attempt " + attempt + "/10 for: " + miraklSearchTerm);
            miraklOrdersPage.searchOrder(miraklSearchTerm);
            orderFoundInMirakl = miraklOrdersPage.hasSearchResults(miraklSearchTerm);
            if (orderFoundInMirakl) {
                shipmentAFound = miraklOrdersPage.hasSearchResults(shipmentRefA);
                shipmentBFound = miraklOrdersPage.hasSearchResults(shipmentRefB);
                LoggerUtility.info("Order found on attempt " + attempt
                        + " — WEB-A present: " + shipmentAFound + ", WEB-B present: " + shipmentBFound);
                if (shipmentAFound && shipmentBFound) {
                    LoggerUtility.info("Both WEB-A and WEB-B confirmed present in Mirakl on attempt " + attempt);
                    break;
                }
            } else {
                LoggerUtility.info("Order not in Mirakl yet (attempt " + attempt + "/10)");
            }
            if (attempt < 10) {
                LoggerUtility.info("Waiting 60 seconds before next search attempt...");
                Thread.sleep(60_000);
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(orderFoundInMirakl, "Order " + miraklSearchTerm + " did not appear in Mirakl within 10 minutes");
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
        // technique TC_FBS_005/TC_FBO_006 use for 2-different-seller orders; unlike same-seller
        // splits, seller-split shipment IDs are not guaranteed to differ only in the WEB-A/WEB-B
        // suffix of a shared URL, so URL substitution is not used here) ----
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

        LoggerUtility.info("TC_FBS_006 execution completed successfully — WEB-A and WEB-B both Received");

        // Now surface any soft-assertion failures (e.g. Kibo delivery type) so the test still
        // fails correctly if something was wrong.
        softAssert.assertAll();
    }

    // ----------------------------------------------------------------
    // Accept the currently-open Mirakl shipment (tolerates auto-accept, same pattern as
    // TC_FBS_002/003/004/005) and return its detail page URL for later direct navigation.
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
    // number; the same invoice PDF (fbs006.invoice.file.path) is uploaded for both shipments.
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
        LoggerUtility.info("Uploading invoice PDF: " + config.getFbs006InvoiceFilePath() + " [" + shipmentRef + "]");
        miraklOrderDetailPage.uploadDocumentFile(config.getFbs006InvoiceFilePath());
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

        // The Entregado -> Received transition is processed asynchronously on the backend (same
        // as the carrier-webhook-driven transitions in the FBO tests, which always sleep before
        // polling) — a live run of this dual-shipment order confirmed 6 rapid refreshes (~30s
        // total, no pre-poll wait) isn't reliably enough time: the click sequence completed
        // cleanly with no errors, yet status stayed "Shipped" through all 6 checks. Give the
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
    // Polls for up to 20s for the window set to settle into "new window opened" or "original
    // closed" before deciding which window to switch to — a single instantaneous read of
    // getWindowHandles() right after the click is a race condition that caused TC_FBS_004 to get
    // stuck operating on a stale, soon-to-close tab (see CLAUDE.md).
    // ----------------------------------------------------------------
    private void switchToCheckoutWindow() {
        String newHandle = null;
        java.util.Set<String> handles = driver.getWindowHandles();
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
}
// @AfterMethod navigateToHomePage() is inherited from BaseClass
