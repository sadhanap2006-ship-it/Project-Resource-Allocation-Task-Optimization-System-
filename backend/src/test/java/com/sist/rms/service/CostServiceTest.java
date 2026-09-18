package com.sist.rms.service;

import com.sist.rms.entity.Developer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * cost = working days x 8 hours x allocation percentage x hourly rate
 */
class CostServiceTest {

    private final CostService costService = new CostService();

    @Test
    @DisplayName("Weekends are not counted as working days")
    void weekendsAreSkipped() {
        // Monday 2026-09-07 to Sunday 2026-09-13 = 5 working days
        assertEquals(5, costService.countWorkingDays(
                LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13)));
    }

    @Test
    @DisplayName("A single working day counts as one")
    void oneDay() {
        assertEquals(1, costService.countWorkingDays(
                LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 7)));
    }

    @Test
    @DisplayName("5 working days at 60% is 24 hours")
    void hoursUseTheAllocationPercentage() {
        // 5 days x 8 hours x 0.60 = 24
        BigDecimal hours = costService.calculateHours(
                LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13), 60);
        assertEquals(0, BigDecimal.valueOf(24).compareTo(hours));
    }

    @Test
    @DisplayName("24 hours at Rs.500 per hour is Rs.12,000")
    void costMultipliesHoursByRate() {
        Developer arun = TestData.developer(1L, "Arun", 100, 500);
        BigDecimal cost = costService.calculateCost(
                arun, LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13), 60);
        assertEquals(0, BigDecimal.valueOf(12000).compareTo(cost));
    }

    @Test
    @DisplayName("A costlier developer on the same work costs more")
    void costlierDeveloperCostsMore() {
        Developer arun = TestData.developer(1L, "Arun", 100, 500);
        Developer karthik = TestData.developer(2L, "Karthik", 100, 700);

        BigDecimal arunCost = costService.calculateCost(
                arun, LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13), 60);
        BigDecimal karthikCost = costService.calculateCost(
                karthik, LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13), 60);

        assertEquals(1, karthikCost.compareTo(arunCost));
    }
}
