package FDA_Automation_Script.FDA_Automation_Script.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

/**
 * Creates fully independent Chrome WebDriver instances.
 * Each call returns a new browser process with its own cookies / local storage —
 * no session sharing between instances. Used by TC_FBS_001_Test (module) to keep
 * the FDA session and the Mirakl session in separate browsers.
 *
 * Does NOT touch DriverFactory or its ThreadLocal — existing tests are unaffected.
 */
public class DualDriverManager {

    private DualDriverManager() {}

    /**
     * Returns a new Chrome WebDriver with the same options/timeouts as DriverFactory
     * (no-sandbox, EAGER page-load strategy, 2-minute implicit wait, maximized window).
     */
    public static WebDriver createFreshChromeDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--start-maximized",
            "--disable-notifications",
            "--disable-popup-blocking"
        );
        // EAGER avoids blocking on slow third-party scripts (same rationale as DriverFactory).
        opts.setPageLoadStrategy(PageLoadStrategy.EAGER);
        WebDriver driver = new ChromeDriver(opts);
        driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
        driver.manage().window().maximize();
        LoggerUtility.info("DualDriverManager: new Chrome browser instance created");
        return driver;
    }
}
