package edu.sist.prs.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Utility for working days and calendar days per Rule R5.
 * Rule R5: One developer working day = 8 hours. Weekends are not working days.
 * Public holidays are not modelled (documented limitation).
 */
public final class WorkingDays {

    private WorkingDays() {}

    public static boolean isWeekend(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    public static boolean isWorkingDay(LocalDate date) {
        return !isWeekend(date);
    }

    public static long countWorkingDays(LocalDate start, LocalDate end) {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");

        if (end.isBefore(start)) {
            return 0;
        }

        long count = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (isWorkingDay(current)) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }
}
