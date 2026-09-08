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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TC_FBO_004_Test extends BaseClass {

    private static final String TC_NAME        = "TC_FBO_004";

    // TC-specific test data (different FDA user from the suite default)
    private static final String TC_FDA_USER    = "mgowda@kognivera.com";
    private static final String TC_FDA_PASS    = "Mithun@12345";
    private static final String TC_SKU_1       = "78078094274";
    private static final String TC_SKU_2       = "78078094107";
    private static final String TC_CARD_NUMBER = "5454545454545454";
    private static final String TC_CARD_EXPIRY = "03/30";
    private static final String TC_CARD_CVV    = "737";
    private static final int    TC_QUANTITY    = 2;

    // Per-shipment data captured from Kibo + Mirakl detail page URLs
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

    // --- Dynamic Test Data (captured once, reused everywhere) ---
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
        LoggerUtility.info("TC_FBO_004: All page objects initialized");

        // TC_FBO_004 uses a different FDA account — swap sessions
        LoggerUtility.info("TC_FBO_004: Switching FDA session to: " + TC_FDA_USER);
        switchToFDATab();
        fdaHomePage.navigateTo(config.getFdaUrl());
        fdaHomePage.logout();
        fdaHomePage.navigateTo(config.getFdaUrl());
        fdaHomePage.clickProfileIcon();
        fdaHomePage.clickLoginLink();
        fdaLoginPage.login(TC_FDA_USER, TC_FDA_PASS);
        LoggerUtility.info("TC_FBO_004: FDA login as " + TC_FDA_USER + " successful");
    }

    @AfterClass(alwaysRun = true)
    public void restoreOriginalFdaSession() {
        LoggerUtility.info("TC_FBO_004 @AfterClass: Restoring original FDA session");
        try {
            switchToFDATab();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.logout();
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.clickProfileIcon();
            fdaHomePage.clickLoginLink();
            fdaLoginPage.login(config.getFdaUsername(), config.getFdaPassword());
            LoggerUtility.info("TC_FBO_004 @AfterClass: FDA session restored as " + config.getFdaUsername());
        } catch (Exception e) {
            LoggerUtility.error("TC_FBO_004 @AfterClass: Failed to restore FDA session: " + e.getMessage());
        }
    }

    @Test(testName = TC_NAME, groups = {"FBO"},
          description = "Verify 2 products qty=2 each from 1 3P seller → 2 Mirakl shipments (WEB-A + WEB-B) → full fulfillment lifecycle")
    public void tc_fbo_004_dual_product_qty2_each_full_fulfillment() throws InterruptedException {

        LoggerUtility.info("TC_FBO_004 execution started");

        // ============================================================
        // PHASE 1: FDA — Product 1 (qty=2) → cart
        // ============================================================

        switchToFDATab();
        LoggerUtility.info("Step 1: Verifying FDA Home Page");
        fdaHomePage.navigateTo(config.getFdaUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Pre-test: clear leftover cart items
        LoggerUtility.info("Pre-test: Clearing any leftover cart items");
        fdaHomePage.navigateTo(config.getFdaUrl() + "checkout/cart");
        fdaCartPage.removeAllItems();
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ---- Product 1 ----
        LoggerUtility.info("Step 2: Clicking search field for Product 1");
        LoggerUtility.info("Step 3: Entering Product 1 SKU: " + TC_SKU_1);
        fdaHomePage.enterSearchQuery(TC_SKU_1);
        LoggerUtility.info("Step 4: Pressing Enter to search");
        fdaHomePage.pressSearchEnter();
        LoggerUtility.info("Product 1 SKU " + TC_SKU_1 + " searched successfully");

        LoggerUtility.info("Step 5: Verifying Product Details Page for Product 1");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "PDP not displayed after searching Product 1 SKU: " + TC_SKU_1);

        LoggerUtility.info("Step 6: Verifying Agregar al carrito button is enabled for Product 1");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "Add to cart button not enabled for Product 1 SKU: " + TC_SKU_1);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 7: Increasing Product 1 quantity to " + TC_QUANTITY);
        fdaPdpPage.increaseQuantity();

        String p1PdpQty = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 8: Product 1 quantity after increment: " + p1PdpQty);
        Assert.assertEquals(p1PdpQty, String.valueOf(TC_QUANTITY),
            "Product 1 quantity on PDP should be " + TC_QUANTITY + ". Actual: " + p1PdpQty);
        LoggerUtility.info("Product 1 quantity changed to " + TC_QUANTITY);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 9: Clicking Agregar al carrito for Product 1");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product 1 added to cart with quantity " + TC_QUANTITY);

        // Navigate home before searching Product 2
        fdaHomePage.navigateTo(config.getFdaUrl());

        // ---- Product 2 ----
        LoggerUtility.info("Step 10: Clicking search field for Product 2");
        LoggerUtility.info("Step 11: Entering Product 2 SKU: " + TC_SKU_2);
        fdaHomePage.enterSearchQuery(TC_SKU_2);
        LoggerUtility.info("Step 12: Pressing Enter to search");
        fdaHomePage.pressSearchEnter();
        LoggerUtility.info("Product 2 SKU " + TC_SKU_2 + " searched successfully");

        LoggerUtility.info("Step 13: Verifying Product Details Page for Product 2");
        Assert.assertTrue(fdaPdpPage.isDisplayed(),
            "PDP not displayed after searching Product 2 SKU: " + TC_SKU_2);

        LoggerUtility.info("Step 14: Verifying Agregar al carrito button is enabled for Product 2");
        Assert.assertTrue(fdaPdpPage.isAddToCartEnabled(),
            "Add to cart button not enabled for Product 2 SKU: " + TC_SKU_2);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 15: Increasing Product 2 quantity to " + TC_QUANTITY);
        fdaPdpPage.increaseQuantity();

        String p2PdpQty = fdaPdpPage.getQuantity();
        LoggerUtility.info("Step 16: Product 2 quantity after increment: " + p2PdpQty);
        Assert.assertEquals(p2PdpQty, String.valueOf(TC_QUANTITY),
            "Product 2 quantity on PDP should be " + TC_QUANTITY + ". Actual: " + p2PdpQty);
        LoggerUtility.info("Product 2 quantity changed to " + TC_QUANTITY);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 17: Clicking Agregar al carrito for Product 2");
        fdaPdpPage.clickAddToCart();
        LoggerUtility.info("Product 2 added to cart with quantity " + TC_QUANTITY);

        // ============================================================
        // PHASE 2: Cart Validation
        // ============================================================

        LoggerUtility.info("Step 18: Opening Mi carrito (cart page)");
        fdaCartPage.openAndRefreshCart(config.getFdaUrl());

        // Step 19: Verify 2 distinct products in cart
        int itemCount = fdaCartPage.getItemCount();
        LoggerUtility.info("Step 19: Cart item count: " + itemCount);
        Assert.assertEquals(itemCount, 2,
            "Cart should contain exactly 2 products. Actual: " + itemCount);
        LoggerUtility.info("Cart contains 2 products");

        // Step 20: Verify product names are present
        List<String> productNames = fdaCartPage.getAllProductNames();
        LoggerUtility.info("Step 20: Cart product names: " + productNames);
        Assert.assertEquals(productNames.size(), 2,
            "Cart should have 2 product name entries. Actual: " + productNames.size());
        Assert.assertFalse(productNames.get(0).isEmpty(),
            "Product 1 name should not be empty in cart");
        Assert.assertFalse(productNames.get(1).isEmpty(),
            "Product 2 name should not be empty in cart");

        // Step 21: Verify both quantities are 2
        List<String> cartQtys = fdaCartPage.getAllQuantities();
        LoggerUtility.info("Step 21: Cart quantities: " + cartQtys);
        Assert.assertEquals(cartQtys.size(), 2,
            "Cart should have 2 quantity inputs. Actual: " + cartQtys.size());
        Assert.assertEquals(cartQtys.get(0), String.valueOf(TC_QUANTITY),
            "Product 1 cart quantity should be " + TC_QUANTITY + ". Actual: " + cartQtys.get(0));
        Assert.assertEquals(cartQtys.get(1), String.valueOf(TC_QUANTITY),
            "Product 2 cart quantity should be " + TC_QUANTITY + ". Actual: " + cartQtys.get(1));
        LoggerUtility.info("Product quantities validated — Product 1 qty=" + cartQtys.get(0)
            + " | Product 2 qty=" + cartQtys.get(1));

        // Step 22: Verify order total
        orderTotal = fdaCartPage.getOrderTotal();
        LoggerUtility.info("Step 22: Order total: " + orderTotal);
        Assert.assertFalse(orderTotal.isEmpty(),
            "Order total should be displayed in cart");
        Assert.assertTrue(orderTotal.contains("1,600") || orderTotal.contains("1600"),
            "Order total should reflect MXN$1,600.00. Actual: " + orderTotal);
        LoggerUtility.info("Order total = " + orderTotal);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: Checkout + Payment
        // ============================================================

        LoggerUtility.info("Step 23: Clicking Proceed to payment");
        fdaCartPage.clickProceedToPayment();
        switchToCheckoutWindow();

        LoggerUtility.info("Step 24: Clicking Siguiente on shipping page");
        fdaPaymentPage.clickNextButton();

        LoggerUtility.info("Step 25: Selecting Pago con Tarjeta Crédito/Débito");
        fdaPaymentPage.selectCreditCardOption();

        LoggerUtility.info("Step 26-27: Clicking card number field and entering card number");
        fdaPaymentPage.enterCardNumber(TC_CARD_NUMBER);
        LoggerUtility.info("Step 28-29: Clicking expiry field and entering expiry: " + TC_CARD_EXPIRY);
        fdaPaymentPage.enterExpiry(TC_CARD_EXPIRY);
        LoggerUtility.info("Step 30-31: Clicking CVV field and entering CVV");
        fdaPaymentPage.enterCvv(TC_CARD_CVV);

        String payBtnText = fdaPaymentPage.getCompletePaymentButtonText();
        LoggerUtility.info("Step 32: Completar pago button text: " + payBtnText);
        Assert.assertTrue(
            payBtnText.contains("Completar pago") || payBtnText.contains("MXN") || payBtnText.contains("$"),
            "Completar pago button should display the order total. Actual: " + payBtnText);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 33: Clicking Completar pago button");
        fdaPaymentPage.clickCompletePayment();
        fdaPaymentPage.handle3dsChallenge();

        LoggerUtility.info("Step 34: Verifying order success page");
        Assert.assertTrue(fdaSuccessPage.isSuccessPageDisplayed(),
            "Order success page not displayed after payment");

        orderId = fdaSuccessPage.getOrderId();
        LoggerUtility.info("Step 35: Order ID = " + orderId);
        Assert.assertFalse(orderId.isEmpty(),
            "Order ID should be present on success page");
        LoggerUtility.info("Order created successfully — Order ID = " + orderId);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 4: FDA Order History
        // ============================================================

        LoggerUtility.info("Step 36: Clicking Mi cuenta profile icon");
        fdaHomePage.clickProfileIcon();
        LoggerUtility.info("Step 37: Clicking Mis pedidos link");
        fdaHomePage.clickMyOrdersLink();

        LoggerUtility.info("Step 38: Verifying Mis pedidos page");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderHistoryDisplayed(),
            "Mis pedidos page not displayed");

        LoggerUtility.info("Step 39: Verifying Order ID " + orderId + " in order history");
        Assert.assertTrue(fdaOrderHistoryPage.isOrderPresent(orderId),
            "Order ID " + orderId + " not found in order history");

        String fdaStatus = fdaOrderHistoryPage.getOrderStatus(orderId);
        LoggerUtility.info("Step 40: FDA order status: " + fdaStatus);
        Assert.assertTrue(
            fdaStatus.equals("Creada") || fdaStatus.equals("Pendiente"),
            "FDA order status should be 'Creada' or 'Pendiente'. Actual: " + fdaStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 5: MIRAKL — Verify WEB-A + WEB-B, Accept Both
        // ============================================================

        LoggerUtility.info("Step 41: Switching to Mirakl tab — session active from suite setup");
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Step 42: Clicking Orders menu in Mirakl");
        miraklOrdersPage.clickOrdersMenu();
        LoggerUtility.info("Step 43: Clicking All Orders");
        miraklOrdersPage.clickAllOrders();

        String miraklSearchTerm = orderId + "WEB";
        String shipmentRefA     = orderId + "WEB-A";
        String shipmentRefB     = orderId + "WEB-B";

        LoggerUtility.info("Step 44-45: Searching Mirakl for order: " + miraklSearchTerm);
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
                LoggerUtility.info("Waiting 60 seconds before next attempt...");
                Thread.sleep(60_000);
                miraklOrdersPage.clickAllOrders();
            }
        }
        Assert.assertTrue(orderFoundInMirakl,
            "Order " + miraklSearchTerm + " did not appear in Mirakl within 5 minutes");

        // Step 46: Verify both WEB-A and WEB-B are present
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefA),
            "Mirakl shipment WEB-A not found: " + shipmentRefA);
        Assert.assertTrue(miraklOrdersPage.hasSearchResults(shipmentRefB),
            "Mirakl shipment WEB-B not found: " + shipmentRefB);
        LoggerUtility.info("WEB-A shipment found: " + shipmentRefA);
        LoggerUtility.info("WEB-B shipment found: " + shipmentRefB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 47: Accept WEB-A
        LoggerUtility.info("Step 47: Opening WEB-A shipment for acceptance: " + shipmentRefA);
        miraklOrderDetailPage.clickOrderInList(shipmentRefA);

        String statusA = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-A status before acceptance: " + statusA);
        Assert.assertEquals(statusA, "Pending acceptance",
            "WEB-A should be 'Pending acceptance'. Actual: " + statusA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        statusA = miraklOrderDetailPage.getOrderStatus();
        Assert.assertEquals(statusA, "Awaiting shipment",
            "WEB-A should be 'Awaiting shipment' after acceptance. Actual: " + statusA);
        String urlShipmentA = driver.getCurrentUrl();
        LoggerUtility.info("WEB-A accepted — status: " + statusA + " | URL: " + urlShipmentA);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Navigate back to All Orders and accept WEB-B
        miraklOrdersPage.clickOrdersMenu();
        miraklOrdersPage.clickAllOrders();
        miraklOrdersPage.searchOrder(miraklSearchTerm);

        LoggerUtility.info("Step 47: Opening WEB-B shipment for acceptance: " + shipmentRefB);
        miraklOrderDetailPage.clickOrderInList(shipmentRefB);

        String statusB = miraklOrderDetailPage.getOrderStatus();
        LoggerUtility.info("WEB-B status before acceptance: " + statusB);
        Assert.assertEquals(statusB, "Pending acceptance",
            "WEB-B should be 'Pending acceptance'. Actual: " + statusB);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        miraklOrderDetailPage.clickAcceptButton();
        refreshAndWait(MIRAKL_ORDER_STATUS_LOCATOR);
        statusB = miraklOrderDetailPage.getOrderStatus();
        Assert.assertEquals(statusB, "Awaiting shipment",
            "WEB-B should be 'Awaiting shipment' after acceptance. Actual: " + statusB);
        String urlShipmentB = driver.getCurrentUrl();
        LoggerUtility.info("WEB-B accepted — status: " + statusB + " | URL: " + urlShipmentB);
        LoggerUtility.info("Both shipments accepted");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 6: KIBO API — Auth + Collect Shipment Data for Both
        // ============================================================

        // Step 49: Kibo authentication
        LoggerUtility.info("Step 49: ========== Kibo Authentication ==========");
        String kiboToken = ApiUtility.kiboAuthenticate();
        Assert.assertFalse(kiboToken.isEmpty(),
            "Kibo access token should not be empty — authentication failed");
        LoggerUtility.info("Kibo authentication successful");

        // Step 50: Find Kibo order ID
        String externalId = orderId + "WEB";
        LoggerUtility.info("Step 50: ========== Find Kibo Order ID ==========");
        LoggerUtility.info("Searching externalId: " + externalId);
        String kiboOrderId = ApiUtility.kiboFindOrderId(kiboToken, externalId);
        Assert.assertFalse(kiboOrderId.isEmpty(),
            "Kibo Order ID not found for externalId: " + externalId);
        LoggerUtility.info("Kibo Order ID: " + kiboOrderId);

        // Step 51: Poll Kibo until both shipments have 3PL data
        LoggerUtility.info("Step 51: ========== Polling Kibo Shipments (2 expected) ==========");
        boolean allShipmentsReady = false;

        for (int attempt = 1; attempt <= 20; attempt++) {
            LoggerUtility.info("Kibo shipment poll attempt " + attempt + "/20");
            Response shipmentsResp = ApiUtility.kiboGetShipmentsByOrderId(kiboToken, kiboOrderId);
            Assert.assertEquals(shipmentsResp.getStatusCode(), 200,
                "Kibo Get Shipments should return 200. Actual: " + shipmentsResp.getStatusCode());

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
                        LoggerUtility.info("Shipment 3PL data not ready — tplShipmentId='"
                            + info.tplShipmentId + "' deliveryPartner='" + info.deliveryPartner + "'");
                    } else {
                        ready.add(info);
                    }
                }

                if (allPopulated && ready.size() >= 2) {
                    // Assign WEB-A / WEB-B by Kibo index (first → WEB-A, second → WEB-B)
                    ready.get(0).shipmentRef     = shipmentRefA;
                    ready.get(0).miraklDetailUrl = urlShipmentA;
                    ready.get(1).shipmentRef     = shipmentRefB;
                    ready.get(1).miraklDetailUrl = urlShipmentB;
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
            "3PL data not fully populated for both shipments within 10 minutes");
        Assert.assertEquals(shipments.size(), 2,
            "Expected exactly 2 shipments from Kibo. Actual: " + shipments.size());

        for (int i = 0; i < shipments.size(); i++) {
            ShipmentInfo info = shipments.get(i);
            LoggerUtility.info("--- Shipment " + (i + 1) + " [" + info.shipmentRef + "] ---");
            LoggerUtility.info("  Delivery Partner : " + info.deliveryPartner);
            LoggerUtility.info("  3PL Shipment ID  : " + info.tplShipmentId);
            LoggerUtility.info("  Carrier          : " + info.carrierName);
            LoggerUtility.info("  Tracking Number  : " + info.trackingNumber);
        }
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 7: Process Both Shipments Independently
        // ============================================================

        // Step 52: Iterate each shipment and dispatch to its carrier flow
        for (ShipmentInfo info : shipments) {
            LoggerUtility.info("========== Processing Shipment: " + info.shipmentRef + " ==========");
            LoggerUtility.info("Delivery Partner: " + info.deliveryPartner);

            // Navigate to this shipment's Mirakl detail page using the captured URL
            switchToMiraklTab();
            driver.get(info.miraklDetailUrl);
            WaitUtility.fluentWait(driver, MIRAKL_ORDER_STATUS_LOCATOR);

            if (info.deliveryPartner.toLowerCase().contains("envioclick")) {
                runEnvioclickFlow(info);
            } else if (info.deliveryPartner.toLowerCase().contains("skydropx")) {
                runSkydropxFlow(info);
            } else {
                Assert.fail("Unknown delivery partner for " + info.shipmentRef
                    + ": " + info.deliveryPartner + ". Expected 'Envioclick' or 'Skydropx'.");
            }
        }

        LoggerUtility.info("TC_FBO_004 completed successfully");
    }

    // ----------------------------------------------------------------
    // Envioclick: En tránsito → Shipped | Entregado → Received
    // ----------------------------------------------------------------
    private void runEnvioclickFlow(ShipmentInfo info) {
        LoggerUtility.info("Calling Envioclick API — En tránsito for: " + info.shipmentRef);
        LoggerUtility.info("EnvioClick API Request — carrier: " + info.carrierName
            + " | idOrder (3pl_shipmentId): " + info.tplShipmentId
            + " | trackingCode: " + info.trackingNumber
            + " | myShipmentReference: " + orderId + "WEB");
        Response enTransitoResp = ApiUtility.postEnvioclickEnTransito(
            info.tplShipmentId, info.trackingNumber, orderId, info.carrierName);
        Assert.assertEquals(enTransitoResp.getStatusCode(), 200,
            "Envioclick En tránsito should return 200 for " + info.shipmentRef
            + ". Actual: " + enTransitoResp.getStatusCode());
        LoggerUtility.info("EnvioClick API Response = " + enTransitoResp.getStatusCode());

        String status = waitForMiraklStatusOneOf(new String[]{"Shipped", "3PL delivery"}, 60);
        LoggerUtility.info("Mirakl status after En tránsito [" + info.shipmentRef + "]: " + status);
        Assert.assertTrue(status.equals("Shipped") || status.equals("3PL delivery"),
            "Mirakl status should be 'Shipped' or '3PL delivery' after En tránsito ["
            + info.shipmentRef + "]. Actual: " + status);
        LoggerUtility.info("Mirakl status = " + status);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Calling Envioclick API — Entregado for: " + info.shipmentRef);
        LoggerUtility.info("EnvioClick API Request — carrier: " + info.carrierName
            + " | idOrder (3pl_shipmentId): " + info.tplShipmentId
            + " | trackingCode: " + info.trackingNumber
            + " | myShipmentReference: " + orderId + "WEB");
        Response entregadoResp = ApiUtility.postEnvioclickEntregado(
            info.tplShipmentId, info.trackingNumber, orderId, info.carrierName);
        Assert.assertEquals(entregadoResp.getStatusCode(), 200,
            "Envioclick Entregado should return 200 for " + info.shipmentRef
            + ". Actual: " + entregadoResp.getStatusCode());
        LoggerUtility.info("EnvioClick API Response = " + entregadoResp.getStatusCode());

        status = waitForMiraklStatus("Received", 60);
        LoggerUtility.info("Mirakl status after Entregado [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Received",
            "Mirakl status should be 'Received' after Entregado ["
            + info.shipmentRef + "]. Actual: " + status);
        LoggerUtility.info("Mirakl status = Received");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
        LoggerUtility.info("Envioclick flow complete — " + info.shipmentRef + " = Received");
    }

    // ----------------------------------------------------------------
    // Skydropx: Picked_up → Shipped | Delivered → Received
    // ----------------------------------------------------------------
    private void runSkydropxFlow(ShipmentInfo info) {
        LoggerUtility.info("Calling Skydropx API — Picked_up for: " + info.shipmentRef);
        LoggerUtility.info("Skydropx API Request — id (3pl_shipmentId): " + info.tplShipmentId);
        Response pickedUpResp = ApiUtility.postSkydropxPickedUp(info.tplShipmentId);
        Assert.assertEquals(pickedUpResp.getStatusCode(), 200,
            "Skydropx Picked_up should return 200 for " + info.shipmentRef
            + ". Actual: " + pickedUpResp.getStatusCode());
        LoggerUtility.info("Skydropx API Response = " + pickedUpResp.getStatusCode());

        String status = waitForMiraklStatus("Shipped", 60);
        LoggerUtility.info("Mirakl status after Picked_up [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Shipped",
            "Mirakl status should be 'Shipped' after Skydropx Picked_up ["
            + info.shipmentRef + "]. Actual: " + status);
        LoggerUtility.info("Mirakl status = Shipped");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        LoggerUtility.info("Calling Skydropx API — Delivered for: " + info.shipmentRef);
        LoggerUtility.info("Skydropx API Request — id (3pl_shipmentId): " + info.tplShipmentId);
        Response deliveredResp = ApiUtility.postSkydropxDelivered(info.tplShipmentId);
        Assert.assertEquals(deliveredResp.getStatusCode(), 200,
            "Skydropx Delivered should return 200 for " + info.shipmentRef
            + ". Actual: " + deliveredResp.getStatusCode());
        LoggerUtility.info("Skydropx API Response = " + deliveredResp.getStatusCode());

        status = waitForMiraklStatus("Received", 60);
        LoggerUtility.info("Mirakl status after Delivered [" + info.shipmentRef + "]: " + status);
        Assert.assertEquals(status, "Received",
            "Mirakl status should be 'Received' after Skydropx Delivered ["
            + info.shipmentRef + "]. Actual: " + status);
        LoggerUtility.info("Mirakl status = Received");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);
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
            LoggerUtility.info("Mirakl status check " + i + "/" + maxRetries + ": " + status);
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
            LoggerUtility.info("Mirakl status check " + i + "/" + maxRetries + ": " + status);
            if (expectedSet.contains(status)) break;
        }
        return status;
    }

    // ----------------------------------------------------------------
    // Extract named custom field from Kibo shipment item map.
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
    // Handle checkout opening in new window/tab after clickProceedToPayment
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
// @AfterMethod navigateToHomePage() is inherited from BaseClass
