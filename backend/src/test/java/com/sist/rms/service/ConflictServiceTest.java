package com.sist.rms.service;

import com.sist.rms.dto.ConflictResponse;
import com.sist.rms.entity.Developer;
import com.sist.rms.entity.DeveloperLeave;
import com.sist.rms.entity.Project;
import com.sist.rms.entity.ResourceAllocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cases 1, 2, 3 and 5 of the specification.
 *
 * detectOverAllocation and detectLeaveConflicts are pure calculations on
 * plain lists, so the three repositories are not used at all here and the
 * test needs no database.
 */
class ConflictServiceTest {

    private final ConflictService conflictService = new ConflictService(null, null, null);

    private final Project projectA = TestData.project(1L, "Project A");
    private final Project projectB = TestData.project(2L, "Project B");

    @Test
    @DisplayName("CASE 1: 60% + 50% on the same dates = 110% -> OVER-ALLOCATION CONFLICT")
    void case1_overAllocation() {
        Developer arun = TestData.developer(1L, "Arun", 100, 500);
        List<ResourceAllocation> allocations = List.of(
                TestData.allocation(arun, projectA, "2026-01-01", "2026-01-31", 60),
                TestData.allocation(arun, projectB, "2026-01-01", "2026-01-31", 50));

        List<ConflictResponse> conflicts = conflictService.detectOverAllocation(arun, allocations);

        assertEquals(1, conflicts.size(), "one conflict is expected");
        ConflictResponse conflict = conflicts.get(0);
        assertEquals("OVER_ALLOCATION", conflict.getType());
        assertEquals(110, conflict.getTotalAllocation());
        assertEquals(LocalDate.of(2026, 1, 1), conflict.getStartDate());
        assertEquals(LocalDate.of(2026, 1, 31), conflict.getEndDate());
        assertTrue(conflict.getMessage().contains("110%"));
    }

    @Test
    @DisplayName("CASE 2: 60% + 40% on the same dates = 100% -> no conflict")
    void case2_exactlyFull() {
        Developer arun = TestData.developer(1L, "Arun", 100, 500);
        List<ResourceAllocation> allocations = List.of(
                TestData.allocation(arun, projectA, "2026-01-01", "2026-01-31", 60),
                TestData.allocation(arun, projectB, "2026-01-01", "2026-01-31", 40));

        assertTrue(conflictService.detectOverAllocation(arun, allocations).isEmpty(),
                "100% is exactly the availability, so it is not an over-allocation");
    }

    @Test
    @DisplayName("CASE 3: 60% and 50% on dates that do NOT overlap -> no conflict")
    void case3_noOverlap() {
        Developer arun = TestData.developer(1L, "Arun", 100, 500);
        List<ResourceAllocation> allocations = List.of(
                TestData.allocation(arun, projectA, "2026-01-01", "2026-01-10", 70),
                TestData.allocation(arun, projectB, "2026-01-20", "2026-01-30", 70));

        assertTrue(conflictService.detectOverAllocation(arun, allocations).isEmpty(),
                "the two allocations never run at the same time");
    }

    @Test
    @DisplayName("The conflict only covers the days that actually overlap")
    void conflictCoversOnlyTheOverlappingDays() {
        Developer arun = TestData.developer(1L, "Arun", 100, 500);
        List<ResourceAllocation> allocations = List.of(
                TestData.allocation(arun, projectA, "2026-01-01", "2026-01-20", 60),
                TestData.allocation(arun, projectB, "2026-01-10", "2026-01-31", 60));

        List<ConflictResponse> conflicts = conflictService.detectOverAllocation(arun, allocations);

        assertEquals(1, conflicts.size());
        assertEquals(LocalDate.of(2026, 1, 10), conflicts.get(0).getStartDate());
        assertEquals(LocalDate.of(2026, 1, 20), conflicts.get(0).getEndDate());
        assertEquals(120, conflicts.get(0).getTotalAllocation());
    }

    @Test
    @DisplayName("A part-time developer at 50% availability is over-allocated at 60%")
    void availabilityBelowHundredIsRespected() {
        Developer partTime = TestData.developer(1L, "Divya", 50, 550);
        List<ResourceAllocation> allocations = List.of(
                TestData.allocation(partTime, projectA, "2026-01-01", "2026-01-31", 60));

        List<ConflictResponse> conflicts = conflictService.detectOverAllocation(partTime, allocations);

        assertEquals(1, conflicts.size());
        assertEquals(60, conflicts.get(0).getTotalAllocation());
        assertEquals(50, conflicts.get(0).getAvailability());
    }

    @Test
    @DisplayName("CASE 5: 80% allocated during leave 10-14 September -> LEAVE CONFLICT")
    void case5_leaveConflict() {
        Developer priya = TestData.developer(2L, "Priya", 100, 650);
        List<ResourceAllocation> allocations = List.of(
                TestData.allocation(priya, projectA, "2026-09-05", "2026-10-05", 80));
        List<DeveloperLeave> leaves = List.of(
                TestData.leave(priya, "2026-09-10", "2026-09-14"));

        List<ConflictResponse> conflicts = conflictService.detectLeaveConflicts(priya, allocations, leaves);

        assertEquals(1, conflicts.size());
        ConflictResponse conflict = conflicts.get(0);
        assertEquals("LEAVE_CONFLICT", conflict.getType());
        assertEquals(LocalDate.of(2026, 9, 10), conflict.getStartDate(), "only the overlapping days");
        assertEquals(LocalDate.of(2026, 9, 14), conflict.getEndDate());
        assertTrue(conflict.getMessage().contains("on leave"));
    }

    @Test
    @DisplayName("Leave that does not overlap any allocation raises nothing")
    void leaveOutsideAllocationIsFine() {
        Developer arun = TestData.developer(1L, "Arun", 100, 500);
        List<ResourceAllocation> allocations = List.of(
                TestData.allocation(arun, projectA, "2026-09-01", "2026-09-30", 60));
        List<DeveloperLeave> leaves = List.of(
                TestData.leave(arun, "2026-10-05", "2026-10-08"));

        assertTrue(conflictService.detectLeaveConflicts(arun, allocations, leaves).isEmpty());
    }

    @Test
    @DisplayName("Peak allocation is the highest simultaneous total, not the sum of everything")
    void peakAllocationIgnoresNonOverlappingWork() {
        Developer arun = TestData.developer(1L, "Arun", 100, 500);
        List<ResourceAllocation> allocations = List.of(
                TestData.allocation(arun, projectA, "2026-01-01", "2026-01-10", 70),
                TestData.allocation(arun, projectB, "2026-02-01", "2026-02-10", 70));

        assertEquals(70, conflictService.peakAllocation(allocations));
    }
}
