package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

// New page object for TC_EDD_001 — Mirakl Settings -> Shop -> Business Calendar tab. No existing
// page object in this codebase covers this screen (confirmed via search across pages/mirakl/*.java).
// CONFIRMED live (2026-09-24): SETTINGS_MENU and SHOP_SUBMENU are correct — a live run reached the
// Shop settings page cleanly via these two locators, landing on
// https://.../mmp/shop/account/shop with a "My Account" section listing tabs: Business calendar,
// Contact Details, Bank Account Details, Billing information, Returns, Imports, CSV settings,
// Circular economy (plus a "See more" to expand further). The diagnostic dump revealed the real tab
// label is "Business calendar" (lowercase 'c') — the original BUSINESS_CALENDAR_TAB locator did an
// exact-case match on "Business Calendar" and never matched, causing a silent 120s timeout. Fixed
// below via a case-insensitive translate() match, same pattern already used in
// MiraklOfferPage.SUBMIT_FOR_APPROVAL_BUTTON_FALLBACK. Field-label locators below are still TODO —
// not yet reached live.
public class MiraklShopSettingsPage extends BasePage {

    private static final By SETTINGS_MENU        = By.xpath("//span[normalize-space()='Settings']");
    private static final By SHOP_SUBMENU          = By.xpath("//a[contains(@id,'shop')] | //span[normalize-space()='Shop']");
    private static final By BUSINESS_CALENDAR_TAB = By.xpath(
        "//*[self::button or self::a or self::span]"
        + "[contains(translate(normalize-space(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'business calendar')]");

    // Label-prefix matchers (same convention as MiraklOfferPage.setFieldByLabel/getFieldValueByLabel)
    // for the specific Business Calendar fields. Kept as raw-text reads (not typed getters) since the
    // exact widget type (checkbox list, calendar picker, plain text) for each is unknown pre-live-run.
    // CONFIRMED live (2026-09-24, per user observation of the live screen): the real field labels are
    // "Working Days", "Non-Working Days", "Holidays", and a single combined "Business Hours (Mexico
    // Time - GMT-6)" field — NOT separate "Business Start Time"/"Business End Time" fields as
    // originally guessed. See getBusinessHoursText() + EddCalculator.parseTimeRange() for the
    // consolidated read.
    private static final String WORKING_DAYS_LABEL     = "working days";
    private static final String NON_WORKING_DAYS_LABEL = "non-working days|non working days";
    private static final String HOLIDAYS_LABEL         = "holidays";
    private static final String BUSINESS_HOURS_LABEL   = "business hours";

    public MiraklShopSettingsPage(WebDriver driver) {
        super(driver);
    }

    public void navigateToBusinessCalendar() {
        LoggerUtility.info("Navigating to Mirakl Settings > Shop > Business Calendar");
        WaitUtility.fluentWaitForClickable(driver, SETTINGS_MENU);
        if (!isShopSubmenuPresent()) {
            jsClick(SETTINGS_MENU);
            WaitUtility.fluentWait(driver, SHOP_SUBMENU);
        }
        jsClick(SHOP_SUBMENU);
        // Short bounded wait (not the shared 2-minute WaitUtility.fluentWaitForClickable) — the tab's
        // exact label/structure is unconfirmed (see class header), so this surfaces the real page
        // structure via logVisibleTabTexts() in well under a minute instead of burning a full 2-minute
        // timeout on a guessed locator a second time.
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15)).until(ExpectedConditions.elementToBeClickable(BUSINESS_CALENDAR_TAB));
        } catch (TimeoutException e) {
            logVisibleTabTexts();
            throw e;
        }
        jsClick(BUSINESS_CALENDAR_TAB);
        LoggerUtility.info("Business Calendar tab opened");
        // Unconditional diagnostic — the field-label getters below are still best-effort/unconfirmed;
        // logging the raw panel text here means a wrong guess is diagnosable from this same run's log
        // instead of needing another MFA-gated live run just to see the real structure.
        getBusinessCalendarRawText();
    }

    // Diagnostic dump — every visible button/link/span/list-item's text on the current page, for
    // discovering the real Business Calendar tab label (and any other Shop-settings tab names) when
    // the guessed BUSINESS_CALENDAR_TAB locator doesn't match. Same "log full raw text on failure"
    // convention already used elsewhere in this codebase (e.g. MiraklOrderDetailPage's diagnostic
    // dumps) — only fires inside the catch block above, not unconditionally.
    private void logVisibleTabTexts() {
        Object texts = ((JavascriptExecutor) driver).executeScript(
            "var els = document.querySelectorAll('button, a, span, li, [role=\"tab\"]');"
            + "var out = [];"
            + "for (var i = 0; i < els.length && out.length < 80; i++) {"
            + "  var t = els[i].textContent.trim();"
            + "  if (t && t.length > 0 && t.length < 50 && out.indexOf(t) === -1) out.push(t);"
            + "}"
            + "return out.join(' | ');");
        LoggerUtility.warn("DIAGNOSTIC — visible tab/button/link/list-item texts on current page: " + texts);
        LoggerUtility.warn("DIAGNOSTIC — current URL: " + driver.getCurrentUrl());
    }

    // Instant JS presence check — avoids the global 2-minute implicit wait if the accordion is
    // already expanded (same pattern as MiraklOfferPage.isAccordionExpanded()).
    private boolean isShopSubmenuPresent() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(\"//a[contains(@id,'shop')] | //span[normalize-space()='Shop']\", "
            + "document, null, XPathResult.BOOLEAN_TYPE, null).booleanValue;");
        return Boolean.TRUE.equals(result);
    }

    public String getWorkingDaysText() {
        return getFieldTextByLabel(WORKING_DAYS_LABEL, "Working Days");
    }

    public String getNonWorkingDaysText() {
        return getFieldTextByLabel(NON_WORKING_DAYS_LABEL, "Non-Working Days");
    }

    public String getHolidaysText() {
        return getFieldTextByLabel(HOLIDAYS_LABEL, "Holidays");
    }

    /** Reads the single combined "Business Hours (Mexico Time - GMT-6)" field — contains both start and end time; parse with EddCalculator.parseTimeRange(). */
    public String getBusinessHoursText() {
        return getFieldTextByLabel(BUSINESS_HOURS_LABEL, "Business Hours");
    }

    // Best-effort label-prefix text read, same approach as MiraklOfferPage.getFieldValueByLabel() but
    // also falling back to the container's own textContent (not just an input/textarea value) since
    // this screen's fields may render as read-only labels/checkbox lists rather than editable inputs.
    private String getFieldTextByLabel(String labelMatchers, String logName) {
        String[] matchers = labelMatchers.toLowerCase().split("\\|");
        Object result = ((JavascriptExecutor) driver).executeScript(
            "var matchers = arguments[0];"
            + "var groups = document.querySelectorAll('.form-group, tr, li, div');"
            + "for (var g = 0; g < groups.length; g++) {"
            + "  var grp = groups[g];"
            + "  var grpText = grp.textContent.trim().toLowerCase();"
            + "  var matched = false;"
            + "  for (var m = 0; m < matchers.length; m++) { if (grpText.indexOf(matchers[m]) === 0) { matched = true; break; } }"
            + "  if (!matched) continue;"
            + "  var el = grp.querySelector('input, textarea');"
            + "  if (el && el.value) { return el.value; }"
            + "  return grp.textContent.trim();"
            + "}"
            + "return '';",
            (Object) matchers);
        String value = result == null ? "" : result.toString().trim();
        LoggerUtility.info("Business Calendar — " + logName + ": " + value);
        return value;
    }

    // Diagnostic/fallback dump of the whole Business Calendar panel's raw text, for logging when the
    // specific label-based getters above don't find a match — same diagnostic-dump convention already
    // used in MiraklOrderDetailPage.
    public String getBusinessCalendarRawText() {
        Object text = ((JavascriptExecutor) driver).executeScript(
            "var el = document.querySelector('main, .content, body');"
            + "return el ? el.textContent.trim() : '';");
        String value = text == null ? "" : text.toString().trim();
        LoggerUtility.info("Business Calendar raw panel text (diagnostic): " + value);
        return value;
    }
}
