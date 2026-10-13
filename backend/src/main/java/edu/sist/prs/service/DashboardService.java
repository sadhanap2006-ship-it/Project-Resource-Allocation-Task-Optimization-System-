package edu.sist.prs.service;

import edu.sist.prs.dto.response.DeveloperDashboardResponse;
import edu.sist.prs.dto.response.PmDashboardResponse;
import edu.sist.prs.engine.BudgetCalculator;
import edu.sist.prs.engine.CapacityCalculator;
import edu.sist.prs.engine.ConflictTypeConstants;
import edu.sist.prs.engine.DeliveryForecaster;
import edu.sist.prs.engine.model.*;
import edu.sist.prs.entity.*;
import edu.sist.prs.exception.ApiException;
import edu.sist.prs.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final AllocationRepository allocationRepository;
    private final ResourceRepository resourceRepository;
    private final ConflictRepository conflictRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UnavailabilityRepository unavailabilityRepository;
    private final BudgetTransactionRepository budgetTransactionRepository;
    private final ConflictDetectionService conflictDetectionService;
    private final SettingsService settingsService;

    public DashboardService(
            ProjectRepository projectRepository,
            MilestoneRepository milestoneRepository,
            AllocationRepository allocationRepository,
            ResourceRepository resourceRepository,
            ConflictRepository conflictRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            UnavailabilityRepository unavailabilityRepository,
            BudgetTransactionRepository budgetTransactionRepository,
            ConflictDetectionService conflictDetectionService,
            SettingsService settingsService
    ) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.allocationRepository = allocationRepository;
        this.resourceRepository = resourceRepository;
        this.conflictRepository = conflictRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.unavailabilityRepository = unavailabilityRepository;
        this.budgetTransactionRepository = budgetTransactionRepository;
        this.conflictDetectionService = conflictDetectionService;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public PmDashboardResponse getPmDashboard() {
        List<ProjectEntity> projects = projectRepository.findAll();
        LocalDate today = LocalDate.now();
        int hoursPerDay = settingsService.getInt("hours.per.day", 8);
        int burnWindow = settingsService.getInt("burn.window.days", 14);

        // Load snapshots
        List<ResourceSnapshot> resources = conflictDetectionService.loadResourceSnapshots();
        List<MilestoneSnapshot> milestones = conflictDetectionService.loadMilestoneSnapshots();
        List<AllocationSnapshot> allocations = conflictDetectionService.loadAllocationSnapshots();
        List<UnavailabilitySnapshot> unavailabilities = conflictDetectionService.loadUnavailabilitySnapshots();
        List<BudgetTransactionEntity> allTxns = budgetTransactionRepository.findAll();
        List<BudgetTransactionSnapshot> txnSnapshots = allTxns.stream()
                .map(t -> new BudgetTransactionSnapshot(t.getId(), t.getProjectId(), t.getTxnDate(), t.getAmount(), t.getCategory(), t.getResourceId(), t.getNote()))
                .toList();

        List<ConflictEntity> openConflicts = conflictRepository.findAll().stream()
                .filter(c -> c.getStatus() == ConflictStatus.OPEN || c.getStatus() == ConflictStatus.ACKNOWLEDGED)
                .toList();

        Map<String, Long> conflictsBySeverity = openConflicts.stream()
                .collect(Collectors.groupingBy(c -> c.getSeverity().name(), Collectors.counting()));
        for (ConflictSeverity s : ConflictSeverity.values()) {
            conflictsBySeverity.putIfAbsent(s.name(), 0L);
        }

        int skillGapsCount = (int) openConflicts.stream()
                .filter(c -> ConflictTypeConstants.SKILL_GAP.equals(c.getType()) || ConflictTypeConstants.UNSTAFFED_MILESTONE.equals(c.getType()))
                .count();

        // Calculate average utilization of active developers today
        List<ResourceSnapshot> activeDevs = resources.stream()
                .filter(r -> r.type() == ResourceType.DEVELOPER && r.status() == ResourceStatus.ACTIVE)
                .toList();
        double totalDevLoad = 0;
        for (ResourceSnapshot dev : activeDevs) {
            totalDevLoad += CapacityCalculator.calculateLoad(dev.id(), today, allocations);
        }
        double avgUtil = activeDevs.isEmpty() ? 0.0 : totalDevLoad / activeDevs.size();

        // Per-project rows
        List<PmDashboardResponse.PmProjectRow> projectRows = new ArrayList<>();
        int atRiskCount = 0;

        for (ProjectEntity p : projects) {
            BigDecimal actualSpend = allTxns.stream()
                    .filter(t -> t.getProjectId().equals(p.getId()))
                    .map(BudgetTransactionEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            double budgetUtilPct = p.getTotalBudget().compareTo(BigDecimal.ZERO) > 0
                    ? actualSpend.multiply(BigDecimal.valueOf(100)).divide(p.getTotalBudget(), 1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            BudgetForecastResult bForecast = BudgetCalculator.calculateProjectForecast(
                    p.getId(), p.getName(), p.getTotalBudget(), p.getStartDate(), p.getEndDate(),
                    milestones, allocations, resources, unavailabilities, txnSnapshots, today, burnWindow, hoursPerDay
            );

            // Progress is effort-weighted average of milestone progress
            List<MilestoneEntity> pMilestones = milestoneRepository.findByProjectIdOrderByStartDateAsc(p.getId());
            double totalEffort = 0;
            double weightedProgress = 0;
            for (MilestoneEntity m : pMilestones) {
                double effort = m.getEstimatedEffortHours() != null ? m.getEstimatedEffortHours().doubleValue() : 0.0;
                totalEffort += effort;
                weightedProgress += effort * (m.getProgressPct() != null ? m.getProgressPct().doubleValue() : 0.0);
            }
            double progressPct = totalEffort > 0 ? weightedProgress / totalEffort : 0.0;

            // Health determination: RED, AMBER, GREEN
            long critCount = openConflicts.stream().filter(c -> p.getId().equals(c.getProjectId()) && c.getSeverity() == ConflictSeverity.CRITICAL).count();
            long warnCount = openConflicts.stream().filter(c -> p.getId().equals(c.getProjectId()) && c.getSeverity() == ConflictSeverity.WARNING).count();

            boolean budgetOverrun = bForecast.runOutDate() != null && bForecast.runOutDate().isBefore(p.getEndDate());

            String health = "GREEN";
            if (critCount > 0 || (bForecast.spi() != null && bForecast.spi() < 0.80) || budgetOverrun) {
                health = "RED";
                atRiskCount++;
            } else if (warnCount > 0 || (bForecast.spi() != null && bForecast.spi() < 0.95)) {
                health = "AMBER";
                atRiskCount++;
            }

            projectRows.add(new PmDashboardResponse.PmProjectRow(
                    p.getId(),
                    p.getCode(),
                    p.getName(),
                    health,
                    Math.round(progressPct * 10.0) / 10.0,
                    p.getTotalBudget(),
                    actualSpend,
                    budgetUtilPct,
                    bForecast.runOutDate()
            ));
        }

        // Upcoming deadlines (next 14 days)
        LocalDate deadlineWindow = today.plusDays(14);
        List<PmDashboardResponse.UpcomingDeadlineItem> upcomingDeadlines = new ArrayList<>();
        Map<Long, ProjectEntity> projectMap = projects.stream().collect(Collectors.toMap(ProjectEntity::getId, p -> p));

        for (MilestoneSnapshot ms : milestones) {
            if (ms.status() != MilestoneStatus.COMPLETED && !ms.endDate().isBefore(today) && !ms.endDate().isAfter(deadlineWindow)) {
                MilestoneDeliveryForecast df = DeliveryForecaster.forecastMilestone(
                        ms, allocations, resources, unavailabilities, today, hoursPerDay, 365
                );
                ProjectEntity p = projectMap.get(ms.projectId());
                upcomingDeadlines.add(new PmDashboardResponse.UpcomingDeadlineItem(
                        ms.id(),
                        ms.name(),
                        ms.projectId(),
                        p != null ? p.getName() : "Unknown",
                        ms.endDate(),
                        (double) ms.progressPct(),
                        (int) df.delayWorkingDays()
                ));
            }
        }
        upcomingDeadlines.sort(Comparator.comparing(PmDashboardResponse.UpcomingDeadlineItem::endDate));

        // Latest notifications
        List<NotificationEntity> recentNotifs = notificationRepository.findAll().stream()
                .sorted(Comparator.comparing(NotificationEntity::getCreatedAt).reversed())
                .limit(5)
                .toList();
        List<PmDashboardResponse.NotificationResponse> notifResponses = recentNotifs.stream()
                .map(n -> new PmDashboardResponse.NotificationResponse(
                        n.getId(),
                        n.getTitle(),
                        n.getMessage(),
                        n.getSeverity().name(),
                        n.getIsRead(),
                        n.getCreatedAt().toString()
                ))
                .toList();

        int totalProjects = projects.size();
        int activeProjects = (int) projects.stream().filter(p -> p.getStatus() == ProjectStatus.ACTIVE).count();

        return new PmDashboardResponse(
                totalProjects,
                activeProjects,
                atRiskCount,
                conflictsBySeverity,
                skillGapsCount,
                Math.round(avgUtil * 10.0) / 10.0,
                projectRows,
                upcomingDeadlines,
                notifResponses
        );
    }

    @Transactional(readOnly = true)
    public DeveloperDashboardResponse getDeveloperDashboard(String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));

        Long resourceId = user.getResourceId();
        if (resourceId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NOT_DEVELOPER", "Current user is not linked to a developer resource");
        }

        ResourceEntity resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found"));

        LocalDate today = LocalDate.now();
        List<ResourceSnapshot> allResources = conflictDetectionService.loadResourceSnapshots();
        List<AllocationSnapshot> allocations = conflictDetectionService.loadAllocationSnapshots();
        List<UnavailabilitySnapshot> unavailabilities = conflictDetectionService.loadUnavailabilitySnapshots();

        // 1. Current load & Peak load (30 days) & Remaining capacity
        int currentLoad = CapacityCalculator.calculateLoad(resourceId, today, allocations);
        int peakLoad = CapacityCalculator.calculatePeakLoad30Days(resourceId, today, allocations);

        ResourceSnapshot rSnap = allResources.stream()
                .filter(r -> r.id().equals(resourceId))
                .findFirst()
                .orElse(null);
        int remaining = rSnap != null ? CapacityCalculator.calculateRemaining(rSnap, today, allocations, unavailabilities) : 0;

        // 2. My Allocations
        List<AllocationEntity> myAllocEntities = allocationRepository.findByResourceId(resourceId);
        Map<Long, ProjectEntity> projectMap = projectRepository.findAll().stream()
                .collect(Collectors.toMap(ProjectEntity::getId, p -> p));
        Map<Long, MilestoneEntity> milestoneMap = milestoneRepository.findAll().stream()
                .collect(Collectors.toMap(MilestoneEntity::getId, m -> m));

        List<DeveloperDashboardResponse.DevAllocationItem> allocItems = myAllocEntities.stream()
                .filter(a -> !a.getEndDate().isBefore(today))
                .map(a -> {
                    MilestoneEntity m = milestoneMap.get(a.getMilestoneId());
                    ProjectEntity p = m != null ? projectMap.get(m.getProjectId()) : null;
                    return new DeveloperDashboardResponse.DevAllocationItem(
                            a.getId(),
                            p != null ? p.getId() : null,
                            p != null ? p.getName() : "Unknown",
                            m != null ? m.getId() : null,
                            m != null ? m.getName() : "Unknown",
                            a.getAllocationPct(),
                            a.getStartDate(),
                            a.getEndDate()
                    );
                })
                .sorted(Comparator.comparing(DeveloperDashboardResponse.DevAllocationItem::startDate))
                .toList();

        // 3. My Milestones
        Set<Long> myMilestoneIds = myAllocEntities.stream().map(AllocationEntity::getMilestoneId).collect(Collectors.toSet());
        List<DeveloperDashboardResponse.DevMilestoneItem> milestoneItems = myMilestoneIds.stream()
                .map(milestoneMap::get)
                .filter(Objects::nonNull)
                .map(m -> {
                    ProjectEntity p = projectMap.get(m.getProjectId());
                    return new DeveloperDashboardResponse.DevMilestoneItem(
                            m.getId(),
                            m.getProjectId(),
                            p != null ? p.getName() : "Unknown",
                            m.getName(),
                            m.getEndDate(),
                            m.getProgressPct().doubleValue(),
                            m.getStatus().name()
                    );
                })
                .sorted(Comparator.comparing(DeveloperDashboardResponse.DevMilestoneItem::endDate))
                .toList();

        // 4. Weekly load for next 8 weeks
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<DeveloperDashboardResponse.DevWeeklyLoadItem> weeklyLoad = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            LocalDate wFrom = weekStart.plusWeeks(i);
            LocalDate wTo = wFrom.plusDays(4); // Mon to Fri
            int peakInWeek = CapacityCalculator.calculatePeakLoad(resourceId, wFrom, wTo, allocations);
            weeklyLoad.add(new DeveloperDashboardResponse.DevWeeklyLoadItem(
                    "Week " + (i + 1) + " (" + wFrom.getMonth().name().substring(0, 3) + " " + wFrom.getDayOfMonth() + ")",
                    wFrom,
                    wTo,
                    peakInWeek
            ));
        }

        // 5. My Leave
        List<UnavailabilityEntity> leaveList = unavailabilityRepository.findByResourceId(resourceId);
        List<DeveloperDashboardResponse.DevLeaveItem> leaveItems = leaveList.stream()
                .map(l -> new DeveloperDashboardResponse.DevLeaveItem(
                        l.getId(),
                        l.getStartDate(),
                        l.getEndDate(),
                        l.getReason(),
                        l.getStatus().name()
                ))
                .sorted(Comparator.comparing(DeveloperDashboardResponse.DevLeaveItem::startDate).reversed())
                .toList();

        // 6. Open conflicts about me
        List<DeveloperDashboardResponse.DevConflictItem> conflicts = conflictRepository.findByResourceId(resourceId).stream()
                .filter(c -> c.getStatus() == ConflictStatus.OPEN || c.getStatus() == ConflictStatus.ACKNOWLEDGED)
                .map(c -> new DeveloperDashboardResponse.DevConflictItem(
                        c.getId(),
                        c.getType(),
                        c.getSeverity().name(),
                        c.getDescription(),
                        c.getStartDate(),
                        c.getEndDate()
                ))
                .toList();

        // 7. Unread notifications count
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user.getId());

        return new DeveloperDashboardResponse(
                resourceId,
                resource.getName(),
                currentLoad,
                peakLoad,
                remaining,
                allocItems,
                milestoneItems,
                weeklyLoad,
                leaveItems,
                conflicts,
                unreadCount
        );
    }
}
