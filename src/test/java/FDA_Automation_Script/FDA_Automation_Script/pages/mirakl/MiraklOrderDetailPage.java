package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class MiraklOrderDetailPage extends BasePage {

    // TODO: Verify locators against actual Mirakl order detail DOM
    private static final By ORDER_STATUS_BADGE     = By.xpath("(//span[contains(@class,'_6e782__sc-s4ukbq-0') and not(ancestor::a)])[1]");
    private static final By ORDER_TOTAL_LABEL      = By.xpath("//span[text()='Total order amount']/../following-sibling::div/h3");
    private static final By ACCEPT_BUTTON          = By.xpath("//span[contains(text(),'Accept')] |//span[text()='Accept2']");
    // "3PL delivery" label in right sidebar, or carrier name field as fallback
    private static final By DELIVERY_PARTNER_LABEL = By.xpath(
        "//span[text()='3PL delivery']/../following-sibling::div/div/p | " +
        "//*[contains(text(),'Delivery partner')]/following-sibling::*[1] | " +
        "//*[normalize-space()='Carrier' or normalize-space()='Carrier name' or " +
        "normalize-space()='Transporteur' or normalize-space()='Shipping carrier']" +
        "/following-sibling::*[1]");

    // Delivery slip PDF filename — always present once Mirakl generates the slip
    // text format: "delivery-4000270061WEB-A.pdf - 23.17 kB"
    private static final By DELIVERY_SLIP_FILENAME = By.xpath(
        "//*[contains(text(),'delivery-') and contains(text(),'.pdf')]");

    // Carrier & tracking — may appear after 3PL processes the shipment label
    private static final By SHIPMENT_CARRIER_NAME  = By.xpath(
        "//*[normalize-space()='Carrier' or normalize-space()='Carrier name' or " +
        "normalize-space()='Transporteur' or normalize-space()='Shipping carrier']" +
        "/following-sibling::*[1] | " +
        "//*[normalize-space()='Carrier' or normalize-space()='Carrier name' or " +
        "normalize-space()='Transporteur']/parent::*[1]/following-sibling::*[1]");
    private static final By SHIPMENT_TRACKING_NUM  = By.xpath(
        "//*[normalize-space()='Tracking number' or normalize-space()='Tracking code' or " +
        "normalize-space()='Numéro de suivi' or normalize-space()='Tracking link']" +
        "/following-sibling::*[1] | " +
        "//*[normalize-space()='Tracking number' or normalize-space()='Tracking code' or " +
        "normalize-space()='Tracking link']/parent::*[1]/following-sibling::*[1]");

    private static final By DELIVERY_SLIP_LOADING  = By.xpath(
        "//*[contains(text(),'Delivery slip generation can take')]");
    // Ready when the delivery slip PDF filename is visible on the page
    private static final By DELIVERY_SLIP_READY    = By.xpath(
        "//*[contains(text(),'delivery-') and contains(text(),'.pdf')]");

    // Confirmed via live DOM inspection (2026-09-07): "More actions" is a real <button
    // data-testid="MORE_ACTIONS"> two levels below the label <span> — the label span is only
    // an ancestor wrapper, so jsClick() on the span-based locator dispatches a click whose
    // target never bubbles into the button's handler and the dropdown never opens. Selenium's
    // real click() (native mouse event, hit-tests actual screen coordinates) does work.
    private static final By MORE_ACTIONS_DROPDOWN   = By.xpath("//button[@data-testid='MORE_ACTIONS'] | //span[normalize-space()='More actions'] | //button[contains(.,'More actions')]");
    // contains() fallback (not just exact match) tolerates an icon/badge sharing the same
    // element as the label text; restricted to li/span/a/button (not div) to avoid matching
    // a large wrapping container whose concatenated text happens to include "Documents"
    private static final By DOCUMENTS_MENU_OPTION   = By.xpath(
        "//li[normalize-space()='Documents'] | //span[normalize-space()='Documents'] | " +
        "//*[self::li or self::span or self::a or self::button][contains(normalize-space(.),'Documents')]");
    // Confirmed via live DOM inspection (2026-09-07): the panel's real heading is "Accounting
    // documents", not "Order documents" — "Order documents" is a separate, empty heading elsewhere
    // on the page. Also, that heading text propagates up through ~21 ancestor divs (this app's
    // whole layout is one big nested div tree), so the old "//div[contains(.,'Order documents')]
    // //button[...]" locator was effectively unscoped. Anchor on the real section heading and take
    // the nearest ancestor div that also contains a button (ancestor:: is a reverse axis, so
    // position [1] is the closest ancestor, not the farthest). The panel has exactly two buttons —
    // "Add" and an unlabeled per-row "Action" button (data-testid="trailing-cell") — so the text
    // filter is required, not optional; an unfiltered //button risked resolving to either one.
    private static final By ADD_DOCUMENT_BUTTON     = By.xpath("//h2[normalize-space()='Accounting documents']/ancestor::div[.//button][1]//button[normalize-space()='Add']");
    private static final By UPLOAD_DOCUMENT_POPUP   = By.xpath("//div[@role='dialog'][.//*[contains(text(),'Upload an order document')]] | //*[contains(text(),'Upload an order document')]");
    // Confirmed via live DOM inspection (2026-09-07): this is a custom combobox
    // (role="combobox", id="type__trigger"), not a native <select> and not a div with "select"
    // in its class name (classes are hashed styled-components, e.g. "_6e782__sc-49km2g-0") — the
    // old locator matched neither branch and blocked for the full 2-minute implicit wait before
    // failing. Opening it renders a real <ul role="listbox"><li role="option">…</li></ul>, so the
    // existing option-selection xpath in selectDocumentType() below already works unchanged.
    private static final By DOCUMENT_TYPE_DROPDOWN  = By.id("type__trigger");
    private static final By DOCUMENT_FILE_INPUT     = By.xpath("//div[@role='dialog']//input[@type='file']");
    private static final By CONFIRM_UPLOAD_BUTTON   = By.xpath("//div[@role='dialog']//button[normalize-space()='Confirm']");
    private static final By DOCUMENT_UPLOADED_MSG   = By.xpath("//*[contains(text(),'document has been uploaded')]");

    private static final By ADD_TRACKING_LINK       = By.xpath("//a[contains(.,'Add tracking information')] | //span[contains(.,'Add tracking information')]");
    // Confirmed via live DOM inspection (2026-09-07, ZZ_Diag_MoreActions_Test screenshot
    // ZZ_DIAG2_00_ORDER_DETAIL.png): once tracking is added, the same link position reads
    // "View tracking information" and opens a dialog showing carrier + tracking number.
    private static final By VIEW_TRACKING_LINK      = By.xpath("//a[contains(.,'View tracking information')] | //span[contains(.,'View tracking information')]");
    private static final By TRACKING_INFO_DIALOG    = By.xpath("//div[@role='dialog']");
    private static final By TRACKING_DIALOG_CARRIER = By.xpath(
        "//div[@role='dialog']//*[normalize-space()='Carrier' or normalize-space()='Carrier name']/following-sibling::*[1] | " +
        "//div[@role='dialog']//*[normalize-space()='Carrier' or normalize-space()='Carrier name']/parent::*[1]/following-sibling::*[1]");
    private static final By TRACKING_DIALOG_NUMBER  = By.xpath(
        "//div[@role='dialog']//*[normalize-space()='Tracking number' or normalize-space()='Tracking code']/following-sibling::*[1] | " +
        "//div[@role='dialog']//*[normalize-space()='Tracking number' or normalize-space()='Tracking code']/parent::*[1]/following-sibling::*[1]");
    // Same Close-button shape confirmed on the Upload-document dialog (title="Close"
    // aria-label="Close") — Roma UI reuses this pattern for every dialog.
    private static final By TRACKING_DIALOG_CLOSE   = By.xpath("//div[@role='dialog']//button[@aria-label='Close' or @title='Close']");
    // NOT verified via live DOM inspection yet — modeled on the same custom combobox pattern
    // confirmed for Document type (role="combobox" trigger, id like "type__trigger", opening a
    // role="listbox"/role="option" list). Tries an id/name/aria-label containing "arrier" first
    // (matches that naming convention), then falls back to the original label-text-based guesses.
    // If this still doesn't match, selectCarrier() below dumps the live DOM so the real locator
    // can be fixed without another full ~7-minute run.
    private static final By CARRIER_DROPDOWN        = By.xpath(
        "//*[@role='combobox'][contains(translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'arrier') " +
        "or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'arrier') " +
        "or contains(translate(@aria-labelledby,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'arrier')] | " +
        "//label[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'carrier')]/following::*[@role='combobox'][1] | " +
        "//*[contains(text(),'Select a carrier')]/following::select[1] | //*[contains(text(),'Select a carrier')]/following::div[contains(@class,'select')][1]");
    private static final By TRACKING_NUMBER_INPUT   = By.xpath(
        "//label[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'tracking')]/following::input[1] | " +
        "//*[contains(text(),'Tracking number')]/following::input[1]");
    private static final By ADD_TRACKING_BUTTON     = By.xpath(
        "//div[@role='dialog']//button[normalize-space()='Add'] | " +
        "//div[contains(.,'Add tracking information')]//button[normalize-space()='Add']");

    // Confirmed via live DOM inspection (2026-09-07, ZZ_Diag screenshot ZZ_DIAG2_01_MORE_ACTIONS_OPEN.png):
    // the real button label is "Mark as shipped" (lowercase 's') — the previous locator's exact/contains
    // match used capital 'S' and could never match. translate() makes the match case-insensitive so a
    // future label-casing tweak on the app side doesn't silently break this again.
    private static final By MARK_AS_SHIPPED_BUTTON  = By.xpath(
        "//button[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'mark as shipped')]");
    private static final By CUSTOM_FIELD_BUTTON     = By.xpath("//span[normalize-space()='Custom field'] | //button[contains(.,'Custom field')]");
    private static final By ENTREGADO_OPTION        = By.xpath(
        "//li[normalize-space()='Entregado'] | //span[normalize-space()='Entregado'] | " +
        "//*[self::li or self::span or self::a or self::button][contains(normalize-space(.),'Entregado')]");
    // NOT verified via live DOM inspection (no diagnostic run has reached this step yet) — modeled
    // on the same custom combobox pattern confirmed for Document type (role="combobox" trigger
    // that opens a role="listbox"/role="option" list). If this dialog uses a different widget,
    // this locator will need a follow-up live-DOM pass, same as Document type and tracking did.
    private static final By ENTREGADO_VALUE_DROPDOWN = By.xpath("//div[@role='dialog']//*[@role='combobox']");
    private static final By ENTREGADO_YES_OPTION     = By.xpath(
        "//li[normalize-space()='Yes'] | //*[@role='option'][normalize-space()='Yes']");
    private static final By CUSTOM_FIELD_CONFIRM_BTN = By.xpath("//div[@role='dialog']//button[normalize-space()='Confirm']");

    public MiraklOrderDetailPage(WebDriver driver) {
        super(driver);
    }

    // BasePage.scrollIntoView() aligns the element's top edge to the viewport top
    // (scrollIntoView(true)), which slides it underneath Mirakl's fixed top navbar and causes
    // a real click() to hit whatever nav element occupies that space instead (confirmed:
    // "Search Everywhere" button intercepted a click meant for "More actions"). Center-aligning
    // avoids that without touching the shared BasePage helper used by every other page object.
    private void scrollToCenter(By locator) {
        org.openqa.selenium.WebElement el = findElement(locator);
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", el);
    }

    public String getOrderStatus() {
        String status = getText(ORDER_STATUS_BADGE);
        LoggerUtility.info("Mirakl Status = " + status);
        return status;
    }

    public String getOrderTotal() {
        String total = getText(ORDER_TOTAL_LABEL);
        LoggerUtility.info("Mirakl order total: " + total);
        return total;
    }

    public void clickAcceptButton() {
        LoggerUtility.info("Clicking Accept button on Mirakl order detail");
        scrollIntoView(ACCEPT_BUTTON);
        jsClick(ACCEPT_BUTTON);
        // Handle optional confirmation dialog (covers both single-order 'Accept' and split 'Accept2')
        By confirmInDialog = By.xpath(
            "//div[@role='dialog']//span[text()='Accept'] | " +
            "//div[@role='dialog']//span[text()='Accept2'] | " +
            "//div[@role='dialog']//button[normalize-space()='Accept'] | " +
            "//div[@role='dialog']//button[normalize-space()='Accept2'] | " +
            "//div[contains(@class,'modal')]//span[text()='Accept'] | " +
            "//div[contains(@class,'modal')]//span[text()='Accept2']");
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(5));
        try {
            if (!driver.findElements(confirmInDialog).isEmpty()) {
                driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
                LoggerUtility.info("Mirakl Accept confirmation dialog detected — confirming");
                jsClick(confirmInDialog);
            }
        } catch (Exception e) {
            LoggerUtility.info("No confirmation dialog detected");
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
        // Wait for the SPA to update the status after acceptance API call completes
        WaitUtility.fluentWaitForText(driver, ORDER_STATUS_BADGE, "Awaiting shipment");
        LoggerUtility.info("Mirakl order accepted — status updated to Awaiting shipment");
    }

    /**
     * Returns true if the Accept button is present within the given timeout.
     * Used to skip acceptance for auto-accepted split-order shipments.
     */
    public boolean isAcceptButtonPresent(int timeoutSeconds) {
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(timeoutSeconds));
        try {
            return !driver.findElements(ACCEPT_BUTTON).isEmpty();
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
    }

    public String getDeliveryPartner() {
        String partner = getText(DELIVERY_PARTNER_LABEL);
        LoggerUtility.info("Mirakl 3PL delivery: " + partner);
        return partner;
    }

    public boolean isShipmentDataAvailable() {
        // Wait up to 30s for the delivery slip PDF filename to appear on page
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(30));
        try {
            driver.findElement(DELIVERY_SLIP_READY);
            LoggerUtility.info("Shipment/delivery slip data is available in Mirakl");
            return true;
        } catch (Exception e) {
            LoggerUtility.info("Shipment data not yet available in Mirakl");
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
    }

    public void waitForShipmentData() {
        LoggerUtility.info("Waiting for shipment data to appear on Mirakl order detail");
        WaitUtility.fluentWait(driver, DELIVERY_SLIP_FILENAME);
    }

    /**
     * Parses the tplShipmentId from the delivery slip PDF filename.
     * Filename format: "delivery-4000270061WEB-A.pdf - 23.17 kB"
     * Returns:          "4000270061WEB-A"
     */
    public String getShipmentParcelRef() {
        scrollIntoView(DELIVERY_SLIP_FILENAME);
        String text = getText(DELIVERY_SLIP_FILENAME);
        LoggerUtility.info("Delivery slip filename text: " + text);
        // Extract the ref between "delivery-" and ".pdf"
        String ref = text.replaceAll(".*?delivery-(.+?)\\.pdf.*", "$1").trim();
        LoggerUtility.info("Mirakl shipment parcel ref (tplShipmentId): " + ref);
        return ref;
    }

    public String getShipmentCarrierName() {
        // Scroll to bottom to ensure all sections are visible
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        String carrier = getText(SHIPMENT_CARRIER_NAME);
        LoggerUtility.info("Mirakl shipment carrier: " + carrier);
        return carrier;
    }

    public String getShipmentTrackingNumber() {
        String tracking = getText(SHIPMENT_TRACKING_NUM);
        LoggerUtility.info("Mirakl shipment tracking number: " + tracking);
        return tracking;
    }

    /**
     * Checks the page source for each candidate SKU and returns the first match.
     * Used by TC_FBO_028 to identify which product belongs to a specific shipment,
     * so the correct productId is sent to the cancel-shipment API.
     */
    public String findProductSkuOnPage(String... candidateSkus) {
        String pageSource = driver.getPageSource();
        for (String sku : candidateSkus) {
            if (pageSource.contains(sku)) {
                LoggerUtility.info("Found product SKU on Mirakl shipment detail page: " + sku);
                return sku;
            }
        }
        LoggerUtility.warn("No candidate SKU found on Mirakl shipment detail page. Checked: "
                + java.util.Arrays.toString(candidateSkus));
        return "";
    }

    public void clickOrderInList(String orderId) {
        LoggerUtility.info("Clicking order in Mirakl list: " + orderId);
        By targetRow = By.xpath("//tbody/tr[contains(.,'" + orderId + "')]//a[1]");
        try {
            jsClick(targetRow);
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            LoggerUtility.warn("Stale on clickOrderInList, retrying");
            jsClick(targetRow);
        }
    }

    // ----------------------------------------------------------------
    // FBS flow: Documents / Tracking / Mark as Shipped / Custom field (TC_FBS_001)
    // ----------------------------------------------------------------

    public void clickMoreActionsDropdown() {
        LoggerUtility.info("Clicking More actions dropdown in Mirakl order detail");
        scrollToCenter(MORE_ACTIONS_DROPDOWN);
        // Real click required — see MORE_ACTIONS_DROPDOWN comment: jsClick does not open this menu.
        click(MORE_ACTIONS_DROPDOWN);
    }

    public void clickDocumentsOption() {
        LoggerUtility.info("Clicking Documents option under More actions");
        try {
            click(DOCUMENTS_MENU_OPTION);
        } catch (org.openqa.selenium.NoSuchElementException e) {
            logVisibleMenuItemTexts();
            throw e;
        }
    }

    // Diagnostic-only: dumps every visible leaf element's own direct text (not full descendant
    // text, so wrapping containers don't drown out the real labels) regardless of tag name —
    // this app's dropdown items are not necessarily <li>/role=menuitem, so a narrow diagnostic
    // locator can miss the real structure entirely (confirmed: it did, on the first attempt).
    private void logVisibleMenuItemTexts() {
        try {
            java.util.List<org.openqa.selenium.WebElement> candidates = driver.findElements(
                By.xpath("//*[string-length(normalize-space(text()))>0]"));
            StringBuilder sb = new StringBuilder("Visible leaf-text elements after opening More actions: ");
            int count = 0;
            for (org.openqa.selenium.WebElement el : candidates) {
                try {
                    if (el.isDisplayed()) {
                        String text = el.getText().trim();
                        if (!text.isEmpty() && text.length() < 60) {
                            sb.append("[").append(el.getTagName()).append(":").append(text).append("] ");
                            if (++count >= 100) { sb.append("...(truncated at 100)"); break; }
                        }
                    }
                } catch (Exception ignored) {
                    // stale between findElements() and isDisplayed() — skip
                }
            }
            LoggerUtility.warn(sb.toString());
        } catch (Exception ignored) {
            // best-effort diagnostic only — never let this mask the real exception
        }
    }

    public void clickAddDocumentButton() {
        LoggerUtility.info("Clicking Add button in Order documents section");
        scrollToCenter(ADD_DOCUMENT_BUTTON);
        // Confirmed via live DOM inspection (2026-09-07): right after Mirakl acceptance, this
        // button is present but genuinely disabled (native disabled="true") while the Accounting
        // documents panel finishes its async setup — Selenium's click() on a disabled element
        // silently no-ops (no exception), which is why the popup never appeared. Wait for it to
        // become enabled (elementToBeClickable checks isEnabled(), i.e. the disabled attribute)
        // before clicking, same pattern as the delivery-slip-generation wait used elsewhere.
        WaitUtility.fluentWaitForClickable(driver, ADD_DOCUMENT_BUTTON);
        click(ADD_DOCUMENT_BUTTON);
    }

    public boolean isUploadDocumentPopupDisplayed() {
        try {
            WaitUtility.fluentWaitForVisible(driver, UPLOAD_DOCUMENT_POPUP);
            LoggerUtility.info("Upload an order document popup displayed: true");
            return true;
        } catch (Exception e) {
            LoggerUtility.info("Upload an order document popup displayed: false");
            dumpAccountingDocumentsButtons();
            dumpAnyDialogOrLoadingState();
            return false;
        }
    }

    // Diagnostic-only: lists every button inside the "Accounting documents" panel (tag, text,
    // aria-label, data-testid) when the upload popup fails to appear, so the real Add/Upload
    // button can be identified without re-running the full checkout flow. Deliberately avoids
    // dumping raw outerHTML — the panel's empty-state illustration is a huge inline <svg> that
    // burns through any reasonable truncation budget before reaching the actual button markup.
    private void dumpAccountingDocumentsButtons() {
        try {
            By panel = By.xpath("//h2[normalize-space()='Accounting documents']/ancestor::div[.//button][1]");
            java.util.List<org.openqa.selenium.WebElement> panels = driver.findElements(panel);
            LoggerUtility.warn("Accounting documents panel matches: " + panels.size());
            if (panels.isEmpty()) return;
            java.util.List<org.openqa.selenium.WebElement> buttons = panels.get(0).findElements(By.xpath(".//button"));
            StringBuilder sb = new StringBuilder("Buttons in Accounting documents panel (" + buttons.size() + "): ");
            for (org.openqa.selenium.WebElement b : buttons) {
                try {
                    sb.append("[text='").append(b.getText().trim())
                      .append("' aria-label=").append(b.getAttribute("aria-label"))
                      .append(" data-testid=").append(b.getAttribute("data-testid"))
                      .append(" displayed=").append(b.isDisplayed()).append("] ");
                } catch (Exception ignored) {}
            }
            LoggerUtility.warn(sb.toString());
        } catch (Exception ignored) {
            // best-effort diagnostic only — never let this mask the real exception
        }
    }

    // Diagnostic-only: checks whether ANY dialog opened (regardless of expected title) and
    // whether a "delivery slip generation" loading message is present, so we can tell apart
    // "the click had no effect", "a different popup opened", and "the panel is still loading"
    // without re-running the full checkout flow.
    private void dumpAnyDialogOrLoadingState() {
        try {
            java.util.List<org.openqa.selenium.WebElement> dialogs = driver.findElements(By.xpath("//div[@role='dialog']"));
            LoggerUtility.warn("Any dialog present: " + dialogs.size());
            for (org.openqa.selenium.WebElement d : dialogs) {
                try {
                    String text = d.getText().trim();
                    if (text.length() > 300) text = text.substring(0, 300) + "...";
                    LoggerUtility.warn("Dialog text: '" + text + "'");
                } catch (Exception ignored) {}
            }
            boolean loading = !driver.findElements(DELIVERY_SLIP_LOADING).isEmpty();
            LoggerUtility.warn("Delivery slip loading message present: " + loading);
        } catch (Exception ignored) {
            // best-effort diagnostic only — never let this mask the real exception
        }
    }

    public void selectDocumentType(String documentType) {
        LoggerUtility.info("Selecting document type: " + documentType);
        click(DOCUMENT_TYPE_DROPDOWN);
        By option = By.xpath("//li[normalize-space()='" + documentType + "'] | //*[@role='option'][normalize-space()='" + documentType + "']");
        WaitUtility.fluentWaitForClickable(driver, option);
        click(option);
    }

    public void uploadDocumentFile(String absoluteFilePath) {
        LoggerUtility.info("Uploading document file: " + absoluteFilePath);
        findElement(DOCUMENT_FILE_INPUT).sendKeys(absoluteFilePath);
    }

    public void clickConfirmUploadButton() {
        LoggerUtility.info("Clicking Confirm button on upload document popup");
        click(CONFIRM_UPLOAD_BUTTON);
    }

    public boolean isDocumentUploadedMessageDisplayed() {
        try {
            WaitUtility.fluentWaitForVisible(driver, DOCUMENT_UPLOADED_MSG);
            LoggerUtility.info("Document upload confirmation message displayed: true");
            return true;
        } catch (Exception e) {
            LoggerUtility.info("Document upload confirmation message displayed: false");
            return false;
        }
    }

    /**
     * Returns true if an "Add tracking information" link is present within the given timeout.
     * Used by TC_FBS_013's return flow to poll the page after the Return API call creates a
     * return record — the browser's already-loaded order detail page doesn't reflect that new
     * record (and its own "Add tracking information" prompt) until refreshed, and the backend
     * needs a few seconds to propagate it even after a refresh.
     */
    public boolean isAddTrackingInformationLinkPresent(int timeoutSeconds) {
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(timeoutSeconds));
        try {
            return !driver.findElements(ADD_TRACKING_LINK).isEmpty();
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
    }

    public void clickAddTrackingInformationLink() {
        LoggerUtility.info("Clicking Add tracking information link");
        // Let the upload-confirmation modal/toast finish fading out before inspecting the page —
        // a screenshot taken immediately after Confirm showed the modal still mid fade-out,
        // ghosted on top of the real "Order documents" panel underneath.
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        try {
            scrollToCenter(ADD_TRACKING_LINK);
            click(ADD_TRACKING_LINK);
        } catch (org.openqa.selenium.NoSuchElementException e) {
            logVisibleMenuItemTexts();
            dumpElementsContainingIgnoreCase("track");
            dumpElementsContainingIgnoreCase("hip");
            dumpElementsContainingIgnoreCase("arrier");
            throw e;
        }
        // Give the tracking form time to render before capturing the confirmation screenshot.
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        ScreenshotUtility.captureScreenshot(driver, "TC_FBS_001_TRACKING_FORM_OPEN", ScreenshotUtility.INFO);
    }

    // Diagnostic-only: case-insensitive scan for any element whose own direct text contains the
    // given substring (e.g. "track" catches "Tracking"/"tracking"), logging tag/id/class so a
    // real locator can be identified without re-running the full checkout flow.
    private void dumpElementsContainingIgnoreCase(String substring) {
        try {
            String lower = substring.toLowerCase();
            By any = By.xpath("//*[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'"
                    + lower + "')]");
            java.util.List<org.openqa.selenium.WebElement> els = driver.findElements(any);
            StringBuilder sb = new StringBuilder("Elements containing '" + substring + "' (ignore-case), count="
                    + els.size() + ": ");
            int count = 0;
            for (org.openqa.selenium.WebElement el : els) {
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

    public void selectCarrier(String carrierName) {
        LoggerUtility.info("Selecting carrier: " + carrierName);
        try {
            click(CARRIER_DROPDOWN);
        } catch (org.openqa.selenium.NoSuchElementException e) {
            dumpElementsContainingIgnoreCase("arrier");
            dumpElementsContainingIgnoreCase("select");
            ScreenshotUtility.captureScreenshot(driver, "TC_FBS_001_CARRIER_DROPDOWN_NOT_FOUND", ScreenshotUtility.INFO);
            throw e;
        }
        By option = By.xpath("//li[normalize-space()='" + carrierName + "'] | //*[@role='option'][normalize-space()='" + carrierName + "']");
        WaitUtility.fluentWaitForClickable(driver, option);
        click(option);
    }

    public void enterTrackingNumber(String trackingNumber) {
        LoggerUtility.info("Entering tracking number: " + trackingNumber);
        try {
            type(TRACKING_NUMBER_INPUT, trackingNumber);
        } catch (org.openqa.selenium.NoSuchElementException e) {
            dumpElementsContainingIgnoreCase("tracking");
            ScreenshotUtility.captureScreenshot(driver, "TC_FBS_001_TRACKING_INPUT_NOT_FOUND", ScreenshotUtility.INFO);
            throw e;
        }
    }

    public void clickAddTrackingButton() {
        LoggerUtility.info("Clicking Add button to save tracking information");
        try {
            click(ADD_TRACKING_BUTTON);
        } catch (org.openqa.selenium.NoSuchElementException e) {
            logVisibleMenuItemTexts();
            ScreenshotUtility.captureScreenshot(driver, "TC_FBS_001_ADD_TRACKING_BUTTON_NOT_FOUND", ScreenshotUtility.INFO);
            throw e;
        }
    }

    public void clickViewTrackingInformationLink() {
        LoggerUtility.info("Clicking View tracking information link");
        scrollToCenter(VIEW_TRACKING_LINK);
        click(VIEW_TRACKING_LINK);
        WaitUtility.fluentWaitForVisible(driver, TRACKING_INFO_DIALOG);
    }

    public String getTrackingDialogCarrierName() {
        String carrier = firstLine(waitForNonEmptyText(TRACKING_DIALOG_CARRIER));
        LoggerUtility.info("Tracking dialog carrier name: " + carrier);
        return carrier;
    }

    // Confirmed via live DOM inspection (2026-09-07): the tracking-number field in this dialog
    // renders a verification-status badge ("Unverified"/"Verified") as a second line right under
    // the number, so a plain getText() returns "62098084\nUnverified" instead of just the number.
    // Take only the first line — the badge text is expected UI, not something to strip elsewhere.
    public String getTrackingDialogTrackingNumber() {
        String tracking = firstLine(waitForNonEmptyText(TRACKING_DIALOG_NUMBER));
        LoggerUtility.info("Tracking dialog tracking number: " + tracking);
        return tracking;
    }

    private String firstLine(String text) {
        return text.split("\\r?\\n", 2)[0].trim();
    }

    // Confirmed via live DOM inspection (2026-09-07, TC_FBS_003 failure screenshot): the
    // "Shipment tracking" side panel renders its shell/labels instantly when it opens, but the
    // Carrier/Tracking values populate a moment later — one run's getText() returned "" right
    // after the panel became visible, while the auto-captured failure screenshot taken a beat
    // later already showed "DHL" and the tracking number filled in. Poll for non-empty text
    // instead of trusting the panel's mere visibility (TRACKING_INFO_DIALOG wait in
    // clickViewTrackingInformationLink only confirms the shell, not the content).
    private String waitForNonEmptyText(By locator) {
        long deadline = System.currentTimeMillis() + 15_000;
        String text = "";
        while (System.currentTimeMillis() < deadline) {
            try {
                text = driver.findElement(locator).getText().trim();
                if (!text.isEmpty()) return text;
            } catch (Exception ignored) {
                // element momentarily stale/absent while the panel finishes rendering — keep polling
            }
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        }
        return text;
    }

    public void closeTrackingInfoDialog() {
        LoggerUtility.info("Closing tracking information dialog");
        click(TRACKING_DIALOG_CLOSE);
    }

    public void clickMarkAsShippedButton() {
        LoggerUtility.info("Clicking Mark as Shipped button");
        scrollToCenter(MARK_AS_SHIPPED_BUTTON);
        click(MARK_AS_SHIPPED_BUTTON);
    }

    public void clickCustomFieldButton() {
        LoggerUtility.info("Clicking Custom field button under More actions");
        click(CUSTOM_FIELD_BUTTON);
    }

    public void clickEntregadoOption() {
        LoggerUtility.info("Selecting Entregado option");
        try {
            click(ENTREGADO_OPTION);
        } catch (org.openqa.selenium.NoSuchElementException e) {
            logVisibleMenuItemTexts();
            throw e;
        }
    }

    public void clickEntregadoValueDropdown() {
        LoggerUtility.info("Clicking Entregado value dropdown");
        click(ENTREGADO_VALUE_DROPDOWN);
    }

    public void selectEntregadoYes() {
        LoggerUtility.info("Selecting 'Yes' for Entregado");
        WaitUtility.fluentWaitForClickable(driver, ENTREGADO_YES_OPTION);
        click(ENTREGADO_YES_OPTION);
    }

    public void clickCustomFieldConfirmButton() {
        LoggerUtility.info("Clicking Confirm button for custom field update");
        click(CUSTOM_FIELD_CONFIRM_BTN);
    }
}
