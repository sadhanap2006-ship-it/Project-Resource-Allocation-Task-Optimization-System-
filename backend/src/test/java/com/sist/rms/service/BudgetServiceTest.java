package com.sist.rms.service;

import com.sist.rms.dto.BudgetForecastResponse;
import com.sist.rms.dto.BudgetResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Case 6 of the specification, plus the divide-by-zero guards.
 * calculateBudget and calculateForecast take plain numbers, so no database
 * is needed and the repositories are not used.
 */
class BudgetServiceTest {

    private final BudgetService budgetService = new BudgetService(null, null, null);

    @Test
    @DisplayName("CASE 6: 500,000 budget, 300,000 spent, 10,000 per day -> 20 days left")
    void case6_forecast() {
        // 300,000 spent over 30 days = 10,000 per day
        LocalDate firstSpend = LocalDate.of(2026, 8, 19);
        LocalDate today = LocalDate.of(2026, 9, 18);

        BudgetForecastResponse forecast = budgetService.calculateForecast(
                1L, "Hospital Management Portal",
                BigDecimal.valueOf(500000), BigDecimal.valueOf(300000),
                firstSpend, today);

        assertEquals(0, BigDecimal.valueOf(200000).compareTo(forecast.getRemainingBudget()));
        assertEquals(30L, forecast.getDaysElapsed());
        assertEquals(0, BigDecimal.valueOf(10000.00).compareTo(forecast.getBurnRatePerDay()));
        assertEquals(20L, forecast.getEstimatedDaysRemaining());
        assertEquals(LocalDate.of(2026, 10, 8), forecast.getEstimatedExhaustionDate());
        assertEquals("At the current burn rate, the project is estimated to exhaust its budget "
                + "in approximately 20 days.", forecast.getMessage());
    }

    @Test
    @DisplayName("Budget utilization is (spending / budget) x 100")
    void utilizationIsCalculated() {
        BudgetResponse budget = budgetService.calculateBudget(
                1L, "Hospital", BigDecimal.valueOf(500000), BigDecimal.valueOf(300000));

        assertEquals(60.0, budget.getUtilizationPercentage());
        assertEquals(0, BigDecimal.valueOf(200000).compareTo(budget.getRemainingBudget()));
        assertEquals("NONE", budget.getWarningLevel());
    }

    @Test
    @DisplayName("90% spent raises a CRITICAL budget warning")
    void criticalWarningAboveNinetyPercent() {
        BudgetResponse budget = budgetService.calculateBudget(
                2L, "E-Commerce", BigDecimal.valueOf(400000), BigDecimal.valueOf(360000));

        assertEquals(90.0, budget.getUtilizationPercentage());
        assertEquals("CRITICAL", budget.getWarningLevel());
    }

    @Test
    @DisplayName("80% spent raises a WARNING")
    void warningAboveSeventyFivePercent() {
        BudgetResponse budget = budgetService.calculateBudget(
                2L, "E-Commerce", BigDecimal.valueOf(100000), BigDecimal.valueOf(80000));

        assertEquals("WARNING", budget.getWarningLevel());
    }

    @Test
    @DisplayName("No spending -> 'No spending data available', and no division happens")
    void noSpendingData() {
        BudgetResponse budget = budgetService.calculateBudget(
                3L, "College ERP", BigDecimal.valueOf(300000), BigDecimal.ZERO);

        assertNull(budget.getUtilizationPercentage());
        assertFalse(budget.isHasSpendingData());
        assertEquals("No spending data available", budget.getMessage());
    }

    @Test
    @DisplayName("Zero budget never causes a divide-by-zero")
    void zeroBudgetIsSafe() {
        BudgetResponse budget = budgetService.calculateBudget(
                4L, "New Project", BigDecimal.ZERO, BigDecimal.ZERO);

        assertNull(budget.getUtilizationPercentage());
        assertEquals("No budget has been set for this project.", budget.getMessage());
    }

    @Test
    @DisplayName("No spending -> the forecast also says 'No spending data available'")
    void forecastWithoutSpending() {
        BudgetForecastResponse forecast = budgetService.calculateForecast(
                3L, "College ERP", BigDecimal.valueOf(300000), BigDecimal.ZERO,
                null, LocalDate.of(2026, 9, 18));

        assertNull(forecast.getBurnRatePerDay());
        assertNull(forecast.getEstimatedDaysRemaining());
        assertEquals("No spending data available", forecast.getMessage());
    }

    @Test
    @DisplayName("An exhausted budget reports 0 days remaining, not a negative number")
    void exhaustedBudget() {
        BudgetForecastResponse forecast = budgetService.calculateForecast(
                5L, "Overspent", BigDecimal.valueOf(100000), BigDecimal.valueOf(120000),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 18));

        assertEquals(0L, forecast.getEstimatedDaysRemaining());
        assertTrue(forecast.getMessage().contains("already fully used"));
    }

    @Test
    @DisplayName("The forecast never claims to be an AI prediction")
    void forecastStatesItIsPlainArithmetic() {
        BudgetForecastResponse forecast = budgetService.calculateForecast(
                1L, "Hospital", BigDecimal.valueOf(500000), BigDecimal.valueOf(300000),
                LocalDate.of(2026, 8, 19), LocalDate.of(2026, 9, 18));

        assertTrue(forecast.getMethodNote().contains("not an AI prediction"));
    }
}
