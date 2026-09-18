package com.sist.rms.service;

import com.sist.rms.dto.*;
import com.sist.rms.entity.*;
import com.sist.rms.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds both dashboards in a single request each, so the JavaScript only
 * has to call one endpoint and draw what it receives.
 */
@Service
public class DashboardService {

    /** Milestones ending within this many days are shown as "upcoming". */
    public static final int UPCOMING_DAYS = 30;

    private final ProjectRepository projectRepository;
    private final DeveloperRepository developerRepository;
    private final MilestoneRepository milestoneRepository;
    private final ResourceAllocationRepository allocationRepository;
    private final DeveloperLeaveRepository leaveRepository;
    private final ConflictService conflictService;
    private final SkillGapService skillGapService;
    private final BudgetService budgetService;
    private final UtilizationService utilizationService;
    private final NotificationService notificationService;
    private final MilestoneService milestoneService;
    private final AllocationService allocationService;
    private final LeaveService leaveService;

    public DashboardService(ProjectRepository projectRepository,
                            DeveloperRepository developerRepository,
                            MilestoneRepository milestoneRepository,
                            ResourceAllocationRepository allocationRepository,
                            DeveloperLeaveRepository leaveRepository,
                            ConflictService conflictService,
                            SkillGapService skillGapService,
                            BudgetService budgetService,
                            UtilizationService utilizationService,
                            NotificationService notificationService,
                            MilestoneService milestoneService,
                            AllocationService allocationService,
                            LeaveService leaveService) {
        this.projectRepository = projectRepository;
        this.developerRepository = developerRepository;
        this.milestoneRepository = milestoneRepository;
        this.allocationRepository = allocationRepository;
        this.leaveRepository = leaveRepository;
        this.conflictService = conflictService;
        this.skillGapService = skillGapService;
        this.budgetService = budgetService;
        this.utilizationService = utilizationService;
        this.notificationService = notificationService;
        this.milestoneService = milestoneService;
        this.allocationService = allocationService;
        this.leaveService = leaveService;
    }

    // -----------------------------------------------------------------
    //  PROJECT MANAGER DASHBOARD
    // -----------------------------------------------------------------

    public DashboardSummaryResponse getManagerDashboard() {
        LocalDate today = LocalDate.now();
        DashboardSummaryResponse summary = new DashboardSummaryResponse();

        // counts
        summary.setTotalProjects(projectRepository.count());
        summary.setActiveProjects(projectRepository.countByStatus(ProjectStatus.IN_PROGRESS));
        summary.setAtRiskProjects(projectRepository.countByStatus(ProjectStatus.AT_RISK));
        summary.setCompletedProjects(projectRepository.countByStatus(ProjectStatus.COMPLETED));
        summary.setTotalDevelopers(developerRepository.count());

        // utilization table + average
        List<UtilizationResponse> utilization = utilizationService.getUtilization();
        summary.setUtilization(utilization);
        double totalCurrent = 0;
        for (UtilizationResponse row : utilization) {
            totalCurrent += row.getCurrentAllocation();
        }
        summary.setAverageUtilization(utilization.isEmpty() ? 0
                : Math.round((totalCurrent / utilization.size()) * 100.0) / 100.0);

        // conflicts
        List<ConflictResponse> conflicts = conflictService.findAllConflicts();
        summary.setConflicts(conflicts);
        long over = 0;
        long leave = 0;
        for (ConflictResponse conflict : conflicts) {
            if ("OVER_ALLOCATION".equals(conflict.getType())) {
                over++;
            } else {
                leave++;
            }
        }
        summary.setOverAllocationConflicts(over);
        summary.setLeaveConflicts(leave);

        // skill gaps
        List<SkillGapResponse> gaps = skillGapService.findAllSkillGaps();
        summary.setSkillGapDetails(gaps);
        summary.setSkillGaps(gaps.size());

        // budget - totals across every project, plus the projects that need attention
        List<BudgetResponse> budgets = budgetService.getAllBudgets();
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalSpending = BigDecimal.ZERO;
        List<BudgetResponse> warnings = new ArrayList<>();
        for (BudgetResponse budget : budgets) {
            totalBudget = totalBudget.add(budget.getTotalBudget());
            totalSpending = totalSpending.add(budget.getCurrentSpending());
            if (!"NONE".equals(budget.getWarningLevel())) {
                warnings.add(budget);
            }
        }
        summary.setTotalBudget(totalBudget);
        summary.setTotalSpending(totalSpending);
        summary.setBudgetWarnings(warnings);

        if (totalBudget.compareTo(BigDecimal.ZERO) <= 0 || totalSpending.compareTo(BigDecimal.ZERO) <= 0) {
            summary.setOverallBudgetUtilization(null);
            summary.setBudgetMessage(BudgetService.NO_SPENDING_MESSAGE);
        } else {
            summary.setOverallBudgetUtilization(totalSpending.multiply(BigDecimal.valueOf(100))
                    .divide(totalBudget, 2, RoundingMode.HALF_UP).doubleValue());
            summary.setBudgetMessage("Overall budget utilization across all projects.");
        }

        // upcoming milestones
        List<MilestoneResponse> upcoming = new ArrayList<>();
        for (Milestone milestone : milestoneRepository
                .findByEndDateBetweenOrderByEndDateAsc(today, today.plusDays(UPCOMING_DAYS))) {
            if (milestone.getStatus() != MilestoneStatus.COMPLETED) {
                upcoming.add(milestoneService.toResponse(milestone));
            }
        }
        summary.setUpcomingMilestones(upcoming);

        // notifications - regenerated first so the dashboard is always current
        summary.setNotifications(notificationService.refresh());
        summary.setUnreadNotifications(notificationService.countUnread());
        return summary;
    }

    // -----------------------------------------------------------------
    //  DEVELOPER DASHBOARD (deliberately simpler)
    // -----------------------------------------------------------------

    public DeveloperDashboardResponse getDeveloperDashboard(Long developerId) {
        Developer developer = developerRepository.findById(developerId)
                .orElseThrow(() -> new com.sist.rms.exception.ResourceNotFoundException("Developer", developerId));
        LocalDate today = LocalDate.now();

        DeveloperDashboardResponse dashboard = new DeveloperDashboardResponse();
        dashboard.setDeveloperId(developer.getId());
        dashboard.setDeveloperName(developer.getName());
        dashboard.setRole(developer.getRole());

        int availability = developer.getAvailabilityPercentage() == null
                ? 100 : developer.getAvailabilityPercentage();
        dashboard.setAvailability(availability);

        List<ResourceAllocation> allocations = allocationRepository.findByDeveloperId(developerId);
        int current = conflictService.totalAllocationOn(today, allocations);
        dashboard.setCurrentAllocation(current);
        dashboard.setAvailableCapacity(Math.max(0, availability - current));

        Set<String> projects = new LinkedHashSet<>();
        List<AllocationResponse> myAllocations = new ArrayList<>();
        List<MilestoneResponse> myMilestones = new ArrayList<>();
        Set<Long> seenMilestones = new LinkedHashSet<>();

        for (ResourceAllocation allocation : allocations) {
            projects.add(allocation.getProject().getName());
            myAllocations.add(allocationService.toResponse(allocation));
            if (allocation.getMilestone() != null && seenMilestones.add(allocation.getMilestone().getId())) {
                myMilestones.add(milestoneService.toResponse(allocation.getMilestone()));
            }
        }
        dashboard.setMyProjects(new ArrayList<>(projects));
        dashboard.setMyAllocations(myAllocations);
        dashboard.setMyMilestones(myMilestones);

        // upcoming deadlines = my milestones ending in the next 30 days
        List<MilestoneResponse> deadlines = new ArrayList<>();
        for (MilestoneResponse milestone : myMilestones) {
            if (!"COMPLETED".equals(milestone.getStatus())
                    && !milestone.getEndDate().isBefore(today)
                    && !milestone.getEndDate().isAfter(today.plusDays(UPCOMING_DAYS))) {
                deadlines.add(milestone);
            }
        }
        dashboard.setUpcomingDeadlines(deadlines);

        List<LeaveResponse> myLeave = new ArrayList<>();
        for (DeveloperLeave leave : leaveRepository.findByDeveloperId(developerId)) {
            myLeave.add(leaveService.toResponse(leave));
        }
        dashboard.setMyLeave(myLeave);

        dashboard.setMyConflicts(conflictService.findConflictsForDeveloper(developerId));
        dashboard.setMyNotifications(notificationService.findForDeveloper(developerId));
        return dashboard;
    }
}
