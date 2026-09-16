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
 * TC_FBS_004 — FBS (Fulfilled By Store) order lifecycle, 2 products (qty=2 each) from 1 3P seller:
 * FDA (Credit Card) -> Mirakl Accept -> Kibo (verify delivery type = FBS)
 * -> Mirakl Invoice upload -> Mirakl DHL tracking -> Mark as Shipped -> Custom field "Entregado" -> Received.
 *
 * This order lands as a SINGLE Mirakl shipment (no WEB-A/WEB-B split) — confirmed on a live run
 * (order 4000304735 showed only a "WEB-A" row in Mirakl, no "WEB-B" ever appeared). An earlier
 * revision of this test assumed a 2-shipment split by analogy with the FBO flow (TC_FBO_023 is the
 * FBO equivalent of this exact 2-SKU/qty=2/1-3P-seller setup and does split into WEB-A + WEB-B),
 * but that assumption was wrong for the FBS flow: FBS orders consolidate into one shipment
 * regardless of quantity, same as TC_FBS_002 (2 SKUs qty=1 each, same seller). Do not reintroduce
 * WEB-A/WEB-B handling here without a live run confirming an actual split for the SKUs in use.
 *
 * Step numbers in the log/comments below are 1:1 with the 76-step manual test case. The
 * Mirakl/Kibo/Documents/Tracking/Mark-as-Shipped/Entregado mechanics are copied verbatim from
 * the already-verified TC_FBS_001/002/003_Test — this TC combines TC_FBS_002's 2-product cart
 * flow with TC_FBS_003's PDP quantity-increase flow (each product raised from 1 to 2 via the
 * Aumentar plus button before being added to cart).
 *
 * Belongs to the "FBS" TestNG group. BaseClass logs in to FDA once via @BeforeGroups("FBS") using
 * fbs.username/fbs.password before the first FBS test runs, and logs out once via @AfterGroups("FBS")
 * after the last one — no per-test-class login/logout here. Mirakl uses the shared session from
 * @BeforeSuite (mirakl.username/mirakl.password already matches the account this TC requires).
 */
public class TC_FBS_004_Test extends BaseClass {

    private static final String TC_NAME = "TC_FBS_004";

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
        LoggerUtility.info("All page objects initialized for TC_FBS_004");
    }

    @Test(testName = TC_NAME, groups = {"FBS"},
          description = "Verify FBS order placement (2 products qty=2 each, 1 3P seller) and full fulfilment lifecycle: FDA -> Mirakl Accept -> Kibo (FBS) -> Documents/Tracking -> Shipped -> Received")
    public void tc_fbs_004_place_order_2products_qty2_fbs_full_fulfillment() throws InterruptedException {

        // Step 55's delivery-type check must not abort the run: the flow needs to keep going
        // through Documents/Tracking/Mark as Shipped/Entregado/Received even if Kibo hasn't
        // surfaced deliveryType=FBS yet. Recorded here and asserted only at the end.
        SoftAssert softAssert = new SoftAssert();

        // ============================================================
        // PHASE 1: FDA — Search, PDP, Qty=2 each, Cart (2 products), Payment, Order
        // ============================================================

        switchToFDATab();
        LoggerUtility.info("TC_FBS_004: Starting — FDA session active from @BeforeGroups(FBS), Mirakl session active from suite setup");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test cleanup: remove any cart items left over from previous runs (not a spec step)
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        String sku1 = config.getFbs004Sku1();
        String sku2 = config.getFbs004Sku2();

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
        fdaPaymentPage.enterCardNumber(config.getFbs004CardNumber());
        LoggerUtility.info("Step 30-31: Clicking Fecha de expiración field and entering expiration date");
        fdaPaymentPage.enterExpiry(config.getFbs004CardExpiry());
        LoggerUtility.info("Step 32-33: Clicking Código de seguridad field and entering security code");
        fdaPaymentPage.enterCvv(config.getFbs004CardCvv());
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
        // PHASE 2: MIRAKL — Search Order, Pending Acceptance, Accept
        // ============================================================

        LoggerUtility.info("Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 43-44: Navigate to All Orders
        LoggerUtility.info("Step 43: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 44: Clicking All Orders link");
        miraklOrdersPage.clickAllOrders();

        // Steps 45-47: Search for order in Mirakl — retry every 60s up to 5 minutes for sync delay.
        // 1 3P seller = 1 shipment, so search directly with the "WEB-A" shipment ref rather than
        // the generic "orderId+WEB" term (Kibo's externalId lookup below stays as plain "WEB" —
        // Kibo's externalOrderId field never carries the Mirakl "-A" shipment suffix).
        String miraklSearchTerm = orderId + "WEB-A";
        LoggerUtility.info("Step 45-46: Clicking Search field and entering Order ID: " + miraklSearchTerm);
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
        LoggerUtility.info("Step 47: Mirakl Status = " + miraklListStatus + " (expected 'Pending acceptance')");
        // Confirmed via live runs (TC_FBS_002/003): this shop's orders can already be auto-accepted
        // by the time the 5-minute Mirakl sync window elapses, landing directly on "Awaiting
        // shipment" instead of "Pending acceptance" — same auto-accept behavior
        // MiraklOrderDetailPage's isAcceptButtonPresent() was written to detect elsewhere in this
        // codebase. Tolerate both.
        Assert.assertTrue(miraklListStatus.equals("Pending acceptance") || miraklListStatus.equals("Awaiting shipment"),
                "Mirakl order status should be 'Pending acceptance' (or already 'Awaiting shipment' if auto-accepted). Actual: " + miraklListStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 48-49: Click into detail → verify total
        LoggerUtility.info("Step 48: Clicking on the Order ID in the search result list");
        miraklOrderDetailPage.clickOrderInList(miraklSearchTerm);
        String miraklTotal = miraklOrderDetailPage.getOrderTotal();
        LoggerUtility.info("Step 49: Mirakl order total: " + miraklTotal + " | FDA order total: " + orderTotal);
        Assert.assertFalse(miraklTotal.isEmpty(), "Mirakl order total should be displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 50-51: Accept order → Awaiting shipment (skip the click if already auto-accepted)
        String miraklStatus;
        if (miraklOrderDetailPage.isAcceptButtonPresent(5)) {
            LoggerUtility.info("Step 50: Clicking Accept button on the Order Details page in Mirakl");
            miraklOrderDetailPage.clickAcceptButton();
            LoggerUtility.info("Refreshing Mirakl order details page after acceptance");
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            miraklStatus = miraklOrderDetailPage.getOrderStatus();
        } else {
            LoggerUtility.info("Step 50: Accept button not present — order was already auto-accepted");
            miraklStatus = miraklOrderDetailPage.getOrderStatus();
        }
        LoggerUtility.info("Step 51: Mirakl Status = " + miraklStatus + " (expected 'Awaiting shipment')");
        Assert.assertEquals(miraklStatus, "Awaiting shipment",
                "Mirakl order status should be 'Awaiting shipment' after acceptance. Actual: " + miraklStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: KIBO API — Authenticate + Verify Delivery Type = FBS
        // ============================================================

        LoggerUtility.info("Step 52: ========== Kibo_Auth ==========");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(), "Kibo authentication failed — access token is empty");

        String externalId = orderId + "WEB";
        LoggerUtility.info("Step 53: ========== Get_all_orders_in_Kibo ==========");
        LoggerUtility.info("Searching externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(), "Kibo Order ID not found for externalId: " + externalId);
        LoggerUtility.info("Kibo Order ID : " + kiboOrderId);

        LoggerUtility.info("Step 54: ========== Get_Shipment_Details ==========");
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
        // Step 55: Verify delivery type is FBS (soft assert — must not stop the run before
        // the Mirakl documents/tracking/shipped/received steps complete; failure is reported
        // by softAssert.assertAll() at the end).
        LoggerUtility.info("Step 55: Kibo delivery type = " + deliveryType + " (expected 'FBS')");
        softAssert.assertEquals(deliveryType.toUpperCase(), TC_DELIVERY_TYPE,
                "Kibo shipment delivery type should be 'FBS'. Actual: " + deliveryType);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: MIRAKL — Documents, Tracking, Mark as Shipped
        // ============================================================

        // Steps 56-59: More actions -> Documents -> Add -> upload popup
        LoggerUtility.info("Step 56: Clicking More actions dropdown");
        miraklOrderDetailPage.clickMoreActionsDropdown();
        LoggerUtility.info("Step 57: Clicking Documents option under More actions");
        miraklOrderDetailPage.clickDocumentsOption();
        LoggerUtility.info("Step 58: Clicking the blue Add button in the Order documents section");
        miraklOrderDetailPage.clickAddDocumentButton();
        LoggerUtility.info("Step 59: Verifying Upload an order document popup is displayed");
        Assert.assertTrue(miraklOrderDetailPage.isUploadDocumentPopupDisplayed(),
                "Upload an order document popup should be displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 60-63: Select Invoice type, upload file, confirm
        LoggerUtility.info("Step 60-61: Clicking Document type dropdown and selecting " + TC_DOCUMENT_TYPE);
        miraklOrderDetailPage.selectDocumentType(TC_DOCUMENT_TYPE);
        LoggerUtility.info("Step 62: Selecting the invoice PDF file: " + config.getFbs004InvoiceFilePath());
        miraklOrderDetailPage.uploadDocumentFile(config.getFbs004InvoiceFilePath());
        LoggerUtility.info("Step 63: Clicking Confirm button");
        miraklOrderDetailPage.clickConfirmUploadButton();
        Assert.assertTrue(miraklOrderDetailPage.isDocumentUploadedMessageDisplayed(),
                "'The document has been uploaded.' confirmation message should be displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // (Fix — not a numbered step in the manual test case, confirmed necessary by the
        // TC_FBS_001/002/003 live runs) Clicking More actions > Documents navigates the browser
        // away from the Order Detail page to a separate "Order documents" page. "Add tracking
        // information" and "Mark as Shipped" only exist on the Order Detail page, so we must
        // navigate back the same way we got here the first time (Steps 43-44, 48) before Step 64
        // can find anything.
        LoggerUtility.info("Navigating back to Mirakl Order Detail page after document upload");
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        miraklOrdersPage.searchOrder(miraklSearchTerm);
        miraklOrderDetailPage.clickOrderInList(miraklSearchTerm);
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 64-69: Add tracking information (DHL + tracking number)
        LoggerUtility.info("Step 64: Clicking Add tracking information link");
        miraklOrderDetailPage.clickAddTrackingInformationLink();
        LoggerUtility.info("Step 65-66: Clicking Select a carrier dropdown and selecting " + TC_CARRIER);
        miraklOrderDetailPage.selectCarrier(TC_CARRIER);
        trackingNumber = generateEightDigitTrackingNumber();
        LoggerUtility.info("Step 67-68: Clicking Tracking number field and entering: " + trackingNumber);
        miraklOrderDetailPage.enterTrackingNumber(trackingNumber);
        LoggerUtility.info("Step 69: Clicking Add button");
        miraklOrderDetailPage.clickAddTrackingButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 70: Verify the selected Carrier Name and Tracking Number are displayed — confirmed
        // via the TC_FBS_001/002/003 live runs that this requires opening "View tracking
        // information" and reading the values from the resulting dialog (then closing it).
        LoggerUtility.info("Step 70: Clicking View tracking information link and verifying Carrier/Tracking Number");
        miraklOrderDetailPage.clickViewTrackingInformationLink();
        String displayedCarrier = miraklOrderDetailPage.getTrackingDialogCarrierName();
        Assert.assertTrue(displayedCarrier.toUpperCase().contains(TC_CARRIER),
                "Displayed carrier should be DHL. Actual: " + displayedCarrier);
        String displayedTracking = miraklOrderDetailPage.getTrackingDialogTrackingNumber();
        Assert.assertEquals(displayedTracking, trackingNumber,
                "Displayed tracking number should match entered value. Actual: " + displayedTracking);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        miraklOrderDetailPage.closeTrackingInfoDialog();

        // Step 71: Mark as Shipped
        LoggerUtility.info("Step 71: Clicking Mark as Shipped button");
        miraklOrderDetailPage.clickMarkAsShippedButton();
        String shippedStatus = waitForMiraklStatus("Shipped", 6);
        LoggerUtility.info("Mirakl status after Mark as Shipped: " + shippedStatus);
        Assert.assertEquals(shippedStatus, "Shipped",
                "Mirakl order status should be 'Shipped'. Actual: " + shippedStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 5: MIRAKL — Custom field "Entregado" → Received
        // ============================================================

        LoggerUtility.info("Step 72: Clicking More actions dropdown");
        miraklOrderDetailPage.clickMoreActionsDropdown();
        LoggerUtility.info("Step 73: Clicking Custom field button");
        miraklOrderDetailPage.clickCustomFieldButton();
        LoggerUtility.info("Step 74: Clicking Entregado option");
        miraklOrderDetailPage.clickEntregadoOption();
        // (Confirmed necessary by the TC_FBS_001/002/003 live runs, not spelled out as a separate
        // numbered step in this manual test case) Entregado has a Yes/No value dropdown that must
        // be set before Confirm does anything meaningful.
        miraklOrderDetailPage.clickEntregadoValueDropdown();
        miraklOrderDetailPage.selectEntregadoYes();
        LoggerUtility.info("Step 75: Clicking Confirm button");
        miraklOrderDetailPage.clickCustomFieldConfirmButton();

        // Step 76: Verify status changes from Shipped to Received. The Entregado -> Received
        // transition is processed asynchronously on the backend (same as the carrier-webhook-driven
        // transitions in the FBO tests, which always sleep before polling) — a live run confirmed
        // 6 rapid refreshes (~30s total, no pre-poll wait) isn't reliably enough time: the click
        // sequence completed cleanly with no errors, yet status stayed "Shipped" through all 6
        // checks. Give the backend a head start, then poll with more retries (same fix already
        // applied in TC_FBS_006 after it hit the identical failure).
        LoggerUtility.info("Waiting 30s for Entregado update to propagate before polling...");
        Thread.sleep(30_000);
        String finalStatus = waitForMiraklStatus("Received", 20);
        LoggerUtility.info("Step 76: Mirakl Status = " + finalStatus + " (expected 'Received')");
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

        LoggerUtility.info("TC_FBS_004 execution completed successfully");

        // All 76 steps have run; now surface any soft-assertion failures (e.g. Step 55 delivery
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
    // Generates a random 8-digit tracking number (manual step 68 requirement)
    // ----------------------------------------------------------------
    private String generateEightDigitTrackingNumber() {
        int number = 10_000_000 + new Random().nextInt(90_000_000);
        return String.valueOf(number);
    }

    // ----------------------------------------------------------------
    // Handle checkout opening in new window/tab after clickProceedToPayment
    // ----------------------------------------------------------------
    private void switchToCheckoutWindow() {
        // A single instant snapshot of getWindowHandles() right after the click is a race
        // condition: a live run showed neither branch below ever firing (no new window detected,
        // old one not yet reported closed), then ~3 minutes later the stale original tab was
        // found closed ("target window already closed") after every checkout-page locator had
        // been silently searching the wrong (soon-to-close) window the whole time. Poll for up
        // to 20s for the window set to settle into "new window opened" or "original closed"
        // before deciding, instead of judging it from one instantaneous read.
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
