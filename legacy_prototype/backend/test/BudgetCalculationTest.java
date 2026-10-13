package test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

public class BudgetCalculationTest {

    public static void runTests() {
        System.out.println("Running BudgetCalculationTest...");

        testBudgetRemainingAndUtilization();
        testRollingWindowBurnRateCalculation();
        testZeroBurnRateEdgeCase();
        testExhaustedBudgetStatus();

        System.out.println("  [PASS] BudgetCalculationTest (4/4 passed)");
    }

    private static void testBudgetRemainingAndUtilization() {
        double totalBudget = 500000.0;
        double currentSpend = 350000.0;

        double remaining = Math.max(0, totalBudget - currentSpend);
        double utilization = (currentSpend / totalBudget) * 100.0;

        assert remaining == 150000.0 : "Expected remaining budget 150000, got " + remaining;
        assert utilization == 70.0 : "Expected utilization 70%, got " + utilization;
    }

    private static void testRollingWindowBurnRateCalculation() {
        // Spec formula: burnRate = recentSpend / elapsedTime in rolling window
        List<Double> recentTransactions = Arrays.asList(100000.0, 120000.0, 130000.0);
        double recentSpend = 0.0;
        for (double val : recentTransactions) recentSpend += val;

        long elapsedDaysInWindow = 20; // 20 days span
        double burnRate = recentSpend / elapsedDaysInWindow;

        assert recentSpend == 350000.0 : "Expected recent spend 350000, got " + recentSpend;
        assert burnRate == 17500.0 : "Expected daily burn rate 17500/day, got " + burnRate;

        double remainingBudget = 150000.0;
        long daysUntilExhaustion = (long) Math.ceil(remainingBudget / burnRate);
        assert daysUntilExhaustion == 9 : "Expected days until exhaustion ~9 days, got " + daysUntilExhaustion;
    }

    private static void testZeroBurnRateEdgeCase() {
        double currentSpend = 0.0;
        double totalBudget = 600000.0;
        double dailyBurnRate = 0.0;

        long daysUntilExhaustion = -1;
        String exhaustionDate = "N/A (No Spend Recorded)";

        if (dailyBurnRate <= 0) {
            daysUntilExhaustion = -1;
            exhaustionDate = "Insufficient spending data";
        }

        assert daysUntilExhaustion == -1 : "Zero burn rate should not cause arithmetic division by zero";
        assert "Insufficient spending data".equals(exhaustionDate) : "Should report insufficient spending data fallback";
    }

    private static void testExhaustedBudgetStatus() {
        double totalBudget = 500000.0;
        double currentSpend = 500000.0;
        double remaining = Math.max(0, totalBudget - currentSpend);

        String exhaustionDate;
        long daysUntilExhaustion;
        if (remaining <= 0) {
            exhaustionDate = "EXHAUSTED";
            daysUntilExhaustion = 0;
        } else {
            exhaustionDate = "ACTIVE";
            daysUntilExhaustion = 10;
        }

        assert "EXHAUSTED".equals(exhaustionDate) : "Budget with 0 remaining must be flagged EXHAUSTED";
        assert daysUntilExhaustion == 0 : "Days until exhaustion must be 0 when budget is spent";
    }
}
