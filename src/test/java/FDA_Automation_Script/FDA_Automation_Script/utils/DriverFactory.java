package FDA_Automation_Script.FDA_Automation_Script.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class DriverFactory {

	private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

	public static WebDriver getDriver() {
		return driverThreadLocal.get();
	}

	public static WebDriver createDriver(boolean headless) {
		String browser = ConfigReader.getInstance().get("browser");
		WebDriver driver;

		switch (browser.toLowerCase()) {
		case "firefox" -> {
			WebDriverManager.firefoxdriver().setup();
			FirefoxOptions opts = new FirefoxOptions();
			if (headless)
				opts.addArguments("-headless");
			opts.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--start-maximized", "--disable-notifications",
					"--disable-popup-blocking");
			// See Chrome branch below for why this is set — PageLoadStrategy is driver-agnostic.
			opts.setPageLoadStrategy(PageLoadStrategy.EAGER);
			driver = new FirefoxDriver(opts);
		}
		default -> {
			WebDriverManager.chromedriver().setup();
			ChromeOptions opts = new ChromeOptions();
			if (headless)
				opts.addArguments("--headless=new");
			opts.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--start-maximized", "--disable-notifications",
					"--disable-popup-blocking");
			// Default PageLoadStrategy (NORMAL) blocks every WebDriver command on a page until the
			// browser considers it fully loaded — including slow/long-hanging third-party scripts
			// that never really finish (confirmed live, 2026-09-09: the PayPal sandbox popup's
			// analytics/fraud-detection scripts kept the page in a "loading" state for ~4 minutes on
			// every PayPal checkout, so waitForPageLoad()'s own 60s WebDriverWait bound was silently
			// overridden — the wait's polling condition itself was blocked at the browser level, not
			// actually taking longer than 60s to individually evaluate). EAGER only waits for
			// DOMContentLoaded, which is enough for our element-based waits (WaitUtility,
			// WebDriverWait) to find what they need without being held hostage by background
			// resources we don't care about.
			opts.setPageLoadStrategy(PageLoadStrategy.EAGER);
			driver = new ChromeDriver(opts);
		}
		}

		driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
		driver.manage().window().maximize();
		driverThreadLocal.set(driver);
		LoggerUtility.info("Browser launched: " + browser);
		return driver;
	}

	public static String openNewTab() {
		WebDriver driver = getDriver();
		java.util.Set<String> currentHandles = driver.getWindowHandles();
		String currentHandle = driver.getWindowHandle();

		// If a secondary tab already exists reuse it — never open more than 2 tabs
		if (currentHandles.size() >= 2) {
			for (String h : currentHandles) {
				if (!h.equals(currentHandle)) {
					driver.switchTo().window(h);
					LoggerUtility.info("Reusing existing secondary tab: " + h);
					return h;
				}
			}
		}

		// No secondary tab yet — open one
		((JavascriptExecutor) driver).executeScript("window.open('');");
		List<String> handles = new ArrayList<>(driver.getWindowHandles());
		String newHandle = handles.get(handles.size() - 1);
		driver.switchTo().window(newHandle);
		LoggerUtility.info("New browser tab opened. Handle: " + newHandle);
		return newHandle;
	}

	public static void switchToTab(String handle) {
		getDriver().switchTo().window(handle);
		LoggerUtility.info("Switched to tab: " + handle);
	}

	public static void quitDriver() {
		WebDriver driver = driverThreadLocal.get();
		if (driver != null) {
			driver.quit();
			driverThreadLocal.remove();
			LoggerUtility.info("Browser closed");
		}
	}
}
