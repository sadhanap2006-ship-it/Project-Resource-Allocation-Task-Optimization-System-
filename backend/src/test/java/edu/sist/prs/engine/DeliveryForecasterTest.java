package edu.sist.prs.engine;

import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.engine.model.MilestoneDeliveryForecast;
import edu.sist.prs.engine.model.MilestoneSnapshot;
import edu.sist.prs.engine.model.ResourceSkillSnapshot;
import edu.sist.prs.engine.model.ResourceSnapshot;
import edu.sist.prs.engine.model.SkillRequirementSnapshot;
import edu.sist.prs.entity.CostUnit;
import edu.sist.prs.entity.MilestoneStatus;
import edu.sist.prs.entity.ProjectStatus;
import edu.sist.prs.entity.ResourceStatus;
import edu.sist.prs.entity.ResourceType;
import edu.sist.prs.util.WorkingDays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DeliveryForecasterTest {

    @Test
    @DisplayName("Delivery Worked Example: 320h estimated, 25% done -> 240h left; Arun (60%, match 66.67%) + Karthik (50%, match 100%) -> 8.0 h/day, 30 working days; 5 days delay")
    void testDeliveryWorkedExample() {
        // Milestone start: Monday 2027-01-04
        LocalDate start = LocalDate.of(2027, 1, 4);
        // Deadline 25 working days away: 5 weeks from Mon Jan 4 is Fri Feb 5
        LocalDate deadline = LocalDate.of(2027, 2, 5);

        SkillRequirementSnapshot reqJava = new SkillRequirementSnapshot(1L, "Java", 2, true);
        SkillRequirementSnapshot reqSpring = new SkillRequirementSnapshot(2L, "Spring Boot", 2, true);
        SkillRequirementSnapshot reqMysql = new SkillRequirementSnapshot(3L, "MySQL", 2, true);

        MilestoneSnapshot milestone = new MilestoneSnapshot(
                10L, 1L, "Backend API", start, deadline,
                MilestoneStatus.IN_PROGRESS, 25, BigDecimal.valueOf(320),
                List.of(reqJava, reqSpring, reqMysql)
        );

        // Arun: Java 3, Spring Boot 2 -> match 66.67%
        ResourceSnapshot arun = new ResourceSnapshot(
                1L, ResourceType.DEVELOPER, "Arun Kumar", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(500), CostUnit.HOUR, null, null,
                List.of(new ResourceSkillSnapshot(1L, "Java", 3), new ResourceSkillSnapshot(2L, "Spring Boot", 2))
        );

        // Karthik: Java 3, Spring Boot 3, MySQL 2 -> match 100%
        ResourceSnapshot karthik = new ResourceSnapshot(
                2L, ResourceType.DEVELOPER, "Karthik Raj", ResourceStatus.ACTIVE,
                100, BigDecimal.valueOf(650), CostUnit.HOUR, null, null,
                List.of(
                        new ResourceSkillSnapshot(1L, "Java", 3),
                        new ResourceSkillSnapshot(2L, "Spring Boot", 3),
                        new ResourceSkillSnapshot(3L, "MySQL", 2)
                )
        );

        AllocationSnapshot arunAlloc = new AllocationSnapshot(
                1L, 1L, 10L, 1L, start, start.plusDays(90), 60, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );
        AllocationSnapshot karthikAlloc = new AllocationSnapshot(
                2L, 2L, 10L, 1L, start, start.plusDays(90), 50, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        );

        // Verify speed factors
        double arunEff = DeliveryForecaster.calculateSpeedFactor(arun, milestone);
        double karthikEff = DeliveryForecaster.calculateSpeedFactor(karthik, milestone);
        assertThat(arunEff).isCloseTo(0.8333, within(0.001));
        assertThat(karthikEff).isEqualTo(1.0);

        MilestoneDeliveryForecast forecast = DeliveryForecaster.forecastMilestone(
                milestone,
                List.of(arunAlloc, karthikAlloc),
                List.of(arun, karthik),
                List.of(),
                start, // today = start
                8,
                1825
        );

        // Remaining effort = 320 * (1 - 0.25) = 240h
        assertThat(forecast.remainingEffortHours()).isEqualByComparingTo("240.00");

        // 30 working days from 2027-01-04 (Mon):
        // 6 full 5-day weeks: Week 1 to 6 -> finishes Friday 2027-02-12
        LocalDate expectedFinish = LocalDate.of(2027, 2, 12);
        assertThat(forecast.forecastFinishDate()).isEqualTo(expectedFinish);

        // Working days between Feb 5 (deadline) and Feb 12: Mon Feb 8 to Fri Feb 12 = 5 working days
        assertThat(forecast.delayWorkingDays()).isEqualTo(5);
        assertThat(forecast.forecastStatus()).isEqualTo("DELAYED");
    }

    @Test
    @DisplayName("Milestone with no developer allocations returns NO_CAPACITY")
    void testMilestoneNoAllocations() {
        LocalDate start = LocalDate.of(2027, 1, 4);
        MilestoneSnapshot milestone = new MilestoneSnapshot(
                10L, 1L, "Unstaffed", start, start.plusDays(20),
                MilestoneStatus.PLANNED, 0, BigDecimal.valueOf(160),
                List.of()
        );

        MilestoneDeliveryForecast forecast = DeliveryForecaster.forecastMilestone(
                milestone, List.of(), List.of(), List.of(), start, 8, 1825
        );

        assertThat(forecast.forecastStatus()).isEqualTo("NO_CAPACITY");
        assertThat(forecast.forecastFinishDate()).isNull();
    }
}
