package com.sist.rms.service;

import com.sist.rms.dto.SkillGapResponse;
import com.sist.rms.entity.*;
import com.sist.rms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Case 4 of the specification.
 *
 * This service does read the database, so the repositories are replaced by
 * Mockito mocks - fake objects that simply return the rows we tell them to.
 */
class SkillGapServiceTest {

    private MilestoneRepository milestoneRepository;
    private MilestoneSkillRepository milestoneSkillRepository;
    private ResourceAllocationRepository allocationRepository;
    private DeveloperSkillRepository developerSkillRepository;
    private DeveloperRepository developerRepository;
    private SkillGapService skillGapService;

    private final Skill java = TestData.skill(1L, "Java");
    private final Skill python = TestData.skill(2L, "Python");
    private final Skill mysql = TestData.skill(3L, "MySQL");
    private final Skill aws = TestData.skill(4L, "AWS");

    private Project project;
    private Milestone milestone;
    private Developer arun;

    @BeforeEach
    void setUp() {
        milestoneRepository = mock(MilestoneRepository.class);
        milestoneSkillRepository = mock(MilestoneSkillRepository.class);
        allocationRepository = mock(ResourceAllocationRepository.class);
        developerSkillRepository = mock(DeveloperSkillRepository.class);
        developerRepository = mock(DeveloperRepository.class);

        skillGapService = new SkillGapService(milestoneRepository, milestoneSkillRepository,
                allocationRepository, developerSkillRepository, developerRepository);

        project = TestData.project(1L, "Hospital Management Portal");
        milestone = TestData.milestone(10L, project, "Cloud Deployment",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 15);
        arun = TestData.developer(1L, "Arun", 100, 500);
    }

    @Test
    @DisplayName("CASE 4: the milestone needs AWS, nobody has AWS -> 'No available developer has this skill.'")
    void case4_nobodyHasTheSkill() {
        // the milestone requires AWS
        when(milestoneSkillRepository.findByMilestoneId(10L))
                .thenReturn(List.of(TestData.milestoneSkill(milestone, aws)));

        // Arun is allocated to it, and he knows Java, Python and MySQL
        when(allocationRepository.findByMilestoneId(10L))
                .thenReturn(List.of(TestData.allocation(arun, project, "2026-11-01", "2026-11-30", 60)));
        when(developerSkillRepository.findByDeveloperId(1L)).thenReturn(List.of(
                TestData.developerSkill(arun, java),
                TestData.developerSkill(arun, python),
                TestData.developerSkill(arun, mysql)));

        // nobody else exists in the company either
        when(developerRepository.findByStatus(DeveloperStatus.ACTIVE)).thenReturn(List.of(arun));

        SkillGapResponse gap = skillGapService.analyseMilestone(milestone);

        assertEquals(List.of("AWS"), gap.getMissingSkills());
        assertTrue(gap.getMessage().startsWith("SKILL GAP"));
        assertEquals(1, gap.getSuggestions().size());
        assertTrue(gap.getSuggestions().get(0)
                .contains("No available developer has this skill."));
    }

    @Test
    @DisplayName("When somebody else has the missing skill, that person is suggested")
    void suggestsADeveloperWhoHasTheSkill() {
        Developer karthik = TestData.developer(5L, "Karthik", 100, 700);

        when(milestoneSkillRepository.findByMilestoneId(10L))
                .thenReturn(List.of(TestData.milestoneSkill(milestone, aws)));
        when(allocationRepository.findByMilestoneId(10L))
                .thenReturn(List.of(TestData.allocation(arun, project, "2026-11-01", "2026-11-30", 60)));
        when(developerSkillRepository.findByDeveloperId(1L))
                .thenReturn(List.of(TestData.developerSkill(arun, java)));
        when(developerSkillRepository.findByDeveloperId(5L))
                .thenReturn(List.of(TestData.developerSkill(karthik, aws)));
        when(developerRepository.findByStatus(DeveloperStatus.ACTIVE))
                .thenReturn(List.of(arun, karthik));

        SkillGapResponse gap = skillGapService.analyseMilestone(milestone);

        assertEquals(List.of("AWS"), gap.getMissingSkills());
        assertTrue(gap.getSuggestions().get(0).contains("Karthik"));
    }

    @Test
    @DisplayName("Two developers together can cover every required skill -> no gap")
    void teamCoversAllSkills() {
        Developer divya = TestData.developer(4L, "Divya", 100, 550);

        when(milestoneSkillRepository.findByMilestoneId(10L)).thenReturn(List.of(
                TestData.milestoneSkill(milestone, java),
                TestData.milestoneSkill(milestone, mysql)));
        when(allocationRepository.findByMilestoneId(10L)).thenReturn(List.of(
                TestData.allocation(arun, project, "2026-11-01", "2026-11-30", 60),
                TestData.allocation(divya, project, "2026-11-01", "2026-11-30", 40)));
        when(developerSkillRepository.findByDeveloperId(1L))
                .thenReturn(List.of(TestData.developerSkill(arun, java)));
        when(developerSkillRepository.findByDeveloperId(4L))
                .thenReturn(List.of(TestData.developerSkill(divya, mysql)));

        SkillGapResponse gap = skillGapService.analyseMilestone(milestone);

        assertTrue(gap.getMissingSkills().isEmpty());
        assertEquals("All required skills are covered.", gap.getMessage());
    }

    @Test
    @DisplayName("Skill match is the share of required skills the developer has")
    void skillMatchPercentage() {
        when(milestoneSkillRepository.findByMilestoneId(10L)).thenReturn(List.of(
                TestData.milestoneSkill(milestone, java),
                TestData.milestoneSkill(milestone, mysql),
                TestData.milestoneSkill(milestone, aws)));
        when(developerSkillRepository.findByDeveloperId(1L)).thenReturn(List.of(
                TestData.developerSkill(arun, java),
                TestData.developerSkill(arun, mysql)));

        // 2 of 3 required skills = 67 %
        assertEquals(67, skillGapService.skillMatchPercentage(1L, 10L));
        assertEquals(List.of("AWS"), skillGapService.missingSkillsForDeveloper(1L, 10L));
    }

    @Test
    @DisplayName("A milestone with no required skills counts as a 100% match")
    void noRequiredSkillsIsAFullMatch() {
        when(milestoneSkillRepository.findByMilestoneId(10L)).thenReturn(List.of());
        assertEquals(100, skillGapService.skillMatchPercentage(1L, 10L));
    }
}
