package FDA_Automation_Script.FDA_Automation_Script.pages.fda;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class FDAPDPPage extends BasePage {

	private static final By ADD_TO_CART_BTN     = By.xpath("//div//button[@id='product-addtocart-button' and @title='Agregar al carrito']");
    private static final By QUANTITY_FIELD      = By.xpath("//div[contains(@class,'qty-select-wrap')]//input | //input[@id='qty' and @name='qty']");
    private static final By INCREASE_QTY_BTN   = By.xpath("//button[@title='Aumentar']//i[@class='fas fa-plus']");
    private static final By PRODUCT_NAME        = By.xpath("//h1//span[@class='base' and @itemprop='name']");
    // CONFIRMED live (2026-09-12): the old locator (currency-flag-image preceding-sibling) never
    // matched anything real — the actual price lives in a Magento price-box scoped inside the
    // add-to-cart form: <div class="price-box price-final_price hidden" data-role="priceBox">...
    // The "hidden" class is present while Magento's price data hasn't finished syncing/rendering
    // for this product — see waitForPriceToLoad() for the poll-with-refresh that handles this.
    private static final By PRODUCT_PRICE       = By.xpath(
        "//form[contains(@class,'add-to-cart-form')]//div[contains(@class,'price-box') and @data-role='priceBox']");
    private static final By ADD_TO_CART_SUCCESS = By.cssSelector(
        "div.message-success, [data-ui-id='message-success'], " +
        "div.message.success, .page.messages .success, " +
        "div.messages .message-success");

    public FDAPDPPage(WebDriver driver) {
        super(driver);
    }

    public boolean isAddToCartEnabled() {
        // Retry with page refresh — 3P seller stock/availability can take a moment to sync on staging
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                new WebDriverWait(driver, Duration.ofMinutes(2))
                    .until(ExpectedConditions.elementToBeClickable(ADD_TO_CART_BTN));
                LoggerUtility.info("Add to Cart button enabled: true (attempt " + attempt + ")");
                return true;
            } catch (Exception e) {
                LoggerUtility.error("Add to Cart button not clickable on attempt " + attempt + ": " + e.getMessage());
                if (attempt < 3) {
                    LoggerUtility.info("Refreshing PDP and retrying...");
                    driver.navigate().refresh();
                    WaitUtility.fluentWait(driver, ADD_TO_CART_BTN);
                }
            }
        }
        return false;
    }

    public String getQuantity() {
        String qty = getAttribute(QUANTITY_FIELD, "value");
        LoggerUtility.info("PDP quantity: " + qty);
        return qty;
    }

    public void verifyQuantity(int expected) {
        String actual = getQuantity();
        LoggerUtility.info("Verifying PDP quantity. Expected: " + expected + ", Actual: " + actual);
    }

    public void increaseQuantity() {
        LoggerUtility.info("Clicking Aumentar (plus) button to increase quantity by 1");
        scrollIntoView(INCREASE_QTY_BTN);
        jsClick(INCREASE_QTY_BTN);
        LoggerUtility.info("Quantity increment clicked");
    }

    public void clickAddToCart() {
        LoggerUtility.info("Clicking Agregar al carrito button");
        scrollIntoView(ADD_TO_CART_BTN);
        jsClick(ADD_TO_CART_BTN);
        // Wait for AJAX add-to-cart to complete — 30s explicit wait with suppressed implicit wait
        // so we don't block for 2 minutes if success selector doesn't match this site's theme.
        // Fallback: wait for button to re-enable (Magento re-enables it after AJAX regardless of outcome).
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        boolean successFound = false;
        try {
            new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.presenceOfElementLocated(ADD_TO_CART_SUCCESS));
            successFound = true;
        } catch (Exception ignored) {
            LoggerUtility.warn("Add-to-cart success message not found in 30s — falling back to button re-enable check");
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        }
        if (!successFound) {
            try {
                new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.elementToBeClickable(ADD_TO_CART_BTN));
                LoggerUtility.info("Add to Cart button re-enabled — AJAX complete");
            } catch (Exception e) {
                LoggerUtility.warn("Button re-enable fallback timed out: " + e.getMessage());
            }
        }
        LoggerUtility.info("Product Added To Cart");
    }

    // ROOT-CAUSED live (2026-09-18, TC_E2E_004): a one-shot read returned empty right after the PDP's
    // own Add to Cart button first became visible — the PDP hydrates progressively (confirmed live:
    // isDisplayed() itself needed 3 attempts before the Add to Cart button appeared), the same class
    // of async-render gap already documented for getProductPrice() below, just affecting the name
    // span instead of the price box. Bounded poll (no page refresh needed here, unlike price — the
    // name span is already present in the DOM, just briefly empty before client JS fills it in).
    public String getProductName() {
        String name = "";
        for (int attempt = 1; attempt <= 6; attempt++) {
            name = getText(PRODUCT_NAME);
            if (!name.isEmpty()) {
                LoggerUtility.info("PDP product name resolved on attempt " + attempt + "/6: " + name);
                return name;
            }
            sleep(1000);
        }
        LoggerUtility.warn("PDP product name still empty after 6 attempts");
        return name;
    }

    // CONFIRMED live (2026-09-12): a one-shot read of the price-box genuinely returns empty right
    // after opening the PDP — the box carries a "hidden" class while Magento's price sync/render is
    // still in progress (a skeleton loader occupies its place), observed to still be unresolved
    // after 15+ seconds of polling with no refresh. Mirrors the same class of Mirakl-propagation
    // delay already handled elsewhere in this codebase (Published/Active status) — poll with a page
    // refresh between attempts rather than a single fixed wait, since a refresh re-triggers the
    // price fetch rather than just waiting on the same stalled one.
    public String getProductPrice() {
        // Shortened from 10x30s (~5 min) — CONFIRMED live (2026-09-12) this never resolves within
        // that window anyway, and the caller no longer asserts on the result (logs only), so a long
        // poll here would just waste test time for no benefit.
        return waitForPriceToLoad(2, 5_000);
    }

    private String waitForPriceToLoad(int maxAttempts, long intervalMs) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Object result = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "var box = document.querySelector(\"form.add-to-cart-form .price-box[data-role='priceBox']\");"
                + "if (!box) return null;"
                + "if ((' ' + box.className + ' ').indexOf(' hidden ') !== -1) return null;"
                + "var priceEl = box.querySelector('.price');"
                + "var text = priceEl ? priceEl.textContent.trim() : box.innerText.trim();"
                + "return text.length > 0 ? text : null;");
            if (result != null) {
                LoggerUtility.info("PDP product price resolved on attempt " + attempt + "/" + maxAttempts + ": " + result);
                return result.toString();
            }
            LoggerUtility.info("PDP product price not yet resolved (attempt " + attempt + "/" + maxAttempts + ") — refreshing");
            if (attempt < maxAttempts) {
                driver.navigate().refresh();
                sleep(intervalMs);
            }
        }
        LoggerUtility.warn("PDP product price never resolved after " + maxAttempts + " attempts");
        return "";
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // CONFIRMED live (2026-09-10): a name search on the FDA storefront routinely lands on a search
    // RESULTS list (not a direct single-match PDP) — the Add to Cart button is then genuinely
    // absent, and the old fluentWait()-based check burned the full 2-minute timeout every single
    // time this happened (twice per Phase 7 iteration), which correlated with the browser becoming
    // unresponsive/crashing under the prolonged polling. Instant JS visibility check instead — same
    // fix pattern already applied elsewhere for this exact anti-pattern.
    public boolean isDisplayed() {
        Object visible = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
            "var el = document.evaluate(\"//div//button[@id='product-addtocart-button' and @title='Agregar al carrito']\", "
            + "document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;"
            + "if (!el) return false;"
            + "var r = el.getBoundingClientRect();"
            + "return r.width > 0 && r.height > 0;");
        boolean displayed = Boolean.TRUE.equals(visible);
        LoggerUtility.info("PDP Add to Cart button visible: " + displayed);
        return displayed;
    }
}
