package FDA_Automation_Script.FDA_Automation_Script.tests;

import FDA_Automation_Script.FDA_Automation_Script.base.BaseClass;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklLoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklOfferPage;
import FDA_Automation_Script.FDA_Automation_Script.utils.DriverFactory;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

// NOTE: This override only correctly skips FDA + suite-default Mirakl login when this class is
// run BY ITSELF (e.g. `mvn test -Dtest=Tc_manualoffercreation_01`). @BeforeSuite is suite-scoped,
// not class-scoped: if this class ever runs as part of the full 25-test suite (`mvn clean test`),
// the other 24 classes' unmodified inherited setupSuite() will STILL fire independently and open
// its own browser — since DriverFactory tracks a single ThreadLocal driver, running both in the
// same suite would orphan one Chrome process. Only use `-Dtest=Tc_manualoffercreation_01` for this TC.
public class Tc_manualoffercreation_01 extends BaseClass {

    private static final String TC_NAME = "Tc_manualoffercreation_01";

    // Test data — verified against the live Mirakl Add Offer form (2026-09-07):
    // - No separate "Shop SKU" field exists; "Offer SKU" (field id shopSku) is the only identifier.
    // - UPC/EAN is read-only catalog product data on this form, not a typed field.
    // - MSI is a Yes/No toggle, not an installment count.
    // - Logistics/warehouse is a required select2 dropdown on the form (not shown in the Offers list).
    private static final String SEARCH_TERM              = "rice";
    private static final String EXPECTED_PRODUCT_NAME    = "Low GI Rice";
    private static final String EXPECTED_PRODUCT_SKU     = "PJ123456782";
    private static final String TC_STOCK_QUANTITY        = "50";
    private static final String TC_PRICE                 = "199.00";
    private static final String TC_MSI                   = "Yes";
    private static final String TC_LOGISTICS             = "seller_warehouse";
    private static final String EXPECTED_STATUS_ACTIVE   = "Active";

    // --- Page objects ---
    private MiraklLoginPage miraklLoginPage;
    private MiraklOfferPage miraklOfferPage;

    // --- Dynamic test data (captured once, reused across seller and operator verification) ---
    private String offerSku;

    // --- Independent Mirakl Seller/Operator credentials, read from config.properties ---
    private String miraklLoginUrl;
    private String sellerEmail;
    private String sellerPassword;
    private String operatorEmail;
    private String operatorPassword;

    // Overrides BaseClass.setupSuite() — must match that exact method name/signature for Java's
    // polymorphic dispatch to replace it. Still launches a browser (driver would otherwise stay
    // null and everything below would NPE) but skips FDA login and the suite-default Mirakl login.
    // fdaTabHandle/miraklTabHandle are deliberately left null, so the inherited @AfterMethod
    // (which only acts when those handles are non-null) becomes a safe no-op for this class.
    @Override
    @BeforeSuite
    public void setupSuite() {
        LoggerUtility.info("Tc_manualoffercreation_01: Skipping FDA login and suite-default Mirakl login — launching independent browser");
        driver = DriverFactory.createDriver(false);
        LoggerUtility.info("Tc_manualoffercreation_01: Browser launched for this TC only");
    }

    @BeforeClass
    public void initAndLoginAsSeller() {
        miraklLoginPage = new MiraklLoginPage(driver);
        miraklOfferPage = new MiraklOfferPage(driver);
        miraklLoginUrl   = config.get("jnag.MiraklSeller.url");
        sellerEmail      = config.get("jnag.MiraklSeller.email");
        sellerPassword   = config.get("jnag.MiraklSeller.password");
        operatorEmail    = config.get("jnag.MiraklOperator.email");
        operatorPassword = config.get("jnag.MiraklOperator.password");
        LoggerUtility.info("Tc_manualoffercreation_01: All page objects initialized");

        LoggerUtility.info("Tc_manualoffercreation_01: Logging in as Mirakl Seller: " + sellerEmail);
        driver.manage().deleteAllCookies();
        driver.get(miraklLoginUrl);
        miraklLoginPage.login(sellerEmail, sellerPassword);
        LoggerUtility.info("Tc_manualoffercreation_01: Mirakl login as Seller " + sellerEmail + " successful");
    }

    // No @AfterClass session-restore needed — the inherited @AfterSuite (tearDownSuite) will quit
    // this browser entirely right after, since our setupSuite() override is what created it.

    @Test(testName = TC_NAME,
          description = "Verify a Seller can manually create an Offer for an existing catalog product in Mirakl, "
              + "and that the Offer is visible and Active for both the Seller and the Operator")
    public void tc_manualoffercreation_01_seller_creates_offer_operator_verifies() {

        // ============================================================
        // PHASE 1: SELLER — Create Offer
        // ============================================================
        LoggerUtility.info("===== Tc_manualoffercreation_01 PHASE 1: Seller Creates Offer =====");

        // Step 2-4: Navigate to Price and stock > Offers, click Add Offer
        LoggerUtility.info("Step 2-3: Navigating to Price and stock > Offers");
        miraklOfferPage.navigateToOffersSection();
        LoggerUtility.info("Step 3: Clicking Add Offer");
        miraklOfferPage.clickAddOffer();
        LoggerUtility.info("Step 4: Verifying Add Offer page is displayed");
        Assert.assertTrue(miraklOfferPage.isAddOfferPageDisplayed(),
            "Tc_manualoffercreation_01 — Add Offer page not displayed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 5-8: Search catalog, verify results, select product
        LoggerUtility.info("Step 5: Entering catalog search term: " + SEARCH_TERM);
        miraklOfferPage.searchCatalogProduct(SEARCH_TERM);
        LoggerUtility.info("Step 6: Clicking search icon");
        miraklOfferPage.clickCatalogSearchIcon();
        LoggerUtility.info("Step 7: Verifying matching products displayed");
        Assert.assertTrue(miraklOfferPage.isProductResultDisplayed(EXPECTED_PRODUCT_NAME, EXPECTED_PRODUCT_SKU),
            "Tc_manualoffercreation_01 — Product " + EXPECTED_PRODUCT_NAME + " / " + EXPECTED_PRODUCT_SKU
                + " not found in catalog search results");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 8-10: Select product, click Sell Yours, verify Create Offer page
        LoggerUtility.info("Step 9: Clicking Sell Yours for: " + EXPECTED_PRODUCT_NAME);
        miraklOfferPage.selectProductSellYours(EXPECTED_PRODUCT_NAME);
        LoggerUtility.info("Step 10: Verifying Create Offer page is displayed");
        Assert.assertTrue(miraklOfferPage.isCreateOfferPageDisplayed(),
            "Tc_manualoffercreation_01 — Create Offer page not displayed after Sell Yours");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 11-17: Fill offer details with a unique Offer SKU.
        // Steps 12-13 (separate Shop SKU / UPC-EAN entry) don't apply on the real form: Offer SKU
        // is the only identifier field, and UPC/EAN is read-only catalog data shown alongside it.
        offerSku = MiraklOfferPage.generateUniqueOfferSku();
        LoggerUtility.info("Step 11: Generated unique Offer SKU: " + offerSku);
        miraklOfferPage.enterOfferSku(offerSku);
        LoggerUtility.info("Step 14: Entering Stock Quantity: " + TC_STOCK_QUANTITY);
        miraklOfferPage.enterStockQuantity(TC_STOCK_QUANTITY);
        LoggerUtility.info("Step 15: Entering Price: " + TC_PRICE);
        miraklOfferPage.enterPrice(TC_PRICE);
        LoggerUtility.info("Step 16: Selecting MSI: " + TC_MSI);
        miraklOfferPage.selectMsi(TC_MSI);
        LoggerUtility.info("Step 17: Selecting Logistics/warehouse: " + TC_LOGISTICS);
        miraklOfferPage.selectLogistics(TC_LOGISTICS);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 18-19: Submit for approval ("Create offer" button), verify success message
        LoggerUtility.info("Step 18: Clicking Create offer");
        miraklOfferPage.clickSubmitForApproval();
        String successMessage = miraklOfferPage.getSuccessMessage();
        LoggerUtility.info("Step 19: Success message: " + successMessage);
        Assert.assertTrue(successMessage.toLowerCase().contains("offer"),
            "Tc_manualoffercreation_01 — Expected a success notification mentioning 'offer'. Actual: " + successMessage);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 2: SELLER — Verify Offer in Offers List
        // ============================================================
        LoggerUtility.info("===== Tc_manualoffercreation_01 PHASE 2: Seller Verifies Offers List =====");

        // Step 20-23: Navigate back to Offers, switch search filter to Offer SKU, search
        LoggerUtility.info("Step 20: Navigating to Price and stock > Offers");
        miraklOfferPage.navigateToOffersSection();
        miraklOfferPage.selectSearchByOfferSku();
        LoggerUtility.info("Step 21: Searching Offers list for: " + offerSku);
        miraklOfferPage.searchOfferBySku(offerSku);
        LoggerUtility.info("Step 22: Clicking search icon");
        miraklOfferPage.clickOffersSearchIcon();
        LoggerUtility.info("Step 23: Verifying created offer displayed in Offers list");
        Assert.assertTrue(miraklOfferPage.hasOfferInList(offerSku),
            "Tc_manualoffercreation_01 — Offer " + offerSku + " not found in Seller Offers list");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 24-25: Verify offer row details and Active status.
        // Logistics is not a column in this Offers list (only selectable on the Add Offer form),
        // so it's not asserted here — Brand is logged instead as an available extra data point.
        LoggerUtility.info("Step 24: Verifying offer row details");
        String sellerOfferSku = miraklOfferPage.getOfferSku(offerSku);
        Assert.assertTrue(sellerOfferSku.contains(offerSku),
            "Tc_manualoffercreation_01 — Offer SKU mismatch. Expected: " + offerSku + ". Actual: " + sellerOfferSku);
        String sellerProductName = miraklOfferPage.getOfferProductName(offerSku);
        Assert.assertTrue(sellerProductName.contains(EXPECTED_PRODUCT_NAME),
            "Tc_manualoffercreation_01 — Offer Product Name mismatch. Actual: " + sellerProductName);
        String sellerProductSku = miraklOfferPage.getOfferProductSku(offerSku);
        Assert.assertTrue(sellerProductSku.contains(EXPECTED_PRODUCT_SKU),
            "Tc_manualoffercreation_01 — Offer Product SKU mismatch. Actual: " + sellerProductSku);
        String sellerPrice = miraklOfferPage.getOfferPrice(offerSku);
        String sellerQuantity = miraklOfferPage.getOfferQuantity(offerSku);
        String sellerBrand = miraklOfferPage.getOfferBrand(offerSku);
        String sellerCondition = miraklOfferPage.getOfferCondition(offerSku);
        LoggerUtility.info("Tc_manualoffercreation_01 | Offer Price      : " + sellerPrice);
        LoggerUtility.info("Tc_manualoffercreation_01 | Offer Quantity   : " + sellerQuantity);
        LoggerUtility.info("Tc_manualoffercreation_01 | Offer Brand      : " + sellerBrand);
        LoggerUtility.info("Tc_manualoffercreation_01 | Offer Condition  : " + sellerCondition);

        LoggerUtility.info("Step 25: Verifying offer Status = Active (Seller view)");
        String sellerStatus = miraklOfferPage.getOfferStatus(offerSku);
        Assert.assertEquals(sellerStatus, EXPECTED_STATUS_ACTIVE,
            "Tc_manualoffercreation_01 — Offer status should be 'Active' in Seller Offers list. Actual: " + sellerStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // ============================================================
        // PHASE 3: OPERATOR — Verify Offer
        // ============================================================
        LoggerUtility.info("===== Tc_manualoffercreation_01 PHASE 3: Operator Verifies Offer =====");

        // Step 26: Clear Seller session, login as Operator
        LoggerUtility.info("Step 26: Clearing Seller session and logging in as Operator: " + operatorEmail);
        driver.manage().deleteAllCookies();
        driver.get(miraklLoginUrl);
        miraklLoginPage.login(operatorEmail, operatorPassword);

        // Step 27-29: Navigate to Offers, switch search filter to Offer SKU, search
        LoggerUtility.info("Step 27: Navigating to Price and stock > Offers as Operator");
        miraklOfferPage.navigateToOffersSection();
        miraklOfferPage.selectSearchByOfferSku();
        LoggerUtility.info("Step 28: Searching Operator Offers list for: " + offerSku);
        miraklOfferPage.searchOfferBySku(offerSku);
        LoggerUtility.info("Step 29: Clicking search icon");
        miraklOfferPage.clickOffersSearchIcon();
        LoggerUtility.info("Step 30: Verifying offer displayed in Operator Offers list");
        Assert.assertTrue(miraklOfferPage.hasOfferInList(offerSku),
            "Tc_manualoffercreation_01 — Offer " + offerSku + " not found in Operator Offers list");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.INFO);

        // Step 31: Verify correct Offer SKU and Active status (Operator view)
        String operatorOfferSku = miraklOfferPage.getOfferSku(offerSku);
        Assert.assertTrue(operatorOfferSku.contains(offerSku),
            "Tc_manualoffercreation_01 — Operator view Offer SKU mismatch. Expected: " + offerSku
                + ". Actual: " + operatorOfferSku);
        LoggerUtility.info("Step 31: Verifying offer Status = Active (Operator view)");
        String operatorStatus = miraklOfferPage.getOfferStatus(offerSku);
        Assert.assertEquals(operatorStatus, EXPECTED_STATUS_ACTIVE,
            "Tc_manualoffercreation_01 — Offer status should be 'Active' in Operator Offers list. Actual: " + operatorStatus);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME, ScreenshotUtility.PASS);

        // ============================================================
        // FINAL SUMMARY LOG
        // ============================================================
        LoggerUtility.info("===== Tc_manualoffercreation_01 COMPLETE — PASS =====");
        LoggerUtility.info("Tc_manualoffercreation_01 | Test Case ID       : " + TC_NAME);
        LoggerUtility.info("Tc_manualoffercreation_01 | Product Name       : " + EXPECTED_PRODUCT_NAME);
        LoggerUtility.info("Tc_manualoffercreation_01 | Product SKU        : " + EXPECTED_PRODUCT_SKU);
        LoggerUtility.info("Tc_manualoffercreation_01 | Offer SKU          : " + offerSku);
        LoggerUtility.info("Tc_manualoffercreation_01 | Seller Status      : " + sellerStatus);
        LoggerUtility.info("Tc_manualoffercreation_01 | Operator Status    : " + operatorStatus);
        LoggerUtility.info("Tc_manualoffercreation_01 | Test Execution Status : PASS");
    }
}
// @AfterMethod navigateToHomePage() is inherited from BaseClass
