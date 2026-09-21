package FDA_Automation_Script.FDA_Automation_Script.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads TC_FBS_001-specific test data from testdata/TC_FBS_001.properties.
 * Intentionally isolated from the main ConfigReader so TC_FBS_001 data
 * never touches config.properties or any other test's data file.
 */
public class TCFBS001TestDataReader {

    private static final String DATA_FILE = "testdata/TC_FBS_001.properties";
    private final Properties properties = new Properties();

    public TCFBS001TestDataReader() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(DATA_FILE)) {
            if (in == null) {
                throw new RuntimeException(DATA_FILE + " not found in classpath. "
                    + "Ensure src/test/resources/testdata/TC_FBS_001.properties exists.");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + DATA_FILE, e);
        }
    }

    private String get(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Property not found in TC_FBS_001 test data: " + key);
        }
        return value.trim();
    }

    public String getFdaUsername()         { return get("fda.username"); }
    public String getFdaPassword()         { return get("fda.password"); }
    public String getFdaSku()              { return get("fda.sku"); }
    public String getFdaCardNumber()       { return get("fda.card.number"); }
    public String getFdaCardExpiration()   { return get("fda.card.expiration"); }
    public String getFdaCardCvv()          { return get("fda.card.securityCode"); }
    public String getMiraklUsername()      { return get("mirakl.username"); }
    public String getMiraklPassword()      { return get("mirakl.password"); }
    public String getMiraklInvoicePath()   { return get("mirakl.invoice.path"); }
    public String getMiraklCarrier()       { return get("mirakl.carrier"); }

    /** Returns card number with all but last 4 digits masked — safe for logs. */
    public String getMaskedCardNumber() {
        String card = getFdaCardNumber().replaceAll("[^0-9]", "");
        if (card.length() <= 4) return "****";
        return "*".repeat(card.length() - 4) + card.substring(card.length() - 4);
    }
}
