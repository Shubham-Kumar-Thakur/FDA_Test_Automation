package FDA_Automation_Script.FDA_Automation_Script.tests;

import FDA_Automation_Script.FDA_Automation_Script.base.BaseClass;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.*;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklLoginPage;
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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.restassured.RestAssured.given;

/**
 * TC_FBS_017 — FBS (Fulfilled By Store) order lifecycle, 2 products (qty=1 each) from
 * 2 DIFFERENT 3P sellers, with a FULL return of only ONE seller's shipment (WEB-B):
 * FDA (Credit Card) -> Mirakl Accept BOTH WEB-A + WEB-B -> Kibo (verify delivery type = FBS on
 * both) -> Documents/Tracking/Mark as Shipped/Entregado -> Received for BOTH shipments
 * independently (same shape as TC_FBS_005, reused verbatim via that class' pattern) -> Return API
 * (order_line_id + create incident) on WEB-B only -> Mirakl return tracking + return label upload
 * -> Mark as received -> Check compliance -> Full refund -> WEB-B closed. WEB-A is left untouched
 * (stays 'Received' forever) — this test never returns it, matching the "return one seller order"
 * title.
 *
 * Same different-seller SKU pairing already confirmed by TC_FBS_005/011 to split into 2 Mirakl
 * shipments (structural property of Mirakl's data model, not quantity-driven — confirmed
 * consistently across every 2-different-seller FBO/FBS test in this codebase).
 *
 * CORRECTION (per direct instruction): an earlier revision returned WEB-A, reasoning that
 * ReturnApiUtility.getMiraklOrderLineId() always appends "-A" internally and so could only ever
 * target WEB-A. Per explicit instruction, this test must return WEB-B instead — fulfill WEB-A,
 * fulfill WEB-B, then return WEB-B. Since the shared utility can't do that (and per this
 * project's "never change a utility class" rule, ReturnApiUtility itself is not modified), this
 * class has its own private getMiraklOrderLineIdForShipment(shipmentRef) that makes the identical
 * GET /api/orders?order_ids={shipmentRef} call but against the caller-supplied full shipment ref
 * (orderId + "WEB-B") instead of a hardcoded "-A" suffix — same inline-Mirakl-API-call precedent
 * already used by TC_FBO_030 for its offer_sku resolution. postReturn() itself is suffix-agnostic
 * (it just takes whatever order_line_id string it's given), so only the order_line_id lookup
 * needed this workaround.
 *
 * Step numbers 1-72 below are 1:1 with the manual test case and match TC_FBS_005's shape exactly
 * (same FDA cart flow, same dual-shipment Accept/Kibo/fulfillment structure) — the manual case
 * only lists ONE non-duplicated pass of steps 39-72, but (per explicit user confirmation, matching
 * TC_FBS_005's own precedent) both resulting Mirakl shipments genuinely need independent
 * fulfillment, so runFbsFulfillmentFlow() below runs twice. Steps 73-96 mirror TC_FBS_013's
 * full-refund return flow, applied only to WEB-B's detail page/URL captured during the accept
 * phase. Unlike TC_FBS_014/016 (partial refund, 2 line items on one shipment, confirmed via live
 * run to need a manual "Mark as closed" click), this manual case has NO "Mark as closed" step —
 * consistent with TC_FBS_013/015's full-refund-of-the-only-line-item pattern, where the shipment
 * is expected to close on its own. This exact combination (dual-shipment fulfillment + single-
 * shipment full return) has not yet been confirmed end-to-end via a live run — if WEB-B's status
 * still reads something other than 'Closed' after the wait+refresh below, check whether Mirakl
 * actually needs the same Mark as closed click here too (MiraklReturnPage.clickMarkAsClosed() /
 * confirmMarkAsClosedPopup(), same as TC_FBS_014/016) before assuming a locator regression.
 *
 * Belongs to the "FBS" TestNG group. BaseClass logs in to FDA once via @BeforeGroups("FBS") using
 * fbs.username/fbs.password before the first FBS test runs, and logs out once via @AfterGroups("FBS")
 * after the last one — no per-test-class login/logout here. Mirakl uses the shared session from
 * @BeforeSuite (mirakl.username/mirakl.password already matches the account this TC requires).
 */
public class TC_FBS_017_Test extends BaseClass {

    private static final String TC_NAME = "TC_FBS_017";

    // Fixed business constants for the FBS flow (not account/test data — not sourced from config)
    private static final String TC_CARRIER       = "DHL";
    private static final String TC_DOCUMENT_TYPE = "Invoice";
    private static final String TC_DELIVERY_TYPE = "FBS";

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
    private MiraklReturnPage      miraklReturnPage;

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
        miraklReturnPage      = new MiraklReturnPage(driver);
        LoggerUtility.info("All page objects initialized for TC_FBS_017");
    }

    @Test(testName = TC_NAME, groups = {"FBS"},
          description = "Verify FBS order placement (2 products qty=1 each, 2 different 3P sellers), full fulfilment of BOTH shipments, and a full return/refund of ONLY WEB-B (one seller): FDA -> Mirakl Accept (both) -> Kibo (FBS) -> Documents/Tracking -> Shipped -> Received (both) -> Return (WEB-B only) -> Compliance -> Full Refund -> WEB-B Closed")
    public void tc_fbs_017_place_order_2products_diff_sellers_fbs_fulfillment_and_return_one_seller() throws InterruptedException {

        // Kibo deliveryType checks (one per shipment entry) must not abort the run: the flow needs
        // to keep going through Documents/Tracking/Mark as Shipped/Entregado/Received for both
        // shipments even if Kibo hasn't surfaced deliveryType=FBS yet. Recorded here and asserted
        // only at the end.
        SoftAssert softAssert = new SoftAssert();

        // ============================================================
        // PHASE 1: FDA — Search, PDP, Cart (2 products, 2 different sellers), Payment, Order
        // ============================================================

        switchToFDATab();
        LoggerUtility.info("TC_FBS_017: Starting — FDA session active from @BeforeGroups(FBS), Mirakl session active from suite setup");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test cleanup: remove any cart items left over from previous runs (not a spec step)
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        String sku1 = config.getFbs017Sku1();
        String sku2 = config.getFbs017Sku2();

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

        // Step 7: Add first product to cart
        LoggerUtility.info("Step 7: Clicking Agregar al carrito for first product");
        fdaPdpPage.clickAddToCart();

        // Navigate home before searching the second product
        fdaHomePage.navigateTo(config.getFdaUrl());

        // Steps 8-10: Search second product (Seller B — different 3P seller)
        LoggerUtility.info("Step 8: Clicking ¿Qué estás buscando? search field");
        LoggerUtility.info("Step 9: Entering second product SKU: " + sku2 + " (Seller B — different 3P seller)");
        fdaHomePage.enterSearchQuery(sku2);
        LoggerUtility.info("Step 10: Pressing Enter on the keyboard");
        fdaHomePage.pressSearchEnter();

        // Steps 11-13: PDP validations for product 2
        LoggerUtility.info("Step 11: Verifying PDP for second product is displayed");
        Assert.assertTrue(fdaPdpPage.isDisplayed(), "PDP not displayed for second product SKU: " + sku2);
        LoggerUtility.info("Step 12: Verifying Agregar al carrito button is enabled");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(), "Add to cart button is not enabled on PDP for SKU: " + sku2);
        LoggerUtility.info("Step 13: Verifying second product quantity is 1");
        Assert.assertEquals(fdaPdpPage.getQuantity(), "1", "Product quantity on PDP should be 1 for SKU: " + sku2);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 14: Add second product to cart
        LoggerUtility.info("Step 14: Clicking Agregar al carrito for second product");
        fdaPdpPage.clickAddToCart();

        // Step 15: Open cart
        LoggerUtility.info("Step 15: Clicking Mi carrito cart icon");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        // Steps 16-19: Verify both products present with qty 1 each
        List<String> cartProductNames = fdaCartPage.getAllProductNames();
        LoggerUtility.info("Step 16-17: Cart product names: " + cartProductNames);
        Assert.assertEquals(cartProductNames.size(), 2, "Cart should contain exactly 2 product names");
        Assert.assertFalse(cartProductNames.get(0).isEmpty(), "First product name should be displayed in cart");
        Assert.assertFalse(cartProductNames.get(1).isEmpty(), "Second product name should be displayed in cart");

        List<String> cartQuantities = fdaCartPage.getAllQuantities();
        LoggerUtility.info("Step 18-19: Cart quantities: " + cartQuantities);
        Assert.assertEquals(cartQuantities.size(), 2, "Cart should contain exactly 2 quantity inputs");
        Assert.assertEquals(cartQuantities.get(0), "1", "First product cart quantity should be 1");
        Assert.assertEquals(cartQuantities.get(1), "1", "Second product cart quantity should be 1");

        // Step 20: Order total for both products
        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 20: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(), "Order total should be displayed in cart");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 21-22: Proceed to payment → shipping page
        LoggerUtility.info("Step 21: Clicking Proceed to payment button");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();
        LoggerUtility.info("Step 22: Clicking Siguiente button on the Shipping page");
        fdaPaymentPage.clickNextButton();

        // Steps 23-30: Payment page
        LoggerUtility.info("Step 23: Selecting Pago con Tarjeta Crédito/Débito radio button");
        fdaPaymentPage.selectCreditCardOption();
        LoggerUtility.info("Step 24-25: Clicking Número de tarjeta field and entering card number");
        fdaPaymentPage.enterCardNumber(config.getFbs017CardNumber());
        LoggerUtility.info("Step 26-27: Clicking Fecha de expiración field and entering expiration date");
        fdaPaymentPage.enterExpiry(config.getFbs017CardExpiry());
        LoggerUtility.info("Step 28-29: Clicking Código de seguridad field and entering security code");
        fdaPaymentPage.enterCvv(config.getFbs017CardCvv());
        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Step 30: Completar pago button text: " + payBtnText);
        Assert.assertTrue(payBtnText.contains("Completar pago") || payBtnText.contains("MXN")
                || payBtnText.contains("$"),
                "Completar pago button should display order total for both products. Actual text: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // (Fix — not a numbered step) Re-verify all three payment fields immediately before
        // submitting — same Adyen async-iframe-re-render mitigation as TC_FBS_013/015/016.
        fdaPaymentPage.verifyAndReenterIfNeeded(config.getFbs017CardNumber(), config.getFbs017CardExpiry(),
                config.getFbs017CardCvv());

        // Steps 31-33: Complete payment → success page → get order ID
        LoggerUtility.info("Step 31: Clicking Completar pago button");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();
        LoggerUtility.info("Step 32: Verifying order success page is displayed");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(), "Order success page not displayed after payment");
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 33: Order ID = " + orderId);
        Assert.assertFalse(orderId.isEmpty(), "Order ID should be present on success page");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 34-38: Order history
        LoggerUtility.info("Step 34: Clicking Mi cuenta profile icon");
        fdaHomePage.clickProfileIcon();
        LoggerUtility.info("Step 35: Clicking Mis pedidos link");
        fdaHomePage.clickMyOrdersLink();
        LoggerUtility.info("Step 36: Verifying Mis pedidos page is displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(), "Mis pedidos page not displayed");
        LoggerUtility.info("Step 37: Verifying Order ID " + orderId + " is present in order history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
                "Order ID " + orderId + " not found in order history");
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 38: FDA order status: " + fdaStatus + " (expected 'Creada')");
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
                "Order status should be 'Creada' in FDA order history. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 2: MIRAKL — Verify WEB-A (Seller A) + WEB-B (Seller B), Accept Both
        // ============================================================
        // Different sellers → 2 Mirakl shipments, same as TC_FBS_005/TC_FBO_005. The manual test
        // case's remaining steps (39-72) are applied independently to each shipment below — see
        // class Javadoc.

        LoggerUtility.info("Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 39-40: Navigate to All Orders
        LoggerUtility.info("Step 39: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 40: Clicking All Orders link");
        miraklOrdersPage.clickAllOrders();

        String shipmentRefA = orderId + "WEB-A";
        String shipmentRefB = orderId + "WEB-B";
        String shipmentRefGeneric = orderId + "WEB";

        // Steps 41-43: Search for the order — retry every 60s up to 10 minutes for Mirakl sync
        // delay. Search ONCE per attempt with the generic "orderId+WEB" term (matches both
        // "...WEB-A" and "...WEB-B" as a substring), then check the single resulting table for
        // both suffixes — same fix as TC_FBS_005/006 (searching each specific suffix back-to-back
        // in the same attempt was confirmed via live run to leave the search field holding both
        // terms concatenated together, matching nothing).
        LoggerUtility.info("Step 41-42: Clicking Search field and entering Order ID: " + shipmentRefGeneric);
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
        LoggerUtility.info("Step 43: Both shipments verified present in Mirakl (Pending acceptance / Awaiting shipment)");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ---- Steps 44-47: Open + verify total + Accept WEB-A ----
        LoggerUtility.info("Step 44: Clicking on WEB-A in the search result list: " + shipmentRefA);
        miraklOrderDetailPage.clickOrderInList(shipmentRefA);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        String miraklTotalA = miraklOrderDetailPage.getOrderTotal();
        LoggerUtility.info("Step 45: WEB-A Mirakl order total: " + miraklTotalA + " | FDA order total: " + orderTotal);
        Assert.assertFalse(miraklTotalA.isEmpty(), "Mirakl order total should be displayed [WEB-A]");
        String urlShipmentA = acceptShipment(shipmentRefA);
        LoggerUtility.info("Steps 46-47: WEB-A accepted — Awaiting shipment. URL: " + urlShipmentA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ---- Open + verify total + Accept WEB-B — navigate back to the order list and
        // re-search (same technique TC_FBS_005/TC_FBO_005 use for 2-different-seller orders;
        // seller-split shipment IDs are not guaranteed to differ only in the WEB-A/WEB-B suffix
        // of a shared URL, so URL substitution is not used here) ----
        LoggerUtility.info("Navigating back to Mirakl order list to open WEB-B: " + shipmentRefB);
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        miraklOrdersPage.searchOrder(shipmentRefB);
        miraklOrderDetailPage.clickOrderInList(shipmentRefB);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        String miraklTotalB = miraklOrderDetailPage.getOrderTotal();
        LoggerUtility.info("WEB-B Mirakl order total: " + miraklTotalB + " | FDA order total: " + orderTotal);
        Assert.assertFalse(miraklTotalB.isEmpty(), "Mirakl order total should be displayed [WEB-B]");
        String urlShipmentB = acceptShipment(shipmentRefB);
        LoggerUtility.info("WEB-B accepted — Awaiting shipment. URL: " + urlShipmentB);
        LoggerUtility.info("Both shipments (WEB-A + WEB-B) accepted successfully");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: KIBO API — Authenticate + Verify Delivery Type = FBS on both shipment entries
        // ============================================================

        LoggerUtility.info("Step 48: ========== Kibo_Auth ==========");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(), "Kibo authentication failed — access token is empty");

        String externalId = orderId + "WEB";
        LoggerUtility.info("Step 49: ========== Get_all_orders_in_Kibo ==========");
        LoggerUtility.info("Searching externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(), "Kibo Order ID not found for externalId: " + externalId);
        LoggerUtility.info("Kibo Order ID : " + kiboOrderId);

        LoggerUtility.info("Step 50: ========== Get_Shipment_Details ==========");
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
        // Step 51: Verify delivery type is FBS on each returned shipment entry (soft assert — must
        // not stop the run before the Mirakl documents/tracking/shipped/received steps complete
        // for either shipment; failures are reported by softAssert.assertAll() at the end).
        if (shipmentList != null) {
            for (int i = 0; i < shipmentList.size(); i++) {
                String deliveryType = extractCustomField(shipmentList.get(i), "deliveryType");
                LoggerUtility.info("Step 51: Kibo shipment entry " + (i + 1) + " delivery type = " + deliveryType + " (expected 'FBS')");
                softAssert.assertEquals(deliveryType.toUpperCase(), TC_DELIVERY_TYPE,
                        "Kibo shipment entry " + (i + 1) + " delivery type should be 'FBS'. Actual: " + deliveryType);
            }
        } else {
            softAssert.fail("Kibo did not return 2 populated shipment entries for order " + kiboOrderId + " within 5 attempts");
        }
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: MIRAKL — Documents, Tracking, Mark as Shipped, Entregado → Received
        // for BOTH shipments independently (Steps 52-72, applied to each)
        // ============================================================

        runFbsFulfillmentFlow(shipmentRefA, urlShipmentA);
        runFbsFulfillmentFlow(shipmentRefB, urlShipmentB);

        // ============================================================
        // PHASE 5: RETURN — API Flow on WEB-B ONLY (Get order_line_id + Create Return Incident)
        // ============================================================

        String orderCommercialId = orderId + "WEB";

        // Step 73: Get Mirakl order_line_id for WEB-B specifically. ReturnApiUtility's shared
        // getMiraklOrderLineId() always appends "-A" internally, so it can't be reused here — see
        // class Javadoc for why this class has its own getMiraklOrderLineIdForShipment() instead
        // (same inline-Mirakl-API-call precedent as TC_FBO_030's offer_sku resolution) rather than
        // modifying the shared utility.
        LoggerUtility.info("Step 73: Calling Get_order_line_id for shipment: " + shipmentRefB);
        String orderLineId = getMiraklOrderLineIdForShipment(shipmentRefB);
        Assert.assertFalse(orderLineId.isEmpty(),
                "order_line_id should not be empty for shipment: " + shipmentRefB);
        LoggerUtility.info("order_line_id retrieved: " + orderLineId);

        // Step 74: Create Return incident via API — same retry-on-transient-failure pattern as
        // TC_FBS_013 (Zscaler/SSLHandshakeException guard).
        LoggerUtility.info("Step 74: Calling Return service API — creating return incident");
        LoggerUtility.info("  order_commercial_id : " + orderCommercialId);
        LoggerUtility.info("  order_line_id       : " + orderLineId);
        Response returnResponse = postReturnWithRetry(orderCommercialId, orderLineId, 3);
        Assert.assertTrue(
                returnResponse.getStatusCode() == 200 || returnResponse.getStatusCode() == 201,
                "Return service should return 200 or 201. Actual: " + returnResponse.getStatusCode()
                        + " | Body: " + returnResponse.getBody().asString());
        LoggerUtility.info("Step 74: Return incident created — HTTP " + returnResponse.getStatusCode());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 6: MIRAKL — Return Tracking, Return Label, Mark Received,
        //          Compliance, Full Refund → WEB-B Closed
        // ============================================================

        // Navigate to WEB-B's own detail page (captured URL) — the return record above was
        // created against WEB-B specifically, so all remaining return-flow steps must run there,
        // not on whichever shipment the browser happens to be showing after Phase 4.
        LoggerUtility.info("Navigating to WEB-B's detail page for the return flow: " + urlShipmentB);
        driver.get(urlShipmentB);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);

        // (Fix — not a numbered step in the manual test case) Same propagation wait as
        // TC_FBS_013/014/015/016: the order detail page doesn't show the return's own "Add
        // tracking information" prompt until refreshed a few times after the Return API call.
        LoggerUtility.info("Waiting for return record to propagate to WEB-B's order detail page");
        boolean returnTrackingLinkVisible = false;
        for (int attempt = 1; attempt <= 6; attempt++) {
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            if (miraklOrderDetailPage.isAddTrackingInformationLinkPresent(5)) {
                returnTrackingLinkVisible = true;
                LoggerUtility.info("Return's Add tracking information link visible after refresh attempt " + attempt + "/6");
                break;
            }
            LoggerUtility.info("Return's Add tracking information link not visible yet (refresh attempt " + attempt + "/6)");
            if (attempt < 6) Thread.sleep(30_000);
        }
        if (!returnTrackingLinkVisible) {
            LoggerUtility.warn("Return's Add tracking information link still not visible after 6 refresh attempts — attempting click anyway");
        }

        // Steps 75-80: Add tracking information again for the return shipment (WEB-B)
        LoggerUtility.info("Step 75: Clicking Add tracking information link (return shipment, WEB-B)");
        miraklOrderDetailPage.clickAddTrackingInformationLink();
        LoggerUtility.info("Step 76-77: Clicking Select a carrier dropdown and selecting " + TC_CARRIER);
        miraklOrderDetailPage.selectCarrier(TC_CARRIER);
        String returnTrackingNumber = generateEightDigitTrackingNumber();
        LoggerUtility.info("Step 78-79: Clicking Tracking number field and entering: " + returnTrackingNumber);
        miraklOrderDetailPage.enterTrackingNumber(returnTrackingNumber);
        LoggerUtility.info("Step 80: Clicking Add button");
        miraklOrderDetailPage.clickAddTrackingButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 81: Kebab button — return-line-specific kebab, not the order-level "More actions"
        // dropdown (see MiraklReturnPage.RETURN_LINE_MORE_ACTIONS_BTN for the confirmed locator).
        LoggerUtility.info("Step 81: Clicking Kebab (More actions) button");
        miraklReturnPage.clickReturnLineMoreActionsButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 82: Add return label
        LoggerUtility.info("Step 82: Clicking Add return label button");
        miraklReturnPage.clickAddReturnLabelOption();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 83: Select file button (intentionally a no-op — see MiraklReturnPage.clickSelectFileButton())
        LoggerUtility.info("Step 83: Clicking Select file button");
        miraklReturnPage.clickSelectFileButton();

        // Step 84: Select the PDF file. Per the same decision already applied to
        // TC_FBS_013/014/015/016: the file is considered selected once sendKeys() completes — do
        // not re-verify via isReturnLabelFileAttached()'s WebDriverWait poll, which was the likely
        // trigger for a recurring browser crash during that wait. Proceed straight to Add (Step 85).
        String returnLabelFilePath = config.getFbs017InvoiceFilePath();
        LoggerUtility.info("Step 84: Selecting return label file: " + returnLabelFilePath);
        miraklReturnPage.uploadReturnLabelFile(returnLabelFilePath);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 85: Add button
        LoggerUtility.info("Step 85: Clicking Add button");
        miraklReturnPage.clickAddReturnLabelButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 86-87: Mark as received (order detail page button, then popup confirmation)
        LoggerUtility.info("Step 86: Clicking Mark as received");
        miraklReturnPage.clickMarkAsReceived();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("Step 87: Confirming Mark as received on popup");
        miraklReturnPage.confirmMarkAsReceivedPopup();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 88: Check compliance
        LoggerUtility.info("Step 88: Clicking Check compliance");
        miraklReturnPage.clickCheckCompliance();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 89: Save
        LoggerUtility.info("Step 89: Clicking Save");
        miraklReturnPage.clickSave();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 90: Refund dropdown
        LoggerUtility.info("Step 90: Clicking Refund dropdown");
        miraklReturnPage.clickRefundDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 91: Full refund — WEB-B has only 1 line item (SKU2, qty=1) and it is being
        // returned in full, so Full refund (not Refund part of the order) is correct, matching
        // TC_FBO_020/022/TC_FBS_013/015's pattern.
        LoggerUtility.info("Step 91: Selecting Full refund from dropdown");
        miraklReturnPage.selectFullRefundFromDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 92: Select dropdown for refund reason
        LoggerUtility.info("Step 92: Clicking Select dropdown for refund reason");
        miraklReturnPage.clickSelectReasonDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 93: Item returned
        LoggerUtility.info("Step 93: Selecting Item returned");
        miraklReturnPage.selectItemReturned();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 94: Confirm on refund popup
        LoggerUtility.info("Step 94: Clicking Confirm on refund popup");
        miraklReturnPage.confirmRefundPopup();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 95: Wait 1 minute and refresh the page
        LoggerUtility.info("Step 95: Waiting 60 seconds for shipment closure to process...");
        Thread.sleep(60_000);
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 96: Verify "Shipment 1: closed" (WEB-B). WEB-B has only 1 line item and it was
        // returned in full, so — per TC_FBS_013/015's established (though not yet independently
        // live-confirmed for this dual-shipment combination) pattern, and matching this manual
        // case having NO "Mark as closed" step, unlike TC_FBS_014/016's partial-refund cases —
        // the shipment is expected to reach 'Closed' on its own via the order/shipment status
        // badge (MiraklOrderDetailPage.getOrderStatus()), not the RETURN section heading. If a
        // live run shows this still reading 'Received', check whether Mirakl needs the same
        // manual "Mark as closed" click here too (see class Javadoc).
        String closedStatus = waitForMiraklStatus("Closed", 6);
        LoggerUtility.info("Step 96: WEB-B shipment status after full refund: " + closedStatus + " (expected 'Closed')");
        Assert.assertEquals(closedStatus, "Closed",
                "Shipment WEB-B should be 'Closed' after full refund. Actual: " + closedStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);

        LoggerUtility.info("TC_FBS_017 execution completed successfully — WEB-B closed, WEB-A untouched (still Received)");

        // All 96 steps have run; now surface any soft-assertion failures (e.g. Step 51 delivery
        // type) so the test still fails correctly if something was wrong.
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
    // run independently for one Mirakl shipment (same helper shape as TC_FBS_005/006). Each call
    // generates its own random tracking number; the same invoice PDF (fbs017.invoice.file.path)
    // is uploaded for both shipments.
    // ----------------------------------------------------------------
    private void runFbsFulfillmentFlow(String shipmentRef, String miraklDetailUrl) throws InterruptedException {
        LoggerUtility.info("========== FBS Fulfillment: " + shipmentRef + " ==========");
        switchToMiraklTab();
        driver.get(miraklDetailUrl);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 52-55: More actions -> Documents -> Add -> upload popup
        LoggerUtility.info("Step 52: Clicking More actions dropdown [" + shipmentRef + "]");
        miraklOrderDetailPage.clickMoreActionsDropdown();
        LoggerUtility.info("Step 53: Clicking Documents option under More actions [" + shipmentRef + "]");
        miraklOrderDetailPage.clickDocumentsOption();
        LoggerUtility.info("Step 54: Clicking the blue Add button in the Order documents section [" + shipmentRef + "]");
        miraklOrderDetailPage.clickAddDocumentButton();
        Assert.assertTrue(miraklOrderDetailPage.isUploadDocumentPopupDisplayed(),
                "Upload an order document popup should be displayed [" + shipmentRef + "]");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 56-59: Select Invoice type, upload file, confirm
        LoggerUtility.info("Step 56-57: Selecting document type " + TC_DOCUMENT_TYPE + " [" + shipmentRef + "]");
        miraklOrderDetailPage.selectDocumentType(TC_DOCUMENT_TYPE);
        LoggerUtility.info("Step 58: Uploading invoice PDF: " + config.getFbs017InvoiceFilePath() + " [" + shipmentRef + "]");
        miraklOrderDetailPage.uploadDocumentFile(config.getFbs017InvoiceFilePath());
        LoggerUtility.info("Step 59: Clicking Confirm button [" + shipmentRef + "]");
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

        // Steps 60-65: Add tracking information (DHL + independently generated tracking number)
        LoggerUtility.info("Step 60: Clicking Add tracking information link [" + shipmentRef + "]");
        miraklOrderDetailPage.clickAddTrackingInformationLink();
        LoggerUtility.info("Step 61-62: Selecting carrier " + TC_CARRIER + " [" + shipmentRef + "]");
        miraklOrderDetailPage.selectCarrier(TC_CARRIER);
        String trackingNumber = generateEightDigitTrackingNumber();
        LoggerUtility.info("Step 63-64: Entering tracking number: " + trackingNumber + " [" + shipmentRef + "]");
        miraklOrderDetailPage.enterTrackingNumber(trackingNumber);
        LoggerUtility.info("Step 65: Clicking Add button [" + shipmentRef + "]");
        miraklOrderDetailPage.clickAddTrackingButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 66: Verify the selected Carrier Name and Tracking Number are displayed
        LoggerUtility.info("Step 66: Verifying Carrier/Tracking Number via View tracking information [" + shipmentRef + "]");
        miraklOrderDetailPage.clickViewTrackingInformationLink();
        String displayedCarrier = miraklOrderDetailPage.getTrackingDialogCarrierName();
        Assert.assertTrue(displayedCarrier.toUpperCase().contains(TC_CARRIER),
                "Displayed carrier should be DHL [" + shipmentRef + "]. Actual: " + displayedCarrier);
        String displayedTracking = miraklOrderDetailPage.getTrackingDialogTrackingNumber();
        Assert.assertEquals(displayedTracking, trackingNumber,
                "Displayed tracking number should match entered value [" + shipmentRef + "]. Actual: " + displayedTracking);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        miraklOrderDetailPage.closeTrackingInfoDialog();

        // Step 67: Mark as Shipped
        LoggerUtility.info("Step 67: Clicking Mark as Shipped button [" + shipmentRef + "]");
        miraklOrderDetailPage.clickMarkAsShippedButton();
        String shippedStatus = waitForMiraklStatus("Shipped", 6);
        LoggerUtility.info("Mirakl status after Mark as Shipped [" + shipmentRef + "]: " + shippedStatus);
        Assert.assertEquals(shippedStatus, "Shipped",
                "Mirakl order status should be 'Shipped' [" + shipmentRef + "]. Actual: " + shippedStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 68-71: Custom field "Entregado" → Received
        LoggerUtility.info("Step 68: Clicking More actions dropdown [" + shipmentRef + "]");
        miraklOrderDetailPage.clickMoreActionsDropdown();
        LoggerUtility.info("Step 69: Clicking Custom field button [" + shipmentRef + "]");
        miraklOrderDetailPage.clickCustomFieldButton();
        LoggerUtility.info("Step 70: Clicking Entregado option [" + shipmentRef + "]");
        miraklOrderDetailPage.clickEntregadoOption();
        // Entregado has a Yes/No value dropdown that must be set before Confirm does anything.
        miraklOrderDetailPage.clickEntregadoValueDropdown();
        miraklOrderDetailPage.selectEntregadoYes();
        LoggerUtility.info("Step 71: Clicking Confirm button [" + shipmentRef + "]");
        miraklOrderDetailPage.clickCustomFieldConfirmButton();

        // Step 72: Verify status changes from Shipped to Received. The Entregado -> Received
        // transition is processed asynchronously on the backend — give the backend a head start,
        // then poll with more retries (same fix already applied in TC_FBS_004/005/006).
        LoggerUtility.info("Waiting 30s for Entregado update to propagate before polling [" + shipmentRef + "]...");
        Thread.sleep(30_000);
        String finalStatus = waitForMiraklStatus("Received", 20);
        LoggerUtility.info("Step 72: Mirakl status after Entregado [" + shipmentRef + "]: " + finalStatus + " (expected 'Received')");
        Assert.assertEquals(finalStatus, "Received",
                "Mirakl order status should change from 'Shipped' to 'Received' [" + shipmentRef + "]. Actual: " + finalStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info(shipmentRef + " fulfillment complete — Received");
    }

    // ----------------------------------------------------------------
    // GET Mirakl /api/orders?order_ids={shipmentRef} and return the first order_line_id found.
    // This is a per-class copy of ReturnApiUtility.getMiraklOrderLineId()'s exact logic (same
    // endpoint, same request shape, same response parsing) — that shared utility hardcodes
    // commercialId + "-A" internally and can only ever target WEB-A, but this test needs to
    // target WEB-B specifically. Per this project's "never change a utility class" rule,
    // ReturnApiUtility itself is not modified; instead this method takes the full shipment ref
    // as-is (already suffixed, e.g. orderId + "WEB-B") instead of appending a hardcoded suffix.
    // Same inline-Mirakl-API-call precedent as TC_FBO_030's offer_sku resolution.
    // ----------------------------------------------------------------
    private String getMiraklOrderLineIdForShipment(String shipmentRef) {
        String miraklBase = config.getMiraklUrl();
        if (miraklBase.endsWith("/")) {
            miraklBase = miraklBase.substring(0, miraklBase.length() - 1);
        }
        String url   = miraklBase + "/api/orders";
        String token = config.get("mirakl.api.token");

        LoggerUtility.info("========== Get Mirakl Order Line ID [" + shipmentRef + "] ==========");
        LoggerUtility.info("GET " + url + "?order_ids=" + shipmentRef);

        Response response = given()
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .queryParam("order_ids", shipmentRef)
                .when()
                .get(url)
                .then()
                .extract()
                .response();

        LoggerUtility.info("Get Order Line ID Status : " + response.getStatusCode());
        LoggerUtility.info("Get Order Line ID Body   : " + response.getBody().asString());

        if (response.getStatusCode() != 200) {
            throw new RuntimeException(
                    "Get Mirakl Order Line ID failed for " + shipmentRef + " — HTTP " + response.getStatusCode()
                    + " | Body: " + response.getBody().asString());
        }

        String orderLineId = response.jsonPath().getString("orders[0].order_lines[0].order_line_id");
        if (orderLineId == null || orderLineId.isEmpty()) {
            throw new RuntimeException(
                    "order_line_id not found in Mirakl API response for: " + shipmentRef
                    + " | Body: " + response.getBody().asString());
        }

        LoggerUtility.info("Order Line ID [" + shipmentRef + "]: " + orderLineId);
        return orderLineId;
    }

    // ----------------------------------------------------------------
    // Retry helper: calls ReturnApiUtility.postReturn() up to maxAttempts times. Same defensive
    // pattern as TC_FBS_013 — retrying only helps if the drop was momentary (see that class'
    // Javadoc for the Zscaler/SSLHandshakeException root cause this guards against).
    // ----------------------------------------------------------------
    private Response postReturnWithRetry(String orderCommercialId, String orderLineId, int maxAttempts) throws InterruptedException {
        Exception lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                LoggerUtility.info("Return API call attempt " + attempt + "/" + maxAttempts);
                return ReturnApiUtility.postReturn(orderCommercialId, orderLineId);
            } catch (Exception e) {
                lastError = e;
                LoggerUtility.warn("Return API call failed on attempt " + attempt + "/" + maxAttempts + ": " + e.getMessage());
                if (attempt < maxAttempts) Thread.sleep(10_000);
            }
        }
        throw new RuntimeException("Return API call failed after " + maxAttempts + " attempts — if this is "
                + "SSLHandshakeException/EOFException, verify the machine is connected to Zscaler "
                + "(or equivalent corporate VPN/proxy): return.service.url is an internal "
                + "*.cloud-ocp-stg.fahorro.com.mx host unreachable without it.", lastError);
    }

    // ----------------------------------------------------------------
    // Poll helper: refresh + read status up to maxRetries times. Sleeps 10s between retries
    // (not after the last one) — same fix as TC_FBS_007/013/014's waitForMiraklStatus, applied
    // here from the start since it's a proven-necessary pattern for slow Mirakl transitions.
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
    // Handle checkout window that may open in a new browser window after clickProceedToPayment —
    // poll-based version (see CLAUDE.md "switchToCheckoutWindow() must poll, not sample once").
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
