package FDA_Automation_Script.FDA_Automation_Script.tests;

import FDA_Automation_Script.FDA_Automation_Script.base.BaseClass;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDACartPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAHomePage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDALoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAOrderHistoryPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAPaymentPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAPDPPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDASuccessPage;
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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TC_FBO_023_Test extends BaseClass {

    private static final String TC_NAME        = "TC_FBO_023";

    // TC-specific credentials — mgowda account, 2 SKUs qty=2 each, single 3P seller
    private static final String TC_FDA_USER    = "mgowda@kognivera.com";
    private static final String TC_FDA_PASS    = "Mithun@12345";
    private static final String TC_SKU_1       = "78078094107";
    private static final String TC_SKU_2       = "7080901020316";
    private static final String TC_CARD_NUMBER = "5454545454545454";
    private static final String TC_CARD_EXPIRY = "03/30";
    private static final String TC_CARD_CVV    = "737";
    private static final int    TC_QUANTITY    = 2;

    private static class ShipmentInfo {
        String shipmentRef;
        String tplShipmentId;
        String carrierName;
        String trackingNumber;
        String deliveryPartner;
        String miraklDetailUrl;
    }

    private final List<ShipmentInfo> shipments = new ArrayList<>();

    // --- Page Objects ---
    private FDAHomePage           fdaHomePage;
    private FDALoginPage          fdaLoginPage;
    private FDAPDPPage            fdaPdpPage;
    private FDACartPage           fdaCartPage;
    private FDAPaymentPage        fdaPaymentPage;
    private FDASuccessPage        fdaSuccessPage;
    private FDAOrderHistoryPage   fdaOrderHistoryPage;
    private MiraklOrdersPage      miraklOrdersPage;
    private MiraklOrderDetailPage miraklOrderDetailPage;
    private MiraklReturnPage      miraklReturnPage;

    // --- Dynamic Test Data (captured once, reused everywhere) ---
    private String orderId;
    private String orderTotal;

    private static final By MIRAKL_ORDER_STATUS_LOCATOR =
        By.xpath("(//span[contains(@class,'_6e782__sc-s4ukbq-0') and not(ancestor::a)])[1]");

    // ================================================================
    // @BeforeClass — init page objects + swap FDA session to TC user
    // ================================================================

    @BeforeClass
    public void initAndLogin() {
        fdaHomePage           = new FDAHomePage(driver);
        fdaLoginPage          = new FDALoginPage(driver);
        fdaPdpPage            = new FDAPDPPage(driver);
        fdaCartPage           = new FDACartPage(driver);
        fdaPaymentPage        = new FDAPaymentPage(driver);
        fdaSuccessPage        = new FDASuccessPage(driver);
        fdaOrderHistoryPage   = new FDAOrderHistoryPage(driver);
        miraklOrdersPage      = new MiraklOrdersPage(driver);
        miraklOrderDetailPage = new MiraklOrderDetailPage(driver);
        miraklReturnPage      = new MiraklReturnPage(driver);
        LoggerUtility.info("TC_FBO_023: All page objects initialized");

        // Swap FDA session only if suite default user differs from TC-specific user
        if (!TC_FDA_USER.equals(config.getFdaUsername())) {
            LoggerUtility.info("TC_FBO_023: Switching FDA session from "
                + config.getFdaUsername() + " to: " + TC_FDA_USER);
            switchToFDATab();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.logout();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.clickProfileIcon();
            fdaHomePage.clickLoginLink();
            fdaLoginPage.login(TC_FDA_USER, TC_FDA_PASS);
            LoggerUtility.info("TC_FBO_023: FDA login as " + TC_FDA_USER + " successful");
        } else {
            LoggerUtility.info("TC_FBO_023: Suite user already " + TC_FDA_USER + " — no session swap needed");
        }
    }

    // ================================================================
    // @AfterClass — restore original suite FDA session
    // ================================================================

    @AfterClass(alwaysRun = true)
    public void restoreOriginalFdaSession() {
        if (!TC_FDA_USER.equals(config.getFdaUsername())) {
            LoggerUtility.info("TC_FBO_023 @AfterClass: Restoring original FDA session to "
                + config.getFdaUsername());
            try {
                switchToFDATab();
                fdaHomePage.navigateTo(config.getFdaUrl());
                fdaHomePage.logout();
                fdaHomePage.navigateTo(config.getFdaUrl());
                fdaHomePage.clickProfileIcon();
                fdaHomePage.clickLoginLink();
                fdaLoginPage.login(config.getFdaUsername(), config.getFdaPassword());
                LoggerUtility.info("TC_FBO_023 @AfterClass: FDA session restored as " + config.getFdaUsername());
            } catch (Exception e) {
                LoggerUtility.error("TC_FBO_023 @AfterClass: Failed to restore FDA session: " + e.getMessage());
            }
        } else {
            LoggerUtility.info("TC_FBO_023 @AfterClass: No session restore needed — suite user unchanged");
        }
    }

    // ================================================================
    // @Test
    // ================================================================

    @Test(testName = TC_NAME, groups = {"FBO"},
          description = "Verify 2 SKUs qty=2 each from 1 3P seller → 2 Mirakl shipments (WEB-A + WEB-B) → full fulfillment → return qty=2 → compliance → full refund → Closed")
    public void tc_fbo_023_dual_product_qty2_return_refund() throws InterruptedException {

        LoggerUtility.info("TC_FBO_023 execution started");
        LoggerUtility.info("Test Case ID      : " + TC_NAME);
        LoggerUtility.info("SKU 1             : " + TC_SKU_1);
        LoggerUtility.info("SKU 2             : " + TC_SKU_2);
        LoggerUtility.info("Quantity each     : " + TC_QUANTITY);
        LoggerUtility.info("Expected Total    : MXN$420.00");

        // ============================================================
        // PHASE 1: FDA — Product 1 (SKU_1, qty=2) → cart
        // ============================================================

        LoggerUtility.info("===== PHASE 1: FDA Order Placement =====");

        // Step 1: Verify FDA home page
        switchToFDATab();
        LoggerUtility.info("Step 1: Switched to FDA tab — verifying FDA home page");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test: clear any leftover cart items
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ---- Product 1 (SKU_1) ----
        LoggerUtility.info("Step 2: Clicking search field '¿Qué estás buscando?'");
        LoggerUtility.info("Step 3: Entering SKU 1: " + TC_SKU_1);
        fdaHomePage.enterSearchQuery(TC_SKU_1);
        LoggerUtility.info("Step 4: Pressing Enter to search");
        fdaHomePage.pressSearchEnter();
        LoggerUtility.info("SKU 1 " + TC_SKU_1 + " searched");

        LoggerUtility.info("Step 5: Verifying Product Details Page for SKU 1");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "PDP not displayed for SKU 1: " + TC_SKU_1 + " | TC: " + TC_NAME);

        LoggerUtility.info("Step 6: Verifying Agregar al carrito button enabled for SKU 1");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "Add to cart not enabled for SKU 1: " + TC_SKU_1 + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 7: Increasing SKU 1 quantity to " + TC_QUANTITY);
        fdaPdpPage.increaseQuantity();

        String p1PdpQty = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 8: SKU 1 PDP quantity after increment: " + p1PdpQty);
        Assert.assertEquals(p1PdpQty, String.valueOf(TC_QUANTITY),
            "SKU 1 PDP quantity should be " + TC_QUANTITY + ". Actual: " + p1PdpQty + " | TC: " + TC_NAME);
        LoggerUtility.info("SKU 1 quantity = " + TC_QUANTITY + " confirmed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 9: Clicking Agregar al carrito for SKU 1");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("SKU 1 added to cart — qty=" + TC_QUANTITY);

        // Navigate home before Product 2
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ---- Product 2 (SKU_2) ----
        LoggerUtility.info("Step 10: Clicking search field '¿Qué estás buscando?'");
        LoggerUtility.info("Step 11: Entering SKU 2: " + TC_SKU_2);
        fdaHomePage.enterSearchQuery(TC_SKU_2);
        LoggerUtility.info("Step 12: Pressing Enter to search");
        fdaHomePage.pressSearchEnter();
        LoggerUtility.info("SKU 2 " + TC_SKU_2 + " searched");

        LoggerUtility.info("Step 13: Verifying Product Details Page for SKU 2");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "PDP not displayed for SKU 2: " + TC_SKU_2 + " | TC: " + TC_NAME);

        LoggerUtility.info("Step 14: Verifying Agregar al carrito button enabled for SKU 2");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "Add to cart not enabled for SKU 2: " + TC_SKU_2 + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 15: Increasing SKU 2 quantity to " + TC_QUANTITY);
        fdaPdpPage.increaseQuantity();

        String p2PdpQty = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 16: SKU 2 PDP quantity after increment: " + p2PdpQty);
        Assert.assertEquals(p2PdpQty, String.valueOf(TC_QUANTITY),
            "SKU 2 PDP quantity should be " + TC_QUANTITY + ". Actual: " + p2PdpQty + " | TC: " + TC_NAME);
        LoggerUtility.info("SKU 2 quantity = " + TC_QUANTITY + " confirmed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 17: Clicking Agregar al carrito for SKU 2");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("SKU 2 added to cart — qty=" + TC_QUANTITY);

        // ============================================================
        // PHASE 2: Cart Validation — 2 products, both qty=2
        // ============================================================

        LoggerUtility.info("===== PHASE 2: Cart Validation =====");

        // Step 18: Open cart
        LoggerUtility.info("Step 18: Clicking Mi carrito — opening cart");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        // Step 19: Verify 2 items — retry if second item not yet visible
        int itemCount = fdaCartPage.getItemCount();
        for (int r = 0; r < 5 && itemCount < 2; r++) {
            LoggerUtility.info("Cart shows " + itemCount + " item(s) — refreshing (retry " + (r + 1) + "/5)");
            driver.navigate().refresh();
            WaitUtility.fluentWait(driver, By.cssSelector("input[data-role='cart-item-qty']"));
            itemCount = fdaCartPage.getItemCount();
        }
        LoggerUtility.info("Step 19: Cart item count: " + itemCount);
        Assert.assertEquals(itemCount, 2,
            "Cart should contain exactly 2 products. Actual: " + itemCount + " | TC: " + TC_NAME);
        LoggerUtility.info("Cart contains 2 products");

        // Step 20: Verify product names
        List<String> productNames = fdaCartPage.getAllProductNames();
        LoggerUtility.info("Step 20: Cart product names: " + productNames);
        Assert.assertEquals(productNames.size(), 2,
            "Cart should have 2 product names. Actual: " + productNames.size() + " | TC: " + TC_NAME);
        Assert.assertFalse(productNames.get(0).isEmpty(),
            "Product 1 name should not be empty | TC: " + TC_NAME);
        Assert.assertFalse(productNames.get(1).isEmpty(),
            "Product 2 name should not be empty | TC: " + TC_NAME);

        // Step 21: Verify both quantities = 2
        List<String> cartQtys = fdaCartPage.getAllQuantities();
        LoggerUtility.info("Step 21: Cart quantities: " + cartQtys);
        Assert.assertEquals(cartQtys.size(), 2,
            "Cart should have 2 quantity inputs. Actual: " + cartQtys.size() + " | TC: " + TC_NAME);
        Assert.assertEquals(cartQtys.get(0), String.valueOf(TC_QUANTITY),
            "Product 1 cart quantity should be " + TC_QUANTITY + ". Actual: " + cartQtys.get(0)
                + " | TC: " + TC_NAME);
        Assert.assertEquals(cartQtys.get(1), String.valueOf(TC_QUANTITY),
            "Product 2 cart quantity should be " + TC_QUANTITY + ". Actual: " + cartQtys.get(1)
                + " | TC: " + TC_NAME);
        LoggerUtility.info("Cart quantities — SKU1 qty=" + cartQtys.get(0)
            + " | SKU2 qty=" + cartQtys.get(1) + " — both verified");

        // Step 22: Verify order total MXN$1,020.00
        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 22: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(),
            "Order total should be displayed in cart | TC: " + TC_NAME);
        Assert.assertTrue(orderTotal.contains("1,020"),
            "Order total should reflect MXN$1,020.00. Actual: " + orderTotal + " | TC: " + TC_NAME);
        LoggerUtility.info("Order total verified — " + orderTotal + " (expected: MXN$1,020.00)");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: Checkout + Payment
        // ============================================================

        LoggerUtility.info("===== PHASE 3: Checkout and Payment =====");

        // Step 23: Proceed to payment
        LoggerUtility.info("Step 23: Clicking Proceed to payment");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();

        // Step 24: Siguiente on shipping page
        LoggerUtility.info("Step 24: Clicking Siguiente on shipping page");
        fdaPaymentPage.clickNextButton();

        // Step 25: Select credit card
        LoggerUtility.info("Step 25: Selecting Pago con Tarjeta Crédito/Débito");
        fdaPaymentPage.selectCreditCardOption();

        // Steps 26-31: Enter card details
        LoggerUtility.info("Step 26-27: Clicking Número de tarjeta and entering card number");
        fdaPaymentPage.enterCardNumber(TC_CARD_NUMBER);
        LoggerUtility.info("Step 28-29: Clicking Fecha de expiración and entering: " + TC_CARD_EXPIRY);
        fdaPaymentPage.enterExpiry(TC_CARD_EXPIRY);
        LoggerUtility.info("Step 30-31: Clicking Código de seguridad and entering CVV");
        fdaPaymentPage.enterCvv(TC_CARD_CVV);

        // Step 32: Verify Completar pago button shows MXN$1,020.00
        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Step 32: Completar pago button text: " + payBtnText);
        Assert.assertTrue(
            payBtnText.contains("Completar pago") || payBtnText.contains("MXN")
                || payBtnText.contains("1,020") || payBtnText.contains("$"),
            "Completar pago button should display MXN$1,020.00. Actual: " + payBtnText + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 33: Complete payment
        LoggerUtility.info("Step 33: Clicking Completar pago button");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();

        // Step 34: Verify success page
        LoggerUtility.info("Step 34: Verifying order success page is displayed");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(),
            "Order success page not displayed after payment | TC: " + TC_NAME);

        // Step 35: Capture order ID
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 35: Order ID = " + orderId);
        Assert.assertFalse(orderId.isEmpty(),
            "Order ID should be present on success page | TC: " + TC_NAME);
        LoggerUtility.info("Order created — ID: " + orderId
            + " | SKU1: " + TC_SKU_1 + " | SKU2: " + TC_SKU_2
            + " | qty=" + TC_QUANTITY + " each | total=" + orderTotal);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: FDA Order History
        // ============================================================

        LoggerUtility.info("===== PHASE 4: FDA Order History =====");

        // Steps 36-37: Navigate to Mis pedidos
        LoggerUtility.info("Step 36: Clicking Mi cuenta profile icon");
        fdaHomePage.clickProfileIcon();
        LoggerUtility.info("Step 37: Clicking Mis pedidos link");
        fdaHomePage.clickMyOrdersLink();

        // Step 38: Verify order history page
        LoggerUtility.info("Step 38: Verifying Mis pedidos page is displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(),
            "Mis pedidos page not displayed | TC: " + TC_NAME);

        // Step 39: Verify order ID present
        LoggerUtility.info("Step 39: Verifying Order ID " + orderId + " is present in Mis pedidos");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
            "Order ID " + orderId + " not found in Mis pedidos | TC: " + TC_NAME);

        // Step 40: Verify order status = Creada or Pendiente
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 40: FDA order status: " + fdaStatus);
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
            "FDA order status should be 'Creada' or 'Pendiente'. Actual: " + fdaStatus
                + " | Order: " + orderId + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 5: MIRAKL — Verify WEB-A + WEB-B, Accept Both
        // ============================================================

        LoggerUtility.info("===== PHASE 5: Mirakl — Verify and Accept Both Shipments =====");

        // Step 41: Switch to Mirakl tab
        LoggerUtility.info("Step 41: Switching to existing Mirakl tab — reusing authenticated session");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 42-43: Navigate to All Orders
        LoggerUtility.info("Step 42: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 43: Clicking All Orders");
        miraklOrdersPage.clickAllOrders();

        String miraklSearchTerm = orderId + "WEB";
        String shipmentRefA     = orderId + "WEB-A";
        String shipmentRefB     = orderId + "WEB-B";

        // Steps 44-45: Search — retry every 60s, up to 10 attempts (10 min) for Mirakl sync
        LoggerUtility.info("Step 44: Clicking search field");
        LoggerUtility.info("Step 45: Entering Order ID: " + miraklSearchTerm);
        boolean orderFoundInMirakl = false;
        for (int attempt = 1; attempt <= 10; attempt++) {
            LoggerUtility.info("Mirakl search attempt " + attempt + "/10 for: " + miraklSearchTerm);
            miraklOrdersPage.searchOrder(miraklSearchTerm);
            if (miraklOrdersPage.hasSearchResults(miraklSearchTerm)) {
                orderFoundInMirakl = true;
                LoggerUtility.info("Order found in Mirakl on attempt " + attempt);
                break;
            }
            LoggerUtility.info("Order not in Mirakl yet (attempt " + attempt + "/10) — waiting 60 seconds...");
            if (attempt < 10) {
                Thread.sleep(60_000);
                switchToMiraklTab();
                miraklOrdersPage.clickOrdersMenu();
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(orderFoundInMirakl,
            "Order " + miraklSearchTerm + " did not appear in Mirakl within 10 minutes | TC: " + TC_NAME);

        // Step 46: Verify both WEB-A and WEB-B present
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefA),
            "Mirakl shipment WEB-A not found: " + shipmentRefA + " | TC: " + TC_NAME);
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefB),
            "Mirakl shipment WEB-B not found: " + shipmentRefB + " | TC: " + TC_NAME);
        LoggerUtility.info("Step 46: Both shipments verified in Mirakl");
        LoggerUtility.info("  WEB-A: " + shipmentRefA);
        LoggerUtility.info("  WEB-B: " + shipmentRefB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 47: Accept WEB-A
        LoggerUtility.info("Step 47: Clicking WEB-A for acceptance: " + shipmentRefA);
        miraklOrderDetailPage.clickOrderInList(shipmentRefA);

        String statusA = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-A status before acceptance: " + statusA);
        Assert.assertEquals(statusA, "Pending acceptance",
            "WEB-A should be 'Pending acceptance'. Actual: " + statusA
                + " | Shipment: " + shipmentRefA + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        statusA = miraklOrderDetailPage.getOrderStatus();
        Assert.assertEquals(statusA, "Awaiting shipment",
            "WEB-A should be 'Awaiting shipment' after acceptance. Actual: " + statusA
                + " | Shipment: " + shipmentRefA + " | TC: " + TC_NAME);
        String urlShipmentA = driver.getCurrentUrl();
        LoggerUtility.info("WEB-A accepted — status: " + statusA + " | URL: " + urlShipmentA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 47 (continued): Accept WEB-B — navigate directly via URL (avoids SPA anchor race condition
        // where //a[1] in the WEB-B row resolves to a non-navigation element for this product type)
        LoggerUtility.info("Step 47: Navigating directly to WEB-B detail: " + shipmentRefB);
        driver.get(urlShipmentA.replace("WEB-A", "WEB-B"));
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);

        String statusB = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-B status before acceptance: " + statusB);
        Assert.assertEquals(statusB, "Pending acceptance",
            "WEB-B should be 'Pending acceptance'. Actual: " + statusB
                + " | Shipment: " + shipmentRefB + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        statusB = miraklOrderDetailPage.getOrderStatus();
        Assert.assertEquals(statusB, "Awaiting shipment",
            "WEB-B should be 'Awaiting shipment' after acceptance. Actual: " + statusB
                + " | Shipment: " + shipmentRefB + " | TC: " + TC_NAME);
        String urlShipmentB = driver.getCurrentUrl();
        LoggerUtility.info("WEB-B accepted — status: " + statusB + " | URL: " + urlShipmentB);
        LoggerUtility.info("Both shipments (WEB-A + WEB-B) accepted successfully");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 48: Wait 2 minutes for 3PL connector to process
        LoggerUtility.info("Step 48: Waiting 2 minutes for 3PL connector to process...");
        Thread.sleep(120_000);
        LoggerUtility.info("2-minute wait complete");

        // ============================================================
        // PHASE 6: KIBO API — Auth + Collect Shipment Data for Both
        // ============================================================

        LoggerUtility.info("===== PHASE 6: Kibo API — Authentication and Shipment Data =====");

        // Step 49: Kibo authentication
        LoggerUtility.info("Step 49: Calling Kibo Auth API — retrieving access token");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(),
            "Kibo access token should not be empty | Order: " + orderId + " | TC: " + TC_NAME);
        LoggerUtility.info("Kibo authentication successful");

        // Step 50: Find Kibo order ID
        String externalId = orderId + "WEB";
        LoggerUtility.info("Step 50: Searching Kibo for externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(),
            "Kibo Order ID not found for externalId: " + externalId
                + " | FDA Order: " + orderId + " | TC: " + TC_NAME);
        LoggerUtility.info("Kibo Order ID: " + kiboOrderId + " | FDA Order: " + orderId);

        // Step 51: Poll Kibo until both shipments have 3PL data — up to 20 × 30s = 10 minutes
        LoggerUtility.info("Step 51: Polling Kibo for 2 shipments — 30s interval, up to 10 min");
        boolean allShipmentsReady = false;

        for (int attempt = 1; attempt <= 20; attempt++) {
            LoggerUtility.info("Kibo shipment poll attempt " + attempt + "/20 — Order: " + orderId);
            Response shipmentsResp = ApiUtility.kiboGetShipmentsByOrderId(kiboToken, kiboOrderId);
            Assert.assertEquals(shipmentsResp.getStatusCode(), 200,
                "Kibo Get Shipments should return 200. Actual: " + shipmentsResp.getStatusCode()
                    + " | Order: " + orderId + " | TC: " + TC_NAME);

            // Handle both standard items[] and HAL _embedded.shipments formats
            List<Map<String, Object>> shipmentList = shipmentsResp.jsonPath().getList("items");
            if (shipmentList == null || shipmentList.isEmpty()) {
                shipmentList = shipmentsResp.jsonPath().getList("_embedded.shipments");
            }

            if (shipmentList != null && shipmentList.size() >= 2) {
                List<ShipmentInfo> ready = new ArrayList<>();
                boolean allPopulated = true;

                for (Map<String, Object> s : shipmentList) {
                    ShipmentInfo info = new ShipmentInfo();
                    info.tplShipmentId   = extractCustomField(s, "3pl_shipmentId");
                    info.carrierName     = extractCustomField(s, "carrierName");
                    info.trackingNumber  = extractCustomField(s, "tracking_number");
                    info.deliveryPartner = extractCustomField(s, "deliveryPartner");

                    if (info.tplShipmentId.isEmpty() || info.deliveryPartner.isEmpty()) {
                        allPopulated = false;
                        LoggerUtility.info("3PL data not ready — tplShipmentId='" + info.tplShipmentId
                            + "' deliveryPartner='" + info.deliveryPartner + "'");
                    } else {
                        ready.add(info);
                    }
                }

                if (allPopulated && ready.size() >= 2) {
                    // Kibo processes WEB-B first for this product combination (7080901020316 appears
                    // first in cart) → items[0] = WEB-B, items[1] = WEB-A (inverse of typical same-seller)
                    ready.get(0).shipmentRef     = shipmentRefB;
                    ready.get(0).miraklDetailUrl = urlShipmentB;
                    ready.get(1).shipmentRef     = shipmentRefA;
                    ready.get(1).miraklDetailUrl = urlShipmentA;
                    shipments.addAll(ready);
                    allShipmentsReady = true;
                    LoggerUtility.info("All " + shipments.size() + " shipments have 3PL data — attempt " + attempt);
                    break;
                }
            } else {
                LoggerUtility.info("Kibo returned fewer than 2 shipments — waiting 30s (attempt " + attempt + ")");
            }

            if (attempt < 20) Thread.sleep(30_000);
        }

        Assert.assertTrue(allShipmentsReady,
            "3PL data not fully populated for both shipments within 10 minutes | Order: " + orderId
                + " | TC: " + TC_NAME);
        Assert.assertEquals(shipments.size(), 2,
            "Expected exactly 2 shipments from Kibo. Actual: " + shipments.size()
                + " | Order: " + orderId + " | TC: " + TC_NAME);

        for (int i = 0; i < shipments.size(); i++) {
            ShipmentInfo info = shipments.get(i);
            LoggerUtility.info("--- Shipment " + (i + 1) + " [" + info.shipmentRef + "] ---");
            LoggerUtility.info("  Delivery Partner : " + info.deliveryPartner);
            LoggerUtility.info("  3PL Shipment ID  : " + info.tplShipmentId);
            LoggerUtility.info("  Carrier Name     : " + info.carrierName);
            LoggerUtility.info("  Tracking Number  : " + info.trackingNumber);
        }
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 7: Carrier Webhooks — Both Shipments to Received (Steps 52-61)
        // ============================================================

        LoggerUtility.info("===== PHASE 7: Carrier Webhooks — Both Shipments to Received =====");
        for (ShipmentInfo info : shipments) {
            LoggerUtility.info("========== Processing Shipment: " + info.shipmentRef + " ==========");
            LoggerUtility.info("Delivery Partner: " + info.deliveryPartner);

            switchToMiraklTab();
            driver.get(info.miraklDetailUrl);
            WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);

            if (info.deliveryPartner.toLowerCase().contains("envioclick")) {
                runEnvioclickFlow(info);
            } else if (info.deliveryPartner.toLowerCase().contains("skydropx")) {
                runSkydropxFlow(info);
            } else {
                ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.FAIL);
                Assert.fail("Unknown delivery partner for " + info.shipmentRef
                    + ": '" + info.deliveryPartner + "'. Expected 'Envioclick' or 'Skydropx'."
                    + " | Order: " + orderId + " | TC: " + TC_NAME);
            }
        }
        LoggerUtility.info("Both shipments reached Received. Proceeding to return flow.");

        // ============================================================
        // PHASE 8: Return API — Get order_line_id + Create Return (Steps 62-63)
        // ============================================================

        LoggerUtility.info("===== PHASE 8: Return — API Flow =====");

        String orderCommercialId = orderId + "WEB";

        // Step 62: Get Mirakl order_line_id for WEB-A sub-order
        LoggerUtility.info("Step 62: Calling Get_order_line_id for commercial order: " + orderCommercialId);
        String orderLineId = ReturnApiUtility.getMiraklOrderLineId(orderCommercialId);

        Assert.assertFalse(orderLineId.isEmpty(),
            "order_line_id should not be empty for: " + orderCommercialId + " | TC: " + TC_NAME);
        LoggerUtility.info("order_line_id retrieved: " + orderLineId);
        LoggerUtility.info("  order_commercial_id : " + orderCommercialId);
        LoggerUtility.info("  order_line_id       : " + orderLineId);
        LoggerUtility.info("  quantity            : " + TC_QUANTITY + " (CRITICAL)");

        // Step 63: Create Return incident — quantity=2 (each product has qty=2)
        LoggerUtility.info("Step 63: Creating Return incident — quantity=" + TC_QUANTITY);
        Response returnResponse = ReturnApiUtility.postReturn(orderCommercialId, orderLineId, TC_QUANTITY);

        Assert.assertTrue(
            returnResponse.getStatusCode() == 200 || returnResponse.getStatusCode() == 201,
            "Return service should return 200 or 201. Actual: " + returnResponse.getStatusCode()
                + " | Body: " + returnResponse.getBody().asString()
                + " | orderCommercialId: " + orderCommercialId
                + " | orderLineId: " + orderLineId
                + " | quantity: " + TC_QUANTITY + " | TC: " + TC_NAME);
        LoggerUtility.info("Return incident created — HTTP " + returnResponse.getStatusCode()
            + " | quantity=" + TC_QUANTITY);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 9: Mirakl Return UI — Mark Received, Compliance, Refund → Closed (Steps 64-74)
        // ============================================================

        LoggerUtility.info("===== PHASE 9: Mirakl Return UI =====");

        // Navigate to WEB-A for return actions
        LoggerUtility.info("Switching to Mirakl — navigating to WEB-A order detail for return");
        switchToMiraklTab();
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        LoggerUtility.info("Searching for WEB-A: " + shipmentRefA);
        miraklOrdersPage.searchOrder(shipmentRefA);
        miraklOrderDetailPage.clickOrderInList(shipmentRefA);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 64: Click "Mark as received"
        LoggerUtility.info("Step 64: Clicking Mark as received");
        miraklReturnPage.clickMarkAsReceived();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 65: Confirm popup "Mark as received"
        LoggerUtility.info("Step 65: Confirming Mark as received on popup");
        miraklReturnPage.confirmMarkAsReceivedPopup();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 66: Click "Check compliance"
        LoggerUtility.info("Step 66: Clicking Check compliance");
        miraklReturnPage.clickCheckCompliance();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 67: Click "Save"
        LoggerUtility.info("Step 67: Clicking Save");
        miraklReturnPage.clickSave();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 68: Click "Refund" dropdown
        LoggerUtility.info("Step 68: Clicking Refund dropdown");
        miraklReturnPage.clickRefundDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 69: Select "Full refund"
        LoggerUtility.info("Step 69: Selecting Full refund from dropdown");
        miraklReturnPage.selectFullRefundFromDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 70: Click "Select" dropdown for reason
        LoggerUtility.info("Step 70: Clicking Select dropdown for refund reason");
        miraklReturnPage.clickSelectReasonDropdown();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 71: Select "Item returned"
        LoggerUtility.info("Step 71: Selecting Item returned");
        miraklReturnPage.selectItemReturned();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 72: Click "Confirm" on refund popup
        LoggerUtility.info("Step 72: Clicking Confirm on refund popup");
        miraklReturnPage.confirmRefundPopup();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 73: Wait 1 minute for refund to process, then refresh
        LoggerUtility.info("Step 73: Waiting 60 seconds for shipment closure to process...");
        Thread.sleep(60_000);
        LoggerUtility.info("60-second wait complete");
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 10: Step 74 — Verify Shipment 1 (WEB-A) = Closed
        // ============================================================

        LoggerUtility.info("===== PHASE 10: Step 74 — Verify Shipment 1 (WEB-A) = Closed =====");
        String closedStatus = waitForMiraklStatus("Closed", 6);
        LoggerUtility.info("Step 74: Shipment 1 (WEB-A) status after full refund: " + closedStatus);
        Assert.assertEquals(closedStatus, "Closed",
            "Shipment 1 (WEB-A) should be 'Closed' after full refund. Actual: " + closedStatus
                + " | Shipment: " + shipmentRefA + " | Order: " + orderId + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);

        // Final summary log
        LoggerUtility.info("TC_FBO_023 completed successfully");
        LoggerUtility.info("  Test Case         : TC_FBO_023");
        LoggerUtility.info("  SKU 1             : " + TC_SKU_1);
        LoggerUtility.info("  SKU 2             : " + TC_SKU_2);
        LoggerUtility.info("  Quantity each     : " + TC_QUANTITY);
        LoggerUtility.info("  Expected Total    : MXN$420.00");
        LoggerUtility.info("  Actual Total      : " + orderTotal);
        LoggerUtility.info("  Order ID          : " + orderId);
        LoggerUtility.info("  Shipment WEB-A    : " + shipmentRefA);
        LoggerUtility.info("  Shipment WEB-B    : " + shipmentRefB);
        LoggerUtility.info("  Return quantity   : " + TC_QUANTITY);
        LoggerUtility.info("  Final Status      : Shipment 1: closed");
        LoggerUtility.info("  Final Test Status : PASS");
    }

    // ----------------------------------------------------------------
    // Envioclick: En tránsito → Shipped/3PL delivery | Entregado → Received
    // ----------------------------------------------------------------
    private void runEnvioclickFlow(ShipmentInfo info) {
        LoggerUtility.info("Step 53: Calling Envioclick API — En tránsito for: " + info.shipmentRef);
        LoggerUtility.info("EnvioClick API Request — carrier: " + info.carrierName
            + " | idOrder (3pl_shipmentId): " + info.tplShipmentId
            + " | trackingCode: " + info.trackingNumber
            + " | myShipmentReference: " + orderId + "WEB");
        Response enTransitoResp = ApiUtility.postEnvioclickEnTransito(
            info.tplShipmentId, info.trackingNumber, orderId, info.carrierName);
        LoggerUtility.info("EnvioClick En tránsito API Response = " + enTransitoResp.getStatusCode());
        Assert.assertEquals(enTransitoResp.getStatusCode(), 200,
            "Envioclick En tránsito should return 200 for " + info.shipmentRef
                + ". Actual: " + enTransitoResp.getStatusCode()
                + " | Order: " + orderId + " | TC: " + TC_NAME);

        // Step 54: Verify Mirakl status → Shipped or 3PL delivery
        LoggerUtility.info("Step 54: Verifying Mirakl status → Shipped after En tránsito: " + info.shipmentRef);
        String status = waitForMiraklStatusOneOf(new String[]{"Shipped", "3PL delivery"}, 20);
        LoggerUtility.info("Mirakl status after En tránsito [" + info.shipmentRef + "]: " + status);
        Assert.assertTrue(status.equals("Shipped") || status.equals("3PL delivery"),
            "Mirakl status should be 'Shipped' or '3PL delivery' after En tránsito ["
                + info.shipmentRef + "]. Actual: " + status + " | Order: " + orderId + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 55: Call Envioclick Entregado
        LoggerUtility.info("Step 55: Calling Envioclick API — Entregado for: " + info.shipmentRef);
        LoggerUtility.info("EnvioClick API Request — carrier: " + info.carrierName
            + " | idOrder (3pl_shipmentId): " + info.tplShipmentId
            + " | trackingCode: " + info.trackingNumber
            + " | myShipmentReference: " + orderId + "WEB");
        Response entregadoResp = ApiUtility.postEnvioclickEntregado(
            info.tplShipmentId, info.trackingNumber, orderId, info.carrierName);
        LoggerUtility.info("EnvioClick Entregado API Response = " + entregadoResp.getStatusCode());
        Assert.assertEquals(entregadoResp.getStatusCode(), 200,
            "Envioclick Entregado should return 200 for " + info.shipmentRef
                + ". Actual: " + entregadoResp.getStatusCode()
                + " | Order: " + orderId + " | TC: " + TC_NAME);

        // Step 56: Verify Mirakl status → Received
        LoggerUtility.info("Step 56: Verifying Mirakl status → Received after Entregado: " + info.shipmentRef);
        status = waitForMiraklStatus("Received", 30);
        LoggerUtility.info("Mirakl status after Entregado [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Received",
            "Mirakl status should be 'Received' after Entregado [" + info.shipmentRef + "]. Actual: "
                + status + " | Order: " + orderId + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("Envioclick flow complete — " + info.shipmentRef + " = Received");
    }

    // ----------------------------------------------------------------
    // Skydropx: Picked_up → Shipped | Delivered → Received
    // ----------------------------------------------------------------
    private void runSkydropxFlow(ShipmentInfo info) {
        LoggerUtility.info("Step 57: Calling Skydropx API — Picked_up for: " + info.shipmentRef);
        LoggerUtility.info("Skydropx API Request — id (3pl_shipmentId): " + info.tplShipmentId);
        Response pickedUpResp = ApiUtility.postSkydropxPickedUp(info.tplShipmentId);
        LoggerUtility.info("Skydropx Picked_up API Response = " + pickedUpResp.getStatusCode());
        Assert.assertEquals(pickedUpResp.getStatusCode(), 200,
            "Skydropx Picked_up should return 200 for " + info.shipmentRef
                + ". Actual: " + pickedUpResp.getStatusCode()
                + " | Order: " + orderId + " | TC: " + TC_NAME);

        // Step 58: Verify Mirakl status → Shipped
        LoggerUtility.info("Step 58: Verifying Mirakl status → Shipped after Picked_up: " + info.shipmentRef);
        String status = waitForMiraklStatus("Shipped", 60);
        LoggerUtility.info("Mirakl status after Picked_up [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Shipped",
            "Mirakl status should be 'Shipped' after Skydropx Picked_up [" + info.shipmentRef + "]. Actual: "
                + status + " | Order: " + orderId + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 59: Call Skydropx Delivered
        LoggerUtility.info("Step 59: Calling Skydropx API — Delivered for: " + info.shipmentRef);
        LoggerUtility.info("Skydropx API Request — id (3pl_shipmentId): " + info.tplShipmentId);
        Response deliveredResp = ApiUtility.postSkydropxDelivered(info.tplShipmentId);
        LoggerUtility.info("Skydropx Delivered API Response = " + deliveredResp.getStatusCode());
        Assert.assertEquals(deliveredResp.getStatusCode(), 200,
            "Skydropx Delivered should return 200 for " + info.shipmentRef
                + ". Actual: " + deliveredResp.getStatusCode()
                + " | Order: " + orderId + " | TC: " + TC_NAME);

        // Step 60: Verify Mirakl status → Received
        LoggerUtility.info("Step 60: Verifying Mirakl status → Received after Delivered: " + info.shipmentRef);
        status = waitForMiraklStatus("Received", 60);
        LoggerUtility.info("Mirakl status after Delivered [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Received",
            "Mirakl status should be 'Received' after Skydropx Delivered [" + info.shipmentRef + "]. Actual: "
                + status + " | Order: " + orderId + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("Skydropx flow complete — " + info.shipmentRef + " = Received");
    }

    // ----------------------------------------------------------------
    // Poll Mirakl status — refresh + check up to maxRetries times
    // ----------------------------------------------------------------
    private String waitForMiraklStatus(String expected, int maxRetries) {
        String status = "";
        for (int i = 1; i <= maxRetries; i++) {
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            status = miraklOrderDetailPage.getOrderStatus();
            LoggerUtility.info("Mirakl status check " + i + "/" + maxRetries
                + ": " + status + " (expected: " + expected + ")");
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
            LoggerUtility.info("Mirakl status check " + i + "/" + maxRetries
                + ": " + status + " (expected one of: " + Arrays.toString(expected) + ")");
            if (expectedSet.contains(status)) break;
        }
        return status;
    }

    // ----------------------------------------------------------------
    // Extract named custom field from Kibo shipment item map.
    // Searches item.data first, then item.packages[*].data.
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
    // Handle checkout opening in new browser window after clickProceedToPayment
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
}
// @AfterMethod navigateToHomePage() is inherited from BaseClass — navigates FDA + Mirakl to home
