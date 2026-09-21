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
 * TC_FBS_015 — FBS (Fulfilled By Store) order lifecycle, 1 product qty=2 from 1 3P seller,
 * with a FULL return of the only line item:
 * FDA (Credit Card) -> Mirakl Accept -> Kibo (verify delivery type = FBS)
 * -> Mirakl Invoice upload -> Mirakl DHL tracking -> Mark as Shipped -> Custom field "Entregado"
 * -> Received -> Return API (order_line_id + create incident) -> Mirakl return tracking + return
 * label upload -> Mark as received -> Check compliance -> Full refund -> Shipment closed.
 *
 * Step numbers in the log/comments below are 1:1 with the 89-step manual test case. Steps 1-40
 * are TC_FBS_003's qty-increase-on-PDP flow (1 SKU raised from qty 1 to 2 instead of a second
 * product), through FDA order history and Mirakl Accept. Steps 41-89 are TC_FBS_013's Kibo
 * delivery-type check + Documents/Tracking/Mark-as-Shipped/Entregado/Received +
 * return/compliance/full-refund/Closed phase verbatim — unlike TC_FBS_014 (2 line items, only 1
 * returned, needed a manual "Mark as closed" click + Kibo return-status fallback), this order has
 * only ONE line item and it is returned in full, so the shipment legitimately reaches 'Closed' on
 * its own via a plain wait + refresh + status poll, same as TC_FBS_013/TC_FBO_020/TC_FBO_022 —
 * matching this test's own manual case, which has no "Mark as closed" step either.
 *
 * Belongs to the "FBS" TestNG group. BaseClass logs in to FDA once via @BeforeGroups("FBS") using
 * fbs.username/fbs.password before the first FBS test runs, and logs out once via @AfterGroups("FBS")
 * after the last one — no per-test-class login/logout here. Mirakl uses the shared session from
 * @BeforeSuite (mirakl.username/mirakl.password already matches the account this TC requires).
 */
public class TC_FBS_015_Test extends BaseClass {

    private static final String TC_NAME = "TC_FBS_015";

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
        LoggerUtility.info("All page objects initialized for TC_FBS_015");
    }

    @Test(testName = TC_NAME, groups = {"FBS"},
          description = "Verify FBS order placement (1 product qty=2, 1 3P seller), full fulfilment, and full return lifecycle: FDA -> Mirakl Accept -> Kibo (FBS) -> Documents/Tracking -> Shipped -> Received -> Return -> Compliance -> Full Refund -> Closed")
    public void tc_fbs_015_place_order_qty2_fbs_full_fulfillment_and_return() throws InterruptedException {

        // Step 44's delivery-type check must not abort the run: the flow needs to keep going
        // through Documents/Tracking/Mark as Shipped/Entregado/Received even if Kibo hasn't
        // surfaced deliveryType=FBS yet. Recorded here and asserted only at the end.
        SoftAssert softAssert = new SoftAssert();

        // ============================================================
        // PHASE 1: FDA — Search, PDP, Qty=2, Cart, Payment, Order
        // ============================================================

        switchToFDATab();
        LoggerUtility.info("TC_FBS_015: Starting — FDA session active from @BeforeGroups(FBS), Mirakl session active from suite setup");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test cleanup: remove any cart items left over from previous runs (not a spec step)
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // Steps 1-3: Search by SKU
        String sku = config.getFbs015Sku();
        LoggerUtility.info("Step 1: Clicking ¿Qué estás buscando? search field");
        LoggerUtility.info("Step 2: Entering SKU: " + sku);
        fdaHomePage.enterSearchQuery(sku);
        LoggerUtility.info("Step 3: Pressing Enter on the keyboard");
        fdaHomePage.pressSearchEnter();

        // Steps 4-6: PDP validations
        LoggerUtility.info("Step 4: Verifying Product Details Page is displayed");
        Assert.assertTrue(fdaPdpPage.isDisplayed(), "PDP not displayed after search for SKU: " + sku);
        LoggerUtility.info("Step 5: Verifying Agregar al carrito button is enabled");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(), "Add to cart button is not enabled on PDP");
        LoggerUtility.info("Step 6: Verifying product quantity is 1");
        Assert.assertEquals(fdaPdpPage.getQuantity(), "1", "Product quantity on PDP should be 1 before increment");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 7-8: Increase quantity from 1 to 2
        LoggerUtility.info("Step 7: Clicking plus button to increase quantity from 1 to " + TC_QUANTITY);
        fdaPdpPage.increaseQuantity();
        String pdpQty = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 8: Verifying product quantity is " + TC_QUANTITY + ". Actual: " + pdpQty);
        Assert.assertEquals(pdpQty, String.valueOf(TC_QUANTITY),
                "Product quantity on PDP should be " + TC_QUANTITY + " after clicking plus. Actual: " + pdpQty);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 9: Add to cart
        LoggerUtility.info("Step 9: Clicking Agregar al carrito");
        fdaPdpPage.clickAddToCart();

        // Steps 10-13: Cart page
        LoggerUtility.info("Step 10: Clicking Mi carrito cart icon");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());
        String cartProductName = fdaCartPage.getProductName();
        LoggerUtility.info("Step 11: Cart product name: " + cartProductName);
        Assert.assertFalse(cartProductName.isEmpty(), "Product name should be displayed in cart");
        String cartQty = fdaCartPage.getQuantity();
        LoggerUtility.info("Step 12: Cart quantity: " + cartQty + " (expected '" + TC_QUANTITY + "')");
        Assert.assertEquals(cartQty, String.valueOf(TC_QUANTITY), "Cart quantity should be " + TC_QUANTITY);
        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 13: Order total for " + TC_QUANTITY + " quantities: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(), "Order total should be displayed in cart");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 14-15: Proceed to payment → shipping page
        LoggerUtility.info("Step 14: Clicking Proceed to payment button");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();
        LoggerUtility.info("Step 15: Clicking Siguiente button on the Shipping page");
        fdaPaymentPage.clickNextButton();

        // Steps 16-23: Payment page
        LoggerUtility.info("Step 16: Selecting Pago con Tarjeta Crédito/Débito radio button");
        fdaPaymentPage.selectCreditCardOption();
        LoggerUtility.info("Step 17-18: Clicking Número de tarjeta field and entering card number");
        fdaPaymentPage.enterCardNumber(config.getFbs015CardNumber());
        LoggerUtility.info("Step 19-20: Clicking Fecha de expiración field and entering expiration date");
        fdaPaymentPage.enterExpiry(config.getFbs015CardExpiry());
        LoggerUtility.info("Step 21-22: Clicking Código de seguridad field and entering security code");
        fdaPaymentPage.enterCvv(config.getFbs015CardCvv());
        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Step 23: Completar pago button text: " + payBtnText);
        Assert.assertTrue(payBtnText.contains("Completar pago") || payBtnText.contains("MXN")
                || payBtnText.contains("$"),
                "Completar pago button should display order total for " + TC_QUANTITY + " quantities. Actual text: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // (Fix — not a numbered step) Re-verify all three payment fields immediately before
        // submitting — same Adyen async-iframe-re-render mitigation as TC_FBS_013. See
        // FDAPaymentPage.verifyAndReenterIfNeeded() for the full explanation.
        fdaPaymentPage.verifyAndReenterIfNeeded(config.getFbs015CardNumber(), config.getFbs015CardExpiry(),
                config.getFbs015CardCvv());

        // Steps 24-26: Complete payment → success page → get order ID
        LoggerUtility.info("Step 24: Clicking Completar pago button");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();
        LoggerUtility.info("Step 25: Verifying order success page is displayed");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(), "Order success page not displayed after payment");
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 26: Order ID = " + orderId);
        Assert.assertFalse(orderId.isEmpty(), "Order ID should be present on success page");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 27-31: Order history
        LoggerUtility.info("Step 27: Clicking Mi cuenta profile icon");
        fdaHomePage.clickProfileIcon();
        LoggerUtility.info("Step 28: Clicking Mis pedidos link");
        fdaHomePage.clickMyOrdersLink();
        LoggerUtility.info("Step 29: Verifying Mis pedidos page is displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(), "Mis pedidos page not displayed");
        LoggerUtility.info("Step 30: Verifying Order ID " + orderId + " is present in order history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
                "Order ID " + orderId + " not found in order history");
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 31: FDA order status: " + fdaStatus + " (expected 'Creada')");
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

        // Steps 32-33: Navigate to All Orders
        LoggerUtility.info("Step 32: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 33: Clicking All Orders link");
        miraklOrdersPage.clickAllOrders();

        // Steps 34-36: Search for order in Mirakl — retry every 60s up to 15 minutes for sync
        // delay (same widened window as TC_FBS_013/014 — this is also a return-flow FBS test).
        // 1 3P seller = 1 shipment, so search directly with the "WEB-A" shipment ref rather than
        // the generic "orderId+WEB" term (Kibo's externalId lookup below stays as plain "WEB" —
        // Kibo's externalOrderId field never carries the Mirakl "-A" shipment suffix).
        String miraklSearchTerm = orderId + "WEB-A";
        LoggerUtility.info("Step 34-35: Clicking Search field and entering Order ID: " + miraklSearchTerm);
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
        LoggerUtility.info("Step 36: Mirakl Status = " + miraklListStatus + " (expected 'Pending acceptance')");
        // Same auto-accept tolerance confirmed via live runs for TC_FBS_002/013 — this shop's
        // orders can already be auto-accepted by the time the Mirakl sync window elapses.
        Assert.assertTrue(miraklListStatus.equals("Pending acceptance") || miraklListStatus.equals("Awaiting shipment"),
                "Mirakl order status should be 'Pending acceptance' (or already 'Awaiting shipment' if auto-accepted). Actual: " + miraklListStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 37-38: Click into detail → verify total
        LoggerUtility.info("Step 37: Clicking on the Order ID in the search result list");
        miraklOrderDetailPage.clickOrderInList(miraklSearchTerm);
        String miraklTotal = miraklOrderDetailPage.getOrderTotal();
        LoggerUtility.info("Step 38: Mirakl order total: " + miraklTotal + " | FDA order total: " + orderTotal);
        Assert.assertFalse(miraklTotal.isEmpty(), "Mirakl order total should be displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 39-40: Accept order → Awaiting shipment (skip the click if already auto-accepted)
        String miraklStatus;
        if (miraklOrderDetailPage.isAcceptButtonPresent(5)) {
            LoggerUtility.info("Step 39: Clicking Accept button on the Order Details page in Mirakl");
            miraklOrderDetailPage.clickAcceptButton();
            LoggerUtility.info("Refreshing Mirakl order details page after acceptance");
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            miraklStatus = miraklOrderDetailPage.getOrderStatus();
        } else {
            LoggerUtility.info("Step 39: Accept button not present — order was already auto-accepted");
            miraklStatus = miraklOrderDetailPage.getOrderStatus();
        }
        LoggerUtility.info("Step 40: Mirakl Status = " + miraklStatus + " (expected 'Awaiting shipment')");
        Assert.assertEquals(miraklStatus, "Awaiting shipment",
                "Mirakl order status should be 'Awaiting shipment' after acceptance. Actual: " + miraklStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: KIBO API — Authenticate + Verify Delivery Type = FBS
        // ============================================================

        LoggerUtility.info("Step 41: ========== Kibo_Auth ==========");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(), "Kibo authentication failed — access token is empty");

        String externalId = orderId + "WEB";
        LoggerUtility.info("Step 42: ========== Get_all_orders_in_Kibo ==========");
        LoggerUtility.info("Searching externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(), "Kibo Order ID not found for externalId: " + externalId);
        LoggerUtility.info("Kibo Order ID : " + kiboOrderId);

        LoggerUtility.info("Step 43: ========== Get_Shipment_Details ==========");
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
        // Step 44: Verify delivery type is FBS (soft assert — must not stop the run before
        // the Mirakl documents/tracking/shipped/received/return steps complete; failure is
        // reported by softAssert.assertAll() at the end).
        LoggerUtility.info("Step 44: Kibo delivery type = " + deliveryType + " (expected 'FBS')");
        softAssert.assertEquals(deliveryType.toUpperCase(), TC_DELIVERY_TYPE,
                "Kibo shipment delivery type should be 'FBS'. Actual: " + deliveryType);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: MIRAKL — Documents, Tracking, Mark as Shipped
        // ============================================================

        // Steps 45-48: More actions -> Documents -> Add -> upload popup
        LoggerUtility.info("Step 45: Clicking More actions dropdown");
        miraklOrderDetailPage.clickMoreActionsDropdown();
        LoggerUtility.info("Step 46: Clicking Documents option under More actions");
        miraklOrderDetailPage.clickDocumentsOption();
        LoggerUtility.info("Step 47: Clicking the blue Add button in the Order documents section");
        miraklOrderDetailPage.clickAddDocumentButton();
        LoggerUtility.info("Step 48: Verifying Upload an order document popup is displayed");
        Assert.assertTrue(miraklOrderDetailPage.isUploadDocumentPopupDisplayed(),
                "Upload an order document popup should be displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 49-52: Select Invoice type, upload file, confirm
        LoggerUtility.info("Step 49-50: Clicking Document type dropdown and selecting " + TC_DOCUMENT_TYPE);
        miraklOrderDetailPage.selectDocumentType(TC_DOCUMENT_TYPE);
        LoggerUtility.info("Step 51: Selecting the invoice PDF file: " + config.getFbs015InvoiceFilePath());
        miraklOrderDetailPage.uploadDocumentFile(config.getFbs015InvoiceFilePath());
        LoggerUtility.info("Step 52: Clicking Confirm button");
        miraklOrderDetailPage.clickConfirmUploadButton();
        Assert.assertTrue(miraklOrderDetailPage.isDocumentUploadedMessageDisplayed(),
                "'The document has been uploaded.' confirmation message should be displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // (Fix — not a numbered step in the manual test case) Clicking More actions > Documents
        // navigates away from the Order Detail page to a separate "Order documents" page.
        // "Add tracking information" and "Mark as Shipped" only exist on the Order Detail page,
        // so we must navigate back the same way we got here the first time (Steps 32-33, 37)
        // before Step 53 can find anything — same fix as TC_FBS_001/003/013.
        LoggerUtility.info("Navigating back to Mirakl Order Detail page after document upload");
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        miraklOrdersPage.searchOrder(miraklSearchTerm);
        miraklOrderDetailPage.clickOrderInList(miraklSearchTerm);
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 53-58: Add tracking information (DHL + tracking number)
        LoggerUtility.info("Step 53: Clicking Add tracking information link");
        miraklOrderDetailPage.clickAddTrackingInformationLink();
        LoggerUtility.info("Step 54-55: Clicking Select a carrier dropdown and selecting " + TC_CARRIER);
        miraklOrderDetailPage.selectCarrier(TC_CARRIER);
        trackingNumber = generateEightDigitTrackingNumber();
        LoggerUtility.info("Step 56-57: Clicking Tracking number field and entering: " + trackingNumber);
        miraklOrderDetailPage.enterTrackingNumber(trackingNumber);
        LoggerUtility.info("Step 58: Clicking Add button");
        miraklOrderDetailPage.clickAddTrackingButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 59: Verify the selected Carrier Name and Tracking Number are displayed
        LoggerUtility.info("Step 59: Clicking View tracking information link and verifying Carrier/Tracking Number");
        miraklOrderDetailPage.clickViewTrackingInformationLink();
        String displayedCarrier = miraklOrderDetailPage.getTrackingDialogCarrierName();
        Assert.assertTrue(displayedCarrier.toUpperCase().contains(TC_CARRIER),
                "Displayed carrier should be DHL. Actual: " + displayedCarrier);
        String displayedTracking = miraklOrderDetailPage.getTrackingDialogTrackingNumber();
        Assert.assertEquals(displayedTracking, trackingNumber,
                "Displayed tracking number should match entered value. Actual: " + displayedTracking);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        miraklOrderDetailPage.closeTrackingInfoDialog();

        // Step 60: Mark as Shipped
        LoggerUtility.info("Step 60: Clicking Mark as Shipped button");
        miraklOrderDetailPage.clickMarkAsShippedButton();
        String shippedStatus = waitForMiraklStatus("Shipped", 6);
        LoggerUtility.info("Mirakl status after Mark as Shipped: " + shippedStatus);
        Assert.assertEquals(shippedStatus, "Shipped",
                "Mirakl order status should be 'Shipped'. Actual: " + shippedStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 5: MIRAKL — Custom field "Entregado" → Received
        // ============================================================

        LoggerUtility.info("Step 61: Clicking More actions dropdown");
        miraklOrderDetailPage.clickMoreActionsDropdown();
        LoggerUtility.info("Step 62: Clicking Custom field button");
        miraklOrderDetailPage.clickCustomFieldButton();
        LoggerUtility.info("Step 63: Clicking Entregado option");
        miraklOrderDetailPage.clickEntregadoOption();
        miraklOrderDetailPage.clickEntregadoValueDropdown();
        miraklOrderDetailPage.selectEntregadoYes();
        LoggerUtility.info("Step 64: Clicking Confirm button");
        miraklOrderDetailPage.clickCustomFieldConfirmButton();

        // Confirmed necessary by TC_FBS_001/002/013 live runs — the Entregado -> Received
        // transition needs a head start before polling reliably reflects it.
        LoggerUtility.info("Waiting 30s for Entregado update to propagate before polling...");
        Thread.sleep(30_000);
        String finalStatus = waitForMiraklStatus("Received", 20);
        LoggerUtility.info("Step 65: Mirakl Status = " + finalStatus + " (expected 'Received')");
        Assert.assertEquals(finalStatus, "Received",
                "Mirakl order status should change from 'Shipped' to 'Received'. Actual: " + finalStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 6: RETURN — API Flow (Get order_line_id + Create Return Incident)
        // ============================================================

        String orderCommercialId = orderId + "WEB";

        // Step 66: Get Mirakl order_line_id for WEB-A
        LoggerUtility.info("Step 66: Calling Get_order_line_id for commercial order: " + orderCommercialId);
        String orderLineId = ReturnApiUtility.getMiraklOrderLineId(orderCommercialId);
        Assert.assertFalse(orderLineId.isEmpty(),
                "order_line_id should not be empty for commercial order: " + orderCommercialId);
        LoggerUtility.info("order_line_id retrieved: " + orderLineId);

        // Step 67: Create Return incident via API — same retry-on-transient-failure pattern as
        // TC_FBS_013 (Zscaler/SSLHandshakeException guard).
        LoggerUtility.info("Step 67: Calling Return service API — creating return incident");
        LoggerUtility.info("  order_commercial_id : " + orderCommercialId);
        LoggerUtility.info("  order_line_id       : " + orderLineId);
        Response returnResponse = postReturnWithRetry(orderCommercialId, orderLineId, 3);
        Assert.assertTrue(
                returnResponse.getStatusCode() == 200 || returnResponse.getStatusCode() == 201,
                "Return service should return 200 or 201. Actual: " + returnResponse.getStatusCode()
                        + " | Body: " + returnResponse.getBody().asString());
        LoggerUtility.info("Step 67: Return incident created — HTTP " + returnResponse.getStatusCode());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 7: MIRAKL — Return Tracking, Return Label, Mark Received,
        //          Compliance, Full Refund → Closed
        // ============================================================

        // (Fix — not a numbered step in the manual test case) Same propagation wait as
        // TC_FBS_013/014: the order detail page doesn't show the return's own "Add tracking
        // information" prompt until refreshed a few times after the Return API call.
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

        // Steps 68-73: Add tracking information again for the return shipment
        LoggerUtility.info("Step 68: Clicking Add tracking information link (return shipment)");
        miraklOrderDetailPage.clickAddTrackingInformationLink();
        LoggerUtility.info("Step 69-70: Clicking Select a carrier dropdown and selecting " + TC_CARRIER);
        miraklOrderDetailPage.selectCarrier(TC_CARRIER);
        String returnTrackingNumber = generateEightDigitTrackingNumber();
        LoggerUtility.info("Step 71-72: Clicking Tracking number field and entering: " + returnTrackingNumber);
        miraklOrderDetailPage.enterTrackingNumber(returnTrackingNumber);
        LoggerUtility.info("Step 73: Clicking Add button");
        miraklOrderDetailPage.clickAddTrackingButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 74: Kebab button — return-line-specific kebab, not the order-level "More actions"
        // dropdown (see MiraklReturnPage.RETURN_LINE_MORE_ACTIONS_BTN for the confirmed locator).
        LoggerUtility.info("Step 74: Clicking Kebab (More actions) button");
        miraklReturnPage.clickReturnLineMoreActionsButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 75: Add return label
        LoggerUtility.info("Step 75: Clicking Add return label button");
        miraklReturnPage.clickAddReturnLabelOption();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 76: Select file button (intentionally a no-op — see MiraklReturnPage.clickSelectFileButton())
        LoggerUtility.info("Step 76: Clicking Select file button");
        miraklReturnPage.clickSelectFileButton();

        // Step 77: Select the PDF file. Per the same decision already applied to TC_FBS_013/014:
        // the file is considered selected once sendKeys() completes — do not re-verify via
        // isReturnLabelFileAttached()'s WebDriverWait poll, which was the likely trigger for a
        // recurring browser crash during that wait. Proceed straight to Add (Step 78).
        String returnLabelFilePath = config.getFbs015InvoiceFilePath();
        LoggerUtility.info("Step 77: Selecting return label file: " + returnLabelFilePath);
        miraklReturnPage.uploadReturnLabelFile(returnLabelFilePath);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 78: Add button
        LoggerUtility.info("Step 78: Clicking Add button");
        miraklReturnPage.clickAddReturnLabelButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 79-80: Mark as received (order detail page button, then popup confirmation)
        LoggerUtility.info("Step 79: Clicking Mark as received");
        miraklReturnPage.clickMarkAsReceived();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("Step 80: Confirming Mark as received on popup");
        miraklReturnPage.confirmMarkAsReceivedPopup();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 81: Check compliance
        LoggerUtility.info("Step 81: Clicking Check compliance");
        miraklReturnPage.clickCheckCompliance();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 82: Save
        LoggerUtility.info("Step 82: Clicking Save");
        miraklReturnPage.clickSave();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 83: Refund dropdown
        LoggerUtility.info("Step 83: Clicking Refund dropdown");
        miraklReturnPage.clickRefundDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 84: Full refund — this order has only 1 line item and it is being returned in
        // full, so Full refund (not Refund part of the order) is correct here, matching
        // TC_FBO_020/022/TC_FBS_013's pattern rather than TC_FBO_026/TC_FBS_014's partial one.
        LoggerUtility.info("Step 84: Selecting Full refund from dropdown");
        miraklReturnPage.selectFullRefundFromDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 85: Select dropdown for refund reason
        LoggerUtility.info("Step 85: Clicking Select dropdown for refund reason");
        miraklReturnPage.clickSelectReasonDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 86: Item returned
        LoggerUtility.info("Step 86: Selecting Item returned");
        miraklReturnPage.selectItemReturned();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 87: Confirm on refund popup
        LoggerUtility.info("Step 87: Clicking Confirm on refund popup");
        miraklReturnPage.confirmRefundPopup();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 88: Wait 1 minute and refresh the page
        LoggerUtility.info("Step 88: Waiting 60 seconds for shipment closure to process...");
        Thread.sleep(60_000);
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 89: Verify "Shipment 1: closed" — the only line item on this shipment was returned
        // in full, so (unlike TC_FBS_014's partial-return case) the shipment legitimately reaches
        // 'Closed' on its own, same confirmed pattern as TC_FBS_013/TC_FBO_020/TC_FBO_022.
        String closedStatus = waitForMiraklStatus("Closed", 6);
        LoggerUtility.info("Step 89: Shipment status after full refund: " + closedStatus + " (expected 'Closed')");
        Assert.assertEquals(closedStatus, "Closed",
                "Shipment WEB-A should be 'Closed' after full refund. Actual: " + closedStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);

        LoggerUtility.info("TC_FBS_015 execution completed successfully");

        // All 89 steps have run; now surface any soft-assertion failures (e.g. Step 44 delivery
        // type) so the test still fails correctly if something was wrong.
        softAssert.assertAll();
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
    // here from the start since it's now a proven-necessary pattern for the Entregado->Received
    // and refund->Closed transitions in every FBS return-flow test.
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
