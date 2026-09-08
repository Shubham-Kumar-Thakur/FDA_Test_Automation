package FDA_Automation_Script.FDA_Automation_Script.pages.fda;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.ArrayList;
import java.util.List;

public class FDACartPage extends BasePage {

    // TODO: Verify locators against actual cart page DOM
    private static final By PRODUCT_NAME_LABEL      = By.cssSelector("table.cart.items td.col.item .product-item-name a, .cart.item td.col.item strong.product-item-name a");
    private static final By QUANTITY_INPUT           = By.cssSelector(".cart.item input[name='cart[*][qty]'], input[data-role='cart-item-qty']");
    private static final By ORDER_TOTAL_LABEL        = By.cssSelector(".grand.totals .price, .totals.grand .price");
    private static final By PROCEED_TO_PAYMENT_BTN  = By.cssSelector("button.checkout, .action.primary.checkout, [data-role='proceed-to-checkout']");

    public FDACartPage(WebDriver driver) {
        super(driver);
    }

    public String getProductName() {
        String name = getText(PRODUCT_NAME_LABEL);
        LoggerUtility.info("Cart product name: " + name);
        return name;
    }

    public String getQuantity() {
        String qty = getAttribute(QUANTITY_INPUT, "value");
        LoggerUtility.info("Cart quantity: " + qty);
        return qty;
    }

    public String getOrderTotal() {
        String total = getText(ORDER_TOTAL_LABEL);
        LoggerUtility.info("Cart order total: " + total);
        return total;
    }

    public void clickProceedToPayment() {
        LoggerUtility.info("Clicking Proceed to payment button");
        click(PROCEED_TO_PAYMENT_BTN);
    }

    public boolean isDisplayed() {
        return isDisplayed(ORDER_TOTAL_LABEL);
    }

    public void removeAllItems() {
        By vaciarBtn = By.xpath("//a[contains(.,'Vaciar carrito')] | //a[contains(@class,'clear')] | //a[@id='empty_cart_button']");
        By emptyMsg  = By.xpath("//*[contains(.,'No tienes artículos')]");

        // Refresh to bypass Magento FPC — ensures real server-side cart data is rendered
        driver.navigate().refresh();

        // Disable implicit wait so the WebDriverWait timeout below is not dominated by 2-min implicit wait
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
        boolean hasItems;
        try {
            // Race "has items" against "already empty" so an empty cart resolves in ~15s,
            // not the full 60s it used to block waiting for a Vaciar carrito button that never appears
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.or(
                    org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(vaciarBtn),
                    org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(emptyMsg)));
            hasItems = !driver.findElements(vaciarBtn).isEmpty();
        } catch (Exception e) {
            hasItems = false;
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }

        if (!hasItems) {
            LoggerUtility.info("Cart is empty");
            return;
        }

        LoggerUtility.info("Clearing cart via Vaciar carrito");
        jsClick(vaciarBtn);

        // Confirmation may be a native JS alert or a Magento HTML modal
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.alertIsPresent());
            driver.switchTo().alert().accept();
            LoggerUtility.info("Accepted native browser confirm dialog");
        } catch (Exception noAlert) {
            // Not a native alert — look for Magento HTML modal confirm button
            By confirmOkBtn = By.xpath(
                "//button[normalize-space(.)='Ok'] | //button[normalize-space(.)='OK'] | " +
                "//button[normalize-space(.)='Aceptar'] | //button[normalize-space(.)='Confirmar'] | " +
                "//button[contains(@class,'action-accept')] | //button[contains(@class,'confirm')]");
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
            try {
                new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
                    .until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(confirmOkBtn));
                jsClick(confirmOkBtn);
                LoggerUtility.info("Clicked HTML confirm button");
            } catch (Exception noBtn) {
                LoggerUtility.warn("No confirmation dialog found — cart may clear without confirmation");
            } finally {
                driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
            }
        }

        // Wait up to 30s for cart to be cleared by Vaciar carrito
        boolean cleared = false;
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(30))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(emptyMsg));
            cleared = true;
        } catch (Exception e) {
            LoggerUtility.warn("Vaciar carrito did not clear cart in 30s — removing items individually");
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }

        if (!cleared) {
            By deleteBtn = By.xpath(
                "//a[@data-role='remove-item'] | " +
                "//a[contains(@class,'action-delete')] | " +
                "//button[contains(@class,'action-delete')]");
            driver.navigate().refresh();
            for (int i = 0; i < 10; i++) {
                driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
                java.util.List<WebElement> btns = driver.findElements(deleteBtn);
                driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
                if (btns.isEmpty()) break;
                LoggerUtility.info("Individual delete: removing item " + (i + 1));
                ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();", btns.get(0));
                driver.navigate().refresh();
                WaitUtility.fluentWait(driver, By.tagName("body"));
            }
            LoggerUtility.info("Individual item removal complete");
        }

        LoggerUtility.info("Cart cleared");
    }

    public void normalizeQuantitiesToOne() {
        By qtySelector = By.cssSelector("input[data-role='cart-item-qty']");
        By updateBtn   = By.xpath(
            "//button[@title='Update Shopping Cart'] | " +
            "//button[normalize-space(.)='Actualizar carrito'] | " +
            "//button[contains(@class,'update')][@type='submit']");

        java.util.List<org.openqa.selenium.WebElement> inputs = driver.findElements(qtySelector);
        boolean needsUpdate = false;
        for (org.openqa.selenium.WebElement inp : inputs) {
            String val = inp.getAttribute("value");
            if (val != null && !"1".equals(val.trim())) {
                LoggerUtility.warn("Cart qty=" + val + " — normalizing to 1");
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "arguments[0].value='1';" +
                    "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));" +
                    "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                    inp);
                needsUpdate = true;
            }
        }
        if (needsUpdate) {
            try {
                jsClick(updateBtn);
            } catch (Exception e) {
                LoggerUtility.warn("Update cart button not found, pressing Enter on last qty input");
                java.util.List<org.openqa.selenium.WebElement> refreshed = driver.findElements(qtySelector);
                if (!refreshed.isEmpty()) {
                    refreshed.get(refreshed.size() - 1).sendKeys(org.openqa.selenium.Keys.RETURN);
                }
            }
            WaitUtility.fluentWait(driver, qtySelector);
            LoggerUtility.info("Cart quantities normalized to 1");
        }
    }

    public int getItemCount() {
        // Count by qty input (one per row, more reliable than product name label)
        List<WebElement> items = driver.findElements(QUANTITY_INPUT);
        if (items.isEmpty()) {
            items = driver.findElements(PRODUCT_NAME_LABEL);
        }
        LoggerUtility.info("Cart item count: " + items.size());
        return items.size();
    }

    public List<String> getAllQuantities() {
        List<WebElement> qtyInputs = driver.findElements(QUANTITY_INPUT);
        List<String> quantities = new ArrayList<>();
        for (WebElement input : qtyInputs) {
            String val = input.getAttribute("value");
            quantities.add(val != null ? val.trim() : "");
        }
        LoggerUtility.info("Cart all quantities: " + quantities);
        return quantities;
    }

    public List<String> getAllProductNames() {
        List<WebElement> nameElements = driver.findElements(PRODUCT_NAME_LABEL);
        List<String> names = new ArrayList<>();
        for (WebElement el : nameElements) {
            String text = el.getText().trim();
            if (!text.isEmpty()) names.add(text);
        }
        LoggerUtility.info("Cart all product names: " + names);
        return names;
    }

    public void openAndRefreshCart(String fdaUrl) {
        LoggerUtility.info("Navigating directly to cart page");
        driver.get(fdaUrl + "checkout/cart");
        // Refresh to bypass Magento FPC — ensures real server-side cart data is rendered
        // (add-to-cart AJAX may still be in flight on first load)
        driver.navigate().refresh();
        WaitUtility.fluentWait(driver, QUANTITY_INPUT);
        LoggerUtility.info("Cart page loaded and product visible");
    }
}
