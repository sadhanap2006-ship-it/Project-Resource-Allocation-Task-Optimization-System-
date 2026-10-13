package edu.sist.prs.service;

import edu.sist.prs.engine.CapacityCalculator;
import edu.sist.prs.engine.ConflictRules;
import edu.sist.prs.engine.ConflictTypeConstants;
import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.engine.model.DetectedConflict;
import edu.sist.prs.engine.model.MilestoneSnapshot;
import edu.sist.prs.engine.model.ResourceSkillSnapshot;
import edu.sist.prs.engine.model.ResourceSnapshot;
import edu.sist.prs.engine.model.SkillRequirementSnapshot;
import edu.sist.prs.engine.model.UnavailabilitySnapshot;
import edu.sist.prs.entity.*;
import edu.sist.prs.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service orchestrating conflict detection, incremental upserts/resolutions,
 * notifications, and startup/nightly scans.
 * Adheres to Section 9.2, Section 12, and Rule R4/R10.
 */
@Service
public class ConflictDetectionService {

    private static final Logger log = LoggerFactory.getLogger(ConflictDetectionService.class);

    private final ConflictRepository conflictRepository;
    private final ResourceRepository resourceRepository;
    private final DeveloperProfileRepository developerProfileRepository;
    private final HardwareAssetRepository hardwareAssetRepository;
    private final CloudResourceRepository cloudResourceRepository;
    private final ResourceSkillRepository resourceSkillRepository;
    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final MilestoneSkillRequirementRepository milestoneSkillRequirementRepository;
    private final AllocationRepository allocationRepository;
    private final UnavailabilityRepository unavailabilityRepository;
    private final BudgetTransactionRepository budgetTransactionRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final NotificationService notificationService;
    private final SettingsService settingsService;

    public ConflictDetectionService(
            ConflictRepository conflictRepository,
            ResourceRepository resourceRepository,
            DeveloperProfileRepository developerProfileRepository,
            HardwareAssetRepository hardwareAssetRepository,
            CloudResourceRepository cloudResourceRepository,
            ResourceSkillRepository resourceSkillRepository,
            ProjectRepository projectRepository,
            MilestoneRepository milestoneRepository,
            MilestoneSkillRequirementRepository milestoneSkillRequirementRepository,
            AllocationRepository allocationRepository,
            UnavailabilityRepository unavailabilityRepository,
            BudgetTransactionRepository budgetTransactionRepository,
            UserRepository userRepository,
            SkillRepository skillRepository,
            NotificationService notificationService,
            SettingsService settingsService
    ) {
        this.conflictRepository = conflictRepository;
        this.resourceRepository = resourceRepository;
        this.developerProfileRepository = developerProfileRepository;
        this.hardwareAssetRepository = hardwareAssetRepository;
        this.cloudResourceRepository = cloudResourceRepository;
        this.resourceSkillRepository = resourceSkillRepository;
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.milestoneSkillRequirementRepository = milestoneSkillRequirementRepository;
        this.allocationRepository = allocationRepository;
        this.unavailabilityRepository = unavailabilityRepository;
        this.budgetTransactionRepository = budgetTransactionRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.notificationService = notificationService;
        this.settingsService = settingsService;
    }

    /**
     * Startup full conflict scan per Section 14:
     * "After seeding, the backend must run a full conflict scan on startup (ApplicationReadyEvent)
     * so the Conflict Center and notifications are populated immediately."
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        log.info("Executing startup conflict detection scan...");
        try {
            runFullScan();
            log.info("Startup conflict detection scan completed successfully.");
        } catch (Exception e) {
            log.error("Error during startup conflict detection scan", e);
        }
    }

    /**
     * Performs a full conflict scan across all active resources and projects.
     */
    @Transactional
    public List<ConflictEntity> runFullScan() {
        LocalDate today = LocalDate.now();
        LocalDate scanStart = today.minusDays(28);

        LocalDate latestMilestoneEnd = milestoneRepository.findAll().stream()
                .map(MilestoneEntity::getEndDate)
                .max(LocalDate::compareTo)
                .orElse(today.plusDays(90));

        LocalDate scanEnd = latestMilestoneEnd.isBefore(today.plusDays(90))
                ? today.plusDays(90)
                : latestMilestoneEnd;

        return executeScan(null, null, scanStart, scanEnd, today);
    }

    /**
     * Synchronous re-evaluation for specific affected resources and/or projects.
     */
    @Transactional
    public void reevaluateScope(Set<Long> affectedResourceIds, Set<Long> affectedProjectIds) {
        LocalDate today = LocalDate.now();
        LocalDate scanStart = today.minusDays(28);

        LocalDate latestMilestoneEnd = milestoneRepository.findAll().stream()
                .map(MilestoneEntity::getEndDate)
                .max(LocalDate::compareTo)
                .orElse(today.plusDays(90));

        LocalDate scanEnd = latestMilestoneEnd.isBefore(today.plusDays(90))
                ? today.plusDays(90)
                : latestMilestoneEnd;

        executeScan(affectedResourceIds, affectedProjectIds, scanStart, scanEnd, today);
    }

    private List<ConflictEntity> executeScan(
            Set<Long> targetResourceIds,
            Set<Long> targetProjectIds,
            LocalDate scanStart,
            LocalDate scanEnd,
            LocalDate today
    ) {
        // 1. Load Snapshots
        List<ResourceSnapshot> resourceSnapshots = loadResourceSnapshots();
        List<MilestoneSnapshot> milestoneSnapshots = loadMilestoneSnapshots();
        List<AllocationSnapshot> allocationSnapshots = loadAllocationSnapshots();
        List<UnavailabilitySnapshot> unavailabilitySnapshots = loadUnavailabilitySnapshots();

        // 2. Read thresholds from SettingsService
        double burnoutThreshold = settingsService.getDouble("burnout.threshold.pct", 85.0);
        int burnoutWeeks = settingsService.getInt("burnout.consecutive.weeks", 3);
        double mismatchThreshold = settingsService.getDouble("skill.mismatch.pct", 50.0);
        int lookaheadDays = settingsService.getInt("unstaffed.lookahead.days", 14);

        // 3. Evaluate conflicts using pure engine
        List<DetectedConflict> detected = new ArrayList<>(ConflictRules.evaluateConflicts(
                resourceSnapshots,
                milestoneSnapshots,
                allocationSnapshots,
                unavailabilitySnapshots,
                scanStart,
                scanEnd,
                today,
                burnoutThreshold,
                burnoutWeeks,
                mismatchThreshold,
                lookaheadDays
        ));

        // Evaluate BUDGET_RISK per Section 9.2 & 9.6
        double cpiWarning = settingsService.getDouble("budget.cpi.warning", 0.9);
        int criticalDays = settingsService.getInt("budget.critical.days", 14);
        int burnWindow = settingsService.getInt("burn.window.days", 14);
        int hoursPerDay = settingsService.getInt("hours.per.day", 8);

        List<ProjectEntity> projects = projectRepository.findAll();
        List<edu.sist.prs.engine.model.BudgetTransactionSnapshot> txnSnapshots = budgetTransactionRepository.findAll().stream()
                .map(t -> new edu.sist.prs.engine.model.BudgetTransactionSnapshot(t.getId(), t.getProjectId(), t.getTxnDate(), t.getAmount(), t.getCategory(), t.getResourceId(), t.getNote()))
                .toList();

        for (ProjectEntity p : projects) {
            if (p.getStatus() == ProjectStatus.COMPLETED || p.getStatus() == ProjectStatus.ARCHIVED) {
                continue;
            }
            edu.sist.prs.engine.model.BudgetForecastResult bResult = edu.sist.prs.engine.BudgetCalculator.calculateProjectForecast(
                    p.getId(), p.getName(), p.getTotalBudget(), p.getStartDate(), p.getEndDate(),
                    milestoneSnapshots, allocationSnapshots, resourceSnapshots, unavailabilitySnapshots,
                    txnSnapshots, today, burnWindow, hoursPerDay
            );

            if (bResult.planTotal().compareTo(p.getTotalBudget()) > 0) {
                detected.add(new DetectedConflict(
                        ConflictTypeConstants.BUDGET_RISK,
                        ConflictSeverity.CRITICAL,
                        p.getId(), null, null,
                        p.getStartDate(), p.getEndDate(),
                        bResult.planTotal(),
                        String.format("Planned cost (₹%s) exceeds total budget (₹%s) for project '%s'.",
                                bResult.planTotal(), p.getTotalBudget(), p.getName()),
                        "BUDGET:" + p.getId() + ":PLAN_EXCEEDS"
                ));
            }

            if (bResult.remaining().compareTo(java.math.BigDecimal.ZERO) < 0) {
                detected.add(new DetectedConflict(
                        ConflictTypeConstants.BUDGET_RISK,
                        ConflictSeverity.CRITICAL,
                        p.getId(), null, null,
                        today, p.getEndDate(),
                        bResult.remaining().abs(),
                        String.format("Budget exceeded by ₹%s for project '%s'.", bResult.remaining().abs(), p.getName()),
                        "BUDGET:" + p.getId() + ":EXCEEDED"
                ));
            }

            if (bResult.runOutDate() != null && bResult.runOutDate().isBefore(p.getEndDate())) {
                long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, bResult.runOutDate());
                ConflictSeverity sev = daysUntil <= criticalDays ? ConflictSeverity.CRITICAL : ConflictSeverity.WARNING;
                detected.add(new DetectedConflict(
                        ConflictTypeConstants.BUDGET_RISK,
                        sev,
                        p.getId(), null, null,
                        today, bResult.runOutDate(),
                        bResult.burnRate(),
                        bResult.message(),
                        "BUDGET:" + p.getId() + ":EARLY_RUNOUT"
                ));
            }

            if (bResult.cpi() != null && bResult.cpi() < cpiWarning) {
                detected.add(new DetectedConflict(
                        ConflictTypeConstants.BUDGET_RISK,
                        ConflictSeverity.WARNING,
                        p.getId(), null, null,
                        today, p.getEndDate(),
                        java.math.BigDecimal.valueOf(bResult.cpi()).setScale(2, java.math.RoundingMode.HALF_UP),
                        String.format("Project '%s' has low Cost Performance Index (CPI = %.2f, threshold %.2f).",
                                p.getName(), bResult.cpi(), cpiWarning),
                        "BUDGET:" + p.getId() + ":LOW_CPI"
                ));
            }
        }

        // Evaluate MILESTONE_DELIVERY_RISK per Section 9.2 & 9.7
        int deliveryWarningDays = settingsService.getInt("delivery.warning.days", 5);
        int maxForecastDays = settingsService.getInt("forecast.max.days", 1825);
        Map<Long, ResourceSnapshot> resMap = resourceSnapshots.stream()
                .collect(Collectors.toMap(ResourceSnapshot::id, Function.identity(), (a, b) -> a));

        for (MilestoneSnapshot ms : milestoneSnapshots) {
            if (ms.status() == MilestoneStatus.PLANNED || ms.status() == MilestoneStatus.IN_PROGRESS) {
                edu.sist.prs.engine.model.MilestoneDeliveryForecast df = edu.sist.prs.engine.DeliveryForecaster.forecastMilestone(
                        ms, allocationSnapshots, resourceSnapshots, unavailabilitySnapshots,
                        today, hoursPerDay, maxForecastDays
                );

                if (df.delayWorkingDays() > 0) {
                    ConflictSeverity sev = df.delayWorkingDays() <= deliveryWarningDays ? ConflictSeverity.WARNING : ConflictSeverity.CRITICAL;
                    detected.add(new DetectedConflict(
                            ConflictTypeConstants.MILESTONE_DELIVERY_RISK,
                            sev,
                            ms.projectId(), ms.id(), null,
                            ms.endDate(), df.forecastFinishDate(),
                            java.math.BigDecimal.valueOf(df.delayWorkingDays()),
                            df.message(),
                            "DELIVERY:" + ms.id()
                    ));
                } else if ("NO_CAPACITY".equals(df.forecastStatus())) {
                    boolean hasDev = allocationSnapshots.stream()
                            .filter(a -> a.milestoneId().equals(ms.id()))
                            .filter(a -> !CapacityCalculator.isExcludedByRuleR9(a))
                            .anyMatch(a -> {
                                ResourceSnapshot r = resMap.get(a.resourceId());
                                return r != null && r.type() == ResourceType.DEVELOPER;
                            });
                    if (hasDev) {
                        detected.add(new DetectedConflict(
                                ConflictTypeConstants.MILESTONE_DELIVERY_RISK,
                                ConflictSeverity.CRITICAL,
                                ms.projectId(), ms.id(), null,
                                ms.startDate(), ms.endDate(),
                                null,
                                "Milestone has allocated developers but produces no working capacity.",
                                "DELIVERY:" + ms.id()
                        ));
                    }
                }
            }
        }

        // Filter detected conflicts if targeted scope was provided
        if (targetResourceIds != null || targetProjectIds != null) {
            detected = detected.stream().filter(c -> {
                boolean matchRes = targetResourceIds != null && c.resourceId() != null && targetResourceIds.contains(c.resourceId());
                boolean matchProj = targetProjectIds != null && c.projectId() != null && targetProjectIds.contains(c.projectId());
                return matchRes || matchProj;
            }).toList();
        }

        // 4. Upsert detected conflicts and resolve outdated ones
        return persistConflictsAndNotify(detected, targetResourceIds, targetProjectIds);
    }

    private List<ConflictEntity> persistConflictsAndNotify(
            List<DetectedConflict> detectedList,
            Set<Long> targetResourceIds,
            Set<Long> targetProjectIds
    ) {
        Set<String> detectedKeys = new HashSet<>();
        List<ConflictEntity> savedEntities = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Map projects to PM users for notification
        Map<Long, Long> projectPmMap = projectRepository.findAll().stream()
                .collect(Collectors.toMap(ProjectEntity::getId, ProjectEntity::getPmUserId, (a, b) -> a));

        // Map resource ID to developer User ID
        Map<Long, Long> resourceDevUserMap = userRepository.findAll().stream()
                .filter(u -> u.getResourceId() != null)
                .collect(Collectors.toMap(UserEntity::getResourceId, UserEntity::getId, (a, b) -> a));

        for (DetectedConflict c : detectedList) {
            detectedKeys.add(c.dedupKey());
            Optional<ConflictEntity> existingOpt = conflictRepository.findByDedupKey(c.dedupKey());

            if (existingOpt.isPresent()) {
                ConflictEntity existing = existingOpt.get();
                existing.setDescription(c.description());
                existing.setSeverity(c.severity());
                existing.setPeakValue(c.peakValue());
                existing.setStartDate(c.startDate());
                existing.setEndDate(c.endDate());

                // If it was resolved earlier but detected again, reopen
                if (existing.getStatus() == ConflictStatus.RESOLVED) {
                    existing.setStatus(ConflictStatus.OPEN);
                    existing.setResolvedAt(null);
                }
                savedEntities.add(conflictRepository.save(existing));
            } else {
                // New conflict
                ConflictEntity newConflict = new ConflictEntity(
                        c.type(),
                        c.severity(),
                        c.projectId(),
                        c.milestoneId(),
                        c.resourceId(),
                        c.startDate(),
                        c.endDate(),
                        c.peakValue(),
                        c.description(),
                        ConflictStatus.OPEN,
                        now,
                        c.dedupKey()
                );
                ConflictEntity saved = conflictRepository.save(newConflict);
                savedEntities.add(saved);

                // Notifications for new conflict (Section 12)
                createNotificationsForNewConflict(saved, c, projectPmMap, resourceDevUserMap);
            }
        }

        // Resolve conflicts in scope that are no longer detected
        List<ConflictEntity> activeInScope;
        if (targetResourceIds != null || targetProjectIds != null) {
            activeInScope = conflictRepository.findAll().stream()
                    .filter(c -> c.getStatus() == ConflictStatus.OPEN || c.getStatus() == ConflictStatus.ACKNOWLEDGED)
                    .filter(c -> (c.getResourceId() != null && targetResourceIds != null && targetResourceIds.contains(c.getResourceId())) ||
                                 (c.getProjectId() != null && targetProjectIds != null && targetProjectIds.contains(c.getProjectId())))
                    .toList();
        } else {
            activeInScope = conflictRepository.findAll().stream()
                    .filter(c -> c.getStatus() == ConflictStatus.OPEN || c.getStatus() == ConflictStatus.ACKNOWLEDGED)
                    .toList();
        }

        for (ConflictEntity entity : activeInScope) {
            if (!detectedKeys.contains(entity.getDedupKey())) {
                entity.setStatus(ConflictStatus.RESOLVED);
                entity.setResolvedAt(now);
                conflictRepository.save(entity);

                // Notification for resolution (Section 12: one INFO notification)
                createNotificationsForResolvedConflict(entity, projectPmMap, resourceDevUserMap);
            }
        }

        return savedEntities;
    }

    private void createNotificationsForNewConflict(
            ConflictEntity saved,
            DetectedConflict c,
            Map<Long, Long> projectPmMap,
            Map<Long, Long> resourceDevUserMap
    ) {
        String title = String.format("%s: %s", c.severity(), c.type());
        String message = c.description();

        // 1. Notify PM if linked to a project
        if (c.projectId() != null && projectPmMap.containsKey(c.projectId())) {
            Long pmUserId = projectPmMap.get(c.projectId());
            notificationService.createNotification(pmUserId, c.projectId(), saved.getId(), c.severity(), title, message);
        }

        // 2. Notify Developer if relevant to them (OVER_ALLOCATION, LEAVE_CONFLICT, BURNOUT_RISK, SKILL_MISMATCH)
        if (c.resourceId() != null && resourceDevUserMap.containsKey(c.resourceId())) {
            String type = c.type();
            if (ConflictTypeConstants.OVER_ALLOCATION.equals(type) ||
                ConflictTypeConstants.LEAVE_CONFLICT.equals(type) ||
                ConflictTypeConstants.BURNOUT_RISK.equals(type) ||
                ConflictTypeConstants.SKILL_MISMATCH.equals(type)) {
                Long devUserId = resourceDevUserMap.get(c.resourceId());
                notificationService.createNotification(devUserId, c.projectId(), saved.getId(), c.severity(), title, message);
            }
        }
    }

    private void createNotificationsForResolvedConflict(
            ConflictEntity resolved,
            Map<Long, Long> projectPmMap,
            Map<Long, Long> resourceDevUserMap
    ) {
        String title = "Conflict Resolved";
        String message = String.format("Resolved: %s", resolved.getDescription());

        if (resolved.getProjectId() != null && projectPmMap.containsKey(resolved.getProjectId())) {
            Long pmUserId = projectPmMap.get(resolved.getProjectId());
            notificationService.createNotification(pmUserId, resolved.getProjectId(), resolved.getId(), ConflictSeverity.INFO, title, message);
        }

        if (resolved.getResourceId() != null && resourceDevUserMap.containsKey(resolved.getResourceId())) {
            Long devUserId = resourceDevUserMap.get(resolved.getResourceId());
            notificationService.createNotification(devUserId, resolved.getProjectId(), resolved.getId(), ConflictSeverity.INFO, title, message);
        }
    }

    // --- Snapshot Loaders ---

    @Transactional(readOnly = true)
    public List<ResourceSnapshot> loadResourceSnapshots() {
        List<ResourceEntity> resources = resourceRepository.findAll();
        List<ResourceSkillEntity> allSkills = resourceSkillRepository.findAll();
        List<CloudResourceEntity> clouds = cloudResourceRepository.findAll();
        Map<Long, String> skillNames = skillRepository.findAll().stream()
                .collect(Collectors.toMap(SkillEntity::getId, SkillEntity::getName));

        Map<Long, List<ResourceSkillSnapshot>> skillMap = new HashMap<>();
        for (ResourceSkillEntity rs : allSkills) {
            String sName = skillNames.getOrDefault(rs.getId().getSkillId(), "Skill #" + rs.getId().getSkillId());
            skillMap.computeIfAbsent(rs.getId().getResourceId(), k -> new ArrayList<>())
                    .add(new ResourceSkillSnapshot(rs.getId().getSkillId(), sName, rs.getProficiency()));
        }

        Map<Long, CloudResourceEntity> cloudMap = clouds.stream()
                .collect(Collectors.toMap(CloudResourceEntity::getResourceId, Function.identity(), (a, b) -> a));

        List<ResourceSnapshot> list = new ArrayList<>();
        for (ResourceEntity r : resources) {
            CloudResourceEntity c = cloudMap.get(r.getId());
            list.add(new ResourceSnapshot(
                    r.getId(),
                    r.getType(),
                    r.getName(),
                    r.getStatus(),
                    r.getCapacityPct(),
                    r.getCostRate(),
                    r.getCostUnit(),
                    c != null ? c.getDailyQuotaUnits() : null,
                    c != null ? c.getMonthlyCap() : null,
                    skillMap.getOrDefault(r.getId(), List.of())
            ));
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<MilestoneSnapshot> loadMilestoneSnapshots() {
        List<MilestoneEntity> milestones = milestoneRepository.findAll();
        List<MilestoneSkillRequirementEntity> reqs = milestoneSkillRequirementRepository.findAll();
        Map<Long, String> skillNames = skillRepository.findAll().stream()
                .collect(Collectors.toMap(SkillEntity::getId, SkillEntity::getName));

        Map<Long, List<SkillRequirementSnapshot>> reqMap = new HashMap<>();
        for (MilestoneSkillRequirementEntity msr : reqs) {
            String sName = skillNames.getOrDefault(msr.getId().getSkillId(), "Skill #" + msr.getId().getSkillId());
            reqMap.computeIfAbsent(msr.getId().getMilestoneId(), k -> new ArrayList<>())
                    .add(new SkillRequirementSnapshot(
                            msr.getId().getSkillId(),
                            sName,
                            msr.getMinProficiency(),
                            msr.getMandatory()
                    ));
        }

        List<MilestoneSnapshot> list = new ArrayList<>();
        for (MilestoneEntity m : milestones) {
            list.add(new MilestoneSnapshot(
                    m.getId(),
                    m.getProjectId(),
                    m.getName(),
                    m.getStartDate(),
                    m.getEndDate(),
                    m.getStatus(),
                    m.getProgressPct(),
                    m.getEstimatedEffortHours(),
                    reqMap.getOrDefault(m.getId(), List.of())
            ));
        }
        return list;
    }

    public List<AllocationSnapshot> loadAllocationSnapshots() {
        List<AllocationEntity> allocations = allocationRepository.findAll();
        List<AllocationSnapshot> list = new ArrayList<>();
        for (AllocationEntity a : allocations) {
            list.add(new AllocationSnapshot(
                    a.getId(),
                    a.getResourceId(),
                    a.getMilestoneId(),
                    a.getMilestone() != null ? a.getMilestone().getProjectId() : null,
                    a.getStartDate(),
                    a.getEndDate(),
                    a.getAllocationPct(),
                    a.getOverrideConfirmed(),
                    a.getMilestone() != null ? a.getMilestone().getStatus() : MilestoneStatus.IN_PROGRESS,
                    a.getMilestone() != null && a.getMilestone().getProject() != null ? a.getMilestone().getProject().getStatus() : ProjectStatus.ACTIVE
            ));
        }
        return list;
    }

    public List<UnavailabilitySnapshot> loadUnavailabilitySnapshots() {
        List<UnavailabilityEntity> unavailabilities = unavailabilityRepository.findAll();
        List<UnavailabilitySnapshot> list = new ArrayList<>();
        for (UnavailabilityEntity u : unavailabilities) {
            list.add(new UnavailabilitySnapshot(
                    u.getId(),
                    u.getResourceId(),
                    u.getStartDate(),
                    u.getEndDate(),
                    u.getType(),
                    u.getStatus()
            ));
        }
        return list;
    }
}
