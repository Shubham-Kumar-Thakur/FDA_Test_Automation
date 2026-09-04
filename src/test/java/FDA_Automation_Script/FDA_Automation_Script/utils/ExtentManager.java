package FDA_Automation_Script.FDA_Automation_Script.utils;

import java.io.File;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

/**
 * Singleton owner of the {@link ExtentReports} instance and the per-thread
 * current {@link ExtentTest}.
 *
 * <p>This class intentionally does NOT use {@code LoggerUtility}. LoggerUtility
 * routes through the Log4j2 {@code ExtentReportLog4j2Appender}, which in turn
 * calls back into this class - logging here would create a circular dependency
 * during report initialization.</p>
 */
public final class ExtentManager {

    private static final String REPORT_DIR = "target/surefire-reports";
    private static final String REPORT_PATH = REPORT_DIR + "/ExecutionReport.html";

    private static ExtentReports extentReports;

    /** Tests run sequentially today, but ThreadLocal keeps this safe if that changes. */
    private static final ThreadLocal<ExtentTest> CURRENT_TEST = new ThreadLocal<>();

    private ExtentManager() {
        // utility class - no instances
    }

    /**
     * Returns the shared {@link ExtentReports} instance, creating and configuring
     * it on first access.
     *
     * @return the singleton ExtentReports instance
     */
    public static synchronized ExtentReports getInstance() {
        if (extentReports == null) {
            extentReports = createInstance();
        }
        return extentReports;
    }

    private static ExtentReports createInstance() {
        new File(REPORT_DIR).mkdirs();

        ExtentSparkReporter spark = new ExtentSparkReporter(REPORT_PATH);
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle("FDA Automation Execution Report");
        spark.config().setReportName("FDA Mirakl Automation Suite");

        ExtentReports reports = new ExtentReports();
        reports.attachReporter(spark);
        reports.setSystemInfo("Application", "Farmacias del Ahorro");
        reports.setSystemInfo("Environment", "Staging");
        reports.setSystemInfo("Browser", "Chrome");
        return reports;
    }

    /**
     * Creates a new test node, stores it as the current test for this thread,
     * and returns it.
     *
     * @param name        the test name (TestNG {@code testName})
     * @param description the test description (may be {@code null})
     * @return the created {@link ExtentTest}
     */
    public static ExtentTest createTest(String name, String description) {
        ExtentTest test = getInstance().createTest(name, description);
        CURRENT_TEST.set(test);
        return test;
    }

    /**
     * @return the current {@link ExtentTest} for this thread, or {@code null}
     *         if no test is active
     */
    public static ExtentTest getTest() {
        return CURRENT_TEST.get();
    }

    /** Clears the current test for this thread. */
    public static void removeTest() {
        CURRENT_TEST.remove();
    }

    /** Writes all buffered report data to disk. */
    public static void flush() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }
}
