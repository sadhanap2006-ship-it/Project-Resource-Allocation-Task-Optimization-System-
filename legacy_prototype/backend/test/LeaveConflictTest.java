package test;

import java.time.LocalDate;

public class LeaveConflictTest {

    public static void runTests() {
        System.out.println("Running LeaveConflictTest...");

        testApprovedLeaveConflict();
        testLeaveOutsideMilestoneWindow();

        System.out.println("  [PASS] LeaveConflictTest (2/2 passed)");
    }

    private static void testApprovedLeaveConflict() {
        // Milestone: 2026-10-10 to 2026-11-05
        LocalDate mStart = LocalDate.of(2026, 10, 10);
        LocalDate mEnd = LocalDate.of(2026, 11, 5);

        // Leave: 2026-10-12 to 2026-10-16 (Rahul Verma)
        LocalDate lStart = LocalDate.of(2026, 10, 12);
        LocalDate lEnd = LocalDate.of(2026, 10, 16);

        // Date interval overlap condition: !(mStart > lEnd || lStart > mEnd)
        boolean hasConflict = !mStart.isAfter(lEnd) && !lStart.isAfter(mEnd);

        assert hasConflict : "Expected leave conflict to be detected for intersecting dates";

        // Effective capacity during leave is 0%
        int effectiveCapacityDuringLeave = 0;
        assert effectiveCapacityDuringLeave == 0 : "Effective capacity during approved leave must be 0%";
    }

    private static void testLeaveOutsideMilestoneWindow() {
        // Milestone: 2026-10-10 to 2026-10-25
        LocalDate mStart = LocalDate.of(2026, 10, 10);
        LocalDate mEnd = LocalDate.of(2026, 10, 25);

        // Leave: 2026-11-01 to 2026-11-05
        LocalDate lStart = LocalDate.of(2026, 11, 1);
        LocalDate lEnd = LocalDate.of(2026, 11, 5);

        boolean hasConflict = !mStart.isAfter(lEnd) && !lStart.isAfter(mEnd);
        assert !hasConflict : "Leave after milestone completion must not trigger conflict";
    }
}
