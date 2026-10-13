package edu.sist.prs.engine;

import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.engine.model.CandidateRank;
import edu.sist.prs.engine.model.MilestoneSnapshot;
import edu.sist.prs.engine.model.ResourceSkillSnapshot;
import edu.sist.prs.engine.model.ResourceSnapshot;
import edu.sist.prs.engine.model.SkillRequirementSnapshot;
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
import static org.assertj.core.api.Assertions.within;

class SkillMatcherTest {

    @Test
    @DisplayName("Skill match worked example: Java 2, Spring Boot 2, MySQL 2; Arun has Java 3, Spring Boot 2, Python 2 -> 66.67%, missing MySQL")
    void testSkillMatchWorkedExample() {
        SkillRequirementSnapshot reqJava = new SkillRequirementSnapshot(1L, "Java", 2, true);
        SkillRequirementSnapshot reqSpring = new SkillRequirementSnapshot(2L, "Spring Boot", 2, true);
        SkillRequirementSnapshot reqMysql = new SkillRequirementSnapshot(3L, "MySQL", 2, true);

        MilestoneSnapshot milestone = new MilestoneSnapshot(
                10L, 1L, "Backend API", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31),
                MilestoneStatus.IN_PROGRESS, 20, BigDecimal.valueOf(300),
                List.of(reqJava, reqSpring, reqMysql)
        );

        ResourceSkillSnapshot sJava = new ResourceSkillSnapshot(1L, "Java", 3);
        ResourceSkillSnapshot sSpring = new ResourceSkillSnapshot(2L, "Spring Boot", 2);
        ResourceSkillSnapshot sPython = new ResourceSkillSnapshot(4L, "Python", 2);

        ResourceSnapshot arun = new ResourceSnapshot(
                1L, ResourceType.DEVELOPER, "Arun Kumar", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(500), CostUnit.HOUR, null, null,
                List.of(sJava, sSpring, sPython)
        );

        double match = SkillMatcher.calculateMatchPercentage(arun, milestone);
        List<String> missing = SkillMatcher.findMissingSkills(arun, milestone);

        // 2 out of 3 skills satisfied -> 66.67%
        assertThat(match).isCloseTo(66.6667, within(0.01));
        assertThat(missing).containsExactly("MySQL");
    }

    @Test
    @DisplayName("Empty skill requirements yields 100% match")
    void testEmptyRequirements() {
        MilestoneSnapshot milestone = new MilestoneSnapshot(
                10L, 1L, "General Task", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31),
                MilestoneStatus.IN_PROGRESS, 20, BigDecimal.valueOf(300),
                List.of()
        );

        ResourceSnapshot dev = new ResourceSnapshot(
                1L, ResourceType.DEVELOPER, "Dev", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(500), CostUnit.HOUR, null, null, List.of()
        );

        assertThat(SkillMatcher.calculateMatchPercentage(dev, milestone)).isEqualTo(100.0);
        assertThat(SkillMatcher.findMissingSkills(dev, milestone)).isEmpty();
    }

    @Test
    @DisplayName("Partial credit: Required min 2, resource has 1 -> credit 0.5")
    void testPartialCredit() {
        SkillRequirementSnapshot req = new SkillRequirementSnapshot(1L, "Java", 2, true);
        ResourceSkillSnapshot skill = new ResourceSkillSnapshot(1L, "Java", 1);

        ResourceSnapshot dev = new ResourceSnapshot(
                1L, ResourceType.DEVELOPER, "Junior", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(300), CostUnit.HOUR, null, null, List.of(skill)
        );

        double credit = SkillMatcher.calculateCredit(dev, req);
        assertThat(credit).isEqualTo(0.5);
    }

    @Test
    @DisplayName("Candidate Ranking worked example: Karthik 0.800, Arun 0.683 (±0.001), order Karthik then Arun")
    void testCandidateRankingWorkedExample() {
        LocalDate start = LocalDate.of(2027, 1, 4); // Monday
        LocalDate end = LocalDate.of(2027, 1, 29);  // Friday

        SkillRequirementSnapshot reqJava = new SkillRequirementSnapshot(1L, "Java", 2, true);
        SkillRequirementSnapshot reqSpring = new SkillRequirementSnapshot(2L, "Spring Boot", 2, true);
        SkillRequirementSnapshot reqMysql = new SkillRequirementSnapshot(3L, "MySQL", 2, true);

        MilestoneSnapshot milestone = new MilestoneSnapshot(
                10L, 1L, "Backend API", start, end,
                MilestoneStatus.IN_PROGRESS, 20, BigDecimal.valueOf(300),
                List.of(reqJava, reqSpring, reqMysql)
        );

        // Arun: Java 3, Spring Boot 2, Python 2 -> match 66.67%, ₹500, free 30% (allocated 70%)
        ResourceSnapshot arun = new ResourceSnapshot(
                1L, ResourceType.DEVELOPER, "Arun Kumar", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(500), CostUnit.HOUR, null, null,
                List.of(new ResourceSkillSnapshot(1L, "Java", 3), new ResourceSkillSnapshot(2L, "Spring Boot", 2))
        );
        AllocationSnapshot arunAlloc = new AllocationSnapshot(
                1L, 1L, 99L, 1L, start, end, 70, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );

        // Karthik: Java 3, Spring Boot 3, MySQL 2 -> match 100%, ₹650, free 60% (allocated 40%)
        ResourceSnapshot karthik = new ResourceSnapshot(
                2L, ResourceType.DEVELOPER, "Karthik Raj", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(650), CostUnit.HOUR, null, null,
                List.of(
                        new ResourceSkillSnapshot(1L, "Java", 3),
                        new ResourceSkillSnapshot(2L, "Spring Boot", 3),
                        new ResourceSkillSnapshot(3L, "MySQL", 2)
                )
        );
        AllocationSnapshot karthikAlloc = new AllocationSnapshot(
                2L, 2L, 99L, 1L, start, end, 40, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );

        List<CandidateRank> ranks = SkillMatcher.rankCandidates(
                milestone,
                60, // P = 60%
                List.of(arun, karthik),
                List.of(arunAlloc, karthikAlloc),
                List.of(),
                0.5, 0.3, 0.2, // w_skill, w_fit, w_cost
                start
        );

        assertThat(ranks).hasSize(2);

        CandidateRank first = ranks.get(0);
        CandidateRank second = ranks.get(1);

        assertThat(first.resourceName()).isEqualTo("Karthik Raj");
        assertThat(first.totalScore()).isCloseTo(0.800, within(0.001));
        assertThat(first.wouldOverload()).isFalse();

        assertThat(second.resourceName()).isEqualTo("Arun Kumar");
        assertThat(second.totalScore()).isCloseTo(0.683, within(0.001));
        assertThat(second.wouldOverload()).isTrue(); // minRemaining 30 < 60
    }

    @Test
    @DisplayName("Candidate Ranking with equal rates yields cheapness 1.0 (costNorm 0)")
    void testCandidateRankingEqualRates() {
        LocalDate start = LocalDate.of(2027, 1, 4);
        LocalDate end = LocalDate.of(2027, 1, 8);

        MilestoneSnapshot milestone = new MilestoneSnapshot(
                10L, 1L, "Task", start, end,
                MilestoneStatus.IN_PROGRESS, 0, BigDecimal.valueOf(100),
                List.of()
        );

        ResourceSnapshot d1 = new ResourceSnapshot(
                1L, ResourceType.DEVELOPER, "D1", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(500), CostUnit.HOUR, null, null, List.of()
        );
        ResourceSnapshot d2 = new ResourceSnapshot(
                2L, ResourceType.DEVELOPER, "D2", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(500), CostUnit.HOUR, null, null, List.of()
        );

        List<CandidateRank> ranks = SkillMatcher.rankCandidates(
                milestone, 50, List.of(d1, d2), List.of(), List.of(),
                0.5, 0.3, 0.2, start
        );

        assertThat(ranks).allMatch(r -> r.cheapnessScore() == 1.0);
    }
}
