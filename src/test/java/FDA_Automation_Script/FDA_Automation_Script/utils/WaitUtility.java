package FDA_Automation_Script.FDA_Automation_Script.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;

/**
 * Use ONLY after driver.navigate().refresh(), page reload, or API execution.
 * Standard element lookups rely on the global 2-minute implicit wait.
 */
public class WaitUtility {

    private static final Duration FLUENT_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration FLUENT_POLLING = Duration.ofSeconds(2);

    private static FluentWait<WebDriver> buildFluentWait(WebDriver driver) {
        return new FluentWait<>(driver)
                .withTimeout(FLUENT_TIMEOUT)
                .pollingEvery(FLUENT_POLLING)
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .ignoring(MoveTargetOutOfBoundsException.class);
    }

    public static WebElement fluentWait(WebDriver driver, By locator) {
        return buildFluentWait(driver)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement fluentWaitForClickable(WebDriver driver, By locator) {
        return buildFluentWait(driver)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static boolean fluentWaitForText(WebDriver driver, By locator, String expectedText) {
        return buildFluentWait(driver)
                .until(ExpectedConditions.textToBe(locator, expectedText));
    }

    public static boolean fluentWaitForTextContains(WebDriver driver, By locator, String partialText) {
        return buildFluentWait(driver)
                .until(ExpectedConditions.textToBePresentInElementLocated(locator, partialText));
    }

    public static boolean fluentWaitForUrl(WebDriver driver, String urlFragment) {
        return buildFluentWait(driver)
                .until(ExpectedConditions.urlContains(urlFragment));
    }

    public static WebElement fluentWaitForVisible(WebDriver driver, By locator) {
        return buildFluentWait(driver)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static boolean fluentWaitForPresence(WebDriver driver, By locator) {
        return buildFluentWait(driver)
                .until(d -> {
                    try {
                        return d.findElement(locator).isDisplayed();
                    } catch (Exception e) {
                        return false;
                    }
                });
    }
}
