package com.sist.rms.service;

import com.sist.rms.dto.ConflictResponse;
import com.sist.rms.dto.SimulationRequest;
import com.sist.rms.dto.SimulationResponse;
import com.sist.rms.entity.*;
import com.sist.rms.exception.ResourceNotFoundException;
import com.sist.rms.exception.ValidationException;
import com.sist.rms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * WHAT-IF RESOURCE SWAP SIMULATOR
 *
 * Question it answers: "what happens if developer A on this milestone is
 * replaced by developer B?"
 *
 * VERY IMPORTANT: run() performs NO insert, update or delete. Every number
 * is worked out in memory from a COPY of the data. A row is written to
 * simulation_scenarios only when the manager calls save().
 *
 * What is compared:
 *   cost          - the cost formula in CostService, over the same dates
 *   skill match   - how many of the milestone's required skills each has
 *   conflicts     - conflicts of the person doing the work, before and after
 *   delivery date - the formula in DeliveryEstimateService, using the
 *                   milestone's total capacity with the swap applied
 */
@Service
public class SimulationService {

    private final SimulationScenarioRepository scenarioRepository;
    private final ResourceAllocationRepository allocationRepository;
    private final DeveloperRepository developerRepository;
    private final DeveloperLeaveRepository leaveRepository;
    private final MilestoneRepository milestoneRepository;
    private final CostService costService;
    private final SkillGapService skillGapService;
    private final ConflictService conflictService;
    private final DeliveryEstimateService deliveryEstimateService;

    public SimulationService(SimulationScenarioRepository scenarioRepository,
                             ResourceAllocationRepository allocationRepository,
                             DeveloperRepository developerRepository,
                             DeveloperLeaveRepository leaveRepository,
                             MilestoneRepository milestoneRepository,
                             CostService costService,
                             SkillGapService skillGapService,
                             ConflictService conflictService,
                             DeliveryEstimateService deliveryEstimateService) {
        this.scenarioRepository = scenarioRepository;
        this.allocationRepository = allocationRepository;
        this.developerRepository = developerRepository;
        this.leaveRepository = leaveRepository;
        this.milestoneRepository = milestoneRepository;
        this.costService = costService;
        this.skillGapService = skillGapService;
        this.conflictService = conflictService;
        this.deliveryEstimateService = deliveryEstimateService;
    }

    // -----------------------------------------------------------------
    //  RUN - calculate only, never write
    // -----------------------------------------------------------------

    public SimulationResponse run(SimulationRequest request) {
        Milestone milestone = milestoneRepository.findById(request.getMilestoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", request.getMilestoneId()));
        Developer currentDeveloper = developerRepository.findById(request.getCurrentDeveloperId())
                .orElseThrow(() -> new ResourceNotFoundException("Developer", request.getCurrentDeveloperId()));
        Developer newDeveloper = developerRepository.findById(request.getNewDeveloperId())
                .orElseThrow(() -> new ResourceNotFoundException("Developer", request.getNewDeveloperId()));

        if (currentDeveloper.getId().equals(newDeveloper.getId())) {
            throw new ValidationException("The replacement developer must be different from the current developer");
        }

        // the real allocation being replaced
        ResourceAllocation currentAllocation = null;
        List<ResourceAllocation> milestoneAllocations =
                allocationRepository.findByMilestoneId(milestone.getId());
        for (ResourceAllocation allocation : milestoneAllocations) {
            if (allocation.getDeveloper().getId().equals(currentDeveloper.getId())) {
                currentAllocation = allocation;
                break;
            }
        }
        if (currentAllocation == null) {
            throw new ValidationException(currentDeveloper.getName()
                    + " is not allocated to the milestone " + milestone.getName());
        }

        int currentPercentage = currentAllocation.getAllocationPercentage();
        int simulatedPercentage = request.getNewAllocationPercentage() == null
                ? currentPercentage : request.getNewAllocationPercentage();

        LocalDate from = currentAllocation.getStartDate();
        LocalDate to = currentAllocation.getEndDate();

        SimulationResponse result = new SimulationResponse();
        result.setSaved(false);
        result.setScenarioName(request.getScenarioName());
        result.setProjectId(milestone.getProject().getId());
        result.setProjectName(milestone.getProject().getName());
        result.setMilestoneId(milestone.getId());
        result.setMilestoneName(milestone.getName());
        result.setCurrentDeveloperName(currentDeveloper.getName());
        result.setNewDeveloperName(newDeveloper.getName());
        result.setCurrentHourlyCost(currentDeveloper.getHourlyCost());
        result.setNewHourlyCost(newDeveloper.getHourlyCost());
        result.setCurrentAllocationPercentage(currentPercentage);
        result.setSimulatedAllocationPercentage(simulatedPercentage);

        // ---- cost ----
        BigDecimal currentCost = costService.calculateCost(currentDeveloper, from, to, currentPercentage);
        BigDecimal simulatedCost = costService.calculateCost(newDeveloper, from, to, simulatedPercentage);
        result.setCurrentCost(currentCost);
        result.setSimulatedCost(simulatedCost);
        result.setBudgetImpact(simulatedCost.subtract(currentCost));

        // ---- skill match ----
        result.setCurrentSkillMatch(skillGapService.skillMatchPercentage(currentDeveloper.getId(), milestone.getId()));
        result.setSimulatedSkillMatch(skillGapService.skillMatchPercentage(newDeveloper.getId(), milestone.getId()));
        result.setCurrentMissingSkills(skillGapService.missingSkillsForDeveloper(currentDeveloper.getId(), milestone.getId()));
        result.setSimulatedMissingSkills(skillGapService.missingSkillsForDeveloper(newDeveloper.getId(), milestone.getId()));

        // ---- conflicts ----
        result.setCurrentConflicts(conflictService.findConflictsForDeveloper(currentDeveloper.getId()).size());

        // build the HYPOTHETICAL allocation list of the replacement developer.
        // The extra allocation object is created with "new" and never saved,
        // so the database is untouched.
        List<ResourceAllocation> hypothetical =
                new ArrayList<>(allocationRepository.findByDeveloperId(newDeveloper.getId()));
        ResourceAllocation pretend = new ResourceAllocation();
        pretend.setDeveloper(newDeveloper);
        pretend.setProject(milestone.getProject());
        pretend.setMilestone(milestone);
        pretend.setStartDate(from);
        pretend.setEndDate(to);
        pretend.setAllocationPercentage(simulatedPercentage);
        hypothetical.add(pretend);

        List<DeveloperLeave> newDeveloperLeaves = leaveRepository.findByDeveloperId(newDeveloper.getId());
        List<ConflictResponse> simulatedConflicts = new ArrayList<>();
        simulatedConflicts.addAll(conflictService.detectOverAllocation(newDeveloper, hypothetical));
        simulatedConflicts.addAll(conflictService.detectLeaveConflicts(newDeveloper, hypothetical, newDeveloperLeaves));

        result.setSimulatedConflicts(simulatedConflicts.size());
        List<String> conflictDetails = new ArrayList<>();
        for (ConflictResponse conflict : simulatedConflicts) {
            conflictDetails.add(conflict.getMessage());
        }
        result.setSimulatedConflictDetails(conflictDetails);

        // ---- delivery date ----
        int currentTotalOnMilestone = 0;
        for (ResourceAllocation allocation : milestoneAllocations) {
            currentTotalOnMilestone += allocation.getAllocationPercentage();
        }
        int simulatedTotalOnMilestone = currentTotalOnMilestone - currentPercentage + simulatedPercentage;

        LocalDate currentDelivery = deliveryEstimateService.estimateDeliveryDate(milestone, currentTotalOnMilestone);
        LocalDate simulatedDelivery = deliveryEstimateService.estimateDeliveryDate(milestone, simulatedTotalOnMilestone);
        result.setCurrentDeliveryDate(currentDelivery);
        result.setSimulatedDeliveryDate(simulatedDelivery);

        if (currentDelivery != null && simulatedDelivery != null) {
            long impact = ChronoUnit.DAYS.between(currentDelivery, simulatedDelivery);
            result.setDeliveryImpactDays(impact);
            if (impact > 0) {
                result.setDeliveryImpactMessage("Delivery moves " + impact + " day(s) LATER.");
            } else if (impact < 0) {
                result.setDeliveryImpactMessage("Delivery moves " + Math.abs(impact) + " day(s) EARLIER.");
            } else {
                result.setDeliveryImpactMessage("Delivery date does not change.");
            }
        } else {
            result.setDeliveryImpactMessage("Delivery date cannot be estimated without an allocation.");
        }

        result.setRecommendation(buildRecommendation(result));
        return result;
    }

    /** A plain if-else summary - no prediction, just the numbers restated. */
    private String buildRecommendation(SimulationResponse result) {
        if (result.getSimulatedSkillMatch() < result.getCurrentSkillMatch()) {
            return "NOT RECOMMENDED: skill match drops from " + result.getCurrentSkillMatch()
                    + "% to " + result.getSimulatedSkillMatch() + "%. Missing: "
                    + String.join(", ", result.getSimulatedMissingSkills());
        }
        if (result.getSimulatedConflicts() > result.getCurrentConflicts()) {
            return "NOT RECOMMENDED: the swap raises the number of conflicts from "
                    + result.getCurrentConflicts() + " to " + result.getSimulatedConflicts() + ".";
        }
        boolean costsMore = result.getBudgetImpact().compareTo(BigDecimal.ZERO) > 0;
        boolean deliversLater = result.getDeliveryImpactDays() != null && result.getDeliveryImpactDays() > 0;

        if (costsMore && deliversLater) {
            return "NOT RECOMMENDED: the swap costs more AND delivers later.";
        }
        if (costsMore) {
            return "POSSIBLE: the swap costs " + result.getBudgetImpact()
                    + " more, but skill match and conflicts do not get worse.";
        }
        if (deliversLater) {
            return "POSSIBLE: the swap is cheaper, but delivery moves later by "
                    + result.getDeliveryImpactDays() + " day(s).";
        }
        return "RECOMMENDED: the swap does not cost more, does not delay delivery and adds no conflicts.";
    }

    // -----------------------------------------------------------------
    //  SAVE - only when the manager asks for it
    // -----------------------------------------------------------------

    @Transactional
    public SimulationResponse save(SimulationRequest request) {
        SimulationResponse result = run(request);

        String name = request.getScenarioName() == null ? "" : request.getScenarioName().trim();
        if (name.isEmpty()) {
            throw new ValidationException("Scenario name is required to save a scenario");
        }

        SimulationScenario scenario = new SimulationScenario();
        scenario.setScenarioName(name);
        scenario.setProject(milestoneRepository.findById(request.getMilestoneId()).get().getProject());
        scenario.setMilestone(milestoneRepository.findById(request.getMilestoneId()).get());
        scenario.setCurrentDeveloper(developerRepository.findById(request.getCurrentDeveloperId()).get());
        scenario.setNewDeveloper(developerRepository.findById(request.getNewDeveloperId()).get());
        scenario.setAllocationPercentage(result.getSimulatedAllocationPercentage());
        scenario.setCurrentCost(result.getCurrentCost());
        scenario.setSimulatedCost(result.getSimulatedCost());
        scenario.setCurrentSkillMatch(result.getCurrentSkillMatch());
        scenario.setSimulatedSkillMatch(result.getSimulatedSkillMatch());
        scenario.setCurrentConflicts(result.getCurrentConflicts());
        scenario.setSimulatedConflicts(result.getSimulatedConflicts());
        scenario.setCurrentDeliveryDate(result.getCurrentDeliveryDate());
        scenario.setSimulatedDeliveryDate(result.getSimulatedDeliveryDate());
        scenario.setBudgetImpact(result.getBudgetImpact());
        scenario.setCreatedAt(java.time.LocalDateTime.now());
        scenario = scenarioRepository.save(scenario);

        result.setId(scenario.getId());
        result.setSaved(true);
        result.setScenarioName(scenario.getScenarioName());
        result.setCreatedAt(scenario.getCreatedAt());
        return result;
    }

    public List<SimulationResponse> findAllSaved() {
        List<SimulationResponse> responses = new ArrayList<>();
        for (SimulationScenario scenario : scenarioRepository.findAllByOrderByCreatedAtDesc()) {
            responses.add(toResponse(scenario));
        }
        return responses;
    }

    public SimulationResponse findSavedById(Long id) {
        return toResponse(getScenarioOrThrow(id));
    }

    @Transactional
    public void delete(Long id) {
        scenarioRepository.delete(getScenarioOrThrow(id));
    }

    public SimulationScenario getScenarioOrThrow(Long id) {
        return scenarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Simulation scenario", id));
    }

    /** Rebuilds the response from a saved row. */
    public SimulationResponse toResponse(SimulationScenario scenario) {
        SimulationResponse response = new SimulationResponse();
        response.setId(scenario.getId());
        response.setSaved(true);
        response.setScenarioName(scenario.getScenarioName());
        response.setProjectId(scenario.getProject().getId());
        response.setProjectName(scenario.getProject().getName());
        if (scenario.getMilestone() != null) {
            response.setMilestoneId(scenario.getMilestone().getId());
            response.setMilestoneName(scenario.getMilestone().getName());
            response.setSimulatedMissingSkills(skillGapService.missingSkillsForDeveloper(
                    scenario.getNewDeveloper().getId(), scenario.getMilestone().getId()));
            response.setCurrentMissingSkills(skillGapService.missingSkillsForDeveloper(
                    scenario.getCurrentDeveloper().getId(), scenario.getMilestone().getId()));
        }
        response.setCurrentDeveloperName(scenario.getCurrentDeveloper().getName());
        response.setNewDeveloperName(scenario.getNewDeveloper().getName());
        response.setCurrentHourlyCost(scenario.getCurrentDeveloper().getHourlyCost());
        response.setNewHourlyCost(scenario.getNewDeveloper().getHourlyCost());
        response.setCurrentAllocationPercentage(scenario.getAllocationPercentage());
        response.setSimulatedAllocationPercentage(scenario.getAllocationPercentage());
        response.setCurrentCost(scenario.getCurrentCost());
        response.setSimulatedCost(scenario.getSimulatedCost());
        response.setBudgetImpact(scenario.getBudgetImpact());
        response.setCurrentSkillMatch(scenario.getCurrentSkillMatch());
        response.setSimulatedSkillMatch(scenario.getSimulatedSkillMatch());
        response.setCurrentConflicts(scenario.getCurrentConflicts());
        response.setSimulatedConflicts(scenario.getSimulatedConflicts());
        response.setCurrentDeliveryDate(scenario.getCurrentDeliveryDate());
        response.setSimulatedDeliveryDate(scenario.getSimulatedDeliveryDate());
        response.setCreatedAt(scenario.getCreatedAt());

        if (scenario.getCurrentDeliveryDate() != null && scenario.getSimulatedDeliveryDate() != null) {
            long impact = ChronoUnit.DAYS.between(scenario.getCurrentDeliveryDate(), scenario.getSimulatedDeliveryDate());
            response.setDeliveryImpactDays(impact);
            response.setDeliveryImpactMessage(impact == 0 ? "Delivery date does not change."
                    : (impact > 0 ? "Delivery moves " + impact + " day(s) LATER."
                                  : "Delivery moves " + Math.abs(impact) + " day(s) EARLIER."));
        }
        response.setRecommendation(buildRecommendation(response));
        return response;
    }
}
