package FDA_Automation_Script.FDA_Automation_Script.pages;

import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public abstract class BasePage {

    protected final WebDriver driver;

    public BasePage(WebDriver driver) {
        this.driver = driver;
    }

    protected WebElement findElement(By locator) {
        return driver.findElement(locator);
    }

    protected void click(By locator) {
        try {
            driver.findElement(locator).click();
        } catch (StaleElementReferenceException e) {
            LoggerUtility.warn("StaleElementReferenceException on click, retrying: " + locator);
            driver.findElement(locator).click();
        }
    }

    protected void type(By locator, String text) {
        WebElement el = driver.findElement(locator);
        el.clear();
        el.sendKeys(text);
    }

    protected String getText(By locator) {
        return driver.findElement(locator).getText().trim();
    }

    protected String getAttribute(By locator, String attribute) {
        return driver.findElement(locator).getAttribute(attribute);
    }

    protected boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    protected void selectByVisibleText(By locator, String text) {
        new Select(driver.findElement(locator)).selectByVisibleText(text);
    }

    protected void scrollIntoView(By locator) {
        WebElement el = driver.findElement(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", el);
    }

    // ROOT-CAUSED live (2026-09-18, TC_E2E_004): findElement() succeeding does not guarantee the
    // returned WebElement is still attached to the DOM by the time executeScript() actually runs a
    // moment later — a React re-render of the surrounding nav/menu between those two calls leaves the
    // reference stale, throwing StaleElementReferenceException on the click itself (confirmed live at
    // MiraklProductImportsPage.navigateToProductImports()'s PRODUCT_IMPORTS_SUBMENU click). Since this
    // is the single shared click helper every page object calls, fixing the re-fetch-and-retry here
    // once covers every caller instead of duplicating the same guard in each page object. Bounded to a
    // small retry count, not caught-and-ignored — a click that keeps failing for a real reason (e.g.
    // the element never existing at all) still throws after the retries are exhausted.
    protected void jsClick(By locator) {
        StaleElementReferenceException lastError = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebElement el = driver.findElement(locator);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
                return;
            } catch (StaleElementReferenceException e) {
                lastError = e;
            }
        }
        throw lastError;
    }

    protected void switchToIframe(By locator) {
        driver.switchTo().frame(driver.findElement(locator));
    }

    protected void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    protected void pressEnter(By locator) {
        driver.findElement(locator).sendKeys(org.openqa.selenium.Keys.ENTER);
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
