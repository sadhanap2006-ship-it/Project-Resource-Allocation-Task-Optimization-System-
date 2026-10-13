package edu.sist.prs.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Utility for inclusive date range operations per Rule R1.
 * Rule R1: All date ranges are inclusive of start and end.
 * Ranges A and B overlap when A.start <= B.end && B.start <= A.end.
 * Implement once in util/DateRanges.overlaps(...).
 */
public final class DateRanges {

    private DateRanges() {}

    public static boolean overlaps(LocalDate startA, LocalDate endA, LocalDate startB, LocalDate endB) {
        Objects.requireNonNull(startA, "startA must not be null");
        Objects.requireNonNull(endA, "endA must not be null");
        Objects.requireNonNull(startB, "startB must not be null");
        Objects.requireNonNull(endB, "endB must not be null");

        // Rule R1
        return !startA.isAfter(endB) && !startB.isAfter(endA);
    }

    public static boolean contains(LocalDate start, LocalDate end, LocalDate date) {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");
        Objects.requireNonNull(date, "date must not be null");

        return !date.isBefore(start) && !date.isAfter(end);
    }

    public static long daysBetweenInclusive(LocalDate start, LocalDate end) {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");

        if (end.isBefore(start)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    public static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
