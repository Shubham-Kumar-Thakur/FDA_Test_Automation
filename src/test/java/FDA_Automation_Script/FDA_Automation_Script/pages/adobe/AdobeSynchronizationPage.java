package FDA_Automation_Script.FDA_Automation_Script.pages.adobe;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

// CONFIRMED live (2026-09-10): the real admin menu path is Mirakl (top-level, level-0) >
// "System" (a non-clickable <strong class="submenu-group-title"> group label, level-1) >
// "Synchronization" (the real <a href=".../mirakl/sync/index/..."> link, level-2). The
// "Synchronization" link is always present in the DOM (Magento's admin menu opens level-0/level-1
// submenus via CSS :hover, not click handlers) — no need to click through "Mirakl"/"System" first;
// clicking the "Synchronization" link directly via JS works regardless of hover/visibility state.
public class AdobeSynchronizationPage extends BasePage {

    private static final By SYNCHRONIZATION_LINK = By.xpath(
        "//a[.//span[normalize-space()='Synchronization']]");
    // CONFIRMED live (2026-09-10): this is a real <tr class="even"> row in a real grid table — the
    // row-name match was already correct. The button's real text is "Import in Magento" (not
    // "Import to Magento" as originally guessed) — kept as a fallback alongside the real text.
    private static final String MCM_ROW_XPATH =
        "//tr[contains(.,'MCM Products Asynchronous Import') and contains(.,'CM52-CM53-CM54')]";
    private static final By IMPORT_TO_MAGENTO_BUTTON = By.xpath(
        MCM_ROW_XPATH + "//button[contains(normalize-space(),'Import in Magento') or contains(normalize-space(),'Import to Magento')] | " +
        MCM_ROW_XPATH + "//a[contains(normalize-space(),'Import in Magento') or contains(normalize-space(),'Import to Magento')]");

    public AdobeSynchronizationPage(WebDriver driver) {
        super(driver);
    }

    public void navigateToSynchronization() {
        LoggerUtility.info("Navigating to Mirakl > System > Synchronization");
        jsClick(SYNCHRONIZATION_LINK);
        LoggerUtility.info("Synchronization page opened");
    }

    public void clickImportToMagento() {
        LoggerUtility.info("Clicking Import in Magento for MCM Products Asynchronous Import (CM52-CM53-CM54)");
        WaitUtility.fluentWait(driver, IMPORT_TO_MAGENTO_BUTTON);
        jsClick(IMPORT_TO_MAGENTO_BUTTON);
        LoggerUtility.info("Import in Magento clicked");
    }

    // CONFIRMED live (2026-09-10): this button queues a real Magento AsynchronousOperations bulk
    // job with NO inline confirmation at all — no success/toast message, no native browser alert,
    // and the grid row itself (Last Sync Date, button state) does not change immediately. The only
    // honest signal available right after the click is that no error occurred and we're still on
    // the Synchronization page (the click didn't throw, didn't navigate away, and didn't surface an
    // error-class message).
    public boolean isImportTriggered() {
        boolean onSyncPage = driver.getCurrentUrl().contains("mirakl/sync");
        // Instant JS DOM check (not isDisplayed()/findElement()) — the expected/good case is that
        // NO error element exists at all, which would otherwise block for the full 2-minute implicit
        // wait on every single run.
        Object errorFound = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(\"//*[contains(@class,'message-error') or contains(@class,'error')]"
            + "[contains(.,'import') or contains(.,'Import')]\", document, null, "
            + "XPathResult.BOOLEAN_TYPE, null).booleanValue;");
        boolean errorShown = Boolean.TRUE.equals(errorFound);
        boolean triggered = onSyncPage && !errorShown;
        LoggerUtility.info("Import triggered (still on Synchronization page, no error message): " + triggered);
        return triggered;
    }
}
