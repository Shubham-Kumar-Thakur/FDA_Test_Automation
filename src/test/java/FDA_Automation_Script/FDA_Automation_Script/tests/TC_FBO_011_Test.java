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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TC_FBO_011_Test extends BaseClass {

    private static final String TC_NAME = "TC_FBO_011";

    // TC-specific test data — DO NOT change these values
    private static final String TC_FDA_USER     = "mgowda@kognivera.com";
    private static final String TC_FDA_PASS     = "Mithun@12345";
    private static final String TC_SKU_1        = "7080901020316";
    private static final String TC_SKU_2        = "78078094274";
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

    // Per-shipment data collected from Kibo
    private static class ShipmentInfo {
        String shipmentRef;
        String tplShipmentId;
        String carrierName;
        String trackingNumber;
        String deliveryPartner;
        String miraklDetailUrl;
    }

    private final List<ShipmentInfo> shipments = new ArrayList<>();

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
          description = "Verify 2 products qty=1 each from 2 different 3P sellers with PayPal payment → 2 Mirakl shipments → both Received")
    public void tc_fbo_011_two_products_qty1_paypal_two_sellers_full_fulfillment() throws InterruptedException {

        LoggerUtility.info(TC_NAME + " execution started");

        // ============================================================
        // PHASE 1: FDA — Search 2 SKUs, Cart, PayPal Payment, Order
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

        // ---- Steps 2-8: Product 1 (TC_SKU_1) ----
        LoggerUtility.info("Step 2: Clicking search field");
        LoggerUtility.info("Step 3: Entering SKU 1: " + TC_SKU_1);
        fdaHomePage.enterSearchQuery(TC_SKU_1);
        LoggerUtility.info("Step 4: Pressing Enter to search");
        fdaHomePage.pressSearchEnter();

        LoggerUtility.info("Step 5: Verifying PDP for Product 1");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            TC_NAME + ": PDP not displayed for SKU 1: " + TC_SKU_1);

        LoggerUtility.info("Step 6: Verifying 'Agregar al carrito' button enabled for Product 1");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            TC_NAME + ": Add to cart button not enabled for SKU 1: " + TC_SKU_1);

        String pdpQty1 = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 7: Product 1 PDP quantity: " + pdpQty1);
        Assert.assertEquals(pdpQty1, "1",
            TC_NAME + ": Product 1 PDP quantity should be 1. Actual: " + pdpQty1);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 8: Clicking 'Agregar al carrito' for Product 1");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product 1 added to cart");

        // Navigate home before searching second product
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ---- Steps 9-15: Product 2 (TC_SKU_2) ----
        LoggerUtility.info("Step 9: Clicking search field");
        LoggerUtility.info("Step 10: Entering SKU 2: " + TC_SKU_2);
        fdaHomePage.enterSearchQuery(TC_SKU_2);
        LoggerUtility.info("Step 11: Pressing Enter to search");
        fdaHomePage.pressSearchEnter();

        LoggerUtility.info("Step 12: Verifying PDP for Product 2");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            TC_NAME + ": PDP not displayed for SKU 2: " + TC_SKU_2);

        LoggerUtility.info("Step 13: Verifying 'Agregar al carrito' button enabled for Product 2");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            TC_NAME + ": Add to cart button not enabled for SKU 2: " + TC_SKU_2);

        String pdpQty2 = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 14: Product 2 PDP quantity: " + pdpQty2);
        Assert.assertEquals(pdpQty2, "1",
            TC_NAME + ": Product 2 PDP quantity should be 1. Actual: " + pdpQty2);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 15: Clicking 'Agregar al carrito' for Product 2");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product 2 added to cart");

        // Quick cart check — both items must be present before full openAndRefreshCart
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        boolean cartHas2Items = false;
        try {
            driver.get(config.getFdaUrl() + "checkout/cart");
            driver.navigate().refresh();
            new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(d -> d.findElements(By.cssSelector("input[data-role='cart-item-qty']")).size() >= 2);
            cartHas2Items = true;
        } catch (Exception e) {
            LoggerUtility.warn(TC_NAME + ": Cart has fewer than 2 items after add — retrying SKU 2. " + e.getMessage());
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        }
        if (!cartHas2Items) {
            LoggerUtility.info(TC_NAME + ": Retrying add-to-cart for SKU 2");
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.enterSearchQuery(TC_SKU_2);
            fdaHomePage.pressSearchEnter();
            fdaPdpPage.isDisplayed();
            fdaPdpPage.clickAddToCart();
            LoggerUtility.info(TC_NAME + ": Retry add-to-cart for SKU 2 complete");
        }

        // ---- Steps 16-22: Cart Validation ----
        LoggerUtility.info("Step 16: Opening cart page");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        int itemCount = fdaCartPage.getItemCount();
        LoggerUtility.info("Step 17: Cart item count: " + itemCount);
        Assert.assertEquals(itemCount, 2,
            TC_NAME + ": Cart should contain exactly 2 products. Actual: " + itemCount);

        List<String> cartProductNames = fdaCartPage.getAllProductNames();
        LoggerUtility.info("Step 18-19: Cart product names: " + cartProductNames);
        Assert.assertEquals(cartProductNames.size(), 2,
            TC_NAME + ": Should have 2 product names in cart. Actual: " + cartProductNames.size());
        Assert.assertFalse(cartProductNames.get(0).isEmpty(),
            TC_NAME + ": Product 1 name should not be empty in cart");
        Assert.assertFalse(cartProductNames.get(1).isEmpty(),
            TC_NAME + ": Product 2 name should not be empty in cart");

        List<String> quantities = fdaCartPage.getAllQuantities();
        LoggerUtility.info("Step 20-21: Cart quantities: " + quantities);
        Assert.assertEquals(quantities.size(), 2,
            TC_NAME + ": Should have 2 quantity inputs in cart. Actual: " + quantities.size());
        Assert.assertEquals(quantities.get(0), "1",
            TC_NAME + ": Product 1 cart quantity should be 1. Actual: " + quantities.get(0));
        Assert.assertEquals(quantities.get(1), "1",
            TC_NAME + ": Product 2 cart quantity should be 1. Actual: " + quantities.get(1));

        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 22: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(),
            TC_NAME + ": Order total should be displayed in cart");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ---- Step 23-25: Checkout + PayPal Selection ----
        LoggerUtility.info("Step 23: Clicking 'Proceed to payment'");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();

        LoggerUtility.info("Step 24: Clicking 'Siguiente' on shipping page");
        fdaPaymentPage.clickNextButton();

        LoggerUtility.info("Step 25: Selecting 'PayPal' radio button");
        fdaPaymentPage.selectPayPalOption();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Capture handles before PayPal popup opens
        Set<String> handlesBeforePayPal = new HashSet<>(driver.getWindowHandles());
        LoggerUtility.info("Window handles before PayPal click: " + handlesBeforePayPal.size());

        // ---- Step 26: Click PayPal Pagar button ----
        LoggerUtility.info("Step 26: Clicking 'PayPal Pagar' button");
        fdaPaymentPage.clickPayPalButton();

        // ---- Steps 27-28: Switch to PayPal popup ----
        LoggerUtility.info("Step 27-28: Detecting and switching to PayPal popup window");
        switchToPayPalWindow(handlesBeforePayPal);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ---- Steps 29-35: PayPal Login + Complete ----
        fdaPayPalPage.waitForPageLoad();
/*
        LoggerUtility.info("Step 29: Clicking PayPal email field");
        fdaPayPalPage.clickEmailField();

        LoggerUtility.info("Step 30: Entering PayPal email (masked in logs)");
        fdaPayPalPage.enterEmail(TC_PAYPAL_EMAIL);

        LoggerUtility.info("Step 31: Clicking 'Siguiente' on PayPal");
        fdaPayPalPage.clickNextButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 32: Clicking 'Contraseña' field on PayPal");
        fdaPayPalPage.clickPasswordField();

        LoggerUtility.info("Step 33: Entering PayPal password (masked in logs)");
        fdaPayPalPage.enterPassword(TC_PAYPAL_PASS);

        LoggerUtility.info("Step 34: Clicking 'Iniciar sesión' on PayPal");
        fdaPayPalPage.clickLoginButton();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
*/
        LoggerUtility.info("Step 35: Clicking 'Compra completa' on PayPal review page");
        fdaPayPalPage.clickCompletePayment();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ---- Steps 36-38: PayPal window closed, back to FDA ----
        LoggerUtility.info("Step 36-38: Waiting for PayPal window to close and switching back to FDA");
        waitForPayPalWindowClose();
        LoggerUtility.info("Switched back to FDA checkout/success window");

        // ---- Steps 39-40: Success Page + Order ID ----
        LoggerUtility.info("Step 39: Verifying FDA order success page");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(),
            TC_NAME + ": Order success page not displayed after PayPal payment");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 40: Order ID = " + orderId);
        Assert.assertFalse(orderId.isEmpty(),
            TC_NAME + ": Order ID should be present on success page");
        LoggerUtility.info("Order created successfully — Order ID = " + orderId);

        // ---- Steps 41-45: Order History Validation ----
        LoggerUtility.info("Step 41: Clicking 'Mi cuenta' profile icon");
        fdaHomePage.clickProfileIcon();

        LoggerUtility.info("Step 42: Clicking 'Mis pedidos' link");
        fdaHomePage.clickMyOrdersLink();

        LoggerUtility.info("Step 43: Verifying 'Mis pedidos' page is displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(),
            TC_NAME + ": Mis pedidos page not displayed");

        LoggerUtility.info("Step 44: Verifying Order ID " + orderId + " is present in order history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
            TC_NAME + ": Order ID " + orderId + " not found in order history");

        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 45: FDA order status: " + fdaStatus);
        Assert.assertTrue(
            fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente") || fdaStatus.equals("Procesando"),
            TC_NAME + ": FDA order status should be 'Creada', 'Pendiente', or 'Procesando'. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 2: MIRAKL — Find WEB-A + WEB-B, Accept Both
        // ============================================================

        LoggerUtility.info("Step 46: Switching to Mirakl tab");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 47: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 48: Clicking All Orders");
        miraklOrdersPage.clickAllOrders();

        String miraklSearchTerm = orderId + "WEB";
        String shipmentRefA     = orderId + "WEB-A";
        String shipmentRefB     = orderId + "WEB-B";

        LoggerUtility.info("Step 49-50: Searching for order: " + miraklSearchTerm);
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

        // Step 51-52: Verify exactly 2 shipments (WEB-A + WEB-B)
        LoggerUtility.info("Step 51-52: Verifying 2 shipments — WEB-A: " + shipmentRefA + " | WEB-B: " + shipmentRefB);
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefA),
            TC_NAME + ": WEB-A shipment not found in Mirakl: " + shipmentRefA);
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefB),
            TC_NAME + ": WEB-B shipment not found in Mirakl: " + shipmentRefB);
        LoggerUtility.info("2 shipments verified — WEB-A and WEB-B present");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 53-55: Accept WEB-A
        LoggerUtility.info("Step 53: Opening and accepting WEB-A shipment: " + shipmentRefA);
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
        LoggerUtility.info("Step 54-55: WEB-A accepted — URL: " + urlShipmentA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Navigate back to accept WEB-B
        LoggerUtility.info("Navigating back to All Orders to accept WEB-B");
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        miraklOrdersPage.searchOrder(miraklSearchTerm);

        // Accept WEB-B
        LoggerUtility.info("Step 53: Opening and accepting WEB-B shipment: " + shipmentRefB);
        miraklOrderDetailPage.clickOrderInList(shipmentRefB);

        String statusB = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-B status before acceptance: " + statusB);
        Assert.assertEquals(statusB, "Pending acceptance",
            TC_NAME + ": WEB-B should be 'Pending acceptance'. Actual: " + statusB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        statusB = miraklOrderDetailPage.getOrderStatus();
        Assert.assertEquals(statusB, "Awaiting shipment",
            TC_NAME + ": WEB-B should be 'Awaiting shipment' after accept. Actual: " + statusB);
        String urlShipmentB = driver.getCurrentUrl();
        LoggerUtility.info("Step 54-55: WEB-B accepted — URL: " + urlShipmentB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 56: Wait 2 minutes after both acceptances
        LoggerUtility.info("Step 56: Waiting 2 minutes after acceptance for orders to stabilize in Kibo...");
        Thread.sleep(120_000);

        // ============================================================
        // PHASE 3: KIBO API — Auth + Collect Shipment Details (2 shipments)
        // ============================================================

        LoggerUtility.info("Step 57-59: ========== Kibo Authentication ==========");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(),
            TC_NAME + ": Kibo access token should not be empty — authentication failed");
        LoggerUtility.info("Kibo authentication successful");

        String externalId = orderId + "WEB";
        LoggerUtility.info("Step 60-65: ========== Find Kibo Order ID ==========");
        LoggerUtility.info("Searching Kibo for externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(),
            TC_NAME + ": Kibo Order ID not found for externalId: " + externalId);
        LoggerUtility.info("Kibo Order ID: " + kiboOrderId);

        LoggerUtility.info("Step 66-69: ========== Polling Kibo Shipments (2 expected) ==========");
        LoggerUtility.info("Polling Kibo shipments (30s interval, up to 10 min)...");
        boolean allShipmentsReady = false;

        for (int attempt = 1; attempt <= 20; attempt++) {
            LoggerUtility.info(TC_NAME + ": Kibo shipment poll attempt " + attempt + "/20");
            Response shipmentsResp = ApiUtility.kiboGetShipmentsByOrderId(kiboToken, kiboOrderId);
            Assert.assertEquals(shipmentsResp.getStatusCode(), 200,
                TC_NAME + ": Kibo Get Shipments should return 200. Actual: " + shipmentsResp.getStatusCode());

            List<Map<String, Object>> shipmentList = shipmentsResp.jsonPath().getList("items");
            if (shipmentList == null || shipmentList.isEmpty()) {
                shipmentList = shipmentsResp.jsonPath().getList("_embedded.shipments");
            }

            if (shipmentList != null && shipmentList.size() >= 2) {
                List<ShipmentInfo> ready = new ArrayList<>();
                boolean allPopulated = true;

                for (Map<String, Object> s : shipmentList) {
                    ShipmentInfo info = new ShipmentInfo();
                    info.deliveryPartner = extractCustomField(s, "deliveryPartner");
                    info.tplShipmentId   = extractCustomField(s, "3pl_shipmentId");
                    info.carrierName     = extractCustomField(s, "carrierName");
                    info.trackingNumber  = extractCustomField(s, "tracking_number");

                    if (info.tplShipmentId.isEmpty() || info.deliveryPartner.isEmpty()) {
                        allPopulated = false;
                        LoggerUtility.info("Shipment 3PL data not ready — tplShipmentId='"
                            + info.tplShipmentId + "' deliveryPartner='" + info.deliveryPartner + "'");
                    } else {
                        boolean trackingOk = !info.deliveryPartner.toLowerCase().contains("envioclick")
                            || !info.trackingNumber.isEmpty();
                        if (!trackingOk) {
                            allPopulated = false;
                            LoggerUtility.info("Envioclick shipment missing trackingNumber — waiting...");
                        } else {
                            ready.add(info);
                        }
                    }
                }

                if (allPopulated && ready.size() >= 2) {
                    // Different-seller pattern: Seller B ships first → items[0] = WEB-B, items[1] = WEB-A
                    ready.get(0).shipmentRef     = shipmentRefB;
                    ready.get(0).miraklDetailUrl = urlShipmentB;
                    ready.get(1).shipmentRef     = shipmentRefA;
                    ready.get(1).miraklDetailUrl = urlShipmentA;
                    shipments.addAll(ready);
                    allShipmentsReady = true;
                    LoggerUtility.info(TC_NAME + ": All " + shipments.size() + " shipments have 3PL data — attempt " + attempt);
                    break;
                }
            } else {
                LoggerUtility.info(TC_NAME + ": Kibo returned fewer than 2 shipments — waiting 30s");
            }
            if (attempt < 20) Thread.sleep(30_000);
        }

        Assert.assertTrue(allShipmentsReady,
            TC_NAME + ": Not all 2 shipment 3PL data populated in Kibo within 10 minutes");
        Assert.assertEquals(shipments.size(), 2,
            TC_NAME + ": Expected exactly 2 shipments from Kibo");

        for (int i = 0; i < shipments.size(); i++) {
            ShipmentInfo info = shipments.get(i);
            LoggerUtility.info("--- Shipment " + (i + 1) + " [" + info.shipmentRef + "] ---");
            LoggerUtility.info("  Delivery Partner : " + info.deliveryPartner);
            LoggerUtility.info("  3PL Shipment ID  : " + info.tplShipmentId);
            LoggerUtility.info("  Carrier          : " + info.carrierName);
            LoggerUtility.info("  Tracking Number  : " + info.trackingNumber);
            Assert.assertFalse(info.deliveryPartner.isEmpty(),
                TC_NAME + ": deliveryPartner missing for shipment " + (i + 1));
            Assert.assertFalse(info.tplShipmentId.isEmpty(),
                TC_NAME + ": 3pl_shipmentId missing for shipment " + (i + 1));
            Assert.assertFalse(info.carrierName.isEmpty(),
                TC_NAME + ": carrierName missing for shipment " + (i + 1));
            if (info.deliveryPartner.toLowerCase().contains("envioclick")) {
                Assert.assertFalse(info.trackingNumber.isEmpty(),
                    TC_NAME + ": tracking_number required for Envioclick but missing for shipment " + (i + 1));
            }
        }
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: Carrier Webhook + Status Validation (per shipment)
        // ============================================================

        // Steps 70-81: Process each shipment independently
        for (ShipmentInfo info : shipments) {
            LoggerUtility.info("Step 70: ========== Processing Shipment: " + info.shipmentRef + " ==========");
            LoggerUtility.info("Delivery Partner: " + info.deliveryPartner);

            switchToMiraklTab();
            driver.get(info.miraklDetailUrl);
            WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);

            if (info.deliveryPartner.toLowerCase().contains("envioclick")) {
                runEnvioclickFlow(info);
            } else if (info.deliveryPartner.toLowerCase().contains("skydropx")) {
                runSkydropxFlow(info);
            } else {
                Assert.fail(TC_NAME + ": Unknown delivery partner for " + info.shipmentRef
                    + ": " + info.deliveryPartner + ". Expected 'envioclick' or 'skydropx'.");
            }
        }

        LoggerUtility.info(TC_NAME + " execution completed successfully — both shipments Received");
    }

    // ----------------------------------------------------------------
    // Envioclick flow for a single shipment
    // ----------------------------------------------------------------
    private void runEnvioclickFlow(ShipmentInfo info) {
        LoggerUtility.info("Step 70: Calling Envioclick API — En tránsito for: " + info.shipmentRef);
        LoggerUtility.info("EnvioClick Request — carrier: " + info.carrierName
            + " | idOrder (3pl_shipmentId): " + info.tplShipmentId
            + " | trackingCode: " + info.trackingNumber
            + " | myShipmentReference: " + orderId + "WEB");
        Response enTransito = ApiUtility.postEnvioclickEnTransito(
            info.tplShipmentId, info.trackingNumber, orderId, info.carrierName);
        Assert.assertEquals(enTransito.getStatusCode(), 200,
            TC_NAME + ": Envioclick En tránsito should return 200 for " + info.shipmentRef
            + ". Actual: " + enTransito.getStatusCode());
        LoggerUtility.info("EnvioClick En tránsito API Response = " + enTransito.getStatusCode());

        LoggerUtility.info("Step 71: Checking Mirakl for Shipped/3PL delivery status — " + info.shipmentRef);
        String status = waitForMiraklStatusOneOf(new String[]{"Shipped", "3PL delivery"}, 20);
        LoggerUtility.info("Mirakl status after En tránsito [" + info.shipmentRef + "]: " + status);
        Assert.assertTrue(status.equals("Shipped") || status.equals("3PL delivery"),
            TC_NAME + ": Mirakl status should be 'Shipped' or '3PL delivery' after En tránsito ["
            + info.shipmentRef + "]. Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 72: Calling Envioclick API — Entregado for: " + info.shipmentRef);
        Response entregado = ApiUtility.postEnvioclickEntregado(
            info.tplShipmentId, info.trackingNumber, orderId, info.carrierName);
        Assert.assertEquals(entregado.getStatusCode(), 200,
            TC_NAME + ": Envioclick Entregado should return 200 for " + info.shipmentRef
            + ". Actual: " + entregado.getStatusCode());
        LoggerUtility.info("EnvioClick Entregado API Response = " + entregado.getStatusCode());

        LoggerUtility.info("Step 73: Checking Mirakl for Received status — " + info.shipmentRef);
        status = waitForMiraklStatus("Received", 20);
        LoggerUtility.info("Mirakl status after Entregado [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Received",
            TC_NAME + ": Mirakl status should be 'Received' after Entregado ["
            + info.shipmentRef + "]. Order: " + orderId + ". Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        LoggerUtility.info("Envioclick flow complete — " + info.shipmentRef + " = Received");
    }

    // ----------------------------------------------------------------
    // Skydropx flow for a single shipment
    // ----------------------------------------------------------------
    private void runSkydropxFlow(ShipmentInfo info) {
        LoggerUtility.info("Step 74: Calling Skydropx API — Picked_up for: " + info.shipmentRef);
        LoggerUtility.info("Skydropx Request — id (3pl_shipmentId): " + info.tplShipmentId);
        Response pickedUp = ApiUtility.postSkydropxPickedUp(info.tplShipmentId);
        Assert.assertEquals(pickedUp.getStatusCode(), 200,
            TC_NAME + ": Skydropx Picked_up should return 200 for " + info.shipmentRef
            + ". Actual: " + pickedUp.getStatusCode());
        LoggerUtility.info("Skydropx Picked_up API Response = " + pickedUp.getStatusCode());

        LoggerUtility.info("Step 75: Checking Mirakl for Shipped status — " + info.shipmentRef);
        String status = waitForMiraklStatus("Shipped", 20);
        LoggerUtility.info("Mirakl status after Picked_up [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Shipped",
            TC_NAME + ": Mirakl status should be 'Shipped' after Skydropx Picked_up ["
            + info.shipmentRef + "]. Order: " + orderId + ". Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 76: Calling Skydropx API — Delivered for: " + info.shipmentRef);
        Response delivered = ApiUtility.postSkydropxDelivered(info.tplShipmentId);
        Assert.assertEquals(delivered.getStatusCode(), 200,
            TC_NAME + ": Skydropx Delivered should return 200 for " + info.shipmentRef
            + ". Actual: " + delivered.getStatusCode());
        LoggerUtility.info("Skydropx Delivered API Response = " + delivered.getStatusCode());

        LoggerUtility.info("Step 77: Checking Mirakl for Received status — " + info.shipmentRef);
        status = waitForMiraklStatus("Received", 20);
        LoggerUtility.info("Mirakl status after Delivered [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Received",
            TC_NAME + ": Mirakl status should be 'Received' after Skydropx Delivered ["
            + info.shipmentRef + "]. Order: " + orderId + ". Actual: " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        LoggerUtility.info("Skydropx flow complete — " + info.shipmentRef + " = Received");
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
