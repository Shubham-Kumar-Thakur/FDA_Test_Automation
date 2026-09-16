package FDA_Automation_Script.FDA_Automation_Script.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Normalizes currency-formatted price strings (e.g. "$100.00", "MXN 100", "100,00")
 * to a BigDecimal so prices captured from Excel, Mirakl, and FDA (each with
 * different currency-symbol/formatting conventions) can be compared numerically
 * rather than by fragile string/partial matching.
 */
public class PriceUtility {

    private PriceUtility() {
    }

    public static BigDecimal normalize(String rawPrice) {
        if (rawPrice == null || rawPrice.isBlank()) {
            throw new IllegalArgumentException("Price string is null/blank — cannot normalize");
        }
        String numericOnly = rawPrice.replaceAll("[^0-9.\\-]", "");
        if (numericOnly.isEmpty() || numericOnly.equals("-") || numericOnly.equals(".")) {
            throw new IllegalArgumentException("No numeric value found in price string: " + rawPrice);
        }
        return new BigDecimal(numericOnly).setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean pricesEqual(String priceA, String priceB) {
        boolean equal = normalize(priceA).compareTo(normalize(priceB)) == 0;
        LoggerUtility.info("Price comparison: '" + priceA + "' vs '" + priceB + "' -> " + equal);
        return equal;
    }
}
