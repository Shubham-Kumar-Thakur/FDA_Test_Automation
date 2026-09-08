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
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
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

public class TC_FBO_009_Test extends BaseClass {

    private static final String TC_NAME = "TC_FBO_009";

    // TC-specific test data — DO NOT change these values
    private static final String TC_FDA_USER     = "mgowda@kognivera.com";
    private static final String TC_FDA_PASS     = "Mithun@12345";
    private static final String TC_SKU          = "78078094274";
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
    private MiraklOrdersPage      miraklOrdersPage;
    private MiraklOrderDetailPage miraklOrderDetailPage;

    // --- Dynamic test data ---
    private String orderId;
    private String orderTotal;
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
          description = "Verify 1 product qty=2 from 1 3P seller with PayPal payment → 1 Mirakl shipment WEB-A → Received")
    public void tc_fbo_009_one_product_qty2_paypal_one_seller_full_fulfillment() throws InterruptedException {

        LoggerUtility.info(TC_NAME + " execution started");

        // ============================================================
        // PHASE 1: FDA — Search SKU, Increase Qty to 2, Cart, PayPal
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

        // Step 2-4: Search
        LoggerUtility.info("Step 2: Clicking search field");
        LoggerUtility.info("Step 3: Entering SKU: " + TC_SKU);
        fdaHomePage.enterSearchQuery(TC_SKU);
        LoggerUtility.info("Step 4: Pressing Enter to search");
        fdaHomePage.pressSearchEnter();

        // Step 5-6: PDP
        LoggerUtility.info("Step 5: Verifying PDP is displayed");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            TC_NAME + ": PDP not displayed for SKU: " + TC_SKU);

        LoggerUtility.info("Step 6: Verifying 'Agregar al carrito' button enabled");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            TC_NAME + ": Add to cart button not enabled for SKU: " + TC_SKU);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 7: Increase quantity to 2
        LoggerUtility.info("Step 7: Clicking 'plus' button to increase quantity to 2");
        fdaPdpPage.increaseQuantity();

        // Wait for qty field to reflect "2" — UI update is async
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> "2".equals(fdaPdpPage.getQuantity()));
        } catch (Exception e) {
            LoggerUtility.warn(TC_NAME + ": Qty not updated to 2 after increaseQuantity — retrying click. " + e.getMessage());
            fdaPdpPage.increaseQuantity();
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        }

        // Step 8: Verify qty = 2 on PDP
        String pdpQty = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 8: PDP quantity after increase: " + pdpQty);
        Assert.assertEquals(pdpQty, "2",
            TC_NAME + ": PDP quantity should be 2 after clicking plus. Actual: " + pdpQty);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 9: Add to cart
        LoggerUtility.info("Step 9: Clicking 'Agregar al carrito' button");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product added to cart (qty=2)");

        // Quick cart check — item must be in cart
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
            LoggerUtility.warn(TC_NAME + ": Cart empty after add-to-cart — retrying. " + e.getMessage());
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        }
        if (!cartHasItem) {
            LoggerUtility.info(TC_NAME + ": Retrying add-to-cart for SKU: " + TC_SKU);
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.enterSearchQuery(TC_SKU);
            fdaHomePage.pressSearchEnter();
            fdaPdpPage.isDisplayed();
            fdaPdpPage.increaseQuantity(); // restore qty to 2
            fdaPdpPage.clickAddToCart();
            LoggerUtility.info(TC_NAME + ": Retry add-to-cart complete");
        }

        // Step 10: Open cart
        LoggerUtility.info("Step 10: Clicking 'Mi carrito' cart icon");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        // Step 11: Verify 1 product in cart
        int itemCount = fdaCartPage.getItemCount();
        LoggerUtility.info("Step 11: Cart item count: " + itemCount);
        Assert.assertEquals(itemCount, 1,
            TC_NAME + ": Cart should contain exactly 1 product. Actual: " + itemCount);

        // Step 12: Verify product name
        List<String> cartProductNames = fdaCartPage.getAllProductNames();
        LoggerUtility.info("Step 12: Cart product names: " + cartProductNames);
        Assert.assertFalse(cartProductNames.isEmpty(),
            TC_NAME + ": Product name should not be empty in cart");
        Assert.assertFalse(cartProductNames.get(0).isEmpty(),
            TC_NAME + ": Product 1 name should not be empty in cart");

        // Step 13: Verify quantity = 2
        List<String> quantities = fdaCartPage.getAllQuantities();
        LoggerUtility.info("Step 13: Cart quantities: " + quantities);
        Assert.assertFalse(quantities.isEmpty(),
            TC_NAME + ": Quantities list should not be empty in cart");
        Assert.assertEquals(quantities.get(0), "2",
            TC_NAME + ": Cart quantity should be 2. Actual: " + quantities.get(0));

        // Step 14: Order total
        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 14: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(),
            TC_NAME + ": Order total should be displayed in cart");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 15-17: Proceed to payment, Siguiente, PayPal radio
        LoggerUtility.info("Step 15: Clicking 'Proceed to payment'");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();

        LoggerUtility.info("Step 16: Clicking 'Siguiente' on shipping page");
        fdaPaymentPage.clickNextButton();

        LoggerUtility.info("Step 17: Clicking 'PayPal' radio button");
        fdaPaymentPage.selectPayPalOption();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Capture handles before PayPal popup
        Set<String> handlesBeforePayPal = new HashSet<>(driver.getWindowHandles());
        LoggerUtility.info("Window handles before PayPal click: " + handlesBeforePayPal.size());

        // Step 18: Click PayPal Pagar
        LoggerUtility.info("Step 18: Clicking 'PayPal Pagar' button");
        fdaPaymentPage.clickPayPalButton();

        // Step 19: Switch to PayPal popup
        LoggerUtility.info("Step 19: Detecting and switching to PayPal popup window");
        switchToPayPalWindow(handlesBeforePayPal);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 20-26: PayPal Login + Complete
        fdaPayPalPage.waitForPageLoad();
/*
        LoggerUtility.info("Step 20: Clicking PayPal email field");
        fdaPayPalPage.clickEmailField();

        LoggerUtility.info("Step 21: Entering PayPal email (masked in logs)");
        fdaPayPalPage.enterEmail(TC_PAYPAL_EMAIL);

        LoggerUtility.info("Step 22: Clicking 'Siguiente' on PayPal");
        fdaPayPalPage.clickNextButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 23: Clicking 'Contraseña' field on PayPal");
        fdaPayPalPage.clickPasswordField();

        LoggerUtility.info("Step 24: Entering PayPal password (masked in logs)");
        fdaPayPalPage.enterPassword(TC_PAYPAL_PASS);

        LoggerUtility.info("Step 25: Clicking 'Iniciar sesión' on PayPal");
        fdaPayPalPage.clickLoginButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
*/
        LoggerUtility.info("Step 26: Clicking 'Compra completa' on PayPal review page");
        fdaPayPalPage.clickCompletePayment();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 27: PayPal closed, back to FDA
        LoggerUtility.info("Step 27: Waiting for PayPal window to close and switching back to FDA");
        waitForPayPalWindowClose();
        LoggerUtility.info("Switched back to FDA checkout/success window");

        // Step 28-29: Success page + Order ID
        LoggerUtility.info("Step 28: Verifying FDA order success page");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(),
            TC_NAME + ": Order success page not displayed after PayPal payment");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 29: Order ID = " + orderId);
        Assert.assertFalse(orderId.isEmpty(),
            TC_NAME + ": Order ID should be present on success page");
        LoggerUtility.info("Order created successfully — Order ID = " + orderId);

        // Steps 30-34: Order History
        LoggerUtility.info("Step 30: Clicking 'Mi cuenta' profile icon");
        fdaHomePage.clickProfileIcon();

        LoggerUtility.info("Step 31: Clicking 'Mis pedidos' link");
        fdaHomePage.clickMyOrdersLink();

        LoggerUtility.info("Step 32: Verifying 'Mis pedidos' page is displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(),
            TC_NAME + ": Mis pedidos page not displayed");

        LoggerUtility.info("Step 33: Verifying Order ID " + orderId + " is present in order history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
            TC_NAME + ": Order ID " + orderId + " not found in order history");

        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 34: FDA order status: " + fdaStatus);
        Assert.assertTrue(
            fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente") || fdaStatus.equals("Procesando"),
            TC_NAME + ": FDA order status should be 'Creada', 'Pendiente', or 'Procesando'. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 2: MIRAKL — Find WEB-A, Accept
        // ============================================================

        LoggerUtility.info("Step 35: Switching to Mirakl tab");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 36: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 37: Clicking All Orders");
        miraklOrdersPage.clickAllOrders();

        String miraklSearchTerm = orderId + "WEB";
        String shipmentRefA     = orderId + "WEB-A";

        LoggerUtility.info("Step 38-39: Searching for order: " + miraklSearchTerm);
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
                driver.get(config.getMiraklUrl());
                miraklOrdersPage.clickOrdersMenu();
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(orderFoundInMirakl,
            TC_NAME + ": Order " + miraklSearchTerm + " did not appear in Mirakl within 5 minutes");

        // Step 40: Verify 1 shipment WEB-A
        LoggerUtility.info("Step 40: Verifying 1 shipment — WEB-A: " + shipmentRefA);
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefA),
            TC_NAME + ": WEB-A shipment not found in Mirakl: " + shipmentRefA);
        LoggerUtility.info("1 shipment verified — WEB-A present");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 41: Accept WEB-A
        LoggerUtility.info("Step 41: Opening and accepting WEB-A shipment: " + shipmentRefA);
        miraklOrderDetailPage.clickOrderInList(shipmentRefA);

        String statusA = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-A status before acceptance: " + statusA);
        Assert.assertEquals(statusA, "Pending acceptance",
            TC_NAME + ": WEB-A should be 'Pending acceptance'. Actual: " + statusA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        statusA = miraklOrderDetailPage.getOrderStatus();
        Assert.assertEquals(statusA, "Awaiting shipment",
            TC_NAME + ": WEB-A should be 'Awaiting shipment' after accept. Actual: " + statusA);
        String urlShipmentA = driver.getCurrentUrl();
        LoggerUtility.info("WEB-A accepted — URL: " + urlShipmentA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 42: Wait 2 minutes
        LoggerUtility.info("Step 42: Waiting 2 minutes after acceptance for order to stabilize in Kibo...");
        Thread.sleep(120_000);

        // ============================================================
        // PHASE 3: KIBO API — Auth + Collect Shipment Details
        // ============================================================

        LoggerUtility.info("Step 43: ========== Kibo Authentication ==========");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(),
            TC_NAME + ": Kibo access token should not be empty — authentication failed");
        LoggerUtility.info("Kibo authentication successful");

        String externalId = orderId + "WEB";
        LoggerUtility.info("Step 44: ========== Find Kibo Order ID ==========");
        LoggerUtility.info("Searching Kibo for externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(),
            TC_NAME + ": Kibo Order ID not found for externalId: " + externalId);
        LoggerUtility.info("Kibo Order ID: " + kiboOrderId);

        LoggerUtility.info("Step 45: ========== Polling Kibo Shipment ==========");
        LoggerUtility.info("Polling Kibo shipment (30s interval, up to 10 min)...");
        String deliveryPartner = "";
        String tplShipmentId   = "";
        String carrierName     = "";
        String trackingNumber  = "";
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
                Map<String, Object> s = shipmentList.get(0);
                deliveryPartner = extractCustomField(s, "deliveryPartner");
                tplShipmentId   = extractCustomField(s, "3pl_shipmentId");
                carrierName     = extractCustomField(s, "carrierName");
                trackingNumber  = extractCustomField(s, "tracking_number");

                boolean trackingOk = !deliveryPartner.toLowerCase().contains("envioclick")
                    || !trackingNumber.isEmpty();

                if (!deliveryPartner.isEmpty() && !tplShipmentId.isEmpty() && trackingOk) {
                    shipmentDataReady = true;
                    LoggerUtility.info(TC_NAME + ": Shipment data ready — attempt " + attempt);
                    break;
                }
                LoggerUtility.info("Shipment 3PL data not fully ready — deliveryPartner='"
                    + deliveryPartner + "' tplShipmentId='" + tplShipmentId
                    + "' trackingNumber='" + trackingNumber + "'");
            } else {
                LoggerUtility.info(TC_NAME + ": Kibo returned no shipments — waiting 30s");
            }
            if (attempt < 20) Thread.sleep(30_000);
        }

        Assert.assertTrue(shipmentDataReady,
            TC_NAME + ": Shipment 3PL data not populated in Kibo within 10 minutes");
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
        LoggerUtility.info("Delivery Partner : " + deliveryPartner);
        LoggerUtility.info("3PL Shipment ID  : " + tplShipmentId);
        LoggerUtility.info("Carrier          : " + carrierName);
        LoggerUtility.info("Tracking Number  : " + trackingNumber);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: Carrier Webhook + Status Validation
        // ============================================================

        // Step 46+: Branch by delivery partner
        LoggerUtility.info("Step 46: Delivery partner: " + deliveryPartner);
        switchToMiraklTab();
        driver.get(urlShipmentA);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);

        if (deliveryPartner.toLowerCase().contains("envioclick")) {
            LoggerUtility.info("Step 47: Calling Envioclick API — En tránsito");
            LoggerUtility.info("EnvioClick Request — carrier: " + carrierName
                + " | idOrder (3pl_shipmentId): " + tplShipmentId
                + " | trackingCode: " + trackingNumber
                + " | myShipmentReference: " + orderId + "WEB");
            Response enTransito = ApiUtility.postEnvioclickEnTransito(
                tplShipmentId, trackingNumber, orderId, carrierName);
            Assert.assertEquals(enTransito.getStatusCode(), 200,
                TC_NAME + ": Envioclick En tránsito should return 200. Actual: " + enTransito.getStatusCode());
            LoggerUtility.info("EnvioClick En tránsito API Response = " + enTransito.getStatusCode());

            LoggerUtility.info("Step 48: Checking Mirakl for Shipped/3PL delivery status");
            String status = waitForMiraklStatusOneOf(new String[]{"Shipped", "3PL delivery"}, 20);
            LoggerUtility.info("Mirakl status after En tránsito: " + status);
            Assert.assertTrue(status.equals("Shipped") || status.equals("3PL delivery"),
                TC_NAME + ": Mirakl status should be 'Shipped' or '3PL delivery' after En tránsito. Actual: " + status);
            ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

            LoggerUtility.info("Step 49: Calling Envioclick API — Entregado");
            Response entregado = ApiUtility.postEnvioclickEntregado(
                tplShipmentId, trackingNumber, orderId, carrierName);
            Assert.assertEquals(entregado.getStatusCode(), 200,
                TC_NAME + ": Envioclick Entregado should return 200. Actual: " + entregado.getStatusCode());
            LoggerUtility.info("EnvioClick Entregado API Response = " + entregado.getStatusCode());

            LoggerUtility.info("Step 50: Checking Mirakl for Received status");
            status = waitForMiraklStatus("Received", 20);
            LoggerUtility.info("Mirakl status after Entregado: " + status);
            Assert.assertEquals(status, "Received",
                TC_NAME + ": Mirakl status should be 'Received' after Entregado. Actual: " + status);

        } else if (deliveryPartner.toLowerCase().contains("skydropx")) {
            LoggerUtility.info("Step 51: Calling Skydropx API — Picked_up");
            LoggerUtility.info("Skydropx Request — id (3pl_shipmentId): " + tplShipmentId);
            Response pickedUp = ApiUtility.postSkydropxPickedUp(tplShipmentId);
            Assert.assertEquals(pickedUp.getStatusCode(), 200,
                TC_NAME + ": Skydropx Picked_up should return 200. Actual: " + pickedUp.getStatusCode());
            LoggerUtility.info("Skydropx Picked_up API Response = " + pickedUp.getStatusCode());

            LoggerUtility.info("Step 52: Checking Mirakl for Shipped status");
            String status = waitForMiraklStatus("Shipped", 20);
            LoggerUtility.info("Mirakl status after Picked_up: " + status);
            Assert.assertEquals(status, "Shipped",
                TC_NAME + ": Mirakl status should be 'Shipped' after Skydropx Picked_up. Actual: " + status);
            ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

            LoggerUtility.info("Step 53: Calling Skydropx API — Delivered");
            Response delivered = ApiUtility.postSkydropxDelivered(tplShipmentId);
            Assert.assertEquals(delivered.getStatusCode(), 200,
                TC_NAME + ": Skydropx Delivered should return 200. Actual: " + delivered.getStatusCode());
            LoggerUtility.info("Skydropx Delivered API Response = " + delivered.getStatusCode());

            LoggerUtility.info("Step 54: Checking Mirakl for Received status");
            status = waitForMiraklStatus("Received", 20);
            LoggerUtility.info("Mirakl status after Delivered: " + status);
            Assert.assertEquals(status, "Received",
                TC_NAME + ": Mirakl status should be 'Received' after Skydropx Delivered. Actual: " + status);

        } else {
            Assert.fail(TC_NAME + ": Unknown delivery partner: " + deliveryPartner
                + ". Expected 'envioclick' or 'skydropx'.");
        }

        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        LoggerUtility.info(TC_NAME + " execution completed successfully — WEB-A = Received");
    }

    // ----------------------------------------------------------------
    // Poll Mirakl status until expected or maxRetries exhausted
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
    // Extract a named custom field from a Kibo shipment item map
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
    // Detect checkout window after clickProceedToPayment
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
    // Detect new PayPal popup and switch to it
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
        Assert.fail(TC_NAME + ": PayPal popup window did not open within 15 seconds");
    }

    // ----------------------------------------------------------------
    // Wait for PayPal popup to close, then switch back to FDA
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
