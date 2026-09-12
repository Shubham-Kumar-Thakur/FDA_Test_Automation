package FDA_Automation_Script.FDA_Automation_Script.tests;

import FDA_Automation_Script.FDA_Automation_Script.base.BaseClass;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAHomePage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAPDPPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDASearchResultsPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklFileImportPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklLoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklOffersPage;
import FDA_Automation_Script.FDA_Automation_Script.utils.DriverFactory;
import FDA_Automation_Script.FDA_Automation_Script.utils.ExcelUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.PriceUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;

/**
 * TC_E2E_009 — Seller updates an offer's price via Excel import, and that new price is verified
 * end-to-end: Mirakl Seller -> Mirakl Operator -> FDA (Adobe Commerce) storefront PDP.
 *
 * Data flow: Mirakl Seller notes the current (pre-update) offer price -> uploads a "Offers" Excel
 * file via File Import -> Seller Offers screen reflects the new price -> a separate Operator
 * session (own browser) confirms the same price under the Seller's shop filter -> FDA storefront
 * search (SKU + Name) and PDP Name/SKU/Price match the Excel data.
 */
public class TC_E2E_009_Test extends BaseClass {

    private static final String TC_NAME = "TC_E2E_009";

    // --- Page Objects (Seller / shared driver) ---
    private MiraklOffersPage miraklOffersPage;
    private MiraklFileImportPage miraklFileImportPage;
    private FDAHomePage fdaHomePage;
    private FDASearchResultsPage fdaSearchResultsPage;
    private FDAPDPPage fdaPdpPage;

    // --- Dynamic Test Data (captured once, reused everywhere — never hardcoded) ---
    private String sellerShopName;
    private String productSku;
    private String productId;
    private String productName;

    // Separate, independent browser instance — kept fully isolated from the shared suite
    // driver/tabs so the Seller session is never logged out mid-test. Owned entirely by this
    // test: created and logged in during @BeforeSuite, quit in @AfterClass. Never touches
    // DriverFactory's shared ThreadLocal.
    private WebDriver operatorDriver;
    private MiraklOffersPage operatorOffersPage;

    // A third, independent, incognito browser instance for the FDA storefront phase — kept fully
    // isolated from both the Seller (shared driver) and Operator sessions above so no cookies/cache
    // from either backoffice session (or a prior storefront visit) can mask whether the updated
    // price has genuinely propagated. Owned entirely by this test: created in Phase 5, quit in
    // @AfterClass.
    private WebDriver fdaDriver;

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
    private static String promptForPrice(String sku, String currentExcelPrice) {
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
    // Only the Seller login happens here — per explicit instruction, Operator and FDA logins
    // are deferred until Phase 4 and Phase 5 respectively, so each session is only opened once
    // its own phase actually needs it (Seller task fully completes first, then Operator logs in
    // and completes its task, then the FDA storefront phase opens its own incognito browser and
    // polls for propagation — no customer login there, see Phase 5).
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
        LoggerUtility.info("TC_E2E_009 @BeforeSuite: Setup complete (Operator/FDA login deferred to their own phases)");
    }

    @BeforeClass
    public void initPageObjects() {
        miraklOffersPage = new MiraklOffersPage(driver);
        miraklFileImportPage = new MiraklFileImportPage(driver);
        fdaHomePage = new FDAHomePage(driver);
        fdaSearchResultsPage = new FDASearchResultsPage(driver);
        fdaPdpPage = new FDAPDPPage(driver);
        LoggerUtility.info("TC_E2E_009: All page objects initialized — Mirakl Seller session already "
            + "established in @BeforeSuite, no session swap needed");
    }

    @AfterClass(alwaysRun = true)
    public void cleanupSeparateBrowsers() {
        // These are test-owned drivers, not the shared suite driver, so quitting them here does
        // not violate the "never quit the shared driver outside @AfterSuite" rule.
        if (operatorDriver != null) {
            try {
                operatorDriver.quit();
                LoggerUtility.info("TC_E2E_009 @AfterClass: Operator browser closed");
            } catch (Exception e) {
                LoggerUtility.error("TC_E2E_009 @AfterClass: Failed to close Operator browser: " + e.getMessage());
            }
        }
        if (fdaDriver != null) {
            try {
                fdaDriver.quit();
                LoggerUtility.info("TC_E2E_009 @AfterClass: FDA storefront browser closed");
            } catch (Exception e) {
                LoggerUtility.error("TC_E2E_009 @AfterClass: Failed to close FDA storefront browser: " + e.getMessage());
            }
        }
    }

    @Test(testName = TC_NAME,
          description = "Verify a Seller-updated offer price (via Excel import) is correctly "
              + "reflected in Mirakl Seller/Operator and on the FDA storefront (search + PDP "
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

        // Step 1: Login to Mirakl as Seller (already established in @BeforeSuite — MiraklLoginPage.login()
        // already asserts a dashboard element was reached before returning, so login success for this
        // session was already confirmed, not assumed).
        switchToMiraklTab();
        driver.get(config.getMiraklUrl());
        LoggerUtility.info("Step 1: Reusing Mirakl Seller session established in @BeforeSuite: "
            + requireEnv("MIRAKL_SELLER_USERNAME"));

        // Step 2: Note the Seller/Shop name (needed later for the Operator's Shop filter)
        sellerShopName = miraklOffersPage.getShopName();
        LoggerUtility.info("Step 2: Captured Seller/Shop name: " + sellerShopName);
        Assert.assertFalse(sellerShopName.isEmpty(), "Seller/Shop name should not be empty | TC: " + TC_NAME);

        // Step 3: From the Excel file, note the product SKU and the tester-provided new price
        // (already written into the file in Phase 0)
        ExcelUtility.OfferData offer = ExcelUtility.readFirstOffer(excelFilePath);
        productSku = offer.sku;
        productId = offer.productId;
        LoggerUtility.info("Step 3: Excel Offer SKU: " + productSku + " | Product ID: " + productId
            + " | Excel price: " + offer.price);
        Assert.assertFalse(productSku.isEmpty(), "Excel product SKU should not be empty | TC: " + TC_NAME);

        // Step 4: Prices and stock -> Offers -> search by SKU -> wait for the grid to render (a
        // genuine WebDriverWait poll via waitForSingleResult(), not a sleep) -> note current price.
        // Also verify the settled row's own Offer SKU column matches what we searched for, rather
        // than trusting "a row is present" alone — guards against reading an unrelated row from a
        // still-transitioning grid.
        miraklOffersPage.navigateToOffers();
        miraklOffersPage.searchBySku(productSku);
        boolean sellerPreUpdateSettled = miraklOffersPage.waitForSingleResult(Duration.ofSeconds(15));
        Assert.assertTrue(sellerPreUpdateSettled,
            "Offer for SKU " + productSku + " should resolve to exactly one row in Seller Offers | TC: " + TC_NAME);
        Assert.assertEquals(miraklOffersPage.getFirstResultOfferSku().trim(), productSku,
            "Seller Offers grid row should match expected Offer SKU | TC: " + TC_NAME);
        String priceBeforeUpdate = miraklOffersPage.getFirstResultPrice();
        productName = miraklOffersPage.getFirstResultProductName();
        LoggerUtility.info("Step 4: Current Seller offer price (before update): " + priceBeforeUpdate
            + " | Product name: " + productName);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

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

        // Best-effort only: a real run (2026-09-10) showed this transient success banner does not
        // reliably appear before the page resets — so it's logged if present but never gates the
        // test. The Track offer imports report polled below is the authoritative source of truth.
        if (miraklFileImportPage.isImportStatusMessageDisplayed()) {
            LoggerUtility.info("Step 9: Transient import banner seen: " + miraklFileImportPage.getImportStatusMessage());
        } else {
            LoggerUtility.info("Step 9: Transient import banner not seen (not authoritative) — "
                + "proceeding to poll the Track offer imports report");
        }

        // Capture the import job ID/reference if shown (best-effort — not all Mirakl versions
        // expose one on this screen).
        if (miraklFileImportPage.isImportIdDisplayed()) {
            LoggerUtility.info("Step 9: Import ID: " + miraklFileImportPage.getImportId());
        }

        // Poll the Track offer imports report (not a blind wait) on a bounded interval until the
        // latest import reaches a terminal status, then assert 0 failed rows — not just "overall
        // success" — before trusting the price was actually updated.
        String importStatus = miraklFileImportPage.waitForImportCompletion(
            Duration.ofMinutes(2), Duration.ofSeconds(8));
        LoggerUtility.info("Step 9: Import reached terminal status: " + importStatus);
        int failedRowCount = miraklFileImportPage.getLatestImportFailedCount();
        Assert.assertEquals(failedRowCount, 0,
            "Import report should show 0 failed rows. Status=" + importStatus + " | Failed rows="
                + failedRowCount + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);

        // ============================================================
        // PHASE 3: Seller — Verify Price Updated
        // ============================================================
        LoggerUtility.info("===== PHASE 3: Mirakl Seller — Verify Updated Price =====");

        // Steps 10-11: Refresh, click Offers, search by SKU, and wait for the grid (explicit wait via
        // waitForSingleResult(), not a sleep) before reading the price.
        driver.navigate().refresh();
        miraklOffersPage.navigateToOffers();
        miraklOffersPage.searchBySku(productSku);
        boolean sellerPostImportSettled = miraklOffersPage.waitForSingleResult(Duration.ofSeconds(15));
        Assert.assertTrue(sellerPostImportSettled,
            "Offer for SKU " + productSku + " not found in Seller Offers after import | TC: " + TC_NAME);
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
        // PHASE 4: Operator — Verify Price via Shop Filter
        // ============================================================
        LoggerUtility.info("===== PHASE 4: Mirakl Operator — Verify Updated Price =====");

        // Steps 12-13: Seller's task is fully complete — only now open a new, independent
        // browser and log in as Operator. Kept fully separate from the Seller tab so that
        // session stays untouched. Credentials from environment variables, never
        // config.properties or hardcoded source. Test-owned driver: quit in @AfterClass.
        String operatorUsername = requireEnv("MIRAKL_OPERATOR_USERNAME");
        String operatorPassword = requireEnv("MIRAKL_OPERATOR_PASSWORD");
        LoggerUtility.info("Step 12: Opening new browser for Mirakl Operator session (" + operatorUsername + ")");
        WebDriverManager.chromedriver().setup();
        ChromeOptions operatorOptions = new ChromeOptions();
        operatorOptions.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--start-maximized",
            "--disable-notifications", "--disable-popup-blocking");
        operatorDriver = new ChromeDriver(operatorOptions);
        operatorDriver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        operatorDriver.get(config.getMiraklUrl());
        MiraklLoginPage operatorLogin = new MiraklLoginPage(operatorDriver);
        operatorLogin.login(operatorUsername, operatorPassword);
        operatorOffersPage = new MiraklOffersPage(operatorDriver);
        LoggerUtility.info("Step 13: Mirakl login as Operator (" + operatorUsername + ") complete");
        ScreenshotUtility.captureScreenshot(operatorDriver, TC_NAME, ScreenshotUtility.INFO);

        // Step 14: Prices and stocks -> Offers
        operatorOffersPage.navigateToOffers();

        // Step 15: Apply filter, select Shop, enter Shop name. A Shop ID filter (to avoid collisions
        // between shops that share a display name) is not applied here — no confirmed UI element
        // exposing a distinct Shop ID has been identified on this Operator screen; only Shop Name is
        // available to filter on for now.
        operatorOffersPage.filterByShop(sellerShopName);
        LoggerUtility.info("Step 15: Applied Shop filter: " + sellerShopName);

        // Step 16: Search by SKU, verify the price — same eventual-consistency poll pattern as the
        // Seller check in Phase 3, since the Operator view can lag the Seller-side update by the
        // same short propagation window.
        String operatorPrice = null;
        boolean operatorOfferFoundAtLeastOnce = false;
        for (int attempt = 1; attempt <= 10; attempt++) {
            operatorOffersPage.searchBySku(productSku);
            // Explicit wait (waitForSingleResult(), not a sleep loop) for the grid to narrow to
            // exactly one row (a unique SKU match) before falling back to the 30-second full-refresh
            // retry — guards against reading a stale/still-transitioning grid.
            boolean settledResults = operatorOffersPage.waitForSingleResult(Duration.ofSeconds(15));
            if (!settledResults) {
                // Tolerate a transient "no results" state right after search/navigation — only
                // fail hard if the offer never appears across all 10 attempts (checked after the
                // loop).
                LoggerUtility.info("Step 16: Offer not visible/settled yet under Operator Shop filter on attempt "
                    + attempt + "/10 — waiting 30 seconds...");
                if (attempt < 10) Thread.sleep(30_000);
            } else {
                operatorOfferFoundAtLeastOnce = true;
                operatorPrice = operatorOffersPage.getFirstResultPrice();
                LoggerUtility.info("Step 16: Price check " + attempt + "/10 — Operator-visible offer price: " + operatorPrice);
                if (PriceUtility.pricesEqual(operatorPrice, offer.price)) {
                    break;
                }
                if (attempt < 10) {
                    LoggerUtility.info("Operator price not updated yet — waiting 30 seconds...");
                    Thread.sleep(30_000);
                }
            }
            if (attempt < 10) {
                operatorDriver.navigate().refresh();
                operatorOffersPage.navigateToOffers();
                operatorOffersPage.filterByShop(sellerShopName);
            }
        }
        Assert.assertTrue(operatorOfferFoundAtLeastOnce,
            "Offer for SKU " + productSku + " not found under Operator Shop filter '" + sellerShopName
                + "' within 10 attempts | TC: " + TC_NAME);
        Assert.assertTrue(PriceUtility.pricesEqual(operatorPrice, offer.price),
            "Operator-visible offer price should match Excel price. Excel=" + offer.price
                + " | Operator=" + operatorPrice + " | TC: " + TC_NAME);
        ScreenshotUtility.captureScreenshot(operatorDriver, TC_NAME, ScreenshotUtility.PASS);

        // ============================================================
        // PHASE 5: FDA Adobe Storefront — Search by Product SKU + PDP Validation
        // ============================================================
        LoggerUtility.info("===== PHASE 5: FDA Storefront — Search by Product SKU + PDP =====");

        // Operator's task is fully complete — only now open a third, independent, incognito browser
        // for the FDA Adobe Storefront (fresh context so no cached price from any prior visit can
        // mask whether propagation genuinely happened — never reuses the Seller or Operator
        // sessions' cookies). Per explicit instruction, this goes straight from opening the
        // storefront to searching — no customer login step.
        LoggerUtility.info("Step 22: Opening new incognito browser for FDA storefront verification");
        WebDriverManager.chromedriver().setup();
        ChromeOptions fdaOptions = new ChromeOptions();
        fdaOptions.addArguments("--incognito", "--no-sandbox", "--disable-dev-shm-usage", "--start-maximized",
            "--disable-notifications", "--disable-popup-blocking");
        fdaDriver = new ChromeDriver(fdaOptions);
        fdaDriver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        fdaHomePage = new FDAHomePage(fdaDriver);
        fdaSearchResultsPage = new FDASearchResultsPage(fdaDriver);
        fdaPdpPage = new FDAPDPPage(fdaDriver);

        // Poll the storefront (not a blind wait) until the updated price is reflected there, up to
        // a max wait tuned to the known Mirakl -> Adobe Commerce reindex/cron SLA. A "no results
        // yet" state is tolerated as part of this poll rather than treated as a hard failure — the
        // product may not even be searchable until the sync completes. Timing out here throws a
        // distinct "not propagated within SLA" error, separate from a normal assertion failure.
        Duration storefrontSlaTimeout = Duration.ofMinutes(10);
        Duration storefrontPollInterval = Duration.ofSeconds(30);
        long storefrontDeadline = System.currentTimeMillis() + storefrontSlaTimeout.toMillis();
        String storefrontPrice = null;
        boolean pricePropagated = false;
        int propagationAttempt = 0;
        while (true) {
            propagationAttempt++;
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.enterSearchQuery(productSku);
            fdaHomePage.pressSearchEnter();
            if (fdaSearchResultsPage.isProductPresent(productSku)) {
                fdaSearchResultsPage.openMatchingResult(productSku);
                if (fdaPdpPage.isDisplayed()) {
                    storefrontPrice = fdaPdpPage.getProductPrice();
                    if (PriceUtility.pricesEqual(storefrontPrice, offer.price)) {
                        pricePropagated = true;
                        LoggerUtility.info("Step 28: Updated price propagated to FDA storefront after "
                            + propagationAttempt + " check(s) — PDP price: " + storefrontPrice);
                        break;
                    }
                    LoggerUtility.info("Step 28: Propagation check " + propagationAttempt
                        + " — storefront PDP price not yet updated (saw: " + storefrontPrice + ")");
                } else {
                    LoggerUtility.info("Step 28: Propagation check " + propagationAttempt
                        + " — product found in search but PDP not yet displayed");
                }
            } else {
                LoggerUtility.info("Step 28: Propagation check " + propagationAttempt
                    + " — product not yet present in FDA search results for SKU " + productSku);
            }
            if (System.currentTimeMillis() >= storefrontDeadline) {
                throw new IllegalStateException("TIMEOUT: Updated price for SKU " + productSku
                    + " did not propagate to the FDA storefront within the " + storefrontSlaTimeout.toMinutes()
                    + "-minute SLA window (Excel price=" + offer.price + ", last seen storefront price="
                    + storefrontPrice + ")");
            }
            Thread.sleep(storefrontPollInterval.toMillis());
        }
        Assert.assertTrue(pricePropagated,
            "Updated price should have propagated to the FDA storefront | TC: " + TC_NAME);
        LoggerUtility.info("Product confirmed present in Product SKU search results");
        ScreenshotUtility.captureScreenshot(fdaDriver, TC_NAME, ScreenshotUtility.INFO);

        // PDP is already open from the last (successful) propagation check above — verify Product
        // Name, SKU, and Price on it directly.
        String pdpPriceBySkuSearch = verifyProductDetailPage(productName, productSku, offer.price);
        ScreenshotUtility.captureScreenshot(fdaDriver, TC_NAME, ScreenshotUtility.PASS);

        // ============================================================
        // PHASE 6: FDA Adobe Storefront — Search by Product Name + PDP Validation
        // ============================================================
        LoggerUtility.info("===== PHASE 6: FDA Storefront — Search by Product Name + PDP =====");

        // Return to the storefront search page, search using the Product Name captured from the
        // Seller Offers grid in Phase 1 (Step 32/33 — using the name actually captured, not assumed).
        fdaHomePage.navigateTo(config.getFdaUrl());
        fdaHomePage.enterSearchQuery(productName);
        fdaHomePage.pressSearchEnter();
        Assert.assertTrue(fdaSearchResultsPage.isProductPresent(productName),
            "Product not found in FDA search results for name: " + productName + " | TC: " + TC_NAME);
        LoggerUtility.info("Product confirmed present in Product Name search results");
        ScreenshotUtility.captureScreenshot(fdaDriver, TC_NAME, ScreenshotUtility.INFO);

        // Open the PDP and verify Product Name, SKU, and Price again
        fdaSearchResultsPage.openMatchingResult(productName);
        String pdpPriceByNameSearch = verifyProductDetailPage(productName, productSku, offer.price);
        ScreenshotUtility.captureScreenshot(fdaDriver, TC_NAME, ScreenshotUtility.PASS);

        // Final summary log — never logs passwords
        LoggerUtility.info("TC_E2E_009 completed successfully");
        LoggerUtility.info("  Test Case          : TC_E2E_009");
        LoggerUtility.info("  Seller/Shop Name   : " + sellerShopName);
        LoggerUtility.info("  Product SKU        : " + productSku);
        LoggerUtility.info("  Product ID         : " + productId);
        LoggerUtility.info("  Product Name       : " + productName);
        LoggerUtility.info("  Price Before Update: " + priceBeforeUpdate);
        LoggerUtility.info("  Price After Update : " + priceAfterUpdate);
        LoggerUtility.info("  Operator Price     : " + operatorPrice);
        LoggerUtility.info("  PDP Price (SKU search)  : " + pdpPriceBySkuSearch);
        LoggerUtility.info("  PDP Price (Name search) : " + pdpPriceByNameSearch);
        LoggerUtility.info("  Final Test Status  : PASS");
    }

    // Opens on whatever PDP is currently displayed and verifies Product Name, SKU, and Price
    // against the expected values — shared by both the SKU-search and Name-search verification
    // blocks in Phase 5/6 so the checks aren't duplicated.
    private String verifyProductDetailPage(String expectedName, String expectedSku, String expectedPrice) {
        Assert.assertTrue(fdaPdpPage.isDisplayed(), "PDP not displayed | TC: " + TC_NAME);

        String pdpProductName = fdaPdpPage.getProductName();
        Assert.assertEquals(pdpProductName.trim(), expectedName.trim(),
            "PDP Product Name should equal expected Product Name. Expected=" + expectedName
                + " | Actual=" + pdpProductName + " | TC: " + TC_NAME);

        String pdpSku = fdaPdpPage.getProductSku();
        Assert.assertEquals(pdpSku.trim(), expectedSku.trim(),
            "PDP SKU should equal expected Product SKU. Expected=" + expectedSku + " | PDP=" + pdpSku
                + " | TC: " + TC_NAME);

        String pdpPrice = fdaPdpPage.getProductPrice();
        Assert.assertTrue(PriceUtility.pricesEqual(pdpPrice, expectedPrice),
            "PDP Price should equal New Price. Expected=" + expectedPrice + " | PDP=" + pdpPrice
                + " | TC: " + TC_NAME);

        return pdpPrice;
    }
}
