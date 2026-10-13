package edu.sist.prs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sist.prs.dto.request.SimulationChangeItem;
import edu.sist.prs.dto.request.SimulationRunRequest;
import edu.sist.prs.dto.request.SimulationSaveRequest;
import edu.sist.prs.engine.BudgetCalculator;
import edu.sist.prs.engine.CapacityCalculator;
import edu.sist.prs.engine.ConflictRules;
import edu.sist.prs.engine.DeliveryForecaster;
import edu.sist.prs.engine.SkillMatcher;
import edu.sist.prs.engine.model.*;
import edu.sist.prs.entity.*;
import edu.sist.prs.exception.ApiException;
import edu.sist.prs.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SimulationService {

    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final AllocationRepository allocationRepository;
    private final ResourceRepository resourceRepository;
    private final BudgetTransactionRepository budgetTransactionRepository;
    private final SimulationScenarioRepository simulationScenarioRepository;
    private final SimulationChangeRepository simulationChangeRepository;
    private final UserRepository userRepository;
    private final ConflictDetectionService conflictDetectionService;
    private final SettingsService settingsService;
    private final ObjectMapper objectMapper;

    public SimulationService(
            ProjectRepository projectRepository,
            MilestoneRepository milestoneRepository,
            AllocationRepository allocationRepository,
            ResourceRepository resourceRepository,
            BudgetTransactionRepository budgetTransactionRepository,
            SimulationScenarioRepository simulationScenarioRepository,
            SimulationChangeRepository simulationChangeRepository,
            UserRepository userRepository,
            ConflictDetectionService conflictDetectionService,
            SettingsService settingsService,
            ObjectMapper objectMapper
    ) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.allocationRepository = allocationRepository;
        this.resourceRepository = resourceRepository;
        this.budgetTransactionRepository = budgetTransactionRepository;
        this.simulationScenarioRepository = simulationScenarioRepository;
        this.simulationChangeRepository = simulationChangeRepository;
        this.userRepository = userRepository;
        this.conflictDetectionService = conflictDetectionService;
        this.settingsService = settingsService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SimulationResult runSimulation(SimulationRunRequest request) {
        return executeSimulation(request.projectId(), request.changes());
    }

    @Transactional
    public SimulationScenarioEntity saveSimulation(SimulationSaveRequest request, String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));

        SimulationResult result = executeSimulation(request.projectId(), request.changes());

        String baselineJson;
        String resultJson;
        try {
            baselineJson = objectMapper.writeValueAsString(Map.of(
                    "planTotal", result.baselinePlanTotal(),
                    "forecastDelivery", result.baselineForecastDelivery() != null ? result.baselineForecastDelivery().toString() : "",
                    "runOutDate", result.baselineRunOutDate() != null ? result.baselineRunOutDate().toString() : ""
            ));
            resultJson = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Failed to serialize scenario JSON: " + e.getMessage());
        }

        SimulationScenarioEntity scenario = new SimulationScenarioEntity(
                request.projectId(),
                request.name(),
                user.getId(),
                baselineJson,
                resultJson
        );
        SimulationScenarioEntity savedScenario = simulationScenarioRepository.save(scenario);

        List<SimulationChangeEntity> changeEntities = new ArrayList<>();
        for (SimulationChangeItem item : request.changes()) {
            SimulationChangeEntity change = new SimulationChangeEntity(
                    savedScenario.getId(),
                    item.type(),
                    item.allocationId(),
                    item.fromResourceId(),
                    item.toResourceId(),
                    item.milestoneId(),
                    item.startDate(),
                    item.endDate(),
                    item.newPct() != null ? item.newPct() : item.pct()
            );
            changeEntities.add(change);
        }
        simulationChangeRepository.saveAll(changeEntities);

        return savedScenario;
    }

    @Transactional(readOnly = true)
    public List<SimulationScenarioEntity> getScenariosByProject(Long projectId) {
        return simulationScenarioRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public SimulationScenarioEntity getScenarioById(Long id) {
        return simulationScenarioRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Simulation scenario not found"));
    }

    private SimulationResult executeSimulation(Long projectId, List<SimulationChangeItem> changes) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Project not found: " + projectId));

        // 1. Load baseline snapshots
        List<ResourceSnapshot> allResources = conflictDetectionService.loadResourceSnapshots();
        List<MilestoneSnapshot> allMilestones = conflictDetectionService.loadMilestoneSnapshots();
        List<AllocationSnapshot> baselineAllocations = conflictDetectionService.loadAllocationSnapshots();
        List<UnavailabilitySnapshot> allUnavailabilities = conflictDetectionService.loadUnavailabilitySnapshots();

        List<BudgetTransactionSnapshot> allTxns = budgetTransactionRepository.findAll().stream()
                .map(t -> new BudgetTransactionSnapshot(t.getId(), t.getProjectId(), t.getTxnDate(), t.getAmount(), t.getCategory(), t.getResourceId(), t.getNote()))
                .toList();

        Map<Long, ResourceSnapshot> resourceMap = allResources.stream()
                .collect(Collectors.toMap(ResourceSnapshot::id, Function.identity(), (a, b) -> a));
        Map<Long, MilestoneSnapshot> milestoneMap = allMilestones.stream()
                .collect(Collectors.toMap(MilestoneSnapshot::id, Function.identity(), (a, b) -> a));

        LocalDate today = LocalDate.now();
        int hoursPerDay = settingsService.getInt("hours.per.day", 8);
        int burnWindow = settingsService.getInt("burn.window.days", 14);
        int maxDays = settingsService.getInt("forecast.max.days", 1825);
        double burnoutThreshold = settingsService.getDouble("burnout.threshold.pct", 85.0);
        int burnoutWeeks = settingsService.getInt("burnout.consecutive.weeks", 3);
        double mismatchThreshold = settingsService.getDouble("skill.mismatch.pct", 50.0);
        int lookaheadDays = settingsService.getInt("unstaffed.lookahead.days", 14);

        // 2. Validate changes and build simulated copy
        List<AllocationSnapshot> simulatedAllocations = new ArrayList<>(baselineAllocations);
        AtomicLong tempIdGen = new AtomicLong(-1000);

        for (int i = 0; i < changes.size(); i++) {
            SimulationChangeItem item = changes.get(i);
            validateAndApplyChange(i, item, project, simulatedAllocations, resourceMap, milestoneMap, tempIdGen);
        }

        // 3. Run engines on baseline
        BudgetForecastResult baselineBudget = BudgetCalculator.calculateProjectForecast(
                project.getId(), project.getName(), project.getTotalBudget(), project.getStartDate(), project.getEndDate(),
                allMilestones, baselineAllocations, allResources, allUnavailabilities, allTxns, today, burnWindow, hoursPerDay
        );

        LocalDate baselineDelivery = null;
        for (MilestoneSnapshot ms : allMilestones) {
            if (ms.projectId().equals(project.getId()) && (ms.status() == MilestoneStatus.PLANNED || ms.status() == MilestoneStatus.IN_PROGRESS)) {
                MilestoneDeliveryForecast df = DeliveryForecaster.forecastMilestone(
                        ms, baselineAllocations, allResources, allUnavailabilities, today, hoursPerDay, maxDays
                );
                if (df.forecastFinishDate() != null) {
                    if (baselineDelivery == null || df.forecastFinishDate().isAfter(baselineDelivery)) {
                        baselineDelivery = df.forecastFinishDate();
                    }
                }
            }
        }

        LocalDate scanStart = today.minusDays(28);
        LocalDate scanEnd = project.getEndDate().plusDays(90);

        List<DetectedConflict> baselineConflicts = ConflictRules.evaluateConflicts(
                allResources, allMilestones, baselineAllocations, allUnavailabilities,
                scanStart, scanEnd, today, burnoutThreshold, burnoutWeeks, mismatchThreshold, lookaheadDays
        );

        // 4. Run engines on simulated copy
        BudgetForecastResult simulatedBudget = BudgetCalculator.calculateProjectForecast(
                project.getId(), project.getName(), project.getTotalBudget(), project.getStartDate(), project.getEndDate(),
                allMilestones, simulatedAllocations, allResources, allUnavailabilities, allTxns, today, burnWindow, hoursPerDay
        );

        LocalDate simulatedDelivery = null;
        for (MilestoneSnapshot ms : allMilestones) {
            if (ms.projectId().equals(project.getId()) && (ms.status() == MilestoneStatus.PLANNED || ms.status() == MilestoneStatus.IN_PROGRESS)) {
                MilestoneDeliveryForecast df = DeliveryForecaster.forecastMilestone(
                        ms, simulatedAllocations, allResources, allUnavailabilities, today, hoursPerDay, maxDays
                );
                if (df.forecastFinishDate() != null) {
                    if (simulatedDelivery == null || df.forecastFinishDate().isAfter(simulatedDelivery)) {
                        simulatedDelivery = df.forecastFinishDate();
                    }
                }
            }
        }

        List<DetectedConflict> simulatedConflicts = ConflictRules.evaluateConflicts(
                allResources, allMilestones, simulatedAllocations, allUnavailabilities,
                scanStart, scanEnd, today, burnoutThreshold, burnoutWeeks, mismatchThreshold, lookaheadDays
        );

        // 5. Diff conflicts (added vs removed)
        Set<String> baselineDedupKeys = baselineConflicts.stream()
                .map(DetectedConflict::dedupKey)
                .collect(Collectors.toSet());
        Set<String> simulatedDedupKeys = simulatedConflicts.stream()
                .map(DetectedConflict::dedupKey)
                .collect(Collectors.toSet());

        List<DetectedConflict> conflictsRemoved = baselineConflicts.stream()
                .filter(c -> !simulatedDedupKeys.contains(c.dedupKey()))
                .toList();
        List<DetectedConflict> conflictsAdded = simulatedConflicts.stream()
                .filter(c -> !baselineDedupKeys.contains(c.dedupKey()))
                .toList();

        // 6. Recommendation logic (Section 9.8)
        boolean hasAddedCritical = conflictsAdded.stream()
                .anyMatch(c -> c.severity() == ConflictSeverity.CRITICAL);

        boolean deliveryNotLater = (simulatedDelivery == null && baselineDelivery == null) ||
                (simulatedDelivery != null && baselineDelivery != null && !simulatedDelivery.isAfter(baselineDelivery));

        boolean withinBudget = simulatedBudget.planTotal().compareTo(project.getTotalBudget()) <= 0;

        String recommendation;
        String recommendationReason;

        if (hasAddedCritical) {
            recommendation = "DO_NOT_APPLY";
            recommendationReason = "The simulated changes introduce one or more CRITICAL conflicts.";
        } else if (conflictsAdded.isEmpty() && deliveryNotLater && withinBudget) {
            recommendation = "APPLY";
            recommendationReason = "Simulation resolves issues or maintains timeline without introducing new conflicts or exceeding budget.";
        } else {
            recommendation = "REVIEW";
            StringBuilder reason = new StringBuilder();
            if (baselineDelivery != null && simulatedDelivery != null) {
                long daysDiff = ChronoUnit.DAYS.between(simulatedDelivery, baselineDelivery);
                if (daysDiff > 0) {
                    reason.append(String.format("Delivery %d days earlier; ", daysDiff));
                } else if (daysDiff < 0) {
                    reason.append(String.format("Delivery %d days later; ", Math.abs(daysDiff)));
                }
            }
            BigDecimal costDiff = baselineBudget.planTotal().subtract(simulatedBudget.planTotal());
            if (costDiff.compareTo(BigDecimal.ZERO) > 0) {
                reason.append(String.format("planned cost ₹%s lower; ", costDiff.toPlainString()));
            } else if (costDiff.compareTo(BigDecimal.ZERO) < 0) {
                reason.append(String.format("planned cost ₹%s higher; ", costDiff.abs().toPlainString()));
            }
            if (!conflictsAdded.isEmpty()) {
                reason.append(String.format("%d new conflict(s) added.", conflictsAdded.size()));
            }
            recommendationReason = reason.toString().trim();
            if (recommendationReason.isEmpty()) {
                recommendationReason = "Review simulated trade-offs carefully before applying.";
            }
        }

        // 7. Resource peak loads comparison
        Set<Long> involvedResourceIds = new HashSet<>();
        for (SimulationChangeItem item : changes) {
            if (item.resourceId() != null) involvedResourceIds.add(item.resourceId());
            if (item.fromResourceId() != null) involvedResourceIds.add(item.fromResourceId());
            if (item.toResourceId() != null) involvedResourceIds.add(item.toResourceId());
        }

        Map<Long, Map<String, Integer>> resourcePeakLoads = new HashMap<>();
        for (Long resId : involvedResourceIds) {
            int basePeak = CapacityCalculator.calculatePeakLoad30Days(resId, today, baselineAllocations);
            int simPeak = CapacityCalculator.calculatePeakLoad30Days(resId, today, simulatedAllocations);
            resourcePeakLoads.put(resId, Map.of("baseline", basePeak, "simulated", simPeak));
        }

        return new SimulationResult(
                project.getId(),
                project.getName(),
                baselineBudget.planTotal(),
                simulatedBudget.planTotal(),
                baselineDelivery,
                simulatedDelivery,
                baselineBudget.runOutDate(),
                simulatedBudget.runOutDate(),
                conflictsRemoved,
                conflictsAdded,
                recommendation,
                recommendationReason,
                resourcePeakLoads,
                List.of()
        );
    }

    private void validateAndApplyChange(
            int index,
            SimulationChangeItem item,
            ProjectEntity project,
            List<AllocationSnapshot> allocations,
            Map<Long, ResourceSnapshot> resourceMap,
            Map<Long, MilestoneSnapshot> milestoneMap,
            AtomicLong tempIdGen
    ) {
        switch (item.type()) {
            case SWAP -> {
                if (item.allocationId() == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Change #" + index + ": allocationId is required for SWAP");
                }
                if (item.toResourceId() == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Change #" + index + ": toResourceId is required for SWAP");
                }
                int allocIdx = findAllocationIndex(allocations, item.allocationId());
                if (allocIdx < 0) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Change #" + index + ": Allocation not found: " + item.allocationId());
                }
                AllocationSnapshot oldAlloc = allocations.get(allocIdx);
                ResourceSnapshot targetRes = resourceMap.get(item.toResourceId());
                if (targetRes == null || targetRes.status() != ResourceStatus.ACTIVE) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Change #" + index + ": Target resource is invalid or inactive");
                }
                ResourceSnapshot sourceRes = resourceMap.get(oldAlloc.resourceId());
                if (sourceRes != null && sourceRes.type() != targetRes.type()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Change #" + index + ": Target resource must be of same type (" + sourceRes.type() + ")");
                }

                allocations.set(allocIdx, new AllocationSnapshot(
                        oldAlloc.id(), targetRes.id(), oldAlloc.milestoneId(), oldAlloc.projectId(),
                        oldAlloc.startDate(), oldAlloc.endDate(), oldAlloc.allocationPct(),
                        true, oldAlloc.milestoneStatus(), oldAlloc.projectStatus()
                ));
            }
            case CHANGE_PCT -> {
                if (item.allocationId() == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Change #" + index + ": allocationId is required for CHANGE_PCT");
                }
                int newPct = item.newPct() != null ? item.newPct() : (item.pct() != null ? item.pct() : 0);
                if (newPct < 1 || newPct > 100) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Change #" + index + ": Percentage must be 1 to 100");
                }
                int allocIdx = findAllocationIndex(allocations, item.allocationId());
                if (allocIdx < 0) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Change #" + index + ": Allocation not found: " + item.allocationId());
                }
                AllocationSnapshot oldAlloc = allocations.get(allocIdx);
                allocations.set(allocIdx, new AllocationSnapshot(
                        oldAlloc.id(), oldAlloc.resourceId(), oldAlloc.milestoneId(), oldAlloc.projectId(),
                        oldAlloc.startDate(), oldAlloc.endDate(), newPct,
                        oldAlloc.overrideConfirmed(), oldAlloc.milestoneStatus(), oldAlloc.projectStatus()
                ));
            }
            case ADD -> {
                if (item.resourceId() == null || item.milestoneId() == null || item.startDate() == null || item.endDate() == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Change #" + index + ": resourceId, milestoneId, startDate, and endDate required for ADD");
                }
                int pct = item.pct() != null ? item.pct() : 0;
                if (pct < 1 || pct > 100) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Change #" + index + ": Percentage must be 1 to 100");
                }
                MilestoneSnapshot ms = milestoneMap.get(item.milestoneId());
                if (ms == null || !ms.projectId().equals(project.getId())) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Change #" + index + ": Milestone does not belong to project");
                }
                if (item.startDate().isBefore(ms.startDate()) || item.endDate().isAfter(ms.endDate())) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Change #" + index + ": Dates outside milestone range");
                }
                ResourceSnapshot res = resourceMap.get(item.resourceId());
                if (res == null || res.status() != ResourceStatus.ACTIVE) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Change #" + index + ": Resource is invalid or inactive");
                }
                allocations.add(new AllocationSnapshot(
                        tempIdGen.getAndDecrement(), res.id(), ms.id(), project.getId(),
                        item.startDate(), item.endDate(), pct,
                        false, ms.status(), project.getStatus()
                ));
            }
            case REMOVE -> {
                if (item.allocationId() == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Change #" + index + ": allocationId is required for REMOVE");
                }
                int allocIdx = findAllocationIndex(allocations, item.allocationId());
                if (allocIdx < 0) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Change #" + index + ": Allocation not found: " + item.allocationId());
                }
                allocations.remove(allocIdx);
            }
        }
    }

    private int findAllocationIndex(List<AllocationSnapshot> allocations, Long allocationId) {
        for (int i = 0; i < allocations.size(); i++) {
            if (allocations.get(i).id().equals(allocationId)) {
                return i;
            }
        }
        return -1;
    }
}
