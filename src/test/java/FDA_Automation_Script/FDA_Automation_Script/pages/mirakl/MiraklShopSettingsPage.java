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

    // SUPERSEDED live (2026-09-25, TC_EDD_001): the 2026-09-24 note above assumed 4 separate
    // text/value fields. A live diagnostic probe found the real structure is very different — see each
    // getter below for its own confirmed markup. Kept only as history of what was originally guessed.

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
        waitForPanelDataToRender();
    }

    // CONFIRMED live (2026-09-25, TC_EDD_001): this panel renders in a waterfall, not a single paint —
    // a diagnostic probe found the "Business holidays" heading/description text present after ~9s, but
    // the actual data (the 7 working-day checkboxes, the Open from/To time inputs) still hadn't
    // rendered even at that point — they only appeared by ~15s. Polling for real keyword TEXT (the
    // earlier approach) is therefore not a reliable readiness signal for this page; poll for the actual
    // interactive element (the first working-day checkbox) instead. Bounded and non-fatal — Phase 1's
    // caller already treats this whole method as best-effort/discovery-mode.
    private void waitForPanelDataToRender() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(30)).until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-nameandlabel*='businessDays%3Ar']")));
        } catch (TimeoutException e) {
            LoggerUtility.warn("Business Calendar panel data (working-day checkboxes) never rendered after 30s");
        }
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

    // CONFIRMED live (2026-09-25, TC_EDD_001): "Working days per week" is a <fieldset> of 7 custom
    // ARIA checkboxes (not native <input type="checkbox">), one per day — e.g.
    // <div role="checkbox" aria-checked="true" data-nameandlabel="businessDays%3Ar4%3A%3A%3AMonday">.
    // The day name is the last ":::"-delimited segment of the URL-decoded data-nameandlabel attribute;
    // the fieldset's own top-level data-nameandlabel ("businessDays::Working days per week") has no
    // ":::" segment and is naturally excluded by the split-length check. Live run showed Mon-Fri
    // checked, Sun/Sat unchecked for this seller — returns comma-joined day names, which
    // EddCalculator.parseWorkingDays() already consumes (English day names, comma-separated).
    public String getWorkingDaysText() {
        return getDayCheckboxNames(true);
    }

    // CONFIRMED live (2026-09-25, TC_EDD_001): there is no separate "Non-Working Days" UI section —
    // it's the complement of the same 7-checkbox set read by getWorkingDaysText() (unchecked =
    // non-working). Note this returns DAY NAMES (e.g. "Sunday,Saturday"), not calendar dates, even
    // though TC_EDD_001_Test currently feeds this into EddCalculator.parseDates() (which expects
    // dates) — that mismatch predates this fix and is a separate, not-yet-live-confirmed question of
    // whether "non-working days" in EddCalculator's model means the weekly pattern's off-days or
    // specific exception dates from the "Business holidays" list (see getHolidaysText()). Left for the
    // caller to reconcile; this method itself now returns exactly what the live DOM shows.
    public String getNonWorkingDaysText() {
        return getDayCheckboxNames(false);
    }

    private String getDayCheckboxNames(boolean checkedValue) {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "var boxes = document.querySelectorAll('[role=\"checkbox\"][data-nameandlabel*=\"businessDays\"]');"
            + "var out = [];"
            + "for (var i = 0; i < boxes.length; i++) {"
            + "  var raw = decodeURIComponent(boxes[i].getAttribute('data-nameandlabel') || '');"
            + "  var parts = raw.split(':::');"
            + "  if (parts.length < 2) continue;"
            + "  var dayName = parts[parts.length - 1];"
            + "  var checked = boxes[i].getAttribute('aria-checked') === 'true';"
            + "  if (checked === arguments[0]) out.push(dayName);"
            + "}"
            + "return out.join(',');",
            checkedValue);
        String value = result == null ? "" : result.toString().trim();
        LoggerUtility.info("Business Calendar — " + (checkedValue ? "Working Days" : "Non-Working Days (weekly)") + ": " + value);
        return value;
    }

    // BEST-EFFORT, NOT LIVE-CONFIRMED for a populated case (2026-09-25, TC_EDD_001): the page has a
    // "Business holidays" list ("You can set business holidays as working days or non-working days.")
    // for date-specific exceptions to the weekly schedule above, with a "Results per page" pagination
    // control — but this seller account has zero entries configured (confirmed live: the pager shows
    // "NaN results", apparently a genuine Mirakl UI bug when the list is empty rather than a "0
    // results" string). With no populated rows to inspect, this scans for a <table>/<tbody> near the
    // heading and returns its row text if one ever appears; returns "" (correctly, matching
    // EddCalculator's blank-input handling) for the current empty state. Re-probe against an account
    // with real holiday entries before trusting the row-parsing shape here.
    public String getHolidaysText() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "var heading = Array.from(document.querySelectorAll('h1,h2,h3,legend'))"
            + "  .find(function(el) { return el.textContent.toLowerCase().indexOf('business holiday') !== -1; });"
            + "if (!heading) return '';"
            + "var container = heading.closest('section, div, form') || heading.parentElement;"
            + "if (!container) return '';"
            + "var rows = container.querySelectorAll('table tbody tr, [role=\"row\"]');"
            + "var out = [];"
            + "for (var i = 0; i < rows.length; i++) {"
            + "  var t = rows[i].textContent.trim();"
            + "  if (t) out.push(t);"
            + "}"
            + "return out.join(';');");
        String value = result == null ? "" : result.toString().trim();
        LoggerUtility.info("Business Calendar — Holidays: " + (value.isEmpty() ? "(none configured)" : value));
        return value;
    }

    // CONFIRMED live (2026-09-25, TC_EDD_001): "Open from"/"To" are two separate custom time-picker
    // combobox widgets, each backed by a real <input type="text" placeholder="hh:mm am/pm"> — e.g.
    // <input id="businessHours.openFrom__trigger" value="10:30 am">. Reading both .value attributes
    // and joining with " to " gives EddCalculator.parseTimeRange() (which just regex-scans for the
    // first two "hh:mm am/pm" matches in the string, order-independent of separator) exactly what it
    // needs — live run showed "10:30 am" / "11:00 am" for this seller.
    public String getBusinessHoursText() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "var from = document.getElementById('businessHours.openFrom__trigger');"
            + "var to = document.getElementById('businessHours.openUntil__trigger');"
            + "var fromVal = from ? from.value.trim() : '';"
            + "var toVal = to ? to.value.trim() : '';"
            + "if (!fromVal && !toVal) return '';"
            + "return fromVal + ' to ' + toVal;");
        String value = result == null ? "" : result.toString().trim();
        LoggerUtility.info("Business Calendar — Business Hours: " + value);
        return value;
    }
}
