package com.sist.rms.service;

import com.sist.rms.entity.Developer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * COST FORMULA (explainable in a viva - no library, no magic)
 *
 *   working_days = number of Monday-to-Friday days between the two dates
 *   hours        = working_days x 8 x (allocation_percentage / 100)
 *   cost         = hours x developer.hourly_cost
 *
 * Example: Arun at Rs.500/hour, 60 %, over 22 working days
 *          hours = 22 x 8 x 0.60 = 105.6
 *          cost  = 105.6 x 500   = Rs.52,800
 */
@Service
public class CostService {

    /** A normal working day is 8 hours. */
    public static final int HOURS_PER_WORKING_DAY = 8;

    /** Counts Monday..Friday days between the two dates, both days included. */
    public long countWorkingDays(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }
        long working = 0;
        LocalDate day = start;
        while (!day.isAfter(end)) {
            DayOfWeek dow = day.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                working++;
            }
            day = day.plusDays(1);
        }
        return working;
    }

    /** Hours a developer actually works in the period at the given allocation. */
    public BigDecimal calculateHours(LocalDate start, LocalDate end, int allocationPercentage) {
        long workingDays = countWorkingDays(start, end);
        return BigDecimal.valueOf(workingDays)
                .multiply(BigDecimal.valueOf(HOURS_PER_WORKING_DAY))
                .multiply(BigDecimal.valueOf(allocationPercentage))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    /** Total rupee cost of one allocation. */
    public BigDecimal calculateCost(Developer developer, LocalDate start, LocalDate end, int allocationPercentage) {
        if (developer == null || developer.getHourlyCost() == null) {
            return BigDecimal.ZERO;
        }
        return calculateHours(start, end, allocationPercentage)
                .multiply(developer.getHourlyCost())
                .setScale(2, RoundingMode.HALF_UP);
    }
}
