package edu.sist.prs.engine;

import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.engine.model.DetectedConflict;
import edu.sist.prs.engine.model.ResourceSnapshot;
import edu.sist.prs.entity.ConflictSeverity;
import edu.sist.prs.entity.CostUnit;
import edu.sist.prs.entity.MilestoneStatus;
import edu.sist.prs.entity.ProjectStatus;
import edu.sist.prs.entity.ResourceStatus;
import edu.sist.prs.entity.ResourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BurnoutTrackerTest {

    @Test
    @DisplayName("Burnout worked example: Weekly loads 90%, 95%, 88% -> BURNOUT_RISK")
    void testBurnoutRiskDetected() {
        // Week 1: 2027-01-04 (Mon) to 2027-01-10 (Sun) -> 90%
        // Week 2: 2027-01-11 (Mon) to 2027-01-17 (Sun) -> 95%
        // Week 3: 2027-01-18 (Mon) to 2027-01-24 (Sun) -> 88%
        LocalDate w1Mon = LocalDate.of(2027, 1, 4);
        LocalDate w1Sun = LocalDate.of(2027, 1, 10);
        LocalDate w2Mon = LocalDate.of(2027, 1, 11);
        LocalDate w2Sun = LocalDate.of(2027, 1, 17);
        LocalDate w3Mon = LocalDate.of(2027, 1, 18);
        LocalDate w3Sun = LocalDate.of(2027, 1, 24);

        ResourceSnapshot priya = new ResourceSnapshot(
                2L, ResourceType.DEVELOPER, "Priya Sharma", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(550), CostUnit.HOUR, null, null, List.of()
        );

        AllocationSnapshot a1 = new AllocationSnapshot(
                1L, 2L, 10L, 1L, w1Mon, w1Sun, 90, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );
        AllocationSnapshot a2 = new AllocationSnapshot(
                2L, 2L, 10L, 1L, w2Mon, w2Sun, 95, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );
        AllocationSnapshot a3 = new AllocationSnapshot(
                3L, 2L, 10L, 1L, w3Mon, w3Sun, 88, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );

        List<DetectedConflict> conflicts = BurnoutTracker.detectBurnout(
                priya,
                w1Mon,
                w3Sun,
                List.of(a1, a2, a3),
                List.of(),
                85.0, // threshold
                3     // consecutive weeks
        );

        assertThat(conflicts).hasSize(1);
        DetectedConflict c = conflicts.get(0);
        assertThat(c.type()).isEqualTo(ConflictTypeConstants.BURNOUT_RISK);
        assertThat(c.severity()).isEqualTo(ConflictSeverity.WARNING);
        assertThat(c.dedupKey()).isEqualTo("BURNOUT:2:" + w1Mon);
    }

    @Test
    @DisplayName("Burnout worked example: Weekly loads 90%, 95%, 80% -> no risk (streak broken)")
    void testBurnoutStreakBroken() {
        LocalDate w1Mon = LocalDate.of(2027, 1, 4);
        LocalDate w1Sun = LocalDate.of(2027, 1, 10);
        LocalDate w2Mon = LocalDate.of(2027, 1, 11);
        LocalDate w2Sun = LocalDate.of(2027, 1, 17);
        LocalDate w3Mon = LocalDate.of(2027, 1, 18);
        LocalDate w3Sun = LocalDate.of(2027, 1, 24);

        ResourceSnapshot priya = new ResourceSnapshot(
                2L, ResourceType.DEVELOPER, "Priya Sharma", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(550), CostUnit.HOUR, null, null, List.of()
        );

        AllocationSnapshot a1 = new AllocationSnapshot(
                1L, 2L, 10L, 1L, w1Mon, w1Sun, 90, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );
        AllocationSnapshot a2 = new AllocationSnapshot(
                2L, 2L, 10L, 1L, w2Mon, w2Sun, 95, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );
        AllocationSnapshot a3 = new AllocationSnapshot(
                3L, 2L, 10L, 1L, w3Mon, w3Sun, 80, false, // 80 < 85 breaks streak
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );

        List<DetectedConflict> conflicts = BurnoutTracker.detectBurnout(
                priya,
                w1Mon,
                w3Sun,
                List.of(a1, a2, a3),
                List.of(),
                85.0,
                3
        );

        assertThat(conflicts).isEmpty();
    }
}
