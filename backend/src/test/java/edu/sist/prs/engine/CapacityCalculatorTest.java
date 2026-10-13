package edu.sist.prs.engine;

import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.engine.model.OverAllocationPeriod;
import edu.sist.prs.engine.model.ResourceSnapshot;
import edu.sist.prs.engine.model.UnavailabilitySnapshot;
import edu.sist.prs.entity.ConflictSeverity;
import edu.sist.prs.entity.CostUnit;
import edu.sist.prs.entity.MilestoneStatus;
import edu.sist.prs.entity.ProjectStatus;
import edu.sist.prs.entity.ResourceStatus;
import edu.sist.prs.entity.ResourceType;
import edu.sist.prs.entity.UnavailabilityStatus;
import edu.sist.prs.entity.UnavailabilityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CapacityCalculatorTest {

    @Test
    @DisplayName("Example 1 (AC2 & Clarification 6): Arun Developer vs Hardware over-allocation")
    void testExample1OverAllocation() {
        LocalDate jan1 = LocalDate.of(2027, 1, 1);
        LocalDate jan9 = LocalDate.of(2027, 1, 9);
        LocalDate jan10 = LocalDate.of(2027, 1, 10); // Sunday
        LocalDate jan11 = LocalDate.of(2027, 1, 11); // Monday
        LocalDate jan20 = LocalDate.of(2027, 1, 20); // Wednesday
        LocalDate jan21 = LocalDate.of(2027, 1, 21);
        LocalDate jan25 = LocalDate.of(2027, 1, 25);

        ResourceSnapshot arunDev = new ResourceSnapshot(
                1L, ResourceType.DEVELOPER, "Arun Kumar", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(500), CostUnit.HOUR, null, null, List.of()
        );

        ResourceSnapshot hardware = new ResourceSnapshot(
                2L, ResourceType.HARDWARE, "GPU Workstation", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(2000), CostUnit.DAY, null, null, List.of()
        );

        AllocationSnapshot allocA = new AllocationSnapshot(
                101L, 1L, 10L, 1L, jan1, jan20, 60, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );
        AllocationSnapshot allocB = new AllocationSnapshot(
                102L, 1L, 11L, 2L, jan10, jan25, 50, true,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );
        List<AllocationSnapshot> devAllocs = List.of(allocA, allocB);

        // Verify daily loads for Arun
        assertThat(CapacityCalculator.calculateLoad(1L, jan1, devAllocs)).isEqualTo(60);
        assertThat(CapacityCalculator.calculateLoad(1L, jan9, devAllocs)).isEqualTo(60);
        assertThat(CapacityCalculator.calculateLoad(1L, jan10, devAllocs)).isEqualTo(110);
        assertThat(CapacityCalculator.calculateLoad(1L, jan11, devAllocs)).isEqualTo(110);
        assertThat(CapacityCalculator.calculateLoad(1L, jan20, devAllocs)).isEqualTo(110);
        assertThat(CapacityCalculator.calculateLoad(1L, jan21, devAllocs)).isEqualTo(50);
        assertThat(CapacityCalculator.calculateLoad(1L, jan25, devAllocs)).isEqualTo(50);

        // 1. DEVELOPER test: 10 Jan is Sunday (capacity 0), so available working days period is 11 Jan to 20 Jan
        List<OverAllocationPeriod> devPeriods = CapacityCalculator.findOverAllocationPeriods(
                arunDev, jan1, jan25, devAllocs, List.of()
        );
        assertThat(devPeriods).hasSize(1);
        OverAllocationPeriod devP = devPeriods.get(0);
        assertThat(devP.startDate()).isEqualTo(jan11); // Monday
        assertThat(devP.endDate()).isEqualTo(jan20);   // Wednesday
        assertThat(devP.peakLoad()).isEqualTo(110);
        assertThat(devP.severity()).isEqualTo(ConflictSeverity.CRITICAL);

        // 2. HARDWARE test: Calendar days (no weekend rule), period is exactly 10 Jan to 20 Jan
        AllocationSnapshot hwAllocA = new AllocationSnapshot(
                201L, 2L, 10L, 1L, jan1, jan20, 60, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );
        AllocationSnapshot hwAllocB = new AllocationSnapshot(
                202L, 2L, 11L, 2L, jan10, jan25, 50, true,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );
        List<AllocationSnapshot> hwAllocs = List.of(hwAllocA, hwAllocB);

        List<OverAllocationPeriod> hwPeriods = CapacityCalculator.findOverAllocationPeriods(
                hardware, jan1, jan25, hwAllocs, List.of()
        );
        assertThat(hwPeriods).hasSize(1);
        OverAllocationPeriod hwP = hwPeriods.get(0);
        assertThat(hwP.startDate()).isEqualTo(jan10); // Sunday included for calendar-day hardware
        assertThat(hwP.endDate()).isEqualTo(jan20);
        assertThat(hwP.peakLoad()).isEqualTo(110);
        assertThat(hwP.severity()).isEqualTo(ConflictSeverity.CRITICAL);
    }

    @Test
    @DisplayName("Exactly 100% capacity and load produces no conflict")
    void testExactly100PercentNoConflict() {
        LocalDate start = LocalDate.of(2027, 2, 1);
        LocalDate end = LocalDate.of(2027, 2, 5);

        ResourceSnapshot res = new ResourceSnapshot(
                1L, ResourceType.DEVELOPER, "Dev", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(500), CostUnit.HOUR, null, null, List.of()
        );

        AllocationSnapshot alloc = new AllocationSnapshot(
                1L, 1L, 10L, 1L, start, end, 100, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );

        List<OverAllocationPeriod> periods = CapacityCalculator.findOverAllocationPeriods(
                res, start, end, List.of(alloc), List.of()
        );
        assertThat(periods).isEmpty();
        assertThat(CapacityCalculator.calculateRemaining(res, start, List.of(alloc), List.of())).isEqualTo(0);
    }

    @Test
    @DisplayName("Half-time person (capacity 50) at 70% produces WARNING")
    void testHalfTimeOverAllocationWarning() {
        LocalDate start = LocalDate.of(2027, 2, 1); // Monday
        LocalDate end = LocalDate.of(2027, 2, 5);   // Friday

        ResourceSnapshot halfTimeDev = new ResourceSnapshot(
                1L, ResourceType.DEVELOPER, "Half-timer", ResourceStatus.ACTIVE,
                50, BigDecimal.valueOf(500), CostUnit.HOUR, null, null, List.of()
        );

        AllocationSnapshot alloc = new AllocationSnapshot(
                1L, 1L, 10L, 1L, start, end, 70, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );

        List<OverAllocationPeriod> periods = CapacityCalculator.findOverAllocationPeriods(
                halfTimeDev, start, end, List.of(alloc), List.of()
        );
        assertThat(periods).hasSize(1);
        OverAllocationPeriod p = periods.get(0);
        assertThat(p.peakLoad()).isEqualTo(70);
        assertThat(p.severity()).isEqualTo(ConflictSeverity.WARNING);
    }

    @Test
    @DisplayName("AC1 Example: Arun 40% on Project A and 35% on Project B -> load 75, remaining 25")
    void testAc1Example() {
        LocalDate date = LocalDate.of(2027, 3, 1); // Monday

        ResourceSnapshot arun = new ResourceSnapshot(
                1L, ResourceType.DEVELOPER, "Arun Kumar", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(500), CostUnit.HOUR, null, null, List.of()
        );

        AllocationSnapshot a1 = new AllocationSnapshot(
                1L, 1L, 10L, 1L, date, date, 40, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );
        AllocationSnapshot a2 = new AllocationSnapshot(
                2L, 1L, 11L, 2L, date, date, 35, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );

        List<AllocationSnapshot> allocs = List.of(a1, a2);
        int load = CapacityCalculator.calculateLoad(1L, date, allocs);
        int remaining = CapacityCalculator.calculateRemaining(arun, date, allocs, List.of());

        assertThat(load).isEqualTo(75);
        assertThat(remaining).isEqualTo(25);
    }

    @Test
    @DisplayName("Priya Example: 50% + 30% -> load 80, remaining 20, no conflict")
    void testPriyaExample() {
        LocalDate date = LocalDate.of(2027, 3, 1); // Monday

        ResourceSnapshot priya = new ResourceSnapshot(
                2L, ResourceType.DEVELOPER, "Priya Sharma", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(550), CostUnit.HOUR, null, null, List.of()
        );

        AllocationSnapshot a1 = new AllocationSnapshot(
                1L, 2L, 10L, 1L, date, date, 50, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );
        AllocationSnapshot a2 = new AllocationSnapshot(
                2L, 2L, 11L, 2L, date, date, 30, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );

        List<AllocationSnapshot> allocs = List.of(a1, a2);
        int load = CapacityCalculator.calculateLoad(2L, date, allocs);
        int remaining = CapacityCalculator.calculateRemaining(priya, date, allocs, List.of());
        List<OverAllocationPeriod> periods = CapacityCalculator.findOverAllocationPeriods(priya, date, date, allocs, List.of());

        assertThat(load).isEqualTo(80);
        assertThat(remaining).isEqualTo(20);
        assertThat(periods).isEmpty();
    }

    @Test
    @DisplayName("Weekend rule: Developer capacity is 0 on Sat/Sun; Hardware capacity is intact")
    void testWeekendRule() {
        LocalDate saturday = LocalDate.of(2027, 1, 9);
        LocalDate sunday = LocalDate.of(2027, 1, 10);
        LocalDate monday = LocalDate.of(2027, 1, 11);

        ResourceSnapshot dev = new ResourceSnapshot(
                1L, ResourceType.DEVELOPER, "Dev", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(500), CostUnit.HOUR, null, null, List.of()
        );
        ResourceSnapshot hw = new ResourceSnapshot(
                2L, ResourceType.HARDWARE, "HW", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(2000), CostUnit.DAY, null, null, List.of()
        );

        assertThat(CapacityCalculator.calculateCapacity(dev, saturday, List.of())).isEqualTo(0);
        assertThat(CapacityCalculator.calculateCapacity(dev, sunday, List.of())).isEqualTo(0);
        assertThat(CapacityCalculator.calculateCapacity(dev, monday, List.of())).isEqualTo(100);

        assertThat(CapacityCalculator.calculateCapacity(hw, saturday, List.of())).isEqualTo(100);
        assertThat(CapacityCalculator.calculateCapacity(hw, sunday, List.of())).isEqualTo(100);
        assertThat(CapacityCalculator.calculateCapacity(hw, monday, List.of())).isEqualTo(100);
    }

    @Test
    @DisplayName("Rule R9: Completed milestones and completed/archived projects are excluded from load")
    void testRuleR9Exclusion() {
        LocalDate date = LocalDate.of(2027, 1, 11);

        AllocationSnapshot completedMilestone = new AllocationSnapshot(
                1L, 1L, 10L, 1L, date, date, 50, false,
                MilestoneStatus.COMPLETED, ProjectStatus.ACTIVE
        );
        AllocationSnapshot completedProject = new AllocationSnapshot(
                2L, 1L, 11L, 2L, date, date, 50, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.COMPLETED
        );
        AllocationSnapshot activeAlloc = new AllocationSnapshot(
                3L, 1L, 12L, 3L, date, date, 30, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );

        int load = CapacityCalculator.calculateLoad(1L, date, List.of(completedMilestone, completedProject, activeAlloc));
        assertThat(load).isEqualTo(30);
    }

    @Test
    @DisplayName("Approved unavailability reduces capacity to 0; rejected unavailability does not")
    void testUnavailabilityCapacity() {
        LocalDate leaveDay = LocalDate.of(2027, 1, 12); // Tuesday

        ResourceSnapshot dev = new ResourceSnapshot(
                1L, ResourceType.DEVELOPER, "Dev", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(500), CostUnit.HOUR, null, null, List.of()
        );

        UnavailabilitySnapshot approvedLeave = new UnavailabilitySnapshot(
                1L, 1L, leaveDay, leaveDay, UnavailabilityType.LEAVE, UnavailabilityStatus.APPROVED
        );
        UnavailabilitySnapshot rejectedLeave = new UnavailabilitySnapshot(
                2L, 1L, leaveDay, leaveDay, UnavailabilityType.LEAVE, UnavailabilityStatus.REJECTED
        );

        assertThat(CapacityCalculator.calculateCapacity(dev, leaveDay, List.of(approvedLeave))).isEqualTo(0);
        assertThat(CapacityCalculator.calculateCapacity(dev, leaveDay, List.of(rejectedLeave))).isEqualTo(100);
    }
}
