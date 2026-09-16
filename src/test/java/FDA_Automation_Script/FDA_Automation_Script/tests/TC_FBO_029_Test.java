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

public class TC_FBO_029_Test extends BaseClass {

    private static final String TC_NAME        = "TC_FBO_029";

    // TC-specific credentials and test data — never stored in config.properties
    private static final String TC_FDA_USER    = "mgowda@kognivera.com";
    private static final String TC_FDA_PASS    = "Mithun@12345";
    private static final String TC_SKU         = "78078094274";
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
        LoggerUtility.info("TC_FBO_029: All page objects initialized");

        // TC_FBO_029 uses a different FDA account — guard prevents double-logout
        // when the previous TC already ran as the same user
        if (!TC_FDA_USER.equals(config.getFdaUsername())) {
            LoggerUtility.info("TC_FBO_029 @BeforeClass: Switching FDA session to: " + TC_FDA_USER);
            switchToFDATab();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.logout();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.clickProfileIcon();
            fdaHomePage.clickLoginLink();
            fdaLoginPage.login(TC_FDA_USER, TC_FDA_PASS);
            LoggerUtility.info("TC_FBO_029 @BeforeClass: FDA login as " + TC_FDA_USER + " successful");
        }
    }

    @AfterClass(alwaysRun = true)
    public void restoreOriginalFdaSession() {
        // Restore suite-default FDA user so subsequent test cases are unaffected
        if (!TC_FDA_USER.equals(config.getFdaUsername())) {
            LoggerUtility.info("TC_FBO_029 @AfterClass: Restoring original FDA session");
            try {
                switchToFDATab();
                fdaHomePage.navigateTo(config.getFdaUrl());
                fdaHomePage.logout();
                fdaHomePage.navigateTo(config.getFdaUrl());
                fdaHomePage.clickProfileIcon();
                fdaHomePage.clickLoginLink();
                fdaLoginPage.login(config.getFdaUsername(), config.getFdaPassword());
                LoggerUtility.info("TC_FBO_029 @AfterClass: FDA session restored as " + config.getFdaUsername());
            } catch (Exception e) {
                LoggerUtility.error("TC_FBO_029 @AfterClass: Failed to restore FDA session: " + e.getMessage());
            }
        }
    }

    @Test(testName = TC_NAME, groups = {"FBO"},
          description = "Verify 1 product qty=2 by 1 3P seller — seller cancels order: FDA → Mirakl Accept → Wait 1 min → Cancel Full Order API → Canceled")
    public void tc_fbo_029_qty2_seller_cancel_flow() throws InterruptedException {

        // ============================================================
        // PHASE 1: FDA — 1 Product Qty=2, Checkout, Capture Order ID
        // ============================================================
        LoggerUtility.info("===== TC_FBO_029 PHASE 1: FDA Order Placement =====");
        LoggerUtility.info("TC_FBO_029 | SKU: " + TC_SKU + " | Quantity: " + TC_QUANTITY);

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

        // Steps 2-4: Search by SKU
        LoggerUtility.info("Step 2: Clicking search field — ¿Qué estás buscando?");
        LoggerUtility.info("Step 3: Entering SKU: " + TC_SKU);
        fdaHomePage.enterSearchQuery(TC_SKU);
        LoggerUtility.info("Step 4: Pressing Enter to search");
        fdaHomePage.pressSearchEnter();

        // Steps 5-6: PDP validations
        LoggerUtility.info("Step 5: Verifying Product Details Page is displayed");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "TC_FBO_029 — PDP not displayed for SKU: " + TC_SKU);
        LoggerUtility.info("Step 6: Verifying Agregar al carrito button is enabled");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "TC_FBO_029 — Add to cart button not enabled for SKU: " + TC_SKU);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 7: Click plus button to increase quantity from 1 → 2
        LoggerUtility.info("Step 7: Clicking plus button to increase quantity to " + TC_QUANTITY);
        fdaPdpPage.increaseQuantity();

        // Step 8: Verify quantity is 2
        String pdpQty = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 8: PDP quantity after increment: " + pdpQty);
        Assert.assertEquals(pdpQty, String.valueOf(TC_QUANTITY),
            "TC_FBO_029 — PDP quantity should be " + TC_QUANTITY + " after clicking plus. Actual: " + pdpQty);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 9: Add to cart
        LoggerUtility.info("Step 9: Clicking Agregar al carrito");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product added to cart with quantity " + TC_QUANTITY);

        // Steps 10-14: Cart page validations
        LoggerUtility.info("Step 10: Opening Mi carrito (cart page)");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        int itemCount = fdaCartPage.getItemCount();
        LoggerUtility.info("Step 11: Cart item count: " + itemCount);
        Assert.assertEquals(itemCount, 1,
            "TC_FBO_029 — Cart should contain exactly 1 product. Actual: " + itemCount);

        String cartProductName = fdaCartPage.getProductName();
        LoggerUtility.info("Step 12: Cart product name: " + cartProductName);
        Assert.assertFalse(cartProductName.isEmpty(),
            "TC_FBO_029 — Product name should be displayed in cart");

        String cartQty = fdaCartPage.getQuantity();
        LoggerUtility.info("Step 13: Cart quantity: " + cartQty);
        Assert.assertEquals(cartQty, String.valueOf(TC_QUANTITY),
            "TC_FBO_029 — Cart quantity should be " + TC_QUANTITY + ". Actual: " + cartQty);

        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 14: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(),
            "TC_FBO_029 — Order total should be displayed in cart");
        Assert.assertTrue(orderTotal.contains("800"),
            "TC_FBO_029 — Order total should reflect MXN$800.00. Actual: " + orderTotal);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 15: Proceed to payment
        LoggerUtility.info("Step 15: Clicking Proceed to payment");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();

        // Step 16: Siguiente on shipping page
        LoggerUtility.info("Step 16: Clicking Siguiente on shipping page");
        fdaPaymentPage.clickNextButton();

        // Step 17: Select Pago con Tarjeta Crédito/Débito
        LoggerUtility.info("Step 17: Selecting Pago con Tarjeta Crédito/Débito");
        fdaPaymentPage.selectCreditCardOption();

        // Steps 18-19: Card number
        LoggerUtility.info("Step 18: Clicking Número de tarjeta text field");
        LoggerUtility.info("Step 19: Entering card number");
        fdaPaymentPage.enterCardNumber(TC_CARD_NUMBER);

        // Steps 20-21: Expiry date
        LoggerUtility.info("Step 20: Clicking Fecha de expiración text field");
        LoggerUtility.info("Step 21: Entering expiry: " + TC_CARD_EXPIRY);
        fdaPaymentPage.enterExpiry(TC_CARD_EXPIRY);

        // Steps 22-23: Security code
        LoggerUtility.info("Step 22: Clicking Código de seguridad text field");
        LoggerUtility.info("Step 23: Entering CVV");
        fdaPaymentPage.enterCvv(TC_CARD_CVV);

        // Step 24: Verify Completar pago button displays order total
        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Step 24: Completar pago button text: " + payBtnText);
        Assert.assertTrue(
            payBtnText.contains("Completar pago") || payBtnText.contains("MXN") || payBtnText.contains("$"),
            "TC_FBO_029 — Completar pago button should display order total. Actual: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 25: Complete payment
        LoggerUtility.info("Step 25: Clicking Completar pago button");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();

        // Step 26: Verify success page
        LoggerUtility.info("Step 26: Verifying order success page");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(),
            "TC_FBO_029 — Success page not displayed after payment");

        // Step 27: Capture Order ID — reused in Mirakl search and Cancel API
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 27: Order ID = " + orderId);
        LoggerUtility.info("TC_FBO_029 | Order ID            : " + orderId);
        LoggerUtility.info("TC_FBO_029 | API Order Reference : " + orderId + "WEB");
        Assert.assertFalse(orderId.isEmpty(),
            "TC_FBO_029 — Order ID should be present on success page");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 28-32: Order history validation
        LoggerUtility.info("Step 28: Clicking Mi cuenta profile icon");
        fdaHomePage.clickProfileIcon();
        LoggerUtility.info("Step 29: Clicking Mis pedidos link");
        fdaHomePage.clickMyOrdersLink();
        LoggerUtility.info("Step 30: Verifying Mis pedidos page is displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(),
            "TC_FBO_029 — Mis pedidos page not displayed");
        LoggerUtility.info("Step 31: Verifying Order ID " + orderId + " is present in order history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
            "TC_FBO_029 — Order ID " + orderId + " not found in FDA order history");
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 32: FDA order status: " + fdaStatus);
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
            "TC_FBO_029 — FDA order status should be 'Creada'. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 2: MIRAKL — Find WEB-A Shipment, Accept
        // ============================================================
        LoggerUtility.info("===== TC_FBO_029 PHASE 2: Mirakl — Accept Order =====");

        // Step 33: Switch to Mirakl tab — session active from suite @BeforeSuite
        LoggerUtility.info("Step 33: Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 34-35: Navigate to All Orders
        LoggerUtility.info("Step 34: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 35: Clicking All Orders");
        miraklOrdersPage.clickAllOrders();

        // Steps 36-38: Search for order — retry every 60s up to 5 min for sync delay
        String miraklSearchTerm = orderId + "WEB";
        String shipmentRef      = orderId + "WEB-A";   // 1 SKU qty=2 → single WEB-A shipment

        LoggerUtility.info("Step 36: Clicking Mirakl search field");
        LoggerUtility.info("Step 37: Searching for order: " + miraklSearchTerm);
        boolean orderFoundInMirakl = false;
        for (int attempt = 1; attempt <= 5; attempt++) {
            LoggerUtility.info("Mirakl search attempt " + attempt + "/5 for: " + miraklSearchTerm);
            miraklOrdersPage.searchOrder(miraklSearchTerm);
            if (miraklOrdersPage.hasSearchResults(miraklSearchTerm)) {
                orderFoundInMirakl = true;
                LoggerUtility.info("Order found in Mirakl on attempt " + attempt);
                break;
            }
            LoggerUtility.info("Order not in Mirakl yet — attempt " + attempt + "/5");
            if (attempt < 5) {
                LoggerUtility.info("Waiting 60 seconds before next Mirakl search attempt...");
                Thread.sleep(60_000);
                miraklOrdersPage.clickOrdersMenu();
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(orderFoundInMirakl,
            "TC_FBO_029 — Order " + miraklSearchTerm + " did not appear in Mirakl within 5 minutes");

        // Step 38: Verify WEB-A shipment row is present in results
        LoggerUtility.info("Step 38: Verifying WEB-A shipment exists: " + shipmentRef);
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRef),
            "TC_FBO_029 — Expected shipment reference " + shipmentRef + " not found in Mirakl search results");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 39: Open WEB-A order detail → verify Pending acceptance → Accept
        LoggerUtility.info("Step 39: Opening Mirakl order: " + shipmentRef);
        miraklOrderDetailPage.clickOrderInList(shipmentRef);
        String miraklStatusBeforeAccept = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("Mirakl status before accept: " + miraklStatusBeforeAccept);
        LoggerUtility.info("TC_FBO_029 | Mirakl Status before accept: " + miraklStatusBeforeAccept);
        Assert.assertEquals(miraklStatusBeforeAccept, "Pending acceptance",
            "TC_FBO_029 — Mirakl order should be 'Pending acceptance'. Actual: " + miraklStatusBeforeAccept);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 39: Accepting order in Mirakl");
        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        String miraklStatusAfterAccept = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("Mirakl status after accept: " + miraklStatusAfterAccept);
        Assert.assertEquals(miraklStatusAfterAccept, "Awaiting shipment",
            "TC_FBO_029 — Mirakl status should be 'Awaiting shipment' after acceptance. Actual: " + miraklStatusAfterAccept);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 40: Business-required 1-minute wait after acceptance before cancellation
        LoggerUtility.info("Step 40: Waiting 60 seconds after acceptance (business-required delay before cancel)");
        Thread.sleep(60_000);
        LoggerUtility.info("Step 40: 60-second wait complete");

        // ============================================================
        // PHASE 3: CANCEL FULL ORDER API
        // ============================================================
        LoggerUtility.info("===== TC_FBO_029 PHASE 3: Cancel Full Order API =====");

        // Steps 41-43: Build the order reference and call Cancel Full Order API
        String apiOrderReference = orderId + "WEB";
        LoggerUtility.info("Step 41: Cancel Full Order — using existing ApiUtility.cancelFullOrder()");
        LoggerUtility.info("Step 42: Order reference (orderId + WEB): " + apiOrderReference);
        LoggerUtility.info("Step 43: Calling Cancel Full Order API");
        Response cancelResponse = ApiUtility.cancelFullOrder(apiOrderReference);

        // Step 44: Validate cancel API response — accepts 200, 201, or 204
        LoggerUtility.info("Step 44: Validating Cancel Full Order API response");
        LoggerUtility.info("TC_FBO_029 | Cancel API HTTP Status : " + cancelResponse.getStatusCode());
        LoggerUtility.info("TC_FBO_029 | Cancel API Body        : " + cancelResponse.getBody().asString());
        Assert.assertTrue(
            cancelResponse.getStatusCode() == 200
                || cancelResponse.getStatusCode() == 201
                || cancelResponse.getStatusCode() == 204,
            "TC_FBO_029 — Cancel Full Order API should return 2xx. Actual HTTP: "
                + cancelResponse.getStatusCode()
                + " | Body: " + cancelResponse.getBody().asString());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: MIRAKL — Poll Until Status = "Canceled"
        // ============================================================
        LoggerUtility.info("===== TC_FBO_029 PHASE 4: Mirakl Status Verification =====");

        // Step 45: Switch back to Mirakl tab — still on the WEB-A order detail page
        LoggerUtility.info("Step 45: Switching back to Mirakl tab");
        switchToMiraklTab();

        // Steps 46-48: Refresh and poll until "Canceled" (FluentWait, no Thread.sleep)
        LoggerUtility.info("Step 46-48: Polling Mirakl for 'Canceled' status (refresh + FluentWait, up to 20 retries)");
        String finalMiraklStatus = waitForMiraklStatus("Canceled", 20);
        LoggerUtility.info("TC_FBO_029 | Final Mirakl Order Status : " + finalMiraklStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        Assert.assertEquals(finalMiraklStatus, "Canceled",
            "TC_FBO_029 — Final Mirakl order status should be 'Canceled'. Actual: " + finalMiraklStatus);

        // ============================================================
        // FINAL SUMMARY LOG
        // ============================================================
        LoggerUtility.info("===== TC_FBO_029 COMPLETE — PASS =====");
        LoggerUtility.info("TC_FBO_029 | Test Case ID            : " + TC_NAME);
        LoggerUtility.info("TC_FBO_029 | FDA User                : " + TC_FDA_USER);
        LoggerUtility.info("TC_FBO_029 | SKU                     : " + TC_SKU);
        LoggerUtility.info("TC_FBO_029 | Quantity                : " + TC_QUANTITY);
        LoggerUtility.info("TC_FBO_029 | Product Name            : " + cartProductName);
        LoggerUtility.info("TC_FBO_029 | Order ID                : " + orderId);
        LoggerUtility.info("TC_FBO_029 | API Order Reference      : " + apiOrderReference);
        LoggerUtility.info("TC_FBO_029 | Order Total             : " + orderTotal);
        LoggerUtility.info("TC_FBO_029 | Initial FDA Status      : " + fdaStatus);
        LoggerUtility.info("TC_FBO_029 | Mirakl Status (pre-accept)  : " + miraklStatusBeforeAccept);
        LoggerUtility.info("TC_FBO_029 | Mirakl Status (post-accept) : " + miraklStatusAfterAccept);
        LoggerUtility.info("TC_FBO_029 | Cancel API HTTP Status   : " + cancelResponse.getStatusCode());
        LoggerUtility.info("TC_FBO_029 | Final Mirakl Status      : " + finalMiraklStatus);
        LoggerUtility.info("TC_FBO_029 | Test Execution Status    : PASS");
    }

    // ----------------------------------------------------------------
    // Polls Mirakl status by refreshing the current page until expectedStatus
    // is reached or maxRetries exhausted. Uses FluentWait — no Thread.sleep.
    // ----------------------------------------------------------------
    private String waitForMiraklStatus(String expectedStatus, int maxRetries) {
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
        java.util.Set<String> handles = driver.getWindowHandles();
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
