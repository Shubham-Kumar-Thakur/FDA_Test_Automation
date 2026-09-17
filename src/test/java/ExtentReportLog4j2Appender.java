

import java.io.Serializable;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.aventstack.extentreports.ExtentTest;

import FDA_Automation_Script.FDA_Automation_Script.utils.ExtentManager;

/**
 * Custom Log4j2 appender that forwards log events to the active
 * {@link ExtentTest}. This lets existing tests emit step-wise report entries
 * through their normal {@code LoggerUtility.info(...)} calls without any code
 * changes.
 *
 * <p>A per-thread re-entrancy guard prevents infinite recursion: if adding to
 * the ExtentTest were ever to log, that log would be suppressed rather than
 * re-entering {@link #append(LogEvent)}.</p>
 */
@Plugin(name = "ExtentReport", category = "Core", elementType = "appender", printObject = true)
public final class ExtentReportLog4j2Appender extends AbstractAppender {

    private static final ThreadLocal<Boolean> IN_APPEND = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private ExtentReportLog4j2Appender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
        if (Boolean.TRUE.equals(IN_APPEND.get())) {
            return; // re-entrancy guard
        }

        ExtentTest test = ExtentManager.getTest();
        if (test == null) {
            return; // no active test - nothing to attach to
        }

        IN_APPEND.set(Boolean.TRUE);
        try {
            String rawMessage = event.getMessage().getFormattedMessage();
            String message = escapeHtml(rawMessage);
            Level level = event.getLevel();

            if (level.isMoreSpecificThan(Level.ERROR)) {
                test.warning("<span style='color:#ff6b6b;font-weight:bold'>[ERROR]</span> " + message);
            } else if (level.isMoreSpecificThan(Level.WARN)) {
                test.warning("<span style='color:#ffa500'>[WARN]</span> " + message);
            } else {
                if (rawMessage != null && rawMessage.matches("(?i)step \\d+.*")) {
                    test.info("<span style='color:#4da6ff;font-weight:bold'>" + message + "</span>");
                } else {
                    test.info(message);
                }
            }
        } catch (Exception ignored) {
            // Never let report logging break the test run.
        } finally {
            IN_APPEND.set(Boolean.FALSE);
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

    /**
     * Factory used by Log4j2 to build the appender from configuration.
     *
     * @param name   the appender name (required)
     * @param filter optional filter
     * @param layout optional layout; a default PatternLayout is used if absent
     * @return a configured appender, or {@code null} if {@code name} is missing
     */
    @PluginFactory
    public static ExtentReportLog4j2Appender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout) {

        if (name == null) {
            LOGGER.error("No name provided for ExtentReportLog4j2Appender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new ExtentReportLog4j2Appender(name, filter, layout);
    }
}
