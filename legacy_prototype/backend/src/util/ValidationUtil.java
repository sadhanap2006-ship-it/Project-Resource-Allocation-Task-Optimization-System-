package util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static boolean isValidPercentage(int pct) {
        return pct >= 1 && pct <= 100;
    }

    public static boolean isPositive(double val) {
        return val >= 0;
    }

    public static boolean isValidDateRange(String start, String end) {
        if (!isNotEmpty(start) || !isNotEmpty(end)) return false;
        try {
            LocalDate d1 = LocalDate.parse(start);
            LocalDate d2 = LocalDate.parse(end);
            return !d2.isBefore(d1);
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
