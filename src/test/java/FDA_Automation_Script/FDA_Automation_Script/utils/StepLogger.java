package FDA_Automation_Script.FDA_Automation_Script.utils;

import java.util.concurrent.atomic.AtomicInteger;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;

import FDA_Automation_Script.FDA_Automation_Script.base.BaseClass;

/**
 * Optional structured step logger for new test cases.
 *
 * <p>Design note: all textual logging goes through {@link LoggerUtility} only.
 * The {@code ExtentReportLog4j2Appender} forwards those messages into the
 * active ExtentTest, so this class must NOT also write text to ExtentReports -
 * doing so would duplicate every line in the report. The one exception is
 * {@link #screenshot(String)}, which embeds an image; images cannot travel
 * through a log message, so that method touches {@link ExtentManager} directly.</p>
 */
public final class StepLogger {

    private static final ThreadLocal<AtomicInteger> COUNTER =
            ThreadLocal.withInitial(() -> new AtomicInteger(0));

    private static final ThreadLocal<String> LAST_STEP = new ThreadLocal<>();

    private StepLogger() {
        // utility class - no instances
    }

    /** Resets the step counter and last-step tracker for the current thread. */
    public static void reset() {
        COUNTER.get().set(0);
        LAST_STEP.remove();
    }

    /**
     * Logs an auto-numbered step. The appender forwards it to ExtentReports.
     *
     * @param description what the step does
     */
    public static void step(String description) {
        int n = COUNTER.get().incrementAndGet();
        String label = "Step " + n + ": " + description;
        LAST_STEP.set(label);
        LoggerUtility.info(label);
    }

    /**
     * Logs an auto-numbered passing step.
     *
     * @param description the step that passed
     */
    public static void pass(String description) {
        int n = COUNTER.get().incrementAndGet();
        String label = "[PASS] Step " + n + ": " + description;
        LAST_STEP.set(label);
        LoggerUtility.info(label);
    }

    /**
     * Logs an auto-numbered failing step and captures a screenshot. Does NOT
     * re-throw; the caller is responsible for propagating the throwable.
     *
     * @param description the step that failed
     * @param throwable   the cause (may be {@code null})
     */
    public static void fail(String description, Throwable throwable) {
        int n = COUNTER.get().incrementAndGet();
        String label = "[FAIL] Step " + n + ": " + description;
        LAST_STEP.set(label);
        if (throwable != null) {
            LoggerUtility.error(label, throwable);
        } else {
            LoggerUtility.error(label);
        }
        screenshot(label);
    }

    /**
     * Logs a free-form informational message (no step number).
     *
     * @param message the message
     */
    public static void info(String message) {
        LoggerUtility.info(message);
    }

    /**
     * Embeds a screenshot of the current browser state in the active ExtentTest.
     * This is the only method that writes to ExtentReports directly.
     *
     * @param label caption for the screenshot
     */
    public static void screenshot(String label) {
        ExtentTest test = ExtentManager.getTest();
        if (test == null) {
            return;
        }
        try {
            if (BaseClass.driver == null) {
                return;
            }
            String base64 = ((TakesScreenshot) BaseClass.driver).getScreenshotAs(OutputType.BASE64);
            test.info(label,
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64, label).build());
        } catch (Exception ignored) {
            // Screenshot embedding must never break a test.
        }
    }

    /**
     * @return the label of the most recent step on this thread, or {@code null}
     */
    public static String getLastStep() {
        return LAST_STEP.get();
    }
}
