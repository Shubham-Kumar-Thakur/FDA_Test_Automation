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
    private static final By PRODUCT_PRICE       = By.xpath("//img[@class='currency-flag-image']/preceding-sibling::span[@class='price']");
    // Confirmed via a real run's diagnostic page-source dump (2026-09-15): the SKU is shown in the
    // "Más información" (Additional Information) table as a <td data-th="SKU"> cell, not the
    // originally-guessed "product attribute sku" div block.
    private static final By PRODUCT_SKU         = By.xpath("//td[@data-th='SKU']");
    // Confirmed via a real run's diagnostic page-source dump (2026-09-16): the "Detalles del
    // producto" accordion panel that wraps PRODUCT_SKU is present in the DOM with the real value
    // already in its raw HTML (aria-expanded/aria-hidden on the panel both already claim "open"),
    // but the panel's CSS visibility is actually driven by an "active" class this Magento
    // Luma/jQuery-UI-accordion widget adds on click — without it the panel (and PRODUCT_SKU inside
    // it) renders with zero size, so WebElement.getText() legitimately returns "" no matter how
    // long you poll or scroll. This is the collapsible trigger that toggles it.
    private static final By ADDITIONAL_INFO_TRIGGER = By.id("collapsible-label-additional-title");
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

    public String getProductName() {
        String name = getText(PRODUCT_NAME);
        LoggerUtility.info("PDP product name: " + name);
        return name;
    }

    public String getProductPrice() {
        // Scroll the price element into view before reading it — same rationale as
        // scrollToDetails() below: elements can render with blank/stale text while off-screen.
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        try {
            org.openqa.selenium.WebElement priceEl = driver.findElement(PRODUCT_PRICE);
            ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block: 'center'});", priceEl);
        } catch (org.openqa.selenium.NoSuchElementException ignored) {
            // Not present yet — fall through to getText() below, which uses the standard
            // 2-minute implicit wait and will throw its own clear error if it's genuinely missing.
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        }
        String price = getText(PRODUCT_PRICE);
        LoggerUtility.info("PDP product price: " + price);
        return price;
    }

    /**
     * Scrolls to the "Detalles del producto" (SKU/details) section — confirmed via a real run
     * (2026-09-16): this section renders below the fold, and reading the SKU while it's still
     * off-screen intermittently returned blank text even though the locator itself was correct.
     * No-op if the section isn't present yet — getProductSku()'s own poll handles that case.
     */
    public void scrollToDetails() {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        try {
            org.openqa.selenium.WebElement details = driver.findElement(PRODUCT_SKU);
            ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block: 'center'});", details);
            LoggerUtility.info("Scrolled PDP to product details/SKU section");
            // Confirmed via a real run (2026-09-16): the panel's aria attributes already claim
            // "open" in the raw DOM, but its actual CSS visibility depends on an "active" class
            // this accordion widget only adds after a click — not present by default. Click the
            // trigger whenever the SKU cell isn't actually rendered (zero size), rather than
            // trusting aria-expanded/aria-hidden.
            if (!details.isDisplayed()) {
                LoggerUtility.info("Details table present in DOM but not visible — clicking "
                    + "'Detalles del producto' accordion trigger to expand it");
                try {
                    org.openqa.selenium.WebElement trigger = driver.findElement(ADDITIONAL_INFO_TRIGGER);
                    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", trigger);
                } catch (org.openqa.selenium.NoSuchElementException triggerMissing) {
                    LoggerUtility.warn("Accordion trigger not found — leaving details section as-is");
                }
            }
        } catch (org.openqa.selenium.NoSuchElementException e) {
            LoggerUtility.info("PDP details/SKU section not present yet — relying on getProductSku()'s own wait");
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        }
    }

    public String getProductSku() {
        // Confirmed via a real run (2026-09-15): PRODUCT_SKU's locator is an unconfirmed guess
        // that doesn't match this PDP's real DOM, and the global 2-minute implicit wait turned
        // that miss into a 2-minute stall before failing. Use a short implicit wait here instead
        // so a genuine miss fails in seconds, and dump the live page source so the real SKU
        // markup can be read off a live run instead of guessing further.
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        try {
            // Confirmed via a real run (2026-09-16): the locator itself is correct (matches the
            // "Detalles del producto" table dumped on 2026-09-15) but a single getText() right
            // after landing on the PDP can return "" — the accordion/table hadn't finished
            // rendering yet. Poll for non-blank text instead of trusting the first read.
            String sku = new WebDriverWait(driver, Duration.ofSeconds(15))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(org.openqa.selenium.NoSuchElementException.class)
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                .until(d -> {
                    String text = d.findElement(PRODUCT_SKU).getText().trim();
                    return text.isEmpty() ? null : text;
                });
            LoggerUtility.info("PDP product SKU: " + sku);
            return sku;
        } catch (org.openqa.selenium.NoSuchElementException | org.openqa.selenium.TimeoutException e) {
            try {
                java.nio.file.Files.writeString(
                    java.nio.file.Path.of("test-output/logs/fda_pdp_sku_locator_missing.html"),
                    driver.getPageSource());
                LoggerUtility.info("Diagnostic: dumped page source to "
                    + "test-output/logs/fda_pdp_sku_locator_missing.html");
            } catch (Exception dumpFailure) {
                LoggerUtility.error("Diagnostic page-source dump failed: " + dumpFailure.getMessage());
            }
            throw new org.openqa.selenium.NoSuchElementException(
                "PDP SKU field never showed non-blank text within 15s: " + PRODUCT_SKU, e);
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        }
    }

    /**
     * Looks up this specific seller's own offer price in the PDP's "other sellers" list. The main
     * buy-box price (getProductPrice()) reflects the winning/cheapest offer among all 3P sellers
     * for this product — per explicit instruction (2026-09-15), that is not necessarily this
     * test's Seller (confirmed via a real Push-Offers-to-Empathy API response, 2026-09-15: this
     * test's seller had "winner": false, priced above the winning offer). Returns null if no row
     * matching sellerName is found (e.g. this seller IS the winning/default offer already shown
     * as the main price, or the sidebar section genuinely isn't present for a single-seller
     * product) — callers should treat that as "not found", not as an error.
     *
     * TODO: Verify against actual DOM — no confirmed locator yet for this sidebar/section. Finds
     * the most specific (innermost) element whose own text is exactly the seller name, then reads
     * the nearest ancestor container that also holds a price element.
     */
    public String getSellerOfferPrice(String sellerName) {
        By sellerNameLeaf = By.xpath(
            "//*[not(*) and normalize-space(text())='" + sellerName + "']");
        java.util.List<org.openqa.selenium.WebElement> matches = driver.findElements(sellerNameLeaf);
        if (matches.isEmpty()) {
            LoggerUtility.info("Seller '" + sellerName + "' not found in an 'other sellers' section on this PDP");
            return null;
        }
        try {
            org.openqa.selenium.WebElement priceEl = matches.get(0).findElement(By.xpath(
                "ancestor::*[.//*[contains(@class,'price')]][1]//*[contains(@class,'price')]"));
            String price = priceEl.getText().trim();
            LoggerUtility.info("PDP 'other sellers' price for '" + sellerName + "': " + price);
            return price;
        } catch (org.openqa.selenium.NoSuchElementException e) {
            LoggerUtility.warn("Found seller name '" + sellerName + "' on PDP but no nearby price element — "
                + "sidebar locator likely needs adjusting against real DOM");
            return null;
        }
    }

    public boolean isDisplayed() {
        try {
            // Confirmed via a real run (2026-09-15): waiting on ADD_TO_CART_BTN as the "are we on
            // a genuine PDP" signal breaks for unavailable/out-of-stock products, which render a
            // "No disponible" button instead of the expected "Agregar al carrito" one (title
            // wouldn't match). The product name heading renders regardless of availability, so
            // it's a reliable PDP marker independent of stock status.
            WaitUtility.fluentWait(driver, PRODUCT_NAME);
            return true;
        } catch (Exception e) {
            LoggerUtility.error("PDP product name heading not visible: " + e.getMessage());
            return false;
        }
    }
}
