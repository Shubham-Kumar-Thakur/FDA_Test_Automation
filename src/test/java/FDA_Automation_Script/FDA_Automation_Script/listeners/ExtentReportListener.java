package FDA_Automation_Script.FDA_Automation_Script.listeners;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;

import FDA_Automation_Script.FDA_Automation_Script.base.BaseClass;
import FDA_Automation_Script.FDA_Automation_Script.utils.ExtentManager;

/**
 * TestNG listener that drives the ExtentReports lifecycle in parallel with the
 * existing {@code TestNGListener}. Runs alongside - never replaces - it.
 *
 * <p>This class deliberately does NOT call {@code LoggerUtility}. Log messages
 * during listener callbacks would be forwarded by the Log4j2 appender into
 * either a just-completed test node or before a node exists, corrupting the
 * report. All diagnostics here go straight to the ExtentTest.</p>
 */
public class ExtentReportListener implements ITestListener, ISuiteListener {

    private static final String PASSED_DIR = "target/screenshots/passed";
    private static final String FAILED_DIR = "target/screenshots/failed";

    // TestNG assertion message formats.
    private static final Pattern EXPECTED_FOUND =
            Pattern.compile("expected \\[(.*?)\\] but found \\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern EXPECTED_WAS =
            Pattern.compile("expected:<(.*?)> but was:<(.*?)>", Pattern.DOTALL);

    private final ThreadLocal<Long> startTime = ThreadLocal.withInitial(() -> 0L);

    @Override
    public void onStart(ISuite suite) {
        ExtentManager.getInstance();
        new File(PASSED_DIR).mkdirs();
        new File(FAILED_DIR).mkdirs();
    }

    @Override
    public void onFinish(ISuite suite) {
        ExtentManager.flush();
        System.out.println("EXTENT REPORT: target/surefire-reports/ExecutionReport.html");
    }

    @Override
    public void onTestStart(ITestResult result) {
        startTime.set(System.currentTimeMillis());
        ExtentManager.createTest(result.getName(), result.getMethod().getDescription());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        try {
            if (test != null) {
                test.info("Duration: " + formatDuration(elapsedMillis()));

                String base64 = captureBase64();
                String savedPath = saveScreenshot(PASSED_DIR, result.getName() + "_PASS", base64);
                if (savedPath != null) {
                    test.info("Screenshot saved: " + savedPath);
                }

                if (base64 != null) {
                    test.pass("&#10003; TEST PASSED",
                            MediaEntityBuilder.createScreenCaptureFromBase64String(base64, "Final State").build());
                } else {
                    test.pass("&#10003; TEST PASSED");
                }
            }
        } catch (Exception e) {
            if (test != null) {
                test.pass("&#10003; TEST PASSED (screenshot capture failed: " + e.getMessage() + ")");
            }
        } finally {
            ExtentManager.removeTest();
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        try {
            if (test != null) {
                test.info("Duration: " + formatDuration(elapsedMillis()));

                Throwable throwable = result.getThrowable();
                if (throwable != null) {
                    test.warning("<b>Exception type:</b> " + throwable.getClass().getName());
                    String msg = throwable.getMessage();
                    if (msg != null) {
                        test.warning("<b>Exception message:</b> " + escapeHtml(msg));
                        logExpectedActual(test, msg);
                    }
                }

                String base64 = captureBase64();
                String savedPath = saveScreenshot(FAILED_DIR, result.getName() + "_FAIL", base64);
                if (savedPath != null) {
                    test.info("Screenshot saved: " + savedPath);
                }

                if (base64 != null) {
                    test.fail("&#10007; TEST FAILED",
                            MediaEntityBuilder.createScreenCaptureFromBase64String(base64, "Failure Screenshot").build());
                } else {
                    test.fail("&#10007; TEST FAILED");
                }

                if (throwable != null) {
                    test.fail(throwable);
                }
            }
        } catch (Exception e) {
            if (test != null) {
                test.fail("&#10007; TEST FAILED (error building failure report: " + e.getMessage() + ")");
            }
        } finally {
            ExtentManager.removeTest();
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        if (test == null) {
            test = ExtentManager.createTest(result.getName(), result.getMethod().getDescription());
        }
        try {
            test.skip("&#9888; TEST SKIPPED");
            Throwable throwable = result.getThrowable();
            if (throwable != null && throwable.getMessage() != null) {
                test.skip("Reason: " + escapeHtml(throwable.getMessage()));
            }
        } finally {
            ExtentManager.removeTest();
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private long elapsedMillis() {
        Long start = startTime.get();
        if (start == null || start == 0L) {
            return 0L;
        }
        return System.currentTimeMillis() - start;
    }

    /** Formats a millisecond duration as {@code "X min YY sec"}. */
    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes + " min " + String.format("%02d", seconds) + " sec";
    }

    /** Parses TestNG assertion messages and logs Expected/Actual when present. */
    private void logExpectedActual(ExtentTest test, String message) {
        Matcher found = EXPECTED_FOUND.matcher(message);
        if (found.find()) {
            test.warning("<b>Expected:</b> <span style='color:#4da6ff'>" + escapeHtml(found.group(1)) + "</span>");
            test.warning("<b>Actual:</b> <span style='color:#ff6b6b'>" + escapeHtml(found.group(2)) + "</span>");
            return;
        }
        Matcher was = EXPECTED_WAS.matcher(message);
        if (was.find()) {
            test.warning("<b>Expected:</b> <span style='color:#4da6ff'>" + escapeHtml(was.group(1)) + "</span>");
            test.warning("<b>Actual:</b> <span style='color:#ff6b6b'>" + escapeHtml(was.group(2)) + "</span>");
        }
    }

    /**
     * Captures the current browser screen as a Base64 string.
     *
     * @return Base64 PNG, or {@code null} if capture is not possible
     */
    private String captureBase64() {
        try {
            if (BaseClass.driver == null) {
                return null;
            }
            return ((TakesScreenshot) BaseClass.driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Persists a Base64 screenshot to disk.
     *
     * @return the file path written, or {@code null} on failure / null input
     */
    private String saveScreenshot(String dir, String fileName, String base64) {
        if (base64 == null) {
            return null;
        }
        try {
            String filePath = dir + "/" + fileName + ".png";
            Files.write(Paths.get(filePath), Base64.getDecoder().decode(base64));
            return filePath;
        } catch (Exception e) {
            return null;
        }
    }

    private static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
