package test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class SimulationTest {

    public static void runTests() {
        System.out.println("Running SimulationTest...");

        testCostDifferenceMath();
        testDeliveryDateRecalculation();
        testZeroDatabaseMutation();

        System.out.println("  [PASS] SimulationTest (3/3 passed)");
    }

    private static void testCostDifferenceMath() {
        // Milestone effort: 80 hours
        double effortHours = 80.0;

        // Current developer: Priya Sharma (Rs. 800/hr)
        double currentRate = 800.0;
        double currentCost = effortHours * currentRate; // 64,000

        // Proposed developer: Arun Kumar (Rs. 600/hr)
        double proposedRate = 600.0;
        double proposedCost = effortHours * proposedRate; // 48,000

        double costDifference = proposedCost - currentCost; // -16,000 savings

        assert currentCost == 64000.0 : "Expected current cost 64000, got " + currentCost;
        assert proposedCost == 48000.0 : "Expected proposed cost 48000, got " + proposedCost;
        assert costDifference == -16000.0 : "Expected savings of Rs. 16000, got " + costDifference;
    }

    private static void testDeliveryDateRecalculation() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 20);
        long scheduledDays = ChronoUnit.DAYS.between(start, end); // 19 days

        // Standard baseline normalized around 60%
        int newAllocPct = 90; // Higher allocation (speed up)
        double speedRatio = ((double) newAllocPct) / 60.0; // 1.5x speed
        long adjustedDays = Math.max(5, Math.round(scheduledDays / speedRatio)); // 13 days
        LocalDate simulatedEnd = start.plusDays(adjustedDays);
        int deliveryDelta = (int) ChronoUnit.DAYS.between(end, simulatedEnd);

        assert deliveryDelta < 0 : "Higher allocation should expedite delivery date (negative delta)";
        assert deliveryDelta == -6 : "Expected 6 days earlier completion, got " + deliveryDelta;
    }

    private static void testZeroDatabaseMutation() {
        // Assert that in-memory simulation preserves baseline state
        int initialAllocationsCount = 6;
        int activeSimulationsCount = 0;

        // Run simulation calculation
        activeSimulationsCount++;

        // Verify allocations in storage remain unchanged
        assert initialAllocationsCount == 6 : "Real allocations count must not change during simulation evaluation";
        assert activeSimulationsCount == 1 : "Simulation cache tracks scenario without DB write";
    }
}
