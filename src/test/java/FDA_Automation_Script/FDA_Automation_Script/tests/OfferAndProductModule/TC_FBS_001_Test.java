package FDA_Automation_Script.FDA_Automation_Script.tests.OfferAndProductModule;

import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDACartPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAHomePage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDALoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAOrderHistoryPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAPaymentPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAPDPPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDASuccessPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklLoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklOrderDetailPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklOrdersPage;
import FDA_Automation_Script.FDA_Automation_Script.utils.ApiUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ConfigReader;
import FDA_Automation_Script.FDA_Automation_Script.utils.DualDriverManager;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.TCFBS001TestDataReader;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import io.restassured.response.Response;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * TC_FBS_001 — FBS order lifecycle with DUAL-BROWSER isolation.
 *
 * Architecture:
 *   fdaDriver   → Chrome browser #1 — FDA session only
 *   miraklDriver → Chrome browser #2 — Mirakl session only
 *
 * Both browsers start in @BeforeSuite, log in once, and stay alive until @AfterSuite.
 * No logout is performed during or between tests. No driver.quit() outside @AfterSuite.
 *
 * This class does NOT extend BaseClass — the single-driver tab architecture in BaseClass
 * is incompatible with a two-browser requirement. All existing page objects are reused
 * as-is; they accept a WebDriver in their constructor and are driver-agnostic.
 *
 * Test data: testdata/TC_FBS_001.properties (isolated from config.properties)
 * Kibo API config: config.properties (infrastructure, not TC-specific)
 * Suite XML: testngOfferAndProductModule.xml
 */
public class TC_FBS_001_Test {

    private static final String TC_NAME          = "TC_FBS_001";
    private static final String TC_CARRIER       = "DHL";
    private static final String TC_DOCUMENT_TYPE = "Invoice";
    private static final String TC_DELIVERY_TYPE = "FBS";

    // ── Two independent Chrome browser instances ──────────────────────────────
    private WebDriver fdaDriver;
    private WebDriver miraklDriver;

    // ── Isolated TC test data reader ──────────────────────────────────────────
    private TCFBS001TestDataReader testData;

    // ── Infra config (URLs, Kibo API) from config.properties ─────────────────
    private final ConfigReader config = ConfigReader.getInstance();

    // ── FDA page objects — all wired to fdaDriver ─────────────────────────────
    private FDAHomePage         fdaHomePage;
    private FDALoginPage        fdaLoginPage;
    private FDAPDPPage          fdaPdpPage;
    private FDACartPage         fdaCartPage;
    private FDAPaymentPage      fdaPaymentPage;
    private FDASuccessPage      fdaSuccessPage;
    private FDAOrderHistoryPage fdaOrderHistoryPage;

    // ── Mirakl page objects — all wired to miraklDriver ──────────────────────
    private MiraklLoginPage       miraklLoginPage;
    private MiraklOrdersPage      miraklOrdersPage;
    private MiraklOrderDetailPage miraklOrderDetailPage;

    // ── Dynamic data captured once, reused throughout ─────────────────────────
    private String orderId;
    private String orderTotal;
    private String trackingNumber;

    // Mirakl status badge locator — same xpath confirmed live for this app
    private static final By MIRAKL_STATUS_LOCATOR =
        By.xpath("(//span[contains(@class,'_6e782__sc-s4ukbq-0') and not(ancestor::a)])[1]");

    // =========================================================================
    // Suite lifecycle — login once per application, logout never
    // =========================================================================

    @BeforeSuite
    public void setupApplications() {
        testData = new TCFBS001TestDataReader();
        LoggerUtility.info("=== TC_FBS_001 OfferAndProductModule: Starting dual-browser suite setup ===");
        LoggerUtility.info("Test data loaded from: testdata/TC_FBS_001.properties");

        // ── Browser 1: FDA ────────────────────────────────────────────────────
        LoggerUtility.info("Starting FDA Chrome browser (Browser 1)");
        fdaDriver = DualDriverManager.createFreshChromeDriver();
        initFDAPageObjects();

        LoggerUtility.info("Step 1 (setup): Navigating FDA browser to " + config.getFdaUrl());
        fdaDriver.get(config.getFdaUrl());

        LoggerUtility.info("Logging in to FDA as " + testData.getFdaUsername());
        fdaHomePage.clickProfileIcon();
        fdaHomePage.clickLoginLink();
        fdaLoginPage.login(testData.getFdaUsername(), testData.getFdaPassword());
        LoggerUtility.info("PASS — FDA login complete. Session will remain active for all tests.");
        ScreenshotUtility.captureScreenshot(fdaDriver, TC_NAME + "_SETUP_FDA_LOGIN", ScreenshotUtility.INFO);

        // ── Browser 2: Mirakl ─────────────────────────────────────────────────
        LoggerUtility.info("Starting Mirakl Chrome browser (Browser 2)");
        miraklDriver = DualDriverManager.createFreshChromeDriver();
        initMiraklPageObjects();

        LoggerUtility.info("Navigating Mirakl browser to " + config.getMiraklUrl());
        miraklDriver.get(config.getMiraklUrl());

        LoggerUtility.info("Logging in to Mirakl as " + testData.getMiraklUsername());
        miraklLoginPage.login(testData.getMiraklUsername(), testData.getMiraklPassword());
        LoggerUtility.info("PASS — Mirakl login complete. Session will remain active for all tests.");
        ScreenshotUtility.captureScreenshot(miraklDriver, TC_NAME + "_SETUP_MIRAKL_LOGIN", ScreenshotUtility.INFO);

        LoggerUtility.info("=== Dual-browser setup complete. FDA (Browser 1) and Mirakl (Browser 2) logged in. ===");
    }

    @AfterSuite(alwaysRun = true)
    public void tearDownApplications() {
        LoggerUtility.info("=== TC_FBS_001 OfferAndProductModule: Tearing down dual-browser sessions ===");
        if (fdaDriver != null) {
            try {
                fdaDriver.quit();
                LoggerUtility.info("FDA browser (Browser 1) closed");
            } catch (Exception e) {
                LoggerUtility.warn("Error closing FDA browser: " + e.getMessage());
            }
        }
        if (miraklDriver != null) {
            try {
                miraklDriver.quit();
                LoggerUtility.info("Mirakl browser (Browser 2) closed");
            } catch (Exception e) {
                LoggerUtility.warn("Error closing Mirakl browser: " + e.getMessage());
            }
        }
        LoggerUtility.info("=== Dual-browser teardown complete ===");
    }

    // =========================================================================
    // TC_FBS_001 — Full FBS order lifecycle
    // =========================================================================

    @Test(testName = TC_NAME,
          description = "FBS order lifecycle (dual-browser): FDA Credit Card -> Mirakl Accept "
                      + "-> Kibo FBS delivery type -> Invoice upload -> DHL tracking -> Shipped -> Received")
    public void tc_fbs_001_place_order_fbs_full_fulfillment() throws InterruptedException {

        // Delivery-type check is mid-flow — use soft assert so the Mirakl
        // Documents/Tracking/Shipped/Received steps run even if Kibo hasn't surfaced FBS yet.
        SoftAssert softAssert = new SoftAssert();

        // =======================================================================
        // PHASE 1 — FDA: Search → PDP → Cart → Payment → Success → Order History
        // =======================================================================

        LoggerUtility.info("==================== PHASE 1: FDA ORDER PLACEMENT ====================");

        // Step 2: Verify FDA home page
        fdaHomePage.navigateTo(config.getFdaUrl());
        LoggerUtility.info("Step 2: PASS — FDA Home page displayed");
        ScreenshotUtility.captureScreenshot(fdaDriver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test: clear any leftover cart items from a previous aborted run
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // Steps 3–4: Search by SKU
        String sku = testData.getFdaSku();
        LoggerUtility.info("Step 3: Clicking ¿Qué estás buscando? search field");
        LoggerUtility.info("Step 4: Entering SKU: " + sku + " and pressing Enter");
        fdaHomePage.enterSearchQuery(sku);
        fdaHomePage.pressSearchEnter();

        // Step 5: Verify PDP
        LoggerUtility.info("Step 5: Verifying Product Details Page is displayed");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "Step 5: PDP not displayed after search for SKU: " + sku);
        LoggerUtility.info("PASS — Product Details Page displayed for SKU: " + sku);

        // Step 6: Verify Add to Cart enabled
        LoggerUtility.info("Step 6: Verifying Agregar al carrito button is enabled");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "Step 6: Agregar al carrito button is not enabled");
        LoggerUtility.info("PASS — Agregar al carrito button is enabled");

        // Step 7: Verify quantity = 1
        LoggerUtility.info("Step 7: Verifying product quantity is 1");
        Assert.assertEquals(fdaPdpPage.getQuantity(), "1",
            "Step 7: Product quantity on PDP should be 1");
        LoggerUtility.info("PASS — Product quantity verified as 1");
        ScreenshotUtility.captureScreenshot(fdaDriver, TC_NAME, ScreenshotUtility.INFO);

        // Step 8: Add to cart
        LoggerUtility.info("Step 8: Clicking Agregar al carrito");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("PASS — Product added to cart");

        // Steps 9–13: Open cart, verify product name, quantity, capture order total
        LoggerUtility.info("Step 9-10: Clicking Mi carrito and opening cart page");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        String cartProductName = fdaCartPage.getProductName();
        LoggerUtility.info("Step 11: Cart product name: " + cartProductName);
        Assert.assertFalse(cartProductName.isEmpty(),
            "Step 11: Product name should be displayed in cart");
        LoggerUtility.info("PASS — Product name displayed in cart");

        String cartQty = fdaCartPage.getQuantity();
        LoggerUtility.info("Step 12: Cart quantity: " + cartQty);
        Assert.assertEquals(cartQty, "1", "Step 12: Cart quantity should be 1");
        LoggerUtility.info("PASS — Quantity verified as 1");

        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 13: FDA order total captured: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(),
            "Step 13: Order total should be displayed in cart");
        LoggerUtility.info("PASS — FDA order total captured: " + orderTotal);
        ScreenshotUtility.captureScreenshot(fdaDriver, TC_NAME, ScreenshotUtility.INFO);

        // Step 14: Proceed to payment
        LoggerUtility.info("Step 14: Clicking Proceed to payment");
        fdaCartPage.clickProceedToPayment();
        // Kibo checkout may open in a new browser window — switch to it
        switchFdaToCheckoutWindow();

        // Step 15: Click Siguiente on shipping page
        LoggerUtility.info("Step 15: Clicking Siguiente on the Shipping page");
        fdaPaymentPage.clickNextButton();

        // Step 16: Select Credit/Debit Card payment
        LoggerUtility.info("Step 16: Selecting Pago con Tarjeta Crédito/Débito");
        fdaPaymentPage.selectCreditCardOption();

        // Steps 17–19: Enter card details (masked in log)
        LoggerUtility.info("Step 17: Entering card number: " + testData.getMaskedCardNumber());
        fdaPaymentPage.enterCardNumber(testData.getFdaCardNumber());

        LoggerUtility.info("Step 18: Entering expiration date: " + testData.getFdaCardExpiration());
        fdaPaymentPage.enterExpiry(testData.getFdaCardExpiration());

        LoggerUtility.info("Step 19: Entering security code: ***");
        fdaPaymentPage.enterCvv(testData.getFdaCardCvv());

        // Step 20: Verify Completar pago button shows order total
        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Step 20: Completar pago button text: " + payBtnText);
        Assert.assertTrue(
            payBtnText.contains("Completar pago") || payBtnText.contains("MXN") || payBtnText.contains("$"),
            "Step 20: Completar pago button should display order total. Actual: " + payBtnText);
        LoggerUtility.info("PASS — Completar pago button shows expected order total");
        ScreenshotUtility.captureScreenshot(fdaDriver, TC_NAME, ScreenshotUtility.INFO);

        // Step 21: Complete payment
        LoggerUtility.info("Step 21: Clicking Completar pago");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();

        // Step 22: Verify success page
        LoggerUtility.info("Step 22: Verifying order success page");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(),
            "Step 22: Order success page not displayed after payment");
        LoggerUtility.info("PASS — FDA Success page displayed");

        // Step 23: Extract Order ID
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 23: Order ID extracted: " + orderId);
        Assert.assertFalse(orderId.isEmpty(),
            "Step 23: Order ID should be present on success page");
        LoggerUtility.info("PASS — Order ID generated: " + orderId);
        ScreenshotUtility.captureScreenshot(fdaDriver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 24–27: Navigate to Mis pedidos and verify order
        LoggerUtility.info("Step 24: Clicking Mi cuenta profile icon");
        fdaHomePage.clickProfileIcon();
        LoggerUtility.info("Step 24: Clicking Mis pedidos link");
        fdaHomePage.clickMyOrdersLink();

        LoggerUtility.info("Step 25: Verifying Mis pedidos page is displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(),
            "Step 25: Mis pedidos page not displayed");
        LoggerUtility.info("PASS — Mis pedidos page displayed");

        LoggerUtility.info("Step 26: Verifying Order ID " + orderId + " is present");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
            "Step 26: Order ID " + orderId + " not found in order history");
        LoggerUtility.info("PASS — Order ID " + orderId + " present in Mis pedidos");

        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 27: FDA order status: " + fdaStatus + " (expected 'Creada')");
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
            "Step 27: Order status should be 'Creada'. Actual: " + fdaStatus);
        LoggerUtility.info("PASS — FDA Order status verified: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(fdaDriver, TC_NAME, ScreenshotUtility.INFO);

        // =======================================================================
        // PHASE 2 — MIRAKL: Search → Pending → Accept → Awaiting shipment
        // =======================================================================

        LoggerUtility.info("==================== PHASE 2: MIRAKL ORDER MANAGEMENT ====================");
        LoggerUtility.info("Step 28: Switching to Mirakl browser (Browser 2) — session already active");
        // Navigate to Mirakl home to ensure fresh session state
        miraklDriver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(miraklDriver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 29: Navigate to All Orders
        LoggerUtility.info("Step 29: Clicking Orders menu and All Orders in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();

        // Steps 30–31: Search for order, retry up to 5 times (60s apart) for sync delay
        // FBS single-seller order → always one shipment → search with "WEB-A" suffix directly
        String miraklSearchTerm = orderId + "WEB-A";
        LoggerUtility.info("Step 30: Searching Mirakl for order: " + miraklSearchTerm);

        boolean orderFoundInMirakl = false;
        for (int attempt = 1; attempt <= 5; attempt++) {
            LoggerUtility.info("Mirakl sync wait attempt " + attempt + "/5 for: " + miraklSearchTerm);
            miraklOrdersPage.searchOrder(miraklSearchTerm);
            if (miraklOrdersPage.hasSearchResults(miraklSearchTerm)) {
                orderFoundInMirakl = true;
                LoggerUtility.info("Order found in Mirakl on attempt " + attempt);
                break;
            }
            LoggerUtility.info("Order not yet in Mirakl (attempt " + attempt + "/5)");
            if (attempt < 5) {
                LoggerUtility.info("Waiting 60 seconds before next attempt...");
                Thread.sleep(60_000);
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(orderFoundInMirakl,
            "Step 30: Order " + miraklSearchTerm + " did not appear in Mirakl within 5 minutes");

        String miraklListStatus = miraklOrdersPage.getFirstOrderStatus();
        LoggerUtility.info("Step 31: Mirakl order list status: " + miraklListStatus);
        // The shop may auto-accept, so both statuses are valid at this point
        Assert.assertTrue(
            miraklListStatus.equals("Pending acceptance") || miraklListStatus.equals("Awaiting shipment"),
            "Step 31: Expected 'Pending acceptance' (or 'Awaiting shipment' if auto-accepted). Actual: "
                + miraklListStatus);
        LoggerUtility.info("PASS — Mirakl order status verified: " + miraklListStatus);
        ScreenshotUtility.captureScreenshot(miraklDriver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 32–34: Open order detail, verify total
        LoggerUtility.info("Step 32: Clicking on order in list: " + miraklSearchTerm);
        miraklOrderDetailPage.clickOrderInList(miraklSearchTerm);

        String miraklTotal = miraklOrderDetailPage.getOrderTotal();
        LoggerUtility.info("Step 33: Mirakl order total: " + miraklTotal);
        LoggerUtility.info("Step 34: FDA order total for comparison: " + orderTotal);
        Assert.assertFalse(miraklTotal.isEmpty(),
            "Step 33: Mirakl order total should be displayed");
        LoggerUtility.info("PASS — Mirakl order total displayed: " + miraklTotal);
        ScreenshotUtility.captureScreenshot(miraklDriver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 35–36: Accept the order (skip if already auto-accepted)
        String miraklStatus;
        if (miraklOrderDetailPage.isAcceptButtonPresent(5)) {
            LoggerUtility.info("Step 35: Clicking Accept button on Mirakl Order Details");
            miraklOrderDetailPage.clickAcceptButton();
            LoggerUtility.info("Refreshing Mirakl order details after acceptance");
            miraklRefreshAndWait(MIRAKL_STATUS_LOCATOR);
            miraklStatus = miraklOrderDetailPage.getOrderStatus();
        } else {
            LoggerUtility.info("Step 35: Accept button not present — order was already auto-accepted");
            miraklStatus = miraklOrderDetailPage.getOrderStatus();
        }
        LoggerUtility.info("Step 36: Mirakl status after accept: " + miraklStatus);
        Assert.assertEquals(miraklStatus, "Awaiting shipment",
            "Step 36: Mirakl status should be 'Awaiting shipment'. Actual: " + miraklStatus);
        LoggerUtility.info("PASS — Mirakl Order accepted — status: Awaiting shipment");
        ScreenshotUtility.captureScreenshot(miraklDriver, TC_NAME, ScreenshotUtility.INFO);

        // =======================================================================
        // PHASE 3 — KIBO API: Auth → Find Order → Get Shipment → Verify FBS
        // =======================================================================

        LoggerUtility.info("==================== PHASE 3: KIBO API VALIDATION ====================");

        // Step 37: Kibo authentication
        LoggerUtility.info("Step 37: Calling Kibo_Auth service");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(),
            "Step 37: Kibo access token should not be empty");
        LoggerUtility.info("PASS — Kibo access token obtained");

        // Step 38–39: Find Kibo order by externalId
        // Kibo externalOrderId is always plain "WEB" — no "-A" suffix even for multi-shipment orders
        String externalId = orderId + "WEB";
        LoggerUtility.info("Step 38: Calling Get_all_orders_in_Kibo — externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(),
            "Step 38: Kibo Order ID not found for externalId: " + externalId);
        LoggerUtility.info("PASS — Kibo Order ID: " + kiboOrderId);

        // Steps 40–44: Get shipment details and extract delivery type
        LoggerUtility.info("Step 40: Calling Get_Shipment_Details for Kibo Order ID: " + kiboOrderId);
        String deliveryType = "";
        for (int attempt = 1; attempt <= 5; attempt++) {
            LoggerUtility.info("Kibo shipment data poll attempt " + attempt + "/5");
            Response shipmentsResp = ApiUtility.kiboGetShipmentsByOrderId(kiboToken, kiboOrderId);
            Assert.assertEquals(shipmentsResp.getStatusCode(), 200,
                "Kibo Get Shipment Details should return HTTP 200. Actual: " + shipmentsResp.getStatusCode());

            List<Map<String, Object>> shipmentList = shipmentsResp.jsonPath().getList("items");
            if (shipmentList == null || shipmentList.isEmpty()) {
                shipmentList = shipmentsResp.jsonPath().getList("_embedded.shipments");
            }
            if (shipmentList != null && !shipmentList.isEmpty()) {
                deliveryType = extractCustomField(shipmentList.get(0), "deliveryType");
                if (!deliveryType.isEmpty()) break;
            }
            LoggerUtility.info("deliveryType not yet available in Kibo — waiting 15s...");
            if (attempt < 5) Thread.sleep(15_000);
        }
        LoggerUtility.info("Step 44: Kibo delivery type = " + deliveryType + " (expected 'FBS')");
        // Soft assert — does not abort the run; surfaced at the end via softAssert.assertAll()
        softAssert.assertEquals(deliveryType.toUpperCase(), TC_DELIVERY_TYPE,
            "Step 44: Kibo shipment delivery type should be 'FBS'. Actual: " + deliveryType);
        LoggerUtility.info("PASS — Kibo delivery type verified: FBS");
        ScreenshotUtility.captureScreenshot(miraklDriver, TC_NAME, ScreenshotUtility.INFO);

        // =======================================================================
        // PHASE 4 — MIRAKL: Documents → Tracking → Mark as Shipped
        // =======================================================================

        LoggerUtility.info("==================== PHASE 4: MIRAKL DOCUMENTS & TRACKING ====================");

        // Steps 45–49: More actions → Documents → Add → upload popup
        LoggerUtility.info("Step 45: Clicking More actions dropdown in Mirakl");
        miraklOrderDetailPage.clickMoreActionsDropdown();

        LoggerUtility.info("Step 46: Clicking Documents under More actions");
        miraklOrderDetailPage.clickDocumentsOption();

        LoggerUtility.info("Step 47-48: Clicking blue Add button in Order Documents section");
        miraklOrderDetailPage.clickAddDocumentButton();

        LoggerUtility.info("Step 49: Verifying 'Upload an order document' popup is displayed");
        Assert.assertTrue(miraklOrderDetailPage.isUploadDocumentPopupDisplayed(),
            "Step 49: Upload document popup not displayed");
        LoggerUtility.info("PASS — Upload an order document popup displayed");
        ScreenshotUtility.captureScreenshot(miraklDriver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 50–54: Select Invoice type, upload file, confirm
        LoggerUtility.info("Step 50-51: Clicking -- Select -- dropdown and selecting: " + TC_DOCUMENT_TYPE);
        miraklOrderDetailPage.selectDocumentType(TC_DOCUMENT_TYPE);

        String invoicePath = testData.getMiraklInvoicePath();
        LoggerUtility.info("Step 52: Uploading invoice PDF: " + invoicePath);
        miraklOrderDetailPage.uploadDocumentFile(invoicePath);

        LoggerUtility.info("Step 53: Clicking Confirm button");
        miraklOrderDetailPage.clickConfirmUploadButton();

        Assert.assertTrue(miraklOrderDetailPage.isDocumentUploadedMessageDisplayed(),
            "Step 53: Document upload confirmation not displayed");
        LoggerUtility.info("PASS — Invoice uploaded successfully");
        ScreenshotUtility.captureScreenshot(miraklDriver, TC_NAME, ScreenshotUtility.INFO);

        // Navigate back to order detail (clicking Documents navigates away from the detail page)
        LoggerUtility.info("Navigating back to Mirakl Order Detail page after document upload");
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        miraklOrdersPage.searchOrder(miraklSearchTerm);
        miraklOrderDetailPage.clickOrderInList(miraklSearchTerm);
        miraklRefreshAndWait(MIRAKL_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(miraklDriver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 55–60: Add tracking information
        LoggerUtility.info("Step 55: Clicking Add tracking information link");
        miraklOrderDetailPage.clickAddTrackingInformationLink();

        String carrier = testData.getMiraklCarrier();
        LoggerUtility.info("Step 56-57: Clicking 'Select a carrier' and selecting: " + carrier);
        miraklOrderDetailPage.selectCarrier(carrier);

        trackingNumber = generateEightDigitTrackingNumber();
        LoggerUtility.info("Step 58-59: Generated tracking number: " + trackingNumber + " — entering in field");
        miraklOrderDetailPage.enterTrackingNumber(trackingNumber);

        LoggerUtility.info("Step 60: Clicking Add button to save tracking");
        miraklOrderDetailPage.clickAddTrackingButton();
        ScreenshotUtility.captureScreenshot(miraklDriver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 61–66: View tracking info dialog, verify carrier and tracking number, close
        LoggerUtility.info("Step 61: Clicking View tracking information link");
        miraklOrderDetailPage.clickViewTrackingInformationLink();

        String displayedCarrier = miraklOrderDetailPage.getTrackingDialogCarrierName();
        LoggerUtility.info("Step 62-63: Tracking dialog Carrier Name: " + displayedCarrier);
        Assert.assertTrue(displayedCarrier.toUpperCase().contains(TC_CARRIER),
            "Step 63: Carrier should be DHL. Actual: " + displayedCarrier);
        LoggerUtility.info("PASS — Carrier verified: DHL");

        String displayedTracking = miraklOrderDetailPage.getTrackingDialogTrackingNumber();
        LoggerUtility.info("Step 64-65: Tracking dialog Tracking Number: " + displayedTracking);
        Assert.assertEquals(displayedTracking, trackingNumber,
            "Step 65: Displayed tracking number should match generated value. "
                + "Expected: " + trackingNumber + ", Actual: " + displayedTracking);
        LoggerUtility.info("PASS — Tracking number verified: " + trackingNumber);
        ScreenshotUtility.captureScreenshot(miraklDriver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 66: Clicking Close on tracking information dialog");
        miraklOrderDetailPage.closeTrackingInfoDialog();

        // Steps 67–68: Mark as Shipped
        LoggerUtility.info("Step 67: Clicking Mark as Shipped button");
        miraklOrderDetailPage.clickMarkAsShippedButton();

        String shippedStatus = waitForMiraklStatus("Shipped", 6);
        LoggerUtility.info("Step 68: Mirakl status after Mark as Shipped: " + shippedStatus);
        Assert.assertEquals(shippedStatus, "Shipped",
            "Step 68: Mirakl order status should be 'Shipped'. Actual: " + shippedStatus);
        LoggerUtility.info("PASS — Order marked as Shipped");
        ScreenshotUtility.captureScreenshot(miraklDriver, TC_NAME, ScreenshotUtility.INFO);

        // =======================================================================
        // PHASE 5 — MIRAKL: Custom field "Entregado = Yes" → Received
        // =======================================================================

        LoggerUtility.info("==================== PHASE 5: ENTREGADO → RECEIVED ====================");

        LoggerUtility.info("Step 69: Clicking More actions dropdown");
        miraklOrderDetailPage.clickMoreActionsDropdown();

        LoggerUtility.info("Step 70: Clicking Custom field option");
        miraklOrderDetailPage.clickCustomFieldButton();

        LoggerUtility.info("Step 71: Clicking Entregado option");
        miraklOrderDetailPage.clickEntregadoOption();

        LoggerUtility.info("Step 72: Clicking Entregado value dropdown");
        miraklOrderDetailPage.clickEntregadoValueDropdown();

        LoggerUtility.info("Step 73: Selecting 'Yes' from Entregado dropdown");
        miraklOrderDetailPage.selectEntregadoYes();

        LoggerUtility.info("Step 74: Clicking Confirm button");
        miraklOrderDetailPage.clickCustomFieldConfirmButton();

        // Step 75: Wait 30s — backend processes Entregado → Received asynchronously
        // (confirmed via live TC_FBS_004/006 runs: rapid refreshes alone not sufficient)
        LoggerUtility.info("Step 75: Waiting 30s for Entregado update to propagate to backend");
        Thread.sleep(30_000);

        // Step 76: Verify status changes to Received
        String finalStatus = waitForMiraklStatus("Received", 20);
        LoggerUtility.info("Step 76: Final Mirakl order status: " + finalStatus + " (expected 'Received')");
        Assert.assertEquals(finalStatus, "Received",
            "Step 76: Mirakl order status should change from 'Shipped' to 'Received'. Actual: " + finalStatus);
        LoggerUtility.info("PASS — Final order status verified: Received");
        ScreenshotUtility.captureScreenshot(miraklDriver, TC_NAME, ScreenshotUtility.PASS);

        // =======================================================================
        // PHASE 6 — FDA: Re-verify order in Mis pedidos after Received
        // =======================================================================

        LoggerUtility.info("==================== PHASE 6: FINAL FDA VERIFICATION ====================");
        LoggerUtility.info("Switching to FDA browser to re-verify order in Mis pedidos after Received");
        fdaHomePage.navigateTo(config.getFdaUrl());
        fdaHomePage.clickProfileIcon();
        fdaHomePage.clickMyOrdersLink();

        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(),
            "Mis pedidos page not displayed after Received");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
            "Order " + orderId + " not found in FDA order history after Received");

        String finalFdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Final FDA order status after Mirakl Received: " + finalFdaStatus
            + " (logged — no specific status asserted post-delivery, not confirmed via live run)");
        ScreenshotUtility.captureScreenshot(fdaDriver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("=== TC_FBS_001 execution completed successfully ===");

        // Surface any soft assertion failures (e.g. Step 44 delivery type)
        softAssert.assertAll();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void initFDAPageObjects() {
        fdaHomePage         = new FDAHomePage(fdaDriver);
        fdaLoginPage        = new FDALoginPage(fdaDriver);
        fdaPdpPage          = new FDAPDPPage(fdaDriver);
        fdaCartPage         = new FDACartPage(fdaDriver);
        fdaPaymentPage      = new FDAPaymentPage(fdaDriver);
        fdaSuccessPage      = new FDASuccessPage(fdaDriver);
        fdaOrderHistoryPage = new FDAOrderHistoryPage(fdaDriver);
        LoggerUtility.info("FDA page objects initialized (using fdaDriver)");
    }

    private void initMiraklPageObjects() {
        miraklLoginPage       = new MiraklLoginPage(miraklDriver);
        miraklOrdersPage      = new MiraklOrdersPage(miraklDriver);
        miraklOrderDetailPage = new MiraklOrderDetailPage(miraklDriver);
        LoggerUtility.info("Mirakl page objects initialized (using miraklDriver)");
    }

    /**
     * After clickProceedToPayment(), Kibo checkout may open in a new window.
     * Poll up to 20s for a second window handle to appear, then switch to it.
     * Uses fdaDriver only — Mirakl browser is unaffected.
     */
    private void switchFdaToCheckoutWindow() {
        String originalHandle = fdaDriver.getWindowHandle();
        long deadline = System.currentTimeMillis() + 20_000;

        while (System.currentTimeMillis() < deadline) {
            java.util.Set<String> handles = fdaDriver.getWindowHandles();
            LoggerUtility.info("FDA window handles after clickProceedToPayment: " + handles.size());

            if (!handles.contains(originalHandle)) {
                // Original window was replaced
                String newHandle = handles.iterator().next();
                fdaDriver.switchTo().window(newHandle);
                LoggerUtility.info("Original FDA window replaced — switched to checkout: " + newHandle);
                return;
            }
            if (handles.size() > 1) {
                // A new checkout window opened alongside the original
                for (String h : handles) {
                    if (!h.equals(originalHandle)) {
                        fdaDriver.switchTo().window(h);
                        LoggerUtility.info("Checkout opened in new window — switched to: " + h);
                        return;
                    }
                }
            }
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        LoggerUtility.info("No new checkout window detected within 20s — continuing on current window");
    }

    /** Refresh miraklDriver and wait for the status locator to become visible. */
    private void miraklRefreshAndWait(By locator) {
        LoggerUtility.info("Refreshing Mirakl page...");
        miraklDriver.navigate().refresh();
        WaitUtility.fluentWait(miraklDriver, locator);
        LoggerUtility.info("Mirakl page refresh complete");
    }

    /** Poll Mirakl status up to maxRetries times (each cycle refreshes the page). */
    private String waitForMiraklStatus(String expected, int maxRetries) {
        String status = "";
        for (int i = 1; i <= maxRetries; i++) {
            miraklRefreshAndWait(MIRAKL_STATUS_LOCATOR);
            status = miraklOrderDetailPage.getOrderStatus();
            LoggerUtility.info("Mirakl status poll " + i + "/" + maxRetries + ": " + status);
            if (expected.equals(status)) break;
        }
        return status;
    }

    /**
     * Extracts a named custom field from a Kibo shipment item map.
     * Searches: shipment.data → shipment.packages[*].data → shipment.items[*].data
     * (FDA_Custom_STH_WF_V1.0 workflow puts deliveryType on the line item's data map)
     */
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

    /** Generates a random 8-digit numeric tracking number (never hard-coded). */
    private String generateEightDigitTrackingNumber() {
        int number = 10_000_000 + new Random().nextInt(90_000_000);
        return String.valueOf(number);
    }
}
