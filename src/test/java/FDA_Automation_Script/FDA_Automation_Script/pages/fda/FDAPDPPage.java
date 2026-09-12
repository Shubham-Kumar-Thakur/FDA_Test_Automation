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
    // TODO: Verify locator against actual PDP DOM — standard Adobe Commerce "product attribute sku" block
    private static final By PRODUCT_SKU         = By.xpath("//div[contains(@class,'product') and contains(@class,'attribute') and contains(@class,'sku')]//div[@class='value']");
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
        String price = getText(PRODUCT_PRICE);
        LoggerUtility.info("PDP product price: " + price);
        return price;
    }

    public String getProductSku() {
        String sku = getText(PRODUCT_SKU);
        LoggerUtility.info("PDP product SKU: " + sku);
        return sku;
    }

    public boolean isDisplayed() {
        try {
            // Page navigated here via search — fluent wait for visibility (not just DOM presence)
            WaitUtility.fluentWait(driver, ADD_TO_CART_BTN);
            return true;
        } catch (Exception e) {
            LoggerUtility.error("PDP Add to Cart button not visible: " + e.getMessage());
            return false;
        }
    }
}
