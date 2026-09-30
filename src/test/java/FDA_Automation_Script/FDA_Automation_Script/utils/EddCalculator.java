package FDA_Automation_Script.FDA_Automation_Script.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Pure business-day EDD calculation for TC_EDD_001 — no Selenium. Implements the exact two-case
// algorithm from the manual test case (Case A: cutoff not crossed, Case B: cutoff crossed) plus
// loose parsers to turn raw text scraped off Mirakl's Business Calendar tab into structured data.
// Mirrors PriceUtility's style: private constructor, static methods, log-and-return-empty on
// unparseable input rather than throwing from a parse path.
public class EddCalculator {

    private EddCalculator() {}

    // Business-mandatory constant from the manual test case: Delivery Days = Lead Time to Ship +
    // Maximum Delivery Days, which is always 2. Hardcoded, not configurable.
    public static final int MAX_DELIVERY_DAYS = 2;

    public static boolean isCutoffCrossed(LocalTime currentMxTime, LocalTime businessEndTime) {
        if (currentMxTime == null || businessEndTime == null) {
            LoggerUtility.warn("EddCalculator.isCutoffCrossed: null input, assuming not crossed");
            return false;
        }
        boolean crossed = currentMxTime.isAfter(businessEndTime);
        LoggerUtility.info("EddCalculator: currentMxTime=" + currentMxTime + ", businessEndTime=" + businessEndTime
            + ", cutoffCrossed=" + crossed);
        return crossed;
    }

    public static boolean isValidBusinessDay(LocalDate date, Set<DayOfWeek> workingDays, Set<LocalDate> nonWorkingDays,
            Set<LocalDate> holidays) {
        if (date == null) return false;
        if (workingDays != null && !workingDays.isEmpty() && !workingDays.contains(date.getDayOfWeek())) return false;
        if (nonWorkingDays != null && nonWorkingDays.contains(date)) return false;
        if (holidays != null && holidays.contains(date)) return false;
        return true;
    }

    /**
     * Implements the TC_EDD_001 manual test case's two-case algorithm:
     * deliveryDays = leadTimeToShipDays + MAX_DELIVERY_DAYS. Both cases skip the current day first.
     * Case A (cutoff not crossed): start counting deliveryDays valid business days from the next
     * valid working day (inclusive). Case B (cutoff crossed): skip one additional valid business day
     * (the "effective order date"), then start counting deliveryDays valid business days from the
     * following valid business day. Returns the deliveryDays-th valid business day in that walk.
     */
    public static LocalDate calculateExpectedEdd(LocalDate referenceDate, LocalTime currentMxTime, LocalTime businessEndTime,
            Set<DayOfWeek> workingDays, Set<LocalDate> nonWorkingDays, Set<LocalDate> holidays, int leadTimeToShipDays) {

        int deliveryDays = leadTimeToShipDays + MAX_DELIVERY_DAYS;
        boolean cutoffCrossed = isCutoffCrossed(currentMxTime, businessEndTime);
        LoggerUtility.info("EddCalculator: referenceDate=" + referenceDate + ", leadTimeToShip=" + leadTimeToShipDays
            + ", MAX_DELIVERY_DAYS=" + MAX_DELIVERY_DAYS + ", deliveryDays=" + deliveryDays
            + ", cutoffCrossed=" + cutoffCrossed);

        LocalDate cursor = referenceDate.plusDays(1); // skip current day — both cases

        if (cutoffCrossed) {
            LocalDate effectiveOrderDate = nextValidBusinessDayOnOrAfter(cursor, workingDays, nonWorkingDays, holidays);
            cursor = nextValidBusinessDayOnOrAfter(effectiveOrderDate.plusDays(1), workingDays, nonWorkingDays, holidays);
            LoggerUtility.info("EddCalculator: Case B (cutoff crossed) — effectiveOrderDate=" + effectiveOrderDate
                + ", counting starts at " + cursor);
        } else {
            cursor = nextValidBusinessDayOnOrAfter(cursor, workingDays, nonWorkingDays, holidays);
            LoggerUtility.info("EddCalculator: Case A (cutoff not crossed) — counting starts at " + cursor);
        }

        LocalDate expected = cursor;
        int counted = 1; // cursor itself is the 1st valid business day counted
        while (counted < deliveryDays) {
            expected = nextValidBusinessDayOnOrAfter(expected.plusDays(1), workingDays, nonWorkingDays, holidays);
            counted++;
        }

        LoggerUtility.info("EddCalculator: expected EDD = " + expected);
        return expected;
    }

    private static LocalDate nextValidBusinessDayOnOrAfter(LocalDate date, Set<DayOfWeek> workingDays,
            Set<LocalDate> nonWorkingDays, Set<LocalDate> holidays) {
        LocalDate candidate = date;
        for (int i = 0; i < 3650; i++) { // 10-year safety bound — avoids an infinite loop on bad input
            if (isValidBusinessDay(candidate, workingDays, nonWorkingDays, holidays)) return candidate;
            candidate = candidate.plusDays(1);
        }
        throw new IllegalStateException("No valid business day found within 10 years of " + date
            + " — check workingDays/nonWorkingDays/holidays input");
    }

    // ---- Loose parsers: raw Mirakl Business Calendar text -> structured data ----
    // TODO: Verify against the actual Business Calendar tab's rendered text/format once observed live
    // — these are best-effort formats, same convention as this project's placeholder locators.

    /** Parses a comma/newline-separated list of weekday names into a Set<DayOfWeek>. Unrecognized tokens are logged and skipped. */
    public static Set<DayOfWeek> parseWorkingDays(String rawText) {
        Set<DayOfWeek> days = new HashSet<>();
        if (rawText == null || rawText.isBlank()) {
            LoggerUtility.warn("EddCalculator.parseWorkingDays: blank input");
            return days;
        }
        for (String token : rawText.split("[,\\n;/]")) {
            String t = token.trim();
            if (t.isEmpty()) continue;
            DayOfWeek day = matchDayOfWeek(t);
            if (day != null) {
                days.add(day);
            } else {
                LoggerUtility.warn("EddCalculator.parseWorkingDays: unrecognized token '" + t + "'");
            }
        }
        LoggerUtility.info("EddCalculator.parseWorkingDays: parsed " + days + " from '" + rawText + "'");
        return days;
    }

    private static DayOfWeek matchDayOfWeek(String token) {
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        for (DayOfWeek day : DayOfWeek.values()) {
            String full = day.getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase(Locale.ROOT);
            String shortName = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toLowerCase(Locale.ROOT);
            if (normalized.equals(full) || normalized.equals(shortName) || full.startsWith(normalized)) {
                return day;
            }
        }
        return null;
    }

    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        // CONFIRMED live (2026-09-30, TC_EDD_001): the Business Calendar holiday row's date is a
        // single-digit-tolerant day-first format like "1/10/2026" (Oct 1, 2026) — "dd/MM/yyyy" and
        // "MM/dd/yyyy" below both require exactly 2 digits per field and fail to parse a single-digit
        // day/month at all (confirmed: LocalDate.parse threw for both against "1/10/2026"). "d/M/yyyy"
        // (variable width) must come before both fixed-width patterns so it wins for this case.
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    };

    /** Parses a comma/newline-separated list of dates (holidays or non-working days) into a Set<LocalDate>, trying several common formats. */
    public static Set<LocalDate> parseDates(String rawText) {
        Set<LocalDate> dates = new HashSet<>();
        if (rawText == null || rawText.isBlank()) {
            LoggerUtility.warn("EddCalculator.parseDates: blank input");
            return dates;
        }
        for (String token : rawText.split("[,\\n;]")) {
            String t = token.trim();
            if (t.isEmpty()) continue;
            LocalDate parsed = parseOneDate(t);
            if (parsed != null) {
                dates.add(parsed);
            } else {
                LoggerUtility.warn("EddCalculator.parseDates: unrecognized date token '" + t + "'");
            }
        }
        LoggerUtility.info("EddCalculator.parseDates: parsed " + dates + " from '" + rawText + "'");
        return dates;
    }

    private static LocalDate parseOneDate(String token) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(token, fmt);
            } catch (Exception ignored) {
                // try next format
            }
        }
        return null;
    }

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(AM|PM|am|pm)?");

    /** Parses a time string like "18:00" or "6:00 PM" into a LocalTime. Returns null if unparseable. */
    public static LocalTime parseTime(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            LoggerUtility.warn("EddCalculator.parseTime: blank input");
            return null;
        }
        Matcher m = TIME_PATTERN.matcher(rawText.trim());
        if (!m.find()) {
            LoggerUtility.warn("EddCalculator.parseTime: could not find a time pattern in '" + rawText + "'");
            return null;
        }
        int hour = Integer.parseInt(m.group(1));
        int minute = Integer.parseInt(m.group(2));
        String meridiem = m.group(3);
        if (meridiem != null) {
            boolean pm = meridiem.equalsIgnoreCase("PM");
            if (pm && hour < 12) hour += 12;
            if (!pm && hour == 12) hour = 0;
        }
        try {
            LocalTime time = LocalTime.of(hour, minute);
            LoggerUtility.info("EddCalculator.parseTime: parsed '" + rawText + "' as " + time);
            return time;
        } catch (Exception e) {
            LoggerUtility.warn("EddCalculator.parseTime: invalid hour/minute in '" + rawText + "'");
            return null;
        }
    }

    /**
     * Parses a combined "Business Hours" string (e.g. "Business Hours (Mexico Time - GMT-6)9:00 AM -
     * 6:00 PM") containing two times — start and end — by finding every time-pattern match in the
     * string rather than just the first (unlike parseTime()). Returns a 2-element array
     * {startTime, endTime}, either of which may be null if fewer than 2 matches were found.
     */
    public static LocalTime[] parseTimeRange(String rawText) {
        LocalTime[] result = new LocalTime[2];
        if (rawText == null || rawText.isBlank()) {
            LoggerUtility.warn("EddCalculator.parseTimeRange: blank input");
            return result;
        }
        Matcher m = TIME_PATTERN.matcher(rawText.trim());
        int index = 0;
        while (m.find() && index < 2) {
            int hour = Integer.parseInt(m.group(1));
            int minute = Integer.parseInt(m.group(2));
            String meridiem = m.group(3);
            if (meridiem != null) {
                boolean pm = meridiem.equalsIgnoreCase("PM");
                if (pm && hour < 12) hour += 12;
                if (!pm && hour == 12) hour = 0;
            }
            try {
                result[index] = LocalTime.of(hour, minute);
            } catch (Exception e) {
                LoggerUtility.warn("EddCalculator.parseTimeRange: invalid hour/minute for match " + index
                    + " in '" + rawText + "'");
            }
            index++;
        }
        LoggerUtility.info("EddCalculator.parseTimeRange: parsed '" + rawText + "' as start="
            + result[0] + ", end=" + result[1]);
        return result;
    }
}
