package com.sist.rms.service;

import com.sist.rms.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Small helpers so the tests read like the specification. */
public final class TestData {

    private TestData() {
    }

    public static Developer developer(Long id, String name, int availability, double hourlyCost) {
        Developer developer = new Developer();
        developer.setId(id);
        developer.setName(name);
        developer.setEmail(name.toLowerCase() + "@sist.edu");
        developer.setRole("Developer");
        developer.setHourlyCost(BigDecimal.valueOf(hourlyCost));
        developer.setAvailabilityPercentage(availability);
        developer.setStatus(DeveloperStatus.ACTIVE);
        return developer;
    }

    public static Project project(Long id, String name) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setStartDate(LocalDate.of(2026, 1, 1));
        project.setEndDate(LocalDate.of(2026, 12, 31));
        project.setBudget(BigDecimal.valueOf(500000));
        project.setStatus(ProjectStatus.IN_PROGRESS);
        return project;
    }

    public static Milestone milestone(Long id, Project project, String name,
                                      LocalDate start, LocalDate end, int effortDays) {
        Milestone milestone = new Milestone();
        milestone.setId(id);
        milestone.setProject(project);
        milestone.setName(name);
        milestone.setStartDate(start);
        milestone.setEndDate(end);
        milestone.setEstimatedEffortDays(effortDays);
        milestone.setProgressPercentage(0);
        milestone.setStatus(MilestoneStatus.PLANNED);
        return milestone;
    }

    public static ResourceAllocation allocation(Developer developer, Project project,
                                                String from, String to, int percentage) {
        ResourceAllocation allocation = new ResourceAllocation();
        allocation.setDeveloper(developer);
        allocation.setProject(project);
        allocation.setStartDate(LocalDate.parse(from));
        allocation.setEndDate(LocalDate.parse(to));
        allocation.setAllocationPercentage(percentage);
        return allocation;
    }

    public static DeveloperLeave leave(Developer developer, String from, String to) {
        DeveloperLeave leave = new DeveloperLeave();
        leave.setDeveloper(developer);
        leave.setStartDate(LocalDate.parse(from));
        leave.setEndDate(LocalDate.parse(to));
        leave.setStatus(LeaveStatus.APPROVED);
        return leave;
    }

    public static Skill skill(Long id, String name) {
        Skill skill = new Skill();
        skill.setId(id);
        skill.setName(name);
        return skill;
    }

    public static DeveloperSkill developerSkill(Developer developer, Skill skill) {
        DeveloperSkill link = new DeveloperSkill();
        link.setDeveloper(developer);
        link.setSkill(skill);
        link.setProficiencyLevel(ProficiencyLevel.INTERMEDIATE);
        return link;
    }

    public static MilestoneSkill milestoneSkill(Milestone milestone, Skill skill) {
        MilestoneSkill link = new MilestoneSkill();
        link.setMilestone(milestone);
        link.setSkill(skill);
        return link;
    }
}
