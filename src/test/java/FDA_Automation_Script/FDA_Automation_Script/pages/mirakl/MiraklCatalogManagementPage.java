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
        if (attemptClickCatalogManagementSubmenu()) {
            LoggerUtility.info("Catalog Management section opened");
            return;
        }
        // ROOT-CAUSED live (TC_E2E_004, 2026-09-17, fourth occurrence): even after guarding both the
        // first click and the retry with an instant presence check (so neither ever pays the 2-minute
        // implicit wait), a live run still landed here with the URL unchanged on the Import Products
        // page — proving the very first click can genuinely just not register at all (a one-off SPA
        // click-handler miss, same general class of flakiness already handled with a retry-once
        // pattern in MiraklProductImportsPage.navigateToProductImports()), not always "click succeeded,
        // accordion collapsed" as earlier occurrences assumed. Retry the whole click sequence once
        // (including re-clicking the top-level "Catalog" accordion) before giving up.
        LoggerUtility.warn("Catalog Management navigation did not land on the target page — retrying the full click sequence once");
        if (attemptClickCatalogManagementSubmenu()) {
            LoggerUtility.info("Catalog Management section opened");
            return;
        }
        ScreenshotUtility.captureScreenshot(driver, "MiraklCatalogManagement_navFailed", ScreenshotUtility.FAIL);
        Object visibleText = ((JavascriptExecutor) driver).executeScript(
            "var out = [];"
            + "document.querySelectorAll('span,h1,h2,h3,div').forEach(function(el) {"
            + "  var t = el.textContent.trim();"
            + "  if (t && t.length > 0 && t.length < 60 && out.indexOf(t) === -1) out.push(t);"
            + "});"
            + "return out.slice(0, 60).join(' | ');");
        LoggerUtility.error("Catalog Management submenu is gone and the page-identity check "
            + "('Catalog Manager' in body text / URL) never matched after two attempts. Current URL: "
            + driver.getCurrentUrl() + ". Visible short text (diagnostic): " + visibleText);
        throw new IllegalStateException("Unable to confirm navigation to Catalog Management after 2 attempts — "
            + "see MiraklCatalogManagement_navFailed_FAIL.png and the diagnostic dump logged above");
    }

    // One full attempt at expanding the "Catalog" accordion (if needed) and clicking into "Catalog
    // Management", then confirming via the instant page-identity check. Returns false (never throws
    // or blocks on the implicit wait) if the attempt didn't land — see navigateToCatalogManagement()
    // for the retry-once-then-fail-with-diagnostics policy built on top of this.
    private boolean attemptClickCatalogManagementSubmenu() {
        // Wait for the sidebar to be genuinely interactive first — same fix already applied to
        // MiraklOfferPage.navigateToOffersSection() and the other new page objects in this test.
        WaitUtility.fluentWaitForClickable(driver, CATALOG_MENU);
        if (!isSubmenuVisible()) {
            jsClick(CATALOG_MENU);
            // ROOT-CAUSED live (TC_E2E_004, 2026-09-17): this used to be WaitUtility.fluentWait(), which
            // pays the full 2-minute implicit wait if the submenu never appears (e.g. the accordion
            // click didn't register). Bounded instant-JS poll instead, matching the pattern used
            // everywhere else in this method.
            for (int i = 0; i < 10 && !isSubmenuElementPresent(); i++) {
                sleep(500);
            }
        }
        // CONFIRMED live (2026-09-10): called right after a product-detail Save when the SPA sidebar
        // is still mid re-render — findElement can grab a reference the instant before the node is
        // replaced, so the click itself throws StaleElementReferenceException even though the menu
        // item is visually present.
        // ROOT-CAUSED live (TC_E2E_004, 2026-09-17): guarded with an instant presence check first —
        // a plain jsClick() here previously paid the full 2-minute implicit wait when the submenu was
        // genuinely absent (not stale, just not there), since NoSuchElementException wasn't caught.
        if (isSubmenuElementPresent()) {
            try {
                jsClick(CATALOG_MANAGEMENT_SUBMENU);
            } catch (org.openqa.selenium.StaleElementReferenceException e) {
                LoggerUtility.warn("Stale element clicking Catalog Management submenu — checking whether navigation already succeeded");
            }
        } else {
            LoggerUtility.info("Catalog Management submenu not present yet — skipping this click and checking page state directly");
        }
        return waitForCatalogManagementPage(10);
    }

    // Instant, non-blocking presence check (JS XPath eval, no findElement/implicit wait involved) —
    // used before ever retrying the real click, so a genuinely-gone submenu fails fast instead of
    // paying the 2-minute implicit wait. See navigateToCatalogManagement().
    private boolean isSubmenuElementPresent() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(\"//span[normalize-space()='Catalog Management' or "
            + "normalize-space()='Catalog management']\", document, null, XPathResult.BOOLEAN_TYPE, null).booleanValue;");
        return Boolean.TRUE.equals(result);
    }

    private boolean isSubmenuVisible() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(\"//span[normalize-space()='Catalog Management']\", document, null, " +
            "XPathResult.BOOLEAN_TYPE, null).booleanValue;");
        return Boolean.TRUE.equals(result);
    }

    // Instant, non-blocking page-identity check (JS textContent scan, no findElement/implicit wait
    // involved) — used to confirm whether the SPA has actually landed on the Catalog Manager page
    // before deciding whether a sidebar-click retry is needed. See navigateToCatalogManagement().
    private boolean isOnCatalogManagementPage() {
        // ROOT-CAUSED live (TC_E2E_004, 2026-09-17): a failure screenshot at the Operator's Catalog
        // Management route (/mcm/front/inventory/list — a different SPA/app than the Seller's /mmp/
        // route) showed only a loading spinner, no rendered content at all. The original check relied
        // solely on 'Catalog Manager' appearing in document.body.textContent, which only becomes true
        // once the SPA finishes rendering — but window.location.href updates immediately on
        // navigation, well before that. Checking the URL too means this returns true as soon as the
        // route actually changes, instead of waiting on render completion that a short bounded poll
        // may not cover.
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.body.textContent.indexOf('Catalog Manager') !== -1"
            + " || window.location.href.indexOf('/mcm/') !== -1"
            + " || window.location.href.indexOf('inventory/list') !== -1;");
        return Boolean.TRUE.equals(result);
    }

    private boolean waitForCatalogManagementPage(int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            if (isOnCatalogManagementPage()) {
                return true;
            }
            sleep(500);
        }
        return false;
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

    // Added for TC_E2E_004 (Operator flow) — never verified against the live DOM, same first-attempt
    // status as MiraklProductImportsPage's Import Products methods: expect to iterate from real
    // evidence. Follows the same "click filter button -> option list renders -> click matching
    // option" pattern already proven live for MiraklProductImportsPage.filterBySeller()'s Provider
    // filter, generalized to any filter button by its visible label.
    public void selectStatusFilter(String status) {
        selectGridFilter("Status", status);
    }

    public void selectBrandFilter(String brand) {
        selectGridFilter("Brand", brand);
    }

    private void selectGridFilter(String filterLabel, String optionText) {
        LoggerUtility.info("Clicking '" + filterLabel + "' filter dropdown");
        By filterButton = By.xpath("//button[normalize-space()='" + filterLabel + "']");
        WaitUtility.fluentWaitForClickable(driver, filterButton);
        jsClick(filterButton);
        sleep(800);

        // ROOT-CAUSED live (TC_E2E_004, Operator, 2026-09-17): a live run's diagnostic dump for the
        // 'Brand' filter showed ONLY the Status-facet items (New/Accepted/etc.) with no brand names at
        // all, even though this exact same dump for the exact same filter succeeded on a prior run —
        // the Brand panel's own option list appears to load asynchronously after the click (unlike the
        // Status facet counts, which seem to already be in the DOM globally) and hadn't rendered yet at
        // the fixed 800ms mark. Retry once with a longer wait before giving up, same class of fix as
        // the retry-once pattern already used for MiraklProductImportsPage's accordion clicks.
        if (!tryClickFilterOption(filterButton, filterLabel, optionText)) {
            LoggerUtility.warn("'" + filterLabel + "' filter option '" + optionText
                + "' not found on first attempt — waiting longer for the panel to finish rendering and retrying once");
            sleep(1500);
            if (!tryClickFilterOption(filterButton, filterLabel, optionText)) {
                // ROOT-CAUSED live (TC_E2E_004, Operator, 2026-09-17): a second live run confirmed this
                // isn't a rendering-delay issue at all — the full (untruncated) diagnostic dump for
                // 'Brand' contained ONLY the global sidebar's <li> items and the Status facet counts,
                // with zero brand names, on both attempts. Large facets in this Catalog Manager UI (this
                // codebase already proved it for "Provider" in
                // MiraklProductImportsPage.filterBySeller()) don't pre-render a full option list at all
                // — they show a search box, and suggestion items only appear after typing into it.
                // Falling back to that same search-then-suggest pattern before giving up.
                LoggerUtility.warn("'" + filterLabel + "' filter option '" + optionText
                    + "' still not found in a static option list — trying the search-box + suggestion-item pattern");
                if (!tryClickFilterOptionViaSearch(filterButton, filterLabel, optionText)) {
                    throw new NoSuchElementException("'" + filterLabel + "' filter option '" + optionText
                        + "' not found via either the static option list or the search-box pattern");
                }
            }
        }
        LoggerUtility.info("'" + filterLabel + "' filter set to: " + optionText);
        sleep(1000);

        // Close the filter panel so the grid re-filters (same toggle-collapse pattern already
        // proven for MiraklProductImportsPage.filterBySeller()).
        jsClick(filterButton);
        sleep(1000);
    }

    // One attempt at finding and clicking optionText within the currently-open filter panel. Returns
    // false (never throws) so the caller can decide whether to retry — see selectGridFilter() above.
    private boolean tryClickFilterOption(By filterButton, String filterLabel, String optionText) {
        Object optionsLog = ((JavascriptExecutor) driver).executeScript(
            "var out = [];"
            + "document.querySelectorAll('[role=\"option\"], .mui-suggestion-item, li').forEach(function(el) {"
            + "  var t = el.textContent.trim(); if (t && t.length < 60) out.push(t);"
            + "});"
            + "return out;");
        LoggerUtility.info("'" + filterLabel + "' filter — visible option-like elements: " + optionsLog);

        // ROOT-CAUSED live (TC_E2E_004, 2026-09-17): the diagnostic dump above proved every status/
        // brand facet renders with a trailing count, e.g. "New (0)" not "New" — an exact-text xpath
        // match against the plain option name never matches. Worse, driver.findElements() itself still
        // polls the full 2-minute implicit wait before returning an empty list (same class of bug as
        // the hub-card/badge-count issues elsewhere in this file), so the resulting "not found" throw
        // only ever surfaced after a silent 2-minute stall. Matched via JS instead: strip a trailing
        // " (<count>)" suffix from each candidate's own text before comparing, and click via JS the
        // moment a match is found — both instant, no findElement/implicit wait involved.
        // CONFIRMED live (TC_E2E_004, Operator, 2026-09-17): counts render comma-grouped for large
        // values (e.g. "New (1,831)"), not just plain digits — the strip regex must allow commas or
        // it silently fails to match on any facet with four-plus-digit counts.
        // ROOT-CAUSED live (TC_E2E_004, Operator, 2026-09-17): a live run's Step 22 grid-wide status
        // check ("Verify only the products with Status = 'New'") proved this filter never actually
        // applies to the grid at all — a post-filter screenshot showed "of 29,120 results" (the
        // catalog's total size, completely unfiltered) with every row except the newest still showing
        // "Published". Clicking the plain <li>/option element (as the click below already did)
        // reported "clicked" successfully and the visible option list even showed "New" selected, but
        // the underlying grid query was never re-triggered. Most likely cause: this is a checkbox-based
        // facet where the actual interactive control is an <input type="checkbox"> nested inside the
        // <li>, not the <li> itself — clicking the wrapper doesn't reliably toggle a nested native
        // checkbox's checked state/change event. Preferring the checkbox if one exists.
        Boolean clicked = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "var target = arguments[0];"
            + "var els = document.querySelectorAll('[role=\"option\"], .mui-suggestion-item, li');"
            + "for (var i = 0; i < els.length; i++) {"
            + "  var t = els[i].textContent.trim().replace(/\\s*\\([\\d,]+\\)$/, '');"
            + "  if (t === target) {"
            + "    var checkbox = els[i].querySelector('input[type=\"checkbox\"]');"
            + "    if (checkbox) { checkbox.click(); } else { els[i].click(); }"
            + "    return true;"
            + "  }"
            + "}"
            + "return false;",
            optionText);
        // Diagnostic: log exactly what was clicked (element tag, whether a checkbox existed and its
        // resulting checked state) plus any Apply/Confirm-like button visible in the panel — real
        // evidence for whether a separate "commit the filter" step is still missing, instead of
        // guessing again if the grid still isn't filtered after this.
        Object clickDiagnostic = ((JavascriptExecutor) driver).executeScript(
            "var target = arguments[0];"
            + "var els = document.querySelectorAll('[role=\"option\"], .mui-suggestion-item, li');"
            + "for (var i = 0; i < els.length; i++) {"
            + "  var t = els[i].textContent.trim().replace(/\\s*\\([\\d,]+\\)$/, '');"
            + "  if (t === target) {"
            + "    var checkbox = els[i].querySelector('input[type=\"checkbox\"]');"
            + "    return 'tag=' + els[i].tagName + ' hasCheckbox=' + (checkbox !== null)"
            + "      + ' checkedAfterClick=' + (checkbox ? checkbox.checked : 'n/a');"
            + "  }"
            + "}"
            + "return 'element not found for diagnostic';",
            optionText);
        LoggerUtility.info("'" + filterLabel + "' option '" + optionText + "' click diagnostic: " + clickDiagnostic);
        Object applyButtons = ((JavascriptExecutor) driver).executeScript(
            "var out = [];"
            + "document.querySelectorAll('button').forEach(function(b) {"
            + "  var t = b.textContent.trim().toLowerCase();"
            + "  if (t && (t.indexOf('apply') !== -1 || t.indexOf('confirm') !== -1 || t.indexOf('done') !== -1)) out.push(b.textContent.trim());"
            + "});"
            + "return out;");
        LoggerUtility.info("'" + filterLabel + "' filter panel — Apply/Confirm/Done-like buttons visible: " + applyButtons);
        return Boolean.TRUE.equals(clicked);
    }

    // Fallback for large facets (e.g. Brand) that don't pre-render a static option list — same
    // search-box-then-suggestion-item pattern already proven live for the Provider filter in
    // MiraklProductImportsPage.filterBySeller(). Placeholder guessed by the same naming convention as
    // that field ("Search by provider..." -> "Search by brand..."); if this guess is wrong, the "No
    // search input found" log line is the concrete next thing to check against a live screenshot.
    private boolean tryClickFilterOptionViaSearch(By filterButton, String filterLabel, String optionText) {
        String searchPlaceholder = "Search by " + filterLabel.toLowerCase() + "...";
        String searchInputXpath = "//input[@placeholder='" + searchPlaceholder + "']";
        Object present = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(arguments[0], document, null, XPathResult.BOOLEAN_TYPE, null).booleanValue;",
            searchInputXpath);
        if (!Boolean.TRUE.equals(present)) {
            LoggerUtility.info("No search input found with placeholder '" + searchPlaceholder + "' for '" + filterLabel + "' filter");
            return false;
        }
        LoggerUtility.info("Found search input for '" + filterLabel + "' filter — typing '" + optionText + "'");
        type(By.xpath(searchInputXpath), optionText);
        sleep(1500);

        Object suggestionsLog = ((JavascriptExecutor) driver).executeScript(
            "var out = [];"
            + "document.querySelectorAll('.mui-suggestion-item').forEach(function(el) {"
            + "  var t = el.textContent.trim(); if (t && t.length < 80) out.push(t);"
            + "});"
            + "return out;");
        LoggerUtility.info("'" + filterLabel + "' search suggestions after typing '" + optionText + "': " + suggestionsLog);

        Boolean clicked = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "var target = arguments[0];"
            + "var els = document.querySelectorAll('.mui-suggestion-item');"
            + "for (var i = 0; i < els.length; i++) {"
            + "  if (els[i].textContent.trim().indexOf(target) !== -1) { els[i].click(); return true; }"
            + "}"
            + "return false;",
            optionText);
        return Boolean.TRUE.equals(clicked);
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

    // Added for TC_E2E_004 — manual test case step 22: "Verify only the products with Status =
    // 'New'." This checks every visible grid row after the Status/Brand filters are applied, not
    // just the one target SKU's own row — an instant JS DOM scan (not findElements()) to avoid the
    // 2-minute implicit wait per row, same class of fix as hasProductWithStatus() above.
    public List<String> getVisibleRowsNotMatchingStatus(String expectedStatus) {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "var target = arguments[0];"
            + "var out = [];"
            + "document.querySelectorAll('tbody tr').forEach(function(row) {"
            + "  var t = row.textContent.trim();"
            + "  if (t.length > 0 && t.indexOf(target) === -1) { out.push(t.substring(0, 120)); }"
            + "});"
            + "return out;",
            expectedStatus);
        @SuppressWarnings("unchecked")
        List<Object> mismatchesRaw = (List<Object>) result;
        List<String> mismatches = new java.util.ArrayList<>();
        for (Object o : mismatchesRaw) mismatches.add(String.valueOf(o));
        LoggerUtility.info("Visible grid rows NOT matching status '" + expectedStatus + "' (" + mismatches.size() + "): " + mismatches);
        return mismatches;
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

    // Added for TC_E2E_004 Part 6: CONFIRMED live (screenshot, Seller "My catalog" grid) the grid has
    // a distinct "Product ID" column (cell also shows a second "UPC: <value>" line underneath) — per
    // explicit user instruction, once a product reaches Published, Mirakl may display a different
    // Product ID in this column than the shop_sku originally submitted, and the Offer Excel's
    // product-id column must be updated to match THIS value before Part 7's offer import, or the
    // offer will fail to link to the published product. Reads the "Product ID" column's own text
    // (the line before "UPC" if present) for whichever row's full text contains searchTerm — not just
    // shop_sku equality — since search may still match the row via other indexed fields even if the
    // displayed Product ID itself has changed.
    public String getDisplayedProductId(String searchTerm) {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "var headers = document.querySelectorAll('th');"
            + "var idx = -1;"
            + "for (var i = 0; i < headers.length; i++) {"
            + "  if (headers[i].textContent.trim().indexOf('Product ID') !== -1) { idx = i; break; }"
            + "}"
            + "if (idx === -1) return null;"
            + "var rows = document.querySelectorAll('tr');"
            + "for (var r = 0; r < rows.length; r++) {"
            + "  if (rows[r].textContent.indexOf(arguments[0]) !== -1) {"
            + "    var cells = rows[r].querySelectorAll('td');"
            + "    if (cells.length > idx) {"
            + "      return cells[idx].textContent.split('UPC')[0].trim();"
            + "    }"
            + "  }"
            + "}"
            + "return null;",
            searchTerm);
        String productId = (result == null) ? null : result.toString();
        LoggerUtility.info("Displayed 'Product ID' column value for row matching '" + searchTerm + "': " + productId);
        return productId;
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

    // Added for TC_E2E_004's explicit requirement: exactly 2 "Valid data" badges are expected on a
    // fully-corrected product detail page (Master + PharmaAtoZ/seller-scope), and "Invalid data"
    // count must be exactly 0. Excludes any exact-match "Invalid data"/"Valid data" node from BOTH
    // counts' own opposite search (a badge never simultaneously matches both phrases, but this
    // avoids double-counting a badge that might render both words in adjacent siblings). Logs each
    // matched element's own trimmed text so a live count mismatch is immediately diagnosable.
    public int countValidDataBadges() {
        return countBadges("Valid data");
    }

    public int countInvalidDataBadges() {
        return countBadges("Invalid data");
    }

    private int countBadges(String phrase) {
        // ROOT-CAUSED live (TC_E2E_004, 2026-09-17): the original xpath
        // "//*[contains(normalize-space(),'phrase')]" matched 31 elements for a page with exactly one
        // real "Valid data" badge, because normalize-space() on an ANCESTOR concatenates all descendant
        // text — so every wrapper div/body/html up the tree from the one real badge also "contains"
        // the phrase. Restricted to leaf-most matches only (element contains the phrase AND no child
        // of it also contains the phrase), same "leaf text" idiom already used elsewhere in this
        // codebase (MiraklOrderDetailPage's logVisibleMenuItemTexts()) for exactly this reason.
        Object countObj = ((JavascriptExecutor) driver).executeScript(
            "var out = [];"
            + "var r = document.evaluate(arguments[0], document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);"
            + "for (var i = 0; i < r.snapshotLength; i++) { out.push(r.snapshotItem(i).textContent.trim().substring(0,80)); }"
            + "return out;",
            "//*[contains(normalize-space(),'" + phrase + "') and not(.//*[contains(normalize-space(),'" + phrase + "')])]");
        @SuppressWarnings("unchecked")
        List<Object> matches = (List<Object>) countObj;
        LoggerUtility.info("'" + phrase + "' badge matches (" + matches.size() + "): " + matches);
        return matches.size();
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
