package FDA_Automation_Script.FDA_Automation_Script.tests;

import FDA_Automation_Script.FDA_Automation_Script.base.BaseClass;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAHomePage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDALoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAPDPPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDASearchResultsPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklFileImportPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklLoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklOffersPage;
import FDA_Automation_Script.FDA_Automation_Script.utils.ApiUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.DriverFactory;
import FDA_Automation_Script.FDA_Automation_Script.utils.ExcelUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.PriceUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;

/**
 * TC_E2E_009 — Seller updates an offer's price via Excel import, and that new price is verified
 * end-to-end: Mirakl Seller -> FDA (Adobe Commerce) storefront PDP.
 *
 * Data flow: Mirakl Seller notes the current (pre-update) offer price -> uploads a "Offers" Excel
 * file via File Import -> Seller Offers screen reflects the new price -> FDA storefront search
 * (SKU + Name) and PDP Name/SKU/Price match the Excel data.
 */
public class TC_E2E_009_Test extends BaseClass {

    private static final String TC_NAME = "TC_E2E_009";

    // --- Page Objects (Seller / shared driver) ---
    private MiraklOffersPage miraklOffersPage;
    private MiraklFileImportPage miraklFileImportPage;
    private FDAHomePage fdaHomePage;
    private FDALoginPage fdaLoginPage;
    private FDASearchResultsPage fdaSearchResultsPage;
    private FDAPDPPage fdaPdpPage;

    // --- Dynamic Test Data (captured once, reused everywhere — never hardcoded) ---
    private String sellerShopName;
    private String productSku;
    private String productId;
    private String productName;

    // Reads TC_E2E_009's login test data from environment variables rather than
    // config.properties or hardcoded source constants, per explicit instruction for this TC.
    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable " + name
                + " is not set — required to run TC_E2E_009. Set it before launching the test run.");
        }
        return value;
    }

    // Prompts on the console for the price to use this run, before any Selenium action happens.
    // Requires running mvn in a terminal that supports stdin (this framework already has a
    // human-in-the-loop precedent — see MiraklLoginPage.handleMfaIfRequired()). Pressing Enter
    // with no input keeps the price currently in the Excel file.
    // Maven Surefire forks a separate JVM to run tests, and that forked process's stdin is
    // claimed by Surefire's own fork-communication protocol — it is never connected to whatever
    // is piped/redirected into the outer `mvn` command. Scanner/BufferedReader reads on
    // System.in inside a Surefire-forked test therefore block forever no matter how stdin is fed
    // to the outer process. `-Dtc.e2e.009.price=<value>` bypasses stdin entirely for `mvn test`
    // runs; the interactive prompt below remains as a fallback for running this class directly
    // (e.g. from an IDE) where stdin isn't intercepted by Surefire.
    private static String promptForPrice(String sku, String currentExcelPrice) {
        String sysPropPrice = System.getProperty("tc.e2e.009.price");
        if (sysPropPrice != null && !sysPropPrice.isBlank()) {
            String trimmed = sysPropPrice.trim();
            LoggerUtility.info("Price supplied via -Dtc.e2e.009.price for SKU " + sku + ": " + trimmed);
            return trimmed;
        }
        System.out.println();
        System.out.println("TC_E2E_009 — Enter the new offer price for SKU '" + sku
            + "' (current Excel price: " + currentExcelPrice + "). Press Enter to keep it unchanged:");
        System.out.print("New price: ");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String input = reader.readLine();
            if (input == null || input.isBlank()) {
                LoggerUtility.info("No price entered — keeping existing Excel price: " + currentExcelPrice);
                return currentExcelPrice;
            }
            String trimmed = input.trim();
            LoggerUtility.info("Tester-provided price for SKU " + sku + ": " + trimmed);
            return trimmed;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read price input from console", e);
        }
    }

    // Overrides BaseClass.setupSuite() so Mirakl login goes straight to this TC's own Seller
    // account (env vars) instead of the suite-default mirakl.username/password — per explicit
    // instruction, the suite-default account is never used at all for this TC, not even
    // transiently. BaseClass.java itself is untouched; this is a plain Java method override
    // living entirely in this file.
    //
    // Only the Seller login happens here — per explicit instruction, the FDA storefront is
    // opened later in its own phase, once the Seller task fully completes and the propagation
    // wait is done.
    //
    // NOTE: this override is only safe when TC_E2E_009 runs in its own isolated suite XML (see
    // testng_e2e_009.xml). If it is ever folded into the shared testng.xml alongside the other
    // 24 TCs, this override and the original BaseClass.setupSuite() (invoked via those other
    // classes) would both run in the same suite, logging into Mirakl on the shared tab twice.
    @BeforeSuite
    public void setupSuite() {
        LoggerUtility.info("TC_E2E_009 @BeforeSuite: Overriding BaseClass.setupSuite() — Mirakl login "
            + "uses the Seller account from MIRAKL_SELLER_USERNAME/PASSWORD, not the suite default");

        driver = DriverFactory.createDriver(false);
        miraklTabHandle = driver.getWindowHandle();
        LoggerUtility.info("TC_E2E_009 @BeforeSuite: Browser launched. Mirakl tab handle: " + miraklTabHandle);

        driver.get(config.getMiraklUrl());
        MiraklLoginPage sellerLogin = new MiraklLoginPage(driver);
        String sellerUser = requireEnv("MIRAKL_SELLER_USERNAME");
        sellerLogin.login(sellerUser, requireEnv("MIRAKL_SELLER_PASSWORD"));
        LoggerUtility.info("TC_E2E_009 @BeforeSuite: Mirakl login as Seller (" + sellerUser + ") complete");
        LoggerUtility.info("TC_E2E_009 @BeforeSuite: Setup complete (FDA storefront login deferred to its own phase)");
    }

    @BeforeClass
    public void initPageObjects() {
        miraklOffersPage = new MiraklOffersPage(driver);
        miraklFileImportPage = new MiraklFileImportPage(driver);
        fdaHomePage = new FDAHomePage(driver);
        fdaLoginPage = new FDALoginPage(driver);
        fdaSearchResultsPage = new FDASearchResultsPage(driver);
        fdaPdpPage = new FDAPDPPage(driver);
        LoggerUtility.info("TC_E2E_009: All page objects initialized — Mirakl Seller session already "
            + "established in @BeforeSuite, no session swap needed");
    }

    @Test(testName = TC_NAME,
          description = "Verify a Seller-updated offer price (via Excel import) is correctly "
              + "reflected in Mirakl Seller and on the FDA storefront (search + PDP "
              + "Name/SKU/Price)")
    public void tc_e2e_009_seller_update_offer_price_via_excel() throws InterruptedException {

        LoggerUtility.info("TC_E2E_009 execution started");

        // ============================================================
        // PHASE 0: Tester Input — ask for the new offer price and write it into the Excel file
        // before anything else runs, so the imported file reflects the tester's chosen price.
        // ============================================================
        LoggerUtility.info("===== PHASE 0: Tester Input — New Offer Price =====");
        String excelFilePath = config.get("excel.offer.file.path");
        ExcelUtility.OfferData excelOfferBeforePrompt = ExcelUtility.readFirstOffer(excelFilePath);
        String newPrice = promptForPrice(excelOfferBeforePrompt.sku, excelOfferBeforePrompt.price);
        ExcelUtility.updatePrice(excelFilePath, excelOfferBeforePrompt.sku, newPrice);

        // ============================================================
        // PHASE 1: Seller — note Shop name, SKU, and current (pre-update) price
        // ============================================================
        LoggerUtility.info("===== PHASE 1: Mirakl Seller — Note Shop, SKU, Current Price =====");

        // Step 1: Login to Mirakl as Seller (already established in @BeforeSuite)
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        LoggerUtility.info("Step 1: Reusing Mirakl Seller session established in @BeforeSuite: "
            + requireEnv("MIRAKL_SELLER_USERNAME"));

        // Step 2: Note the Seller/Shop name (captured for the test summary log)
        sellerShopName = miraklOffersPage.getShopName();
        LoggerUtility.info("Step 2: Captured Seller/Shop name: " + sellerShopName);
        Assert.assertFalse(sellerShopName.isEmpty(), "Seller/Shop name should not be empty | TC: " + TC_NAME);

        // Step 3: From the Excel file, note the product SKU and the tester-provided new price
        // (already written into the file in Phase 0)
        ExcelUtility.OfferData offer = ExcelUtility.readFirstOffer(excelFilePath);
        productSku = offer.sku;
        productId = offer.productId;
        LoggerUtility.info("Step 3: Excel product SKU: " + productSku + " | Product ID: " + productId
            + " | Excel price: " + offer.price);
        Assert.assertFalse(productSku.isEmpty(), "Excel product SKU should not be empty | TC: " + TC_NAME);
        Assert.assertFalse(productId == null || productId.isEmpty(),
            "Excel Product ID should not be empty | TC: " + TC_NAME);

        // Step 4: Prices and stock -> Offers -> search by Product ID -> note current price.
        // Confirmed via a real run (2026-09-10): hasResults() only checks "row 1 exists" — right
        // after a search fires, the grid can still be showing its previous unfiltered/paginated
        // state (many rows) while the AJAX filter is in flight, and row 1 in that state can be a
        // completely unrelated offer (observed: a different product entirely). Per explicit
        // instruction, no Offer SKU/product-name comparison is used here — instead, wait for the
        // grid to narrow to exactly one row, which only happens once the Product ID search has
        // actually taken effect (a unique match), before trusting that row's price/name.
        miraklOffersPage.navigateToOffers();
        miraklOffersPage.searchByProductId(productId);
        for (int attempt = 1; attempt <= 5; attempt++) {
            if (miraklOffersPage.getResultsCount() == 1) {
                break;
            }
            LoggerUtility.info("Step 4: Offers grid not yet narrowed to a single match for Product ID "
                + productId + " (attempt " + attempt + "/5, saw " + miraklOffersPage.getResultsCount()
                + " row(s)) — waiting 2 seconds...");
            Thread.sleep(2_000);
        }
        Assert.assertEquals(miraklOffersPage.getResultsCount(), 1,
            "Offer for Product ID " + productId + " should resolve to exactly one row in Seller Offers | TC: " + TC_NAME);
        String priceBeforeUpdate = miraklOffersPage.getFirstResultPrice();
        productName = miraklOffersPage.getFirstResultProductName();
        LoggerUtility.info("Step 4: Current Seller offer price (before update): " + priceBeforeUpdate
            + " | Product name: " + productName);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 5: Wait 3 seconds
        Thread.sleep(3_000);

        // ============================================================
        // PHASE 2: Seller — Import Excel Offer File
        // ============================================================
        LoggerUtility.info("===== PHASE 2: Mirakl Seller — Import Offer File =====");

        // Step 6: Prices and Stock -> File Import
        miraklFileImportPage.navigateToFileImport();

        // Steps 7-8: "Select File" triggers a native OS dialog Selenium can't control, so it's
        // bypassed — the path is sent straight to the underlying <input type="file">.
        miraklFileImportPage.uploadOfferFile(excelFilePath);
        LoggerUtility.info("Steps 7-8: Selected offer Excel file: " + excelFilePath);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 9: File Content = Offers, click Import
        miraklFileImportPage.selectFileContent("Offers");
        miraklFileImportPage.clickImport();
        // The transient "File imported" banner is checked best-effort/non-blocking only — a real
        // run (2026-09-10) showed it does not reliably appear before the form resets. The
        // authoritative check is the "Track offer imports" report poll below.
        if (miraklFileImportPage.isImportStatusMessageDisplayed()) {
            LoggerUtility.info("Step 9: Import submitted — banner status: " + miraklFileImportPage.getImportStatusMessage());
        } else {
            LoggerUtility.info("Step 9: Import submitted — banner not observed (non-blocking, expected per known Mirakl UI timing)");
        }
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Wait until the import process actually completes — polls the "Track offer imports"
        // report (every 8s, up to 2 min) for a terminal status, then asserts 0 failed rows.
        String importStatus = miraklFileImportPage.waitForImportCompletion(
            Duration.ofMinutes(2), Duration.ofSeconds(8));
        LoggerUtility.info("Import reached terminal status: " + importStatus);
        int failedCount = miraklFileImportPage.getLatestImportFailedCount();
        Assert.assertEquals(failedCount, 0,
            "Import should complete with 0 failed rows. Status=" + importStatus
                + " | Failed=" + failedCount + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);

        // ============================================================
        // PHASE 3: Seller — Verify Price Updated
        // ============================================================
        LoggerUtility.info("===== PHASE 3: Mirakl Seller — Verify Updated Price =====");

        // Steps 10-11: Refresh, click Offers, search the SKU, and check the price — checked only
        // once per explicit instruction (no outer retry/wait loop), then proceed straight to the
        // FDA storefront phase regardless of the outcome here.
        driver.navigate().refresh();
        miraklOffersPage.navigateToOffers();
        miraklOffersPage.searchByProductId(productId);
        // Keep the short inner settle-check (no re-search, just re-reading the same result) so the
        // one search attempt isn't read mid-AJAX-response — see Step 4 above for why.
        boolean sellerPostImportSettled = false;
        for (int settleAttempt = 1; settleAttempt <= 5; settleAttempt++) {
            if (miraklOffersPage.getResultsCount() == 1) {
                sellerPostImportSettled = true;
                break;
            }
            if (settleAttempt < 5) Thread.sleep(2_000);
        }
        Assert.assertTrue(sellerPostImportSettled,
            "Offer for Product ID " + productId + " not found in Seller Offers after import | TC: " + TC_NAME);
        String priceAfterUpdate = miraklOffersPage.getFirstResultPrice();
        LoggerUtility.info("Step 11: Seller offer price after import: " + priceAfterUpdate);
        Assert.assertFalse(PriceUtility.pricesEqual(priceBeforeUpdate, priceAfterUpdate),
            "Offer price should have changed after Excel import. Before=" + priceBeforeUpdate
                + " | After=" + priceAfterUpdate + " | TC: " + TC_NAME);
        Assert.assertTrue(PriceUtility.pricesEqual(priceAfterUpdate, offer.price),
            "Updated Seller offer price should match Excel price. Excel=" + offer.price
                + " | Seller=" + priceAfterUpdate + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);

        // ============================================================
        // PHASE 3B: Push Offers to Empathy — REST API trigger
        // ============================================================
        LoggerUtility.info("===== PHASE 3B: Push Offers to Empathy (REST API) =====");

        // Triggers the Mirakl-side sync that propagates the Seller's updated offer price to the
        // FDA storefront's catalog/search index (Empathy). GET, Cookie-only auth, no body —
        // matches the cancelFullOrder/cancelShipment Cookie-only pattern in ApiUtility.
        Response pushToEmpathyResponse = ApiUtility.pushOffersToEmpathy();
        Assert.assertEquals(200, pushToEmpathyResponse.getStatusCode(),
            "Push Offers to Empathy should return a 200 status. Actual=" + pushToEmpathyResponse.getStatusCode()
                + " | TC: " + TC_NAME);

        // ============================================================
        // PHASE 4: FDA Storefront — Login, Search by Product Name, PLP + PDP Validation
        // ============================================================
        LoggerUtility.info("===== PHASE 4: FDA Storefront — Login, Search by Product Name, PLP + PDP =====");

        // Push Offers to Empathy (Phase 3B) already returned 200 — that's the deterministic signal
        // the sync completed, so search immediately rather than blind-waiting on a fixed sleep.
        fdaTabHandle = DriverFactory.openNewTab();
        fdaHomePage.navigateTo(config.getFdaUrl());
        fdaHomePage.clickProfileIcon();
        fdaHomePage.clickLoginLink();
        fdaLoginPage.login(config.getFdaUsername(), config.getFdaPassword());
        Assert.assertTrue(fdaHomePage.isLoggedIn(), "FDA storefront login should succeed | TC: " + TC_NAME);
        LoggerUtility.info("FDA storefront login successful");

        // Search using the Product Name only, per explicit instruction (2026-09-15) — a Product ID
        // search on this storefront was found to be unreliable (sometimes falls back to a generic,
        // unrelated result set instead of the real match), whereas a Product Name search reliably
        // surfaces a PLP grid to click through to the PDP.
        fdaHomePage.navigateTo(config.getFdaUrl());
        fdaHomePage.enterSearchQuery(productName);
        fdaHomePage.pressSearchEnter();
        Assert.assertTrue(fdaSearchResultsPage.isProductPresent(productName),
            "Product not found in FDA search results for name: " + productName + " | TC: " + TC_NAME);
        LoggerUtility.info("Product confirmed present in Product Name search results");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Check the price shown on the PLP (results grid) card. Per explicit instruction
        // (2026-09-15): the price can lag behind propagation even after Push Offers to Empathy
        // (Phase 3B) returns 200 — poll by refreshing and re-reading (scroll-into-view happens
        // inside getResultItemPrice() itself) rather than a single one-shot check.
        String plpPrice = null;
        if (fdaSearchResultsPage.isResultsGridDisplayed()) {
            fdaSearchResultsPage.scrollToTop();
            boolean plpMatched = false;
            for (int attempt = 1; attempt <= 5; attempt++) {
                plpPrice = fdaSearchResultsPage.getResultItemPrice(productName);
                plpMatched = PriceUtility.pricesEqual(plpPrice, offer.price);
                if (plpMatched) {
                    break;
                }
                LoggerUtility.info("PLP price not yet updated (attempt " + attempt + "/5, saw " + plpPrice
                    + ", expected " + offer.price + ") — refreshing and retrying...");
                Thread.sleep(3_000);
                driver.navigate().refresh();
            }
            Assert.assertTrue(plpMatched, "PLP price should match the Seller-updated price after retries. "
                + "Expected=" + offer.price + " | Last seen PLP=" + plpPrice + " | TC: " + TC_NAME);
        } else {
            LoggerUtility.info("Product Name search redirected straight to PDP — no PLP grid to check");
        }

        // Click through to the PDP and verify Product Name, SKU, and Price there too
        fdaSearchResultsPage.openMatchingResult(productName);
        String pdpPrice = verifyProductDetailPage(productName, productId, offer.price);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);

        // Final summary log — never logs passwords
        LoggerUtility.info("TC_E2E_009 completed successfully");
        LoggerUtility.info("  Test Case          : TC_E2E_009");
        LoggerUtility.info("  Seller/Shop Name   : " + sellerShopName);
        LoggerUtility.info("  Product SKU        : " + productSku);
        LoggerUtility.info("  Product ID         : " + productId);
        LoggerUtility.info("  Product Name       : " + productName);
        LoggerUtility.info("  Price Before Update: " + priceBeforeUpdate);
        LoggerUtility.info("  Price After Update : " + priceAfterUpdate);
        LoggerUtility.info("  PLP Price          : " + plpPrice);
        LoggerUtility.info("  PDP Price          : " + pdpPrice);
        LoggerUtility.info("  Final Test Status  : PASS");
    }

    // Opens on whatever PDP is currently displayed and verifies Product Name, SKU (Product ID),
    // and Price against the expected values — shared by both the Product-SKU-search and
    // Product-Name-search verification blocks in Phase 5/6 so the checks aren't duplicated.
    private String verifyProductDetailPage(String expectedName, String expectedSku, String expectedPrice)
            throws InterruptedException {
        Assert.assertTrue(fdaPdpPage.isDisplayed(), "PDP not displayed | TC: " + TC_NAME);

        String pdpProductName = fdaPdpPage.getProductName();
        Assert.assertEquals(pdpProductName.trim(), expectedName.trim(),
            "PDP Product Name should equal expected Product Name. Expected=" + expectedName
                + " | Actual=" + pdpProductName + " | TC: " + TC_NAME);

        String pdpSku = fdaPdpPage.getProductSku();
        Assert.assertEquals(pdpSku.trim(), expectedSku.trim(),
            "PDP SKU should equal expected Product SKU. Expected=" + expectedSku + " | PDP=" + pdpSku
                + " | TC: " + TC_NAME);

        // The main buy-box price reflects the winning/cheapest offer among all 3P sellers for this
        // product, which is not necessarily this test's Seller (per explicit instruction,
        // 2026-09-15 — confirmed via a real Push-Offers-to-Empathy API response the same day where
        // this test's seller had "winner": false, priced above the actual winning offer) — check
        // the main price first, falling back to this seller's own offer in the PDP's "other
        // sellers" list. The price can also lag behind propagation even after Push Offers to
        // Empathy (Phase 3B) returns 200, same as the PLP check above — poll by refreshing and
        // re-reading rather than a single one-shot check.
        String pdpPrice = readPdpPriceForSeller(expectedPrice);
        boolean pdpMatched = PriceUtility.pricesEqual(pdpPrice, expectedPrice);
        for (int attempt = 1; !pdpMatched && attempt <= 5; attempt++) {
            LoggerUtility.info("PDP price not yet updated (attempt " + attempt + "/5, saw " + pdpPrice
                + ", expected " + expectedPrice + ") — refreshing and retrying...");
            Thread.sleep(3_000);
            driver.navigate().refresh();
            Assert.assertTrue(fdaPdpPage.isDisplayed(), "PDP not displayed after refresh | TC: " + TC_NAME);
            pdpPrice = readPdpPriceForSeller(expectedPrice);
            pdpMatched = PriceUtility.pricesEqual(pdpPrice, expectedPrice);
        }
        Assert.assertTrue(pdpMatched,
            "PDP Price (main buy-box or this seller's own offer) should equal New Price after retries. "
                + "Expected=" + expectedPrice + " | Last seen PDP=" + pdpPrice + " | Seller=" + sellerShopName
                + " | TC: " + TC_NAME);

        return pdpPrice;
    }

    // Reads the main buy-box PDP price, falling back to this seller's own offer in the "other
    // sellers" list if the main price doesn't match expectedPrice — see verifyProductDetailPage()
    // for why (the winning/cheapest seller isn't necessarily this test's seller).
    private String readPdpPriceForSeller(String expectedPrice) {
        String pdpPrice = fdaPdpPage.getProductPrice();
        if (!PriceUtility.pricesEqual(pdpPrice, expectedPrice)) {
            String sellerOfferPrice = fdaPdpPage.getSellerOfferPrice(sellerShopName);
            if (sellerOfferPrice != null) {
                LoggerUtility.info("Main PDP price (" + pdpPrice + ") belongs to a different (winning) "
                    + "seller — checking this test's own seller ('" + sellerShopName + "') offer instead: "
                    + sellerOfferPrice);
                pdpPrice = sellerOfferPrice;
            }
        }
        return pdpPrice;
    }
}
