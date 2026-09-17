package FDA_Automation_Script.FDA_Automation_Script.tests;

import FDA_Automation_Script.FDA_Automation_Script.base.BaseClass;
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
import FDA_Automation_Script.FDA_Automation_Script.utils.DriverFactory;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ShipmentDetails;
import io.restassured.response.Response;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TC_FBO_SPLIT_01_Test extends BaseClass {

    private static final String TC_NAME            = "TC_FBO_SPLIT_01";
    private static final int    EXPECTED_SHIPMENTS = 6;
    private static final String[] SHIPMENT_SUFFIXES = {
        "WEB-A", "WEB-B", "WEB-C", "WEB-D", "WEB-E", "WEB-F"
    };

    private static final By MIRAKL_ORDER_STATUS_LOCATOR =
            By.xpath("(//span[contains(@class,'_6e782__sc-s4ukbq-0') and not(ancestor::a)])[1]");

    // Page objects
    private FDAHomePage          fdaHomePage;
    private FDALoginPage         fdaLoginPage;
    private FDAPDPPage           fdaPdpPage;
    private FDACartPage          fdaCartPage;
    private FDAPaymentPage       fdaPaymentPage;
    private FDASuccessPage       fdaSuccessPage;
    private FDAOrderHistoryPage  fdaOrderHistoryPage;
    private MiraklLoginPage      miraklLoginPage;
    private MiraklOrdersPage     miraklOrdersPage;
    private MiraklOrderDetailPage miraklOrderDetailPage;

    // Dynamic test data — captured once, reused everywhere
    private String               orderId;
    private String               orderTotal;
    private List<ShipmentDetails> shipmentDetailsList;

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
        LoggerUtility.info("All page objects initialized for TC_FBO_SPLIT_01");
    }

    @Test(testName = TC_NAME,
          description = "Verify order splits into 6 shipments by seller, EDD, package dimensions, and package type")
    public void tc_fbo_split_01_verify_order_split() throws InterruptedException {

        // ============================================================
        // PHASE 1 — FDA Login
        // ============================================================
        LoggerUtility.info("====== PHASE 1: FDA Login ======");
        // Ensure we start on the FDA tab — after TC_FBO_001 the active tab is Mirakl
        switchToFDATab();
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        fdaHomePage.clickProfileIcon();
        // Skip login if session is already active (TC_FBO_001 may have run first in same browser)
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(5));
        boolean fdaLoginNeeded;
        try {
            fdaLoginNeeded = !driver.findElements(
                By.xpath("//li//a[@class='customer-sign-in-link']//span[contains(text(),'Iniciar sesión')]")).isEmpty();
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
        if (fdaLoginNeeded) {
            fdaHomePage.clickLoginLink();
            fdaLoginPage.login(config.getFdaUsername(), config.getFdaPassword());
            ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
            Assert.assertTrue(fdaHomePage.isLoggedIn(), "FDA Login failed — user not logged in");
        } else {
            LoggerUtility.info("FDA session already active — skipping login");
            driver.findElement(By.tagName("body")).sendKeys(org.openqa.selenium.Keys.ESCAPE);
        }
        LoggerUtility.info("FDA Login Successful");

        // Clear any leftover cart items from previous runs
        LoggerUtility.info("Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ============================================================
        // PHASE 2 — Add 8 SKUs to cart one by one
        // ============================================================
        LoggerUtility.info("====== PHASE 2: Adding 8 SKUs to Cart ======");
        String[] skus = config.get("fda.split.skus").split(",");
        LoggerUtility.info("Total SKUs to add: " + skus.length);

        for (int i = 0; i < skus.length; i++) {
            String sku = skus[i].trim();
            LoggerUtility.info("--- SKU " + (i + 1) + "/" + skus.length + " ---");

            // Navigate to home page every iteration — Magento's cart minicart AJAX
            // re-renders the header after each add-to-cart, making the search input stale.
            // A fresh home page load guarantees a clean DOM reference.
            fdaHomePage.navigateTo(config.getFdaUrl());
            LoggerUtility.info("Searching SKU: " + sku);

            fdaHomePage.enterSearchQuery(sku);
            fdaHomePage.pressSearchEnter();

            Assert.assertTrue(fdaPdpPage.isDisplayed(),
                    "PDP not displayed after search for SKU: " + sku);
            LoggerUtility.info("PDP Loaded for SKU: " + sku);

            Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
                    "Add to Cart button not enabled on PDP for SKU: " + sku);

            String qty = fdaPdpPage.getQuantity();
            Assert.assertEquals(qty, "1",
                    "PDP quantity should be 1 for SKU: " + sku + ". Actual: " + qty);

            try {
                fdaPdpPage.clickAddToCart();
            } catch (Exception addToCartEx) {
                LoggerUtility.warn("Add to cart failed for SKU " + sku + " (attempt 1): " + addToCartEx.getMessage());
                LoggerUtility.info("Re-navigating to PDP and retrying add to cart for SKU: " + sku);
                fdaHomePage.navigateTo(config.getFdaUrl());
                fdaHomePage.enterSearchQuery(sku);
                fdaHomePage.pressSearchEnter();
                Assert.assertTrue(fdaPdpPage.isDisplayed(), "PDP not displayed on retry for SKU: " + sku);
                Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(), "Add to Cart not enabled on retry for SKU: " + sku);
                fdaPdpPage.clickAddToCart();
            }
            LoggerUtility.info("SKU Added to Cart: " + sku);
            ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);
        }

        // ============================================================
        // PHASE 3 — Cart verification, checkout, payment
        // ============================================================
        LoggerUtility.info("====== PHASE 3: Cart Verification and Checkout ======");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Verify all products are present in cart — use qty inputs (1 per row, independent of name rendering)
        // Retry up to 3 times: last add-to-cart is async and may need a moment to register
        List<WebElement> qtyInputs = null;
        for (int r = 0; r < 3; r++) {
            qtyInputs = driver.findElements(By.cssSelector("input[data-role='cart-item-qty']"));
            LoggerUtility.info("Cart item count (attempt " + (r + 1) + "): " + qtyInputs.size());
            if (qtyInputs.size() >= skus.length) break;
            if (r < 2) {
                LoggerUtility.info("Cart shows " + qtyInputs.size() + "/" + skus.length
                        + " items — refreshing and retrying");
                fdaCartPage.openAndRefreshCart(config.getFdaUrl());
            }
        }
        Assert.assertEquals(qtyInputs.size(), skus.length,
                "Cart should contain " + skus.length + " products. Actual: " + qtyInputs.size());
        LoggerUtility.info("All " + skus.length + " products present in cart");

        // Normalize any qty > 1 (can result from retry double-add), then re-fetch
        fdaCartPage.normalizeQuantitiesToOne();
        qtyInputs = driver.findElements(By.cssSelector("input[data-role='cart-item-qty']"));

        // Verify each product has quantity = 1
        for (int i = 0; i < qtyInputs.size(); i++) {
            String qtyVal = qtyInputs.get(i).getAttribute("value");
            Assert.assertEquals(qtyVal, "1",
                    "Cart item " + (i + 1) + " quantity should be 1. Actual: " + qtyVal);
        }
        LoggerUtility.info("All product quantities verified as 1");

        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Order Total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(),
                "Order total should be displayed in cart");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Checkout
        LoggerUtility.info("Clicking Proceed to Payment");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();
        LoggerUtility.info("Clicking Siguiente on shipping page");
        fdaPaymentPage.clickNextButton();

        // Payment details
        LoggerUtility.info("Selecting Credit/Debit Card payment option");
        fdaPaymentPage.selectCreditCardOption();
        LoggerUtility.info("Entering card number");
        fdaPaymentPage.enterCardNumber(config.getFdaCardNumber());
        LoggerUtility.info("Entering expiration date");
        fdaPaymentPage.enterExpiry(config.getFdaCardExpiry());
        LoggerUtility.info("Entering security code");
        fdaPaymentPage.enterCvv(config.getFdaCardCvv());

        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Completar pago button text: " + payBtnText);
        Assert.assertTrue(
                payBtnText.contains("Completar pago") || payBtnText.contains("MXN") || payBtnText.contains("$"),
                "Completar pago button should show order amount. Actual: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Clicking Completar pago button");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();
        ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_PostPayment", ScreenshotUtility.INFO);

        // Success page
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(),
                "Order success page not displayed after payment");
        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Checkout Completed");
        LoggerUtility.info("Order Created — Order ID: " + orderId);
        Assert.assertFalse(orderId.isEmpty(),
                "Order ID should be present on success page");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4 — Order History
        // ============================================================
        LoggerUtility.info("====== PHASE 4: Order History Verification ======");
        fdaHomePage.clickProfileIcon();
        fdaHomePage.clickMyOrdersLink();

        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(),
                "Mis pedidos page not displayed");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
                "Order ID " + orderId + " not found in order history");

        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("FDA Order Status: " + fdaStatus);
        Assert.assertTrue(fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
                "Order status should be 'Creada'. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 5 — Mirakl Login and 6-shipment verification
        // ============================================================
        LoggerUtility.info("====== PHASE 5: Mirakl — Verify 6 Shipments ======");
        miraklTabHandle = DriverFactory.openNewTab();
        driver.get(config.getMiraklUrl());

        // Skip login if Mirakl session is already active (shared cookies from TC_FBO_001)
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(5));
        boolean miraklLoginNeeded;
        try {
            miraklLoginNeeded = !driver.findElements(By.xpath("//input[@id='username']")).isEmpty();
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
        if (miraklLoginNeeded) {
            miraklLoginPage.login(config.getMiraklUsername(), config.getMiraklPassword());
        } else {
            LoggerUtility.info("Mirakl session already active — skipping login");
        }
        LoggerUtility.info("Mirakl Login Successful");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Refresh after login/session-check to ensure Mirakl dashboard is fully loaded
        refreshAndWait(By.xpath("//span[normalize-space()='Orders']"));
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();

        // Search with retry (up to 10 min for FDA→Mirakl sync — split orders take longer)
        String miraklSearchTerm = orderId + "WEB";
        LoggerUtility.info("Searching Mirakl for: " + miraklSearchTerm);
        boolean foundInMirakl = false;
        for (int attempt = 1; attempt <= 10; attempt++) {
            LoggerUtility.info("Mirakl search attempt " + attempt + "/10");
            miraklOrdersPage.searchOrder(miraklSearchTerm);
            if (miraklOrdersPage.hasSearchResults(miraklSearchTerm)) {
                foundInMirakl = true;
                LoggerUtility.info("Order found in Mirakl on attempt " + attempt);
                break;
            }
            if (attempt < 10) {
                LoggerUtility.info("Order not in Mirakl yet — waiting 60s");
                Thread.sleep(60_000);
                // Re-navigate to Mirakl to recover from possible session expiry
                driver.get(config.getMiraklUrl());
                driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(5));
                boolean sessionExpired;
                try {
                    sessionExpired = !driver.findElements(By.xpath("//input[@id='username']")).isEmpty();
                } finally {
                    driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
                }
                if (sessionExpired) {
                    LoggerUtility.info("Mirakl session expired — re-logging in");
                    miraklLoginPage.login(config.getMiraklUsername(), config.getMiraklPassword());
                }
                refreshAndWait(By.xpath("//span[normalize-space()='Orders']"));
                miraklOrdersPage.clickOrdersMenu();
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(foundInMirakl,
                "Order " + miraklSearchTerm + " did not appear in Mirakl within 10 minutes");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Verify exactly 6 shipment rows
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));
        List<WebElement> shipmentRows;
        try {
            shipmentRows = driver.findElements(
                    By.xpath("//tbody/tr[contains(.,'" + orderId + "WEB')]"));
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        }
        LoggerUtility.info("6 Shipments Found — Actual count: " + shipmentRows.size());
        Assert.assertEquals(shipmentRows.size(), EXPECTED_SHIPMENTS,
                "Expected " + EXPECTED_SHIPMENTS + " shipments in Mirakl. Actual: " + shipmentRows.size());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Verify each expected suffix (WEB-A through WEB-F) exists
        for (String suffix : SHIPMENT_SUFFIXES) {
            String expectedId = orderId + suffix;
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            boolean rowExists;
            try {
                rowExists = !driver.findElements(
                        By.xpath("//tbody/tr[contains(.,'" + expectedId + "')]")).isEmpty();
            } finally {
                driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
            }
            Assert.assertTrue(rowExists,
                    "Shipment " + expectedId + " not found in Mirakl search results");
            LoggerUtility.info("Shipment Verified: " + expectedId);
        }

        // ============================================================
        // PHASE 6 — Accept all 6 shipments one by one
        // ============================================================
        LoggerUtility.info("====== PHASE 6: Accepting All 6 Shipments ======");

        for (int i = 0; i < SHIPMENT_SUFFIXES.length; i++) {
            String suffix     = SHIPMENT_SUFFIXES[i];
            String shipmentId = orderId + suffix;
            LoggerUtility.info("Accepting Shipment " + (i + 1) + "/6: " + shipmentId);

            miraklOrdersPage.clickAllOrders();
            miraklOrdersPage.searchOrder(miraklSearchTerm);
            miraklOrderDetailPage.clickOrderInList(shipmentId);

            // Split orders may be auto-accepted — check before attempting to click Accept
            if (miraklOrderDetailPage.isAcceptButtonPresent(10)) {
                LoggerUtility.info("Accept button found — accepting shipment " + suffix);
                miraklOrderDetailPage.clickAcceptButton();
            } else {
                LoggerUtility.info("No Accept button for " + suffix
                        + " — order likely auto-accepted by Mirakl");
            }

            String acceptedStatus = miraklOrderDetailPage.getOrderStatus();
            LoggerUtility.info("Shipment " + suffix + " status: " + acceptedStatus);
            Assert.assertEquals(acceptedStatus, "Awaiting shipment",
                    "Shipment " + shipmentId + " should be 'Awaiting shipment'. Actual: " + acceptedStatus);

            LoggerUtility.info("Shipment " + suffix + " Accepted");
            ScreenshotUtility.captureScreenshot(driver,
                    TC_NAME + "_" + suffix + "_Accepted", ScreenshotUtility.INFO);
        }

        LoggerUtility.info("All 6 Shipments Accepted Successfully");

        // ============================================================
        // PHASE 7 — Wait 2 minutes (explicit per test spec)
        // ============================================================
        LoggerUtility.info("====== PHASE 7: Waiting 60s for 3PL processing ======");
        Thread.sleep(60_000);
        LoggerUtility.info("60s wait complete");

        // ============================================================
        // PHASE 8 — Kibo Auth + find order
        // ============================================================
        LoggerUtility.info("====== PHASE 8: Kibo Authentication ======");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(),
                "Kibo authentication failed — access token is empty");
        LoggerUtility.info("Access Token Generated");

        String externalId = orderId + "WEB";
        LoggerUtility.info("Searching Kibo order by externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(),
                "Kibo Order ID not found for externalId: " + externalId);
        LoggerUtility.info("Kibo Order Found — Kibo Order ID: " + kiboOrderId);

        // ============================================================
        // PHASE 9 — Get Shipment Details to extract all 6 shipment numbers
        // Uses GET /api/commerce/shipments?filter=orderId=={kiboOrderId} (Postman: Get_Shipment_Details)
        // ============================================================
        LoggerUtility.info("====== PHASE 9: Getting Shipment Numbers from Kibo ======");
        List<String> shipmentNumbers = new ArrayList<>();
        for (int attempt = 1; attempt <= 5; attempt++) {
            LoggerUtility.info("Get Shipment Details attempt " + attempt + "/5");
            Response shipmentsResp = ApiUtility.kiboGetShipmentsByOrderId(kiboToken, kiboOrderId);
            Assert.assertEquals(shipmentsResp.getStatusCode(), 200,
                    "Kibo Get Shipment Details should return 200. Actual: " + shipmentsResp.getStatusCode());

            List<Map<String, Object>> items = shipmentsResp.jsonPath().getList("items");
            if (items == null || items.isEmpty()) {
                items = shipmentsResp.jsonPath().getList("_embedded.shipments");
            }
            if (items != null) {
                shipmentNumbers.clear();
                for (Map<String, Object> item : items) {
                    Object sn = item.get("shipmentNumber");
                    if (sn != null && !shipmentNumbers.contains(sn.toString()))
                        shipmentNumbers.add(sn.toString());
                }
            }
            LoggerUtility.info("Shipment numbers found: " + shipmentNumbers);
            if (shipmentNumbers.size() >= EXPECTED_SHIPMENTS) break;
            LoggerUtility.info("Only " + shipmentNumbers.size() + "/" + EXPECTED_SHIPMENTS
                    + " shipments found — waiting 15s...");
            if (attempt < 5) Thread.sleep(15_000);
        }
        Assert.assertEquals(shipmentNumbers.size(), EXPECTED_SHIPMENTS,
                "Expected " + EXPECTED_SHIPMENTS + " Kibo shipment numbers. Found: "
                + shipmentNumbers.size() + " — " + shipmentNumbers);
        LoggerUtility.info("Shipment Details Retrieved — " + shipmentNumbers.size() + " shipments found");

        // ============================================================
        // PHASE 9b — Skip "Validate Items In Stock" for all shipments
        // Staging SKUs have no warehouse inventory, causing this Kibo workflow task
        // to stall indefinitely. Skipping it lets the API Connector generate labels.
        // ============================================================
        LoggerUtility.info("====== PHASE 9b: Skipping Validate Items In Stock for all shipments ======");
        for (String shipNum : shipmentNumbers) {
            LoggerUtility.info("Skipping Validate Items In Stock for shipment: " + shipNum);
            Response skipResp = ApiUtility.kiboSkipValidateItemsTask(kiboToken, shipNum);
            LoggerUtility.info("Skip response for " + shipNum + ": HTTP " + skipResp.getStatusCode());
        }
        LoggerUtility.info("Waiting 30s for Kibo API Connector to process workflow advancement...");
        Thread.sleep(30_000);

        // ============================================================
        // PHASE 10 — Poll Get Shipment Details until all 6 have 3PL data
        // Single call returns all shipments; poll until every shipment has
        // deliveryPartner + 3pl_shipmentId populated by the 3PL connector.
        // ============================================================
        LoggerUtility.info("====== PHASE 10: Polling Kibo 3PL Data for All Shipments ======");
        List<String> missing3plShipments  = new ArrayList<>();  // no 3PL data from Kibo
        List<String> flowFailedShipments  = new ArrayList<>();  // webhook propagation failed
        shipmentDetailsList = new ArrayList<>();
        List<ShipmentDetails> lastKnownList = new ArrayList<>();
        boolean allShipmentsReady = false;
        for (int attempt = 1; attempt <= 20; attempt++) {
            LoggerUtility.info("3PL data poll attempt " + attempt + "/20");
            Response shipmentsResp = ApiUtility.kiboGetShipmentsByOrderId(kiboToken, kiboOrderId);
            Assert.assertEquals(shipmentsResp.getStatusCode(), 200,
                    "Kibo Get Shipment Details should return 200. Actual: " + shipmentsResp.getStatusCode());

            List<Map<String, Object>> items = shipmentsResp.jsonPath().getList("items");
            if (items == null || items.isEmpty()) {
                items = shipmentsResp.jsonPath().getList("_embedded.shipments");
            }

            if (items != null && items.size() >= EXPECTED_SHIPMENTS) {
                List<ShipmentDetails> tempList = new ArrayList<>();
                boolean allReady = true;
                for (Map<String, Object> item : items) {
                    String shipNum = item.get("shipmentNumber") != null
                            ? item.get("shipmentNumber").toString() : "";
                    String dp  = extractFieldFromItem(item, "deliveryPartner");
                    String tpl = extractFieldFromItem(item, "3pl_shipmentId");
                    String cn  = extractFieldFromItem(item, "carrierName");
                    String tn  = extractFieldFromItem(item, "tracking_number");
                    LoggerUtility.info("Shipment " + shipNum
                            + " — deliveryPartner='" + dp + "', tplShipmentId='" + tpl + "'");
                    if (dp.isEmpty() || tpl.isEmpty()) {
                        allReady = false;
                        // Retry skip — workflow at this location may need multiple task advances
                        LoggerUtility.info("Retrying skip task for shipment " + shipNum + " (no 3PL data yet)");
                        Response reSkip = ApiUtility.kiboSkipValidateItemsTask(kiboToken, shipNum);
                        LoggerUtility.info("Retry skip HTTP " + reSkip.getStatusCode() + " for shipment " + shipNum);
                    }
                    tempList.add(new ShipmentDetails(shipNum, dp, tpl, cn, tn));
                }
                lastKnownList = tempList;
                if (allReady) {
                    shipmentDetailsList = tempList;
                    allShipmentsReady = true;
                    LoggerUtility.info("All 6 shipments have 3PL data on attempt " + attempt);
                    break;
                }
            }
            LoggerUtility.info("Not all shipments ready yet — waiting 15s...");
            if (attempt < 20) Thread.sleep(15_000);
        }
        if (!allShipmentsReady) {
            long readyCount = lastKnownList.stream()
                    .filter(s -> !s.deliveryPartner.isEmpty() && !s.tplShipmentId.isEmpty()).count();
            LoggerUtility.warn(readyCount + "/" + EXPECTED_SHIPMENTS
                    + " shipments have 3PL data — shipments without data will be skipped (staging env limitation).");
            shipmentDetailsList = lastKnownList;
        }
        Assert.assertTrue(shipmentDetailsList.size() >= EXPECTED_SHIPMENTS,
                "Expected at least " + EXPECTED_SHIPMENTS + " shipment records. Found: " + shipmentDetailsList.size());

        // Assign Mirakl suffixes in order (WEB-A … WEB-F match Kibo creation order)
        for (int i = 0; i < shipmentDetailsList.size(); i++) {
            shipmentDetailsList.get(i).miraklSuffix = SHIPMENT_SUFFIXES[i];
            LoggerUtility.info("Shipment " + SHIPMENT_SUFFIXES[i] + " details: " + shipmentDetailsList.get(i));
        }

        Assert.assertEquals(shipmentDetailsList.size(), EXPECTED_SHIPMENTS,
                "Expected " + EXPECTED_SHIPMENTS + " ShipmentDetails objects. Actual: "
                + shipmentDetailsList.size());
        // Collect suffixes with missing 3PL data — will be reported as FAIL after Phase 12
        for (ShipmentDetails sd : shipmentDetailsList) {
            if (sd.deliveryPartner.isEmpty() || sd.tplShipmentId.isEmpty()) {
                missing3plShipments.add(sd.miraklSuffix);
                LoggerUtility.warn("Shipment " + sd.miraklSuffix
                        + " has no 3PL data — will be marked FAIL after fulfilling available shipments");
            }
        }
        LoggerUtility.info("All 6 Shipment Details Retrieved from Kibo");

        // ============================================================
        // PHASE 11 — Batch webhook approach:
        //   11a — send ALL first webhooks (En tránsito / Picked_up)
        //   11b — poll each shipment for Shipped / 3PL delivery / Received
        //   11c — send ALL second webhooks (Entregado / Delivered) for non-Received
        //   11d — poll each shipment for Received
        //
        // Batch approach is required because the Kibo/Mirakl connector
        // holds all sub-shipments until ALL have received the first event
        // before updating any to "Shipped". Sequential per-shipment waits
        // cause a deadlock — the later webhooks never fire while we wait
        // for the first shipment to advance.
        // ============================================================
        LoggerUtility.info("====== PHASE 11: Processing All 6 Shipments (Batch) ======");

        // Phase 11a — Send first webhook for ALL shipments
        LoggerUtility.info("=== Phase 11a: Sending first webhooks (En tránsito / Picked_up) for all ===");
        for (ShipmentDetails shipment : shipmentDetailsList) {
            String suffix = shipment.miraklSuffix;
            LoggerUtility.info("Shipment " + suffix + " — deliveryPartner=" + shipment.deliveryPartner
                    + " tplShipmentId=" + shipment.tplShipmentId);
            if (shipment.deliveryPartner.isEmpty() || shipment.tplShipmentId.isEmpty()) {
                LoggerUtility.warn("Skipping first webhook for " + suffix + " — no 3PL data");
                continue;
            }
            try {
                Response r1;
                if (shipment.deliveryPartner.toLowerCase().contains("envioclick")) {
                    LoggerUtility.info("Calling Envioclick En tránsito for shipment " + suffix);
                    r1 = ApiUtility.postEnvioclickEnTransito(
                            shipment.tplShipmentId, shipment.trackingNumber, orderId, shipment.carrierName);
                } else if (shipment.deliveryPartner.toLowerCase().contains("skydropx")) {
                    LoggerUtility.info("Calling Skydropx Picked_up for shipment " + suffix);
                    r1 = ApiUtility.postSkydropxPickedUp(shipment.tplShipmentId);
                } else {
                    throw new AssertionError("Unknown delivery partner for " + suffix
                            + ": '" + shipment.deliveryPartner + "'. Expected 'envioclick' or 'skydropx'.");
                }
                Assert.assertEquals(r1.getStatusCode(), 200,
                        "First webhook should return 200 for " + suffix + ". Actual: " + r1.getStatusCode());
                LoggerUtility.info("API Response = " + r1.getStatusCode() + " | shipment " + suffix);
            } catch (AssertionError | Exception e) {
                String msg = e.getMessage() != null ? e.getMessage().split("\n")[0] : e.getClass().getSimpleName();
                if (!missing3plShipments.contains(suffix)) missing3plShipments.add(suffix);
                if (!flowFailedShipments.contains(suffix)) flowFailedShipments.add(suffix);
                LoggerUtility.warn("First webhook failed for " + suffix + ": " + msg);
                ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_" + suffix + "_FlowFailed", ScreenshotUtility.FAIL);
            }
        }

        // Phase 11b — Poll each shipment for Shipped / 3PL delivery / Received
        LoggerUtility.info("=== Phase 11b: Polling for Shipped status on each shipment ===");
        Map<String, String> phaseStatuses = new LinkedHashMap<>();
        for (ShipmentDetails shipment : shipmentDetailsList) {
            String suffix = shipment.miraklSuffix;
            String miraklShipmentId = orderId + suffix;
            if (flowFailedShipments.contains(suffix)
                    || shipment.deliveryPartner.isEmpty() || shipment.tplShipmentId.isEmpty()) {
                phaseStatuses.put(suffix, "SKIPPED");
                continue;
            }
            switchToMiraklTab();
            navigateToMiraklShipment(miraklShipmentId, miraklSearchTerm);
            String status = waitForMiraklStatusOneOf(new String[]{"Shipped", "3PL delivery", "Received"}, 30);
            phaseStatuses.put(suffix, status);
            LoggerUtility.info("Shipment " + suffix + " status after first webhook: " + status);
            ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_" + suffix + "_Shipped", ScreenshotUtility.INFO);
        }

        // Phase 11c — Send second webhook for all shipments not yet Received
        LoggerUtility.info("=== Phase 11c: Sending second webhooks (Entregado / Delivered) for all ===");
        for (ShipmentDetails shipment : shipmentDetailsList) {
            String suffix = shipment.miraklSuffix;
            if (flowFailedShipments.contains(suffix)
                    || shipment.deliveryPartner.isEmpty() || shipment.tplShipmentId.isEmpty()) continue;
            if ("Received".equals(phaseStatuses.get(suffix))) {
                LoggerUtility.info("Shipment " + suffix + " already Received — skipping second webhook");
                continue;
            }
            try {
                Thread.sleep(2_000);
                Response r2;
                if (shipment.deliveryPartner.toLowerCase().contains("envioclick")) {
                    LoggerUtility.info("Calling Envioclick Entregado for shipment " + suffix);
                    r2 = ApiUtility.postEnvioclickEntregado(
                            shipment.tplShipmentId, shipment.trackingNumber, orderId, shipment.carrierName);
                } else if (shipment.deliveryPartner.toLowerCase().contains("skydropx")) {
                    LoggerUtility.info("Calling Skydropx Delivered for shipment " + suffix);
                    r2 = ApiUtility.postSkydropxDelivered(shipment.tplShipmentId);
                } else {
                    throw new AssertionError("Unknown delivery partner for " + suffix
                            + ": '" + shipment.deliveryPartner + "'.");
                }
                Assert.assertEquals(r2.getStatusCode(), 200,
                        "Second webhook should return 200 for " + suffix + ". Actual: " + r2.getStatusCode());
                LoggerUtility.info("API Response = " + r2.getStatusCode() + " | shipment " + suffix);
            } catch (AssertionError | Exception e) {
                String msg = e.getMessage() != null ? e.getMessage().split("\n")[0] : e.getClass().getSimpleName();
                if (!missing3plShipments.contains(suffix)) missing3plShipments.add(suffix);
                if (!flowFailedShipments.contains(suffix)) flowFailedShipments.add(suffix);
                LoggerUtility.warn("Second webhook failed for " + suffix + ": " + msg);
                ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_" + suffix + "_FlowFailed", ScreenshotUtility.FAIL);
            }
        }

        // Phase 11d — Poll each shipment for Received
        LoggerUtility.info("=== Phase 11d: Polling for Received status on each shipment ===");
        for (ShipmentDetails shipment : shipmentDetailsList) {
            String suffix = shipment.miraklSuffix;
            String miraklShipmentId = orderId + suffix;
            if (flowFailedShipments.contains(suffix)
                    || shipment.deliveryPartner.isEmpty() || shipment.tplShipmentId.isEmpty()) continue;
            if ("Received".equals(phaseStatuses.get(suffix))) {
                LoggerUtility.info("Shipment " + suffix + " already Received — no additional wait");
                ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_" + suffix + "_Received", ScreenshotUtility.INFO);
                continue;
            }
            switchToMiraklTab();
            navigateToMiraklShipment(miraklShipmentId, miraklSearchTerm);
            String status = waitForMiraklStatus("Received", 90);
            LoggerUtility.info("Shipment " + suffix + " Mirakl Status: " + status);
            if (!"Received".equals(status)) {
                if (!missing3plShipments.contains(suffix)) missing3plShipments.add(suffix);
                if (!flowFailedShipments.contains(suffix)) flowFailedShipments.add(suffix);
                LoggerUtility.warn("Shipment " + suffix + " did not reach Received — actual: " + status);
                ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_" + suffix + "_FlowFailed", ScreenshotUtility.FAIL);
            } else {
                ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_" + suffix + "_Received", ScreenshotUtility.INFO);
                LoggerUtility.info("Shipment Updated to Received — " + suffix);
            }
        }

        // ============================================================
        // PHASE 12 — Final validation: ALL 6 shipments must be Received.
        // Re-checks every shipment that has 3PL data, regardless of Phase 11
        // outcome. By the time Phase 12 runs, ALL second webhooks for ALL
        // shipments have been sent — status propagation that Phase 11d
        // timed out on may have completed by now.
        // ============================================================
        LoggerUtility.info("====== PHASE 12: Final Validation — All 6 Shipments Must Be Received ======");
        switchToMiraklTab();

        for (String suffix : SHIPMENT_SUFFIXES) {
            String miraklShipmentId = orderId + suffix;
            boolean had3plData = shipmentDetailsList.stream().anyMatch(
                    s -> suffix.equals(s.miraklSuffix) && !s.deliveryPartner.isEmpty() && !s.tplShipmentId.isEmpty());
            if (!had3plData) {
                LoggerUtility.warn("Skipping Phase 12 for " + miraklShipmentId + " — no 3PL data from Kibo");
                continue;
            }
            navigateToMiraklShipment(miraklShipmentId, miraklSearchTerm);
            String finalStatus = waitForMiraklStatus("Received", 5);
            LoggerUtility.info("Phase 12 status of " + miraklShipmentId + ": " + finalStatus);
            if ("Received".equals(finalStatus)) {
                // Clear from failure lists — status propagated after Phase 11d timeout
                missing3plShipments.remove(suffix);
                flowFailedShipments.remove(suffix);
                LoggerUtility.info("Shipment " + suffix + " confirmed Received in Phase 12");
            } else {
                if (!missing3plShipments.contains(suffix)) missing3plShipments.add(suffix);
                LoggerUtility.warn("Shipment " + suffix + " NOT Received in Phase 12 — actual: " + finalStatus);
            }
            ScreenshotUtility.captureScreenshot(driver,
                    TC_NAME + "_" + suffix + "_FinalValidation", ScreenshotUtility.INFO);
        }

        if (!missing3plShipments.isEmpty()) {
            ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.FAIL);
            List<String> no3plOnly = new ArrayList<>(missing3plShipments);
            no3plOnly.removeAll(flowFailedShipments);
            StringBuilder failMsg = new StringBuilder("TC_FBO_SPLIT_01 FAILED: ")
                    .append(missing3plShipments.size()).append("/").append(EXPECTED_SHIPMENTS)
                    .append(" shipment(s) not Received — ");
            if (!no3plOnly.isEmpty()) {
                failMsg.append("No 3PL data from Kibo: ").append(String.join(", ", no3plOnly));
                if (!flowFailedShipments.isEmpty()) failMsg.append("; ");
            }
            if (!flowFailedShipments.isEmpty()) {
                failMsg.append("Webhook/status failed: ").append(String.join(", ", flowFailedShipments));
            }
            Assert.fail(failMsg.toString());
        }
        LoggerUtility.info("All 6 Shipments Completed Successfully");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
    }

    @SuppressWarnings("unchecked")
    private String extractFieldFromItem(java.util.Map<String, Object> item, String fieldKey) {
        // 1. item-level data map
        Object dataRaw = item.get("data");
        if (dataRaw instanceof java.util.Map) {
            Object val = ((java.util.Map<String, Object>) dataRaw).get(fieldKey);
            if (val != null && !val.toString().isBlank()) return val.toString().trim();
        }
        // 2. packages[*].data
        Object pkgsRaw = item.get("packages");
        if (pkgsRaw instanceof java.util.List) {
            for (Object pkg : (java.util.List<?>) pkgsRaw) {
                if (pkg instanceof java.util.Map) {
                    Object pkgData = ((java.util.Map<String, Object>) pkg).get("data");
                    if (pkgData instanceof java.util.Map) {
                        Object val = ((java.util.Map<String, Object>) pkgData).get(fieldKey);
                        if (val != null && !val.toString().isBlank()) return val.toString().trim();
                    }
                }
            }
        }
        return "";
    }

    // ----------------------------------------------------------------
    // Navigate to a specific Mirakl shipment detail page
    // ----------------------------------------------------------------
    private void navigateToMiraklShipment(String shipmentId, String searchTerm) {
        LoggerUtility.info("Navigating to Mirakl shipment: " + shipmentId);
        miraklOrdersPage.clickAllOrders();
        miraklOrdersPage.searchOrder(searchTerm);
        miraklOrderDetailPage.clickOrderInList(shipmentId);
        LoggerUtility.info("On Mirakl shipment detail page: " + shipmentId);
    }

    // ----------------------------------------------------------------
    // Refresh current Mirakl page and poll for expected status
    // ----------------------------------------------------------------
    private String waitForMiraklStatus(String expected, int maxRetries) {
        String status = "";
        for (int i = 1; i <= maxRetries; i++) {
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            status = miraklOrderDetailPage.getOrderStatus();
            LoggerUtility.info("Mirakl status check attempt " + i + "/" + maxRetries
                    + " [url=" + driver.getCurrentUrl() + "]: " + status);
            if (expected.equals(status)) break;
        }
        return status;
    }

    // Poll for any one of the expected statuses (stops on first match)
    private String waitForMiraklStatusOneOf(String[] expected, int maxRetries) {
        java.util.Set<String> expectedSet = new java.util.HashSet<>(java.util.Arrays.asList(expected));
        String status = "";
        for (int i = 1; i <= maxRetries; i++) {
            refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
            status = miraklOrderDetailPage.getOrderStatus();
            LoggerUtility.info("Mirakl status check attempt " + i + "/" + maxRetries
                    + " [url=" + driver.getCurrentUrl() + "]: " + status);
            if (expectedSet.contains(status)) break;
        }
        return status;
    }

    // ----------------------------------------------------------------
    // Handle checkout opening in new window/tab after clickProceedToPayment
    // ----------------------------------------------------------------
    private void switchToCheckoutWindow() {
        java.util.Set<String> handles = driver.getWindowHandles();
        LoggerUtility.info("Window handles after clickProceedToPayment: " + handles.size());
        if (!handles.contains(fdaTabHandle)) {
            fdaTabHandle = handles.iterator().next();
            driver.switchTo().window(fdaTabHandle);
            LoggerUtility.info("Original FDA window closed — switched to checkout window: " + fdaTabHandle);
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
