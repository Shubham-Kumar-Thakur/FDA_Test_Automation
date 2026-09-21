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

    // BasePage.scrollIntoView() aligns the element's top edge to the viewport top
    // (scrollIntoView(true)), which slides it underneath Mirakl's fixed top navbar and causes a
    // real click() to hit whatever nav element occupies that space instead — same issue already
    // documented in MiraklOrderDetailPage.scrollToCenter() for the equivalent order-level kebab.
    // Duplicated here (rather than promoted to BasePage) to match that file's existing precedent
    // of keeping this scoped to pages that need a real click() on a top-nav-adjacent element.
    private void scrollToCenter(By locator) {
        WebElement el = findElement(locator);
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", el);
    }

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

    // TC_FBS_014 Step 98 — "Mark as closed" button in the "Return: Received" section header,
    // sitting directly next to the return-line kebab (confirmed via live UI screenshot,
    // 2026-09-18) — not behind any dropdown, same header row as MARK_AS_RECEIVED_BTN was in
    // while the return was still "In progress". Appears once the return has been marked
    // received, compliance-checked, and refunded. No confirmation popup — the page must be
    // refreshed afterward for the status change to render.
    //
    // CORRECTION (2026-09-18): a live run's log showed clickMarkAsClosed() completing with no
    // exception ("Mark as closed clicked") yet the return status stayed 'Return: Received' after
    // the wait+refresh. Root cause: an earlier revision led with the exact hashed styled-
    // components class captured from a live DOM screenshot
    // (@class='_6e782__sc-1j2t9k3-0 iViXAc _6e782__sc-7urc50-0 kPfhqn') — but that class is a
    // generic "primary button" style, not unique to this button (e.g. the "Refund" dropdown
    // trigger near the top of the page is styled the same way). For an XPath union (`a | b | c`),
    // Selenium returns matches in DOCUMENT order, not in the order the alternatives are written,
    // so leading with the class match did not make it take priority — whichever same-classed
    // button appears first in the DOM (almost certainly one nearer the top of the page, e.g.
    // Refund) is what actually got clicked, silently, with no error. Every OTHER action button in
    // this exact Return section (clickMarkAsReceived(), clickCheckCompliance(), clickSave(),
    // clickRefundDropdown()) uses a plain normalize-space() text match with jsClick() and worked
    // correctly in that same run, confirming that combination is reliable here — dropped the
    // ambiguous class-based match entirely and rely solely on the button's own (unique) text.
    private static final By MARK_AS_CLOSED_BTN = By.xpath(
        "//button[normalize-space()='Mark as closed'] | " +
        "//span[normalize-space()='Mark as closed']/ancestor::button[1] | " +
        "//button[contains(normalize-space(),'Mark as closed')]");

    // TC_FBS_014 Step 98 — "Mark as closed" button INSIDE the confirmation popup that appears
    // after the first click, same dialog-scoped pattern as MARK_AS_RECEIVED_POPUP_BTN above (per
    // direct instruction: clicking "Mark as closed" opens a popup that itself needs a second
    // "Mark as closed" click to confirm).
    private static final By MARK_AS_CLOSED_POPUP_BTN = By.xpath(
        "//div[@role='dialog']//button[normalize-space()='Mark as closed'] | " +
        "//div[@role='dialog']//span[normalize-space()='Mark as closed']/ancestor::button[1] | " +
        "//div[contains(@class,'modal')]//button[normalize-space()='Mark as closed'] | " +
        "//div[contains(@class,'dialog')]//button[normalize-space()='Mark as closed']");

    // TC_FBS_014 Step 99 — the "Return: <status>" heading (e.g. "Return: Received", "Return:
    // Closed") is a status on the RETURN itself, separate from the overall order/shipment badge
    // shown at the top of the page next to "Order no. ..." (MiraklOrderDetailPage.ORDER_STATUS_BADGE
    // / getOrderStatus()). Confirmed via live UI screenshot, 2026-09-18: after clicking "Mark as
    // closed" and refreshing, the top-of-page order badge stayed "Received" — only this return
    // section's own heading changed to "Return: Closed". Innermost-element XPath (excludes any
    // element that itself contains a descendant starting with "Return:") avoids matching a large
    // wrapping container instead of the actual heading text node.
    private static final By RETURN_STATUS_HEADING = By.xpath(
        "//*[starts-with(normalize-space(.),'Return:') and not(.//*[starts-with(normalize-space(.),'Return:')])]");

    // TC_FBS_014 Step 99
    public String getReturnStatusText() {
        String text = getText(RETURN_STATUS_HEADING).trim();
        LoggerUtility.info("Return section status: " + text);
        return text;
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

    // TC_FBS_014 Step 98 — click happens BEFORE the wait+refresh, not after (see the
    // TC_FBS_014_Test.java Step 98 comment for why this was corrected).
    public void clickMarkAsClosed() {
        LoggerUtility.info("Step 98: Clicking Mark as closed button");
        WaitUtility.fluentWait(driver, MARK_AS_CLOSED_BTN);
        scrollIntoView(MARK_AS_CLOSED_BTN);
        jsClick(MARK_AS_CLOSED_BTN);
        LoggerUtility.info("Mark as closed clicked");
    }

    // TC_FBS_014 Step 98 — confirm on the popup that appears after the first "Mark as closed"
    // click, same pattern as confirmMarkAsReceivedPopup() above.
    public void confirmMarkAsClosedPopup() {
        LoggerUtility.info("Step 98: Clicking Mark as closed on confirmation popup");
        WaitUtility.fluentWait(driver, MARK_AS_CLOSED_POPUP_BTN);
        jsClick(MARK_AS_CLOSED_POPUP_BTN);
        LoggerUtility.info("Mark as closed popup confirmed");
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

    // ================================================================
    // TC_FBS_013 — Return label upload flow ("Kebab" menu on the return shipment, between
    // "Add tracking information" and "Mark as received").
    //
    // Step 81 — CORRECTION (confirmed via live DOM inspection, 2026-09-16): this is NOT the same
    // order-level "More actions" dropdown used in Steps 46/65 (MORE_ACTIONS_DROPDOWN). That
    // earlier assumption was never actually verified against the DOM and was wrong — reusing
    // MiraklOrderDetailPage.clickMoreActionsDropdown() here left the return-label flow silently
    // fumbling through fallback/wait-timeout paths for the rest of the sequence (Steps 82-84 each
    // took 40s-2min instead of completing immediately) because the order-level dropdown never
    // contains an "Add return label" item — that item lives in this separate, return-line-specific
    // kebab button.
    //
    // A first attempt locked onto a button with id="id3" from one DOM snapshot; a subsequent run
    // showed that id is dynamically generated per page render (not stable), so it stopped matching
    // and the click silently no-oped (root cause turned out to be jsClick(), see below — the
    // sibling-based locator itself may have been fine). A second attempt used a global position
    // index among all title="More actions" buttons on the page ((//button[@title='More
    // actions'])[2]) — that clicked a real button, but confirmed via live screenshot (2026-09-16)
    // to be the WRONG one further down the page, proving the page has more than 2 such buttons and
    // a bare page-wide index isn't reliable. Corrected to combine both confirmed facts instead of
    // relying on either alone: it has title="More actions" AND it is the immediate sibling of the
    // "Mark as received" button in the "Return: In progress" section (confirmed via live
    // screenshot). Scoping by both together should be unambiguous regardless of how many other
    // "More actions" buttons exist elsewhere on the page.
    private static final By RETURN_LINE_MORE_ACTIONS_BTN = By.xpath(
        "//button[normalize-space()='Mark as received']/following-sibling::button[@title='More actions'][1] | " +
        "//button[normalize-space()='Mark as received']/parent::*/following-sibling::*//button[@title='More actions'][1] | " +
        "//button[normalize-space()='Mark as received']/ancestor::*[1]//button[@title='More actions'][1]");

    // Step 81
    public void clickReturnLineMoreActionsButton() {
        LoggerUtility.info("Step 81: Clicking return line's More actions (Kebab) button");
        WaitUtility.fluentWait(driver, RETURN_LINE_MORE_ACTIONS_BTN);
        scrollToCenter(RETURN_LINE_MORE_ACTIONS_BTN);
        // Real click required, same as MiraklOrderDetailPage.MORE_ACTIONS_DROPDOWN — this is the
        // same dropdown component, and jsClick() dispatches a click that never bubbles into its
        // real event handler, so the menu silently never opens even though no exception is thrown.
        click(RETURN_LINE_MORE_ACTIONS_BTN);
        LoggerUtility.info("Return line More actions button clicked");
    }
    // ================================================================

    // Step 82 — menu item revealed by the More actions ("Kebab") dropdown
    private static final By ADD_RETURN_LABEL_OPTION = By.xpath(
        "//li[normalize-space()='Add return label'] | //span[normalize-space()='Add return label'] | " +
        "//*[self::li or self::span or self::a or self::button][contains(normalize-space(.),'Add return label')]");

    // Steps 83-85 — CORRECTION (confirmed via live run, 2026-09-16): the "Add return label" panel
    // is NOT wrapped in a role="dialog" element, unlike the "Upload an order document" popup
    // (MiraklOrderDetailPage.DOCUMENT_FILE_INPUT etc., which does use role="dialog" and works
    // fine). A live run burned two full 2-minute implicit-wait timeouts (Select file button, then
    // the file input) because both locators required that non-existent role="dialog" ancestor
    // before matching anything else. Dropped the role="dialog" scoping page-wide; if this proves
    // too broad (matching an unrelated button/input elsewhere), the diagnostic dump in the catch
    // block of uploadReturnLabelFile() below will show what's actually on the page.
    private static final By SELECT_FILE_BUTTON = By.xpath(
        "//button[normalize-space()='Select file' or contains(normalize-space(),'Select file')]");

    // Step 84 — the hidden file input for the "Add return label" panel. Not scoped to role="dialog"
    // (see correction above) — takes the last matching file input on the page, since this panel's
    // input is expected to be the most recently rendered one relative to any earlier document-
    // upload input still lingering in the DOM.
    private static final By RETURN_LABEL_FILE_INPUT = By.xpath("(//input[@type='file'])[last()]");

    // Step 85 — "Add" submit button for the "Add return label" panel (see correction above)
    private static final By RETURN_LABEL_ADD_BUTTON = By.xpath("//button[normalize-space()='Add']");

    // Step 82 — CORRECTION (confirmed via live screenshot, 2026-09-16): a live run's screenshot,
    // taken immediately after this method returned, showed the kebab dropdown still fully OPEN
    // with "Add return label" visibly listed as an unclicked option — jsClick() never actually
    // activated the menu item, same root cause already fixed for the kebab button itself
    // (RETURN_LINE_MORE_ACTIONS_BTN/clickReturnLineMoreActionsButton()). Step 83's 120-second
    // "Select file" search then failed because no panel had ever opened, and polling against that
    // stuck open-menu overlay for two minutes is the likely trigger for the browser disconnecting
    // shortly after ("invalid session id: session deleted..."). Switched to a real click(), plus
    // scrollToCenter() (not scrollIntoView()) to avoid the fixed top navbar intercepting the click
    // — same fix pattern as the kebab button.
    public void clickAddReturnLabelOption() {
        LoggerUtility.info("Step 82: Clicking Add return label option");
        WaitUtility.fluentWait(driver, ADD_RETURN_LABEL_OPTION);
        scrollToCenter(ADD_RETURN_LABEL_OPTION);
        click(ADD_RETURN_LABEL_OPTION);
        LoggerUtility.info("Add return label option clicked");
    }

    // Step 83 — "Select file" is a label wrapping a hidden <input type="file">; a real click
    // opens the native OS file picker, which Selenium cannot drive or dismiss. As with document
    // upload elsewhere in this codebase (MiraklOrderDetailPage.uploadDocumentFile(), which never
    // clicks any visible trigger), the actual file selection happens via a direct sendKeys() on
    // the hidden input in uploadReturnLabelFile() below.
    //
    // CORRECTION (2026-09-16): this method used to actually perform a real click(SELECT_FILE_BUTTON)
    // — contradicting its own comment above — because every earlier live run had that click time
    // out and get swallowed by the try/catch before it could ever fire (the panel never opened due
    // to the Step 82 jsClick() bug, fixed separately). Once Step 82 was fixed and the panel started
    // opening for real, this click started actually succeeding — and a live run right after showed
    // uploadReturnLabelFile()'s sendKeys() no longer throwing, but isReturnLabelFileAttached() still
    // returning false after the full 2-minute wait, consistent with a native OS file-picker dialog
    // now sitting open on top of the browser and interfering with the DOM-level upload confirmation.
    // Removed the real click entirely — this step is intentionally a no-op/log-only, matching the
    // proven-safe uploadDocumentFile() pattern, so only sendKeys() ever touches the file input.
    public void clickSelectFileButton() {
        LoggerUtility.info("Step 83: Select file button — intentionally not clicked (would open an "
                + "unautomatable native OS file picker); actual selection happens via sendKeys() on "
                + "the hidden input in uploadReturnLabelFile()");
    }

    // Step 84
    public void uploadReturnLabelFile(String absoluteFilePath) {
        LoggerUtility.info("Step 84: Uploading return label file: " + absoluteFilePath);
        dumpAllFileInputs();
        try {
            findElement(RETURN_LABEL_FILE_INPUT).sendKeys(absoluteFilePath);
        } catch (org.openqa.selenium.NoSuchElementException e) {
            dumpElementsContainingIgnoreCase("return label");
            dumpElementsContainingIgnoreCase("select file");
            throw e;
        }
        // CORRECTION (2026-09-16): a live run showed the browser session dying (NoSuchSessionException,
        // "unable to send message to renderer") sometime during isReturnLabelFileAttached()'s ~90s
        // polling right after this sendKeys() — even with the risky real click on "Select file"
        // already removed, so the crash trigger is something in the widget's own file-processing
        // JS reacting to the injected file, not the click. A brief settle pause here gives that
        // initial JS handling (e.g. client-side PDF validation/preview generation) a moment to
        // finish before WebDriverWait starts hammering the page with repeated CDP calls, which may
        // reduce the chance of colliding with it — not a confirmed fix, just a mitigation.
        try { Thread.sleep(3000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    // Step 84 — verify the uploaded file's name is shown as attached (not scoped to role="dialog",
    // see correction above)
    public boolean isReturnLabelFileAttached(String fileName) {
        try {
            By attachedFile = By.xpath("//*[contains(text(),'" + fileName + "')]");
            WaitUtility.fluentWaitForVisible(driver, attachedFile);
            LoggerUtility.info("Return label file attached: true (" + fileName + ")");
            return true;
        } catch (Exception e) {
            LoggerUtility.info("Return label file attached: false (" + fileName + ")");
            // CORRECTION (2026-09-16): a live run showed these diagnostic dumps printing nothing at
            // all right before @AfterMethod hit "invalid session id" — meaning the browser had
            // already crashed by the time this catch block ran, and driver.findElements() inside
            // dumpElementsContainingIgnoreCase()/dumpAllFileInputs() threw NoSuchSessionException,
            // silently swallowed by their own try/catch(ignored). Check session liveness first so a
            // dead browser is reported clearly instead of producing an empty, misleading diagnostic.
            boolean sessionAlive = true;
            try {
                driver.getCurrentUrl();
            } catch (Exception sessionCheckEx) {
                sessionAlive = false;
                LoggerUtility.warn("Browser session appears to have died during the file-attachment wait: "
                        + sessionCheckEx.getClass().getSimpleName() + " — "
                        + (sessionCheckEx.getMessage() == null ? "" : sessionCheckEx.getMessage().split("\n")[0]));
            }
            if (sessionAlive) {
                // Diagnostic-only: the full filename (with extension) may not be how the panel
                // actually renders confirmation (truncated name, generic "1 file selected" message,
                // size-only display, etc.) — dump what's really on the page so the real match text
                // can be fixed without another full live run.
                String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
                dumpElementsContainingIgnoreCase(baseName);
                dumpElementsContainingIgnoreCase("pdf");
                dumpElementsContainingIgnoreCase("attach");
                dumpAllFileInputs();
            }
            return false;
        }
    }

    // Diagnostic-only: dumps every input[type=file] currently in the DOM (tag/id/class/visibility)
    // so a locator can be fixed without another full live run if RETURN_LABEL_FILE_INPUT ever
    // stops matching the right one.
    private void dumpAllFileInputs() {
        try {
            java.util.List<WebElement> inputs = driver.findElements(By.xpath("//input[@type='file']"));
            LoggerUtility.warn("input[type=file] count on page: " + inputs.size());
            for (WebElement el : inputs) {
                try {
                    LoggerUtility.warn("file input: id=" + el.getAttribute("id") + " class=" + el.getAttribute("class")
                            + " displayed=" + el.isDisplayed());
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {
            // best-effort diagnostic only — never let this mask the real exception
        }
    }

    // Diagnostic-only: case-insensitive scan for any element whose own direct text contains the
    // given substring, logging tag/id/class so a real locator can be identified without
    // re-running the full checkout flow. Mirrors MiraklOrderDetailPage.dumpElementsContainingIgnoreCase().
    private void dumpElementsContainingIgnoreCase(String substring) {
        try {
            String lower = substring.toLowerCase();
            By any = By.xpath("//*[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'"
                    + lower + "')]");
            List<WebElement> els = driver.findElements(any);
            StringBuilder sb = new StringBuilder("Elements containing '" + substring + "' (ignore-case), count="
                    + els.size() + ": ");
            int count = 0;
            for (WebElement el : els) {
                try {
                    String text = el.getText().trim();
                    if (text.length() > 60) text = text.substring(0, 60) + "...";
                    sb.append("[").append(el.getTagName()).append(" id=").append(el.getAttribute("id"))
                      .append(" text='").append(text).append("'] ");
                    if (++count >= 20) { sb.append("...(truncated)"); break; }
                } catch (Exception ignored) {}
            }
            LoggerUtility.warn(sb.toString());
        } catch (Exception ignored) {
            // best-effort diagnostic only — never let this mask the real exception
        }
    }

    // Step 85
    public void clickAddReturnLabelButton() {
        LoggerUtility.info("Step 85: Clicking Add button to submit return label");
        WaitUtility.fluentWait(driver, RETURN_LABEL_ADD_BUTTON);
        scrollIntoView(RETURN_LABEL_ADD_BUTTON);
        jsClick(RETURN_LABEL_ADD_BUTTON);
        LoggerUtility.info("Return label Add button clicked");
    }
}
