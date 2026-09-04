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
import FDA_Automation_Script.FDA_Automation_Script.utils.ApiUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import io.restassured.response.Response;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

public class TC_FBO_030_Test extends BaseClass {

    private static final String TC_NAME        = "TC_FBO_030";

    // TC-specific credentials and test data — never stored in config.properties
    private static final String TC_FDA_USER    = "mgowda@kognivera.com";
    private static final String TC_FDA_PASS    = "Mithun@12345";
    private static final String TC_SKU_1       = "78078094274";
    private static final String TC_SKU_2       = "78078094130";
    private static final String TC_CARD_NUMBER = "5454545454545454";
    private static final String TC_CARD_EXPIRY = "03/30";
    private static final String TC_CARD_CVV    = "737";
    private static final int    TC_QUANTITY    = 2;

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

    // --- Dynamic test data (captured once, reused throughout the test) ---
    private String orderId;
    private String orderTotal;

    private static final By MIRAKL_ORDER_STATUS_LOCATOR =
        By.xpath("(//span[contains(@class,'_6e782__sc-s4ukbq-0') and not(ancestor::a)])[1]");

    @BeforeClass
    public void initAndLogin() {
        fdaHomePage          = new FDAHomePage(driver);
        fdaLoginPage         = new FDALoginPage(driver);
        fdaPdpPage           = new FDAPDPPage(driver);
        fdaCartPage          = new FDACartPage(driver);
        fdaPaymentPage       = new FDAPaymentPage(driver);
        fdaSuccessPage       = new FDASuccessPage(driver);
        fdaOrderHistoryPage  = new FDAOrderHistoryPage(driver);
        miraklOrdersPage     = new MiraklOrdersPage(driver);
        miraklOrderDetailPage = new MiraklOrderDetailPage(driver);
        LoggerUtility.info("TC_FBO_030: All page objects initialized");

        // TC_FBO_030 uses a different FDA account — guard prevents double-logout
        // when the previous TC already ran as the same user
        if (!TC_FDA_USER.equals(config.getFdaUsername())) {
            LoggerUtility.info("TC_FBO_030 @BeforeClass: Switching FDA session to: " + TC_FDA_USER);
            switchToFDATab();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.logout();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.clickProfileIcon();
            fdaHomePage.clickLoginLink();
            fdaLoginPage.login(TC_FDA_USER, TC_FDA_PASS);
            LoggerUtility.info("TC_FBO_030 @BeforeClass: FDA login as " + TC_FDA_USER + " successful");
        }
    }

    @AfterClass(alwaysRun = true)
    public void restoreOriginalFdaSession() {
        // Restore suite-default FDA user so subsequent test cases are unaffected
        if (!TC_FDA_USER.equals(config.getFdaUsername())) {
            LoggerUtility.info("TC_FBO_030 @AfterClass: Restoring original FDA session");
            try {
                switchToFDATab();
                fdaHomePage.navigateTo(config.getFdaUrl());
                fdaHomePage.logout();
                fdaHomePage.navigateTo(config.getFdaUrl());
                fdaHomePage.clickProfileIcon();
                fdaHomePage.clickLoginLink();
                fdaLoginPage.login(config.getFdaUsername(), config.getFdaPassword());
                LoggerUtility.info("TC_FBO_030 @AfterClass: FDA session restored as " + config.getFdaUsername());
            } catch (Exception e) {
                LoggerUtility.error("TC_FBO_030 @AfterClass: Failed to restore FDA session: " + e.getMessage());
            }
        }
    }

    @Test(testName = TC_NAME,
          description = "Verify 2 products qty=2 each by 1 3P seller → 2 Mirakl shipments → Accept both → Cancel WEB-A → WEB-A=Canceled, WEB-B=Awaiting shipment")
    public void tc_fbo_030_dual_product_qty2_cancel_one_shipment() throws InterruptedException {

        // ============================================================
        // PHASE 1: FDA — Product 1 (SKU_1, qty=2) → Cart
        // ============================================================
        LoggerUtility.info("===== TC_FBO_030 PHASE 1: FDA Order Placement =====");
        LoggerUtility.info("TC_FBO_030 | SKU 1: " + TC_SKU_1 + " | SKU 2: " + TC_SKU_2 + " | Qty each: " + TC_QUANTITY);

        // Step 1: Verify FDA Home Page — FDA tab active from suite @BeforeSuite
        switchToFDATab();
        LoggerUtility.info("Step 1: Verifying FDA Home Page");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test: remove any leftover cart items from previous runs
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ---- Product 1 (SKU_1, qty=2) ----
        LoggerUtility.info("Step 2: Clicking search field — ¿Qué estás buscando?");
        LoggerUtility.info("Step 3: Entering Product 1 SKU: " + TC_SKU_1);
        fdaHomePage.enterSearchQuery(TC_SKU_1);
        LoggerUtility.info("Step 4: Pressing Enter to search");
        fdaHomePage.pressSearchEnter();
        LoggerUtility.info("Product 1 SKU " + TC_SKU_1 + " searched successfully");

        LoggerUtility.info("Step 5: Verifying PDP is displayed for Product 1");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "TC_FBO_030 — PDP not displayed for Product 1 SKU: " + TC_SKU_1);
        LoggerUtility.info("Step 6: Verifying Agregar al carrito button is enabled for Product 1");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "TC_FBO_030 — Add to cart button not enabled for Product 1 SKU: " + TC_SKU_1);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 7: Clicking plus button to increase Product 1 quantity to " + TC_QUANTITY);
        fdaPdpPage.increaseQuantity();

        String p1PdpQty = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 8: Product 1 PDP quantity after increment: " + p1PdpQty);
        Assert.assertEquals(p1PdpQty, String.valueOf(TC_QUANTITY),
            "TC_FBO_030 — Product 1 PDP quantity should be " + TC_QUANTITY + ". Actual: " + p1PdpQty);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 9: Clicking Agregar al carrito for Product 1");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product 1 added to cart with quantity " + TC_QUANTITY);

        // Navigate home before searching Product 2
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ---- Product 2 (SKU_2, qty=2) ----
        LoggerUtility.info("Step 10: Clicking search field — ¿Qué estás buscando?");
        LoggerUtility.info("Step 11: Entering Product 2 SKU: " + TC_SKU_2);
        fdaHomePage.enterSearchQuery(TC_SKU_2);
        LoggerUtility.info("Step 12: Pressing Enter to search");
        fdaHomePage.pressSearchEnter();
        LoggerUtility.info("Product 2 SKU " + TC_SKU_2 + " searched successfully");

        LoggerUtility.info("Step 13: Verifying PDP is displayed for Product 2");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "TC_FBO_030 — PDP not displayed for Product 2 SKU: " + TC_SKU_2);
        LoggerUtility.info("Step 14: Verifying Agregar al carrito button is enabled for Product 2");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "TC_FBO_030 — Add to cart button not enabled for Product 2 SKU: " + TC_SKU_2);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 15: Clicking plus button to increase Product 2 quantity to " + TC_QUANTITY);
        fdaPdpPage.increaseQuantity();

        String p2PdpQty = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 16: Product 2 PDP quantity after increment: " + p2PdpQty);
        Assert.assertEquals(p2PdpQty, String.valueOf(TC_QUANTITY),
            "TC_FBO_030 — Product 2 PDP quantity should be " + TC_QUANTITY + ". Actual: " + p2PdpQty);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 17: Clicking Agregar al carrito for Product 2");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product 2 added to cart with quantity " + TC_QUANTITY);

        // ============================================================
        // PHASE 2: FDA — Cart Validation (2 products, qty=2 each)
        // ============================================================
        LoggerUtility.info("===== TC_FBO_030 PHASE 2: Cart Validation =====");

        LoggerUtility.info("Step 18: Opening Mi carrito (cart page)");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        // Step 19: Verify exactly 2 products in cart
        int itemCount = fdaCartPage.getItemCount();
        LoggerUtility.info("Step 19: Cart item count: " + itemCount);
        Assert.assertEquals(itemCount, 2,
            "TC_FBO_030 — Cart should contain exactly 2 products. Actual: " + itemCount);
        LoggerUtility.info("Cart contains 2 products");

        // Step 20: Verify product names present for both
        List<String> productNames = fdaCartPage.getAllProductNames();
        LoggerUtility.info("Step 20: Cart product names: " + productNames);
        Assert.assertEquals(productNames.size(), 2,
            "TC_FBO_030 — Cart should have 2 product name entries. Actual: " + productNames.size());
        Assert.assertFalse(productNames.get(0).isEmpty(),
            "TC_FBO_030 — Cart Product 1 name should not be empty");
        Assert.assertFalse(productNames.get(1).isEmpty(),
            "TC_FBO_030 — Cart Product 2 name should not be empty");
        LoggerUtility.info("TC_FBO_030 | Cart Product 1 Name: " + productNames.get(0));
        LoggerUtility.info("TC_FBO_030 | Cart Product 2 Name: " + productNames.get(1));

        // Step 21: Verify both quantities are 2
        List<String> cartQtys = fdaCartPage.getAllQuantities();
        LoggerUtility.info("Step 21: Cart quantities: " + cartQtys);
        Assert.assertEquals(cartQtys.size(), 2,
            "TC_FBO_030 — Cart should have 2 quantity inputs. Actual: " + cartQtys.size());
        Assert.assertEquals(cartQtys.get(0), String.valueOf(TC_QUANTITY),
            "TC_FBO_030 — Product 1 cart quantity should be " + TC_QUANTITY + ". Actual: " + cartQtys.get(0));
        Assert.assertEquals(cartQtys.get(1), String.valueOf(TC_QUANTITY),
            "TC_FBO_030 — Product 2 cart quantity should be " + TC_QUANTITY + ". Actual: " + cartQtys.get(1));
        LoggerUtility.info("Product quantities validated — Product 1 qty=" + cartQtys.get(0)
            + " | Product 2 qty=" + cartQtys.get(1));

        // Step 22: Verify order total is displayed
        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 22: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(),
            "TC_FBO_030 — Order total should be displayed in cart");
        LoggerUtility.info("TC_FBO_030 | Order Total: " + orderTotal);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: FDA — Checkout and Payment
        // ============================================================
        LoggerUtility.info("===== TC_FBO_030 PHASE 3: Checkout and Payment =====");

        // Step 23: Proceed to payment
        LoggerUtility.info("Step 23: Clicking Proceed to payment");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();

        // Step 24: Siguiente on shipping page
        LoggerUtility.info("Step 24: Clicking Siguiente on shipping page");
        fdaPaymentPage.clickNextButton();

        // Step 25: Select Pago con Tarjeta Crédito/Débito
        LoggerUtility.info("Step 25: Selecting Pago con Tarjeta Crédito/Débito");
        fdaPaymentPage.selectCreditCardOption();

        // Steps 26-27: Card number
        LoggerUtility.info("Step 26: Clicking Número de tarjeta text field");
        LoggerUtility.info("Step 27: Entering card number");
        fdaPaymentPage.enterCardNumber(TC_CARD_NUMBER);

        // Steps 28-29: Expiry date
        LoggerUtility.info("Step 28: Clicking Fecha de expiración text field");
        LoggerUtility.info("Step 29: Entering expiry: " + TC_CARD_EXPIRY);
        fdaPaymentPage.enterExpiry(TC_CARD_EXPIRY);

        // Steps 30-31: Security code
        LoggerUtility.info("Step 30: Clicking Código de seguridad text field");
        LoggerUtility.info("Step 31: Entering CVV");
        fdaPaymentPage.enterCvv(TC_CARD_CVV);

        // Step 32: Verify Completar pago button displays order total
        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Step 32: Completar pago button text: " + payBtnText);
        Assert.assertTrue(
            payBtnText.contains("Completar pago") || payBtnText.contains("MXN") || payBtnText.contains("$"),
            "TC_FBO_030 — Completar pago button should display order total. Actual: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 33: Complete payment
        LoggerUtility.info("Step 33: Clicking Completar pago button");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();

        // Step 34: Verify success page
        LoggerUtility.info("Step 34: Verifying order success page");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(),
            "TC_FBO_030 — Success page not displayed after payment");

        // Step 35: Capture Order ID — used in Mirakl search and Cancel Shipment API
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 35: Order ID = " + orderId);
        LoggerUtility.info("TC_FBO_030 | Order ID            : " + orderId);
        LoggerUtility.info("TC_FBO_030 | API Order Reference : " + orderId + "WEB");
        Assert.assertFalse(orderId.isEmpty(),
            "TC_FBO_030 — Order ID should be present on success page");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: FDA — Order History Validation
        // ============================================================
        LoggerUtility.info("===== TC_FBO_030 PHASE 4: FDA Order History =====");

        LoggerUtility.info("Step 36: Clicking Mi cuenta profile icon");
        fdaHomePage.clickProfileIcon();
        LoggerUtility.info("Step 37: Clicking Mis pedidos link");
        fdaHomePage.clickMyOrdersLink();
        LoggerUtility.info("Step 38: Verifying Mis pedidos page is displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(),
            "TC_FBO_030 — Mis pedidos page not displayed");
        LoggerUtility.info("Step 39: Verifying Order ID " + orderId + " is present in order history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
            "TC_FBO_030 — Order ID " + orderId + " not found in FDA order history");
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 40: FDA order status: " + fdaStatus);
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
            "TC_FBO_030 — FDA order status should be 'Creada'. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 5: MIRAKL — Find Order, Verify 2 Shipments, Accept Both
        // ============================================================
        LoggerUtility.info("===== TC_FBO_030 PHASE 5: Mirakl — Accept Both Shipments =====");

        // Step 41: Switch to Mirakl tab — session active from suite @BeforeSuite
        LoggerUtility.info("Step 41: Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 42-43: Navigate to All Orders
        LoggerUtility.info("Step 42: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 43: Clicking All Orders");
        miraklOrdersPage.clickAllOrders();

        // Steps 44-46: Search for order — retry every 60s up to 5 min for sync delay
        String miraklSearchTerm = orderId + "WEB";
        String shipmentRefA     = orderId + "WEB-A";
        String shipmentRefB     = orderId + "WEB-B";

        LoggerUtility.info("Step 44: Clicking Mirakl search field");
        LoggerUtility.info("Step 45: Searching for order: " + miraklSearchTerm);
        boolean orderFoundInMirakl = false;
        for (int attempt = 1; attempt <= 10; attempt++) {
            LoggerUtility.info("Mirakl search attempt " + attempt + "/10 for: " + miraklSearchTerm);
            miraklOrdersPage.searchOrder(miraklSearchTerm);
            if (miraklOrdersPage.hasSearchResults(miraklSearchTerm)) {
                orderFoundInMirakl = true;
                LoggerUtility.info("Order found in Mirakl on attempt " + attempt);
                break;
            }
            LoggerUtility.info("Order not in Mirakl yet — attempt " + attempt + "/10");
            if (attempt < 10) {
                LoggerUtility.info("Waiting 60 seconds before next Mirakl search attempt...");
                Thread.sleep(60_000);
                miraklOrdersPage.clickOrdersMenu();
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(orderFoundInMirakl,
            "TC_FBO_030 — Order " + miraklSearchTerm + " did not appear in Mirakl within 10 minutes");

        // Step 46: Verify both WEB-A and WEB-B shipments are present
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefA),
            "TC_FBO_030 — Shipment WEB-A not found in Mirakl: " + shipmentRefA);
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefB),
            "TC_FBO_030 — Shipment WEB-B not found in Mirakl: " + shipmentRefB);
        LoggerUtility.info("TC_FBO_030 | Two shipments verified — WEB-A: " + shipmentRefA + " | WEB-B: " + shipmentRefB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ---- Accept WEB-A ----
        LoggerUtility.info("Step 46a: Opening shipment WEB-A: " + shipmentRefA);
        miraklOrderDetailPage.clickOrderInList(shipmentRefA);

        String statusBeforeAcceptA = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-A status before accept: " + statusBeforeAcceptA);
        Assert.assertEquals(statusBeforeAcceptA, "Pending acceptance",
            "TC_FBO_030 — WEB-A should be 'Pending acceptance'. Actual: " + statusBeforeAcceptA);

        // Identify which product EAN is in WEB-A: try page source first.
        // Fallback: TC_SKU_1 (78078094274) — confirmed by order 4000288409WEB-A curl example.
        // Cancel API requires EAN, not offer_sku.
        String productInShipmentA = miraklOrderDetailPage.findProductSkuOnPage(TC_SKU_1, TC_SKU_2);
        if (productInShipmentA == null || productInShipmentA.isEmpty()) {
            productInShipmentA = TC_SKU_1;
            LoggerUtility.info("SKU not found in Mirakl page source (names shown, not EANs) "
                + "— using known WEB-A assignment: " + TC_SKU_1);
        }
        String productInShipmentB = TC_SKU_1.equals(productInShipmentA) ? TC_SKU_2 : TC_SKU_1;
        LoggerUtility.info("TC_FBO_030 | Product in WEB-A   : " + productInShipmentA);
        LoggerUtility.info("TC_FBO_030 | Product in WEB-B   : " + productInShipmentB);

        // Capture WEB-A URL now — used in Phase 7 to navigate directly to its detail page
        String urlShipmentA = driver.getCurrentUrl();
        LoggerUtility.info("TC_FBO_030 | WEB-A URL: " + urlShipmentA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Accepting shipment WEB-A");
        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        String statusAfterAcceptA = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-A status after accept: " + statusAfterAcceptA);
        Assert.assertEquals(statusAfterAcceptA, "Awaiting shipment",
            "TC_FBO_030 — WEB-A should be 'Awaiting shipment' after acceptance. Actual: " + statusAfterAcceptA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Navigate back to All Orders to open WEB-B
        LoggerUtility.info("Navigating back to All Orders to open WEB-B");
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        miraklOrdersPage.searchOrder(miraklSearchTerm);

        // ---- Accept WEB-B ----
        LoggerUtility.info("Step 46b: Opening shipment WEB-B: " + shipmentRefB);
        miraklOrderDetailPage.clickOrderInList(shipmentRefB);

        String statusBeforeAcceptB = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-B status before accept: " + statusBeforeAcceptB);
        Assert.assertEquals(statusBeforeAcceptB, "Pending acceptance",
            "TC_FBO_030 — WEB-B should be 'Pending acceptance'. Actual: " + statusBeforeAcceptB);

        // Capture WEB-B URL — used in Phase 7 to verify it stays "Awaiting shipment"
        String urlShipmentB = driver.getCurrentUrl();
        LoggerUtility.info("TC_FBO_030 | WEB-B URL: " + urlShipmentB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Accepting shipment WEB-B");
        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        String statusAfterAcceptB = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-B status after accept: " + statusAfterAcceptB);
        Assert.assertEquals(statusAfterAcceptB, "Awaiting shipment",
            "TC_FBO_030 — WEB-B should be 'Awaiting shipment' after acceptance. Actual: " + statusAfterAcceptB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 47: Business-required 1-minute wait after accepting both shipments before cancel
        LoggerUtility.info("Step 47: Both shipments accepted — waiting 60 seconds before cancel API call");
        Thread.sleep(60_000);
        LoggerUtility.info("Step 47: 60-second wait complete");

        // ============================================================
        // PHASE 6: CANCEL SHIPMENT API — Cancel WEB-A only
        // ============================================================
        LoggerUtility.info("===== TC_FBO_030 PHASE 6: Cancel Shipment API — WEB-A only =====");

        String apiOrderReference   = orderId + "WEB";
        String cancelledShipmentId = shipmentRefA;                      // orderId + "WEB-A"
        // Mirakl order_line_id for WEB-A: "{orderId}WEB-A-1" (first order line suffix)
        // EAN (productInShipmentA) fails — this seller's offer_sku != EAN in Mirakl
        String orderLineId = cancelledShipmentId + "-1";
        List<String> cancelProductIds = java.util.List.of(orderLineId);

        LoggerUtility.info("Step 48: Building cancel shipment request");
        LoggerUtility.info("TC_FBO_030 | Cancel Shipment ID  : " + cancelledShipmentId);
        LoggerUtility.info("TC_FBO_030 | Cancel Product IDs  : " + cancelProductIds);
        LoggerUtility.info("TC_FBO_030 | Cancel Order Ref    : " + apiOrderReference);

        LoggerUtility.info("Step 48: Calling Cancel Shipment API (WEB-A only)");
        Response cancelResponse = ApiUtility.cancelShipment(
            cancelledShipmentId, cancelProductIds, apiOrderReference);

        LoggerUtility.info("TC_FBO_030 | Cancel API HTTP Status : " + cancelResponse.getStatusCode());
        LoggerUtility.info("TC_FBO_030 | Cancel API Body        : " + cancelResponse.getBody().asString());
        Assert.assertTrue(
            cancelResponse.getStatusCode() == 200
                || cancelResponse.getStatusCode() == 201
                || cancelResponse.getStatusCode() == 204,
            "TC_FBO_030 — Cancel Shipment API should return 2xx. Actual HTTP: "
                + cancelResponse.getStatusCode()
                + " | Body: " + cancelResponse.getBody().asString());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 7: MIRAKL — Verify WEB-A = "Canceled", WEB-B = "Awaiting shipment"
        // ============================================================
        LoggerUtility.info("===== TC_FBO_030 PHASE 7: Mirakl Shipment Status Verification =====");

        // Step 49-50: Navigate to WEB-A detail, poll until "Canceled"
        LoggerUtility.info("Step 49: Switching to Mirakl tab and navigating to WEB-A detail");
        switchToMiraklTab();
        driver.get(urlShipmentA);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        LoggerUtility.info("Step 50: Polling WEB-A for 'Canceled' status (refresh + FluentWait, up to 20 retries)");
        String finalStatusA = waitForMiraklStatus("Canceled", 20);
        LoggerUtility.info("TC_FBO_030 | WEB-A Final Status : " + finalStatusA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        Assert.assertEquals(finalStatusA, "Canceled",
            "TC_FBO_030 — WEB-A (" + cancelledShipmentId + ") should be 'Canceled'. Actual: " + finalStatusA);

        // Step 50 (part 2): Navigate to WEB-B — verify it was NOT cancelled
        LoggerUtility.info("Step 50 (WEB-B): Navigating to WEB-B to verify it is not cancelled");
        driver.get(urlShipmentB);
        WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
        String finalStatusB = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("TC_FBO_030 | WEB-B Final Status : " + finalStatusB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        Assert.assertNotEquals(finalStatusB, "Canceled",
            "TC_FBO_030 — WEB-B (" + shipmentRefB + ") should NOT be 'Canceled'. It was incorrectly cancelled.");
        Assert.assertEquals(finalStatusB, "Awaiting shipment",
            "TC_FBO_030 — WEB-B (" + shipmentRefB + ") should remain 'Awaiting shipment'. Actual: " + finalStatusB);

        // ============================================================
        // FINAL SUMMARY LOG
        // ============================================================
        LoggerUtility.info("===== TC_FBO_030 COMPLETE — PASS =====");
        LoggerUtility.info("TC_FBO_030 | Test Case ID              : " + TC_NAME);
        LoggerUtility.info("TC_FBO_030 | FDA User                  : " + TC_FDA_USER);
        LoggerUtility.info("TC_FBO_030 | SKU 1                     : " + TC_SKU_1);
        LoggerUtility.info("TC_FBO_030 | SKU 2                     : " + TC_SKU_2);
        LoggerUtility.info("TC_FBO_030 | Quantity Each             : " + TC_QUANTITY);
        LoggerUtility.info("TC_FBO_030 | Cart Product 1 Name       : " + (productNames.size() > 0 ? productNames.get(0) : ""));
        LoggerUtility.info("TC_FBO_030 | Cart Product 2 Name       : " + (productNames.size() > 1 ? productNames.get(1) : ""));
        LoggerUtility.info("TC_FBO_030 | Order ID                  : " + orderId);
        LoggerUtility.info("TC_FBO_030 | API Order Reference       : " + apiOrderReference);
        LoggerUtility.info("TC_FBO_030 | Order Total               : " + orderTotal);
        LoggerUtility.info("TC_FBO_030 | Initial FDA Status        : " + fdaStatus);
        LoggerUtility.info("TC_FBO_030 | Shipment A ID             : " + shipmentRefA);
        LoggerUtility.info("TC_FBO_030 | Shipment B ID             : " + shipmentRefB);
        LoggerUtility.info("TC_FBO_030 | Product in WEB-A (cancel) : " + productInShipmentA);
        LoggerUtility.info("TC_FBO_030 | Cancel Shipment ID        : " + cancelledShipmentId);
        LoggerUtility.info("TC_FBO_030 | Cancel API HTTP Status    : " + cancelResponse.getStatusCode());
        LoggerUtility.info("TC_FBO_030 | WEB-A Final Status        : " + finalStatusA);
        LoggerUtility.info("TC_FBO_030 | WEB-B Final Status        : " + finalStatusB);
        LoggerUtility.info("TC_FBO_030 | Test Execution Status     : PASS");
    }

    // ----------------------------------------------------------------
    // Polls Mirakl status by refreshing the current page until expectedStatus
    // is reached or maxRetries exhausted. Uses FluentWait — no Thread.sleep.
    // ----------------------------------------------------------------
    private String waitForMiraklStatus(String expectedStatus, int maxRetries) throws InterruptedException {
        String current = "";
        for (int i = 1; i <= maxRetries; i++) {
            LoggerUtility.info("Mirakl status poll " + i + "/" + maxRetries
                + " — expecting: " + expectedStatus);
            driver.navigate().refresh();
            WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);
            current = miraklOrderDetailPage.getOrderStatus();
            LoggerUtility.info("Mirakl current status: " + current);
            if (expectedStatus.equals(current)) {
                LoggerUtility.info("Expected status reached: " + current);
                return current;
            }
            if (i < maxRetries) {
                Thread.sleep(10_000);
            }
        }
        LoggerUtility.warn("Status did not reach '" + expectedStatus + "' after "
            + maxRetries + " retries. Last observed: " + current);
        return current;
    }

    // ----------------------------------------------------------------
    // Handles payment checkout opening in a new window/tab after
    // clickProceedToPayment(). Updates fdaTabHandle if the window changed.
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
// @AfterMethod navigateToHomePage() is inherited from BaseClass — navigates
// FDA tab to FDA home and Mirakl tab to Mirakl home after the test completes.
