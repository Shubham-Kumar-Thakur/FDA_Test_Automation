package com.fda.automation.utils;

import com.fda.automation.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public class DriverFactory {

    private DriverFactory() {}

    public static WebDriver createDriver(String browser, boolean headless) {
        return switch (browser.toLowerCase()) {
            case "chrome" -> createChrome(headless);
            case "firefox" -> createFirefox(headless);
            case "edge" -> createEdge(headless);
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
        };
    }

    public static WebDriver createDriver() {
        ConfigManager cfg = ConfigManager.getInstance();
        return createDriver(cfg.getBrowser(), cfg.isHeadless());
    }

    private static WebDriver createChrome(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        if (headless) opts.addArguments("--headless=new");
        opts.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1080");
        return new ChromeDriver(opts);
    }

    private static WebDriver createFirefox(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions opts = new FirefoxOptions();
        if (headless) opts.addArguments("--headless");
        return new FirefoxDriver(opts);
    }

    private static WebDriver createEdge(boolean headless) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions opts = new EdgeOptions();
        if (headless) opts.addArguments("--headless=new");
        return new EdgeDriver(opts);
        dfghjkghjk
    }
}
