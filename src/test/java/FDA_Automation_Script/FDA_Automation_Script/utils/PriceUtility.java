package FDA_Automation_Script.FDA_Automation_Script.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Parses/formats/compares price text pulled from Mirakl and FDA UI (e.g. "$45.00", "MXN$45.00",
// "45,00") into plain doubles — used by TC_OU_009's PLP/PDP price-match checks.
public class PriceUtility {

    private PriceUtility() {}

    public static double parse(String priceText) {
        if (priceText == null || priceText.isBlank()) return Double.NaN;
        String cleaned = priceText.replaceAll("[^0-9.,]", "");
        // Treat comma as a thousands separator (Mirakl/FDA prices in this app never use it as a
        // decimal point) — strip it, then parse the remaining dot-decimal number.
        cleaned = cleaned.replace(",", "");
        if (cleaned.isBlank()) {
            LoggerUtility.warn("PriceUtility.parse: no digits found in '" + priceText + "'");
            return Double.NaN;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            LoggerUtility.warn("PriceUtility.parse could not parse '" + priceText + "' (cleaned: '" + cleaned + "')");
            return Double.NaN;
        }
    }

    public static String format(double price) {
        return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** True if both prices parse and are equal within a 1-cent tolerance. */
    public static boolean matches(double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) return false;
        return Math.abs(a - b) < 0.01;
    }

    public static boolean matches(String priceTextA, double b) {
        return matches(parse(priceTextA), b);
    }
}
