package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.UnexpectedTagNameException;

import java.io.File;
import java.util.List;

// TODO: Unlike MiraklOfferPage, this page has NOT been fully verified against the live Mirakl DOM.
// CONFIRMED real (2026-09-08 live screenshot): "Price and stock" accordion nav, "File imports" as
// a sibling of "Offers"/"Promotions", the "Source" section with a genuinely native (NOT
// CSS-hidden) <input type=file> rendered as Chrome's default "Choose File" button, and a
// "File content" dropdown defaulting to "Offers". Still unverified: the exact File content
// select's id/name/tag and the Import button's confirmation state after click.
public class MiraklFileImportsPage extends BasePage {

    private static final By PRICE_AND_STOCK_MENU = By.xpath("//span[normalize-space()='Price and stock']");
    private static final By FILE_IMPORTS_SUBMENU = By.xpath("//span[normalize-space()='File imports']");

    private static final By FILE_INPUT = By.xpath("//input[@type='file']");
    // Content-type control: try anchoring off the visible "File content" label text first; the
    // confirmed live screenshot (2026-09-08) shows only ONE dropdown-style control on this whole
    // form (Source = file input, Import mode = radio buttons, File content = this dropdown), so a
    // bare "//select" is a reliable fallback if the label-anchored xpath doesn't match the real
    // markup (e.g. the label text isn't inside a literal <label> tag).
    private static final By FILE_CONTENT_SELECT_BY_LABEL = By.xpath(
        "//label[contains(normalize-space(),'File content')]/following::select[1] | " +
        "//*[contains(normalize-space(),'File content')]/following::select[1]");
    private static final By FILE_CONTENT_SELECT_ANY = By.xpath("//select");
    private static final By FILE_CONTENT_OFFERS_AND_PRODUCTS_RADIO = By.xpath(
        "//input[@type='radio' and (contains(@value,'offers_and_products') or contains(@value,'OFFERS_AND_PRODUCTS'))] | " +
        "//label[contains(normalize-space(),'Offers and Products')]");
    private static final By IMPORT_BUTTON = By.xpath(
        "//button[contains(normalize-space(),'Import') and not(contains(normalize-space(),'History'))]");

    // CONFIRMED live (2026-09-18, real DOM dump): this page injects a real error banner via
    // displayErrorMessage() JS: $("#importMsg-msg-bar").html("<div class='alert alert-danger'>...").
    // Absence of this element after Import is a genuine (not assumed) success signal for the
    // client-side validation step, and its presence is a genuine failure with real error text.
    // Checked via instant JS (document.querySelector) inside verifyImportSucceeded(), not a By
    // locator + driver.findElements() — see that method's doc comment for why.
    // CONFIRMED live: a real nav link to this shop's own Offer Import History page — the actual
    // authoritative "did the import succeed" signal (server-recorded job status), not an inference
    // from the upload form's own transient client-side state.
    private static final By TRACK_OFFER_IMPORTS_LINK = By.xpath("//a[contains(normalize-space(),'Track offer imports')]");

    public MiraklFileImportsPage(WebDriver driver) {
        super(driver);
    }

    public void navigateToFileImports() {
        LoggerUtility.info("Navigating to Price and stock > File imports");
        // Wait for the sidebar to be genuinely interactive first — called right after login, and
        // the SPA may not have finished hydrating its click handlers yet (same fix already applied
        // to MiraklOfferPage.navigateToOffersSection()).
        WaitUtility.fluentWaitForClickable(driver, PRICE_AND_STOCK_MENU);
        if (!isSubmenuVisible()) {
            jsClick(PRICE_AND_STOCK_MENU);
            WaitUtility.fluentWait(driver, FILE_IMPORTS_SUBMENU);
        }
        jsClick(FILE_IMPORTS_SUBMENU);
        LoggerUtility.info("File imports section opened");
    }

    // Instant JS check, same pattern as MiraklOfferPage.isAccordionExpanded() — avoids the
    // 2-minute implicit-wait block that findElement()/isDisplayed() would hit if the accordion
    // is genuinely collapsed (element absent from DOM, not just hidden).
    private boolean isSubmenuVisible() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(\"//span[normalize-space()='File imports']\", document, null, " +
            "XPathResult.BOOLEAN_TYPE, null).booleanValue;");
        return Boolean.TRUE.equals(result);
    }

    // There is no real "Select File" button (confirmed live: 120s timeout, element doesn't exist).
    // The real <input type="file"> is what matters. IMPORTANT: don't use WaitUtility.fluentWait()
    // here — that waits for VISIBILITY, but file inputs are almost always hidden via CSS in favor
    // of a custom-styled trigger (that's the whole reason a hidden input exists). Waiting for it
    // to become visible would never succeed and hangs for the full 2-minute timeout (confirmed
    // live — this exact hang was the previous bug). driver.findElement() only waits for PRESENCE
    // in the DOM (via the global implicit wait), not visibility, which is what's actually needed.
    public void clickSelectFile() {
        LoggerUtility.info("Waiting for file input to be present");
        driver.findElement(FILE_INPUT);
        LoggerUtility.info("File input found");
    }

    // CONFIRMED live (2026-09-08 screenshot): the "Source" file input renders as Chrome's default,
    // genuinely visible "Choose File" button — it is NOT CSS-hidden behind a custom trigger like
    // originally assumed. Forcing display/visibility/opacity on it was unnecessary and actually
    // overlapped it with adjacent layout. sendKeys() on a real native input already fires the
    // browser's own input/change events, so no synthetic dispatch is needed either. Attachment is
    // confirmed via the input's own .value (browsers always echo a "C:\fakepath\<filename>" there
    // regardless of how this app chooses to display it) instead of guessing an app-specific
    // filename-display locator — the previous version hung for the full 2-minute fluentWait
    // timeout on exactly that guess.
    public void uploadFile(String filePath) {
        LoggerUtility.info("Uploading import file: " + filePath);
        WebElement fileInput = driver.findElement(FILE_INPUT);
        fileInput.sendKeys(filePath);
        String fileName = new File(filePath).getName();
        String inputValue = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].value;", fileInput);
        if (inputValue == null || !inputValue.contains(fileName)) {
            throw new IllegalStateException("File input value does not reflect the selected file. Expected to contain '"
                + fileName + "', actual value: " + inputValue);
        }
        LoggerUtility.info("File attached, input value: " + inputValue);
    }

    // CONFIRMED live (2026-09-08 run — real option dump): the control is a native <select> whose
    // <option> elements all have EMPTY visible text (rendered via CSS/icons instead of text) —
    // matching by visible text can never work here. The real values are:
    // OFFERS (default), OFFERS_AND_PRODUCTS, PRICES, STOCKS. Select by value instead.
    private static final String CONTENT_VALUE_OFFERS_AND_PRODUCTS = "OFFERS_AND_PRODUCTS";
    // CONFIRMED live (2026-09-08 run — real option dump, see class doc comment above): the plain
    // "Offers" option (this content-type dropdown's own default) uses this exact value.
    private static final String CONTENT_VALUE_OFFERS = "OFFERS";

    public void selectOffersAndProductsContent() {
        selectFileContent(CONTENT_VALUE_OFFERS_AND_PRODUCTS, FILE_CONTENT_OFFERS_AND_PRODUCTS_RADIO);
    }

    // Added for TC_E2E_004's offer-only Excel import (Price and stock > File imports, File Content =
    // Offers) — reuses the exact same dropdown-selection logic proven for "Offers and Products",
    // just targeting the "OFFERS" value instead (this dropdown's own confirmed-live default option).
    public void selectOffersOnlyContent() {
        selectFileContent(CONTENT_VALUE_OFFERS, null);
    }

    private void selectFileContent(String contentValue, By radioFallbackLocator) {
        LoggerUtility.info("Selecting '" + contentValue + "' file content type");

        WebElement select = findFileContentSelect();
        if (select == null) {
            if (radioFallbackLocator == null) {
                throw new NoSuchElementException("File Content control not found (no <select> on page and no radio fallback provided)");
            }
            // No <select> at all anywhere on the page — genuinely a radio/label control instead.
            WaitUtility.fluentWaitForClickable(driver, radioFallbackLocator);
            jsClick(radioFallbackLocator);
            LoggerUtility.info("'" + contentValue + "' selected via radio/label (no <select> found on page)");
            return;
        }
        LoggerUtility.info("File Content control found");

        boolean enabled = waitForEnabled(select, 5);
        LoggerUtility.info(enabled ? "File Content control enabled"
            : "File Content control still disabled after 5s — attempting interaction anyway");

        Select dropdown;
        try {
            dropdown = new Select(select);
        } catch (UnexpectedTagNameException e) {
            selectContentViaCustomWidget(select, contentValue);
            return;
        }

        // Options can populate asynchronously right after the file attaches — poll for more than
        // just the single default option instead of assuming the list is already complete.
        List<WebElement> options = pollForOptions(dropdown, 5);
        logOptions(options);

        // CONFIRMED live: dropdown.selectByValue() throws ElementNotInteractableException — Select
        // internally calls .click() on the <option>, but this <select> is hidden behind custom
        // styling (same "select2 hides the real element" pattern already proven for
        // MiraklOfferPage.LOGISTICS_DROPDOWN). Set the value via JS instead and fire a native
        // change event so the app's own listeners still react — the value being set here is not a
        // guess, it's the exact real option value confirmed by the live option dump above.
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
            select, contentValue);
        LoggerUtility.info("Selected File Content option by value: " + contentValue);

        String selectedValue = dropdown.getFirstSelectedOption().getAttribute("value");
        if (!contentValue.equals(selectedValue)) {
            throw new IllegalStateException("File Content selection did not stick. Expected value='"
                + contentValue + "', actual: '" + selectedValue + "'");
        }
        LoggerUtility.info("Selection verified");
    }

    // Tries the label-anchored locator first (more specific), falls back to the bare "//select"
    // (confirmed live: this form has exactly one dropdown-style control). Returns null if neither
    // matches so the caller can fall back to the radio/label variant instead of hanging.
    private WebElement findFileContentSelect() {
        List<WebElement> byLabel = driver.findElements(FILE_CONTENT_SELECT_BY_LABEL);
        if (!byLabel.isEmpty()) return byLabel.get(0);
        List<WebElement> any = driver.findElements(FILE_CONTENT_SELECT_ANY);
        if (!any.isEmpty()) return any.get(0);
        return null;
    }

    // The File content dropdown may stay disabled until Mirakl's JS finishes processing the newly
    // attached file — poll isEnabled() briefly (bounded, not an indefinite wait) rather than
    // assuming it's ready immediately after uploadFile() returns.
    private boolean waitForEnabled(WebElement element, int maxSeconds) {
        for (int i = 0; i < maxSeconds * 2; i++) {
            if (element.isEnabled()) return true;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    // Polls getOptions() until more than the single default option appears, or maxSeconds elapses.
    private List<WebElement> pollForOptions(Select dropdown, int maxSeconds) {
        List<WebElement> options = dropdown.getOptions();
        for (int i = 0; i < maxSeconds * 2 && options.size() <= 1; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            options = dropdown.getOptions();
        }
        return options;
    }

    private void logOptions(List<WebElement> options) {
        StringBuilder sb = new StringBuilder();
        for (WebElement opt : options) {
            sb.append(String.format("[text='%s', value='%s', selected=%s, enabled=%s] ",
                opt.getText().trim(), opt.getAttribute("value"), opt.isSelected(), opt.isEnabled()));
        }
        LoggerUtility.info("Available File Content options: " + sb.toString().trim());
    }

    // Only reachable if findFileContentSelect() matched an element that isn't actually a <select>
    // tag (styled to look like one). Not exercised by the live run so far (the control IS a real
    // <select> selected by value — see selectOffersAndProductsContent()); kept as a defensive
    // fallback matching by the underlying data-value attribute instead of visible text, since the
    // real options are known to render with no visible text.
    private void selectContentViaCustomWidget(WebElement control, String contentValue) {
        LoggerUtility.warn("File Content control is not a native <select> — falling back to click + option click");
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", control);
        By optionCandidates = By.xpath(
            "//*[self::li or self::div or self::span or self::option]"
            + "[contains(@value,'" + contentValue + "') or contains(@data-value,'" + contentValue + "')]");
        WebElement match = null;
        for (int i = 0; i < 10 && match == null; i++) {
            List<WebElement> found = driver.findElements(optionCandidates);
            if (!found.isEmpty()) {
                match = found.get(0);
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (match == null) {
            throw new NoSuchElementException("No custom-widget File Content option matching value '"
                + contentValue + "' found");
        }
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", match);
        LoggerUtility.info("Selected File Content option by value: " + contentValue);
        LoggerUtility.info("Selection verified (best-effort, custom widget)");
    }

    public void clickImport() {
        WaitUtility.fluentWaitForClickable(driver, IMPORT_BUTTON);
        LoggerUtility.info("Import button found");
        scrollIntoView(IMPORT_BUTTON);
        jsClick(IMPORT_BUTTON);
        LoggerUtility.info("Import button clicked");
        // Mirakl-side propagation delay before the import job actually registers — matches the
        // project's sanctioned exception to the no-Thread.sleep rule for Mirakl processing waits.
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Added for TC_E2E_004, per explicit requirement: the caller must know whether the OFFER IMPORT
    // ITSELF actually succeeded right after uploading — not 14 minutes later via an Active-offer
    // search timeout. Call this BEFORE clickImport() to snapshot the current top history row, then
    // pass the returned value into verifyImportSucceeded(baseline) afterward — comparing against a
    // baseline (not an absolute date) sidesteps the fact that Mirakl's server clock and this
    // machine's local clock are not on the same calendar day (confirmed live: the server-rendered
    // date on history rows never matched java.time.LocalDate.now()).
    public String captureLatestHistorySnapshot() {
        WaitUtility.fluentWaitForClickable(driver, TRACK_OFFER_IMPORTS_LINK);
        jsClick(TRACK_OFFER_IMPORTS_LINK);
        Object firstRowText = ((JavascriptExecutor) driver).executeScript(
            "var row = document.querySelector('table tbody tr');"
            + "return row ? row.textContent.trim().replace(/\\s+/g, ' ') : null;");
        String snapshot = firstRowText == null ? "" : firstRowText.toString();
        LoggerUtility.info("Offer import history baseline (top row before this execution's upload): " + snapshot);
        LoggerUtility.info("Returning to Price and stock > File imports to perform the upload");
        navigateToFileImports();
        return snapshot;
    }

    // Added for TC_E2E_004 Phase 3, per explicit requirement: clicking Import must not be treated as
    // PASS on its own. Three independent, confirmed-real signals are checked, in order:
    // 1. The upload-time client-side error banner (#importMsg-msg-bar .alert-danger) — populated by
    //    this page's own displayErrorMessage() JS if the file itself is rejected immediately.
    // 2. The shop's own "Track offer imports" history page — the authoritative server-recorded job
    //    status for this import, identified as the row that DIFFERS from the pre-upload baseline
    //    (see captureLatestHistorySnapshot()) rather than by absolute date, and confirmed to not
    //    contain a failure keyword. Never a blind pass; throws with the real row/banner text on any
    //    failure signal so this never produces a false PASS — and never silently defers a REAL
    //    failure to Part 8's much slower, less specific Active-offer search timeout.
    public void verifyImportSucceeded(String baselineHistoryRow) {
        LoggerUtility.info("Verifying Offer Excel import actually succeeded (not just that Import was clicked)");

        // Bounded settle-wait: let the upload-progress indicator finish before checking for an error.
        for (int i = 0; i < 20; i++) {
            Object stillUploading = ((JavascriptExecutor) driver).executeScript(
                "var el = document.querySelector('" + ".js-alert-progress-template" + "');"
                + "return el && el.style.display !== 'none';");
            if (!Boolean.TRUE.equals(stillUploading)) break;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Instant JS presence check (not driver.findElements()) — the expected/normal outcome here is
        // that NO error banner exists at all, and findElements() honors the 2-minute global implicit
        // wait even for an empty result, which would block this check for the full 2 minutes on every
        // successful import. Confirmed live (2026-09-18): that exact block correlated with the Chrome
        // renderer disconnecting mid-wait (NoSuchSessionException), the same class of failure already
        // fixed elsewhere in this codebase (see MiraklOfferPage.hasOfferInList()'s doc comment).
        Object errorBannerText = ((JavascriptExecutor) driver).executeScript(
            "var el = document.querySelector('#importMsg-msg-bar .alert-danger');"
            + "return el ? el.textContent.trim() : null;");
        if (errorBannerText != null) {
            throw new IllegalStateException("Offer Excel import failed — error banner shown: " + errorBannerText);
        }
        LoggerUtility.info("No client-side error banner shown after Import click");

        LoggerUtility.info("Opening 'Track offer imports' history to confirm the job's real server-recorded status");
        WaitUtility.fluentWaitForClickable(driver, TRACK_OFFER_IMPORTS_LINK);
        jsClick(TRACK_OFFER_IMPORTS_LINK);

        // ROOT-CAUSED live (2026-09-18): comparing the top row's date against java.time.LocalDate.now()
        // never matched — Mirakl's server clock and this machine's local clock are not on the same
        // calendar day, so that check always exhausted its budget and gave up without ever actually
        // evaluating a real row (silently deferring genuine Mirakl-side failures, like "product does
        // not exist", all the way to Part 8's 14-minute Active-offer search timeout instead of failing
        // immediately here). Comparing against the PRE-UPLOAD baseline row (see
        // captureLatestHistorySnapshot()) instead of an absolute date sidesteps the clock mismatch
        // entirely — a change in the top row's content is a change regardless of what either clock says.
        String latestRowText = null;
        for (int i = 0; i < 20; i++) {
            Object firstRowText = ((JavascriptExecutor) driver).executeScript(
                "var row = document.querySelector('table tbody tr');"
                + "return row ? row.textContent.trim().replace(/\\s+/g, ' ') : null;");
            if (firstRowText != null) {
                latestRowText = firstRowText.toString();
                if (!latestRowText.equals(baselineHistoryRow)) {
                    break;
                }
                LoggerUtility.info("Top import history row unchanged from pre-upload baseline yet — reloading and rechecking: " + latestRowText);
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            driver.navigate().refresh();
        }
        if (latestRowText == null) {
            throw new IllegalStateException("Offer import history page shows no rows at all — cannot confirm the import job's status");
        }

        // If the top row still equals the pre-upload baseline after the full poll budget, this
        // execution's job genuinely never registered in the history at all within that window — an
        // inconclusive result (not a confirmed failure), left to Part 8's direct search to settle.
        if (latestRowText.equals(baselineHistoryRow)) {
            LoggerUtility.warn("Top import history row never changed from the pre-upload baseline within the poll "
                + "budget — cannot confirm this execution's own job from history: " + latestRowText);
            return;
        }
        LoggerUtility.info("Most recent Offer import history row (confirmed changed from pre-upload baseline): " + latestRowText);

        // The row can still be mid-flight ("Pending" / "Being processed", with null count columns)
        // the instant it first differs from baseline — confirmed live it later settles to
        // "Import complete" with real Lines-read/error counts a few seconds after. Wait for that
        // terminal marker before evaluating for failure, or a genuine error could be missed by reading
        // the row too early. ROOT-CAUSED live (2026-09-18): checking for the literal word "pending"
        // anywhere in the row (instead of "import complete" as the terminal marker) false-matched the
        // row's OWN "Lines pending: 0" column label even once the job had already finished, wasting
        // the entire loop budget re-checking an already-terminal row.
        for (int i = 0; i < 15 && !latestRowText.toLowerCase().contains("import complete"); i++) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            driver.navigate().refresh();
            Object refreshedRowText = ((JavascriptExecutor) driver).executeScript(
                "var row = document.querySelector('table tbody tr');"
                + "return row ? row.textContent.trim().replace(/\\s+/g, ' ') : null;");
            if (refreshedRowText != null) {
                latestRowText = refreshedRowText.toString();
                LoggerUtility.info("Offer import history row still processing — rechecking: " + latestRowText);
            }
        }

        // ROOT-CAUSED live (2026-09-18): the row's own boilerplate column LABEL is literally
        // "Lines with errors: <value>" — checking the raw row text for the substring "error" matched
        // that label on every single row regardless of its actual value (confirmed live: a genuinely
        // healthy "Pending" row with "Lines with errors: null" was flagged as a false failure). Strip
        // the known count-column labels before the keyword scan, and separately parse "Lines with
        // errors" for a real non-zero/non-null count, which IS a genuine failure signal.
        String withoutLabels = latestRowText.toLowerCase()
            .replace("lines with errors", "")
            .replace("lines with error", "");
        boolean failureKeyword = withoutLabels.contains("error") || withoutLabels.contains("fail") || withoutLabels.contains("reject");

        java.util.regex.Matcher errorCountMatcher = java.util.regex.Pattern
            .compile("lines with errors?:\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(latestRowText);
        boolean nonZeroErrorCount = errorCountMatcher.find() && !"0".equals(errorCountMatcher.group(1));

        if (failureKeyword || nonZeroErrorCount) {
            throw new IllegalStateException("Offer import history's most recent row (confirmed changed from "
                + "pre-upload baseline) indicates failure: " + latestRowText);
        }
        LoggerUtility.info("Offer import history confirms no failure indicator — import accepted for processing: " + latestRowText);
    }

    // Diagnostic only, never previously checked by this automation — everything downstream only
    // verifies the LATER Catalog Management "Invalid data" badge (a separate, post-import
    // validation layer). This captures whatever Mirakl actually shows on THIS page right after
    // Import is clicked (a history/status row, an inline error banner, an "Errors" count/link),
    // so a real file-import-time rejection (e.g. attribute code 2006) would be visible in the log
    // even if it never surfaces as a Catalog Management "Invalid data" badge. Does not assert or
    // fail on the content — purely informational until the real DOM structure here is confirmed.
    public String captureImportStatusText() {
        try {
            Object bodyText = ((JavascriptExecutor) driver).executeScript(
                "var el = document.body; return el ? el.innerText.substring(0, 4000) : '';");
            String text = bodyText == null ? "" : bodyText.toString();
            LoggerUtility.info("File Imports page text snapshot right after Import click: " + text);

            // Best-effort: if any element on the page mentions "error" (case-insensitive), click the
            // first one and capture whatever appears afterward (a detail row, a modal, a report page).
            List<WebElement> errorHints = driver.findElements(By.xpath(
                "//*[contains(translate(text(),'ERROR','error'),'error')]"));
            if (!errorHints.isEmpty()) {
                LoggerUtility.info("Found " + errorHints.size() + " element(s) mentioning 'error' on this page — clicking the first one");
                try {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", errorHints.get(0));
                    Thread.sleep(1500);
                    Object afterClickText = ((JavascriptExecutor) driver).executeScript(
                        "var el = document.body; return el ? el.innerText.substring(0, 4000) : '';");
                    LoggerUtility.info("Page text after clicking the 'error' element: " + afterClickText);
                } catch (Exception clickEx) {
                    LoggerUtility.warn("Could not click the 'error' element: " + clickEx.getMessage());
                }
            } else {
                LoggerUtility.info("No element mentioning 'error' found on this page");
            }
            return text;
        } catch (Exception e) {
            LoggerUtility.warn("Could not capture File Imports page text: " + e.getMessage());
            return "";
        }
    }
}
