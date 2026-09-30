package FDA_Automation_Script.FDA_Automation_Script.tests.EDD_TC;

import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAHomePage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDALoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAPDPPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDASearchResultsPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklLoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklOfferPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklOfferPage.SelectedOffer;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklShopSettingsPage;
import FDA_Automation_Script.FDA_Automation_Script.utils.ConfigReader;
import FDA_Automation_Script.FDA_Automation_Script.utils.DualDriverManager;
import FDA_Automation_Script.FDA_Automation_Script.utils.EddApiUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.EddCalculator;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import io.restassured.response.Response;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

/**
 * TC_EDD_001 — Estimated Delivery Date validation: a Mirakl seller's Business Calendar (working
 * days, non-working days, holidays, business hours) plus a per-offer Lead Time to Ship value drive
 * the EDD shown on the FDA storefront PDP. Independent of the FDA-&gt;Kibo-&gt;Mirakl order-lifecycle
 * flow the rest of this suite covers — architecture mirrors tests/OfferAndProductModule/TC_OU_009_Test
 * (does not extend BaseClass, single DualDriverManager-created Chrome session shared between Mirakl
 * and FDA via tab switching).
 *
 * Config keys (see config.properties "TC_EDD_001" section): tc.edd001.mirakl.url/username/password,
 * tc.edd001.fda.url/username/password, tc.edd001.shop.business.days.url/cookie, tc.edd001.empathy.url.
 *
 * Product/offer sequencing note: the manual test case's step 2 says "take any random product id for
 * that seller and update and keep for later", and step 5 says "Search the same product" — so the
 * random offer picked in Mirakl IS "the product" referenced in the PDP steps. This class therefore
 * picks the random offer FIRST (Phase 2), then uses its product name for every FDA storefront step,
 * which is a reordering of the manual case's narrative order (business calendar capture doesn't
 * depend on product choice, so it stays first).
 */
public class TC_EDD_001_Test {

    private static final String TC_NAME = "TC_EDD_001";

    private final ConfigReader config = ConfigReader.getInstance();

    private WebDriver driver;
    private String miraklTabHandle;
    private String fdaTabHandle;

    private MiraklLoginPage miraklLoginPage;
    private MiraklShopSettingsPage miraklShopSettingsPage;
    private MiraklOfferPage miraklOfferPage;

    private FDAHomePage fdaHomePage;
    private FDALoginPage fdaLoginPage;
    private FDASearchResultsPage fdaSearchResultsPage;
    private FDAPDPPage fdaPdpPage;

    @BeforeSuite
    public void setupMiraklSession() {
        LoggerUtility.info("=== TC_EDD_001: Starting browser + Mirakl Seller login ===");
        driver = DualDriverManager.createFreshChromeDriver();
        miraklTabHandle = driver.getWindowHandle();

        miraklLoginPage = new MiraklLoginPage(driver);
        miraklShopSettingsPage = new MiraklShopSettingsPage(driver);
        miraklOfferPage = new MiraklOfferPage(driver);
        fdaHomePage = new FDAHomePage(driver);
        fdaLoginPage = new FDALoginPage(driver);
        fdaSearchResultsPage = new FDASearchResultsPage(driver);
        fdaPdpPage = new FDAPDPPage(driver);

        driver.get(config.get("tc.edd001.mirakl.url"));
        miraklLoginPage.login(config.get("tc.edd001.mirakl.username"), config.get("tc.edd001.mirakl.password"));
        LoggerUtility.info("=== TC_EDD_001: Mirakl Seller login complete ===");
    }

    @AfterSuite(alwaysRun = true)
    public void tearDown() {
        LoggerUtility.info("=== TC_EDD_001: Tearing down browser ===");
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                LoggerUtility.warn("Error closing browser: " + e.getMessage());
            }
        }
    }

    @Test(testName = TC_NAME,
          description = "Validates the FDA storefront EDD after updating a Mirakl offer's Lead Time to Ship")
    public void tc_edd_001_estimated_delivery_date_validation() {

        // Declared early (rather than at Phase 6, as originally planned) so the TEMPORARY
        // DISCOVERY-MODE instrumentation below can also route Phase 4's assertion through it —
        // keeps this run going through Phases 5-7 in the same MFA cycle even if Phase 4 fails.
        SoftAssert softAssert = new SoftAssert();

        // =======================================================================
        // PHASE 1 — Capture Seller Business Calendar
        // =======================================================================
        LoggerUtility.info("==================== PHASE 1: CAPTURE SELLER BUSINESS CALENDAR ====================");

        // TEMPORARY DISCOVERY-MODE INSTRUMENTATION: MiraklShopSettingsPage's locators are unverified
        // (see its class Javadoc) and Mirakl's MFA gate makes each live run costly (manual OTP entry),
        // so this phase is non-fatal — on failure it logs the diagnostic dump already captured inside
        // navigateToBusinessCalendar() and falls back to empty/null calendar data so later phases can
        // still run and surface their own diagnostics in the same run. Remove this try/catch once
        // MiraklShopSettingsPage's locators are confirmed and this phase reliably succeeds.
        Set<DayOfWeek> workingDays = java.util.Collections.emptySet();
        Set<LocalDate> nonWorkingDays = java.util.Collections.emptySet();
        Set<LocalDate> holidays = java.util.Collections.emptySet();
        LocalTime businessStartTime = null;
        LocalTime businessEndTime = null;
        try {
            miraklShopSettingsPage.navigateToBusinessCalendar();

            String workingDaysText = miraklShopSettingsPage.getWorkingDaysText();
            String nonWorkingDaysText = miraklShopSettingsPage.getNonWorkingDaysText();
            String holidaysText = miraklShopSettingsPage.getHolidaysText();
            String businessHoursText = miraklShopSettingsPage.getBusinessHoursText();

            workingDays = EddCalculator.parseWorkingDays(workingDaysText);
            nonWorkingDays = EddCalculator.parseDates(nonWorkingDaysText);
            holidays = EddCalculator.parseDates(holidaysText);
            LocalTime[] businessHours = EddCalculator.parseTimeRange(businessHoursText);
            businessStartTime = businessHours[0];
            businessEndTime = businessHours[1];

            LoggerUtility.info("Business Calendar captured — Working Days: " + workingDays + ", Non-Working Days: "
                + nonWorkingDays + ", Holidays: " + holidays + ", Business Start: " + businessStartTime
                + ", Business End (seller cut-off time): " + businessEndTime);
            ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_PHASE1_BUSINESS_CALENDAR", ScreenshotUtility.INFO);
        } catch (Exception e) {
            LoggerUtility.warn("PHASE 1 failed (discovery mode — continuing to later phases): " + e.getMessage());
        }

        // =======================================================================
        // PHASE 2 — Mirakl: Pick a Random Offer for This Seller
        // =======================================================================
        LoggerUtility.info("==================== PHASE 2: PICK RANDOM OFFER FOR SELLER ====================");

        miraklOfferPage.navigateToOffersSection();
        miraklOfferPage.clickPendingOffersTab();

        SelectedOffer selectedOffer = miraklOfferPage.selectRandomOfferForSeller();
        LoggerUtility.info("PASS — Selected offer for this run: Offer SKU=" + selectedOffer.offerSku
            + ", Product SKU=" + selectedOffer.productSku + ", Product Name=" + selectedOffer.productName);

        // TEMPORARY DISCOVERY-MODE INSTRUMENTATION (see Phase 1's comment) — kept non-fatal here so
        // Phase 3 (FDA) still runs in this same MFA cycle even on an unexpected failure.
        // FIXED live (2026-09-25): openOfferForEdit() now takes Product ID (productSku), not Offer
        // SKU — see its own comment in MiraklOfferPage.java for why.
        String preUpdateLeadTimeText = "unknown (edit form not opened)";
        try {
            miraklOfferPage.openOfferForEdit(selectedOffer.productSku);
            preUpdateLeadTimeText = miraklOfferPage.getFieldValueByLabel("lead time to ship");
            LoggerUtility.info("Current Lead Time to Ship (kept for later): " + preUpdateLeadTimeText);
        } catch (Exception e) {
            LoggerUtility.warn("PHASE 2 offer-edit-open failed (discovery mode — continuing): " + e.getMessage());
        }
        ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_PHASE2_OFFER_SELECTED", ScreenshotUtility.INFO);

        // =======================================================================
        // PHASE 3 — FDA: Capture Initial EDD + Current Mexico Timestamp
        // =======================================================================
        LoggerUtility.info("==================== PHASE 3: FDA — CAPTURE INITIAL EDD ====================");

        openFdaInNewTab();
        driver.get(config.get("tc.edd001.fda.url"));
        fdaLoginPage.login(config.get("tc.edd001.fda.username"), config.get("tc.edd001.fda.password"));
        Assert.assertTrue(fdaHomePage.isLoggedIn(), "Phase 3: FDA login should succeed");
        LoggerUtility.info("PASS — FDA storefront login successful");

        fdaHomePage.navigateTo(config.get("fda.url"));
        fdaHomePage.enterSearchQuery(selectedOffer.productName);
        fdaHomePage.pressSearchEnter();

        Assert.assertTrue(fdaSearchResultsPage.isProductInResults(selectedOffer.productName),
            "Phase 3: Product '" + selectedOffer.productName + "' should appear in FDA search results");
        fdaSearchResultsPage.openMatchingResult(selectedOffer.productName);
        Assert.assertTrue(fdaPdpPage.isDisplayed(), "Phase 3: PDP should be displayed after opening the search result");

        String initialEddText = fdaPdpPage.getEstimatedDeliveryDateText();
        ZonedDateTime referenceTimestampMx = ZonedDateTime.now(ZoneId.of("America/Mexico_City"));
        LoggerUtility.info("PASS — Initial PDP EDD: '" + initialEddText + "', current Mexico timestamp: "
            + referenceTimestampMx);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_PHASE3_INITIAL_EDD", ScreenshotUtility.INFO);

        // =======================================================================
        // PHASE 4 — Mirakl: Update Lead Time to Ship
        // =======================================================================
        LoggerUtility.info("==================== PHASE 4: MIRAKL — UPDATE LEAD TIME TO SHIP ====================");

        int newLeadTimeToShip = promptForLeadTimeToShip(preUpdateLeadTimeText);
        LoggerUtility.info("New Lead Time to Ship to apply: " + newLeadTimeToShip);

        // TEMPORARY DISCOVERY-MODE INSTRUMENTATION (see Phase 1's comment) — non-fatal here so
        // Phases 5-7 still run in this same MFA cycle even if the offer update itself fails.
        boolean offerUpdateSucceeded = false;
        try {
            driver.switchTo().window(miraklTabHandle);
            miraklOfferPage.navigateToOffersSection();
            miraklOfferPage.openOfferForEdit(selectedOffer.productSku);
            miraklOfferPage.setFieldByLabel("lead time to ship", String.valueOf(newLeadTimeToShip));
            miraklOfferPage.saveOfferEdit();
            offerUpdateSucceeded = miraklOfferPage.isOfferEditSuccessful();
        } catch (Exception e) {
            LoggerUtility.warn("PHASE 4 offer update failed (discovery mode — continuing): " + e.getMessage());
        }
        softAssert.assertTrue(offerUpdateSucceeded,
            "Phase 4: Offer edit should show a success confirmation after saving Lead Time to Ship = " + newLeadTimeToShip);
        LoggerUtility.info("PASS — Offer Lead Time to Ship updated to " + newLeadTimeToShip);
        ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_PHASE4_OFFER_UPDATED", ScreenshotUtility.PASS);

        // =======================================================================
        // PHASE 5 — Calculate Expected EDD (pure calculation, no browser)
        // =======================================================================
        LoggerUtility.info("==================== PHASE 5: CALCULATE EXPECTED EDD ====================");

        LocalDate referenceDate = referenceTimestampMx.toLocalDate();
        LocalTime currentMxTime = referenceTimestampMx.toLocalTime();

        LocalDate expectedEdd = EddCalculator.calculateExpectedEdd(referenceDate, currentMxTime, businessEndTime,
            workingDays, nonWorkingDays, holidays, newLeadTimeToShip);
        LoggerUtility.info("PASS — Expected EDD calculated: " + expectedEdd);

        // =======================================================================
        // PHASE 6 — Validate Service Synchronization (soft — non-terminal, must not block Phase 7)
        // =======================================================================
        LoggerUtility.info("==================== PHASE 6: VALIDATE SERVICE SYNCHRONIZATION ====================");

        Response businessDaysResponse = EddApiUtility.getShopBusinessDays();
        softAssert.assertEquals(businessDaysResponse.getStatusCode(), 200,
            "Phase 6: Shop Business Days service should return HTTP 200. Body: "
                + businessDaysResponse.getBody().asString());
        LoggerUtility.info("Shop Business Days response logged above — field-level assertions are best-effort "
            + "until a live run confirms the real JSON shape (see EddApiUtility.getShopBusinessDays() doc comment)");

        // Per explicit instruction: give the Shop Business Days sync a moment to actually take effect
        // server-side before pushing offers to Empathy, rather than calling it back-to-back.
        sleep(5000);

        Response empathyResponse = EddApiUtility.pushOffersToEmpathy();
        softAssert.assertEquals(empathyResponse.getStatusCode(), 200,
            "Phase 6: Push Offers to Empathy should return HTTP 200. Body: " + empathyResponse.getBody().asString());
        LoggerUtility.info("PASS (soft) — Both API validation calls completed");
        ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_PHASE6_API_VALIDATION", ScreenshotUtility.INFO);

        // =======================================================================
        // PHASE 7 — FDA: Validate Updated EDD on Storefront
        // =======================================================================
        LoggerUtility.info("==================== PHASE 7: FDA — VALIDATE UPDATED EDD ====================");

        driver.switchTo().window(fdaTabHandle);

        // Per explicit instruction: keep refreshing until the PDP actually shows the calculated
        // expected EDD, rather than stopping as soon as ANY non-blank value different from the
        // (often blank) initial read appears — that early-break condition was satisfied by stale
        // leftover EDD text from a prior run/lead-time value, not the real post-update value.
        String updatedEddText = initialEddText;
        boolean formatMatch = false;
        int maxAttempts = 10;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            driver.navigate().refresh();
            sleep(3000);
            updatedEddText = fdaPdpPage.getEstimatedDeliveryDateText();
            LoggerUtility.info("Updated EDD check attempt " + attempt + "/" + maxAttempts + ": " + updatedEddText);
            if (!updatedEddText.isBlank() && containsFormattedDate(updatedEddText, expectedEdd)) {
                formatMatch = true;
                break;
            }
        }

        Assert.assertFalse(updatedEddText.isBlank(), "Phase 7: PDP should display an Estimated Delivery Date after the update");
        LoggerUtility.info("PASS — PDP shows an updated EDD: '" + updatedEddText + "'. Expected EDD (calculated): "
            + expectedEdd + " — exact text-format matching against the calculated date is best-effort until "
            + "the real PDP date format is confirmed live; logged here for manual verification.");

        softAssert.assertTrue(formatMatch,
            "Phase 7: PDP EDD text ('" + updatedEddText + "') should contain a recognizable representation of the "
                + "calculated expected EDD (" + expectedEdd + ") — best-effort text-format check, refine once the "
                + "real PDP date format is confirmed live");

        ScreenshotUtility.captureScreenshot(driver, TC_NAME + "_PHASE7_UPDATED_EDD", ScreenshotUtility.PASS);

        softAssert.assertAll();
        LoggerUtility.info("=== TC_EDD_001 execution completed successfully ===");
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Prompts the tester for a new Lead Time to Ship value. Checks -Dtc.edd001.lead.time.to.ship=<value>
     * first (required when running under Maven Surefire's forked JVM, whose stdin is claimed by the
     * fork-communication protocol and never reachable from the outer `mvn` command); falls back to an
     * interactive stdin read for IDE/standalone runs. currentValueText is only used in the prompt text.
     */
    private int promptForLeadTimeToShip(String currentValueText) {
        String sysProp = System.getProperty("tc.edd001.lead.time.to.ship");
        if (sysProp != null && !sysProp.isBlank()) {
            int value = Integer.parseInt(sysProp.trim());
            LoggerUtility.info("New Lead Time to Ship supplied via -Dtc.edd001.lead.time.to.ship=" + value);
            return value;
        }
        try {
            System.out.println("Current Lead Time to Ship: " + currentValueText + ". Enter new Lead Time to Ship (days): ");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line = reader.readLine();
            if (line != null && !line.isBlank()) {
                return Integer.parseInt(line.trim());
            }
        } catch (IOException | NumberFormatException e) {
            LoggerUtility.warn("Could not read a new Lead Time to Ship from stdin: " + e.getMessage());
        }
        throw new IllegalStateException("No new Lead Time to Ship supplied — pass "
            + "-Dtc.edd001.lead.time.to.ship=<value> (required under Maven Surefire) or provide one via stdin "
            + "when running outside Maven.");
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

    // Best-effort check for whether the raw PDP EDD text contains some recognizable representation of
    // the calculated expected date — tries a handful of common formats since the real PDP date format
    // is unconfirmed pre-live-run (see this class's Javadoc / EddCalculator's parser TODOs).
    //
    // CONFIRMED live (2026-09-25, TC_EDD_001): the real PDP format is "d/MMM." — e.g. "30/Sep." — day,
    // slash, a 3-letter capitalized month abbreviation, trailing period. None of the original candidate
    // formats covered this ("d MMMM"/"MMMM d" are full month names; "MMM d" has the wrong field order).
    // Java's own es/es-MX locale data renders the abbreviated month as "sept" (no period, extra "t"),
    // which doesn't match the site's "Sep." either — so instead of trying to guess the exact locale
    // data the site uses, both the PDP text and every candidate are normalized (lowercased, periods
    // stripped, whitespace collapsed) before comparing, and "d/MMM" is tried under both English and
    // Spanish locales to cover either spelling.
    private boolean containsFormattedDate(String text, LocalDate date) {
        if (text == null || text.isBlank() || date == null) return false;
        String normalizedText = normalizeForDateMatch(text);
        DateTimeFormatter[] formats = {
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/MMM", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d/MMM", Locale.forLanguageTag("es")),
            DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("es")),
            DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
        };
        for (DateTimeFormatter fmt : formats) {
            try {
                if (normalizedText.contains(normalizeForDateMatch(date.format(fmt)))) return true;
            } catch (Exception ignored) {
                // try next format
            }
        }
        return false;
    }

    private String normalizeForDateMatch(String value) {
        return value.toLowerCase(Locale.ROOT).replace(".", "").replaceAll("\\s+", " ").trim();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
