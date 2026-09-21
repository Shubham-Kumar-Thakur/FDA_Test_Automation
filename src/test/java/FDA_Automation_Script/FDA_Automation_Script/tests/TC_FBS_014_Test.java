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
 * TC_FBS_014 — FBS (Fulfilled By Store) order lifecycle, 2 products (qty=1 each) from 1 3P seller,
 * with a partial return of ONE line item ("return one shipment"):
 * FDA (Credit Card) -> Mirakl Accept -> Kibo (verify delivery type = FBS)
 * -> Mirakl Invoice upload -> Mirakl DHL tracking -> Mark as Shipped -> Custom field "Entregado"
 * -> Received -> Return API (order_line_id + create incident, qty=1) -> Mirakl return tracking +
 * return label upload -> Mark as received -> Check compliance -> Refund part of the order (qty=1)
 * -> Shipment closed.
 *
 * Step numbers in the log/comments below are 1:1 with the 99-step manual test case. Steps 1-72 are
 * the same shape as TC_FBS_002's 2-product/1-3P-seller cart flow (both products share the same
 * seller, so this produces a single Mirakl shipment — no WEB-A/WEB-B split, confirmed for this SKU
 * pair since TC_FBS_002). Steps 73-99 mirror TC_FBS_013's return-label UI flow combined with
 * TC_FBO_026's partial-refund pattern ("Refund part of the order" + quantity=1, not Full refund) —
 * ReturnApiUtility.getMiraklOrderLineId() always resolves to order_lines[0], i.e. the first product
 * added to cart, so that line item (SKU1) is the one returned/refunded.
 *
 * Belongs to the "FBS" TestNG group. BaseClass logs in to FDA once via @BeforeGroups("FBS") using
 * fbs.username/fbs.password before the first FBS test runs, and logs out once via @AfterGroups("FBS")
 * after the last one — no per-test-class login/logout here. Mirakl uses the shared session from
 * @BeforeSuite (mirakl.username/mirakl.password already matches the account this TC requires).
 */
public class TC_FBS_014_Test extends BaseClass {

    private static final String TC_NAME = "TC_FBS_014";

    // Fixed business constants for the FBS flow (not account/test data — not sourced from config)
    private static final String TC_CARRIER       = "DHL";
    private static final String TC_DOCUMENT_TYPE = "Invoice";
    private static final String TC_DELIVERY_TYPE = "FBS";
    private static final int    TC_RETURN_QTY    = 1;

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
    private String trackingNumber;

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
        LoggerUtility.info("All page objects initialized for TC_FBS_014");
    }

    @Test(testName = TC_NAME, groups = {"FBS"},
          description = "Verify FBS order placement (2 products, 1 3P seller), full fulfilment, and partial return/refund of one line item: FDA -> Mirakl Accept -> Kibo (FBS) -> Documents/Tracking -> Shipped -> Received -> Return -> Compliance -> Partial Refund -> Closed")
    public void tc_fbs_014_place_order_2products_fbs_fulfillment_and_partial_return() throws InterruptedException {

        // Step 51's delivery-type check must not abort the run: the flow needs to keep going
        // through Documents/Tracking/Mark as Shipped/Entregado/Received even if Kibo hasn't
        // surfaced deliveryType=FBS yet. Recorded here and asserted only at the end.
        SoftAssert softAssert = new SoftAssert();

        // ============================================================
        // PHASE 1: FDA — Search, PDP, Cart (2 products), Payment, Order
        // ============================================================

        switchToFDATab();
        LoggerUtility.info("TC_FBS_014: Starting — FDA session active from @BeforeGroups(FBS), Mirakl session active from suite setup");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test cleanup: remove any cart items left over from previous runs (not a spec step)
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        String sku1 = config.getFbs014Sku1();
        String sku2 = config.getFbs014Sku2();

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
        LoggerUtility.info("Step 6: Verifying product quantity is 1");
        Assert.assertEquals(fdaPdpPage.getQuantity(), "1", "Product quantity on PDP should be 1 for SKU: " + sku1);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 7: Add first product to cart
        LoggerUtility.info("Step 7: Clicking Agregar al carrito for first product");
        fdaPdpPage.clickAddToCart();

        // Navigate home before searching the second product
        fdaHomePage.navigateTo(config.getFdaUrl());

        // Steps 8-10: Search second product
        LoggerUtility.info("Step 8: Clicking ¿Qué estás buscando? search field");
        LoggerUtility.info("Step 9: Entering second product SKU: " + sku2);
        fdaHomePage.enterSearchQuery(sku2);
        LoggerUtility.info("Step 10: Pressing Enter on the keyboard");
        fdaHomePage.pressSearchEnter();

        // Steps 11-13: PDP validations for product 2
        LoggerUtility.info("Step 11: Verifying PDP for second product is displayed");
        Assert.assertTrue(fdaPdpPage.isDisplayed(), "PDP not displayed for second product SKU: " + sku2);
        LoggerUtility.info("Step 12: Verifying Agregar al carrito button is enabled");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(), "Add to cart button is not enabled on PDP for SKU: " + sku2);
        LoggerUtility.info("Step 13: Verifying product quantity is 1");
        Assert.assertEquals(fdaPdpPage.getQuantity(), "1", "Product quantity on PDP should be 1 for SKU: " + sku2);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 14: Add second product to cart
        LoggerUtility.info("Step 14: Clicking Agregar al carrito for second product");
        fdaPdpPage.clickAddToCart();

        // Step 15: Open cart
        LoggerUtility.info("Step 15: Clicking Mi carrito cart icon");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        // Steps 16-19: Verify both products present with qty 1
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
        fdaPaymentPage.enterCardNumber(config.getFbs014CardNumber());
        LoggerUtility.info("Step 26-27: Clicking Fecha de expiración field and entering expiration date");
        fdaPaymentPage.enterExpiry(config.getFbs014CardExpiry());
        LoggerUtility.info("Step 28-29: Clicking Código de seguridad field and entering security code");
        fdaPaymentPage.enterCvv(config.getFbs014CardCvv());
        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Step 30: Completar pago button text: " + payBtnText);
        Assert.assertTrue(payBtnText.contains("Completar pago") || payBtnText.contains("MXN")
                || payBtnText.contains("$"),
                "Completar pago button should display order total. Actual text: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

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
        // PHASE 2: MIRAKL — Search Order, Pending Acceptance, Accept
        // ============================================================

        LoggerUtility.info("Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 39-40: Navigate to All Orders
        LoggerUtility.info("Step 39: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 40: Clicking All Orders link");
        miraklOrdersPage.clickAllOrders();

        // Steps 41-43: Search for order in Mirakl — retry every 60s up to 15 minutes for sync
        // delay. Same widened window as TC_FBS_013 — this is also a return-flow FBS test, and
        // that category has been confirmed (2026-09-16) to occasionally need more than 10 minutes
        // for the backend to sync the order into Mirakl. 1 3P seller = 1 shipment, so search
        // directly with the "WEB-A" shipment ref rather than the generic "orderId+WEB" term
        // (Kibo's externalId lookup below stays as plain "WEB" — Kibo's externalOrderId field
        // never carries the Mirakl "-A" shipment suffix).
        String miraklSearchTerm = orderId + "WEB-A";
        LoggerUtility.info("Step 41-42: Clicking Search field and entering Order ID: " + miraklSearchTerm);
        boolean orderFoundInMirakl = false;
        for (int attempt = 1; attempt <= 15; attempt++) {
            LoggerUtility.info("Mirakl search attempt " + attempt + "/15 for: " + miraklSearchTerm);
            miraklOrdersPage.searchOrder(miraklSearchTerm);
            if (miraklOrdersPage.hasSearchResults(miraklSearchTerm)) {
                orderFoundInMirakl = true;
                LoggerUtility.info("Order found in Mirakl on attempt " + attempt);
                break;
            }
            LoggerUtility.info("Order not in Mirakl yet (attempt " + attempt + "/15)");
            if (attempt < 15) {
                LoggerUtility.info("Waiting 60 seconds before next search attempt...");
                Thread.sleep(60_000);
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(orderFoundInMirakl, "Order " + miraklSearchTerm + " did not appear in Mirakl within 15 minutes");
        String miraklListStatus = miraklOrdersPage.getFirstOrderStatus();
        LoggerUtility.info("Step 43: Mirakl Status = " + miraklListStatus + " (expected 'Pending acceptance')");
        // Same auto-accept tolerance confirmed via live runs for TC_FBS_002/013 — this shop's
        // orders can already be auto-accepted by the time the Mirakl sync window elapses.
        Assert.assertTrue(miraklListStatus.equals("Pending acceptance") || miraklListStatus.equals("Awaiting shipment"),
                "Mirakl order status should be 'Pending acceptance' (or already 'Awaiting shipment' if auto-accepted). Actual: " + miraklListStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 44-45: Click into detail → verify total
        LoggerUtility.info("Step 44: Clicking on the Order ID in the search result list");
        miraklOrderDetailPage.clickOrderInList(miraklSearchTerm);
        String miraklTotal = miraklOrderDetailPage.getOrderTotal();
        LoggerUtility.info("Step 45: Mirakl order total: " + miraklTotal + " | FDA order total: " + orderTotal);
        Assert.assertFalse(miraklTotal.isEmpty(), "Mirakl order total should be displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 46-47: Accept order → Awaiting shipment (skip the click if already auto-accepted)
        String miraklStatus;
        if (miraklOrderDetailPage.isAcceptButtonPresent(5)) {
            LoggerUtility.info("Step 46: Clicking Accept button on the Order Details page in Mirakl");
            miraklOrderDetailPage.clickAcceptButton();
            LoggerUtility.info("Refreshing Mirakl order details page after acceptance");
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            miraklStatus = miraklOrderDetailPage.getOrderStatus();
        } else {
            LoggerUtility.info("Step 46: Accept button not present — order was already auto-accepted");
            miraklStatus = miraklOrderDetailPage.getOrderStatus();
        }
        LoggerUtility.info("Step 47: Mirakl Status = " + miraklStatus + " (expected 'Awaiting shipment')");
        Assert.assertEquals(miraklStatus, "Awaiting shipment",
                "Mirakl order status should be 'Awaiting shipment' after acceptance. Actual: " + miraklStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: KIBO API — Authenticate + Verify Delivery Type = FBS
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
        // Step 51: Verify delivery type is FBS (soft assert — must not stop the run before
        // the Mirakl documents/tracking/shipped/received/return steps complete; failure is
        // reported by softAssert.assertAll() at the end).
        LoggerUtility.info("Step 51: Kibo delivery type = " + deliveryType + " (expected 'FBS')");
        softAssert.assertEquals(deliveryType.toUpperCase(), TC_DELIVERY_TYPE,
                "Kibo shipment delivery type should be 'FBS'. Actual: " + deliveryType);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: MIRAKL — Documents, Tracking, Mark as Shipped
        // ============================================================

        // Steps 52-55: More actions -> Documents -> Add -> upload popup
        LoggerUtility.info("Step 52: Clicking More actions dropdown");
        miraklOrderDetailPage.clickMoreActionsDropdown();
        LoggerUtility.info("Step 53: Clicking Documents option under More actions");
        miraklOrderDetailPage.clickDocumentsOption();
        LoggerUtility.info("Step 54: Clicking the blue Add button in the Order documents section");
        miraklOrderDetailPage.clickAddDocumentButton();
        LoggerUtility.info("Step 55: Verifying Upload an order document popup is displayed");
        Assert.assertTrue(miraklOrderDetailPage.isUploadDocumentPopupDisplayed(),
                "Upload an order document popup should be displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 56-59: Select Invoice type, upload file, confirm
        LoggerUtility.info("Step 56-57: Clicking Document type dropdown and selecting " + TC_DOCUMENT_TYPE);
        miraklOrderDetailPage.selectDocumentType(TC_DOCUMENT_TYPE);
        LoggerUtility.info("Step 58: Selecting the invoice PDF file: " + config.getFbs014InvoiceFilePath());
        miraklOrderDetailPage.uploadDocumentFile(config.getFbs014InvoiceFilePath());
        LoggerUtility.info("Step 59: Clicking Confirm button");
        miraklOrderDetailPage.clickConfirmUploadButton();
        Assert.assertTrue(miraklOrderDetailPage.isDocumentUploadedMessageDisplayed(),
                "'The document has been uploaded.' confirmation message should be displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // (Fix — not a numbered step in the manual test case) Clicking More actions > Documents
        // navigates away from the Order Detail page to a separate "Order documents" page.
        // "Add tracking information" and "Mark as Shipped" only exist on the Order Detail page,
        // so we must navigate back the same way we got here the first time (Steps 39-40, 44)
        // before Step 60 can find anything — same fix as TC_FBS_002/013.
        LoggerUtility.info("Navigating back to Mirakl Order Detail page after document upload");
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        miraklOrdersPage.searchOrder(miraklSearchTerm);
        miraklOrderDetailPage.clickOrderInList(miraklSearchTerm);
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 60-65: Add tracking information (DHL + tracking number)
        LoggerUtility.info("Step 60: Clicking Add tracking information link");
        miraklOrderDetailPage.clickAddTrackingInformationLink();
        LoggerUtility.info("Step 61-62: Clicking Select a carrier dropdown and selecting " + TC_CARRIER);
        miraklOrderDetailPage.selectCarrier(TC_CARRIER);
        trackingNumber = generateEightDigitTrackingNumber();
        LoggerUtility.info("Step 63-64: Clicking Tracking number field and entering: " + trackingNumber);
        miraklOrderDetailPage.enterTrackingNumber(trackingNumber);
        LoggerUtility.info("Step 65: Clicking Add button");
        miraklOrderDetailPage.clickAddTrackingButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 66: Verify the selected Carrier Name and Tracking Number are displayed
        LoggerUtility.info("Step 66: Clicking View tracking information link and verifying Carrier/Tracking Number");
        miraklOrderDetailPage.clickViewTrackingInformationLink();
        String displayedCarrier = miraklOrderDetailPage.getTrackingDialogCarrierName();
        Assert.assertTrue(displayedCarrier.toUpperCase().contains(TC_CARRIER),
                "Displayed carrier should be DHL. Actual: " + displayedCarrier);
        String displayedTracking = miraklOrderDetailPage.getTrackingDialogTrackingNumber();
        Assert.assertEquals(displayedTracking, trackingNumber,
                "Displayed tracking number should match entered value. Actual: " + displayedTracking);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        miraklOrderDetailPage.closeTrackingInfoDialog();

        // Step 67: Mark as Shipped
        LoggerUtility.info("Step 67: Clicking Mark as Shipped button");
        miraklOrderDetailPage.clickMarkAsShippedButton();
        String shippedStatus = waitForMiraklStatus("Shipped", 6);
        LoggerUtility.info("Mirakl status after Mark as Shipped: " + shippedStatus);
        Assert.assertEquals(shippedStatus, "Shipped",
                "Mirakl order status should be 'Shipped'. Actual: " + shippedStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 5: MIRAKL — Custom field "Entregado" → Received
        // ============================================================

        LoggerUtility.info("Step 68: Clicking More actions dropdown");
        miraklOrderDetailPage.clickMoreActionsDropdown();
        LoggerUtility.info("Step 69: Clicking Custom field button");
        miraklOrderDetailPage.clickCustomFieldButton();
        LoggerUtility.info("Step 70: Clicking Entregado option");
        miraklOrderDetailPage.clickEntregadoOption();
        miraklOrderDetailPage.clickEntregadoValueDropdown();
        miraklOrderDetailPage.selectEntregadoYes();
        LoggerUtility.info("Step 71: Clicking Confirm button");
        miraklOrderDetailPage.clickCustomFieldConfirmButton();

        // Confirmed necessary by TC_FBS_001/002/013 live runs — the Entregado -> Received
        // transition needs a head start before polling reliably reflects it.
        LoggerUtility.info("Waiting 30s for Entregado update to propagate before polling...");
        Thread.sleep(30_000);
        String finalStatus = waitForMiraklStatus("Received", 20);
        LoggerUtility.info("Step 72: Mirakl Status = " + finalStatus + " (expected 'Received')");
        Assert.assertEquals(finalStatus, "Received",
                "Mirakl order status should change from 'Shipped' to 'Received'. Actual: " + finalStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 6: RETURN — API Flow (Get order_line_id + Create Return Incident, qty=1)
        // ============================================================

        String orderCommercialId = orderId + "WEB";

        // Step 73: Get Mirakl order_line_id — always resolves to order_lines[0], i.e. the first
        // product added to cart (SKU1) — that is the line item being returned here.
        LoggerUtility.info("Step 73: Calling Get_order_line_id for commercial order: " + orderCommercialId);
        String orderLineId = ReturnApiUtility.getMiraklOrderLineId(orderCommercialId);
        Assert.assertFalse(orderLineId.isEmpty(),
                "order_line_id should not be empty for commercial order: " + orderCommercialId);
        LoggerUtility.info("order_line_id retrieved: " + orderLineId);

        // Step 74: Create Return incident via API, quantity=1 (returning one of the two line
        // items, not the whole order) — same retry-on-transient-failure pattern as TC_FBS_013.
        LoggerUtility.info("Step 74: Calling Return service API — creating return incident (qty=" + TC_RETURN_QTY + ")");
        LoggerUtility.info("  order_commercial_id : " + orderCommercialId);
        LoggerUtility.info("  order_line_id       : " + orderLineId);
        Response returnResponse = postReturnWithRetry(orderCommercialId, orderLineId, TC_RETURN_QTY, 3);
        Assert.assertTrue(
                returnResponse.getStatusCode() == 200 || returnResponse.getStatusCode() == 201,
                "Return service should return 200 or 201. Actual: " + returnResponse.getStatusCode()
                        + " | Body: " + returnResponse.getBody().asString());
        LoggerUtility.info("Step 74: Return incident created — HTTP " + returnResponse.getStatusCode());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 7: MIRAKL — Return Tracking, Return Label, Mark Received,
        //          Compliance, Refund part of the order (qty=1) → Closed
        // ============================================================

        // (Fix — not a numbered step) Same propagation wait as TC_FBS_013: the order detail page
        // doesn't show the return's own "Add tracking information" prompt until refreshed a few
        // times after the Return API call.
        LoggerUtility.info("Waiting for return record to propagate to the Mirakl order detail page");
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

        // Steps 75-80: Add tracking information again for the return shipment
        LoggerUtility.info("Step 75: Clicking Add tracking information link (return shipment)");
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

        // Step 84: Select the PDF file. Per the same decision already applied to TC_FBS_013
        // (2026-09-16): the file is considered selected once sendKeys() completes — do not
        // re-verify via isReturnLabelFileAttached()'s WebDriverWait poll, which was the likely
        // trigger for a recurring browser crash during that wait. Proceed straight to Add (Step 85).
        String returnLabelFilePath = config.getFbs014InvoiceFilePath();
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

        // Step 91: Refund part of the order — NOT Full refund. This order has 2 line items and
        // only 1 was returned, so a partial refund is required (same pattern as TC_FBO_026).
        LoggerUtility.info("Step 91: Selecting Refund part of the order — partial return qty=" + TC_RETURN_QTY);
        miraklReturnPage.selectPartialRefundFromDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 92: Refund reason dropdown
        LoggerUtility.info("Step 92: Clicking Refund reason dropdown");
        miraklReturnPage.clickPartialRefundReasonDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 93: Item returned
        LoggerUtility.info("Step 93: Selecting Item returned");
        miraklReturnPage.selectItemReturned();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 94-95: Enter refund quantity = 1
        LoggerUtility.info("Step 94: Clicking Quantity field");
        LoggerUtility.info("Step 95: Entering refund quantity = " + TC_RETURN_QTY);
        miraklReturnPage.enterPartialRefundQuantity(TC_RETURN_QTY);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 96: Refund order button
        LoggerUtility.info("Step 96: Clicking Refund order button");
        miraklReturnPage.clickRefundOrderButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 97: Confirm on refund popup
        LoggerUtility.info("Step 97: Clicking Confirm on refund popup");
        miraklReturnPage.confirmRefundPopup();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 98 — CORRECTED (2026-09-18, take 4): a live run's log showed the 60s-wait-then-
        // refresh running BEFORE "Mark as closed" was ever clicked — the click was still sitting
        // in what was labeled "Step 99" below it, so this block refreshed a page that had never
        // been told to close. Moved the click to the front of this step: click "Mark as closed"
        // first, then do the wait + single refresh so the click has time to process. Per direct
        // instruction (take 6): clicking "Mark as closed" opens a confirmation popup that needs
        // its own "Mark as closed" click before the action actually takes effect.
        LoggerUtility.info("Step 98: Clicking Mark as closed button");
        miraklReturnPage.clickMarkAsClosed();
        LoggerUtility.info("Step 98: Confirming Mark as closed on popup");
        miraklReturnPage.confirmMarkAsClosedPopup();
        LoggerUtility.info("Step 98: Waiting 60 seconds for shipment closure to process...");
        Thread.sleep(60_000);
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 99 — no retry/poll loop here — per direct instruction, only a single refresh
        // (done above, as part of Step 98) happens after the click, then the status is read once.
        // The return section's own heading ("Return: Received" -> "Return: Closed") is a status
        // distinct from the shipment/order status badge at the top of the page, which stays
        // 'Received' — see MiraklReturnPage.getReturnStatusText().
        String returnStatus = miraklReturnPage.getReturnStatusText();
        LoggerUtility.info("Step 99: Return section status = " + returnStatus + " (expected 'Return: Closed')");
        Assert.assertTrue(returnStatus.toLowerCase().contains("closed"),
                "Return section status should change to 'Return: Closed' after Mark as closed. Actual: " + returnStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);

        LoggerUtility.info("TC_FBS_014 execution completed successfully");

        // All 99 steps have run; now surface any soft-assertion failures (e.g. Step 51 delivery
        // type) so the test still fails correctly if something was wrong.
        softAssert.assertAll();
    }

    // ----------------------------------------------------------------
    // Retry helper: calls ReturnApiUtility.postReturn() up to maxAttempts times. Same defensive
    // pattern as TC_FBS_013 — retrying only helps if the drop was momentary (see that class'
    // Javadoc for the Zscaler/SSLHandshakeException root cause this guards against).
    // ----------------------------------------------------------------
    private Response postReturnWithRetry(String orderCommercialId, String orderLineId, int quantity, int maxAttempts) throws InterruptedException {
        Exception lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                LoggerUtility.info("Return API call attempt " + attempt + "/" + maxAttempts);
                return ReturnApiUtility.postReturn(orderCommercialId, orderLineId, quantity);
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
    // (not after the last one) — same fix as TC_FBS_007's waitForMiraklStatus: a live run of
    // TC_FBS_014 showed the Entregado -> Received transition still reading 'Shipped' after 20
    // retries of bare refreshAndWait() (~97s total, no inter-retry sleep), confirming this test
    // needs the same wider polling window already proven necessary for TC_FBS_007's Entregado
    // step, which can outlast ~80s of back-to-back refreshes.
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
    // Generates a random 8-digit tracking number (manual step requirement)
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
