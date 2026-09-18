package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

// CONFIRMED live (2026-09-09, Operator sidebar dump): the "Catalog" accordion's real submenu items
// are "Catalog management", "Catalog imports", "Templates", "Products" — there is NO "Product
// Imports" item at all (the manual test spec's step 6/38 wording "Product Imports" refers to this
// "Catalog imports" page). CONFIRMED live (fully-settled dump): "Catalog imports" itself is NOT a
// data grid at all — it's a landing/hub page with three link cards: "Shop product imports"
// (data-testid="hub-card-SELLER_PRODUCT_IMPORTS", href /mmp/operator/product/import/history —
// "Track platform products imported by sellers. Manage errors and relaunch imports."), "Platform
// category list", and "Catalog product synchronizations". The real per-product New/Pending/
// Published status grid lives one click further in, under "Shop product imports".
public class MiraklProductImportsPage extends BasePage {

    private static final By CATALOG_MENU = By.xpath("//span[normalize-space()='Catalog']");
    // ROOT-CAUSED live (TC_E2E_003): this class's own original assumption — that the manual spec's
    // "Product Imports" wording always refers to the Operator's "Catalog imports" label — was only ever
    // confirmed against an OPERATOR sidebar dump, never a Seller's. Calling this as SELLER (right after
    // a Create Product submission) timed out twice (4 full minutes) waiting for literal "Catalog
    // imports"/"Catalog Imports" text. The manual spec itself (Steps 53-54) says "expand Catalog" then
    // "Click on Product Imports" verbatim — matching a literal "Product Imports" label just as plausibly
    // as "Catalog imports". Broadened to match either, so this works regardless of which role/label
    // variant is actually rendered.
    private static final By PRODUCT_IMPORTS_SUBMENU = By.xpath(
        "//span[normalize-space()='Catalog imports' or normalize-space()='Catalog Imports' "
        + "or normalize-space()='Product Imports' or normalize-space()='Product imports']");
    private static final By SHOP_PRODUCT_IMPORTS_CARD = By.cssSelector("[data-testid='hub-card-SELLER_PRODUCT_IMPORTS']");

    // CONFIRMED live (2026-09-09, Operator Catalog Management full DOM dump): there is NO plain
    // "Seller" text input anywhere on this grid — the manual spec's "Click on 'Seller' and enter the
    // seller name" refers to the grid's "Provider" filter button, which opens a panel containing a
    // "Search by provider..." search input and a checkbox list of provider names with counts (a
    // "mui-suggestion-item" widget — the exact same pattern already proven live for the Fragile/MSI
    // fields in MiraklCatalogManagementPage).
    private static final By PROVIDER_FILTER_BUTTON = By.xpath("//button[normalize-space()='Provider']");
    private static final By PROVIDER_SEARCH_INPUT = By.xpath("//input[@placeholder='Search by provider...']");
    private static final By NEW_STATUS_ROW_CHECKBOXES = By.xpath(
        "//tr[contains(.,'New')]//input[@type='checkbox']");
    private static final By MORE_ACTIONS_BUTTON = By.xpath(
        "//button[contains(normalize-space(),'More Actions') or contains(normalize-space(),'More actions')]");
    // CONFIRMED live (2026-09-10): the More Actions dropdown renders BOTH an <li> wrapper and an
    // <a> inside it with the same text "Edit catalogs" — clicking the <li> (a non-interactive
    // wrapper) silently does nothing, leaving the dropdown open. Only the <a> is the real clickable
    // element.
    private static final By EDIT_CATALOGS_OPTION = By.xpath(
        "//a[normalize-space()='Edit Catalogs' or normalize-space()='Edit catalogs']");
    // CONFIRMED live (2026-09-10): "Edit catalogs" is NOT a checkbox list at all — it's a combobox
    // picker ("Add to catalogs" / "Remove from catalogs"), real dialog text: "You are about to
    // change catalogs for 1 product." The "Add to catalogs" trigger is
    // role="combobox" id="catalogsToAdd__trigger" — same click-trigger-then-pick-option pattern
    // already proven for Fragile/MSI.
    private static final By CATALOGS_TO_ADD_TRIGGER = By.id("catalogsToAdd__trigger");
    private static final By CATALOG_POPUP_CONFIRM_BUTTON = By.xpath(
        "//div[@role='dialog']//button[normalize-space()='Confirm']");
    private static final By ACCEPT_BUTTON = By.xpath("//button[normalize-space()='Accept']");
    private static final By ACCEPT_PRODUCTS_POPUP = By.xpath(
        "//div[@role='dialog'][contains(.,'Accept Products') or contains(.,'Accept products')]");
    private static final By ACCEPT_POPUP_CONFIRM_BUTTON = By.xpath(
        "//div[@role='dialog']//button[normalize-space()='Confirm']");

    // TODO: none of these locators have been verified against the live DOM yet — this whole
    // Product-only import sub-flow (TC_E2E_004 Part 1) is new territory for this page object, which
    // until now only ever handled the Operator's accept-into-catalog flow and the status grid. Expect
    // the first live run to need iteration, same as every other new locator set in this project's
    // history (see MiraklFileImportsPage/MiraklOfferPage's own doc comments for that pattern).
    private static final By IMPORT_PRODUCTS_BUTTON = By.xpath(
        "//button[contains(normalize-space(),'Import Products') or contains(normalize-space(),'Import products')] | "
        + "//a[contains(normalize-space(),'Import Products') or contains(normalize-space(),'Import products')]");
    private static final By PRODUCT_FILE_INPUT = By.xpath("//input[@type='file']");
    private static final By CONFIRM_PRODUCT_IMPORT_BUTTON = By.xpath("//button[normalize-space()='Confirm']");

    public MiraklProductImportsPage(WebDriver driver) {
        super(driver);
    }

    // Part 1, Step 3: "Click Import Products" — a distinct action from navigateToProductImports()
    // (which lands on the per-product status grid via the "Shop product imports" card). Called
    // right after navigateToProductImports() lands on the Catalog imports hub page.
    public void clickImportProducts() {
        LoggerUtility.info("Clicking Import Products");
        WaitUtility.fluentWaitForClickable(driver, IMPORT_PRODUCTS_BUTTON);
        // Diagnostic: log which element(s) this xpath actually matched (text, tag, href) — this
        // whole sub-flow is unverified, so if the click lands on the wrong thing, this is the first
        // evidence to look at rather than guessing again.
        Object matches = ((JavascriptExecutor) driver).executeScript(
            "var out = [];"
            + "document.evaluate(arguments[0], document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);"
            + "var r = document.evaluate(arguments[0], document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);"
            + "for (var i = 0; i < r.snapshotLength; i++) {"
            + "  var el = r.snapshotItem(i);"
            + "  out.push(el.tagName + \" text='\" + el.textContent.trim().substring(0,60) + \"' href='\" + (el.getAttribute('href')||'') + \"'\");"
            + "}"
            + "return out.join(' | ');",
            "//button[contains(normalize-space(),'Import Products') or contains(normalize-space(),'Import products')] | "
            + "//a[contains(normalize-space(),'Import Products') or contains(normalize-space(),'Import products')]");
        LoggerUtility.info("'Import Products' locator matched: " + matches);
        jsClick(IMPORT_PRODUCTS_BUTTON);
        LoggerUtility.info("Page URL after clicking Import Products: " + driver.getCurrentUrl());
    }

    // ROOT-CAUSED live (TC_E2E_004): a real screenshot taken right after sendKeys() showed the file
    // ("products-en_US-....xlsx - 65.14 kB") already correctly attached and displayed by this
    // drag-and-drop-styled React uploader — sendKeys() on the CSS-hidden <input type=file> worked
    // the very first time. The failure was a false negative in THIS method's own verification: it
    // read the raw input's .value attribute, which this component never populates (it manages the
    // attached file via its own JS state, not the native input value) — clicking a "Select File"
    // trigger and retrying never had any real effect either, since the upload had already succeeded.
    // Verify via the visible filename+size confirmation text the UI actually renders instead.
    public void uploadProductFile(String filePath) {
        LoggerUtility.info("Uploading Product Excel file: " + filePath);
        String fileName = new java.io.File(filePath).getName();

        List<WebElement> fileInputs = driver.findElements(PRODUCT_FILE_INPUT);
        LoggerUtility.info("File input(s) found on page: " + fileInputs.size());
        if (fileInputs.isEmpty()) {
            ScreenshotUtility.captureScreenshot(driver, "MiraklProductImportsPage_noFileInput", ScreenshotUtility.FAIL);
            throw new org.openqa.selenium.NoSuchElementException(
                "No <input type='file'> found anywhere on the page after clicking Import Products. Current URL: "
                    + driver.getCurrentUrl());
        }
        fileInputs.get(0).sendKeys(filePath);

        // ROOT-CAUSED live (TC_E2E_004): a prior run's own failure screenshot — taken immediately
        // after this poll gave up — showed the file already correctly attached, meaning the render
        // finished a moment after the last poll attempt. 5s (10x500ms) wasn't consistently enough;
        // widened to 20s.
        boolean confirmed = false;
        for (int i = 0; i < 20; i++) {
            Object present = ((JavascriptExecutor) driver).executeScript(
                "return document.body.textContent.indexOf(arguments[0]) !== -1;", fileName);
            if (Boolean.TRUE.equals(present)) {
                confirmed = true;
                break;
            }
            sleep(1000);
        }
        if (!confirmed) {
            ScreenshotUtility.captureScreenshot(driver, "MiraklProductImportsPage_uploadFailed", ScreenshotUtility.FAIL);
            Object diag = ((JavascriptExecutor) driver).executeScript(
                "return {textLen: document.body.textContent.length, "
                + "innerTextLen: document.body.innerText.length, "
                + "iframeCount: document.querySelectorAll('iframe').length, "
                + "snippet: document.body.textContent.substring(0, 500)};");
            LoggerUtility.error("Upload confirmation diagnostic: " + diag);
            throw new IllegalStateException("Product file '" + fileName + "' never appeared as an attached-file "
                + "confirmation on the page after upload");
        }
        LoggerUtility.info("Product file attached and confirmed visible on page: " + fileName);
    }

    // ROOT-CAUSED live (TC_E2E_004): a real screenshot of this "Import products" form showed only
    // two sections — "Product file" (the drag-and-drop uploader) and "Import mode" (Standard
    // upload / ... radio buttons) — there is NO separate "File Content" selector at all on this
    // form. That makes sense: this endpoint IS specifically the product-only import (as opposed to
    // MiraklFileImportsPage's generic Offers/Products/Prices/Stocks uploader, which genuinely needs
    // a content-type selector). This is now a safe no-op so the test's step sequence doesn't need to
    // change, but it deliberately does NOT search for a bare "//select" anymore — that risked
    // matching and mutating some unrelated dropdown elsewhere on the page.
    public void selectProductContent() {
        LoggerUtility.info("No 'File Content' selector exists on the Import Products form (confirmed live — "
            + "this endpoint is product-only by definition) — nothing to select, proceeding to Confirm");
    }

    // Part 1, Step 4: "Click Confirm" — distinct button label from MiraklFileImportsPage's own
    // "Import" button, consistent with this being a genuinely separate upload form/flow.
    public void clickConfirmProductImport() {
        LoggerUtility.info("Clicking Confirm on Import Products form");
        WaitUtility.fluentWaitForClickable(driver, CONFIRM_PRODUCT_IMPORT_BUTTON);
        jsClick(CONFIRM_PRODUCT_IMPORT_BUTTON);
    }

    public void navigateToProductImports() {
        LoggerUtility.info("Navigating to Catalog > Product Imports");
        // Wait for the sidebar to be genuinely interactive first — same fix already applied to
        // MiraklOfferPage.navigateToOffersSection() and MiraklFileImportsPage.navigateToFileImports().
        WaitUtility.fluentWaitForClickable(driver, CATALOG_MENU);
        if (!isSubmenuVisible()) {
            jsClick(CATALOG_MENU);
            try {
                WaitUtility.fluentWait(driver, PRODUCT_IMPORTS_SUBMENU);
            } catch (org.openqa.selenium.TimeoutException e) {
                // ROOT-CAUSED live (TC_E2E_003): confirmed live the accordion click can silently not
                // register the very first time when called right after a hard
                // driver.navigate().refresh() (TC_E2E_003's Step 30) — the same SPA-not-finished-
                // hydrating class of issue already documented for this exact accordion pattern in
                // MiraklOfferPage.navigateToOffersSection() ("the SPA may not have finished hydrating
                // its click handlers yet"). fluentWaitForClickable() above only proves the element is
                // clickable per Selenium's own criteria, not that React has finished wiring its handler.
                // Retry the click once rather than failing outright on what is very likely a one-off
                // timing miss, not a genuinely broken/missing menu.
                LoggerUtility.warn("Product Imports submenu did not appear within the wait after clicking "
                    + "the Catalog accordion — retrying the click once");
                jsClick(CATALOG_MENU);
                try {
                    WaitUtility.fluentWait(driver, PRODUCT_IMPORTS_SUBMENU);
                } catch (org.openqa.selenium.TimeoutException e2) {
                    // Diagnostic dump of every short <span> actually rendered on the page — real
                    // evidence of the current role's true sidebar wording, so a third blind guess at
                    // the label text isn't needed if this still fails.
                    Object visibleSpans = ((JavascriptExecutor) driver).executeScript(
                        "var out = [];"
                        + "document.querySelectorAll('span').forEach(function(el) {"
                        + "  var t = el.textContent.trim();"
                        + "  if (t && t.length > 0 && t.length < 40) out.push(t);"
                        + "});"
                        + "return out.join(' | ');");
                    LoggerUtility.error("Product Imports submenu never appeared after 2 attempts. All "
                        + "short <span> text currently on the page (diagnostic): " + visibleSpans);
                    throw e2;
                }
            }
        }
        jsClick(PRODUCT_IMPORTS_SUBMENU);
        LoggerUtility.info("Product Imports section opened");

        // CONFIRMED live (2026-09-09, Operator): this label leads to a landing/hub page, not a data
        // grid directly — one more click through "Shop product imports" reaches the real per-product
        // New/Pending/Published status grid.
        // CONFIRMED live (TC_E2E_004, Seller, 2026-09-17): the Seller-visible variant does NOT show
        // this hub card at all — it lands directly on the page with the "Import Products" button at
        // the right side. The original check here, `driver.findElements(...).isEmpty()`, was NOT
        // actually the "instant, non-blocking" check its own comment claimed: findElements() still
        // polls for the full implicit wait (2 minutes) before returning empty when nothing matches,
        // which is exactly the silent 2-minute stall observed on this exact line in two separate live
        // runs. Replaced with a real instant JS presence check (same pattern as isSubmenuVisible()).
        boolean hubCardPresent = isHubCardPresent();
        if (hubCardPresent) {
            WaitUtility.fluentWait(driver, SHOP_PRODUCT_IMPORTS_CARD);
            jsClick(SHOP_PRODUCT_IMPORTS_CARD);
            LoggerUtility.info("Shop product imports grid opened");
        } else {
            LoggerUtility.info("No 'Shop product imports' hub card present — assuming this role/label "
                + "variant landed directly on the per-product status grid");
        }
    }

    private boolean isSubmenuVisible() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(\"//span[normalize-space()='Catalog imports']\", document, null, " +
            "XPathResult.BOOLEAN_TYPE, null).booleanValue;");
        return Boolean.TRUE.equals(result);
    }

    // Instant, non-blocking presence check (JS querySelector, no findElement/implicit wait involved)
    // — see the CONFIRMED-live comment above this method's call site in navigateToProductImports().
    private boolean isHubCardPresent() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.querySelector(\"[data-testid='hub-card-SELLER_PRODUCT_IMPORTS']\") !== null;");
        return Boolean.TRUE.equals(result);
    }

    public void filterBySeller(String sellerName) {
        LoggerUtility.info("Filtering by Provider: " + sellerName);
        WaitUtility.fluentWaitForClickable(driver, PROVIDER_FILTER_BUTTON);
        jsClick(PROVIDER_FILTER_BUTTON);
        WaitUtility.fluentWait(driver, PROVIDER_SEARCH_INPUT);
        type(PROVIDER_SEARCH_INPUT, sellerName);
        sleep(1500);

        List<WebElement> suggestions = driver.findElements(By.xpath(
            "//div[contains(@class,'mui-suggestion-item')][contains(.,'" + sellerName + "')]"));
        if (suggestions.isEmpty()) {
            throw new org.openqa.selenium.NoSuchElementException(
                "Provider filter: no suggestion item found containing '" + sellerName + "'");
        }
        WebElement target = suggestions.get(0);
        LoggerUtility.info("Provider suggestion found: '" + target.getText().trim() + "'");
        try {
            target.click();
        } catch (Exception e) {
            LoggerUtility.warn("Provider suggestion real click failed (" + e.getMessage() + ") — falling back to JS click");
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", target);
        }
        sleep(500);

        // Close the filter panel so the selection is committed and the grid re-filters — reusing the
        // same toggle button (clicking it again collapses the panel).
        jsClick(PROVIDER_FILTER_BUTTON);
        sleep(1500);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // CONFIRMED live (2026-09-08 run): isDisplayed()/findElement() blocks for the FULL 2-minute
    // implicit wait every time the row is genuinely absent (product not there yet) — turning each
    // failed poll attempt into ~2.5 minutes instead of the intended ~30s, burning the whole 10-attempt
    // budget in ~25 minutes. Use an instant JS DOM check instead (same pattern already proven for
    // isSubmenuVisible() above) — it never engages Selenium's implicit wait.
    public boolean hasProductWithStatus(String productIdentifier, String status) {
        String xpath = "//tr[contains(.,'" + productIdentifier + "') and contains(.,'" + status + "')]";
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(arguments[0], document, null, XPathResult.BOOLEAN_TYPE, null).booleanValue;",
            xpath);
        boolean present = Boolean.TRUE.equals(result);
        LoggerUtility.info("Product " + productIdentifier + " with status '" + status + "' present: " + present);
        return present;
    }

    public void selectAllNewStatusProducts() {
        LoggerUtility.info("Selecting all products with status New");
        for (WebElement el : driver.findElements(NEW_STATUS_ROW_CHECKBOXES)) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

    public void clickMoreActions() {
        LoggerUtility.info("Clicking More Actions");
        jsClick(MORE_ACTIONS_BUTTON);
    }

    public void clickEditCatalogs() {
        LoggerUtility.info("Clicking Edit Catalogs");
        WaitUtility.fluentWait(driver, EDIT_CATALOGS_OPTION);
        jsClick(EDIT_CATALOGS_OPTION);
    }

    public void selectFdaCatalog() {
        LoggerUtility.info("Selecting FDA Catalog via 'Add to catalogs' combobox");

        // Instant JS presence check — avoids the confirmed-live 2-minute implicit-wait stall on a
        // failed findElement() when the dialog hasn't finished rendering yet.
        boolean triggerPresent = false;
        for (int i = 0; i < 10; i++) {
            Object found = ((JavascriptExecutor) driver).executeScript(
                "return document.evaluate(\"//*[@id='catalogsToAdd__trigger']\", document, null, "
                + "XPathResult.BOOLEAN_TYPE, null).booleanValue;");
            if (Boolean.TRUE.equals(found)) {
                triggerPresent = true;
                break;
            }
            sleep(500);
        }
        if (!triggerPresent) {
            throw new org.openqa.selenium.NoSuchElementException(
                "'Add to catalogs' combobox trigger (#catalogsToAdd__trigger) never appeared in Edit catalogs dialog");
        }

        List<WebElement> triggers = driver.findElements(CATALOGS_TO_ADD_TRIGGER);
        WebElement trigger = triggers.get(0);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", trigger);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", trigger);
        sleep(1000);

        // Same "click trigger -> options render in a listbox" pattern already proven live for the
        // Fragile/MSI mui-suggestion-item widgets — try both the standard role="option" markup and
        // the mui-suggestion-item class this platform reuses elsewhere.
        List<WebElement> options = driver.findElements(By.xpath(
            "//*[@role='listbox' or @role='presentation']//*[@role='option'] | "
            + "//div[contains(@class,'mui-suggestion-item')]"));
        if (options.isEmpty()) {
            // Some renders need a keystroke into the trigger's own search input to populate the menu.
            List<WebElement> searchInputs = driver.findElements(By.xpath(
                "//*[@id='catalogsToAdd__trigger']//input | //*[@id='catalogsToAdd__trigger']/following::input[1]"));
            if (!searchInputs.isEmpty()) {
                searchInputs.get(0).sendKeys("FDA");
                sleep(1000);
                options = driver.findElements(By.xpath(
                    "//*[@role='listbox' or @role='presentation']//*[@role='option'] | "
                    + "//div[contains(@class,'mui-suggestion-item')]"));
            }
        }
        if (options.isEmpty()) {
            throw new org.openqa.selenium.NoSuchElementException(
                "'Add to catalogs' combobox opened but no option/mui-suggestion-item elements found");
        }

        LoggerUtility.info("Add to catalogs options found: " + options.size());
        for (WebElement o : options) {
            LoggerUtility.info("  option text='" + o.getText().trim() + "'");
        }

        WebElement target = options.get(0);
        for (WebElement o : options) {
            if (o.getText().trim().toUpperCase().contains("FDA")) {
                target = o;
                break;
            }
        }
        LoggerUtility.info("Selecting catalog option: '" + target.getText().trim() + "'");
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", target);
        sleep(500);
    }

    public void confirmCatalogSelection() {
        LoggerUtility.info("Confirming catalog selection");
        jsClick(CATALOG_POPUP_CONFIRM_BUTTON);
        // CONFIRMED live (2026-09-10): the "Edit catalogs" confirmation is an ASYNC backend update
        // ("Your products are being updated with the new catalogs edits."). Clicking Accept
        // immediately afterward intermittently raced this update — one run's second product never
        // showed the Accept Products popup at all. A short settle delay before Accept is cheap
        // insurance against that race.
        sleep(2000);
    }

    // Added for TC_E2E_004 — manual test case step 28: "Verify the 'Accept' button is clickable"
    // before actually clicking it. WaitUtility.fluentWaitForClickable() wraps
    // ExpectedConditions.elementToBeClickable(), so returning normally here IS the clickability
    // verification (it throws TimeoutException if the button never becomes clickable).
    public boolean isAcceptButtonClickable() {
        try {
            WaitUtility.fluentWaitForClickable(driver, ACCEPT_BUTTON);
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            LoggerUtility.warn("Accept button never became clickable: " + e.getMessage());
            return false;
        }
    }

    public void clickAccept() {
        LoggerUtility.info("Clicking Accept");
        WaitUtility.fluentWaitForClickable(driver, ACCEPT_BUTTON);
        jsClick(ACCEPT_BUTTON);
    }

    // CONFIRMED live (2026-09-10): isDisplayed() -> driver.findElement() honors the global 2-minute
    // implicit wait — when the popup genuinely doesn't appear (the race condition above), this
    // burned the full 2 minutes before failing. Short bounded JS poll instead: fast when the popup
    // is there (as it normally is), tolerant of a brief render delay, never a multi-minute stall.
    public boolean isAcceptProductsPopupDisplayed() {
        boolean displayed = false;
        for (int i = 0; i < 6; i++) {
            Object found = ((JavascriptExecutor) driver).executeScript(
                "return document.evaluate(\"//div[@role='dialog'][contains(.,'Accept Products') or contains(.,'Accept products')]\", "
                + "document, null, XPathResult.BOOLEAN_TYPE, null).booleanValue;");
            if (Boolean.TRUE.equals(found)) {
                displayed = true;
                break;
            }
            sleep(500);
        }
        LoggerUtility.info("Accept Products popup displayed: " + displayed);
        return displayed;
    }

    public void confirmAcceptProducts() {
        LoggerUtility.info("Confirming Accept Products");
        jsClick(ACCEPT_POPUP_CONFIRM_BUTTON);
        // ROOT-CAUSED live (TC_E2E_004, 2026-09-17): a screenshot taken immediately after this click
        // (before this fix) showed the "Accept products" dialog still fully open with its own Confirm
        // button visible — either the click hadn't been processed yet or the modal's close transition
        // hadn't finished, and the subsequent confirmation-banner check then found nothing (no bug in
        // that check itself, the flow just hadn't progressed far enough yet). Bounded instant-JS poll
        // for the dialog to actually disappear before returning, so callers can trust the Accept has
        // genuinely gone through by the time this method returns.
        for (int i = 0; i < 10 && isAcceptDialogPresent(); i++) {
            sleep(500);
        }
        if (isAcceptDialogPresent()) {
            LoggerUtility.warn("'Accept products' dialog still present 5s after clicking Confirm — retrying the click once");
            jsClick(ACCEPT_POPUP_CONFIRM_BUTTON);
            for (int i = 0; i < 10 && isAcceptDialogPresent(); i++) {
                sleep(500);
            }
        }
    }

    private boolean isAcceptDialogPresent() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(\"//div[@role='dialog'][contains(.,'Accept Products') or "
            + "contains(.,'Accept products')]\", document, null, XPathResult.BOOLEAN_TYPE, null).booleanValue;");
        return Boolean.TRUE.equals(result);
    }

    // Added for TC_E2E_003's exact-message verification requirement — same broad "any non-danger
    // notification/alert/toast" pattern already proven live for MiraklOfferPage.getSuccessMessage()
    // (that banner's exact copy differs per action, so the same detection strategy is reused rather
    // than a new locator family). Bounded JS poll (not isDisplayed()/findElement()) so a banner that
    // never appears fails fast instead of stalling on the 2-minute implicit wait.
    // ROOT-CAUSED live (TC_E2E_004, 2026-09-17, third occurrence): a real screenshot at the exact
    // moment this check ran proved the banner WAS on screen (dark navy toast, exact text "Your changes
    // were successful and will be processed as soon as possible.") but its element never matched any of
    // 'notification'/'alert'/'toast' in @class — this Accept flow's toast uses different CSS entirely.
    // Broadened to ALSO match the leaf-most element containing the known message text (the "not(.//...)"
    // clause picks the innermost matching node, avoiding the earlier leaf-vs-ancestor over-matching bug
    // seen in MiraklCatalogManagementPage's badge counting). Defined as one shared xpath STRING (not
    // just a By) so the instant-check and the actual extraction below can never drift apart again —
    // that mismatch was exactly what caused the prior 2-minute implicit-wait stall.
    private static final String CONFIRMATION_BANNER_XPATH =
        "//*[(contains(@class,'notification') or contains(@class,'alert') or contains(@class,'toast')) "
        + "and not(contains(@class,'danger'))] | "
        + "//*[contains(normalize-space(),'successful') and not(.//*[contains(normalize-space(),'successful')])]";
    private static final By CONFIRMATION_BANNER = By.xpath(CONFIRMATION_BANNER_XPATH);

    public String getAcceptConfirmationMessage() {
        // ROOT-CAUSED live (TC_E2E_004, 2026-09-17): driver.findElements(...).isEmpty() still pays the
        // full 2-minute implicit wait per failed attempt when the banner is genuinely absent — with 10
        // attempts that's up to a ~20-minute stall (confirmed live: stuck at this exact step for 17+
        // minutes straight) before even reaching the "no banner" fallback. Same class of bug as the
        // hub-card/badge-count/filter-option issues elsewhere in this codebase. Instant JS presence
        // check instead — no findElement/implicit wait involved.
        boolean appeared = false;
        for (int i = 0; i < 10; i++) {
            if (isConfirmationBannerPresent()) {
                appeared = true;
                break;
            }
            sleep(500);
        }
        if (!appeared) {
            LoggerUtility.warn("No confirmation banner appeared after Accept Products was confirmed");
            return "";
        }
        String message = driver.findElement(CONFIRMATION_BANNER).getText().replace("×", "").trim();
        LoggerUtility.info("Accept Products confirmation message: " + message);
        return message;
    }

    // ROOT-CAUSED live (TC_E2E_004, 2026-09-17, second occurrence): this originally checked a broader/
    // different selector than CONFIRMATION_BANNER itself (adding role='alert' and '.banner', missing
    // the "not 'danger'" exclusion) — it could return true for an unrelated element while
    // CONFIRMATION_BANNER's own xpath matched nothing, so the subsequent driver.findElement(
    // CONFIRMATION_BANNER) call still blocked the full 2-minute implicit wait before throwing
    // (confirmed live: exactly a 2-minute gap, not the expected ~5s). Must check the EXACT SAME xpath
    // as CONFIRMATION_BANNER, not a broader approximation of it.
    private boolean isConfirmationBannerPresent() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(arguments[0], document, null, "
            + "XPathResult.BOOLEAN_TYPE, null).booleanValue;",
            CONFIRMATION_BANNER_XPATH);
        return Boolean.TRUE.equals(result);
    }
}
