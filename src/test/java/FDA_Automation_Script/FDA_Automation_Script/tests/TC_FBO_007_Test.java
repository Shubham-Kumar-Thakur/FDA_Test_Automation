package FDA_Automation_Script.FDA_Automation_Script.tests;

import FDA_Automation_Script.FDA_Automation_Script.base.BaseClass;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDACartPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAHomePage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDALoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAOrderHistoryPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAPaymentPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAPayPalPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAPDPPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDASuccessPage;
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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TC_FBO_007_Test extends BaseClass {

    private static final String TC_NAME = "TC_FBO_007";

    // TC-specific test data — DO NOT change these values
    private static final String TC_FDA_USER      = "mgowda@kognivera.com";
    private static final String TC_FDA_PASS      = "Mithun@12345";
    private static final String TC_SKU           = "78078094274";
    private static final String TC_PAYPAL_EMAIL  = "sb-1dwgi10803338@personal.example.com";
    private static final String TC_PAYPAL_PASS   = "3+UEw1Q!";
    private static final String TC_EXPECTED_TOTAL = "400";  // MXN$400.00

    // --- Page Objects ---
    private FDAHomePage           fdaHomePage;
    private FDALoginPage          fdaLoginPage;
    private FDAPDPPage            fdaPdpPage;
    private FDACartPage           fdaCartPage;
    private FDAPaymentPage        fdaPaymentPage;
    private FDAPayPalPage         fdaPayPalPage;
    private FDASuccessPage        fdaSuccessPage;
    private FDAOrderHistoryPage   fdaOrderHistoryPage;
    private MiraklOrdersPage      miraklOrdersPage;
    private MiraklOrderDetailPage miraklOrderDetailPage;

    // --- Dynamic test data ---
    private String orderId;
    private String orderTotal;
    private String tplShipmentId;
    private String carrierName;
    private String trackingNumber;
    private String deliveryPartner;
    private String paypalWindowHandle;

    private static final By MIRAKL_ORDER_STATUS_LOCATOR =
        By.xpath("(//span[contains(@class,'_6e782__sc-s4ukbq-0') and not(ancestor::a)])[1]");

    @BeforeClass
    public void initAndLogin() {
        fdaHomePage          = new FDAHomePage(driver);
        fdaLoginPage         = new FDALoginPage(driver);
        fdaPdpPage           = new FDAPDPPage(driver);
        fdaCartPage          = new FDACartPage(driver);
        fdaPaymentPage       = new FDAPaymentPage(driver);
        fdaPayPalPage        = new FDAPayPalPage(driver);
        fdaSuccessPage       = new FDASuccessPage(driver);
        fdaOrderHistoryPage  = new FDAOrderHistoryPage(driver);
        miraklOrdersPage     = new MiraklOrdersPage(driver);
        miraklOrderDetailPage = new MiraklOrderDetailPage(driver);
        LoggerUtility.info(TC_NAME + ": All page objects initialized");

        // TC_FBO_007 uses a different FDA account — logout current session and re-login
        if (!TC_FDA_USER.equals(config.getFdaUsername())) {
            LoggerUtility.info(TC_NAME + ": Switching FDA session to: " + TC_FDA_USER);
            switchToFDATab();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.logout();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.clickProfileIcon();
            fdaHomePage.clickLoginLink();
            fdaLoginPage.login(TC_FDA_USER, TC_FDA_PASS);
            LoggerUtility.info(TC_NAME + ": FDA login as " + TC_FDA_USER + " successful");
        }
    }

    @AfterClass(alwaysRun = true)
    public void restoreOriginalFdaSession() {
        if (!TC_FDA_USER.equals(config.getFdaUsername())) {
            LoggerUtility.info(TC_NAME + " @AfterClass: Restoring suite FDA session");
            try {
                switchToFDATab();
                fdaHomePage.navigateTo(config.getFdaUrl());
                fdaHomePage.logout();
                fdaHomePage.navigateTo(config.getFdaUrl());
                fdaHomePage.clickProfileIcon();
                fdaHomePage.clickLoginLink();
                fdaLoginPage.login(config.getFdaUsername(), config.getFdaPassword());
                LoggerUtility.info(TC_NAME + " @AfterClass: FDA session restored as " + config.getFdaUsername());
            } catch (Exception e) {
                LoggerUtility.error(TC_NAME + " @AfterClass: Failed to restore FDA session: " + e.getMessage());
            }
        }
    }

    @Test(testName = TC_NAME, groups = {"FBO"},
          description = "Verify 3P seller order: 1 product qty=1 with PayPal payment → full fulfillment lifecycle")
    public void tc_fbo_007_single_product_qty1_paypal_full_fulfillment() throws InterruptedException {

        LoggerUtility.info(TC_NAME + " execution started");

        // ============================================================
        // PHASE 1: FDA — Search, PDP, Cart, PayPal Payment, Order
        // ============================================================

        // Step 1: FDA Home Page
        switchToFDATab();
        LoggerUtility.info("Step 1: Verifying FDA Home Page");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test: clear leftover cart items
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // Steps 2-4: Search SKU
        LoggerUtility.info("Step 2: Clicking search field — ¿Qué estás buscando?");
        LoggerUtility.info("Step 3: Entering SKU: " + TC_SKU);
        fdaHomePage.enterSearchQuery(TC_SKU);
        LoggerUtility.info("Step 4: Pressing Enter to search");
        fdaHomePage.pressSearchEnter();
        LoggerUtility.info("SKU " + TC_SKU + " searched successfully");

        // Step 5: PDP validation
        LoggerUtility.info("Step 5: Verifying Product Details Page is displayed");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            TC_NAME + ": PDP not displayed after searching SKU: " + TC_SKU);

        // Step 6: Add to cart button enabled
        LoggerUtility.info("Step 6: Verifying 'Agregar al carrito' button is enabled");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            TC_NAME + ": Add to cart button is not enabled on PDP for SKU: " + TC_SKU);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 7: Verify quantity = 1 (default — no increment for this TC)
        String pdpQty = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 7: PDP product quantity: " + pdpQty);
        Assert.assertEquals(pdpQty, "1",
            TC_NAME + ": Product quantity on PDP should be 1 (default). Actual: " + pdpQty);

        // Step 8: Add to cart
        LoggerUtility.info("Step 8: Clicking 'Agregar al carrito'");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product added to cart with quantity 1");

        // Verify add-to-cart completed — quick 30s check before committing to 2-min wait
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        boolean cartHasItem = false;
        try {
            driver.get(config.getFdaUrl() + "checkout/cart");
            driver.navigate().refresh();
            new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input[data-role='cart-item-qty']")));
            cartHasItem = true;
        } catch (Exception e) {
            LoggerUtility.warn(TC_NAME + ": Cart empty after first add-to-cart — retrying. " + e.getMessage());
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        }
        if (!cartHasItem) {
            LoggerUtility.info(TC_NAME + ": Retrying add-to-cart — navigating back to PDP");
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.enterSearchQuery(TC_SKU);
            fdaHomePage.pressSearchEnter();
            fdaPdpPage.isDisplayed();
            fdaPdpPage.clickAddToCart();
            LoggerUtility.info(TC_NAME + ": Retry add-to-cart complete");
        }

        // Step 9: Navigate to cart
        LoggerUtility.info("Step 9: Clicking Mi carrito (cart icon) — opening cart page");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        // Step 10: Verify 1 product in cart
        int itemCount = fdaCartPage.getItemCount();
        LoggerUtility.info("Step 10: Cart item count: " + itemCount);
        Assert.assertEquals(itemCount, 1,
            TC_NAME + ": Cart should contain exactly 1 product. Actual item count: " + itemCount);

        // Step 11: Verify product name in cart
        String cartProductName = fdaCartPage.getProductName();
        LoggerUtility.info("Step 11: Cart product name: " + cartProductName);
        Assert.assertFalse(cartProductName.isEmpty(),
            TC_NAME + ": Product name should be displayed in cart");

        // Step 12: Verify quantity = 1
        String cartQty = fdaCartPage.getQuantity();
        LoggerUtility.info("Step 12: Cart product quantity: " + cartQty);
        Assert.assertEquals(cartQty, "1",
            TC_NAME + ": Cart product quantity should be 1. Actual: " + cartQty);

        // Step 13: Verify order total
        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 13: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(),
            TC_NAME + ": Order total should be displayed in cart");
        Assert.assertTrue(orderTotal.contains(TC_EXPECTED_TOTAL),
            TC_NAME + ": Order total should reflect MXN$400.00. Actual: " + orderTotal);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 14: Proceed to payment
        LoggerUtility.info("Step 14: Clicking 'Proceed to payment'");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();

        // Step 15: Click Siguiente on shipping page
        LoggerUtility.info("Step 15: Clicking 'Siguiente' on shipping page");
        fdaPaymentPage.clickNextButton();

        // Step 16: Select PayPal radio button
        LoggerUtility.info("Step 16: Selecting 'PayPal' radio button on payment page");
        fdaPaymentPage.selectPayPalOption();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Capture existing window handles before PayPal popup opens
        Set<String> handlesBeforePayPal = new HashSet<>(driver.getWindowHandles());
        LoggerUtility.info("Window handles before PayPal click: " + handlesBeforePayPal.size());

        // Step 18: Click PayPal Pagar button
        LoggerUtility.info("Step 18: Clicking 'PayPal Pagar' button");
        fdaPaymentPage.clickPayPalButton();

        // Step 19: Switch to PayPal popup window
        LoggerUtility.info("Step 19: Detecting and switching to PayPal popup window");
        switchToPayPalWindow(handlesBeforePayPal);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 20: Wait for PayPal page and click email field
        fdaPayPalPage.waitForPageLoad();
        LoggerUtility.info("Step 20: Clicking PayPal 'Correo electrónico o número de celular' field");
        fdaPayPalPage.clickEmailField();

        // Step 21: Enter PayPal email
        LoggerUtility.info("Step 21: Entering PayPal email (value masked in logs)");
        fdaPayPalPage.enterEmail(TC_PAYPAL_EMAIL);

        // Step 22: Click Siguiente on PayPal
        LoggerUtility.info("Step 22: Clicking 'Siguiente' on PayPal login page");
        fdaPayPalPage.clickNextButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 23: Click PayPal password field
        LoggerUtility.info("Step 23: Clicking 'Contraseña' field on PayPal");
        fdaPayPalPage.clickPasswordField();

        // Step 24: CRITICAL — Validate PayPal password is not blank before proceeding
        // DO NOT invent a password. Fail immediately with a clear message if blank.
        LoggerUtility.info("Step 24: Validating PayPal password from test data");
        if (TC_PAYPAL_PASS == null || TC_PAYPAL_PASS.isBlank()) {
            ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.FAIL);
            Assert.fail(
                TC_NAME + ": PayPal password is BLANK in the supplied test data. " +
                "The test cannot proceed past PayPal login without a valid credential. " +
                "Please provide the correct PayPal sandbox password for account: " + TC_PAYPAL_EMAIL +
                ". DO NOT invent a password — update the TC_PAYPAL_PASS constant in " + TC_NAME + "_Test.java.");
        }
        fdaPayPalPage.enterPassword(TC_PAYPAL_PASS);

        // Step 25: Click Iniciar sesión
        LoggerUtility.info("Step 25: Clicking 'Iniciar sesión' on PayPal");
        fdaPayPalPage.clickLoginButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 26: Click Compra completa
        LoggerUtility.info("Step 26: Clicking 'Compra completa' on PayPal review page");
        fdaPayPalPage.clickCompletePayment();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 27: Wait for PayPal window to close and switch back to FDA
        LoggerUtility.info("Step 27: Waiting for PayPal window to close and switching back to FDA");
        waitForPayPalWindowClose();
        LoggerUtility.info("Switched back to FDA checkout/success window");

        // Step 28: Verify success page
        LoggerUtility.info("Step 28: Verifying FDA order success page");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(),
            TC_NAME + ": Order success page not displayed after PayPal payment");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 29: Get Order ID
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 29: Order ID = " + orderId);
        Assert.assertFalse(orderId.isEmpty(),
            TC_NAME + ": Order ID should be present on success page");
        LoggerUtility.info("Order created successfully — Order ID = " + orderId);

        // Step 30: Click Mi cuenta
        LoggerUtility.info("Step 30: Clicking 'Mi cuenta' profile icon");
        fdaHomePage.clickProfileIcon();

        // Step 31: Click Mis pedidos
        LoggerUtility.info("Step 31: Clicking 'Mis pedidos' link");
        fdaHomePage.clickMyOrdersLink();

        // Step 32: Verify Mis pedidos page
        LoggerUtility.info("Step 32: Verifying 'Mis pedidos' page is displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(),
            TC_NAME + ": Mis pedidos page not displayed after navigation");

        // Step 33: Verify Order ID in history
        LoggerUtility.info("Step 33: Verifying Order ID " + orderId + " is present in order history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
            TC_NAME + ": Order ID " + orderId + " not found in order history");

        // Step 34: Verify order status is Creada
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 34: FDA order status: " + fdaStatus);
        Assert.assertTrue(
            fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente") || fdaStatus.equals("Procesando"),
            TC_NAME + ": FDA order status should be 'Creada', 'Pendiente', or 'Procesando'. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 2: MIRAKL — Find WEB-A Shipment, Accept
        // ============================================================

        // Step 35: Switch to Mirakl tab
        LoggerUtility.info("Step 35: Switching to existing Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 36-37: Navigate to All Orders
        LoggerUtility.info("Step 36: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 37: Clicking All Orders");
        miraklOrdersPage.clickAllOrders();

        // Steps 38-40: Search and verify WEB-A shipment
        String miraklSearchTerm = orderId + "WEB";
        String shipmentRef      = orderId + "WEB-A";

        LoggerUtility.info("Step 38: Clicking Mirakl Search field");
        LoggerUtility.info("Step 39: Searching for order: " + miraklSearchTerm);
        boolean orderFoundInMirakl = false;
        for (int attempt = 1; attempt <= 5; attempt++) {
            LoggerUtility.info(TC_NAME + ": Mirakl search attempt " + attempt + "/5 for: " + miraklSearchTerm);
            miraklOrdersPage.searchOrder(miraklSearchTerm);
            if (miraklOrdersPage.hasSearchResults(miraklSearchTerm)) {
                orderFoundInMirakl = true;
                LoggerUtility.info(TC_NAME + ": Mirakl order found on attempt " + attempt);
                break;
            }
            LoggerUtility.info(TC_NAME + ": Order not in Mirakl yet (attempt " + attempt + "/5)");
            if (attempt < 5) {
                LoggerUtility.info("Waiting 60 seconds before next Mirakl search attempt...");
                Thread.sleep(60_000);
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(orderFoundInMirakl,
            TC_NAME + ": Order " + miraklSearchTerm + " did not appear in Mirakl within 5 minutes");

        // Step 40: Verify WEB-A shipment
        LoggerUtility.info("Step 40: Verifying WEB-A shipment exists — shipmentRef: " + shipmentRef);
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRef),
            TC_NAME + ": Expected shipment reference " + shipmentRef + " not found in Mirakl");
        LoggerUtility.info("WEB-A shipment identified: " + shipmentRef);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 41: Open order, verify Pending acceptance, Accept
        LoggerUtility.info("Step 41: Opening Mirakl order: " + shipmentRef);
        miraklOrderDetailPage.clickOrderInList(shipmentRef);

        String miraklStatus = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("Mirakl status before acceptance: " + miraklStatus);
        Assert.assertEquals(miraklStatus, "Pending acceptance",
            TC_NAME + ": Mirakl status should be 'Pending acceptance'. Actual: " + miraklStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Accepting order in Mirakl");
        miraklOrderDetailPage.clickAcceptButton();

        // Step 42: Verify Awaiting shipment after acceptance
        LoggerUtility.info("Step 42: Refreshing Mirakl — waiting for 'Awaiting shipment' status");
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        miraklStatus = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("Mirakl status after acceptance: " + miraklStatus);
        Assert.assertEquals(miraklStatus, "Awaiting shipment",
            TC_NAME + ": Mirakl status should be 'Awaiting shipment'. Actual: " + miraklStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 41 (user spec): Wait 2 minutes after acceptance for order to stabilize in Kibo/Mirakl
        LoggerUtility.info("Step 41: Waiting 2 minutes after acceptance before calling Kibo API...");
        Thread.sleep(120_000);

        // ============================================================
        // PHASE 3: KIBO API — Authenticate + Get Shipment Details
        // ============================================================

        LoggerUtility.info("========== Order Accepted in Mirakl ==========");

        // Step 43: Kibo authentication
        LoggerUtility.info("Step 43: ========== Kibo Authentication ==========");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(),
            TC_NAME + ": Kibo access token should not be empty — authentication failed");
        LoggerUtility.info("Kibo authentication successful");

        // Step 44: Find Kibo order ID by externalId
        String externalId = orderId + "WEB";
        LoggerUtility.info("Step 44: ========== Find Kibo Order ID ==========");
        LoggerUtility.info("Searching Kibo for externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(),
            TC_NAME + ": Kibo Order ID not found for externalId: " + externalId);
        LoggerUtility.info("Kibo Order ID: " + kiboOrderId);

        // Step 45: Poll Kibo shipments until 3PL data is populated
        LoggerUtility.info("Step 45: ========== Get Shipment Details ==========");
        LoggerUtility.info("Polling Kibo shipments (30s interval, up to 10 min)...");
        boolean shipmentDataReady = false;
        for (int attempt = 1; attempt <= 20; attempt++) {
            LoggerUtility.info(TC_NAME + ": Kibo shipment poll attempt " + attempt + "/20");
            Response shipmentsResp = ApiUtility.kiboGetShipmentsByOrderId(kiboToken, kiboOrderId);
            Assert.assertEquals(shipmentsResp.getStatusCode(), 200,
                TC_NAME + ": Kibo Get Shipments should return 200. Actual: " + shipmentsResp.getStatusCode());

            List<Map<String, Object>> shipmentList = shipmentsResp.jsonPath().getList("items");
            if (shipmentList == null || shipmentList.isEmpty()) {
                shipmentList = shipmentsResp.jsonPath().getList("_embedded.shipments");
            }

            if (shipmentList != null && !shipmentList.isEmpty()) {
                Map<String, Object> shipment = shipmentList.get(0);
                deliveryPartner = extractCustomField(shipment, "deliveryPartner");
                tplShipmentId   = extractCustomField(shipment, "3pl_shipmentId");
                carrierName     = extractCustomField(shipment, "carrierName");
                trackingNumber  = extractCustomField(shipment, "tracking_number");

                // For Envioclick, also wait for trackingNumber to be populated
                boolean trackingOk = !deliveryPartner.toLowerCase().contains("envioclick")
                    || !trackingNumber.isEmpty();
                if (!deliveryPartner.isEmpty() && !tplShipmentId.isEmpty() && trackingOk) {
                    shipmentDataReady = true;
                    LoggerUtility.info(TC_NAME + ": Shipment 3PL data ready on attempt " + attempt);
                    break;
                }
            }
            LoggerUtility.info(TC_NAME + ": 3PL data not ready — deliveryPartner='" + deliveryPartner
                + "', tplShipmentId='" + tplShipmentId + "'. Waiting 30s...");
            if (attempt < 20) Thread.sleep(30_000);
        }
        Assert.assertTrue(shipmentDataReady,
            TC_NAME + ": 3PL data (deliveryPartner, 3pl_shipmentId) not populated in Kibo within 10 minutes");

        LoggerUtility.info("Delivery Partner  : " + deliveryPartner);
        LoggerUtility.info("3pl_shipmentId    : " + tplShipmentId);
        LoggerUtility.info("Carrier Name      : " + carrierName);
        LoggerUtility.info("Tracking Number   : " + trackingNumber);

        Assert.assertFalse(deliveryPartner.isEmpty(),
            TC_NAME + ": deliveryPartner missing from Kibo shipment data");
        Assert.assertFalse(tplShipmentId.isEmpty(),
            TC_NAME + ": 3pl_shipmentId missing from Kibo shipment data");
        Assert.assertFalse(carrierName.isEmpty(),
            TC_NAME + ": carrierName missing from Kibo shipment data");
        if (deliveryPartner.toLowerCase().contains("envioclick")) {
            Assert.assertFalse(trackingNumber.isEmpty(),
                TC_NAME + ": tracking_number required for Envioclick flow but missing from Kibo shipment data");
        }

        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("Shipment generated — Carrier = " + carrierName + " | 3PL Shipment ID = " + tplShipmentId);

        // ============================================================
        // PHASE 4: Webhook + Status Validation (Envioclick OR Skydropx)
        // ============================================================

        // Step 46: Branch on delivery partner
        LoggerUtility.info("Step 46: Delivery partner is: " + deliveryPartner);
        if (deliveryPartner.toLowerCase().contains("envioclick")) {
            runEnvioclickFlow();
        } else if (deliveryPartner.toLowerCase().contains("skydropx")) {
            runSkydropxFlow();
        } else {
            Assert.fail(TC_NAME + ": Unknown delivery partner: " + deliveryPartner
                + ". Expected 'envioclick' or 'skydropx'.");
        }

        LoggerUtility.info(TC_NAME + " execution completed successfully");
    }

    // ----------------------------------------------------------------
    // Envioclick flow: En tránsito → Shipped/3PL delivery | Entregado → Received
    // ----------------------------------------------------------------
    private void runEnvioclickFlow() {
        // Step 47: En tránsito
        LoggerUtility.info("Step 47: Calling Envioclick API — En tránsito");
        LoggerUtility.info("EnvioClick Request — carrier: " + carrierName
            + " | idOrder (3pl_shipmentId): " + tplShipmentId
            + " | trackingCode: " + trackingNumber
            + " | myShipmentReference: " + orderId + "WEB");
        Response enTransitoResp = ApiUtility.postEnvioclickEnTransito(
            tplShipmentId, trackingNumber, orderId, carrierName);
        Assert.assertEquals(enTransitoResp.getStatusCode(), 200,
            TC_NAME + ": Envioclick En tránsito API should return 200. Actual: " + enTransitoResp.getStatusCode());
        LoggerUtility.info("EnvioClick En tránsito API Response = " + enTransitoResp.getStatusCode());

        // Step 48: Mirakl → Shipped or 3PL delivery
        LoggerUtility.info("Step 48: Switching to Mirakl — checking for Shipped/3PL delivery status");
        switchToMiraklTab();
        String status = waitForMiraklStatusOneOf(new String[]{"Shipped", "3PL delivery"}, 60);
        LoggerUtility.info("Mirakl status after En tránsito: " + status);
        Assert.assertTrue(status.equals("Shipped") || status.equals("3PL delivery"),
            TC_NAME + ": Mirakl status should be 'Shipped' or '3PL delivery' after En tránsito. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 49: Entregado
        LoggerUtility.info("Step 49: Calling Envioclick API — Entregado");
        LoggerUtility.info("EnvioClick Request — carrier: " + carrierName
            + " | idOrder (3pl_shipmentId): " + tplShipmentId
            + " | trackingCode: " + trackingNumber
            + " | myShipmentReference: " + orderId + "WEB");
        Response entregadoResp = ApiUtility.postEnvioclickEntregado(
            tplShipmentId, trackingNumber, orderId, carrierName);
        Assert.assertEquals(entregadoResp.getStatusCode(), 200,
            TC_NAME + ": Envioclick Entregado API should return 200. Actual: " + entregadoResp.getStatusCode());
        LoggerUtility.info("EnvioClick Entregado API Response = " + entregadoResp.getStatusCode());

        // Step 50: Mirakl → Received
        LoggerUtility.info("Step 50: Refreshing Mirakl — checking for Received status");
        status = waitForMiraklStatus("Received", 20);
        LoggerUtility.info("Mirakl status after Entregado: " + status);
        Assert.assertEquals(status, "Received",
            TC_NAME + ": Mirakl status should be 'Received' after Entregado. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        LoggerUtility.info("Envioclick flow complete — Mirakl Status = Received");
    }

    // ----------------------------------------------------------------
    // Skydropx flow: Picked_up → Shipped | Delivered → Received
    // ----------------------------------------------------------------
    private void runSkydropxFlow() {
        // Step 51: Picked_up
        LoggerUtility.info("Step 51: Calling Skydropx API — Picked_up");
        LoggerUtility.info("Skydropx Request — id (3pl_shipmentId): " + tplShipmentId);
        Response pickedUpResp = ApiUtility.postSkydropxPickedUp(tplShipmentId);
        Assert.assertEquals(pickedUpResp.getStatusCode(), 200,
            TC_NAME + ": Skydropx Picked_up API should return 200. Actual: " + pickedUpResp.getStatusCode());
        LoggerUtility.info("Skydropx Picked_up API Response = " + pickedUpResp.getStatusCode());

        // Step 52: Mirakl → Shipped
        LoggerUtility.info("Step 52: Switching to Mirakl — checking for Shipped status");
        switchToMiraklTab();
        String status = waitForMiraklStatus("Shipped", 60);
        LoggerUtility.info("Mirakl status after Picked_up: " + status);
        Assert.assertEquals(status, "Shipped",
            TC_NAME + ": Mirakl status should be 'Shipped' after Skydropx Picked_up. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 53: Delivered
        LoggerUtility.info("Step 53: Calling Skydropx API — Delivered");
        LoggerUtility.info("Skydropx Request — id (3pl_shipmentId): " + tplShipmentId);
        Response deliveredResp = ApiUtility.postSkydropxDelivered(tplShipmentId);
        Assert.assertEquals(deliveredResp.getStatusCode(), 200,
            TC_NAME + ": Skydropx Delivered API should return 200. Actual: " + deliveredResp.getStatusCode());
        LoggerUtility.info("Skydropx Delivered API Response = " + deliveredResp.getStatusCode());

        // Step 54: Mirakl → Received
        LoggerUtility.info("Step 54: Refreshing Mirakl — checking for Received status");
        status = waitForMiraklStatus("Received", 20);
        LoggerUtility.info("Mirakl status after Delivered: " + status);
        Assert.assertEquals(status, "Received",
            TC_NAME + ": Mirakl status should be 'Received' after Skydropx Delivered. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        LoggerUtility.info("Skydropx flow complete — Mirakl Status = Received");
    }

    // ----------------------------------------------------------------
    // Poll Mirakl status — refresh until expected status or maxRetries
    // ----------------------------------------------------------------
    private String waitForMiraklStatus(String expected, int maxRetries) {
        String status = "";
        for (int i = 1; i <= maxRetries; i++) {
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            status = miraklOrderDetailPage.getOrderStatus();
            LoggerUtility.info(TC_NAME + ": Mirakl status check " + i + "/" + maxRetries + ": " + status);
            if (expected.equals(status)) break;
        }
        return status;
    }

    private String waitForMiraklStatusOneOf(String[] expected, int maxRetries) {
        Set<String> expectedSet = new HashSet<>(Arrays.asList(expected));
        String status = "";
        for (int i = 1; i <= maxRetries; i++) {
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            status = miraklOrderDetailPage.getOrderStatus();
            LoggerUtility.info(TC_NAME + ": Mirakl status check " + i + "/" + maxRetries + ": " + status);
            if (expectedSet.contains(status)) break;
        }
        return status;
    }

    // ----------------------------------------------------------------
    // Extract a named custom field from a Kibo shipment item map.
    // Searches item.data, then item.packages[*].data.
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
        return "";
    }

    // ----------------------------------------------------------------
    // Handle checkout window opening in a new OS window after clickProceedToPayment
    // Updates fdaTabHandle to the checkout window handle if it changed.
    // ----------------------------------------------------------------
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

    // ----------------------------------------------------------------
    // Detect the new PayPal popup window and switch to it.
    // handlesBeforePayPal = set of window handles captured before clicking PayPal button.
    // ----------------------------------------------------------------
    private void switchToPayPalWindow(Set<String> handlesBeforePayPal) {
        // Wait up to 15 seconds for the new PayPal window to appear
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
                LoggerUtility.info("Step 19: Switched to PayPal popup window. Handle: " + paypalWindowHandle);
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
