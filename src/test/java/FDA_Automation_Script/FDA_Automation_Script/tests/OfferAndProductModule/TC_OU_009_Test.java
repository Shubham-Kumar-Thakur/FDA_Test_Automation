package FDA_Automation_Script.FDA_Automation_Script.tests.OfferAndProductModule;

import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAHomePage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDALoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAPDPPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDASearchResultsPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklFileImportsPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklLoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklOfferPage;
import FDA_Automation_Script.FDA_Automation_Script.utils.ApiUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ConfigReader;
import FDA_Automation_Script.FDA_Automation_Script.utils.DualDriverManager;
import FDA_Automation_Script.FDA_Automation_Script.utils.ExcelUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.PriceUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import io.restassured.response.Response;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

/**
 * TC_OU_009 — Seller updates an offer's price via Excel import, and the new price is verified
 * end-to-end: Mirakl Seller (Prices &amp; Stock -&gt; Offers / File imports) -&gt; Push Offers to
 * Empathy -&gt; FDA storefront (PLP + PDP).
 *
 * Config keys (see config.properties "TC_OU_009" section): tc.ou009.mirakl.url/username/password,
 * tc.ou009.fda.url/username/password, tc.ou009.offer.excel.path, tc.ou009.push.offers.url/cookie.
 */
public class TC_OU_009_Test {

    private static final String TC_NAME = "TC_OU_009";

    private final ConfigReader config = ConfigReader.getInstance();

    private WebDriver driver;
    private String miraklTabHandle;
    private String fdaTabHandle;

    private MiraklLoginPage miraklLoginPage;
    private MiraklOfferPage miraklOfferPage;
    private MiraklFileImportsPage miraklFileImportsPage;

    private FDAHomePage fdaHomePage;
    private FDALoginPage fdaLoginPage;
    private FDASearchResultsPage fdaSearchResultsPage;
    private FDAPDPPage fdaPdpPage;

    @BeforeSuite
    public void setupMiraklSession() {
        LoggerUtility.info("=== TC_OU_009: Starting browser + Mirakl Seller login ===");
        driver = DualDriverManager.createFreshChromeDriver();
        miraklTabHandle = driver.getWindowHandle();

        miraklLoginPage = new MiraklLoginPage(driver);
        miraklOfferPage = new MiraklOfferPage(driver);
        miraklFileImportsPage = new MiraklFileImportsPage(driver);
        fdaHomePage = new FDAHomePage(driver);
        fdaLoginPage = new FDALoginPage(driver);
        fdaSearchResultsPage = new FDASearchResultsPage(driver);
        fdaPdpPage = new FDAPDPPage(driver);

        driver.get(config.get("tc.ou009.mirakl.url"));
        miraklLoginPage.login(config.get("tc.ou009.mirakl.username"), config.get("tc.ou009.mirakl.password"));
        LoggerUtility.info("=== TC_OU_009: Mirakl Seller login complete ===");
    }

    @AfterSuite(alwaysRun = true)
    public void tearDown() {
        LoggerUtility.info("=== TC_OU_009: Tearing down browser ===");
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                LoggerUtility.warn("Error closing browser: " + e.getMessage());
            }
        }
    }

    @Test(testName = TC_NAME,
          description = "Seller updates an offer's price via Excel import and validates it on the FDA storefront")
    public void tc_ou_009_seller_update_offer_price_via_excel() {

        String excelPath = config.get("tc.ou009.offer.excel.path");

        // =======================================================================
        // PHASE 0 — Update Offer Price in Excel
        // =======================================================================
        LoggerUtility.info("==================== PHASE 0: UPDATE OFFER PRICE IN EXCEL ====================");

        ExcelUtility.OfferRow originalRow = ExcelUtility.readFirstOfferRow(excelPath);
        LoggerUtility.info("Offer read from Excel — Offer SKU: " + originalRow.offerSku
            + ", Product ID: " + originalRow.productId + ", current Excel price: " + originalRow.price);

        double newPrice = promptForPrice(originalRow.price);
        LoggerUtility.info("New offer price to apply: " + newPrice);

        ExcelUtility.updatePrice(excelPath, newPrice);
        LoggerUtility.info("PASS — Excel price updated to " + newPrice + " for Offer SKU " + originalRow.offerSku);

        // =======================================================================
        // PHASE 1 — Mirakl Seller: Capture Existing Offer Details
        // =======================================================================
        LoggerUtility.info("==================== PHASE 1: MIRAKL SELLER — CAPTURE EXISTING OFFER ====================");

        String shopName = miraklLoginPage.getLoggedInAccountName();
        LoggerUtility.info("Seller/Shop name: " + shopName);

        String offerSku = originalRow.offerSku;
        String productId = originalRow.productId;

        miraklOfferPage.navigateToOffersSection();
        miraklOfferPage.selectSearchByProductId();
        miraklOfferPage.searchOfferByProductId(productId);

        int rowCount = miraklOfferPage.getVisibleOfferRowCount();
        LoggerUtility.info("Offers returned for Product ID " + productId + ": " + rowCount);
        Assert.assertEquals(rowCount, 1,
            "Phase 1: Searching by Product ID " + productId + " should return exactly one offer. Actual: " + rowCount);
        LoggerUtility.info("PASS — Exactly one offer returned for Product ID " + productId);

        String preUpdatePrice = miraklOfferPage.getOfferPrice(offerSku);
        String productName = miraklOfferPage.getOfferProductName(offerSku);
        LoggerUtility.info("PASS — Captured current offer price: " + preUpdatePrice + ", product name: " + productName);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_PHASE1_CAPTURED", ScreenshotUtility.INFO);

        // =======================================================================
        // PHASE 2 — Import Updated Offer File
        // =======================================================================
        LoggerUtility.info("==================== PHASE 2: IMPORT UPDATED OFFER FILE ====================");

        miraklFileImportsPage.navigateToFileImports();
        String baselineHistoryRow = miraklFileImportsPage.captureLatestHistorySnapshot();

        miraklFileImportsPage.uploadFile(excelPath);
        miraklFileImportsPage.selectOffersOnlyContent();
        miraklFileImportsPage.clickImport();
        LoggerUtility.info("PASS — Offer Excel import submitted");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_PHASE2_IMPORT_SUBMITTED", ScreenshotUtility.INFO);

        miraklFileImportsPage.verifyImportSucceeded(baselineHistoryRow);
        LoggerUtility.info("PASS — Offer import reached a terminal, non-failed state");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_PHASE2_IMPORT_VERIFIED", ScreenshotUtility.INFO);

        // =======================================================================
        // PHASE 3 — Verify Updated Price in Mirakl
        // =======================================================================
        LoggerUtility.info("==================== PHASE 3: VERIFY UPDATED PRICE IN MIRAKL ====================");

        driver.navigate().refresh();
        miraklOfferPage.navigateToOffersSection();
        miraklOfferPage.selectSearchByProductId();
        miraklOfferPage.searchOfferByProductId(productId);

        Assert.assertTrue(miraklOfferPage.hasOfferInList(offerSku),
            "Phase 3: Offer " + offerSku + " should still be present after import");

        String postUpdatePrice = miraklOfferPage.getOfferPrice(offerSku);
        double postUpdatePriceValue = PriceUtility.parse(postUpdatePrice);
        LoggerUtility.info("Offer price before update: " + preUpdatePrice + ", after update: " + postUpdatePrice
            + ", expected (Excel): " + newPrice);

        Assert.assertFalse(PriceUtility.matches(preUpdatePrice, postUpdatePriceValue),
            "Phase 3: Offer price should have changed from the pre-update value. Before: " + preUpdatePrice
                + ", After: " + postUpdatePrice);
        Assert.assertTrue(PriceUtility.matches(postUpdatePriceValue, newPrice),
            "Phase 3: Updated Mirakl offer price should match the Excel price. Expected: " + newPrice
                + ", Actual: " + postUpdatePrice);
        LoggerUtility.info("PASS — Mirakl offer price updated and matches Excel: " + postUpdatePrice);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_PHASE3_PRICE_VERIFIED", ScreenshotUtility.PASS);

        // =======================================================================
        // PHASE 3B — Push Updated Offer to Empathy
        // =======================================================================
        LoggerUtility.info("==================== PHASE 3B: PUSH UPDATED OFFER TO EMPATHY ====================");

        Response pushResponse = ApiUtility.pushOffersToEmpathy();
        Assert.assertEquals(pushResponse.getStatusCode(), 200,
            "Phase 3B: Push Offers to Empathy should return HTTP 200. Actual: " + pushResponse.getStatusCode());
        LoggerUtility.info("PASS — Push Offers to Empathy returned HTTP 200");

        // =======================================================================
        // PHASE 4 — FDA Storefront Validation
        // =======================================================================
        LoggerUtility.info("==================== PHASE 4: FDA STOREFRONT VALIDATION ====================");

        openFdaInNewTab();

        driver.get(config.get("tc.ou009.fda.url"));
        fdaLoginPage.login(config.get("tc.ou009.fda.username"), config.get("tc.ou009.fda.password"));
        Assert.assertTrue(fdaHomePage.isLoggedIn(), "Phase 4: FDA login should succeed");
        LoggerUtility.info("PASS — FDA storefront login successful");

        fdaHomePage.navigateTo(config.get("fda.url"));
        fdaHomePage.enterSearchQuery(productName);
        fdaHomePage.pressSearchEnter();

        Assert.assertTrue(fdaSearchResultsPage.isProductInResults(productName),
            "Phase 4: Product '" + productName + "' should appear in FDA search results");
        LoggerUtility.info("PASS — Product found in FDA search results");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_PHASE4_SEARCH_RESULTS", ScreenshotUtility.INFO);

        // --- PLP Validation (non-blocking per requirement — PDP is the authoritative check) ---
        LoggerUtility.info("---- PLP Validation ----");
        Assert.assertTrue(fdaSearchResultsPage.isResultsGridDisplayed(), "Phase 4: PLP results grid should be displayed");

        boolean plpMatched = false;
        for (int attempt = 1; attempt <= 5 && !plpMatched; attempt++) {
            String plpPriceText = fdaSearchResultsPage.getResultItemPrice(productName);
            LoggerUtility.info("PLP price check attempt " + attempt + "/5: " + plpPriceText);
            if (PriceUtility.matches(plpPriceText, newPrice)) {
                plpMatched = true;
                break;
            }
            if (attempt < 5) {
                driver.navigate().refresh();
                sleep(3000);
            }
        }
        if (plpMatched) {
            LoggerUtility.info("PASS — PLP price matches updated price: " + newPrice);
        } else {
            LoggerUtility.warn("PLP price did not match updated price " + newPrice + " within 5 attempts — "
                + "non-blocking, proceeding to PDP (authoritative check)");
        }
        ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_PHASE4_PLP", ScreenshotUtility.INFO);

        // --- PDP Validation (authoritative) ---
        LoggerUtility.info("---- PDP Validation ----");
        fdaSearchResultsPage.openMatchingResult(productName);

        Assert.assertTrue(fdaPdpPage.isDisplayed(), "Phase 4: PDP should be displayed after opening the search result");

        String pdpName = fdaPdpPage.getProductName();
        Assert.assertTrue(pdpName.toLowerCase().contains(productName.toLowerCase())
                || productName.toLowerCase().contains(pdpName.toLowerCase()),
            "Phase 4: PDP product name should match expected. Expected: " + productName + ", Actual: " + pdpName);
        LoggerUtility.info("PASS — PDP product name verified: " + pdpName);

        String pdpSku = fdaPdpPage.getProductSku();
        Assert.assertTrue(pdpSku.contains(productId) || productId.contains(pdpSku),
            "Phase 4: PDP SKU/Product ID should match expected. Expected: " + productId + ", Actual: " + pdpSku);
        LoggerUtility.info("PASS — PDP SKU/Product ID verified: " + pdpSku);

        double pdpPriceValue = Double.NaN;
        boolean pdpMatched = false;
        for (int attempt = 1; attempt <= 5 && !pdpMatched; attempt++) {
            String pdpPriceText = fdaPdpPage.getProductPrice();
            pdpPriceValue = PriceUtility.parse(pdpPriceText);
            LoggerUtility.info("PDP price check attempt " + attempt + "/5: " + pdpPriceText);
            if (PriceUtility.matches(pdpPriceValue, newPrice)) {
                pdpMatched = true;
                break;
            }
            if (attempt < 5) sleep(3000);
        }

        if (!pdpMatched) {
            LoggerUtility.info("Main PDP price did not match this seller's updated price — checking "
                + "seller-specific offer row for '" + shopName + "'");
            String sellerPriceText = fdaPdpPage.getSellerOfferPrice(shopName);
            double sellerPriceValue = PriceUtility.parse(sellerPriceText);
            if (PriceUtility.matches(sellerPriceValue, newPrice)) {
                pdpMatched = true;
                pdpPriceValue = sellerPriceValue;
            }
        }

        Assert.assertTrue(pdpMatched,
            "Phase 4: PDP price (main or seller-specific) should match updated Excel price " + newPrice
                + ". Last observed PDP price: " + PriceUtility.format(pdpPriceValue));
        LoggerUtility.info("PASS — PDP price verified: " + PriceUtility.format(pdpPriceValue));
        ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_PHASE4_PDP_VERIFIED", ScreenshotUtility.PASS);

        LoggerUtility.info("=== TC_OU_009 execution completed successfully ===");
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Prompts the tester for a new offer price. Checks -Dtc.ou009.price=<value> first (required when
     * running under Maven Surefire's forked JVM, whose stdin is claimed by the fork-communication
     * protocol and never reachable from the outer `mvn` command); falls back to an interactive stdin
     * read for IDE/standalone runs. currentPrice is only used in the prompt text.
     */
    private double promptForPrice(double currentPrice) {
        String sysProp = System.getProperty("tc.ou009.price");
        if (sysProp != null && !sysProp.isBlank()) {
            double value = Double.parseDouble(sysProp.trim());
            LoggerUtility.info("New offer price supplied via -Dtc.ou009.price=" + value);
            return value;
        }
        try {
            System.out.println("Current offer price: " + currentPrice + ". Enter new offer price: ");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line = reader.readLine();
            if (line != null && !line.isBlank()) {
                return Double.parseDouble(line.trim());
            }
        } catch (IOException | NumberFormatException e) {
            LoggerUtility.warn("Could not read a new price from stdin: " + e.getMessage());
        }
        throw new IllegalStateException("No new offer price supplied — pass -Dtc.ou009.price=<value> "
            + "(required under Maven Surefire) or provide one via stdin when running outside Maven.");
    }

    /** Opens the FDA storefront in a new browser tab within the same Chrome session as Mirakl. */
    private void openFdaInNewTab() {
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("window.open('about:blank','_blank');");
        Set<String> handles = driver.getWindowHandles();
        for (String handle : handles) {
            if (!handle.equals(miraklTabHandle)) {
                fdaTabHandle = handle;
                driver.switchTo().window(fdaTabHandle);
                LoggerUtility.info("Opened FDA storefront in new tab: " + fdaTabHandle);
                return;
            }
        }
        throw new IllegalStateException("Could not find a new window handle for the FDA tab");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
