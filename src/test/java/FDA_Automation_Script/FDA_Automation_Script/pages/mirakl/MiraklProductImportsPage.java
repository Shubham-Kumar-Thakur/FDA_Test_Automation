package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
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

    public MiraklProductImportsPage(WebDriver driver) {
        super(driver);
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
        // New/Pending/Published status grid. Never confirmed whether the Seller-visible variant behaves
        // the same way — check for the hub card with an instant, non-blocking presence check instead of
        // assuming either shape unconditionally, so this works for whichever variant is actually shown.
        boolean hubCardPresent = !driver.findElements(SHOP_PRODUCT_IMPORTS_CARD).isEmpty();
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

    public void clickAccept() {
        LoggerUtility.info("Clicking Accept");
        WaitUtility.fluentWait(driver, ACCEPT_BUTTON);
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
    }

    // Added for TC_E2E_003's exact-message verification requirement — same broad "any non-danger
    // notification/alert/toast" pattern already proven live for MiraklOfferPage.getSuccessMessage()
    // (that banner's exact copy differs per action, so the same detection strategy is reused rather
    // than a new locator family). Bounded JS poll (not isDisplayed()/findElement()) so a banner that
    // never appears fails fast instead of stalling on the 2-minute implicit wait.
    private static final By CONFIRMATION_BANNER = By.xpath(
        "//*[(contains(@class,'notification') or contains(@class,'alert') or contains(@class,'toast')) "
        + "and not(contains(@class,'danger'))]");

    public String getAcceptConfirmationMessage() {
        boolean appeared = false;
        for (int i = 0; i < 10; i++) {
            if (!driver.findElements(CONFIRMATION_BANNER).isEmpty()) {
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
}
