package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.List;

public class MiraklReturnPage extends BasePage {

    // TODO: Verify all locators against actual Mirakl return UI DOM

    // Step 68 — "Mark as received" button on order detail page (after return created via API)
    private static final By MARK_AS_RECEIVED_BTN = By.xpath(
        "//button[normalize-space()='Mark as received'] | " +
        "//span[normalize-space()='Mark as received']/ancestor::button[1] | " +
        "//*[@data-testid='mark-as-received'] | " +
        "//button[contains(normalize-space(),'Mark as received')]");

    // Step 69 — "Mark as received" button INSIDE the confirmation popup
    private static final By MARK_AS_RECEIVED_POPUP_BTN = By.xpath(
        "//div[@role='dialog']//button[normalize-space()='Mark as received'] | " +
        "//div[@role='dialog']//span[normalize-space()='Mark as received']/ancestor::button[1] | " +
        "//div[contains(@class,'modal')]//button[normalize-space()='Mark as received'] | " +
        "//div[contains(@class,'dialog')]//button[normalize-space()='Mark as received']");

    // Step 70 — "Check compliance" button
    private static final By CHECK_COMPLIANCE_BTN = By.xpath(
        "//button[normalize-space()='Check compliance'] | " +
        "//span[normalize-space()='Check compliance']/ancestor::button[1] | " +
        "//button[contains(normalize-space(),'Check compliance')]");

    // Step 71 — "Save" button
    private static final By SAVE_BTN = By.xpath(
        "//button[normalize-space()='Save'] | " +
        "//span[normalize-space()='Save']/ancestor::button[1] | " +
        "//button[@type='submit' and normalize-space()='Save']");

    // Step 72 — "Refund" dropdown trigger button (opens dropdown with Full refund option)
    private static final By REFUND_DROPDOWN_BTN = By.xpath(
        "//button[normalize-space()='Refund'] | " +
        "//span[normalize-space()='Refund']/ancestor::button[1] | " +
        "//button[contains(normalize-space(),'Refund') and not(contains(normalize-space(),'Full'))]");

    // Step 73 — "Full refund" item inside the opened dropdown
    private static final By FULL_REFUND_DROPDOWN_ITEM = By.xpath(
        "//li[normalize-space()='Full refund'] | " +
        "//li[contains(normalize-space(),'Full refund')] | " +
        "//span[normalize-space()='Full refund'] | " +
        "//div[contains(@class,'menu')]//span[contains(normalize-space(),'Full refund')] | " +
        "//*[contains(@class,'dropdown-item') and contains(normalize-space(),'Full refund')] | " +
        "//ul[contains(@class,'dropdown') or contains(@class,'menu')]//li[contains(.,'Full refund')]");

    // Step 74 — reason dropdown inside the Full refund dialog (Mirakl renders various patterns)
    private static final By SELECT_REASON_DROPDOWN = By.xpath(
        // native <select> inside a dialog/modal
        "//div[@role='dialog']//select | " +
        "//div[contains(@class,'modal') or contains(@class,'MuiDialog')]//select | " +
        // React-Select control inside a dialog
        "//div[@role='dialog']//*[contains(@class,'react-select__control')] | " +
        "//div[@role='dialog']//*[contains(@class,'Select-control')] | " +
        // role=combobox/listbox inside a dialog (Material-UI, Ant Design, etc.)
        "//div[@role='dialog']//*[@role='combobox' or @role='listbox'] | " +
        "//div[contains(@class,'modal')]//*[@role='combobox' or @role='listbox'] | " +
        // any native select not hidden (broad fallback — covers inline refund forms)
        "//select[not(@hidden) and not(@aria-hidden='true')] | " +
        // React-Select control anywhere (if no dialog wrapper)
        "//*[contains(@class,'react-select__control')] | " +
        // button or div labeled Select/Seleccionar
        "//button[normalize-space()='Select' or normalize-space()='Seleccionar'] | " +
        "//*[@role='combobox' and not(@aria-hidden='true')]");

    // Step 75 — "Item returned" option inside the OPEN reason dropdown.
    // Scope to role=listbox/option containers so we don't accidentally match stale page text.
    private static final By ITEM_RETURNED_OPTION = By.xpath(
        "//*[@role='listbox']//*[contains(normalize-space(),'Item returned')] | " +
        "//*[@role='option' and contains(normalize-space(),'Item returned')] | " +
        "//*[@data-value and contains(normalize-space(),'Item returned')] | " +
        "//li[normalize-space()='Item returned'] | " +
        "//li[contains(normalize-space(),'Item returned')] | " +
        "//option[normalize-space()='Item returned'] | " +
        "//option[contains(normalize-space(),'Item returned')]");

    // Step 76 — "Confirm" button inside the refund confirmation popup
    private static final By CONFIRM_REFUND_POPUP_BTN = By.xpath(
        "//div[@role='dialog']//button[normalize-space()='Confirm'] | " +
        "//div[@role='dialog']//span[normalize-space()='Confirm']/ancestor::button[1] | " +
        "//div[contains(@class,'modal')]//button[normalize-space()='Confirm'] | " +
        "//button[normalize-space()='Confirm']");

    public MiraklReturnPage(WebDriver driver) {
        super(driver);
    }

    // Step 68
    public void clickMarkAsReceived() {
        LoggerUtility.info("Step 68: Clicking Mark as received button");
        WaitUtility.fluentWait(driver, MARK_AS_RECEIVED_BTN);
        scrollIntoView(MARK_AS_RECEIVED_BTN);
        jsClick(MARK_AS_RECEIVED_BTN);
        LoggerUtility.info("Mark as received clicked");
    }

    // Step 69
    public void confirmMarkAsReceivedPopup() {
        LoggerUtility.info("Step 69: Clicking Mark as received on confirmation popup");
        WaitUtility.fluentWait(driver, MARK_AS_RECEIVED_POPUP_BTN);
        jsClick(MARK_AS_RECEIVED_POPUP_BTN);
        LoggerUtility.info("Mark as received popup confirmed");
    }

    // Step 70
    public void clickCheckCompliance() {
        LoggerUtility.info("Step 70: Clicking Check compliance button");
        WaitUtility.fluentWait(driver, CHECK_COMPLIANCE_BTN);
        scrollIntoView(CHECK_COMPLIANCE_BTN);
        jsClick(CHECK_COMPLIANCE_BTN);
        LoggerUtility.info("Check compliance clicked");
    }

    // Step 71
    public void clickSave() {
        LoggerUtility.info("Step 71: Clicking Save button");
        WaitUtility.fluentWait(driver, SAVE_BTN);
        scrollIntoView(SAVE_BTN);
        jsClick(SAVE_BTN);
        LoggerUtility.info("Save clicked");
    }

    // Step 72
    public void clickRefundDropdown() {
        LoggerUtility.info("Step 72: Clicking Refund dropdown button");
        WaitUtility.fluentWait(driver, REFUND_DROPDOWN_BTN);
        scrollIntoView(REFUND_DROPDOWN_BTN);
        jsClick(REFUND_DROPDOWN_BTN);
        LoggerUtility.info("Refund dropdown opened");
    }

    // Step 73
    public void selectFullRefundFromDropdown() {
        LoggerUtility.info("Step 73: Selecting Full refund from dropdown");
        WaitUtility.fluentWait(driver, FULL_REFUND_DROPDOWN_ITEM);
        scrollIntoView(FULL_REFUND_DROPDOWN_ITEM);
        // Standard click fires mousedown+mouseup+click — needed for React dropdown components
        try {
            click(FULL_REFUND_DROPDOWN_ITEM);
        } catch (Exception e) {
            LoggerUtility.warn("Standard click failed on Full refund — falling back to jsClick: " + e.getMessage());
            jsClick(FULL_REFUND_DROPDOWN_ITEM);
        }
        LoggerUtility.info("Full refund selected");
    }

    // Step 74 — click the reason dropdown that appears after "Full refund"
    public void clickSelectReasonDropdown() {
        LoggerUtility.info("Step 74: Clicking Select dropdown for reason");
        WaitUtility.fluentWait(driver, SELECT_REASON_DROPDOWN);
        scrollIntoView(SELECT_REASON_DROPDOWN);

        WebElement el = driver.findElement(SELECT_REASON_DROPDOWN);
        String tag = el.getTagName();
        LoggerUtility.info("Reason field tag: " + tag + ", class: " + el.getAttribute("class"));

        if ("select".equalsIgnoreCase(tag)) {
            // Native <select> — will be handled by selectItemReturned()
            LoggerUtility.info("Reason field is a native <select>");
        } else {
            // Custom dropdown — standard click fires real mouse events for React
            try {
                click(SELECT_REASON_DROPDOWN);
            } catch (Exception e) {
                LoggerUtility.warn("Standard click failed on reason dropdown — jsClick fallback: " + e.getMessage());
                jsClick(SELECT_REASON_DROPDOWN);
            }
            LoggerUtility.info("Reason dropdown clicked (custom component)");
        }
    }

    // Step 75 — pick "Item returned" from whichever control is open
    public void selectItemReturned() {
        LoggerUtility.info("Step 75: Selecting Item returned");

        // Native <select> path
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        List<WebElement> selects = driver.findElements(
            By.xpath("//select[contains(@name,'reason') or contains(@id,'reason')]"));
        driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));

        if (!selects.isEmpty()) {
            selectByPartialText(selects.get(0), "Item returned");
            LoggerUtility.info("Item returned selected from native <select>");
            return;
        }

        // Custom dropdown list item / span
        WaitUtility.fluentWait(driver, ITEM_RETURNED_OPTION);
        jsClick(ITEM_RETURNED_OPTION);
        LoggerUtility.info("Item returned selected from custom dropdown");
    }

    // Step 76 — "Confirm" inside the refund popup
    public void confirmRefundPopup() {
        LoggerUtility.info("Step 76: Clicking Confirm on refund popup");
        WaitUtility.fluentWait(driver, CONFIRM_REFUND_POPUP_BTN);
        jsClick(CONFIRM_REFUND_POPUP_BTN);
        LoggerUtility.info("Refund popup confirmed");
    }

    // Helper — select first <option> whose text contains partialText (case-insensitive)
    private void selectByPartialText(WebElement selectElement, String partialText) {
        Select select = new Select(selectElement);
        String lower = partialText.toLowerCase();
        for (WebElement option : select.getOptions()) {
            if (option.getText().toLowerCase().contains(lower)) {
                option.click();
                return;
            }
        }
        throw new RuntimeException("No <option> containing '" + partialText + "' found in <select>");
    }

    // ================================================================
    // TC_FBO_026 — Partial Refund flow
    // ================================================================

    // "Refund part of the order" option inside the opened Refund dropdown
    private static final By PARTIAL_REFUND_DROPDOWN_ITEM = By.xpath(
        "//span[normalize-space()='Refund part of the order']");

    // Reason code dropdown trigger inside the partial refund dialog
    private static final By PARTIAL_REFUND_REASON_TRIGGER = By.xpath(
        "//div[@id='orderLines.0.reasonCode__trigger']");

    // Quantity input field inside the partial refund dialog
    private static final By PARTIAL_REFUND_QUANTITY_INPUT = By.xpath(
        "//input[@id='orderLines.0.quantity'] | " +
        "//input[contains(@id,'quantity') and @type='number'] | " +
        "//input[contains(@name,'quantity') and @type='number'] | " +
        "//input[@type='number' and not(@hidden)] | //label[text()='Quantity']/../../../following-sibling::div[1]/div/div/input");

    // Span text node used to locate the "Refund order" button
    private static final By REFUND_ORDER_SPAN = By.xpath("//span[text()='Refund order']");
    // Ancestor button — React onClick lives here, not on the span
    private static final By REFUND_ORDER_BTN = By.xpath(
        "//span[text()='Refund order']/ancestor::button[1]");

    // Step 61 (TC_FBO_026) — select "Refund part of the order" from opened Refund dropdown
    public void selectPartialRefundFromDropdown() {
        LoggerUtility.info("Step 61: Selecting Refund part of the order from dropdown");
        WaitUtility.fluentWait(driver, PARTIAL_REFUND_DROPDOWN_ITEM);
        scrollIntoView(PARTIAL_REFUND_DROPDOWN_ITEM);
        try {
            click(PARTIAL_REFUND_DROPDOWN_ITEM);
        } catch (Exception e) {
            LoggerUtility.warn("Standard click failed on Refund part of order — jsClick: " + e.getMessage());
            jsClick(PARTIAL_REFUND_DROPDOWN_ITEM);
        }
        LoggerUtility.info("Refund part of the order selected");
    }

    // Step 62 (TC_FBO_026) — click the reason code dropdown trigger in the partial refund dialog.
    // scrollIntoView(true) places element at top (y≈19) behind sticky header.
    // Use block:'center' to move it to mid-viewport so standard click lands without interception.
    public void clickPartialRefundReasonDropdown() {
        LoggerUtility.info("Step 62: Clicking reason code dropdown trigger in partial refund dialog");
        WaitUtility.fluentWait(driver, PARTIAL_REFUND_REASON_TRIGGER);
        WebElement trigger = driver.findElement(PARTIAL_REFUND_REASON_TRIGGER);
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block:'center',inline:'nearest'});", trigger);
        try {
            click(PARTIAL_REFUND_REASON_TRIGGER);
        } catch (Exception e) {
            LoggerUtility.warn("Standard click failed on reason trigger — jsClick: " + e.getMessage());
            jsClick(PARTIAL_REFUND_REASON_TRIGGER);
        }
        LoggerUtility.info("Partial refund reason dropdown opened");
    }

    // Steps 64-65 (TC_FBO_026) — set refund quantity via JavaScript (field may be hidden/framework-managed)
    public void enterPartialRefundQuantity(int quantity) {
        LoggerUtility.info("Steps 64-65: Entering partial refund quantity: " + quantity);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        // Use React-compatible native value setter so onChange fires
        String script =
            "var input = document.getElementById('orderLines.0.quantity');" +
            "if (!input) input = document.querySelector('input[id*=\"quantity\"]');" +
            "if (input) {" +
            "  var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
            "  setter.call(input, '" + quantity + "');" +
            "  input.dispatchEvent(new Event('input', {bubbles:true}));" +
            "  input.dispatchEvent(new Event('change', {bubbles:true}));" +
            "  return 'ok';" +
            "} return 'not_found';";
        Object result = js.executeScript(script);
        LoggerUtility.info("Partial refund quantity JS set result: " + result + " | qty=" + quantity);
        if (!"ok".equals(result)) {
            LoggerUtility.warn("JS quantity set returned '" + result + "' — falling back to Selenium type");
            try {
                WaitUtility.fluentWait(driver, PARTIAL_REFUND_QUANTITY_INPUT);
                scrollIntoView(PARTIAL_REFUND_QUANTITY_INPUT);
                type(PARTIAL_REFUND_QUANTITY_INPUT, String.valueOf(quantity));
                LoggerUtility.info("Partial refund quantity entered via Selenium: " + quantity);
            } catch (Exception e) {
                LoggerUtility.warn("Selenium type also failed — quantity may be pre-filled: " + e.getMessage());
            }
        } else {
            LoggerUtility.info("Partial refund quantity set via JavaScript: " + quantity);
        }
    }

    // Step 66 (TC_FBO_026) — click "Refund order" to submit the partial refund.
    // Re-finds btn on every attempt: enterPartialRefundQuantity() triggers a React re-render
    // that replaces the DOM node, so any reference captured before the loop is stale by the
    // time we scroll/click. center-scroll keeps element out of sticky-header shadow.
    public void clickRefundOrderButton() {
        LoggerUtility.info("Step 66: Clicking Refund order button");
        WaitUtility.fluentWait(driver, REFUND_ORDER_SPAN);

        for (int attempt = 1; attempt <= 3; attempt++) {
            LoggerUtility.info("Refund order click attempt " + attempt + "/3");
            try {
                WebElement btn = driver.findElement(REFUND_ORDER_BTN);
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center',inline:'nearest'});", btn);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                LoggerUtility.info("jsClick fired on attempt " + attempt);
            } catch (Exception e) {
                LoggerUtility.warn("Click failed attempt " + attempt + ": " + e.getMessage());
            }
            // Probe for Confirm popup (5-sec window, restore 2-min global wait)
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(5));
            List<WebElement> popups = driver.findElements(CONFIRM_REFUND_POPUP_BTN);
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
            if (!popups.isEmpty()) {
                LoggerUtility.info("Confirm popup appeared after attempt " + attempt);
                return;
            }
            if (attempt < 3) {
                LoggerUtility.warn("Confirm popup not visible after attempt " + attempt + " — retrying");
            }
        }
        LoggerUtility.info("Refund order button click sequence complete");
    }
}
