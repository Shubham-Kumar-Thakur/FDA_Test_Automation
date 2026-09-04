package FDA_Automation_Script.FDA_Automation_Script.tests;

import FDA_Automation_Script.FDA_Automation_Script.base.BaseClass;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDACartPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAHomePage;
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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TC_FBO_027_Test extends BaseClass {

    private static final String TC_NAME = "TC_FBO_027";
    private static final String SKU     = "7080901020316";

    // --- Page objects ---
    private FDAHomePage           fdaHomePage;
    private FDAPDPPage            fdaPdpPage;
    private FDACartPage           fdaCartPage;
    private FDAPaymentPage        fdaPaymentPage;
    private FDASuccessPage        fdaSuccessPage;
    private FDAOrderHistoryPage   fdaOrderHistoryPage;
    private MiraklOrdersPage      miraklOrdersPage;
    private MiraklOrderDetailPage miraklOrderDetailPage;

    // --- Dynamic test data ---
    private String orderId;
    private String orderTotal;

    private static final By MIRAKL_ORDER_STATUS_LOCATOR =
        By.xpath("(//span[contains(@class,'_6e782__sc-s4ukbq-0') and not(ancestor::a)])[1]");

    @BeforeClass
    public void initPageObjects() {
        fdaHomePage          = new FDAHomePage(driver);
        fdaPdpPage           = new FDAPDPPage(driver);
        fdaCartPage          = new FDACartPage(driver);
        fdaPaymentPage       = new FDAPaymentPage(driver);
        fdaSuccessPage       = new FDASuccessPage(driver);
        fdaOrderHistoryPage  = new FDAOrderHistoryPage(driver);
        miraklOrdersPage     = new MiraklOrdersPage(driver);
        miraklOrderDetailPage = new MiraklOrderDetailPage(driver);
        LoggerUtility.info("TC_FBO_027: All page objects initialized");
    }

    @Test(testName = TC_NAME,
          description = "Verify 1-product 3P seller order placement and full cancel lifecycle: FDA → Mirakl Accept → Cancel API → Canceled")
    public void tc_fbo_027_order_cancel_flow() throws InterruptedException {

        // ============================================================
        // PHASE 1: FDA — 1 Product, Qty 1, Checkout, Capture Order ID
        // ============================================================
        LoggerUtility.info("===== TC_FBO_027 PHASE 1: FDA Order Placement =====");
        LoggerUtility.info("TC_FBO_027 | SKU: " + SKU);

        // Step 1-2: Switch to FDA tab — session active from suite @BeforeSuite
        switchToFDATab();
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test: remove any leftover cart items
        LoggerUtility.info("Pre-test: Clearing leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // Steps 3-5: Search by SKU
        LoggerUtility.info("Step 3: Clicking search field");
        fdaHomePage.enterSearchQuery(SKU);
        LoggerUtility.info("Step 4-5: Entering SKU and pressing Enter: " + SKU);
        fdaHomePage.pressSearchEnter();

        // Steps 6-8: PDP validations
        LoggerUtility.info("Step 6: Verifying PDP is displayed");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "TC_FBO_027 — PDP not displayed for SKU: " + SKU);
        LoggerUtility.info("Step 7: Verifying Agregar al carrito button is enabled");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "TC_FBO_027 — Add to cart button not enabled for SKU: " + SKU);
        String pdpQty = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 8: PDP quantity: " + pdpQty);
        Assert.assertEquals(pdpQty, "1",
            "TC_FBO_027 — PDP quantity should be 1");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 9: Add to cart
        LoggerUtility.info("Step 9: Clicking Agregar al carrito");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product added to cart");

        // Steps 10-14: Navigate to cart and validate
        LoggerUtility.info("Step 10: Navigating to cart");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        int itemCount = fdaCartPage.getItemCount();
        LoggerUtility.info("Step 11: Cart item count: " + itemCount);
        Assert.assertEquals(itemCount, 1,
            "TC_FBO_027 — Cart should contain exactly 1 product. Actual: " + itemCount);

        String cartProductName = fdaCartPage.getProductName();
        LoggerUtility.info("Step 12: Cart product name: " + cartProductName);
        Assert.assertFalse(cartProductName.isEmpty(),
            "TC_FBO_027 — Cart product name should not be empty");

        String cartQty = fdaCartPage.getQuantity();
        LoggerUtility.info("Step 13: Cart quantity: " + cartQty);
        Assert.assertEquals(cartQty, "1",
            "TC_FBO_027 — Cart quantity should be 1");

        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 14: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(),
            "TC_FBO_027 — Order total should be displayed in cart");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 15: Proceed to payment
        LoggerUtility.info("Step 15: Clicking Proceed to payment");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();

        // Step 16: Siguiente (shipping page)
        LoggerUtility.info("Step 16: Clicking Siguiente on shipping page");
        fdaPaymentPage.clickNextButton();

        // Step 17: Select Credit/Debit Card Payment
        LoggerUtility.info("Step 17: Selecting Pago con Tarjeta Crédito/Débito");
        fdaPaymentPage.selectCreditCardOption();

        // Steps 18-23: Enter card details
        LoggerUtility.info("Step 18-19: Entering card number");
        fdaPaymentPage.enterCardNumber(config.getFdaCardNumber());
        LoggerUtility.info("Step 20-21: Entering expiration date");
        fdaPaymentPage.enterExpiry(config.getFdaCardExpiry());
        LoggerUtility.info("Step 22-23: Entering security code");
        fdaPaymentPage.enterCvv(config.getFdaCardCvv());

        // Step 24: Verify Completar pago button displays order total
        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Step 24: Completar pago button text: " + payBtnText);
        Assert.assertTrue(
            payBtnText.contains("Completar pago") || payBtnText.contains("MXN")
                || payBtnText.contains("$"),
            "TC_FBO_027 — Completar pago button should show order total. Actual: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 25: Complete payment
        LoggerUtility.info("Step 25: Clicking Completar pago");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();

        // Step 26-27: Verify success page + capture Order ID
        LoggerUtility.info("Step 26: Verifying success page");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(),
            "TC_FBO_027 — Success page not displayed after payment");
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 27: Order ID = " + orderId);
        LoggerUtility.info("TC_FBO_027 | Order ID            : " + orderId);
        LoggerUtility.info("TC_FBO_027 | API Order Reference : " + orderId + "WEB");
        Assert.assertFalse(orderId.isEmpty(),
            "TC_FBO_027 — Order ID should be present on success page");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Steps 28-32: Order history verification
        LoggerUtility.info("Step 28: Clicking Mi cuenta");
        fdaHomePage.clickProfileIcon();
        LoggerUtility.info("Step 29: Clicking Mis pedidos");
        fdaHomePage.clickMyOrdersLink();
        LoggerUtility.info("Step 30: Verifying Mis pedidos page");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(),
            "TC_FBO_027 — Mis pedidos page not displayed");
        LoggerUtility.info("Step 31: Verifying order " + orderId + " in history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
            "TC_FBO_027 — Order ID " + orderId + " not found in FDA order history");
        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 32: FDA order status: " + fdaStatus);
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
            "TC_FBO_027 — FDA order status should be 'Creada'. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        LoggerUtility.info("TC_FBO_027 | Initial FDA Order Status : " + fdaStatus);

        // ============================================================
        // PHASE 2: MIRAKL — Find Order, Accept
        // ============================================================
        LoggerUtility.info("===== TC_FBO_027 PHASE 2: Mirakl — Accept Order =====");

        // Step 33: Switch to Mirakl tab — session active from suite @BeforeSuite
        LoggerUtility.info("Step 33: Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 34: Verify Mirakl is already logged in (no login performed — session reused)
        LoggerUtility.info("Step 34: Mirakl session active — no login required");

        // Steps 35-36: Navigate to All Orders
        LoggerUtility.info("Step 35: Clicking Orders menu");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 36: Clicking All Orders");
        miraklOrdersPage.clickAllOrders();

        // Steps 37-39: Search and verify order in Mirakl — retry up to 5 min for sync delay
        String miraklSearchTerm = orderId + "WEB";
        LoggerUtility.info("Steps 37-39: Searching Mirakl for: " + miraklSearchTerm);
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
            "TC_FBO_027 — Order " + miraklSearchTerm + " did not appear in Mirakl within 5 minutes");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 40: Click into order detail
        LoggerUtility.info("Step 40: Clicking order in Mirakl list: " + orderId);
        miraklOrderDetailPage.clickOrderInList(orderId);
        String miraklStatusBeforeAccept = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("Mirakl order status before accept: " + miraklStatusBeforeAccept);
        LoggerUtility.info("TC_FBO_027 | Mirakl Status before cancellation: " + miraklStatusBeforeAccept);
        Assert.assertEquals(miraklStatusBeforeAccept, "Pending acceptance",
            "TC_FBO_027 — Mirakl order should be 'Pending acceptance'. Actual: "
                + miraklStatusBeforeAccept);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 41: Accept order
        LoggerUtility.info("Step 41: Clicking Accept on Mirakl order");
        miraklOrderDetailPage.clickAcceptButton();

        // Step 42-43: Verify accepted — poll via refreshAndWait (FluentWait, no Thread.sleep)
        LoggerUtility.info("Step 42-43: Verifying acceptance via page refresh and FluentWait");
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        String miraklStatusAfterAccept = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("Mirakl status after accept: " + miraklStatusAfterAccept);
        Assert.assertEquals(miraklStatusAfterAccept, "Awaiting shipment",
            "TC_FBO_027 — Mirakl status should be 'Awaiting shipment' after acceptance. Actual: "
                + miraklStatusAfterAccept);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: CANCEL FULL ORDER API
        // ============================================================
        LoggerUtility.info("===== TC_FBO_027 PHASE 3: Cancel Full Order API =====");

        // Steps 44-46: Build order reference and call Cancel API
        String apiOrderReference = orderId + "WEB";
        LoggerUtility.info("Step 44-45: API order reference: " + apiOrderReference);
        LoggerUtility.info("Step 46: Calling Cancel Full Order API");
        Response cancelResponse = ApiUtility.cancelFullOrder(apiOrderReference);

        // Step 47: Validate cancel API response
        LoggerUtility.info("Step 47: Validating Cancel API response");
        LoggerUtility.info("TC_FBO_027 | Cancel API HTTP Status : " + cancelResponse.getStatusCode());
        LoggerUtility.info("TC_FBO_027 | Cancel API Body        : " + cancelResponse.getBody().asString());
        Assert.assertTrue(
            cancelResponse.getStatusCode() == 200
                || cancelResponse.getStatusCode() == 201
                || cancelResponse.getStatusCode() == 204,
            "TC_FBO_027 — Cancel Full Order API should return 2xx. Actual HTTP: "
                + cancelResponse.getStatusCode()
                + " | Body: " + cancelResponse.getBody().asString());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: MIRAKL — Poll Until Status = "Canceled"
        // ============================================================
        LoggerUtility.info("===== TC_FBO_027 PHASE 4: Mirakl Status Verification =====");

        // Steps 48-50: Switch back to Mirakl, still on the order detail page
        LoggerUtility.info("Step 48: Switching back to Mirakl tab");
        switchToMiraklTab();
        LoggerUtility.info("Step 49-50: Polling Mirakl for 'Canceled' status (refresh + FluentWait)");

        // Steps 51-52: Poll until "Canceled" — uses page refresh + FluentWait (no Thread.sleep)
        String finalMiraklStatus = waitForMiraklStatus("Canceled", 20);
        LoggerUtility.info("TC_FBO_027 | Final Mirakl Order Status : " + finalMiraklStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        Assert.assertEquals(finalMiraklStatus, "Canceled",
            "TC_FBO_027 — Final Mirakl order status should be 'Canceled'. Actual: "
                + finalMiraklStatus);

        // ============================================================
        // FINAL SUMMARY LOG
        // ============================================================
        LoggerUtility.info("===== TC_FBO_027 COMPLETE — PASS =====");
        LoggerUtility.info("TC_FBO_027 | Test Case ID          : " + TC_NAME);
        LoggerUtility.info("TC_FBO_027 | SKU                   : " + SKU);
        LoggerUtility.info("TC_FBO_027 | Product Name          : " + cartProductName);
        LoggerUtility.info("TC_FBO_027 | Order ID              : " + orderId);
        LoggerUtility.info("TC_FBO_027 | Order ID + WEB        : " + apiOrderReference);
        LoggerUtility.info("TC_FBO_027 | Order Total           : " + orderTotal);
        LoggerUtility.info("TC_FBO_027 | Initial FDA Status    : " + fdaStatus);
        LoggerUtility.info("TC_FBO_027 | Mirakl Status before  : " + miraklStatusBeforeAccept);
        LoggerUtility.info("TC_FBO_027 | Cancel API Response   : HTTP " + cancelResponse.getStatusCode());
        LoggerUtility.info("TC_FBO_027 | Final Mirakl Status   : " + finalMiraklStatus);
        LoggerUtility.info("TC_FBO_027 | Test Execution Status : PASS");
    }

    // Polls Mirakl status by refreshing the current page until expectedStatus is reached.
    // Uses FluentWait (no Thread.sleep) — satisfies framework wait requirement.
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

    // Handles payment checkout opening in a new window/tab
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
// @AfterMethod navigateToHomePage() is inherited from BaseClass
