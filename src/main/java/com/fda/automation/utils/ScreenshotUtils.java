package com.fda.automation.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScreenshotUtils {
    private static final Logger log = LogManager.getLogger(ScreenshotUtils.class);
    private static final String SCREENSHOT_DIR = "target/screenshots/";

    private ScreenshotUtils() {}

    public static String capture(WebDriver driver, String testName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String fileName = testName.replaceAll("[^a-zA-Z0-9_\\-]", "_") + "_" + timestamp + ".png";
        Path destPath = Paths.get(SCREENSHOT_DIR, fileName);
        try {
            Files.createDirectories(destPath.getParent());
            byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(destPath, bytes);
            log.info("Screenshot saved: {}", destPath.toAbsolutePath());
            return destPath.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("Failed to save screenshot for: {}", testName, e);
            return null;
        }
    }
}
