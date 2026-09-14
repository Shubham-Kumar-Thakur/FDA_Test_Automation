package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/** Mirakl "Price and stock -> File imports" screen — used to import the Excel offer file. */
public class MiraklFileImportPage extends BasePage {

    // Confirmed against live Seller dashboard left-nav (2026-09-07): submenu label is
    // "File imports" (plural), sibling of "Offers" and "Promotions" under "Price and stock".
    private static final By FILE_IMPORT_LINK = By.xpath("//span[normalize-space()='File imports']");
    // Confirmed against live "Import file" tab (2026-09-07): "Select file" is present but not
    // necessarily a <button> tag — matched by visible text on any clickable-looking element.
    // NOTE: not actually clicked (see clickSelectFile() below) — it triggers a native OS file
    // dialog Selenium can't control.
    private static final By SELECT_FILE_BUTTON = By.xpath(
        "//*[self::button or self::label or self::div or self::span][normalize-space(.)='Select file']");
    // The native OS file-picker triggered by "Select file" is bypassed entirely —
    // Selenium sends the path straight to the underlying <input type="file">.
    private static final By FILE_INPUT = By.xpath("//input[@type='file']");
    // Confirmed against live "Import file" tab (2026-09-07): "File content" is a custom dropdown
    // (not a native <select>) that already defaults to "Offers" — selectFileContent() below
    // checks the current value first and only interacts with it if a change is actually needed.
    private static final By FILE_CONTENT_LABEL_VALUE = By.xpath(
        "//*[normalize-space(.)='File content']/following::*[self::button or self::div][1]");
    // TODO: Verify option-list locator against actual DOM for values other than "Offers"
    // (e.g. "Offers and Products") — only "Offers" has been confirmed against a real run so far.
    // Confirmed working (2026-09-07): clicking "Import" is genuinely synchronous here — a green
    // "File imported" success banner appears immediately, no async processing wait needed.
    private static final By IMPORT_BUTTON = By.xpath("//button[normalize-space()='Import']");
    // text()-based (not normalize-space(.)) so this matches the banner's own text node, not any
    // ancestor container whose combined descendant text happens to contain "imported" — the
    // broader contains(.,...) form matched the whole page in a real run.
    //
    // Must NOT include a generic contains(text(),'imported') fallback — confirmed via screenshot
    // (2026-09-09 real run) that the static "Delete and replace" import-mode description
    // ("...replace it with data from the imported file.") also contains that word and is always
    // present on the page before Import is even clicked, causing a false-positive match that made
    // this check pass even when the import never actually ran.
    private static final By IMPORT_STATUS_MESSAGE = By.xpath("//*[contains(text(),'File imported')]");
    // TODO: Verify locator — no distinct "import ID" was visible on this confirmation screen;
    // "Track offer imports" (a separate tab) is where Mirakl shows historical import records/IDs.
    private static final By IMPORT_ID = By.xpath("//td[contains(@class,'import-id')] | //span[contains(@class,'import-id')]");

    // "Track offer imports" is a sibling tab of "Import file" on the same File imports screen
    // (confirmed present in the real "Import file" screenshot, 2026-09-10) — this is the
    // authoritative source of import completion/failure status, unlike the transient "File
    // imported" banner (which a real run on 2026-09-10 showed does NOT reliably appear: the form
    // resets to blank before the check runs). Column headers/order are not yet confirmed against
    // a live report row, so cell lookups below are done by header text rather than a fixed index —
    // tolerant of column reordering, and a diagnostic page-source dump is written on failure so the
    // real header text can be read off a live run instead of guessing further.
    private static final By TRACK_OFFER_IMPORTS_TAB = By.xpath("//*[normalize-space()='Track offer imports']");
    private static final By IMPORT_HISTORY_HEADERS = By.xpath("//table//thead//th | //table//tr[1]/th");
    private static final By LATEST_IMPORT_ROW_CELLS = By.xpath("//table//tbody/tr[1]/td");

    // Any of these substrings appearing (case-insensitive) in the latest import row's status cell
    // is treated as a terminal (no-longer-processing) state.
    private static final Set<String> TERMINAL_STATUS_KEYWORDS = Set.of(
        "success", "complete", "completed", "finished", "done", "error", "failed", "fail");

    public MiraklFileImportPage(WebDriver driver) {
        super(driver);
    }

    public void navigateToFileImport() {
        LoggerUtility.info("Navigating Mirakl: Price and stock -> File imports");
        jsClick(FILE_IMPORT_LINK);
    }

    // Intentionally NOT clicked in the test flow — kept only in case a future DOM change makes
    // the underlying <input type="file"> unreachable without it. Clicking "Select file" for real
    // opens a native OS file dialog that Selenium cannot interact with or dismiss.
    public void clickSelectFile() {
        LoggerUtility.info("Clicking Select file button");
        click(SELECT_FILE_BUTTON);
    }

    public void uploadOfferFile(String absoluteFilePath) {
        LoggerUtility.info("Uploading offer file: " + absoluteFilePath);
        uploadFile(FILE_INPUT, absoluteFilePath);
    }

    /** Selects the given "File content" option (e.g. "Offers", "Offers and Products"). */
    public void selectFileContent(String optionText) {
        String current = getText(FILE_CONTENT_LABEL_VALUE);
        if (optionText.equalsIgnoreCase(current.trim())) {
            LoggerUtility.info("File Content already set to '" + optionText + "' — no interaction needed");
            return;
        }
        LoggerUtility.info("Selecting File Content = " + optionText + " (was: " + current + ")");
        jsClick(FILE_CONTENT_LABEL_VALUE);
        By option = By.xpath("//li[normalize-space()='" + optionText + "'] | "
            + "//div[@role='option' and normalize-space()='" + optionText + "']");
        jsClick(option);
    }

    public void clickImport() {
        LoggerUtility.info("Clicking Import button");
        click(IMPORT_BUTTON);
    }

    // Uses a short implicit wait for this existence check instead of the global 2-minute one —
    // each poll attempt otherwise blocks for the full 2 minutes when the element isn't present
    // yet, turning a 12-attempt/60s poll into 20+ minutes of blocked WebDriver commands, which
    // reproducibly crashed the browser (disconnected: unable to send message to renderer) in
    // real runs. Mirrors the existing short-implicit-wait pattern already used elsewhere
    // (e.g. MiraklOrdersPage.hasSearchResults()).
    public boolean isImportStatusMessageDisplayed() {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        try {
            return driver.findElement(IMPORT_STATUS_MESSAGE).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        }
    }

    public String getImportStatusMessage() {
        String status = getText(IMPORT_STATUS_MESSAGE);
        LoggerUtility.info("Mirakl import status message: " + status);
        return status;
    }

    // Navigates directly to the history page URL instead of clicking the "Track offer imports"
    // link. Root-caused via a real run's diagnostic page-source dump (2026-09-14,
    // track_offer_imports_timeout.html): after jsClick() on TRACK_OFFER_IMPORTS_TAB, the page
    // source showed we were still on the "File imports" landing page (/mmp/shop/import) — the
    // synthetic JS click never triggered this React app's client-side route change (the link's
    // confirmed href from that same dump is /mmp/shop/offer/import/history), so every subsequent
    // findElements() call was searching the wrong page and eventually timed out. A direct
    // driver.get() to the same href, built from the current origin, sidesteps the click entirely.
    //
    // A short explicit wait for the first data row follows the navigation. Root-caused via a
    // second real run (2026-09-14): the confirmed history page is a legacy jQuery/DataTables
    // screen (id="offersDatatable") whose <tbody> rows render a beat after driver.get() returns
    // (the <thead> is present immediately, but the data row lags) — every one of 15 poll attempts
    // in that run hit the empty-tbody race and logged "not yet visible", yet the diagnostic dump
    // taken a moment after the final failure showed a fully populated row with a terminal
    // "Import complete" status. This wait absorbs that race per-attempt instead of relying on the
    // global implicit wait, which was observed returning empty results almost immediately rather
    // than polling for the deciding 2 minutes.
    public void navigateToTrackOfferImports() {
        LoggerUtility.info("Navigating Mirakl: Price and stock -> File imports -> Track offer imports");
        java.net.URI currentUri = java.net.URI.create(driver.getCurrentUrl());
        String historyUrl = currentUri.getScheme() + "://" + currentUri.getAuthority()
            + "/mmp/shop/offer/import/history";
        driver.get(historyUrl);
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, Duration.ofSeconds(10))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                    LATEST_IMPORT_ROW_CELLS));
        } catch (org.openqa.selenium.TimeoutException e) {
            // Tolerated here — the caller's own retry loop (waitForImportCompletion) already
            // handles a still-empty table as a normal "not yet visible" state.
        }
    }

    // Reads the latest (row 1) import history row's cell under the column whose header contains
    // headerKeyword (case-insensitive) — header-based instead of a fixed index since the real
    // column order/wording hasn't been confirmed against a live report yet.
    private String getLatestImportCellByHeader(String headerKeyword) {
        List<org.openqa.selenium.WebElement> headers = driver.findElements(IMPORT_HISTORY_HEADERS);
        List<org.openqa.selenium.WebElement> cells = driver.findElements(LATEST_IMPORT_ROW_CELLS);
        for (int i = 0; i < headers.size() && i < cells.size(); i++) {
            if (headers.get(i).getText().trim().toLowerCase().contains(headerKeyword.toLowerCase())) {
                return cells.get(i).getText().trim();
            }
        }
        throw new NoSuchElementException(
            "No column header containing '" + headerKeyword + "' found in Track offer imports table");
    }

    public String getLatestImportStatus() {
        String status = getLatestImportCellByHeader("status");
        LoggerUtility.info("Mirakl Track offer imports — latest import status: " + status);
        return status;
    }

    // Mirakl reports rejected rows under varying labels across versions ("Failed", "Errors",
    // "Ignored lines") — tries each in turn before giving up and dumping diagnostic page source.
    public int getLatestImportFailedCount() {
        String raw = null;
        for (String headerKeyword : new String[] {"fail", "error", "ignor"}) {
            try {
                raw = getLatestImportCellByHeader(headerKeyword);
                break;
            } catch (NoSuchElementException ignored) {
                // try next keyword
            }
        }
        if (raw == null) {
            dumpDiagnostic("track_offer_imports_failed_column_missing.html");
            throw new NoSuchElementException("Could not locate a Failed/Errors/Ignored column in "
                + "Track offer imports table — see diagnostic page-source dump");
        }
        String digitsOnly = raw.replaceAll("[^0-9]", "");
        int count = digitsOnly.isEmpty() ? 0 : Integer.parseInt(digitsOnly);
        LoggerUtility.info("Mirakl Track offer imports — latest import failed/ignored line count: "
            + count + " (raw cell text: '" + raw + "')");
        return count;
    }

    // Polls the Track offer imports report (not a blind wait) until the latest row's status reaches
    // a terminal state, or throws with a clear, distinctly-worded error on timeout. Uses the same
    // bounded check-then-sleep pattern already established elsewhere in this framework for
    // cross-system propagation waits (e.g. Kibo 3PL polling in ApiUtility-based tests).
    public String waitForImportCompletion(Duration maxWait, Duration pollInterval) throws InterruptedException {
        long deadlineMillis = System.currentTimeMillis() + maxWait.toMillis();
        String lastStatus = "";
        int attempt = 0;
        while (true) {
            attempt++;
            navigateToTrackOfferImports();
            try {
                lastStatus = getLatestImportStatus();
                String normalized = lastStatus.toLowerCase();
                for (String keyword : TERMINAL_STATUS_KEYWORDS) {
                    if (normalized.contains(keyword)) {
                        LoggerUtility.info("Import reached terminal status '" + lastStatus + "' after "
                            + attempt + " check(s)");
                        return lastStatus;
                    }
                }
                LoggerUtility.info("Import status not yet terminal (attempt " + attempt + "): '" + lastStatus + "'");
            } catch (NoSuchElementException e) {
                LoggerUtility.info("Import history row not yet visible (attempt " + attempt + ") — retrying");
            }
            if (System.currentTimeMillis() >= deadlineMillis) {
                dumpDiagnostic("track_offer_imports_timeout.html");
                throw new IllegalStateException("TIMEOUT: Import did not reach a terminal status within "
                    + maxWait.toSeconds() + "s — last seen status: '" + lastStatus + "'");
            }
            Thread.sleep(pollInterval.toMillis());
        }
    }

    private void dumpDiagnostic(String fileName) {
        try {
            java.nio.file.Files.writeString(
                java.nio.file.Path.of("test-output/logs/" + fileName), driver.getPageSource());
            LoggerUtility.info("Diagnostic: dumped page source to test-output/logs/" + fileName);
        } catch (Exception dumpFailure) {
            LoggerUtility.error("Diagnostic page-source dump failed: " + dumpFailure.getMessage());
        }
    }

    public boolean isImportIdDisplayed() {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        try {
            return driver.findElement(IMPORT_ID).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        }
    }

    public String getImportId() {
        String id = getText(IMPORT_ID);
        LoggerUtility.info("Mirakl import ID: " + id);
        return id;
    }
}
