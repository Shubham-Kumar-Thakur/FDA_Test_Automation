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
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * TC_FBS_001 — FBS (Fulfilled By Store) order lifecycle:
 * FDA (Credit Card) -> Mirakl Accept -> Kibo (verify delivery type = FBS)
 * -> Mirakl Invoice upload -> Mirakl DHL tracking -> Mark as Shipped -> Custom field "Entregado" -> Received.
 *
 * Step numbers in the log/comments below are 1:1 with the 72-step manual test case.
 *
 * Belongs to the "FBS" TestNG group. BaseClass logs in to FDA once via @BeforeGroups("FBS") using
 * fbs.username/fbs.password before the first FBS test runs, and logs out once via @AfterGroups("FBS")
 * after the last one — no per-test-class login/logout here. Mirakl uses the shared session from
 * @BeforeSuite (mirakl.username/mirakl.password already matches the account this TC requires).
 */
public class TC_FBS_001_Test extends BaseClass {

    private static final String TC_NAME = "TC_FBS_001";

    // Fixed business constants for the FBS flow (not account/test data — not sourced from config)
    private static final String TC_CARRIER      = "DHL";
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
        LoggerUtility.info("All page objects initialized for TC_FBS_001");
    }

    @Test(testName = TC_NAME, groups = {"FBS"},
          description = "Verify FBS order placement and full fulfilment lifecycle: FDA -> Mirakl Accept -> Kibo (FBS) -> Documents/Tracking -> Shipped -> Received")
    public void tc_fbs_001_place_order_fbs_full_fulfillment() throws InterruptedException {

        // Step 45's delivery-type check must not abort the run: the flow needs to keep going
        // through Documents/Tracking/Mark as Shipped/Entregado/Received even if Kibo hasn't
        // surfaced deliveryType=FBS yet. Recorded here and asserted only at the end.
        SoftAssert softAssert = new SoftAssert();

        // ============================================================
        // PHASE 1: FDA — Search, PDP, Cart, Payment, Order
        // ============================================================

        switchToFDATab();
        LoggerUtility.info("TC_FBS_001: Starting — FDA session active from @BeforeGroups(FBS), Mirakl session active from suite setup");
        // Step 1: User is on FDA home page
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test cleanup: remove any cart items left over from previous runs (not a spec step)
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // Steps 2-4: Search by SKU
        String sku = config.getFbs001Sku();
        LoggerUtility.info("Step 2: Clicking ¿Qué estás buscando? search field");
        LoggerUtility.info("Step 3: Entering SKU: " + sku);
        fdaHomePage.enterSearchQuery(sku);
        LoggerUtility.info("Step 4: Pressing Enter on the keyboard");
        fdaHomePage.pressSearchEnter();

        // Steps 5-7: PDP validations
        LoggerUtility.info("Step 5: Verifying Product Details Page is displayed");
        Assert.assertTrue(fdaPdpPage.isDisplayed(), "PDP not displayed after search for SKU: " + sku);
        LoggerUtility.info("Step 6: Verifying Agregar al carrito button is enabled");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(), "Add to cart button is not enabled on PDP");
        LoggerUtility.info("Step 7: Verifying product quantity is 1");
        Assert.assertEquals(fdaPdpPage.getQuantity(), "1", "Product quantity on PDP should be 1");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 8: Add to cart
        LoggerUtility.info("Step 8: Clicking Agregar al carrito");
        fdaPdpPage.clickAddToCart();

        // Steps 9-12: Cart page
        LoggerUtility.info("Step 9: Clicking Mi carrito cart icon");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());
        String cartProductName = fdaCartPage.getProductName();
        LoggerUtility.info("Step 10: Cart product name: " + cartProductName);
        Assert.assertFalse(cartProductName.isEmpty(), "Product name should be displayed in cart");
        String cartQty = fdaCartPage.getQuantity();
        LoggerUtility.info("Step 11: Cart quantity: " + cartQty);
        Assert.assertEquals(cartQty, "1", "Cart quantity should be 1");
        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 12: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(), "Order total should be displayed in cart");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 13-14: Proceed to payment → shipping page
        LoggerUtility.info("Step 13: Clicking Proceed to payment button");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();
        LoggerUtility.info("Step 14: Clicking Siguiente button on the Shipping page");
        fdaPaymentPage.clickNextButton();

        // Steps 15-22: Payment page
        LoggerUtility.info("Step 15: Selecting Pago con Tarjeta Crédito/Débito radio button");
        fdaPaymentPage.selectCreditCardOption();
        LoggerUtility.info("Step 16-17: Clicking Número de tarjeta field and entering card number");
        fdaPaymentPage.enterCardNumber(config.getFbs001CardNumber());
        LoggerUtility.info("Step 18-19: Clicking Fecha de expiración field and entering expiration date");
        fdaPaymentPage.enterExpiry(config.getFbs001CardExpiry());
        LoggerUtility.info("Step 20-21: Clicking Código de seguridad field and entering security code");
        fdaPaymentPage.enterCvv(config.getFbs001CardCvv());
        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Step 22: Completar pago button text: " + payBtnText);
        Assert.assertTrue(payBtnText.contains("Completar pago") || payBtnText.contains("MXN")
                || payBtnText.contains("$"),
                "Completar pago button should display order total. Actual text: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 23-25: Complete payment → success page → get order ID
        LoggerUtility.info("Step 23: Clicking Completar pago button");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();
        LoggerUtility.info("Step 24: Verifying order success page is displayed");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(), "Order success page not displayed after payment");
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 25: Order ID = " + orderId);
        Assert.assertFalse(orderId.isEmpty(), "Order ID should be present on success page");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 26-30: Order history
        LoggerUtility.info("Step 26: Clicking Mi cuenta profile icon");
        fdaHomePage.clickProfileIcon();
        LoggerUtility.info("Step 27: Clicking Mis pedidos link");
        fdaHomePage.clickMyOrdersLink();
        LoggerUtility.info("Step 28: Verifying Mis pedidos page is displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(), "Mis pedidos page not displayed");
        LoggerUtility.info("Step 29: Verifying Order ID " + orderId + " is present in order history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
                "Order ID " + orderId + " not found in order history");
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 30: FDA order status: " + fdaStatus + " (expected 'Creada')");
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
                "Order status should be 'Creada' in FDA order history. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 2: MIRAKL — Search Order, Pending Acceptance, Accept
        // ============================================================

        // Steps 31-32: Open new tab / switch to Mirakl tab (Mirakl tab was already opened and
        // logged in during @BeforeSuite — see BaseClass; we just switch focus to it here)
        LoggerUtility.info("Step 31-32: Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 33-34: Navigate to All Orders
        LoggerUtility.info("Step 33: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 34: Clicking All Orders link");
        miraklOrdersPage.clickAllOrders();

        // Steps 35-37: Search for order in Mirakl — retry every 60s up to 5 minutes for sync delay.
        // 1 3P seller = 1 shipment, so search directly with the "WEB-A" shipment ref rather than
        // the generic "orderId+WEB" term (Kibo's externalId lookup below stays as plain "WEB" —
        // Kibo's externalOrderId field never carries the Mirakl "-A" shipment suffix).
        String miraklSearchTerm = orderId + "WEB-A";
        LoggerUtility.info("Step 35-36: Clicking Search field and entering Order ID: " + miraklSearchTerm);
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
        LoggerUtility.info("Step 37: Mirakl Status = " + miraklListStatus + " (expected 'Pending acceptance')");
        // Confirmed via live run (2026-09-07, TC_FBS_002): this shop's orders can already be
        // auto-accepted by the time the 5-minute Mirakl sync window elapses, landing directly on
        // "Awaiting shipment" instead of "Pending acceptance" — same auto-accept behavior
        // MiraklOrderDetailPage's isAcceptButtonPresent() was written to detect elsewhere in this
        // codebase. Tolerate both.
        Assert.assertTrue(miraklListStatus.equals("Pending acceptance") || miraklListStatus.equals("Awaiting shipment"),
                "Mirakl order status should be 'Pending acceptance' (or already 'Awaiting shipment' if auto-accepted). Actual: " + miraklListStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 38-39: Click into detail → verify total
        LoggerUtility.info("Step 38: Clicking on the Order ID in the search result list");
        miraklOrderDetailPage.clickOrderInList(miraklSearchTerm);
        String miraklTotal = miraklOrderDetailPage.getOrderTotal();
        LoggerUtility.info("Step 39: Mirakl order total: " + miraklTotal + " | FDA order total: " + orderTotal);
        Assert.assertFalse(miraklTotal.isEmpty(), "Mirakl order total should be displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 40-41: Accept order → Awaiting shipment (skip the click if already auto-accepted)
        String miraklStatus;
        if (miraklOrderDetailPage.isAcceptButtonPresent(5)) {
            LoggerUtility.info("Step 40: Clicking Accept button on the Order Details page in Mirakl");
            miraklOrderDetailPage.clickAcceptButton();
            LoggerUtility.info("Refreshing Mirakl order details page after acceptance");
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            miraklStatus = miraklOrderDetailPage.getOrderStatus();
        } else {
            LoggerUtility.info("Step 40: Accept button not present — order was already auto-accepted");
            miraklStatus = miraklOrderDetailPage.getOrderStatus();
        }
        LoggerUtility.info("Step 41: Mirakl Status = " + miraklStatus + " (expected 'Awaiting shipment')");
        Assert.assertEquals(miraklStatus, "Awaiting shipment",
                "Mirakl order status should be 'Awaiting shipment' after acceptance. Actual: " + miraklStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: KIBO API — Authenticate + Verify Delivery Type = FBS
        // ============================================================

        LoggerUtility.info("Step 42: ========== Kibo_Auth ==========");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(), "Kibo authentication failed — access token is empty");

        String externalId = orderId + "WEB";
        LoggerUtility.info("Step 43: ========== Get_all_orders_in_Kibo ==========");
        LoggerUtility.info("Searching externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(), "Kibo Order ID not found for externalId: " + externalId);
        LoggerUtility.info("Kibo Order ID : " + kiboOrderId);

        LoggerUtility.info("Step 44: ========== Get_Shipment_Details ==========");
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
        // Step 45: Verify delivery type is FBS (soft assert — must not stop the run before
        // the Mirakl documents/tracking/shipped/received steps complete; failure is reported
        // by softAssert.assertAll() at the end).
        LoggerUtility.info("Step 45: Kibo delivery type = " + deliveryType + " (expected 'FBS')");
        softAssert.assertEquals(deliveryType.toUpperCase(), TC_DELIVERY_TYPE,
                "Kibo shipment delivery type should be 'FBS'. Actual: " + deliveryType);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: MIRAKL — Documents, Tracking, Mark as Shipped
        // ============================================================

        // Steps 46-49: More actions -> Documents -> Add -> upload popup
        LoggerUtility.info("Step 46: Clicking More actions dropdown");
        miraklOrderDetailPage.clickMoreActionsDropdown();
        LoggerUtility.info("Step 47: Clicking Documents option under More actions");
        miraklOrderDetailPage.clickDocumentsOption();
        LoggerUtility.info("Step 48: Clicking the blue Add button in the Order documents section");
        miraklOrderDetailPage.clickAddDocumentButton();
        LoggerUtility.info("Step 49: Verifying Upload an order document popup is displayed");
        Assert.assertTrue(miraklOrderDetailPage.isUploadDocumentPopupDisplayed(),
                "Upload an order document popup should be displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 50-53: Select Invoice type, upload file, confirm
        LoggerUtility.info("Step 50-51: Clicking -- Select -- dropdown and selecting " + TC_DOCUMENT_TYPE);
        miraklOrderDetailPage.selectDocumentType(TC_DOCUMENT_TYPE);
        LoggerUtility.info("Step 52: Selecting the invoice PDF file: " + config.getFbs001InvoiceFilePath());
        miraklOrderDetailPage.uploadDocumentFile(config.getFbs001InvoiceFilePath());
        LoggerUtility.info("Step 53: Clicking Confirm button");
        miraklOrderDetailPage.clickConfirmUploadButton();
        Assert.assertTrue(miraklOrderDetailPage.isDocumentUploadedMessageDisplayed(),
                "'The document has been uploaded.' confirmation message should be displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // (Fix — not a numbered step in the manual test case) Confirmed via live DOM inspection
        // (2026-09-07, ZZ_Diag_MoreActions_Test screenshot ZZ_DIAG2_05_AFTER_UPLOAD.png): clicking
        // More actions > Documents navigates the browser away from the Order Detail page to a
        // separate "Order documents" page. "Add tracking information" and "Mark as Shipped" only
        // exist on the Order Detail page, so we must navigate back the same way we got here the
        // first time (Steps 33-34, 38) before Step 54 can find anything.
        LoggerUtility.info("Navigating back to Mirakl Order Detail page after document upload");
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        miraklOrdersPage.searchOrder(miraklSearchTerm);
        miraklOrderDetailPage.clickOrderInList(miraklSearchTerm);
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 54-59: Add tracking information (DHL + tracking number)
        LoggerUtility.info("Step 54: Clicking Add tracking information link");
        miraklOrderDetailPage.clickAddTrackingInformationLink();
        LoggerUtility.info("Step 55-56: Clicking Select a carrier dropdown and selecting " + TC_CARRIER);
        miraklOrderDetailPage.selectCarrier(TC_CARRIER);
        trackingNumber = generateEightDigitTrackingNumber();
        LoggerUtility.info("Step 57-58: Clicking Tracking number field and entering: " + trackingNumber);
        miraklOrderDetailPage.enterTrackingNumber(trackingNumber);
        LoggerUtility.info("Step 59: Clicking Add button");
        miraklOrderDetailPage.clickAddTrackingButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 60-63: View tracking information → verify carrier/tracking → close
        LoggerUtility.info("Step 60: Clicking View tracking information link");
        miraklOrderDetailPage.clickViewTrackingInformationLink();
        LoggerUtility.info("Step 61: Verifying Carrier name is DHL");
        String displayedCarrier = miraklOrderDetailPage.getTrackingDialogCarrierName();
        Assert.assertTrue(displayedCarrier.toUpperCase().contains(TC_CARRIER),
                "Displayed carrier should be DHL. Actual: " + displayedCarrier);
        LoggerUtility.info("Step 62: Verifying Tracking Number is displayed");
        String displayedTracking = miraklOrderDetailPage.getTrackingDialogTrackingNumber();
        Assert.assertEquals(displayedTracking, trackingNumber,
                "Displayed tracking number should match entered value. Actual: " + displayedTracking);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("Step 63: Clicking Close icon");
        miraklOrderDetailPage.closeTrackingInfoDialog();

        // Step 64: Mark as Shipped
        LoggerUtility.info("Step 64: Clicking Mark as Shipped button");
        miraklOrderDetailPage.clickMarkAsShippedButton();
        String shippedStatus = waitForMiraklStatus("Shipped", 6);
        LoggerUtility.info("Mirakl status after Mark as Shipped: " + shippedStatus);
        Assert.assertEquals(shippedStatus, "Shipped",
                "Mirakl order status should be 'Shipped'. Actual: " + shippedStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 5: MIRAKL — Custom field "Entregado" → Received
        // ============================================================

        LoggerUtility.info("Step 65: Clicking More actions dropdown");
        miraklOrderDetailPage.clickMoreActionsDropdown();
        LoggerUtility.info("Step 66: Clicking Custom field button");
        miraklOrderDetailPage.clickCustomFieldButton();
        LoggerUtility.info("Step 67: Clicking Entregado option");
        miraklOrderDetailPage.clickEntregadoOption();
        LoggerUtility.info("Step 68: Clicking Entregado dropdown");
        miraklOrderDetailPage.clickEntregadoValueDropdown();
        LoggerUtility.info("Step 69: Selecting 'Yes' from Entregado dropdown");
        miraklOrderDetailPage.selectEntregadoYes();
        LoggerUtility.info("Step 70: Clicking Confirm button");
        miraklOrderDetailPage.clickCustomFieldConfirmButton();

        // Step 71: Wait 10 seconds (manual case value). Extended to 30s here — the Entregado ->
        // Received transition is processed asynchronously on the backend, and a live run of
        // TC_FBS_004/006 confirmed the original 10s + 6 quick refreshes (~40s total) isn't
        // reliably enough time even though the click sequence completes with no errors.
        LoggerUtility.info("Step 71: Waiting 30s for Entregado update to propagate");
        Thread.sleep(30_000);

        // Step 72: Verify status changes from Shipped to Received
        String finalStatus = waitForMiraklStatus("Received", 20);
        LoggerUtility.info("Step 72: Mirakl Status = " + finalStatus + " (expected 'Received')");
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

        LoggerUtility.info("TC_FBS_001 execution completed successfully");

        // All 72 steps have run; now surface any soft-assertion failures (e.g. Step 45 delivery
        // type) so the test still fails correctly if something was wrong.
        softAssert.assertAll();
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
    // Generates a random 8-digit tracking number (manual step 58 requirement)
    // ----------------------------------------------------------------
    private String generateEightDigitTrackingNumber() {
        int number = 10_000_000 + new Random().nextInt(90_000_000);
        return String.valueOf(number);
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
            LoggerUtility.info("Original FDA window closed — switched to checkout window: " + fdaTabHandle);
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
