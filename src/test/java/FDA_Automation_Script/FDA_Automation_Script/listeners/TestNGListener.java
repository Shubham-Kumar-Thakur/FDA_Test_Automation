package FDA_Automation_Script.FDA_Automation_Script.listeners;

import FDA_Automation_Script.FDA_Automation_Script.base.BaseClass;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestNGListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        LoggerUtility.info("==================================================");
        LoggerUtility.info("Test Started: " + result.getName());
        LoggerUtility.info("==================================================");
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LoggerUtility.info("Test Passed: " + result.getName());
        if (BaseClass.driver != null) {
            ScreenshotUtility.captureScreenshot(BaseClass.driver, result.getName(), ScreenshotUtility.PASS);
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        LoggerUtility.error("Test Failed: " + result.getName());
        LoggerUtility.error("Failure reason: " + result.getThrowable().getMessage());
        if (BaseClass.driver != null) {
            ScreenshotUtility.captureScreenshot(BaseClass.driver, result.getName(), ScreenshotUtility.FAIL);
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LoggerUtility.warn("Test Skipped: " + result.getName());
    }
}
