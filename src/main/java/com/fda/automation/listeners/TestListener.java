package com.fda.automation.listeners;

import com.fda.automation.base.BaseTest;
import com.fda.automation.utils.ScreenshotUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener {
    private static final Logger log = LogManager.getLogger(TestListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        log.info(">>> START: {}", result.getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info(">>> PASS:  {}", result.getName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log.error(">>> FAIL:  {} — {}", result.getName(),
                result.getThrowable() != null ? result.getThrowable().getMessage() : "unknown");
        captureScreenshot(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn(">>> SKIP:  {}", result.getName());
    }

    @Override
    public void onStart(ITestContext context) {
        log.info("=== Suite started: {} ===", context.getName());
    }

    @Override
    public void onFinish(ITestContext context) {
        log.info("=== Suite finished: {} | pass={} fail={} skip={} ===",
                context.getName(),
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
    }

    private void captureScreenshot(ITestResult result) {
        Object instance = result.getInstance();
        if (instance instanceof BaseTest baseTest) {
            WebDriver driver = baseTest.getDriver();
            if (driver != null) {
                ScreenshotUtils.capture(driver, result.getName());
            }
        }
    }
}
