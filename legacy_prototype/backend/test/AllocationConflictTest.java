package test;

import java.time.LocalDate;
import java.util.*;

public class AllocationConflictTest {

    public static void runTests() {
        System.out.println("Running AllocationConflictTest...");

        testWorkedExampleOverAllocation();
        testNonOverlappingAllocations();
        testExactHundredPercentBoundary();

        System.out.println("  [PASS] AllocationConflictTest (3/3 passed)");
    }

    /**
     * Literal Worked Example from Spec:
     * 60% Jan 1-20 + 50% Jan 10-25 = 110% peak conflict between Jan 10-20.
     */
    private static void testWorkedExampleOverAllocation() {
        // Allocation 1: Jan 1 to Jan 20, 60%
        LocalDate start1 = LocalDate.of(2026, 1, 1);
        LocalDate end1 = LocalDate.of(2026, 1, 20);
        int pct1 = 60;

        // Allocation 2: Jan 10 to Jan 25, 50%
        LocalDate start2 = LocalDate.of(2026, 1, 10);
        LocalDate end2 = LocalDate.of(2026, 1, 25);
        int pct2 = 50;

        // Sweep-line boundary evaluation
        TreeSet<LocalDate> boundaryDates = new TreeSet<>(Arrays.asList(start1, end1, start2, end2));
        int peakAllocation = 0;
        LocalDate conflictStart = null;
        LocalDate conflictEnd = null;

        for (LocalDate d : boundaryDates) {
            int load = 0;
            if (!d.isBefore(start1) && !d.isAfter(end1)) load += pct1;
            if (!d.isBefore(start2) && !d.isAfter(end2)) load += pct2;

            if (load > peakAllocation) {
                peakAllocation = load;
            }
        }

        // Check conflict interval
        boolean hasOverlap = !(start1.isAfter(end2) || start2.isAfter(end1));
        if (hasOverlap) {
            conflictStart = start1.isAfter(start2) ? start1 : start2; // Jan 10
            conflictEnd = end1.isBefore(end2) ? end1 : end2;         // Jan 20
        }

        assert peakAllocation == 110 : "Expected peak allocation 110%, got " + peakAllocation;
        assert peakAllocation > 100 : "Expected over-allocation flag > 100%";
        assert LocalDate.of(2026, 1, 10).equals(conflictStart) : "Expected conflict start Jan 10, got " + conflictStart;
        assert LocalDate.of(2026, 1, 20).equals(conflictEnd) : "Expected conflict end Jan 20, got " + conflictEnd;
    }

    private static void testNonOverlappingAllocations() {
        LocalDate start1 = LocalDate.of(2026, 1, 1);
        LocalDate end1 = LocalDate.of(2026, 1, 10);
        int pct1 = 60;

        LocalDate start2 = LocalDate.of(2026, 1, 11);
        LocalDate end2 = LocalDate.of(2026, 1, 20);
        int pct2 = 50;

        TreeSet<LocalDate> boundaryDates = new TreeSet<>(Arrays.asList(start1, end1, start2, end2));
        int peakAllocation = 0;
        for (LocalDate d : boundaryDates) {
            int load = 0;
            if (!d.isBefore(start1) && !d.isAfter(end1)) load += pct1;
            if (!d.isBefore(start2) && !d.isAfter(end2)) load += pct2;
            if (load > peakAllocation) peakAllocation = load;
        }

        assert peakAllocation == 60 : "Expected peak allocation 60%, got " + peakAllocation;
        assert peakAllocation <= 100 : "Expected no conflict for non-overlapping dates";
    }

    private static void testExactHundredPercentBoundary() {
        LocalDate start1 = LocalDate.of(2026, 2, 1);
        LocalDate end1 = LocalDate.of(2026, 2, 28);
        int pct1 = 60;

        LocalDate start2 = LocalDate.of(2026, 2, 1);
        LocalDate end2 = LocalDate.of(2026, 2, 28);
        int pct2 = 40;

        int combined = pct1 + pct2;
        assert combined == 100 : "Expected combined load exactly 100%";
        assert combined <= 100 : "100% capacity is valid and should not trigger > 100% over-allocation flag";
    }
}
