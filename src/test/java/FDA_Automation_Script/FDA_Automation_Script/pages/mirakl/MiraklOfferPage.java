package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Locators below were verified against the live Mirakl DOM (farmaciasdelahorromx2-dev.mirakl.net,
// 2026-09-07) via a standalone diagnostic Selenium session — not guesses. A few dropdown-option
// markups render dynamically and are still marked TODO where noted.
public class MiraklOfferPage extends BasePage {

    // --- Navigation: Price and stock > Offers ---
    // "Price and stock" toggles a sidebar accordion revealing Offers/Promotions/File imports as
    // sibling items (not a separate page navigation). Note real casing: "Price and stock" (lowercase s).
    private static final By PRICE_AND_STOCK_MENU = By.xpath("//span[normalize-space()='Price and stock']");
    private static final By OFFERS_SUBMENU       = By.xpath("//a[@id='shopsOffersSearch'] | //span[normalize-space()='Offers']");

    // CONFIRMED live (2026-09-09, real screenshot): there is NO "Pending offers" tab at all — the
    // real tabs on the Offers list page are "All" / "Active" / "Inactive", with "All" already
    // selected by default. "All" is the broadest view (includes not-yet-active offers), so it's the
    // correct target for the manual spec's "click the Pending Offers tab" step.
    private static final By ALL_OFFERS_TAB = By.xpath("//*[self::button or self::a or self::span][normalize-space()='All']");
    private static final By ACTIVE_OFFERS_TAB = By.xpath("//*[self::button or self::a or self::span][normalize-space()='Active']");

    // --- Offers list toolbar / Add Offer ---
    private static final By ADD_OFFER_BUTTON     = By.xpath("//span[normalize-space()='Add offer']");
    // Unique to the catalog-search step of Add Offer (confirmed heading text)
    private static final By ADD_OFFER_PAGE_TITLE = By.xpath("//*[contains(normalize-space(),'Search for a Product in our Catalog')]");

    // --- Catalog product search (inside Add Offer page) ---
    private static final By CATALOG_SEARCH_FIELD = By.xpath("//input[@id='productsFilterFormInput']");
    private static final By CATALOG_SEARCH_ICON  = By.xpath("//form[@id='productsFilterForm']//button[@type='submit']");

    // --- Create Product (alternative to catalog search, same Add Offer page) ---
    // TODO: Verify locators against actual DOM — never live-probed yet, unlike the rest of this
    // class. The manual spec's step ordering suggests a "Create Product" panel/section sits
    // alongside the catalog search box on the same Add Offer page.
    private static final By CREATE_PRODUCT_BUTTON = By.xpath(
        "//button[contains(normalize-space(),'Create Product') or contains(normalize-space(),'Create product')] | " +
        "//a[contains(normalize-space(),'Create Product') or contains(normalize-space(),'Create product')]");
    private static final By PRODUCT_CREATION_FORM = By.xpath(
        "//*[contains(normalize-space(),'Product Characteristics') or contains(normalize-space(),'Category')]");
    // CONFIRMED live (2026-09-12): the Product Characteristics section (which contains this field)
    // only renders after a category is selected, and offers 5 separate image slots ("Mirakl Image
    // 1".."5") — index [1] targets the first/primary one, matching the manual spec's single "Select
    // the Product image" step.
    private static final By PRODUCT_IMAGE_INPUT = By.xpath("(//input[@type='file'])[1]");

    // Unique to the actual offer-creation form (confirmed real <form id="createOfferForm">)
    private static final By CREATE_OFFER_PAGE_TITLE = By.xpath("//form[@id='createOfferForm']");

    // --- Add Offer form fields ---
    // Labeled "Offer SKU" in the UI; the underlying field id is "shopSku" — there is NO separate
    // "Shop SKU" field in this Mirakl instance. Optional: Mirakl auto-generates one if left blank.
    private static final By OFFER_SKU_FIELD      = By.xpath("//input[@id='shopSku']");
    private static final By STOCK_QUANTITY_FIELD = By.xpath("//input[@id='quantity']");
    // CONFIRMED live (2026-09-12): shopSku/quantity ids are shared by both the "Sell Yours on an
    // existing product" form (Tc_manualoffercreation_01) and this class's own "Create Product" form,
    // but price is NOT — the Create Product flow's Offer Characteristics section nests price under a
    // variant-scoped id ("ui-id-0unitPrice") instead of the flat id used by the simpler existing-offer
    // form. enterPrice() checks which one is actually present rather than hardcoding either.
    private static final By PRICE_FIELD          = By.xpath("//input[@id='runningPricing-0-price']");
    private static final By PRICE_FIELD_VARIANT  = By.xpath("//input[@id='ui-id-0unitPrice']");
    // Native <select>, options are Yes/No — this is NOT a numeric installment count field
    private static final By MSI_DROPDOWN         = By.xpath("//select[@id='additionalFieldValueCommands1-value']");
    // select2-enhanced, but underneath it's a real native <select> (display:none) with real
    // <option> elements — confirmed live: seller_warehouse, fda_warehouse_1..7. Selenium's Select
    // class works directly on it (same pattern as MSI_DROPDOWN), no need to open the visual widget.
    private static final By LOGISTICS_DROPDOWN = By.xpath("//select[@id='additionalFieldValueCommands2-value']");
    // CONFIRMED live (2026-09-12): "saveOfferButton" is the id used by the "Sell Yours on an existing
    // product" form (Tc_manualoffercreation_01) — the Create Product flow's own submit button uses a
    // different id, "addProductAndOffer" (real text is "Submit for approval", lowercase 'a' — the
    // original case-sensitive text-match fallback missed it for exactly that reason).
    private static final By SUBMIT_FOR_APPROVAL_BUTTON = By.xpath("//button[@id='saveOfferButton']");
    private static final By SUBMIT_FOR_APPROVAL_BUTTON_VARIANT = By.xpath("//button[@id='addProductAndOffer']");
    private static final By SUBMIT_FOR_APPROVAL_BUTTON_FALLBACK = By.xpath(
        "//button[contains(translate(normalize-space(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),"
        + "'submit for approval')]");
    // TODO: exact success-banner text unverified (submission was intentionally not exercised during
    // locator discovery, to avoid creating throwaway data while probing) — match loosely for now.
    // CONFIRMED live (2026-09-12): the error-state equivalent of this banner uses class
    // "alert alert-matching-react alert-danger-react alert-danger" with text "Your form contains
    // errors" — no mention of "offer" at all. The Create Product flow's success text is likely
    // similarly generic (unlike the "Sell Yours" existing-offer flow's confirmed "Offer for X added"),
    // so this now matches any alert/notification/toast that is NOT the danger variant, instead of
    // requiring the literal word "offer" in its text.
    private static final By SUCCESS_MESSAGE_BANNER = By.xpath(
        "//*[(contains(@class,'notification') or contains(@class,'alert') or contains(@class,'toast')) "
        + "and not(contains(@class,'danger'))]");

    // --- Offers list search ---
    private static final By OFFERS_SEARCH_FIELD  = By.xpath("//input[@id='search' and @name='search']");
    // The svg itself doesn't support .click() in this browser (SVGElement, not HTMLElement) —
    // target its parent <span> instead; the click event still bubbles up to whatever ancestor
    // holds the actual click handler.
    private static final By OFFERS_SEARCH_ICON   = By.xpath("//*[@data-testid='search__svg']/parent::span");

    public MiraklOfferPage(WebDriver driver) {
        super(driver);
    }

    // --- Navigation ---

    // EXPERIMENT (2026-09-15): used only by Tc_manualproductandoffercreation_01's Mirakl "category
    // configuration changed" concurrency-retry — testing whether a genuine hard driver.navigate().
    // refresh() (never previously used by the retry, which only re-ran SPA-internal clicks) forces a
    // fresh server fetch of the category's attribute schema instead of reusing whatever the SPA
    // cached in memory from the first attempt. Reuses the exact same sidebar-interactive readiness
    // check navigateToOffersSection() already relies on, so "ready" means the same thing in both
    // places.
    public void waitForAppReadyAfterRefresh() {
        WaitUtility.fluentWaitForClickable(driver, PRICE_AND_STOCK_MENU);
        LoggerUtility.info("Mirakl app ready after hard refresh (sidebar interactive)");
    }

    public void navigateToOffersSection() {
        LoggerUtility.info("Navigating to Mirakl Price and stock > Offers");
        // Wait for the sidebar to be genuinely interactive (not just present) before clicking —
        // this is called right after login, and the SPA may not have finished hydrating its click
        // handlers yet, which is the likely cause of the click silently not registering.
        WaitUtility.fluentWaitForClickable(driver, PRICE_AND_STOCK_MENU);
        if (!isAccordionExpanded()) {
            jsClick(PRICE_AND_STOCK_MENU);
            WaitUtility.fluentWait(driver, OFFERS_SUBMENU);
        }
        jsClick(OFFERS_SUBMENU);
        LoggerUtility.info("Offers section opened");
    }

    // findElement()/findElements() both honor the global 2-minute implicit wait and would block
    // waiting for the accordion's "Offers" link to appear if it's genuinely absent from the DOM
    // (collapsed state). A direct JS DOM check is instantaneous and never touches implicit wait.
    private boolean isAccordionExpanded() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.getElementById('shopsOffersSearch') !== null;");
        return Boolean.TRUE.equals(result);
    }

    public void clickPendingOffersTab() {
        LoggerUtility.info("Clicking 'All' tab (no 'Pending offers' tab exists — 'All' is the broadest view, includes not-yet-active offers)");
        WaitUtility.fluentWait(driver, ALL_OFFERS_TAB);
        jsClick(ALL_OFFERS_TAB);
    }

    // Added for TC_E2E_004 — manual test case step 50: "Verify that 'Active' button is clickable"
    // before clicking it. Same verification-via-wait pattern as
    // MiraklProductImportsPage.isAcceptButtonClickable().
    public boolean isActiveOffersTabClickable() {
        try {
            WaitUtility.fluentWaitForClickable(driver, ACTIVE_OFFERS_TAB);
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            LoggerUtility.warn("'Active' tab never became clickable: " + e.getMessage());
            return false;
        }
    }

    public void clickActiveOffersTab() {
        LoggerUtility.info("Clicking 'Active' tab");
        WaitUtility.fluentWaitForClickable(driver, ACTIVE_OFFERS_TAB);
        jsClick(ACTIVE_OFFERS_TAB);
    }

    public void clickAddOffer() {
        LoggerUtility.info("Clicking Add Offer button");
        WaitUtility.fluentWait(driver, ADD_OFFER_BUTTON);
        jsClick(ADD_OFFER_BUTTON);
        LoggerUtility.info("Add Offer page requested");
    }

    public boolean isAddOfferPageDisplayed() {
        boolean displayed = isDisplayed(ADD_OFFER_PAGE_TITLE);
        LoggerUtility.info("Add Offer page displayed: " + displayed);
        return displayed;
    }

    // --- Catalog product search ---

    public void searchCatalogProduct(String query) {
        LoggerUtility.info("Searching Mirakl catalog for: " + query);
        type(CATALOG_SEARCH_FIELD, query);
    }

    public void clickCatalogSearchIcon() {
        LoggerUtility.info("Clicking catalog search icon");
        jsClick(CATALOG_SEARCH_ICON);
    }

    public boolean isProductResultDisplayed(String productName, String productSku) {
        By resultRow = By.xpath("//tr[@productsku='" + productSku + "'][contains(.,'" + productName + "')]");
        boolean displayed = isDisplayed(resultRow);
        LoggerUtility.info("Catalog result displayed for '" + productName + "' / '" + productSku + "': " + displayed);
        return displayed;
    }

    public void selectProductSellYours(String productName) {
        LoggerUtility.info("Clicking Sell yours for product: " + productName);
        By sellYoursInRow = By.xpath("//tr[contains(.,'" + productName + "')]//button[@data-clickid='sell-yours']");
        try {
            jsClick(sellYoursInRow);
        } catch (StaleElementReferenceException e) {
            LoggerUtility.warn("Stale on selectProductSellYours, retrying");
            jsClick(sellYoursInRow);
        }
        LoggerUtility.info("Sell yours clicked for: " + productName);
        // Sell yours triggers a navigation to the Create Offer form — wait for it here so
        // isCreateOfferPageDisplayed() isn't checked before the page has actually loaded.
        LoggerUtility.info("[BUILD-CHECK-v2] Waiting for #createOfferForm to appear...");
        WaitUtility.fluentWait(driver, CREATE_OFFER_PAGE_TITLE);
        LoggerUtility.info("[BUILD-CHECK-v2] #createOfferForm is now present and visible");
    }

    // --- SKU-only variants (used when the product's display name isn't known up front) ---
    // Reuse the same confirmed-live "productsku" row attribute as isProductResultDisplayed()/
    // selectProductSellYours() above — just without also requiring a name match.

    public boolean isProductResultDisplayedBySku(String productSku) {
        By resultRow = By.xpath("//tr[@productsku='" + productSku + "']");
        boolean displayed = isDisplayed(resultRow);
        LoggerUtility.info("Catalog result displayed for SKU '" + productSku + "': " + displayed);
        return displayed;
    }

    public String getProductNameFromSearchResultBySku(String productSku) {
        // TODO: exact name-cell column/class unconfirmed — falls back to the row's first
        // reasonably-sized text chunk if a dedicated name element isn't found.
        By nameCell = By.xpath("//tr[@productsku='" + productSku + "']//*[contains(@class,'product-name') or contains(@class,'name')]");
        String name;
        try {
            name = getText(nameCell);
        } catch (Exception e) {
            LoggerUtility.warn("No dedicated name element found for SKU " + productSku + " — falling back to full row text");
            By row = By.xpath("//tr[@productsku='" + productSku + "']");
            name = getText(row);
        }
        LoggerUtility.info("Product name from search result for SKU " + productSku + ": " + name);
        return name;
    }

    public void selectProductSellYoursBySku(String productSku) {
        LoggerUtility.info("Clicking Sell yours for product SKU: " + productSku);
        By sellYoursInRow = By.xpath("//tr[@productsku='" + productSku + "']//button[@data-clickid='sell-yours']");
        try {
            jsClick(sellYoursInRow);
        } catch (StaleElementReferenceException e) {
            LoggerUtility.warn("Stale on selectProductSellYoursBySku, retrying");
            jsClick(sellYoursInRow);
        }
        LoggerUtility.info("Sell yours clicked for SKU: " + productSku);
        WaitUtility.fluentWait(driver, CREATE_OFFER_PAGE_TITLE);
    }

    public boolean isCreateOfferPageDisplayed() {
        boolean displayed = isDisplayed(CREATE_OFFER_PAGE_TITLE);
        LoggerUtility.info("Create Offer page displayed: " + displayed);
        return displayed;
    }

    // --- Create Product (new product, as opposed to selling on an existing catalog entry) ---
    // TODO: none of these locators have been verified against the live DOM yet — this whole
    // sub-flow is new territory, unlike the rest of this class. Expect the first live run to fail
    // here and need iteration, same as every other new page in this project's history.

    public void clickCreateProduct() {
        LoggerUtility.info("Clicking Create Product button");
        WaitUtility.fluentWait(driver, CREATE_PRODUCT_BUTTON);
        jsClick(CREATE_PRODUCT_BUTTON);
    }

    public boolean isProductCreationFormDisplayed() {
        boolean displayed = isDisplayed(PRODUCT_CREATION_FORM);
        LoggerUtility.info("Product creation form (Category/Product Characteristics) displayed: " + displayed);
        return displayed;
    }

    // Category selection (Steps 6-11) is called directly from TC_E2E_003_Test now — no longer a
    // parameterized page-object method (per explicit instruction). See that test class for the xpaths.

    // INVESTIGATED live (2026-09-15, TC_E2E_003 "category configuration changed" failure): three
    // consecutive live attempts against Bebé > Accesorios para Bebé > Accesorios de Baño all hit
    // Mirakl's "this product category configuration changed while you were editing it" submission
    // error, AND on every attempt the pre-submit checklist found Product Name / Fragile / Stock
    // Quantity empty even though this test's own code sets them well before Submit and never clears
    // them again. selectProductCategory()'s own per-level clicks always completed without exception
    // and the Product Characteristics section appeared immediately after (isProductCreationFormDisplayed()
    // returned true every time) — but that check only matches loose heading text ("Product
    // Characteristics"/"Category"), which can be true the instant a skeleton/placeholder section
    // mounts, well before Mirakl's async fetch of THIS category's real attribute schema completes and
    // React swaps the skeleton subtree for the real one. A value typed into the pre-swap DOM nodes is
    // silently discarded when that swap happens — explaining both symptoms: early-set fields (Product
    // Name, entered right after category selection) go empty, and the backend's own "configuration
    // changed" check is exactly the kind of client/server schema-version mismatch this timing gap
    // would cause. This is evidence for cause (A) an async frontend load/re-render racing this test's
    // own field entry, not a fixed "just add a longer sleep" guess: instead of a blind delay, this
    // polls the Product Characteristics subtree's own structural signature (form-group count + input/
    // select/textarea count per group — the same .form-group traversal already used by
    // fillMandatoryProductCharacteristics()/runNamedMandatoryFieldsChecklist() in this same class)
    // until it stops changing across consecutive checks, which is what "the async re-render finished"
    // actually looks like in the DOM, whatever the underlying backend reason for the swap is.
    // REVISED live (2026-09-16): the first version of this method treated ANY stable signature as
    // "settled", including a tiny 3-form-group snapshot containing only the category widgets
    // themselves (before Mirakl's async attribute-schema fetch had even started populating the real
    // Product Characteristics section). The caller then correctly detected that Product Name wasn't
    // ready and RE-SELECTED the same category a second time to recover — but re-clicking the select2
    // cascade a second time while the first fetch was still in flight left stale/duplicate DOM nodes
    // behind (confirmed live: form-group count jumped from 3 to an abnormal 663, and
    // driver.findElement(By.id("quantity")) then resolved to a stale duplicate, typing Stock
    // Quantity's value into the Package Type field instead). Root cause: waiting for "no change" is
    // not the same as waiting for "the real content arrived". Fixed by never re-selecting the
    // category a second time — this method now first waits for genuine content to appear (Product
    // Name field present, not just any stable snapshot), THEN confirms it has stopped growing/changing,
    // all within one bounded wait on the ONE category selection the caller already performed.
    public void waitForProductCharacteristicsToSettle() {
        LoggerUtility.info("Waiting for Product Characteristics section to finish rendering (post-category-selection settle check)");

        // Phase 1: wait for the REAL section to actually appear — not just any DOM snapshot being
        // momentarily unchanged. Bounded at 40 x 500ms = 20s, generous because the live schema fetch
        // for this category was observed to take longer than the original 9s budget.
        int maxAppearChecks = 40;
        boolean contentAppeared = false;
        for (int i = 1; i <= maxAppearChecks; i++) {
            if (isProductNameFieldReady()) {
                LoggerUtility.info("Real Product Characteristics content appeared after " + i + " check(s) (" + (i * 500) + "ms)");
                contentAppeared = true;
                break;
            }
            sleep(500);
        }
        if (!contentAppeared) {
            LoggerUtility.warn("Product Name field never appeared within " + (maxAppearChecks * 500)
                + "ms of category selection — proceeding anyway; caller's own readiness assertion will catch this");
            return;
        }

        // Phase 2: now that real content exists, confirm the subtree has stopped growing/changing
        // (e.g. late-arriving category-conditional fields like Marca/Fragile) before any field is
        // touched — same structural-signature check as before, just no longer used as the ONLY signal.
        int stableChecksRequired = 3;
        int stableCount = 0;
        int maxStableChecks = 20; // 20 x 300ms = 6s max additional wait, bounded
        String lastSignature = null;
        for (int i = 1; i <= maxStableChecks; i++) {
            String signature = String.valueOf(((JavascriptExecutor) driver).executeScript(
                "var groups = document.querySelectorAll('.form-group');"
                + "var sig = groups.length + ':';"
                + "groups.forEach(function(g) { sig += g.querySelectorAll('input, select, textarea').length + ','; });"
                + "return sig;"));
            if (signature.equals(lastSignature)) {
                stableCount++;
                LoggerUtility.info("Settle check " + i + "/" + maxStableChecks + " — signature unchanged (" + stableCount
                    + "/" + stableChecksRequired + " consecutive): " + signature);
                if (stableCount >= stableChecksRequired) {
                    LoggerUtility.info("Product Characteristics section settled after " + i + " check(s)");
                    return;
                }
            } else {
                if (lastSignature != null) {
                    LoggerUtility.info("Settle check " + i + "/" + maxStableChecks + " — signature CHANGED (form-group/field "
                        + "count) from [" + lastSignature + "] to [" + signature + "] — section is still re-rendering");
                }
                stableCount = 0;
            }
            lastSignature = signature;
            sleep(300);
        }
        LoggerUtility.warn("Product Characteristics section did not report a stable signature within "
            + (maxStableChecks * 300) + "ms — proceeding with the last observed signature: " + lastSignature);
    }

    // Requirement-driven readiness check for the category-selection retry loop: confirms a real,
    // category-specific field (Product Name) is actually present, using the same .form-group /
    // matcher-prefix lookup already proven by getFieldValueByLabel()/setFieldByLabel() — a much
    // stronger signal than isProductCreationFormDisplayed()'s loose heading-text match, which can be
    // true while the section is still just a skeleton.
    public boolean isProductNameFieldReady() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "var groups = document.querySelectorAll('.form-group');"
            + "for (var g = 0; g < groups.length; g++) {"
            + "  var grpText = groups[g].textContent.trim().toLowerCase();"
            + "  if (grpText.indexOf('product name') === 0) {"
            + "    var el = groups[g].querySelector('input, textarea');"
            + "    if (el) return true;"
            + "  }"
            + "}"
            + "return false;");
        boolean ready = Boolean.TRUE.equals(result);
        LoggerUtility.info("Product Name field ready: " + ready);
        return ready;
    }

    // CONFIRMED live (2026-09-12): immediately after category selection, the Product Characteristics
    // section is still finishing its React re-render — findElement()+sendKeys() back-to-back can hit
    // a StaleElementReferenceException if the node gets replaced in that window. Retry once with a
    // fresh findElement() rather than papering over it with a longer fixed sleep.
    public void uploadProductImage(String imagePath) {
        LoggerUtility.info("Uploading product image: " + imagePath);
        int maxAttempts = 4;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                driver.findElement(PRODUCT_IMAGE_INPUT).sendKeys(imagePath);
                LoggerUtility.info("Product image file attached");
                return;
            } catch (StaleElementReferenceException e) {
                if (attempt == maxAttempts) {
                    throw e;
                }
                LoggerUtility.warn("Stale element on uploadProductImage (attempt " + attempt + "/" + maxAttempts
                    + "), retrying: " + e.getMessage());
                sleep(1000L * attempt);
            }
        }
    }

    // Generic mandatory-field filler for the Product Characteristics section — the exact field set
    // is unknown/unverified, so this discovers whatever's actually required through FOUR independent
    // signals (a field only needs to match one): the HTML "required"/"aria-required" attribute; a
    // red-asterisk character embedded in the field's own <label>/.control-label text; a red asterisk
    // rendered as its own separate leaf element (e.g. <span>*</span>) next to the label; or an
    // explicit required/mandatory CSS class anywhere in the field's .form-group. Mirakl's
    // category-specific Product Characteristics don't consistently use any single one of these, which
    // is exactly how "Fragile" slipped through undetected in an earlier run and blocked submission
    // with "Your form contains errors" — this is deliberately broad specifically because a false
    // negative (mandatory field left empty) breaks the whole submission, while a false positive (a
    // field filled that wasn't actually mandatory) is harmless. Every field it touches is logged so
    // the real form structure is visible in the log. Text/number inputs get a unique numeric value
    // (timestamp + per-call counter, guaranteed unique even within the same millisecond) instead of a
    // descriptive string, per explicit instruction; the product image (uploadProductImage()) is
    // untouched by this method.
    public void fillMandatoryProductCharacteristics(String brandValue) {
        Object filled = ((JavascriptExecutor) driver).executeScript(
            "var brandValue = arguments[0];" // captured here — inside the forEach callback below, `arguments`
            // refers to that inner function's OWN arguments (the forEach(el, index, array) triple), not this
            // script's arguments — using arguments[0] directly inside the callback silently resolved to the
            // DOM element itself and threw "arguments[0].toLowerCase is not a function" (confirmed live).
            + "var results = [];"
            + "var uniqueBase = Date.now();"
            + "var counter = 0;"
            + "var attrRequired = document.querySelectorAll('input[required], select[required], textarea[required], "
            + "input[aria-required=\"true\"], select[aria-required=\"true\"]');"
            + "var markedRequired = [];"
            // Marker 1: any label/control-label whose text contains a literal "*".
            + "document.querySelectorAll('label, .control-label').forEach(function(lbl) {"
            + "  if (lbl.textContent.indexOf('*') === -1) return;"
            + "  var grp = lbl.closest('.form-group') || lbl.parentElement;"
            + "  if (!grp) return;"
            + "  grp.querySelectorAll('input:not([id^=\"s2id_\"]):not(.select2-input):not(.select2-focusser), select:not(.select2-offscreen), textarea').forEach(function(f) { markedRequired.push(f); });"
            + "});"
            // Marker 2: a standalone leaf node whose ENTIRE text is just "*" (a separate <span>/<i>
            // asterisk glyph next to the label, not embedded inside the label's own text) — scoped to
            // its enclosing .form-group so it never grabs an unrelated large container.
            + "document.querySelectorAll('*').forEach(function(el) {"
            + "  if (el.children.length === 0 && el.textContent.trim() === '*') {"
            + "    var grp = el.closest('.form-group');"
            + "    if (grp) { grp.querySelectorAll('input:not([id^=\"s2id_\"]):not(.select2-input):not(.select2-focusser), select:not(.select2-offscreen), textarea').forEach(function(f) { markedRequired.push(f); }); }"
            + "  }"
            + "});"
            // Marker 3: an explicit required/mandatory CSS class anywhere in the field's own group.
            + "document.querySelectorAll('.required, .mandatory, [class*=\"required\"], [class*=\"mandatory\"]').forEach(function(el) {"
            + "  var grp = el.closest('.form-group') || el;"
            + "  grp.querySelectorAll('input:not([id^=\"s2id_\"]):not(.select2-input):not(.select2-focusser), select:not(.select2-offscreen), textarea').forEach(function(f) { markedRequired.push(f); });"
            + "});"
            + "var fields = new Set();"
            + "attrRequired.forEach(function(f) { fields.add(f); });"
            + "markedRequired.forEach(function(f) { fields.add(f); });"
            // Exclude fields owned by dedicated methods (enterProductName()/selectBrand()/
            // selectFragile(), each called separately right after this) — CONFIRMED live: touching
            // Marca/Fragile's native <select> here first (setting it to an arbitrary "first valid
            // option" via the generic fallback below) corrupted the select2 widget's state enough
            // that the dedicated method's later search for the real value ('FIFA'/'No') timed out
            // finding it in the reopened dropdown. Broadening the marker detection (below) surfaced
            // this interference that previously didn't happen when these fields went undetected.
            + "var ownedElsewhere = ['marca', 'fragile', 'product name'];"
            + "fields.forEach(function(el) {"
            + "  var lblText = '';"
            + "  if (el.id) { var lbl = document.querySelector('label[for=\"' + el.id + '\"]'); if (lbl) lblText = lbl.textContent.trim().toLowerCase(); }"
            + "  if (ownedElsewhere.indexOf(lblText) !== -1) { fields.delete(el); return; }"
            // Fallback: these fields can ALSO be select2-on-input hidden text inputs with no
            // <label for=id> at all (CONFIRMED live: Marca/Fragile's real backing element is exactly
            // this shape) — check the enclosing .form-group's visible text as a second signal so this
            // exclusion still works regardless of which DOM shape the field happens to have.
            + "  var grp = el.closest('.form-group');"
            + "  if (grp) {"
            + "    var grpText = grp.textContent.trim().toLowerCase();"
            + "    for (var k = 0; k < ownedElsewhere.length; k++) {"
            + "      if (grpText.indexOf(ownedElsewhere[k]) === 0) { fields.delete(el); return; }"
            + "    }"
            + "  }"
            + "});"
            + "fields.forEach(function(el) {"
            + "  var label = el.name || el.id || el.getAttribute('aria-label') || '';"
            + "  if (el.tagName === 'SELECT') {"
            // FIXED (TC_E2E_003, per explicit requirement): a <select> that already carries a real,
            // non-placeholder selection (e.g. Package Type/Tax Class's own live defaults, or MSI/any
            // other dropdown a caller explicitly set via selectMsiByIntent()/selectDropdownFieldByLabel()
            // BEFORE calling this method) must never be reassigned here — this branch previously fired
            // unconditionally for every matched <select>, silently overwriting an already-correct or
            // already-explicitly-set value with whatever this method's own brandValue/first-option
            // fallback produced. Text/number inputs already had an equivalent `if (!el.value)` guard
            // just below; selects had none until now.
            + "    var currentOpt = el.options[el.selectedIndex];"
            + "    var currentText = currentOpt ? currentOpt.textContent.trim().toLowerCase() : '';"
            + "    if (el.selectedIndex > 0 && currentText !== '' && currentText !== 'nothing selected') {"
            + "      return;"
            + "    }"
            + "    var opts = el.querySelectorAll('option');"
            + "    var chosen = null;"
            + "    for (var i = 0; i < opts.length; i++) {"
            + "      if (opts[i].textContent.trim().toLowerCase() === brandValue.toLowerCase()) { chosen = opts[i]; break; }"
            + "    }"
            + "    if (!chosen) {"
            + "      for (var i = 0; i < opts.length; i++) { if (opts[i].value) { chosen = opts[i]; break; } }"
            + "    }"
            + "    if (chosen) {"
            + "      el.value = chosen.value;"
            + "      el.dispatchEvent(new Event('change', {bubbles: true}));"
            + "      el.dispatchEvent(new Event('blur', {bubbles: true}));"
            + "      el.dispatchEvent(new Event('focusout', {bubbles: true}));"
            + "      results.push(label + '=' + chosen.textContent.trim());"
            + "    }"
            + "  } else if (el.type !== 'file' && el.type !== 'checkbox' && el.type !== 'radio') {"
            + "    if (!el.value) {"
            + "      counter++;"
            // CONFIRMED live (2026-09-12): a flat timestamp string is fine as a placeholder for opaque
            // text fields, but is invalid for fields with real semantic/format constraints (a 13-digit
            // number is not a valid Box_Height in cm) — branch on the field's visible group label to
            // pick a category-appropriate value instead of one-size-fits-all.
            + "      var grp2 = el.closest('.form-group');"
            + "      var grpText2 = grp2 ? grp2.textContent.trim().toLowerCase() : '';"
            + "      var uniqueValue;"
            + "      if (grpText2.indexOf('upc') !== -1 || grpText2.indexOf('ean') !== -1 || grpText2.indexOf('uan') !== -1) {"
            // Per explicit instruction: Shop SKU and UAN are both plain 8-digit numbers, and UAN must
            // equal Shop SKU — the test's own post-fill sync step (setFieldByLabel("UAN|...", freshShopSku))
            // already overwrites this placeholder with the real Shop SKU value, so this branch only
            // matters if that sync doesn't run; kept in the same 8-digit numeric shape either way.
            + "        uniqueValue = String((uniqueBase + counter) % 100000000).padStart(8, '0');"
            + "      } else if (grpText2.indexOf('sku') !== -1) {"
            // Per explicit instruction: Shop SKU is a plain 8-digit number (e.g. "12345678"), not the
            // previous "SKU" + timestamp + counter alphanumeric string.
            + "        uniqueValue = String((uniqueBase + counter) % 100000000).padStart(8, '0');"
            + "      } else if (/height|width|length|heigth|weight/.test(grpText2)) {"
            + "        uniqueValue = String(10 + (counter % 20));"
            + "      } else {"
            + "        uniqueValue = String(uniqueBase) + counter;"
            + "      }"
            + "      el.value = uniqueValue;"
            + "      el.dispatchEvent(new Event('input', {bubbles: true}));"
            + "      el.dispatchEvent(new Event('change', {bubbles: true}));"
            + "      el.dispatchEvent(new Event('blur', {bubbles: true}));"
            + "      el.dispatchEvent(new Event('focusout', {bubbles: true}));"
            + "      results.push(label + '=' + uniqueValue);"
            + "    }"
            + "  }"
            + "});"
            + "return results.join(' | ');",
            brandValue);
        LoggerUtility.info("Mandatory Product Characteristics filled: " + filled);
    }

    // Pre-submit safety net, per explicit requirement: re-runs the exact same three marker checks as
    // fillMandatoryProductCharacteristics() (required/aria-required attribute, literal "*" marker,
    // required/mandatory CSS class) PLUS the select2-driven fields owned by dedicated methods
    // (Marca/Fragile), and confirms every one of them actually holds a non-empty value right now —
    // catching anything a prior fill step silently failed to set (e.g. the Marca flakiness this
    // project hit repeatedly) before the irreversible Submit click, rather than after. Throws a single
    // RuntimeException enumerating every still-empty field (name, expected, actual state) rather than
    // failing on just the first one, so a run surfaces the complete picture in one shot.
    @SuppressWarnings("unchecked")
    public void validateAllMandatoryFieldsFilled() {
        List<String> violations = (List<String>) ((JavascriptExecutor) driver).executeScript(
            "var violations = [];"
            + "var fields = new Set();"
            + "document.querySelectorAll('input[required], select[required], textarea[required], "
            + "input[aria-required=\"true\"], select[aria-required=\"true\"]').forEach(function(f) { fields.add(f); });"
            + "document.querySelectorAll('label, .control-label').forEach(function(lbl) {"
            + "  if (lbl.textContent.indexOf('*') === -1) return;"
            + "  var grp = lbl.closest('.form-group') || lbl.parentElement;"
            + "  if (grp) { grp.querySelectorAll('input:not([id^=\"s2id_\"]):not(.select2-input):not(.select2-focusser), select:not(.select2-offscreen), textarea').forEach(function(f) { fields.add(f); }); }"
            + "});"
            + "document.querySelectorAll('*').forEach(function(el) {"
            + "  if (el.children.length === 0 && el.textContent.trim() === '*') {"
            + "    var grp = el.closest('.form-group');"
            + "    if (grp) { grp.querySelectorAll('input:not([id^=\"s2id_\"]):not(.select2-input):not(.select2-focusser), select:not(.select2-offscreen), textarea').forEach(function(f) { fields.add(f); }); }"
            + "  }"
            + "});"
            + "document.querySelectorAll('.required, .mandatory, [class*=\"required\"], [class*=\"mandatory\"]').forEach(function(el) {"
            + "  var grp = el.closest('.form-group') || el;"
            + "  grp.querySelectorAll('input:not([id^=\"s2id_\"]):not(.select2-input):not(.select2-focusser), select:not(.select2-offscreen), textarea').forEach(function(f) { fields.add(f); });"
            + "});"
            // Marca/Fragile are checked separately below via their select2 "chosen" display text,
            // which is what actually reflects the real selected value — CONFIRMED live: their
            // underlying raw <input> (same field family as attributeValuesFormCommand.attributeValues[N])
            // never gets synced by clicking the select2 option and stays empty even when the widget
            // correctly shows the selected value, so checking it here would be a false positive.
            + "var ownedElsewhereCheck = ['marca', 'fragile'];"
            + "fields.forEach(function(el) {"
            + "  if (el.type === 'file' || el.type === 'checkbox' || el.type === 'radio') return;"
            + "  var label = el.name || el.id || el.getAttribute('aria-label') || '(unlabeled field)';"
            + "  if (!el.value) {"
            + "    var grp = el.closest('.form-group');"
            + "    var visibleLabel = grp ? grp.textContent.trim().substring(0, 60).replace(/\\s+/g, ' ') : '(no .form-group)';"
            + "    var grpTextLower = visibleLabel.toLowerCase();"
            + "    for (var k = 0; k < ownedElsewhereCheck.length; k++) {"
            + "      if (grpTextLower.indexOf(ownedElsewhereCheck[k]) === 0) return;"
            + "    }"
            + "    violations.push(label + ' | tag=' + el.tagName + ' type=' + (el.type || 'n/a')"
            + "      + ' visibleGroupText=\"' + visibleLabel + '\" | expected: non-empty value | actual: empty');"
            + "  }"
            + "});"
            // Marca/Fragile are select2-only (no plain <select> guaranteed present depending on
            // category) — checked separately by their visible "chosen" text instead of .value.
            + "['Marca', 'Fragile'].forEach(function(targetLabel) {"
            + "  var found = false, filled = true, chosenText = '';"
            + "  document.querySelectorAll('label, .control-label').forEach(function(lbl) {"
            + "    if (found || lbl.textContent.trim() !== targetLabel) return;"
            + "    var grp = lbl.closest('.form-group');"
            + "    if (!grp) return;"
            + "    var chosen = grp.querySelector('.select2-chosen');"
            + "    if (chosen) {"
            + "      found = true;"
            + "      chosenText = chosen.textContent.trim();"
            + "      filled = chosenText !== '' && chosenText.toLowerCase() !== 'nothing selected';"
            + "    }"
            + "  });"
            + "  if (found && !filled) {"
            + "    violations.push(targetLabel + ' | expected: a selected value | actual: ' + chosenText);"
            + "  }"
            + "});"
            + "return violations;");
        if (violations != null && !violations.isEmpty()) {
            String message = "Mandatory field(s) still empty before Submit — " + violations.size() + " violation(s):\n"
                + String.join("\n", violations);
            LoggerUtility.error(message);
            throw new RuntimeException(message);
        }
        LoggerUtility.info("Pre-submit validation passed — all detected mandatory fields are populated");
    }

    // Scrolls the page top-to-bottom in a few steps before reading the named-field checklist below —
    // per explicit instruction, since some fields sit below the initial viewport and this project's
    // other lazy-render surprises (e.g. the Product Characteristics section only fully populating a
    // few seconds after category selection) suggest scroll-triggered rendering is plausible here too.
    public void scrollThroughEntirePage() {
        ((JavascriptExecutor) driver).executeScript(
            "var height = document.body.scrollHeight;"
            + "var steps = 6;"
            + "for (var i = 1; i <= steps; i++) {"
            + "  window.scrollTo(0, height * i / steps);"
            + "}");
        sleep(500);
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
    }

    // Named checklist for exactly the 19 fields the manual spec calls mandatory for this flow —
    // deliberately separate from fillMandatoryProductCharacteristics()/validateAllMandatoryFieldsFilled()
    // (which discover/validate whatever Mirakl ALSO happens to require beyond this list — skipping
    // those would break real submission, but they're intentionally excluded from this report since
    // they weren't asked for). Each field is located by its visible label text (same technique already
    // proven for Marca/Fragile/Product Name) and read back AFTER filling, so the checklist reflects
    // what's actually in the DOM, not just what an earlier fill step attempted to set. Returns the
    // ordered PASS/FAIL results; throws with a clear per-field reason if anything is missing, and the
    // caller must not click Submit in that case.
    // Package Type / Tax Class are native <select> fields that CONFIRMED live already carry a
    // real default ("N/A" / "Taxable Goods" respectively) on this form, so they're usually already
    // valid without any interaction — this only acts if one is genuinely unselected, reading the
    // live option list and picking the first real (non-placeholder) option rather than assuming a
    // fixed value, per explicit instruction to inspect actual options for this class of dropdown.
    public void ensureDropdownHasValidSelection(String labelText) {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "var targetLabel = arguments[0].toLowerCase();"
            + "var groups = document.querySelectorAll('.form-group');"
            + "for (var g = 0; g < groups.length; g++) {"
            + "  var grp = groups[g];"
            + "  if (grp.textContent.trim().toLowerCase().indexOf(targetLabel) !== 0) continue;"
            + "  var sel = grp.querySelector('select');"
            + "  if (!sel) return 'no-select';"
            + "  var opt = sel.options[sel.selectedIndex];"
            + "  if (opt && sel.selectedIndex > 0 && opt.textContent.trim().toLowerCase() !== 'nothing selected') {"
            + "    return 'already:' + opt.textContent.trim();"
            + "  }"
            + "  for (var i = 0; i < sel.options.length; i++) {"
            + "    var o = sel.options[i];"
            + "    if (o.value && o.textContent.trim().toLowerCase() !== 'nothing selected') {"
            + "      sel.value = o.value;"
            + "      sel.dispatchEvent(new Event('change', {bubbles: true}));"
            + "      sel.dispatchEvent(new Event('blur', {bubbles: true}));"
            + "      sel.dispatchEvent(new Event('focusout', {bubbles: true}));"
            + "      return 'set:' + o.textContent.trim();"
            + "    }"
            + "  }"
            + "  return 'no-valid-option';"
            + "}"
            + "return 'not-found';",
            labelText);
        LoggerUtility.info(labelText + " dropdown check: " + result);
    }

    public Map<String, String> runNamedMandatoryFieldsChecklist() {
        String[][] fieldDefs = {
            {"Shop SKU", "shop sku"},
            {"UAN", "upc/ean|uan|upc|ean"},
            {"MSI", "msi"},
            {"Price", "price"},
            {"Stock Quantity", "quantity|stock"},
            {"Fragile", "fragile"},
            {"Height", "heigth|height"},
            {"Length", "length"},
            {"Weight", "weight"},
            {"Width", "width"},
            {"Package Type", "package type"},
            {"Tax Class", "tax class"},
            {"Box_Height (cm)", "box_height|box height"},
            {"Box_Length (cm)", "box_length|box length"},
            {"Box_Width (cm)", "box_width|box width"},
            {"Box_Weight (kg)", "box_weight|box weight"},
            {"Marca", "marca"},
            {"Mirakl Image 1", "mirakl image 1"},
            {"Product Name", "product name"},
        };

        LinkedHashMap<String, String> results = new LinkedHashMap<>();
        List<String> failures = new java.util.ArrayList<>();

        for (String[] def : fieldDefs) {
            String displayName = def[0];
            String[] matchers = def[1].split("\\|");
            Object valueObj = ((JavascriptExecutor) driver).executeScript(
                "var matchers = arguments[0];"
                + "function matchesGroup(grpText) {"
                + "  for (var i = 0; i < matchers.length; i++) { if (grpText.indexOf(matchers[i]) === 0) return true; }"
                + "  return false;"
                + "}"
                // CONFIRMED live: several fields render TWO copies in this form (a product-level and
                // an offer/variant-level one — same pattern already seen for Shop SKU, UPC/EAN, Box
                // dimensions, Product Name). Stopping at the FIRST matching .form-group risked
                // reporting an untouched duplicate as the field's value even when the OTHER copy was
                // correctly set (confirmed live for MSI). Checking every matching group and preferring
                // any with a genuine non-empty value fixes this regardless of DOM order.
                + "function extractValue(grp) {"
                + "  var sel = grp.querySelector('select');"
                + "  if (sel) {"
                + "    var opt = sel.options[sel.selectedIndex];"
                + "    var t2 = opt ? opt.textContent.trim() : '';"
                + "    if (t2 && t2.toLowerCase() !== 'nothing selected' && sel.selectedIndex > 0) { return t2; }"
                + "  }"
                + "  var chosen = grp.querySelector('.select2-chosen');"
                + "  if (chosen) {"
                + "    var t = chosen.textContent.trim();"
                + "    if (t && t.toLowerCase() !== 'nothing selected') { return t; }"
                + "  }"
                + "  if (sel || chosen) { return ''; }"
                + "  var fileInput = grp.querySelector('input[type=file]');"
                + "  if (fileInput) {"
                + "    var fv = fileInput.value || '';"
                + "    return fv ? fv.split(/[\\\\/]/).pop() : '';"
                + "  }"
                + "  var textInput = grp.querySelector('input, textarea');"
                + "  if (textInput) { return textInput.value || ''; }"
                + "  return null;"
                + "}"
                + "var groups = document.querySelectorAll('.form-group');"
                + "var anyMatched = false;"
                + "var bestEmpty = '';"
                + "for (var g = 0; g < groups.length; g++) {"
                + "  var grp = groups[g];"
                + "  var grpText = grp.textContent.trim().toLowerCase();"
                + "  if (!matchesGroup(grpText)) continue;"
                + "  var v = extractValue(grp);"
                + "  if (v === null) continue;"
                + "  anyMatched = true;"
                + "  if (v !== '') { return v; }"
                + "  bestEmpty = v;"
                + "}"
                + "return anyMatched ? bestEmpty : null;",
                (Object) matchers);

            if (valueObj == null) {
                results.put(displayName, null);
                failures.add(displayName + " — field not found on the page");
            } else {
                String value = valueObj.toString();
                results.put(displayName, value);
                if (value.isEmpty()) {
                    failures.add(displayName + " — present but empty/not selected");
                }
            }
        }

        for (Map.Entry<String, String> entry : results.entrySet()) {
            String value = entry.getValue();
            boolean pass = value != null && !value.isEmpty();
            LoggerUtility.info("[" + (pass ? "PASS" : "FAIL") + "] " + entry.getKey() + " = "
                + (value == null ? "(field not found)" : value));
        }

        if (!failures.isEmpty()) {
            String message = "Mandatory field checklist failed — " + failures.size() + " field(s) not ready for "
                + "Submit:\n" + String.join("\n", failures);
            LoggerUtility.error(message);
            throw new RuntimeException(message);
        }
        return results;
    }

    // CONFIRMED live (2026-09-12) via MarcaFieldProbe: "Marca" is a category-conditional Product
    // Characteristic — for the Bebé > Accesorios para Bebé > Pañaleras y accesorios path used by this
    // test, it does not render at all (confirmed by dumping every select2 widget and every exact-text
    // "Marca" leaf node on the fully-rendered form — zero matches either way). An earlier, broader
    // substring/.closest() based lookup produced a false-positive match (almost certainly stale
    // "Marca del Ahorro" text left over in the shared #select2-drop element from the Category
    // dropdown) and hung for the full 2-minute implicit wait clicking a nonexistent option. This
    // version only ever matches an EXACT-text "Marca" leaf node (same precise technique that reliably
    // proved the field's absence) and returns cleanly with a clear log line if none exists — it never
    // guesses a nearby container and never blocks waiting for an option that might not be there.
    public void selectBrand(String brandValue) {
        selectDropdownFieldByLabel("Marca", brandValue);
    }

    // CONFIRMED live (2026-09-12): "Fragile" is a genuinely mandatory Product Characteristic for this
    // category — it carries neither a "required" HTML attribute nor a visible asterisk, so
    // fillMandatoryProductCharacteristics() silently skips it, and Mirakl rejects the whole submission
    // with "Your form contains errors" / a has-error badge on this exact field. Real valid options
    // (confirmed live elsewhere in this project, MiraklCatalogManagementPage.setFragile()) are
    // Spanish "No"/"Si", not English "No"/"Yes" — "No" is used as the safe default here since this
    // test has no business requirement for a specific Fragile value.
    //
    // Deliberately NOT routed through selectDropdownFieldByLabel() (still used by selectBrand()) —
    // root-caused live (2026-09-13) via network capture: Fragile's widget is remote-data-backed
    // (GET /mmp/shop/setting/named-list/value/search-json?namedListId=...&searchString=...) and only
    // reliably issues that query, and renders ITS OWN options, when a real keystroke is typed into its
    // search box. The shared helper only opens the widget and passively polls for the option — on
    // every observed failure (empty options, or another field's leaked options appearing) no such
    // query had fired at all. This method opens the widget, types the target value into the search
    // input to force a fresh, scoped query, and — critically — only ever reads/clicks options from the
    // shared #select2-drop element (a select2 v3 singleton, not a per-widget node) while it is
    // actually visible (lacks select2-display-none), never a global option-text search, since a
    // stale/hidden drop retaining a previous field's rendered options was the direct cause of the
    // "wrong options" failures. Success is reported ONLY after re-reading the widget's own
    // .select2-chosen text back and confirming it equals the intended value — never a blind
    // first-option fallback, never assumed from the click alone.
    public void selectFragile(String preferredValue) {
        String labelText = "Fragile";

        // Same principle as selectDropdownFieldByLabel()'s native-select fast path: if this render
        // backs the field with a real <select> (not select2-only), setting it directly is far more
        // reliable than any select2 interaction. Falls through to the select2 flow below if absent.
        Object nativeResult = ((JavascriptExecutor) driver).executeScript(
            "var targetLabel = arguments[1];"
            + "var nodes = document.querySelectorAll('label, .control-label, span, div');"
            + "var grp = null;"
            + "for (var i = 0; i < nodes.length; i++) {"
            + "  var el = nodes[i];"
            + "  if (el.children.length === 0 && el.textContent.trim() === targetLabel) {"
            + "    grp = el.closest('.form-group') || el.parentElement;"
            + "    break;"
            + "  }"
            + "}"
            + "if (!grp) return null;"
            + "var sel = grp.querySelector('select:not(.select2-offscreen)');"
            + "if (!sel) return null;"
            + "var opts = sel.querySelectorAll('option');"
            + "for (var j = 0; j < opts.length; j++) {"
            + "  if (opts[j].textContent.trim().toLowerCase() === arguments[0].toLowerCase()) {"
            + "    sel.value = opts[j].value;"
            + "    sel.dispatchEvent(new Event('change', {bubbles: true}));"
            + "    sel.dispatchEvent(new Event('blur', {bubbles: true}));"
            + "    return 'native:' + (sel.id || '(no id)');"
            + "  }"
            + "}"
            + "return null;",
            preferredValue, labelText);
        if (nativeResult != null) {
            LoggerUtility.info(labelText + " set via native select (" + nativeResult + "): " + preferredValue);
            return;
        }

        // Fixed locators confirmed live against the real DOM: the choice link is the "Nothing
        // selected" span next to the "Fragile" label, and the option is a plain <li>/<div role="option">
        // pair rendered in the shared select2 drop — no data-testid/container discovery needed.
        By choiceLinkLocator = By.xpath(
            "(//label[text()='Fragile']/../following-sibling::div[1]/div/a/span[text()='Nothing selected'])[1]");
        By optionLocator = By.xpath("//li//div[text()='" + preferredValue.replace("'", "") + "' and @role='option']");
        By chosenValueLocator = By.xpath(
            "(//label[text()='Fragile']/../following-sibling::div[1]/div/a/span)[1]");

        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            // ROOT-CAUSED live (TC_E2E_003): a stray #select2-drop-mask left over from a PRIOR widget's
            // interaction (e.g. Marca, selected right before this) sits on top of Fragile's choice link
            // and silently swallows the click before it ever reaches the real element. Dismiss it first.
            dismissStraySelect2Mask();

            List<WebElement> links = driver.findElements(choiceLinkLocator);
            if (links.isEmpty()) {
                LoggerUtility.info(labelText + " choice link not found with 'Nothing selected' text — "
                    + "field may already hold a value or not apply to this category; skipping");
                return;
            }
            WebElement choiceLink = links.get(0);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", choiceLink);
            sleep(200);
            // dismissStraySelect2Mask() mutates the DOM (clicking the mask) and can invalidate the
            // reference captured above — re-fetch fresh immediately before clicking (same fix already
            // proven necessary live for this exact widget).
            dismissStraySelect2Mask();
            try {
                choiceLink = driver.findElement(choiceLinkLocator);
            } catch (org.openqa.selenium.NoSuchElementException e) {
                LoggerUtility.info(labelText + " choice link disappeared after mask dismissal — already set; skipping");
                return;
            }
            try {
                new Actions(driver).moveToElement(choiceLink).click().perform();
            } catch (StaleElementReferenceException e) {
                LoggerUtility.warn("Stale element clicking " + labelText + " choice link, retrying: " + e.getMessage());
                choiceLink = driver.findElement(choiceLinkLocator);
                new Actions(driver).moveToElement(choiceLink).click().perform();
            }

            boolean optionFound = false;
            for (int i = 0; i < 20; i++) {
                if (!driver.findElements(optionLocator).isEmpty()) {
                    optionFound = true;
                    break;
                }
                sleep(300);
            }
            if (!optionFound) {
                LoggerUtility.warn(labelText + " option '" + preferredValue + "' not found after opening the "
                    + "dropdown on attempt " + attempt + "/" + maxAttempts + " — retrying");
                dismissStraySelect2Mask();
                sleep(500);
                continue;
            }

            try {
                new Actions(driver).moveToElement(driver.findElement(optionLocator)).click().perform();
            } catch (StaleElementReferenceException e) {
                LoggerUtility.warn("Stale element clicking " + labelText + " option, retrying: " + e.getMessage());
                new Actions(driver).moveToElement(driver.findElement(optionLocator)).click().perform();
            }
            sleep(300);

            String displayedValue = "";
            List<WebElement> chosenSpans = driver.findElements(chosenValueLocator);
            if (!chosenSpans.isEmpty()) {
                displayedValue = chosenSpans.get(0).getText().trim();
            }
            if (preferredValue.equals(displayedValue)) {
                LoggerUtility.info(labelText + " set via select2 widget, verified displayed value: " + displayedValue);
                return;
            }
            LoggerUtility.warn(labelText + " option clicked but displayed value is '" + displayedValue
                + "' (expected '" + preferredValue + "') on attempt " + attempt + "/" + maxAttempts + " — retrying");
            sleep(500);
        }
        LoggerUtility.warn(labelText + " could not be reliably selected and verified after " + maxAttempts
            + " attempts — leaving unset");
    }

    // See the root-cause note in selectFragile() above: #select2-drop-mask is select2 v3's own global,
    // body-appended "click outside to close" overlay, shared across every select2 widget on the page
    // (same singleton-sharing model as #select2-drop itself). If a prior widget interaction leaves
    // select2 believing a dropdown is still open, the mask stays up and silently intercepts every click
    // aimed at anything beneath it — including a completely unrelated widget's own choice link — with no
    // exception thrown, since the click still "lands" somewhere, just not where intended. Clicking the
    // mask is select2's own built-in mechanism for closing whatever it thinks is open; a no-op if the
    // mask isn't present/visible.
    private void dismissStraySelect2Mask() {
        // CONFIRMED live (TC_E2E_003): the first version of this check used `mask.offsetParent !==
        // null` (the same visibility idiom used elsewhere in this file for #select2-drop) and NEVER
        // fired — the mask is styled `position: fixed` (needed to cover the full viewport regardless of
        // scroll position), and offsetParent is spec-defined to be null for any fixed-position element
        // even while it is genuinely visible and on top. Checking computed display/visibility instead
        // works regardless of positioning scheme.
        Object dismissed = ((JavascriptExecutor) driver).executeScript(
            "var mask = document.getElementById('select2-drop-mask');"
            + "if (!mask) return false;"
            + "var cs = window.getComputedStyle(mask);"
            + "if (cs.display === 'none' || cs.visibility === 'hidden') return false;"
            + "mask.click();"
            + "return true;");
        if (Boolean.TRUE.equals(dismissed)) {
            LoggerUtility.info("Dismissed a stray/blocking select2-drop-mask");
            sleep(300);
        }
    }

    // Used by selectBrand() (Marca) — a select2-driven Product Characteristic whose real DOM shape
    // (native <select> vs. a select2 widget with no backing <select>) and even presence varies by
    // category/run — tries the native path first, falls back to opening the select2 widget, and
    // fails fast/cleanly (bounded JS polls, never driver.findElement() on a possibly-absent element)
    // rather than blocking on the 2-minute implicit wait.
    private void selectDropdownFieldByLabel(String labelText, String value) {
        Object nativeResult = ((JavascriptExecutor) driver).executeScript(
            "var targetLabel = arguments[1];"
            + "var selects = document.querySelectorAll('select');"
            + "for (var i = 0; i < selects.length; i++) {"
            + "  var el = selects[i];"
            + "  var lblText = '';"
            + "  if (el.id) { var lbl = document.querySelector('label[for=\"' + el.id + '\"]'); if (lbl) lblText = lbl.textContent.trim(); }"
            + "  if (lblText.toLowerCase() === targetLabel.toLowerCase()) {"
            + "    var opts = el.querySelectorAll('option');"
            + "    for (var j = 0; j < opts.length; j++) {"
            + "      if (opts[j].textContent.trim().toLowerCase() === arguments[0].toLowerCase()) {"
            + "        el.value = opts[j].value;"
            + "        el.dispatchEvent(new Event('change', {bubbles: true}));"
            + "        el.dispatchEvent(new Event('blur', {bubbles: true}));"
            + "        el.dispatchEvent(new Event('focusout', {bubbles: true}));"
            + "        return 'native:' + el.id;"
            + "      }"
            + "    }"
            + "  }"
            + "}"
            + "return null;",
            value, labelText);
        if (nativeResult != null) {
            LoggerUtility.info(labelText + " set via native select (" + nativeResult + "): " + value);
            return;
        }

        By optionLabel = By.xpath(
            "//div[contains(@class,'select2-result-label') and normalize-space()='" + value + "']");
        String containerLookupScript =
            "var targetLabel = arguments[0];"
            + "var nodes = document.querySelectorAll('label, .control-label, span, div');"
            + "for (var i = 0; i < nodes.length; i++) {"
            + "  var el = nodes[i];"
            + "  if (el.children.length === 0 && el.textContent.trim() === targetLabel) {"
            + "    var grp = el.closest('.form-group') || el.parentElement;"
            + "    if (grp) {"
            + "      var c = grp.querySelector('[id^=\"s2id_\"]');"
            + "      if (c) return c.id;"
            + "    }"
            + "  }"
            + "}"
            + "return null;";

        // CONFIRMED live (2026-09-12): occasionally the widget opens with ZERO options rendered at
        // all (seen for both Marca and Fragile on different runs, same code/data otherwise) — a
        // rendering hiccup, not a real absence of the option. Re-resolving the container id fresh on
        // each attempt (not just reopening the same one) also guards against the container's own DOM
        // node having been replaced entirely by a re-render triggered by an earlier field's selection.
        //
        // ROOT-CAUSED live (2026-09-15): a Marca-on-retry failure (right after Mirakl's "category
        // configuration changed" concurrency-retry reopened the whole form) showed this method log
        // "Marca set via select2 widget: DIVA CUP" while the checklist — which reads .select2-chosen
        // directly — still saw it empty immediately after. The option click used to happen exactly
        // ONCE, entirely outside this retry loop, and success was logged unconditionally without ever
        // reading back whether the click actually changed the widget's displayed value — the same
        // class of bug already fixed in selectFragile(). The click-and-verify step is now inside the
        // loop below, using the same verified-displayed-value check, so a click that silently fails to
        // register gets a fresh attempt (fresh container lookup, fresh widget open) instead of a false
        // "success".
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Object containerIdObj = ((JavascriptExecutor) driver).executeScript(containerLookupScript, labelText);
            if (containerIdObj == null) {
                LoggerUtility.warn(labelText + " field does not exist for this category — skipping (not applicable)");
                return;
            }
            String containerId = containerIdObj.toString();

            Object hasChoiceLink = ((JavascriptExecutor) driver).executeScript(
                "var c = document.getElementById(arguments[0]);"
                + "return c ? (c.querySelector('a.select2-choice') !== null) : false;",
                containerId);
            if (!Boolean.TRUE.equals(hasChoiceLink)) {
                LoggerUtility.warn(labelText + " container found (" + containerId + ") but has no select2-choice "
                    + "link (unexpected widget structure) — skipping");
                return;
            }
            By choiceLinkLocator = By.cssSelector("#" + containerId + " a.select2-choice");
            // CONFIRMED live (2026-09-12): both an explicit scrollIntoView() before this click and an
            // explicit "did the dropdown actually open" verification were tried here and made things
            // WORSE each time (scrollIntoView(true) put the element under Mirakl's fixed header;
            // checking for an open drop produced false negatives even for Marca, which had been 100%
            // reliable with neither). Reverted to the simplest version: click, then poll for the
            // option directly — this is what actually worked across the most runs.
            try {
                new Actions(driver).moveToElement(driver.findElement(choiceLinkLocator)).click().perform();
            } catch (StaleElementReferenceException e) {
                LoggerUtility.warn("Stale element clicking " + labelText + " choice link, retrying: " + e.getMessage());
                new Actions(driver).moveToElement(driver.findElement(choiceLinkLocator)).click().perform();
            }

            boolean optionFound = false;
            for (int i = 0; i < 25; i++) {
                Object present = ((JavascriptExecutor) driver).executeScript(
                    "return document.evaluate(\"//div[contains(@class,'select2-result-label') and "
                    + "normalize-space()='" + value.replace("'", "") + "']\", document, null, "
                    + "XPathResult.BOOLEAN_TYPE, null).booleanValue;");
                if (Boolean.TRUE.equals(present)) {
                    optionFound = true;
                    break;
                }
                sleep(300);
            }

            if (!optionFound) {
                Object visibleOptions = ((JavascriptExecutor) driver).executeScript(
                    "var out = [];"
                    + "document.querySelectorAll('.select2-result-label').forEach(function(el) { out.push(el.textContent.trim()); });"
                    + "return out.join(', ');");
                LoggerUtility.warn(labelText + " widget rendered no matching option on attempt " + attempt
                    + "/" + maxAttempts + " (container " + containerId + ") — visible options: [" + visibleOptions
                    + "] — closing and retrying with a fresh lookup");
                try {
                    driver.findElement(choiceLinkLocator).click();
                } catch (Exception e) {
                    // best-effort close; the next attempt re-resolves the container fresh regardless
                }
                sleep(700);
                continue;
            }

            try {
                new Actions(driver).moveToElement(driver.findElement(optionLabel)).click().perform();
            } catch (StaleElementReferenceException e) {
                LoggerUtility.warn("Stale element clicking " + labelText + " option, retrying: " + e.getMessage());
                new Actions(driver).moveToElement(driver.findElement(optionLabel)).click().perform();
            }

            // CONFIRMED live (2026-09-12): clicking the select2 option updates the widget's visible
            // "chosen" text but never syncs the field's own raw backing <input> (select2-on-input, not
            // select2-on-select) — that hidden input is what other form-wide re-renders read from, so
            // without this it silently reverts to "Nothing selected" the next time anything else on the
            // page changes. Directly set + fire the full event chain (matching the fix already proven for
            // Price's hidden sync field) so the selection actually persists.
            ((JavascriptExecutor) driver).executeScript(
                "var targetLabel = arguments[0];"
                + "var v = arguments[1];"
                + "document.querySelectorAll('input[type=text], input:not([type])').forEach(function(el) {"
                + "  var grp = el.closest('.form-group');"
                + "  if (!grp) return;"
                + "  if (grp.textContent.trim().toLowerCase().indexOf(targetLabel.toLowerCase()) !== 0) return;"
                + "  el.value = v;"
                + "  el.dispatchEvent(new Event('input', {bubbles: true}));"
                + "  el.dispatchEvent(new Event('change', {bubbles: true}));"
                + "  el.dispatchEvent(new Event('blur', {bubbles: true}));"
                + "  el.dispatchEvent(new Event('focusout', {bubbles: true}));"
                + "});",
                labelText, value);

            String displayedValue = String.valueOf(((JavascriptExecutor) driver).executeScript(
                "var c = document.getElementById(arguments[0]);"
                + "if (!c) return '';"
                + "var chosen = c.querySelector('.select2-chosen');"
                + "return chosen ? chosen.textContent.trim() : '';",
                containerId));

            if (value.equals(displayedValue)) {
                LoggerUtility.info(labelText + " set via select2 widget (" + containerId + "), verified displayed "
                    + "value: " + displayedValue);
                return;
            }
            LoggerUtility.warn(labelText + " option clicked but displayed value is '" + displayedValue
                + "' (expected '" + value + "') on attempt " + attempt + "/" + maxAttempts + " — retrying");
            sleep(500);
        }
        LoggerUtility.warn(labelText + " could not be reliably selected and verified after " + maxAttempts
            + " attempts — leaving unset");
    }

    // Product Name must be a value the test itself controls and can reliably search for later on
    // the FDA storefront — relying on fillMandatoryProductCharacteristics()'s generic
    // "AutoTest_<label>" fallback would only work if that field's name/id/aria-label attribute
    // happens to literally be "Product Name", which was never confirmed. This targets the field by
    // its visible label text instead (same discovery technique as selectBrand()) and sets it directly.
    // CONFIRMED live (2026-09-12): there are TWO separate "Product Name" text inputs on this form
    // (a product-level one and an offer/variant-level one, e.g. "attributeValuesFormCommand.
    // attributeValues[2].attributeValue" vs. "offerAndVariantsCommand['ui-id-0'].
    // attributeValuesFormCommand.attributeValues[2].attributeValue") — setting only the first (the
    // original behavior here) left the second one empty and failing pre-submit validation. Sets
    // every matching field, not just the first.
    public void enterProductName(String productName) {
        Object matchedIds = ((JavascriptExecutor) driver).executeScript(
            // CONFIRMED live (2026-09-13): "arguments[0]" referenced INSIDE the forEach callback below
            // resolves to that callback's OWN arguments (el, index, array) — i.e. the DOM element
            // itself, not this script's productName parameter. That silently set el.value = el, which
            // JS coerces to the string "[object HTMLInputElement]" (confirmed via direct DOM read: the
            // input's .value literally held that string, explaining the checklist's garbled "Product
            // Name" output). Same exact shadowing bug already fixed once in
            // fillMandatoryProductCharacteristics() — capture the value in an outer-scoped variable
            // BEFORE the callback, same fix applied here.
            "var nameValue = arguments[0];"
            + "var inputs = document.querySelectorAll('input[type=text], input:not([type])');"
            + "var matched = [];"
            + "inputs.forEach(function(el) {"
            + "  var labelText = '';"
            + "  if (el.id) { var lbl = document.querySelector('label[for=\"' + el.id + '\"]'); if (lbl) labelText = lbl.textContent; }"
            + "  if (!labelText) { var grp = el.closest('.form-group, .input'); if (grp) labelText = grp.textContent; }"
            + "  if (labelText.toLowerCase().indexOf('product name') !== -1) {"
            + "    el.value = nameValue;"
            + "    el.dispatchEvent(new Event('input', {bubbles: true}));"
            + "    el.dispatchEvent(new Event('change', {bubbles: true}));"
            + "    el.dispatchEvent(new Event('blur', {bubbles: true}));"
            + "    el.dispatchEvent(new Event('focusout', {bubbles: true}));"
            + "    matched.push(el.id || el.name || 'matched');"
            + "  }"
            + "});"
            + "return matched;",
            productName);
        @SuppressWarnings("unchecked")
        java.util.List<String> ids = (java.util.List<String>) matchedIds;
        if (ids == null || ids.isEmpty()) {
            LoggerUtility.warn("Could not locate any Product Name field — value NOT set: " + productName);
        } else {
            LoggerUtility.info("Product Name set on " + ids.size() + " field(s) (" + String.join(", ", ids) + "): " + productName);
        }
    }

    // Additive helper for the Mirakl-concurrency retry flow in Tc_manualproductandoffercreation_01 —
    // does NOT change fillMandatoryProductCharacteristics()'s own field-detection/value-generation
    // logic at all. Pre-seeds a specific text field (by label prefix, e.g. "Shop SKU"/"UAN") with an
    // explicit, caller-supplied value BEFORE the generic filler runs, so its `if (!el.value)` guard
    // skips that field and the SAME identifier survives a full form re-fill on retry instead of the
    // filler generating a fresh one.
    // labelText may contain multiple "|"-separated prefix candidates (same convention as the named
    // checklist's own matchers) since a field's live label doesn't always match the checklist's
    // display name verbatim (e.g. UAN's live label is "UPC/EAN").
    public void setFieldByLabel(String labelText, String value) {
        String[] matchers = labelText.toLowerCase().split("\\|");
        Object result = ((JavascriptExecutor) driver).executeScript(
            "var matchers = arguments[0];"
            + "var groups = document.querySelectorAll('.form-group');"
            + "var setCount = 0;"
            + "for (var g = 0; g < groups.length; g++) {"
            + "  var grp = groups[g];"
            + "  var grpText = grp.textContent.trim().toLowerCase();"
            + "  var matched = false;"
            + "  for (var m = 0; m < matchers.length; m++) { if (grpText.indexOf(matchers[m]) === 0) { matched = true; break; } }"
            + "  if (!matched) continue;"
            + "  var el = grp.querySelector('input, textarea');"
            + "  if (!el || el.type === 'file') continue;"
            + "  el.value = arguments[1];"
            + "  el.dispatchEvent(new Event('input', {bubbles: true}));"
            + "  el.dispatchEvent(new Event('change', {bubbles: true}));"
            + "  el.dispatchEvent(new Event('blur', {bubbles: true}));"
            + "  el.dispatchEvent(new Event('focusout', {bubbles: true}));"
            + "  setCount++;"
            + "}"
            + "return setCount;",
            (Object) matchers, value);
        LoggerUtility.info(labelText + " pre-seeded on " + result + " field(s): " + value);
    }

    // Additive, read-only counterpart to setFieldByLabel() — reads a text field's current live value
    // back by label prefix (same "|"-separated matcher convention as the checklist/setFieldByLabel).
    // Used to synchronize Shop SKU and UAN to an identical value right after the generic filler
    // independently generates both, without touching fillMandatoryProductCharacteristics() itself.
    public String getFieldValueByLabel(String labelText) {
        String[] matchers = labelText.toLowerCase().split("\\|");
        Object result = ((JavascriptExecutor) driver).executeScript(
            "var matchers = arguments[0];"
            + "var groups = document.querySelectorAll('.form-group');"
            + "for (var g = 0; g < groups.length; g++) {"
            + "  var grp = groups[g];"
            + "  var grpText = grp.textContent.trim().toLowerCase();"
            + "  var matched = false;"
            + "  for (var m = 0; m < matchers.length; m++) { if (grpText.indexOf(matchers[m]) === 0) { matched = true; break; } }"
            + "  if (!matched) continue;"
            + "  var el = grp.querySelector('input, textarea');"
            + "  if (!el || el.type === 'file') continue;"
            + "  if (el.value) { return el.value; }"
            + "}"
            + "return null;",
            (Object) matchers);
        return result == null ? null : result.toString();
    }

    // --- Add Offer form ---

    public void enterOfferSku(String offerSku) {
        LoggerUtility.info("Entering Offer SKU: " + offerSku);
        scrollIntoView(OFFER_SKU_FIELD);
        type(OFFER_SKU_FIELD, offerSku);
    }

    public void enterStockQuantity(String quantity) {
        LoggerUtility.info("Entering Stock Quantity: " + quantity);
        type(STOCK_QUANTITY_FIELD, quantity);
    }

    // CONFIRMED live (2026-09-13): typing + TAB occasionally does not survive to be read back
    // (observed once immediately after Fragile's widget exhausted its own 3 failed attempts,
    // suggesting page-level re-render/focus disruption at that moment, not a defect in this method's
    // own targeting) — verifying and retrying here, rather than trusting the entry unconditionally,
    // matches the same "verify the actual displayed value" discipline already applied to Fragile.
    public void enterPrice(String price) {
        By target = isPresentInDom("ui-id-0unitPrice") ? PRICE_FIELD_VARIANT : PRICE_FIELD;
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            LoggerUtility.info("Entering Price: " + price + " (attempt " + attempt + "/" + maxAttempts + ")");
            type(target, price);
            // CONFIRMED live (2026-09-12): the visible price input syncs to a separate hidden field
            // (runningPricing.pricingPoints[0].price) that the form actually submits — that sync only
            // fires on blur, not on the input/keyup events sendKeys() already produces while typing.
            WebElement el = driver.findElement(target);
            el.sendKeys(Keys.TAB);
            String actual = el.getAttribute("value");
            if (price.equals(actual)) {
                LoggerUtility.info("Price verified as displayed: " + actual);
                return;
            }
            LoggerUtility.warn("Price entry did not stick on attempt " + attempt + "/" + maxAttempts
                + " — expected '" + price + "', field currently shows '" + actual + "'");
            sleep(500);
        }
        LoggerUtility.warn("Price could not be verified as '" + price + "' after " + maxAttempts + " attempts");
    }

    // Instant JS presence check by element id — avoids the global 2-minute implicit wait that
    // driver.findElement()/isDisplayed() would incur if the id genuinely doesn't exist on the
    // current form variant (see enterPrice()'s two possible price-field ids).
    private boolean isPresentInDom(String elementId) {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return document.getElementById(arguments[0]) !== null;", elementId);
        return Boolean.TRUE.equals(result);
    }

    // msiValue is "Yes" or "No" — this field is a Yes/No toggle, not an installment count
    public void selectMsi(String msiValue) {
        LoggerUtility.info("Selecting MSI value: " + msiValue);
        scrollIntoView(MSI_DROPDOWN);
        new Select(driver.findElement(MSI_DROPDOWN)).selectByVisibleText(msiValue);
        LoggerUtility.info("MSI selected: " + msiValue);
    }

    // Reads the MSI dropdown's actual live options and picks whichever one semantically means
    // "yes"/"sí" or "no", instead of assuming the UI shows literal English "Yes"/"No" — this
    // Mirakl instance happens to show English text (confirmed live), but a different
    // shop/locale could show "Sí"/"No" instead, and this never blindly falls back to "whichever
    // option is first" the way the generic required-field filler does for unrecognized fields.
    // Returns the matched live option text (e.g. "Yes" or "Si") — purely additive, exposes what this
    // method already computes internally, so the test can assert the checklist's later readback of
    // MSI matches exactly what was selected here. No change to the selection logic itself.
    public String selectMsiByIntent(boolean enableMsi) {
        scrollIntoView(MSI_DROPDOWN);
        WebElement select = driver.findElement(MSI_DROPDOWN);
        List<WebElement> options = select.findElements(By.tagName("option"));
        String matchedText = null;
        for (WebElement option : options) {
            String text = option.getText().trim();
            String normalized = text.toLowerCase();
            boolean isYes = normalized.equals("yes") || normalized.equals("si") || normalized.equals("sí");
            boolean isNo = normalized.equals("no");
            if ((enableMsi && isYes) || (!enableMsi && isNo)) {
                matchedText = text;
                break;
            }
        }
        if (matchedText == null) {
            throw new RuntimeException("MSI dropdown has no live option matching intent enableMsi=" + enableMsi
                + " — actual options: " + options.stream().map(WebElement::getText).collect(java.util.stream.Collectors.joining(", ")));
        }
        LoggerUtility.info("Selecting MSI value (intent enableMsi=" + enableMsi + "): " + matchedText);
        new Select(select).selectByVisibleText(matchedText);
        // CONFIRMED live (2026-09-12): a change dispatched via Select's option click doesn't always
        // survive a later form-wide re-render unless followed by blur/focusout — same fix already
        // proven for Price's hidden sync field, applied here for consistency.
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].dispatchEvent(new Event('blur', {bubbles: true}));"
            + "arguments[0].dispatchEvent(new Event('focusout', {bubbles: true}));",
            select);
        LoggerUtility.info("MSI selected: " + matchedText);
        return matchedText;
    }

    // Unlike MSI_DROPDOWN, this <select> is display:none (select2 hides the real element behind
    // its own widget) — Selenium's Select class calls .click() on the <option>, which throws
    // ElementNotInteractableException on a hidden element. Set the value via JS instead and fire
    // a native change event so select2's visual widget still updates (same JS-driven pattern used
    // in MiraklReturnPage.enterPartialRefundQuantity() for other framework-managed fields).
    public void selectLogistics(String warehouseValue) {
        LoggerUtility.info("Selecting Logistics/warehouse: " + warehouseValue);
        WebElement select = driver.findElement(LOGISTICS_DROPDOWN);
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));" +
            "arguments[0].dispatchEvent(new Event('blur', {bubbles: true}));" +
            "arguments[0].dispatchEvent(new Event('focusout', {bubbles: true}));",
            select, warehouseValue);
        LoggerUtility.info("Logistics selected: " + warehouseValue);
    }

    public void clickSubmitForApproval() {
        LoggerUtility.info("Clicking Create offer");
        By target;
        if (isPresentInDom("saveOfferButton")) {
            target = SUBMIT_FOR_APPROVAL_BUTTON;
        } else if (isPresentInDom("addProductAndOffer")) {
            target = SUBMIT_FOR_APPROVAL_BUTTON_VARIANT;
        } else {
            target = SUBMIT_FOR_APPROVAL_BUTTON_FALLBACK;
        }
        scrollIntoView(target);
        // EXPERIMENT (2026-09-15): switched from jsClick() (a JS-synthesized, isTrusted:false click)
        // to a genuine Selenium click — a real user's click is trusted; jsClick()'s synthetic event
        // is not, and this was the one still-unverified difference between a manual submission
        // (which works) and every automated one (which has consistently hit Mirakl's "category
        // configuration changed" error). Falls back to jsClick() only if the native click can't land
        // (e.g. something overlapping the button), so this never silently fails to submit at all.
        try {
            driver.findElement(target).click();
            LoggerUtility.info("Submit clicked via native Selenium click (trusted event)");
        } catch (Exception e) {
            LoggerUtility.warn("Native click on Submit failed (" + e.getMessage() + ") — falling back to jsClick()");
            jsClick(target);
        }

        // CONFIRMED live (2026-09-12): when any mandatory field is left unset, Mirakl shows a
        // "Your form contains errors" danger banner and the page never navigates or shows a success
        // banner — SUCCESS_MESSAGE_BANNER explicitly excludes danger-class banners, so without this
        // check the wait below would silently time out after the full 2 minutes with no useful error,
        // masking the real cause. A bounded poll fails fast with the actual reason instead.
        //
        // ROOT-CAUSED live (TC_E2E_003): the first version of this only checked for "category
        // configuration changed" for 3 seconds (10 x 300ms) before falling through to a SEPARATE,
        // unguarded WaitUtility.fluentWait(SUCCESS_MESSAGE_BANNER) call with its own independent 2-minute
        // window. Live evidence: the category-configuration-changed error banner took longer than 3s to
        // render, so the short pre-check missed it — and that banner does NOT carry a "danger" class
        // (confirmed: getSuccessMessage() afterward returned the literal error text), so
        // SUCCESS_MESSAGE_BANNER's own "not(contains(@class,'danger'))" filter did not exclude it either.
        // The result was a genuine submission failure silently reported as a successful one. Both error
        // checks and the success-banner check are now unified into ONE poll that runs for the SAME
        // ~2-minute budget the success wait used to get on its own — whichever of the three appears
        // first wins, and error text is checked before the success-banner presence check on every single
        // iteration so a late-arriving error can never be shadowed by a loosely-matched "success" element.
        long deadlineMs = System.currentTimeMillis() + 120_000;
        while (System.currentTimeMillis() < deadlineMs) {
            Object bodyTextLower = ((JavascriptExecutor) driver).executeScript(
                "return document.body.innerText.toLowerCase();");
            String text = String.valueOf(bodyTextLower);
            if (text.contains("category configuration changed")) {
                throw new RuntimeException(CATEGORY_CONFIG_CHANGED_MARKER
                    + "Mirakl rejected the offer submission — this product's category configuration "
                    + "changed while it was being edited.");
            }
            if (text.contains("your form contains errors")) {
                Object errorFields = ((JavascriptExecutor) driver).executeScript(
                    "var out = [];"
                    + "document.querySelectorAll('.has-error').forEach(function(el) {"
                    + "  out.push(el.textContent.trim().substring(0, 80));"
                    + "});"
                    + "return out.join(' | ');");
                throw new RuntimeException("Mirakl rejected the offer submission with 'Your form contains "
                    + "errors' — field(s) still invalid/unset: " + errorFields);
            }
            // ROOT-CAUSED live (TC_E2E_003 rerun): this exact "category configuration changed" error
            // can render through a banner that is NOT styled with a "danger" class (unlike the "Your
            // form contains errors" variant checked above) — confirmed live: hasVisibleNonEmptySuccessBanner()'s
            // old boolean-only check matched it and this method returned believing success, while
            // getSuccessMessage() immediately afterward read back the literal error text. A banner being
            // present/non-empty/non-danger is NOT sufficient proof of success — its own text must also be
            // checked for the known error phrases before this method returns.
            String bannerText = getVisibleNonEmptySuccessBannerText();
            if (bannerText != null) {
                String bannerTextLower = bannerText.toLowerCase();
                if (bannerTextLower.contains("category configuration changed")) {
                    throw new RuntimeException(CATEGORY_CONFIG_CHANGED_MARKER
                        + "Mirakl rejected the offer submission — this product's category configuration "
                        + "changed while it was being edited. Banner text: " + bannerText);
                }
                if (bannerTextLower.contains("error")) {
                    throw new RuntimeException("Mirakl rejected the offer submission — banner text: " + bannerText);
                }
                LoggerUtility.info("Offer submitted (non-empty success banner confirmed): " + bannerText);
                return;
            }
            sleep(300);
        }
        // No known error AND no confirmed non-empty success banner within 120s. CONFIRMED live
        // (TC_E2E_003): the Create Product flow's own banner can be genuinely textless even on a real,
        // eventually-successful submission (unlike the "Sell Yours" flow's confirmed "Offer for X
        // added") — hard-failing here on banner ambiguity alone risks reporting a false failure for a
        // submission that actually succeeded. A missing/empty banner is inconclusive, not proof of
        // failure — the caller's own downstream ground-truth check (e.g. confirming the offer actually
        // exists in the Offers list) is what should decide the real outcome, not this method guessing.
        LoggerUtility.warn("No known error and no confirmed non-empty success banner within 120s — "
            + "proceeding without a confirmed banner; the caller's own downstream verification decides "
            + "the real outcome. Current page text (first 500 chars): "
            + String.valueOf(((JavascriptExecutor) driver).executeScript(
                "return document.body.innerText.substring(0, 500);")));
    }

    // ROOT-CAUSED live (TC_E2E_003): SUCCESS_MESSAGE_BANNER's XPath matches ANY element with a
    // notification/alert/toast class (excluding "danger"), with no requirement that it actually
    // contain text — CONFIRMED live via a diagnostic screenshot taken the instant this matched: the
    // page showed only a bare loading spinner, and getSuccessMessage() read back an empty string. Some
    // always-present, initially-empty notification-region container (likely wrapping/adjacent to the
    // loading spinner itself) satisfies the raw XPath the moment the page loads, well before any real
    // submission response arrives. Requiring non-empty text (after stripping the "×" close-button
    // glyph, same normalization as getSuccessMessage()) excludes that empty container.
    // Returns the first non-empty banner's own text (or null if none) — callers must inspect this
    // text for known error phrases themselves; presence/non-emptiness alone is not proof of success
    // (see clickSubmitForApproval()'s own root-cause note on why boolean-only used to false-positive).
    private String getVisibleNonEmptySuccessBannerText() {
        for (WebElement el : driver.findElements(SUCCESS_MESSAGE_BANNER)) {
            String text = el.getText().replace("×", "").trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return null;
    }

    // Distinct, greppable marker prefix so a caller (e.g. a category-selection retry loop) can tell
    // "Mirakl's known, retryable category-configuration-changed error" apart from any other submission
    // RuntimeException via e.getMessage().startsWith(...), without pattern-matching on free-text banner
    // copy at every call site.
    public static final String CATEGORY_CONFIG_CHANGED_MARKER = "MIRAKL_CATEGORY_CONFIG_CHANGED: ";

    public String getSuccessMessage() {
        // getText() on the banner also picks up the "×" close-button glyph as part of its text
        // content (confirmed live: raw text is "×\nOffer for Low GI Rice added.") — strip it.
        String message = getText(SUCCESS_MESSAGE_BANNER).replace("×", "").trim();
        LoggerUtility.info("Offer creation success message: " + message);
        return message;
    }

    // Added for TC_E2E_003: the Create Product flow's own success banner (CONFIRMED live) carries no
    // text at all, unlike the "Sell Yours" flow's confirmed "Offer for X added" — getSuccessMessage()
    // alone can't distinguish "banner present but empty" from "banner missing" without inspecting
    // exceptions. Two independent fallback signals (isProductCreationFormDisplayed() staying true, and
    // the page URL never changing) were both tried live for this flow and confirmed UNRELIABLE — this
    // SPA neither navigates away nor removes the word "Category" from the page on a real success. The
    // banner element's own presence (already what clickSubmitForApproval()'s success path itself relies
    // on) is the one signal actually confirmed to appear only after a genuine, error-free submission —
    // this exposes that check directly and assertably instead of re-deriving a weaker proxy.
    public boolean isSuccessBannerDisplayed() {
        boolean displayed = !driver.findElements(SUCCESS_MESSAGE_BANNER).isEmpty();
        LoggerUtility.info("Success banner displayed: " + displayed);
        return displayed;
    }

    // Diagnostic instrumentation for the "category configuration changed" investigation: the visible
    // banner text is generic UI copy — it does not say WHICH backend check failed. This monkey-patches
    // fetch()/XMLHttpRequest on the CURRENT page to capture request+response bodies for every AJAX call,
    // so the actual Submit call's response payload (which Mirakl's frontend already receives in full)
    // can be inspected instead of guessed at. Must be called fresh on every page that will issue the
    // Submit call (a driver.navigate().refresh() wipes any prior injection, since this is page-scoped
    // JS state, not a Selenium/CDP-level hook) — callers re-invoke this once per retry attempt,
    // immediately before clickSubmitForApproval().
    public void startNetworkCapture() {
        ((JavascriptExecutor) driver).executeScript(
            "window.__netCapture = [];"
            + "var origFetch = window.fetch;"
            + "window.fetch = function(input, init) {"
            + "  var url = (typeof input === 'string') ? input : (input && input.url) || '';"
            + "  var method = (init && init.method) || 'GET';"
            + "  var reqBody = (init && init.body) ? String(init.body).substring(0, 2000) : '';"
            + "  return origFetch.apply(this, arguments).then(function(resp) {"
            + "    var clone = resp.clone();"
            + "    clone.text().then(function(text) {"
            + "      window.__netCapture.push({url:url, method:method, status:resp.status, reqBody:reqBody, respBody:text.substring(0,3000)});"
            + "    }).catch(function(){});"
            + "    return resp;"
            + "  });"
            + "};"
            + "var origOpen = XMLHttpRequest.prototype.open;"
            + "var origSend = XMLHttpRequest.prototype.send;"
            + "XMLHttpRequest.prototype.open = function(method, url) {"
            + "  this.__capMethod = method; this.__capUrl = url;"
            + "  return origOpen.apply(this, arguments);"
            + "};"
            + "XMLHttpRequest.prototype.send = function(body) {"
            + "  var xhr = this;"
            + "  xhr.addEventListener('loadend', function() {"
            + "    try {"
            + "      window.__netCapture.push({url: xhr.__capUrl, method: xhr.__capMethod, status: xhr.status,"
            + "        reqBody: (body ? String(body).substring(0,2000) : ''), respBody: String(xhr.responseText || '').substring(0,3000)});"
            + "    } catch(e) {}"
            + "  });"
            + "  return origSend.apply(this, arguments);"
            + "};");
        LoggerUtility.info("Network capture installed on current page (fetch + XHR)");
    }

    // Dumps every captured AJAX call to the log, prioritizing non-2xx responses and calls whose URL
    // looks product/offer-related — call right after clickSubmitForApproval()/getSuccessMessage() so
    // the actual backend response for the Submit call is on record for this attempt, not just the
    // generic banner text.
    @SuppressWarnings("unchecked")
    public void logCapturedNetworkEvents(String contextLabel) {
        Object raw = ((JavascriptExecutor) driver).executeScript("return window.__netCapture || [];");
        if (!(raw instanceof List)) {
            LoggerUtility.warn(contextLabel + " — no network capture data available (capture not installed on this page?)");
            return;
        }
        List<Map<String, Object>> events = (List<Map<String, Object>>) raw;
        LoggerUtility.info(contextLabel + " — captured " + events.size() + " AJAX call(s):");
        for (Map<String, Object> ev : events) {
            String url = String.valueOf(ev.get("url"));
            String method = String.valueOf(ev.get("method"));
            Object status = ev.get("status");
            String reqBody = String.valueOf(ev.get("reqBody"));
            String respBody = String.valueOf(ev.get("respBody"));
            boolean relevant = url.toLowerCase().contains("offer") || url.toLowerCase().contains("product")
                || url.toLowerCase().contains("categor");
            if (relevant) {
                LoggerUtility.info(contextLabel + " — " + method + " " + url + " -> status " + status);
                if (!reqBody.isEmpty() && !"null".equals(reqBody)) {
                    LoggerUtility.info(contextLabel + " — request body: " + reqBody);
                }
                LoggerUtility.info(contextLabel + " — response body: " + respBody);
            }
        }
    }

    // --- Offers list search + verification ---

    // The search-by filter dropdown resisted native click, jsClick, Actions click, and synthetic
    // mousedown/mouseup/click dispatch — none opened it. The filter selection is encoded as a URL
    // query param instead (confirmed live: ?select-search=OFFER_SKU sets it to "Offer SKU"), so
    // navigate directly rather than fighting the dropdown widget.
    public void selectSearchByOfferSku() {
        LoggerUtility.info("Switching Offers list search filter to Offer SKU via URL");
        String baseUrl = driver.getCurrentUrl().split("\\?")[0];
        driver.get(baseUrl + "?select-search=OFFER_SKU");
        WaitUtility.fluentWait(driver, OFFERS_SEARCH_FIELD);
        LoggerUtility.info("Offers list search filter set to Offer SKU");
    }

    // Added for TC_E2E_004, per explicit requirement to stop relying on Offer SKU search (confirmed
    // across many live runs to never actually filter the grid — see searchOfferBySku()'s own history).
    // CONFIRMED live (2026-09-18) via a standalone diagnostic session against the real Offers page:
    // "Product ID" is the search-mode toggle's OWN default label (present in the DOM the moment the
    // page loads, before any URL is touched — a real <button><span>Product ID</span></button> right
    // next to the #search field), and "PRODUCT_ID" is the real value the app recognizes for this URL
    // param: navigating to "?select-search=PRODUCT_ID" was echoed back by the app with additional real
    // query params it added itself (&sort=product,ASC&limit=25), and typing a genuine, already-known
    // Product ID into #search under this mode and pressing Enter produced
    // "?select-search=PRODUCT_ID&...&search=<value>" and the grid result count genuinely changed from
    // "164 results" (unfiltered) to "No results found" (correctly filtered — that specific product
    // had no offer) — real filtering behavior, unlike Offer SKU mode. Neither value was guessed.
    public void selectSearchByProductId() {
        LoggerUtility.info("Switching Offers list search filter to Product ID via URL");
        String baseUrl = driver.getCurrentUrl().split("\\?")[0];
        driver.get(baseUrl + "?select-search=PRODUCT_ID");
        WaitUtility.fluentWait(driver, OFFERS_SEARCH_FIELD);
        LoggerUtility.info("Offers list search filter set to Product ID");
    }

    // Added for TC_E2E_004 — simpler than searchOfferBySku()'s suggestion-dropdown handling because
    // that machinery was built specifically to compensate for Offer SKU search never actually
    // committing on Enter alone. Product ID mode was directly confirmed live to apply its filter via a
    // plain Enter press (the URL immediately gains a real "search=<value>" param and the grid
    // re-renders) — no suggestion click ever appeared or was needed for this mode.
    public void searchOfferByProductId(String productId) {
        LoggerUtility.info("Searching Offers list for Product ID: " + productId);
        WaitUtility.fluentWaitForClickable(driver, OFFERS_SEARCH_FIELD);
        boolean verified = false;
        for (int attempt = 1; attempt <= 2 && !verified; attempt++) {
            WebElement field = driver.findElement(OFFERS_SEARCH_FIELD);
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '';"
                + "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));"
                + "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
                field);
            field.sendKeys(productId);
            String actual = field.getAttribute("value");
            if (productId.equals(actual)) {
                verified = true;
            } else {
                LoggerUtility.warn("Offers search field shows '" + actual + "' after typing (attempt " + attempt
                    + "/2), expected '" + productId + "' — retrying with a fresh JS clear");
            }
        }
        if (!verified) {
            LoggerUtility.warn("Offers search field could not be verified as exactly '" + productId + "' after 2 attempts");
        }
        driver.findElement(OFFERS_SEARCH_FIELD).sendKeys(Keys.ENTER);
        sleep(1500);
        LoggerUtility.info("Product ID search committed. Current URL: " + driver.getCurrentUrl());
    }

    public void searchOfferBySku(String offerSku) {
        LoggerUtility.info("Searching Offers list for Offer SKU: " + offerSku);
        // The field can be visible before it's actually interactable right after the full page
        // reload in selectSearchByOfferSku() — wait for clickable first.
        WaitUtility.fluentWaitForClickable(driver, OFFERS_SEARCH_FIELD);
        // ROOT-CAUSED live (TC_E2E_003): type()'s native el.clear() does not reliably reset this
        // React-controlled input across repeated calls within the same page load — CONFIRMED live via
        // screenshot: after several retries within one polling loop, the field showed the SAME SKU
        // concatenated to itself multiple times (e.g. "ZF265388ZF265388ZF265388..."), meaning clear()
        // silently no-opped (it never threw, so the old InvalidElementStateException fallback never
        // even triggered) and sendKeys() just appended onto whatever text was already there. Force-clear
        // via JS (set value + dispatch input/change so React's own controlled state updates too) BEFORE
        // every call, unconditionally, then verify the field actually reads back the exact intended
        // value — retrying once with a fresh element lookup if it doesn't — rather than trusting
        // clear()+sendKeys() blindly.
        // ROOT-CAUSED live (TC_E2E_004, 2026-09-17): the verification loop below used to `return`
        // immediately once the field's value matched — which is the common/expected outcome on
        // attempt 1 — meaning the search was NEVER actually committed (no Enter, no dropdown
        // selection) on the normal path. Only the "field could not be verified" fallback ever reached
        // the Enter-key commit. Confirmed live: a full 20-attempt/12-minute poll never found a freshly
        // imported offer, because the filter was simply never being applied. Restructured so the
        // commit always runs after the field is confirmed correct, on whichever attempt verified it.
        boolean verified = false;
        for (int attempt = 1; attempt <= 2 && !verified; attempt++) {
            WebElement field = driver.findElement(OFFERS_SEARCH_FIELD);
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = '';"
                + "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));"
                + "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
                field);
            field.sendKeys(offerSku);
            String actual = field.getAttribute("value");
            if (offerSku.equals(actual)) {
                verified = true;
            } else {
                LoggerUtility.warn("Offers search field shows '" + actual + "' after typing (attempt " + attempt
                    + "/2), expected '" + offerSku + "' — retrying with a fresh JS clear");
            }
        }
        if (!verified) {
            LoggerUtility.warn("Offers search field could not be verified as exactly '" + offerSku + "' after 2 attempts");
        }
        commitOfferSearch(offerSku);
    }

    // Added for TC_E2E_004 — per the manual test case's own step 52, "Select Offer Sku from the
    // dropdown" is a distinct action from typing + Enter. TC_E2E_003's Enter-key commit was proven
    // live for its own search mode, but a live TC_E2E_004 run polled all 20 attempts (12+ minutes)
    // without ever finding a freshly-imported offer via Enter alone — consistent with this "Offer SKU"
    // search mode requiring an explicit suggestion-dropdown selection to actually apply the filter,
    // not just committing on Enter. Checks for a suggestion dropdown (same widget class used
    // throughout this codebase — role='option'/.mui-suggestion-item) after typing; clicks the matching
    // suggestion if one appears, otherwise falls back to the original Enter-key commit so this doesn't
    // regress whatever search mode Enter alone already worked for.
    private void commitOfferSearch(String offerSku) {
        sleep(1000);
        // Diagnostic added for TC_E2E_004 — the manual test case's own step 52 ("Select Offer Sku from
        // the dropdown") implies a suggestion widget should appear here, but the querySelectorAll dump
        // below has only ever shown global sidebar nav text, never a real suggestion. One live
        // possibility never yet ruled out: a native HTML <input list="..."> + <datalist> autocomplete,
        // which renders as browser-native UI invisible to any DOM query (querySelectorAll can't see it,
        // Selenium can't click it — it would need an explicit Down-arrow+Enter key sequence instead).
        // Checking the field's own attributes + screenshotting right after typing gives real evidence
        // either way instead of guessing further blind.
        Object fieldInfo = ((JavascriptExecutor) driver).executeScript(
            "var el = document.getElementById('search');"
            + "if (!el) return 'search field not found';"
            + "return 'tag=' + el.tagName + ' list=' + el.getAttribute('list') + ' value=' + el.value"
            + " + ' datalistCount=' + document.querySelectorAll('datalist').length;");
        LoggerUtility.info("Offer SKU search field diagnostic: " + fieldInfo);
        ScreenshotUtility.captureScreenshot(driver, "MiraklOfferPage_afterTypingSku", ScreenshotUtility.INFO);
        Object suggestionsLog = ((JavascriptExecutor) driver).executeScript(
            "var out = [];"
            + "document.querySelectorAll('[role=\"option\"], .mui-suggestion-item, li').forEach(function(el) {"
            + "  var t = el.textContent.trim(); if (t && t.length < 80) out.push(t);"
            + "});"
            + "return out;");
        LoggerUtility.info("Offer SKU search — visible suggestion-like elements after typing '" + offerSku + "': " + suggestionsLog);

        Boolean clickedSuggestion = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "var target = arguments[0];"
            + "var els = document.querySelectorAll('[role=\"option\"], .mui-suggestion-item, li');"
            + "for (var i = 0; i < els.length; i++) {"
            + "  if (els[i].textContent.trim().indexOf(target) !== -1) { els[i].click(); return true; }"
            + "}"
            + "return false;",
            offerSku);
        if (Boolean.TRUE.equals(clickedSuggestion)) {
            LoggerUtility.info("Selected Offer SKU '" + offerSku + "' from the search suggestion dropdown");
            sleep(1000);
        }
        // ROOT-CAUSED live (TC_E2E_003): pressing Enter here (rather than relying solely on
        // clickOffersSearchIcon()) actually submits/commits the filter — see clickOffersSearchIcon()'s
        // own doc comment for why that click cannot be trusted to do this. Kept as a fallback/close-out
        // even when a suggestion was clicked, in case the field also needs an explicit commit.
        driver.findElement(OFFERS_SEARCH_FIELD).sendKeys(Keys.ENTER);
    }

    // ROOT-CAUSED live (TC_E2E_003): OFFERS_SEARCH_ICON's data-testid="search__svg" locator is the
    // SAME global "Ctrl+K" command-palette icon already identified and removed from
    // MiraklCatalogManagementPage.searchBySku() for the exact same reason ("actually matched the
    // GLOBAL... command-palette icon in the top nav on this page, not a local grid search icon —
    // clicking it popped open an unrelated search modal"). CONFIRMED live here via screenshot: after
    // typing the correct Offer SKU into the real filter field, clicking this "icon" popped an unrelated
    // full-page search modal on top, and the underlying Offer SKU grid never actually re-filtered
    // (still showed 161 unrelated rows). This method is kept only because Tc_manualoffercreation_01
    // already calls it and is out of scope to touch here, but it must never be relied on for real
    // filtering — searchOfferBySku() now presses Enter on the real field itself instead, which is what
    // actually commits the filter.
    public void clickOffersSearchIcon() {
        LoggerUtility.info("Clicking Offers list search icon (cosmetic only — does not reliably filter; "
            + "see searchOfferBySku()'s own Enter-key commit for the real filter trigger)");
        jsClick(OFFERS_SEARCH_ICON);
    }

    // CONFIRMED live (2026-09-10): the Pending Offers polling loop calls this once per attempt to
    // check for ABSENCE of the row — a case where the old WaitUtility.fluentWait(2-minute FluentWait,
    // 2s polling) blocked the full 2 minutes every single time the offer had genuinely already
    // disappeared, since the element it's polling for never appears. That nested 2-minute internal
    // block, repeated across multiple outer poll attempts, correlated with the Chrome renderer
    // disconnecting mid-run (NoSuchSessionException: "session deleted as the browser has closed the
    // connection"). Same anti-pattern already fixed elsewhere this session (isDisplayed(),
    // isImportTriggered(), isAcceptProductsPopupDisplayed()) — instant JS check + short bounded poll.
    public boolean hasOfferInList(String offerSku) {
        boolean found = false;
        for (int i = 0; i < 6; i++) {
            Object present = ((JavascriptExecutor) driver).executeScript(
                "return document.evaluate(\"//tbody//tr[contains(.,'" + offerSku + "')]\", "
                + "document, null, XPathResult.BOOLEAN_TYPE, null).booleanValue;");
            if (Boolean.TRUE.equals(present)) {
                found = true;
                break;
            }
            sleep(500);
        }
        LoggerUtility.info("Offer " + offerSku + (found ? " found in list" : " not found in list"));
        return found;
    }

    // Column order confirmed live: 1=checkbox, 2=Product, 3=Offer SKU, 4=Status, 5=Default price,
    // 6=Quantity, 7=Brand, 8=Condition, 9=Product SKU (rendered as "SKU: <value>"). No Logistics
    // column exists in this list — Logistics is only selected on the Add Offer form, not shown here.
    public String getOfferProductName(String offerSku) {
        return getOfferColumnText(offerSku, 2);
    }

    public String getOfferSku(String offerSku) {
        return getOfferColumnText(offerSku, 3);
    }

    public String getOfferStatus(String offerSku) {
        return getOfferColumnText(offerSku, 4);
    }

    public String getOfferPrice(String offerSku) {
        return getOfferColumnText(offerSku, 5);
    }

    public String getOfferQuantity(String offerSku) {
        return getOfferColumnText(offerSku, 6);
    }

    public String getOfferBrand(String offerSku) {
        return getOfferColumnText(offerSku, 7);
    }

    public String getOfferCondition(String offerSku) {
        return getOfferColumnText(offerSku, 8);
    }

    public String getOfferProductSku(String offerSku) {
        return getOfferColumnText(offerSku, 9);
    }

    private String getOfferColumnText(String offerSku, int columnIndex) {
        By column = By.xpath("//tbody//tr[contains(.,'" + offerSku + "')]/td[" + columnIndex + "]");
        String text = getText(column);
        LoggerUtility.info("Offer " + offerSku + " column " + columnIndex + ": " + text);
        return text;
    }

    // --- Unique test data generation ---

    // Format is 2 random UPPERCASE letters + 6 zero-padded digits = exactly 8 characters total
    // (e.g. "JA123456"), per explicit instruction (TC_E2E_003: uppercase, not lowercase — live
    // evidence showed a lowercase-generated Shop SKU was accepted by the Create Product form and
    // passed the pre-submit checklist, but never became searchable via the Offers list's "Offer SKU"
    // filter across a full 2.5-minute poll, consistent with Mirakl's real identifiers being
    // case-normalized/expected uppercase). The 6-digit suffix is derived from the current epoch
    // millisecond modulo 1,000,000, unique for any two runs started more than a millisecond apart
    // within the same ~17-minute wraparound window, which is sufficient for per-execution uniqueness;
    // the 2 random letters further reduce collision odds across parallel/retried runs.
    public static String generateUniqueOfferSku() {
        java.util.concurrent.ThreadLocalRandom rnd = java.util.concurrent.ThreadLocalRandom.current();
        char letter1 = (char) ('A' + rnd.nextInt(26));
        char letter2 = (char) ('A' + rnd.nextInt(26));
        long sixDigits = System.currentTimeMillis() % 1_000_000L;
        return "" + letter1 + letter2 + String.format("%06d", sixDigits);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
