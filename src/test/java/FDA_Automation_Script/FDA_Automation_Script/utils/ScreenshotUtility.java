package FDA_Automation_Script.FDA_Automation_Script.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ScreenshotUtility {

    public static final String PASS    = "PASS";
    public static final String FAIL    = "FAIL";
    public static final String INFO    = "INFO";

    private static final String SCREENSHOT_DIR = "test-output/screenshots/";

    public static String captureScreenshot(WebDriver driver, String testCaseName, String status) {
        String fileName = testCaseName + "_" + status + ".png";
        String fullPath = SCREENSHOT_DIR + fileName;
        try {
            File destDir = new File(SCREENSHOT_DIR);
            if (!destDir.exists()) destDir.mkdirs();
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File destFile = new File(fullPath);
            Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LoggerUtility.info("Screenshot captured: " + fullPath);
        } catch (IOException e) {
            LoggerUtility.error("Failed to capture screenshot: " + e.getMessage(), e);
        }
        return fullPath;
    }
}
