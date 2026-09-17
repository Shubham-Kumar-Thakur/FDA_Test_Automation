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

    public void selectOffersAndProductsContent() {
        LoggerUtility.info("Selecting 'Offers and Products' file content type");

        WebElement select = findFileContentSelect();
        if (select == null) {
            // No <select> at all anywhere on the page — genuinely a radio/label control instead.
            WaitUtility.fluentWaitForClickable(driver, FILE_CONTENT_OFFERS_AND_PRODUCTS_RADIO);
            jsClick(FILE_CONTENT_OFFERS_AND_PRODUCTS_RADIO);
            LoggerUtility.info("'Offers and Products' selected via radio/label (no <select> found on page)");
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
            selectOffersAndProductsViaCustomWidget(select);
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
            select, CONTENT_VALUE_OFFERS_AND_PRODUCTS);
        LoggerUtility.info("Selected File Content option by value: " + CONTENT_VALUE_OFFERS_AND_PRODUCTS);

        String selectedValue = dropdown.getFirstSelectedOption().getAttribute("value");
        if (!CONTENT_VALUE_OFFERS_AND_PRODUCTS.equals(selectedValue)) {
            throw new IllegalStateException("File Content selection did not stick. Expected value='"
                + CONTENT_VALUE_OFFERS_AND_PRODUCTS + "', actual: '" + selectedValue + "'");
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
    private void selectOffersAndProductsViaCustomWidget(WebElement control) {
        LoggerUtility.warn("File Content control is not a native <select> — falling back to click + option click");
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", control);
        By optionCandidates = By.xpath(
            "//*[self::li or self::div or self::span or self::option]"
            + "[contains(@value,'" + CONTENT_VALUE_OFFERS_AND_PRODUCTS + "') or contains(@data-value,'" + CONTENT_VALUE_OFFERS_AND_PRODUCTS + "')]");
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
                + CONTENT_VALUE_OFFERS_AND_PRODUCTS + "' found");
        }
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", match);
        LoggerUtility.info("Selected File Content option by value: " + CONTENT_VALUE_OFFERS_AND_PRODUCTS);
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
