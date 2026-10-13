package edu.sist.prs.engine;

import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.engine.model.DetectedConflict;
import edu.sist.prs.engine.model.MilestoneSnapshot;
import edu.sist.prs.engine.model.ResourceSkillSnapshot;
import edu.sist.prs.engine.model.ResourceSnapshot;
import edu.sist.prs.engine.model.SkillRequirementSnapshot;
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

class ConflictRulesTest {

    @Test
    @DisplayName("Skill gap: AWS 3 required, holders only at level 2 -> CRITICAL SKILL_GAP")
    void testSkillGapCritical() {
        SkillRequirementSnapshot reqAws = new SkillRequirementSnapshot(1L, "AWS", 3, true);
        MilestoneSnapshot milestone = new MilestoneSnapshot(
                10L, 1L, "Cloud Auto", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 28),
                MilestoneStatus.PLANNED, 0, BigDecimal.valueOf(100),
                List.of(reqAws)
        );

        // Developer has AWS at level 2, not level 3
        ResourceSnapshot dev = new ResourceSnapshot(
                1L, ResourceType.DEVELOPER, "Divya", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(600), CostUnit.HOUR, null, null,
                List.of(new ResourceSkillSnapshot(1L, "AWS", 2))
        );

        List<DetectedConflict> conflicts = ConflictRules.evaluateConflicts(
                List.of(dev),
                List.of(milestone),
                List.of(),
                List.of(),
                LocalDate.of(2027, 2, 1),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2027, 1, 15),
                85.0, 3, 50.0, 14
        );

        assertThat(conflicts).anyMatch(c ->
                c.type().equals(ConflictTypeConstants.SKILL_GAP) &&
                c.severity() == ConflictSeverity.CRITICAL &&
                c.dedupKey().equals("SKILLGAP:10:1")
        );
    }

    @Test
    @DisplayName("Skill gap: Holder exists at level 3, but is fully booked -> WARNING SKILL_GAP")
    void testSkillGapWarningFullyBooked() {
        LocalDate start = LocalDate.of(2027, 2, 1); // Monday
        LocalDate end = LocalDate.of(2027, 2, 5);   // Friday

        SkillRequirementSnapshot req = new SkillRequirementSnapshot(1L, "AWS", 3, true);
        MilestoneSnapshot milestone = new MilestoneSnapshot(
                10L, 1L, "Cloud Auto", start, end,
                MilestoneStatus.PLANNED, 0, BigDecimal.valueOf(100),
                List.of(req)
        );

        ResourceSnapshot dev = new ResourceSnapshot(
                1L, ResourceType.DEVELOPER, "Divya", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(600), CostUnit.HOUR, null, null,
                List.of(new ResourceSkillSnapshot(1L, "AWS", 3))
        );

        // Fully booked with 100% on another milestone across the dates
        AllocationSnapshot otherAlloc = new AllocationSnapshot(
                99L, 1L, 999L, 2L, start, end, 100, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );

        List<DetectedConflict> conflicts = ConflictRules.evaluateConflicts(
                List.of(dev),
                List.of(milestone),
                List.of(otherAlloc),
                List.of(),
                start, end, start.minusDays(10),
                85.0, 3, 50.0, 14
        );

        assertThat(conflicts).anyMatch(c ->
                c.type().equals(ConflictTypeConstants.SKILL_GAP) &&
                c.severity() == ConflictSeverity.WARNING &&
                c.dedupKey().equals("SKILLGAP:10:1")
        );
    }

    @Test
    @DisplayName("Leave Example 4: Rahul 80% on milestone; approved leave 10-14 Sep -> CRITICAL LEAVE_CONFLICT")
    void testLeaveConflictSingle() {
        LocalDate start = LocalDate.of(2026, 9, 10);
        LocalDate end = LocalDate.of(2026, 9, 14);

        ResourceSnapshot rahul = new ResourceSnapshot(
                3L, ResourceType.DEVELOPER, "Rahul Verma", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(450), CostUnit.HOUR, null, null, List.of()
        );

        AllocationSnapshot alloc = new AllocationSnapshot(
                30L, 3L, 5L, 1L, start, end, 80, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );

        UnavailabilitySnapshot leave = new UnavailabilitySnapshot(
                50L, 3L, start, end, UnavailabilityType.LEAVE, UnavailabilityStatus.APPROVED
        );

        List<DetectedConflict> conflicts = ConflictRules.evaluateConflicts(
                List.of(rahul),
                List.of(),
                List.of(alloc),
                List.of(leave),
                start, end, start.minusDays(1),
                85.0, 3, 50.0, 14
        );

        assertThat(conflicts).anyMatch(c ->
                c.type().equals(ConflictTypeConstants.LEAVE_CONFLICT) &&
                c.severity() == ConflictSeverity.CRITICAL &&
                c.dedupKey().equals("LEAVE:30:50")
        );
    }

    @Test
    @DisplayName("Leave over two projects creates two distinct LEAVE_CONFLICTs")
    void testLeaveConflictTwoProjects() {
        LocalDate start = LocalDate.of(2026, 9, 10);
        LocalDate end = LocalDate.of(2026, 9, 14);

        ResourceSnapshot rahul = new ResourceSnapshot(
                3L, ResourceType.DEVELOPER, "Rahul Verma", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(450), CostUnit.HOUR, null, null, List.of()
        );

        AllocationSnapshot allocProj1 = new AllocationSnapshot(
                31L, 3L, 5L, 1L, start, end, 40, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );
        AllocationSnapshot allocProj2 = new AllocationSnapshot(
                32L, 3L, 6L, 2L, start, end, 40, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );

        UnavailabilitySnapshot leave = new UnavailabilitySnapshot(
                50L, 3L, start, end, UnavailabilityType.LEAVE, UnavailabilityStatus.APPROVED
        );

        List<DetectedConflict> conflicts = ConflictRules.evaluateConflicts(
                List.of(rahul),
                List.of(),
                List.of(allocProj1, allocProj2),
                List.of(leave),
                start, end, start.minusDays(1),
                85.0, 3, 50.0, 14
        );

        List<DetectedConflict> leaveConflicts = conflicts.stream()
                .filter(c -> c.type().equals(ConflictTypeConstants.LEAVE_CONFLICT))
                .toList();

        assertThat(leaveConflicts).hasSize(2);
        assertThat(leaveConflicts).extracting(DetectedConflict::dedupKey)
                .containsExactlyInAnyOrder("LEAVE:31:50", "LEAVE:32:50");
    }

    @Test
    @DisplayName("Inactive resource: allocation to INACTIVE resource produces CRITICAL INACTIVE_RESOURCE")
    void testInactiveResourceConflict() {
        LocalDate date = LocalDate.of(2027, 1, 10);

        ResourceSnapshot inactiveDev = new ResourceSnapshot(
                1L, ResourceType.DEVELOPER, "Old Dev", ResourceStatus.INACTIVE,
                100, BigDecimal.valueOf(500), CostUnit.HOUR, null, null, List.of()
        );

        AllocationSnapshot alloc = new AllocationSnapshot(
                15L, 1L, 10L, 1L, date, date, 50, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );

        List<DetectedConflict> conflicts = ConflictRules.evaluateConflicts(
                List.of(inactiveDev),
                List.of(),
                List.of(alloc),
                List.of(),
                date, date, date,
                85.0, 3, 50.0, 14
        );

        assertThat(conflicts).anyMatch(c ->
                c.type().equals(ConflictTypeConstants.INACTIVE_RESOURCE) &&
                c.severity() == ConflictSeverity.CRITICAL &&
                c.dedupKey().equals("INACTIVE:15")
        );
    }

    @Test
    @DisplayName("Skill mismatch: Developer with match < 50% produces WARNING SKILL_MISMATCH")
    void testSkillMismatch() {
        LocalDate date = LocalDate.of(2027, 1, 10);

        SkillRequirementSnapshot reqJava = new SkillRequirementSnapshot(1L, "Java", 2, true);
        SkillRequirementSnapshot reqSpring = new SkillRequirementSnapshot(2L, "Spring Boot", 2, true);
        MilestoneSnapshot milestone = new MilestoneSnapshot(
                10L, 1L, "Backend", date, date.plusDays(10),
                MilestoneStatus.IN_PROGRESS, 0, BigDecimal.valueOf(100),
                List.of(reqJava, reqSpring)
        );

        // Rahul has React, not Java or Spring Boot -> match = 0%
        ResourceSnapshot rahul = new ResourceSnapshot(
                3L, ResourceType.DEVELOPER, "Rahul Verma", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(450), CostUnit.HOUR, null, null,
                List.of(new ResourceSkillSnapshot(3L, "React", 3))
        );

        AllocationSnapshot alloc = new AllocationSnapshot(
                25L, 3L, 10L, 1L, date, date.plusDays(5), 20, true,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );

        List<DetectedConflict> conflicts = ConflictRules.evaluateConflicts(
                List.of(rahul),
                List.of(milestone),
                List.of(alloc),
                List.of(),
                date, date.plusDays(10), date,
                85.0, 3, 50.0, 14
        );

        assertThat(conflicts).anyMatch(c ->
                c.type().equals(ConflictTypeConstants.SKILL_MISMATCH) &&
                c.severity() == ConflictSeverity.WARNING &&
                c.dedupKey().equals("MISMATCH:25")
        );
    }

    @Test
    @DisplayName("Unstaffed milestone: Starting within lookahead window without developer produces WARNING")
    void testUnstaffedMilestone() {
        LocalDate today = LocalDate.of(2027, 1, 1);
        LocalDate start = LocalDate.of(2027, 1, 10); // Within 14 days lookahead
        LocalDate end = LocalDate.of(2027, 1, 31);

        MilestoneSnapshot milestone = new MilestoneSnapshot(
                20L, 1L, "Future Milestone", start, end,
                MilestoneStatus.PLANNED, 0, BigDecimal.valueOf(100),
                List.of()
        );

        List<DetectedConflict> conflicts = ConflictRules.evaluateConflicts(
                List.of(),
                List.of(milestone),
                List.of(),
                List.of(),
                today, end, today,
                85.0, 3, 50.0, 14
        );

        assertThat(conflicts).anyMatch(c ->
                c.type().equals(ConflictTypeConstants.UNSTAFFED_MILESTONE) &&
                c.severity() == ConflictSeverity.WARNING &&
                c.dedupKey().equals("UNSTAFFED:20")
        );
    }
}
