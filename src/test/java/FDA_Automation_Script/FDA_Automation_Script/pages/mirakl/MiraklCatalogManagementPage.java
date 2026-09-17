package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

// TODO: Not verified against the live Mirakl DOM — locators are best-effort guesses, EXCEPT where
// noted "CONFIRMED live" from an actual screenshot.
// "Catalog" as a top-level sidebar item is confirmed real (same as MiraklProductImportsPage).
public class MiraklCatalogManagementPage extends BasePage {

    private static final By CATALOG_MENU = By.xpath("//span[normalize-space()='Catalog']");
    private static final By CATALOG_MANAGEMENT_SUBMENU = By.xpath(
        "//span[normalize-space()='Catalog Management' or normalize-space()='Catalog management']");

    // CONFIRMED live (2026-09-08 screenshot): the page is titled "Catalog Manager" with tabs
    // "My catalog" / "To review" / "Platform Catalog". There is NO dedicated "SKU"/"Product ID"
    // field, filter panel, or expandable section to open — the always-visible search box is a
    // single control: a scope dropdown ("All") + text input with the real placeholder
    // "Search by identifier, title or vgc" + an adjacent magnifying-glass icon button. The old
    // "SKU"/"Product ID" placeholder guess never existed in this DOM, hence the 2-minute timeouts.
    private static final By SKU_SEARCH_FIELD = By.xpath(
        "//input[contains(@placeholder,'identifier') or contains(@placeholder,'SKU') or contains(@placeholder,'Product ID')]");
    private static final By TO_REVIEW_TAB = By.xpath(
        "//*[self::a or self::button or self::span or self::li][normalize-space()='To review']");
    // REMOVED (2026-09-08 live evidence): data-testid='search__svg' actually matched the GLOBAL
    // "Ctrl+K" command-palette icon in the top nav on this page, not a local grid search icon —
    // clicking it popped open an unrelated search modal. The live screenshot also confirmed typing
    // alone already filters the grid (both SKU rows appeared correctly with no click needed), so
    // the icon click is not just wrong but unnecessary.

    // --- Product detail page (after clicking a product name link) ---
    // "Invalid data"/"Valid data" badges are checked via instant JS in isInvalidDataDisplayed()/
    // isValidDataDisplayed() below, not a static By — see those methods for why.
    // CONFIRMED live (2026-09-09 settled screenshot): rendered as a pencil-icon + "Edit" link next
    // to the product title (e.g. "Jellydancer Baby Toy  ✏ Edit"), NOT a plain <button> —
    // matches either tag by exact trimmed text instead of assuming <button>.
    private static final By EDIT_BUTTON = By.xpath("//*[self::a or self::button][normalize-space()='Edit']");
    // CONFIRMED live (2026-09-09 full DOM dump): Fragile and MSI are custom div-based Bootstrap
    // dropdown widgets (data-testid="<field>-valuesDropdown") — see setDropdownFieldToValidOption()
    // below for the full story.
    private static final By SAVE_BUTTON = By.xpath("//button[normalize-space()='Save']");

    public MiraklCatalogManagementPage(WebDriver driver) {
        super(driver);
    }

    public void navigateToCatalogManagement() {
        LoggerUtility.info("Navigating to Catalog > Catalog Management");
        // Wait for the sidebar to be genuinely interactive first — same fix already applied to
        // MiraklOfferPage.navigateToOffersSection() and the other new page objects in this test.
        WaitUtility.fluentWaitForClickable(driver, CATALOG_MENU);
        if (!isSubmenuVisible()) {
            jsClick(CATALOG_MENU);
            WaitUtility.fluentWait(driver, CATALOG_MANAGEMENT_SUBMENU);
        }
        // CONFIRMED live (2026-09-10): called right after a product-detail Save when the SPA sidebar
        // is still mid re-render — findElement can grab a reference the instant before the node is
        // replaced, so the click itself throws StaleElementReferenceException even though the menu
        // item is visually present. Same retry-once pattern as MiraklOrderDetailPage.clickOrderInList().
        try {
            jsClick(CATALOG_MANAGEMENT_SUBMENU);
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            LoggerUtility.warn("Stale element clicking Catalog Management submenu — retrying");
            jsClick(CATALOG_MANAGEMENT_SUBMENU);
        }
        LoggerUtility.info("Catalog Management section opened");
    }

    private boolean isSubmenuVisible() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(\"//span[normalize-space()='Catalog Management']\", document, null, " +
            "XPathResult.BOOLEAN_TYPE, null).booleanValue;");
        return Boolean.TRUE.equals(result);
    }

    // Confirmed live for Seller: Catalog Manager has "My catalog" (default) / "To review" /
    // "Platform Catalog" tabs. A not-yet-accepted product may only appear under "To review" — never
    // confirmed whether Operator's view has the same tabs, so this is tolerant: instant JS check,
    // clicks if present, just logs and continues if not (never throws).
    public void clickToReviewTabIfPresent() {
        Object present = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(\"//*[normalize-space()='To review']\", document, null, "
                + "XPathResult.BOOLEAN_TYPE, null).booleanValue;");
        if (Boolean.TRUE.equals(present)) {
            LoggerUtility.info("'To review' tab found — clicking it");
            jsClick(TO_REVIEW_TAB);
            sleep(2000);
        } else {
            LoggerUtility.info("'To review' tab not present on this page — staying on current/default tab");
        }
    }

    // Instant JS DOM check instead of isDisplayed()/findElement() — the latter blocks for the full
    // 2-minute implicit wait on every failed poll attempt when the row is genuinely absent (confirmed
    // live in MiraklProductImportsPage's identical pattern: ~2.5 min/attempt instead of ~30s).
    public boolean hasProductWithStatus(String productIdentifier, String status) {
        String xpath = "//tr[contains(.,'" + productIdentifier + "') and contains(.,'" + status + "')]";
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(arguments[0], document, null, XPathResult.BOOLEAN_TYPE, null).booleanValue;",
            xpath);
        boolean present = Boolean.TRUE.equals(result);
        LoggerUtility.info("Product " + productIdentifier + " with status '" + status + "' present: " + present);
        return present;
    }

    // Diagnostic only — logs the full rendered text of this product's row (includes whatever the
    // grid's "Related offers" column currently shows for it), so the actual linked-offer count is
    // visible in the log without needing a live screenshot round-trip to check.
    public void logProductRowDetails(String productIdentifier) {
        Object rowText = ((JavascriptExecutor) driver).executeScript(
            "var rows = document.querySelectorAll('tr');"
            + "for (var i = 0; i < rows.length; i++) {"
            + "  if (rows[i].textContent.indexOf(arguments[0]) !== -1) return rows[i].textContent;"
            + "}"
            + "return null;",
            productIdentifier);
        LoggerUtility.info("Product " + productIdentifier + " — full row text (includes Related offers column): " + rowText);
    }

    // CONFIRMED live (2026-09-09): Catalog Management's own status vocabulary for a product record
    // does not necessarily include "New" — that word may only apply to the separate Catalog imports/
    // Product Imports batch-level view. Rather than guess which single word applies, accept ANY of a
    // list of candidate status words and log the row's ACTUAL rendered text so the real value is
    // visible in the log without needing another live probe round.
    public boolean hasProductWithAnyStatus(String productIdentifier, List<String> statuses) {
        Object rowText = ((JavascriptExecutor) driver).executeScript(
            "var rows = document.querySelectorAll('tr');"
            + "for (var i = 0; i < rows.length; i++) {"
            + "  if (rows[i].textContent.indexOf(arguments[0]) !== -1) return rows[i].textContent;"
            + "}"
            + "return null;",
            productIdentifier);
        LoggerUtility.info("Product " + productIdentifier + " — actual row text found: " + rowText);

        if (rowText == null) {
            LoggerUtility.info("Product " + productIdentifier + " with any status " + statuses + " present: false (row not found)");
            return false;
        }
        String rowTextLower = ((String) rowText).toLowerCase();
        for (String status : statuses) {
            if (rowTextLower.contains(status.toLowerCase())) {
                LoggerUtility.info("Product " + productIdentifier + " matched status '" + status + "'");
                return true;
            }
        }
        LoggerUtility.info("Product " + productIdentifier + " with any status " + statuses + " present: false");
        return false;
    }

    // --- Per-SKU data validation/correction flow (Fragile/MSI) ---

    // Rewritten per live screenshot evidence: no filter panel exists to open — the search box is
    // always visible on the "My catalog" tab. CONFIRMED live: typing alone already filters the grid
    // (both SKU rows appeared correctly) — no icon click needed, and the icon locator previously
    // used here actually matched the unrelated global "Ctrl+K" command palette. Enter is kept only
    // as a harmless no-op safety net in case a given deployment needs it to commit the filter.
    public void searchBySku(String sku) {
        LoggerUtility.info("Searching Catalog Management for SKU: " + sku);

        // Instant JS presence check before ever calling fluentWaitForClickable() — if the primary
        // locator genuinely doesn't match on this page (confirmed live: it doesn't on Catalog
        // imports), fail fast with a diagnostic dump of every input actually present instead of
        // burning the full 2-minute wait first.
        Object primaryPresent = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(\"//input[contains(@placeholder,'identifier') or contains(@placeholder,'SKU') or contains(@placeholder,'Product ID')]\", "
            + "document, null, XPathResult.BOOLEAN_TYPE, null).booleanValue;");

        By fieldToUse = SKU_SEARCH_FIELD;
        if (!Boolean.TRUE.equals(primaryPresent)) {
            LoggerUtility.warn("Primary identifier search field not found — trying broader fallback locator");
            By fallback = By.xpath(
                "//input[contains(@placeholder,'identifier') or contains(@placeholder,'SKU') or "
                + "contains(@placeholder,'Product ID') or contains(@placeholder,'Search') or @type='search']");
            Object fallbackPresent = ((JavascriptExecutor) driver).executeScript(
                "return document.evaluate(arguments[0], document, null, XPathResult.BOOLEAN_TYPE, null).booleanValue;",
                "//input[contains(@placeholder,'identifier') or contains(@placeholder,'SKU') or "
                + "contains(@placeholder,'Product ID') or contains(@placeholder,'Search') or @type='search']");
            if (Boolean.TRUE.equals(fallbackPresent)) {
                fieldToUse = fallback;
            } else {
                Object allInputs = ((JavascriptExecutor) driver).executeScript(
                    "return Array.from(document.querySelectorAll('input')).map(function(i){"
                    + "return '[placeholder=\"'+(i.placeholder||'')+'\" type=\"'+i.type+'\" name=\"'+(i.name||'')+'\"]';});");
                LoggerUtility.warn("No search input found by any known locator on this page. All <input> elements present: " + allInputs);
            }
        }

        WaitUtility.fluentWaitForClickable(driver, fieldToUse);
        LoggerUtility.info("Search control found");

        ScreenshotUtility.captureScreenshot(driver, "MiraklCatalogManagement_beforeSearch", ScreenshotUtility.INFO);

        type(fieldToUse, sku);
        LoggerUtility.info("SKU entered: " + sku);

        pressEnter(fieldToUse);
        LoggerUtility.info("Search executed");

        // Brief settle time for the grid's AJAX refresh to start — the caller's pollUntil() on
        // hasProductRow() is what actually waits for the row to appear.
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        ScreenshotUtility.captureScreenshot(driver, "MiraklCatalogManagement_afterSearch", ScreenshotUtility.INFO);
    }

    public boolean hasProductRow(String sku) {
        String xpath = "//tr[contains(.,'" + sku + "')]";
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(arguments[0], document, null, XPathResult.BOOLEAN_TYPE, null).booleanValue;",
            xpath);
        boolean present = Boolean.TRUE.equals(result);
        LoggerUtility.info("Catalog Management row for SKU " + sku + " present: " + present);
        return present;
    }

    public void clickProductNameLink(String sku) {
        LoggerUtility.info("Clicking product name link for SKU: " + sku);
        By link = By.xpath("//tr[contains(.,'" + sku + "')]//a");
        WaitUtility.fluentWait(driver, link);
        jsClick(link);
    }

    // Instant JS DOM check instead of isDisplayed() — CONFIRMED live (2026-09-09): once Save
    // genuinely clears "Invalid data", isDisplayed() on the now-absent badge honors the full
    // 2-minute implicit wait before returning false (same stall pattern already fixed elsewhere in
    // this file for hasProductRow()/hasProductWithStatus()) — it doesn't throw, it just blocks.
    public boolean isInvalidDataDisplayed() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(\"//*[contains(normalize-space(),'Invalid data')]\", document, null, "
                + "XPathResult.BOOLEAN_TYPE, null).booleanValue;");
        boolean displayed = Boolean.TRUE.equals(result);
        LoggerUtility.info("'Invalid data' badge displayed: " + displayed);
        if (displayed) {
            logValidationErrorMessages();
        }
        return displayed;
    }

    // Diagnostic only — logs every element whose class suggests it's a validation error/warning
    // message, so WHY a product is Invalid (beyond just Fragile/MSI) is visible directly in the log
    // instead of requiring a live screenshot round-trip to inspect the product detail page manually.
    private void logValidationErrorMessages() {
        Object errorTexts = ((JavascriptExecutor) driver).executeScript(
            "return Array.from(document.querySelectorAll("
            + "'[class*=\"error\"], [class*=\"invalid\"], [class*=\"alert\"], [class*=\"warning\"]'))"
            + ".map(function(e){return e.textContent.trim();})"
            + ".filter(function(t){return t && t.length < 300;});");
        LoggerUtility.info("Validation error/warning elements found on product detail page: " + errorTexts);
    }

    public boolean isValidDataDisplayed() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(\"//*[contains(normalize-space(),'Valid data')]\", document, null, "
                + "XPathResult.BOOLEAN_TYPE, null).booleanValue;");
        boolean displayed = Boolean.TRUE.equals(result);
        LoggerUtility.info("'Valid data' badge displayed: " + displayed);
        return displayed;
    }

    public void clickEdit() {
        LoggerUtility.info("Clicking Edit on product detail page");
        WaitUtility.fluentWaitForClickable(driver, EDIT_BUTTON);
        jsClick(EDIT_BUTTON);
    }

    private static final String CASE_INSENSITIVE_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String CASE_INSENSITIVE_LOWER = "abcdefghijklmnopqrstuvwxyz";

    // CONFIRMED live (2026-09-09, full DOM dump of the "Edit my version" page): Fragile and MSI are
    // NOT <input>/<select> elements at all — they're Bootstrap-style "dropdown btn-group" divs with
    // NO real form control inside, e.g.:
    //   <div data-testid="is_fragile-valuesDropdown" class="form-group mui-form-group-autocomplete ...">
    //     <label id="['UNSAFE_STRING_PREFIX_is_fragile']" for="['UNSAFE_STRING_PREFIX_is_fragile']">...
    //     <div class="mui-form-control-autocomplete"><div class="dropdown btn-group">
    //       <div class="form-control mui-custom-toggle" ...><div class="...">Si</div>...
    // Every previous strategy (id/name-contains, label 'for'-attribute, following::, spatial
    // proximity via getBoundingClientRect) searched for <input>/<select> tags, so all of them
    // skipped straight past this div-only widget and landed on the very next REAL <input> on the
    // page instead — confirmed live via screenshot to be "Brand Store Image 1" immediately below.
    // The container's `data-testid="<field>-valuesDropdown"` attribute is clean (not mangled like
    // the id/for attributes) and uniquely identifies the field — use it directly instead of any
    // DOM-order or geometry heuristic.
    public void setFragile() {
        LoggerUtility.info("Locating and selecting a valid Fragile option");
        setDropdownFieldToValidOption("fragile", "Fragile", null);
    }

    // CONFIRMED live (2026-09-09): the real valid options for this field are exactly "No" and "Si" —
    // use this overload when the manual test spec calls for a specific value (e.g. "Set Fragile to
    // 'Si'" for a later SKU) rather than "any valid option".
    public void setFragile(String preferredValue) {
        LoggerUtility.info("Locating and selecting Fragile option: '" + preferredValue + "'");
        setDropdownFieldToValidOption("fragile", "Fragile", preferredValue);
    }

    public String setMsi() {
        LoggerUtility.info("Locating and selecting a valid MSI option");
        return setDropdownFieldToValidOption("msi", "MSI", null);
    }

    // Returns the ACTUAL visible option text that was clicked, so a caller can verify the live
    // Mirakl UI ended up showing the exact value it asked for (e.g. "No"), not just assume the
    // click succeeded.
    public String setMsi(String preferredValue) {
        LoggerUtility.info("Locating and selecting MSI option: '" + preferredValue + "'");
        return setDropdownFieldToValidOption("msi", "MSI", preferredValue);
    }

    // CONFIRMED live (2026-09-12): MSI's dropdown renders as EITHER "Yes"/"No" or "Si"/"No" depending
    // on session/row — requesting a fixed string like "Yes" and falling back to "first option" on a
    // miss (setMsi(String)'s behavior) silently selects the WRONG option when the variant is Si/No
    // (falls back to "No" instead of "Si"). This reads the ACTUAL live suggestion texts first,
    // classifies each as true-like ("yes"/"si") or false-like ("no"), and clicks whichever matches
    // the given boolean intent — never searches for the literal strings "true"/"false", and never
    // falls back to an arbitrary option if no semantic match exists.
    public String setMsiForBoolean(boolean trueLike, String rawExcelValue) {
        LoggerUtility.info("Locating and selecting MSI option for boolean intent: " + trueLike);
        String containerXpath = "//div[contains(translate(@data-testid, '"
            + CASE_INSENSITIVE_UPPER + "', '" + CASE_INSENSITIVE_LOWER + "'), 'msi-valuesdropdown')]";

        for (int i = 0; i < 15; i++) {
            Object found = ((JavascriptExecutor) driver).executeScript(
                "return document.evaluate(arguments[0], document, null, XPathResult.BOOLEAN_TYPE, null).booleanValue;",
                containerXpath);
            if (Boolean.TRUE.equals(found)) {
                if (i > 0) LoggerUtility.info("MSI container appeared after " + i + " scroll step(s)");
                break;
            }
            ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 400);");
            sleep(400);
        }

        List<WebElement> containers = driver.findElements(By.xpath(containerXpath));
        if (containers.isEmpty()) {
            throw new NoSuchElementException("MSI container not found via data-testid containing 'msi-valuesDropdown'");
        }
        WebElement container = containers.get(0);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", container);
        sleep(300);
        LoggerUtility.info("MSI container located via data-testid='" + container.getAttribute("data-testid") + "'");

        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].style.outline='4px solid red'; arguments[0].style.outlineOffset='2px';", container);
        ScreenshotUtility.captureScreenshot(driver, "MiraklCatalogManagement_MSI_resolvedField", ScreenshotUtility.INFO);

        List<WebElement> toggles = container.findElements(By.xpath(".//div[contains(@class,'mui-custom-toggle')]"));
        if (toggles.isEmpty()) {
            throw new NoSuchElementException("MSI container found but no 'mui-custom-toggle' inside it");
        }
        toggles.get(0).click();
        sleep(1000);

        List<WebElement> suggestions = container.findElements(By.xpath(".//div[contains(@class,'mui-suggestion-item')]"));
        if (suggestions.isEmpty()) {
            List<WebElement> searchInputs = container.findElements(By.xpath(".//input"));
            if (!searchInputs.isEmpty()) {
                searchInputs.get(0).sendKeys("N");
                sleep(1000);
                suggestions = container.findElements(By.xpath(".//div[contains(@class,'mui-suggestion-item')]"));
            }
        }
        if (suggestions.isEmpty()) {
            LoggerUtility.warn("MSI dropdown opened but no mui-suggestion-item found inside its own container");
            throw new NoSuchElementException("MSI dropdown opened but no suggestion options found");
        }

        LoggerUtility.info("MSI suggestions found: " + suggestions.size());
        for (WebElement s : suggestions) {
            LoggerUtility.info("  suggestion text='" + s.getText().trim() + "'");
        }

        WebElement targetOption = null;
        for (WebElement s : suggestions) {
            String text = s.getText().trim().toLowerCase();
            boolean isTrueLike = text.equals("yes") || text.equals("si");
            boolean isFalseLike = text.equals("no");
            if ((trueLike && isTrueLike) || (!trueLike && isFalseLike)) {
                targetOption = s;
                break;
            }
        }
        if (targetOption == null) {
            StringBuilder found = new StringBuilder();
            for (WebElement s : suggestions) found.append("'").append(s.getText().trim()).append("' ");
            String expectedSemantic = trueLike ? "Yes or Si" : "No";
            throw new NoSuchElementException("MSI dropdown selection failed — Excel MSI value='" + rawExcelValue
                + "', mapped semantic value='" + expectedSemantic + "', actual live dropdown options=["
                + found.toString().trim() + "]");
        }

        String optionText = targetOption.getText().trim();
        try {
            targetOption.click();
        } catch (Exception e) {
            LoggerUtility.warn("MSI real click failed (" + e.getMessage() + ") — falling back to JS click");
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", targetOption);
        }
        sleep(500);

        boolean errorStillPresent = !container.findElements(
            By.xpath(".//*[contains(text(),'does not belong to the list')]")).isEmpty();
        LoggerUtility.info("MSI selected option '" + optionText + "' — error still present in container: " + errorStillPresent);
        return optionText;
    }

    private String setDropdownFieldToValidOption(String testIdKeyword, String fieldName, String preferredValue) {
        String lower = testIdKeyword.toLowerCase();
        String containerXpath = "//div[contains(translate(@data-testid, '"
            + CASE_INSENSITIVE_UPPER + "', '" + CASE_INSENSITIVE_LOWER + "'), '" + lower + "-valuesdropdown')]";

        // Scroll incrementally until the container is in the DOM — instant JS check, not
        // findElements(), to avoid the confirmed-live 2-minute implicit-wait stall per failed attempt.
        for (int i = 0; i < 15; i++) {
            Object found = ((JavascriptExecutor) driver).executeScript(
                "return document.evaluate(arguments[0], document, null, XPathResult.BOOLEAN_TYPE, null).booleanValue;",
                containerXpath);
            if (Boolean.TRUE.equals(found)) {
                if (i > 0) LoggerUtility.info(fieldName + " container appeared after " + i + " scroll step(s)");
                break;
            }
            ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 400);");
            sleep(400);
        }

        List<WebElement> containers = driver.findElements(By.xpath(containerXpath));
        if (containers.isEmpty()) {
            throw new NoSuchElementException(fieldName
                + " container not found via data-testid containing '" + lower + "-valuesDropdown'");
        }
        WebElement container = containers.get(0);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", container);
        sleep(300);
        LoggerUtility.info(fieldName + " container located via data-testid='"
            + container.getAttribute("data-testid") + "'");

        // Visual proof captured BEFORE any interaction — confirmed necessary live (2026-09-09):
        // every prior field-resolution strategy silently landed on the wrong element more than
        // once, and only a screenshot taken before acting on it made each guess self-verifying
        // without needing another live MFA round-trip to diagnose.
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].style.outline='4px solid red'; arguments[0].style.outlineOffset='2px';", container);
        ScreenshotUtility.captureScreenshot(driver, "MiraklCatalogManagement_" + fieldName + "_resolvedField",
            ScreenshotUtility.INFO);

        List<WebElement> toggles = container.findElements(By.xpath(".//div[contains(@class,'mui-custom-toggle')]"));
        if (toggles.isEmpty()) {
            throw new NoSuchElementException(fieldName + " container found but no 'mui-custom-toggle' inside it");
        }
        WebElement toggle = toggles.get(0);
        toggle.click();
        sleep(1000);

        // Suggestion items are scoped to THIS container's own dropdown wrapper — never a page-wide
        // search — so a suggestion list left open elsewhere on the page (e.g. a different
        // autocomplete field) can't be mistaken for this field's own options.
        List<WebElement> suggestions = container.findElements(By.xpath(".//div[contains(@class,'mui-suggestion-item')]"));
        if (suggestions.isEmpty()) {
            // CONFIRMED live: the toggle's display div doesn't always immediately reveal suggestion
            // items — some renders need an actual keystroke into the toggle's own search input
            // (id prefix "input-search-single-") to populate the menu.
            List<WebElement> searchInputs = container.findElements(By.xpath(".//input"));
            if (!searchInputs.isEmpty()) {
                searchInputs.get(0).sendKeys("N");
                sleep(1000);
                suggestions = container.findElements(By.xpath(".//div[contains(@class,'mui-suggestion-item')]"));
            }
        }
        if (suggestions.isEmpty()) {
            LoggerUtility.warn(fieldName + " dropdown opened but no mui-suggestion-item found inside its own container");
            throw new NoSuchElementException(fieldName + " dropdown opened but no suggestion options found");
        }

        LoggerUtility.info(fieldName + " suggestions found: " + suggestions.size());
        for (WebElement s : suggestions) {
            LoggerUtility.info("  suggestion text='" + s.getText().trim() + "'");
        }

        WebElement targetOption = suggestions.get(0);
        if (preferredValue != null && !preferredValue.isBlank()) {
            for (WebElement s : suggestions) {
                if (s.getText().trim().equalsIgnoreCase(preferredValue.trim())) {
                    targetOption = s;
                    break;
                }
            }
            if (!targetOption.getText().trim().equalsIgnoreCase(preferredValue.trim())) {
                LoggerUtility.warn(fieldName + " preferred value '" + preferredValue
                    + "' not found among suggestions — falling back to first option");
            }
        }
        String optionText = targetOption.getText().trim();
        try {
            targetOption.click();
        } catch (Exception e) {
            LoggerUtility.warn(fieldName + " real click failed (" + e.getMessage() + ") — falling back to JS click");
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", targetOption);
        }
        sleep(500);

        boolean errorStillPresent = !container.findElements(
            By.xpath(".//*[contains(text(),'does not belong to the list')]")).isEmpty();
        LoggerUtility.info(fieldName + " selected option '" + optionText + "' — error still present in container: "
            + errorStillPresent);
        return optionText;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void clickSave() {
        LoggerUtility.info("Clicking Save on product detail page");
        WaitUtility.fluentWaitForClickable(driver, SAVE_BUTTON);
        jsClick(SAVE_BUTTON);
        // CONFIRMED live (2026-09-09): the Invalid/Valid data badge does not update in the DOM the
        // instant Save is clicked — the SPA needs a moment to process the save and re-render. Without
        // this, isValidDataDisplayed() called immediately after clickSave() reads the pre-save DOM
        // and reports false even though the save genuinely succeeded (reproduced live: real run
        // failed the very next assertion with 'Valid data' displayed=false right after a successful
        // Save click).
        sleep(3000);
    }

    public void navigateBackToList() {
        LoggerUtility.info("Navigating back to Catalog Management list");
        navigateToCatalogManagement();
        // CONFIRMED live (2026-09-10): the very next searchBySku() call for the NEXT SKU in the loop
        // sometimes found ZERO <input> elements on the page at all ("All <input> elements present:
        // []") — the list page hadn't finished rendering yet. A settle wait here (before the caller
        // searches) is cheaper and more reliable than relying on searchBySku()'s own fallback to
        // recover from a page that hasn't rendered anything yet.
        sleep(3000);
    }
}
