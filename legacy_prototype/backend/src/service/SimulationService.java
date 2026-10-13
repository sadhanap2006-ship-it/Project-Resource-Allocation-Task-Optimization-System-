package service;

import dao.AllocationDAO;
import dao.DeveloperDAO;
import dao.MilestoneDAO;
import dao.SimulationDAO;
import dao.SkillDAO;
import model.Allocation;
import model.Developer;
import model.Milestone;
import model.WhatIfScenario;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class SimulationService {

    private final MilestoneDAO milestoneDAO = new MilestoneDAO();
    private final DeveloperDAO developerDAO = new DeveloperDAO();
    private final AllocationDAO allocationDAO = new AllocationDAO();
    private final SkillDAO skillDAO = new SkillDAO();
    private final SimulationDAO simulationDAO = new SimulationDAO();

    // In-memory simulation cache for instant retrieval/apply
    private static final Map<Integer, WhatIfScenario> SIMULATION_CACHE = new HashMap<>();
    private static int SIM_ID_COUNTER = 1000;

    public WhatIfScenario simulateSwap(int milestoneId, int currentDevId, int proposedDevId, int newAllocPct) throws SQLException {
        Milestone m = milestoneDAO.findById(milestoneId);
        Developer curDev = developerDAO.findById(currentDevId);
        Developer propDev = developerDAO.findById(proposedDevId);

        if (m == null || curDev == null || propDev == null) {
            return null;
        }

        WhatIfScenario scenario = new WhatIfScenario();
        int simId = ++SIM_ID_COUNTER;
        scenario.setId(simId);
        scenario.setMilestoneId(m.getId());
        scenario.setMilestoneName(m.getName());
        scenario.setProjectId(m.getProjectId());
        scenario.setProjectName(m.getProjectName());

        // Current Developer baseline
        scenario.setCurrentDeveloperId(curDev.getId());
        scenario.setCurrentDeveloperName(curDev.getName());
        scenario.setCurrentCostPerHour(curDev.getHourlyCost());
        double effortHours = m.getEstimatedEffortHours();
        double currentCost = effortHours * curDev.getHourlyCost();
        scenario.setCurrentMilestoneCost(Math.round(currentCost * 100.0) / 100.0);
        scenario.setCurrentDeliveryDate(m.getEndDate());

        // Current skill match
        List<String> requiredSkills = skillDAO.findSkillsByMilestoneId(m.getId());
        List<String> curSkills = skillDAO.findSkillsByDeveloperId(curDev.getId());
        int curMatchCount = 0;
        for (String r : requiredSkills) {
            if (curSkills.contains(r)) curMatchCount++;
        }
        int curMatchPct = requiredSkills.isEmpty() ? 100 : (int) Math.round(((double) curMatchCount / requiredSkills.size()) * 100);
        scenario.setCurrentSkillMatchPercentage(curMatchPct);

        // Proposed Developer simulated state
        scenario.setProposedDeveloperId(propDev.getId());
        scenario.setProposedDeveloperName(propDev.getName());
        scenario.setProposedCostPerHour(propDev.getHourlyCost());
        double simCost = effortHours * propDev.getHourlyCost();
        scenario.setSimulatedMilestoneCost(Math.round(simCost * 100.0) / 100.0);

        // Capacity and Delivery Date shift calculation
        // Baseline assumed 100% capacity ratio. If proposed dev allocated at newAllocPct vs remaining capacity:
        scenario.setAllocationPercentage(newAllocPct);
        LocalDate milestoneStart = LocalDate.parse(m.getStartDate());
        LocalDate milestoneEnd = LocalDate.parse(m.getEndDate());
        long scheduledDays = ChronoUnit.DAYS.between(milestoneStart, milestoneEnd);
        if (scheduledDays <= 0) scheduledDays = 14;

        // Higher allocation expedites; lower allocation delays
        double speedRatio = ((double) newAllocPct) / 60.0; // Normalized around standard 60% load
        long adjustedDays = Math.max(5, Math.round(scheduledDays / speedRatio));
        LocalDate simulatedEnd = milestoneStart.plusDays(adjustedDays);
        int deliveryDelta = (int) ChronoUnit.DAYS.between(milestoneEnd, simulatedEnd);

        scenario.setSimulatedDeliveryDate(simulatedEnd.toString());
        scenario.setDeliveryDaysDelta(deliveryDelta);

        // Proposed skill match
        List<String> propSkills = skillDAO.findSkillsByDeveloperId(propDev.getId());
        int propMatchCount = 0;
        List<String> missingSkills = new ArrayList<>();
        for (String r : requiredSkills) {
            if (propSkills.contains(r)) propMatchCount++;
            else missingSkills.add(r);
        }
        int propMatchPct = requiredSkills.isEmpty() ? 100 : (int) Math.round(((double) propMatchCount / requiredSkills.size()) * 100);
        scenario.setSimulatedSkillMatchPercentage(propMatchPct);

        // Cost differences
        double costDiff = simCost - currentCost;
        scenario.setMilestoneCostDifference(Math.round(costDiff * 100.0) / 100.0);
        scenario.setCostDifferencePerHour(propDev.getHourlyCost() - curDev.getHourlyCost());

        // Simulated conflict checks
        List<Allocation> propAllocs = allocationDAO.findByDeveloperId(propDev.getId());
        int simulatedCumulative = newAllocPct;
        for (Allocation a : propAllocs) {
            if (a.getMilestoneId() != m.getId()) {
                simulatedCumulative += a.getAllocationPercentage();
            }
        }

        List<String> simConflicts = new ArrayList<>();
        if (simulatedCumulative > 100) {
            simConflicts.add("OVER_ALLOCATION: " + propDev.getName() + " will reach " + simulatedCumulative + "% total allocation.");
        }
        if (!missingSkills.isEmpty()) {
            simConflicts.add("SKILL_GAP: " + propDev.getName() + " lacks " + String.join(", ", missingSkills));
        }
        scenario.setSimulatedConflicts(simConflicts);

        // Determine Scenario Verdict
        String verdict;
        String recommendation;
        if (simulatedCumulative > 100) {
            verdict = "CRITICAL_RISK";
            recommendation = "Reject swap: " + propDev.getName() + " would exceed 100% capacity (" + simulatedCumulative + "%).";
        } else if (propMatchPct < 50) {
            verdict = "SUBOPTIMAL";
            recommendation = "Caution: " + propDev.getName() + " only matches " + propMatchPct + "% of required skills (" + String.join(", ", missingSkills) + " missing).";
        } else if (costDiff <= 0 && deliveryDelta <= 0) {
            verdict = "OPTIMAL";
            recommendation = "Highly Recommended: Swap saves ₹" + Math.abs(costDiff) + " and delivers on or ahead of schedule.";
        } else {
            verdict = "ACCEPTABLE";
            recommendation = "Feasible: Delivery shift of " + (deliveryDelta >= 0 ? "+" : "") + deliveryDelta + " days with cost delta ₹" + costDiff + ".";
        }

        scenario.setScenarioVerdict(verdict);
        scenario.setRecommendation(recommendation);

        // Cache in memory (Real DB is NOT modified!)
        SIMULATION_CACHE.put(simId, scenario);
        return scenario;
    }

    public WhatIfScenario getSimulation(int id) {
        return SIMULATION_CACHE.get(id);
    }

    public dto.SimulationResponseDTO getSimulationDTO(int id) {
        WhatIfScenario s = SIMULATION_CACHE.get(id);
        if (s == null) return null;
        return mapToDTO(s);
    }

    public dto.SimulationResponseDTO mapToDTO(WhatIfScenario s) {
        dto.SimulationResponseDTO dto = new dto.SimulationResponseDTO();
        dto.setId(s.getId());
        dto.setMilestoneId(s.getMilestoneId());
        dto.setMilestoneName(s.getMilestoneName());
        dto.setProjectId(s.getProjectId());
        dto.setProjectName(s.getProjectName());
        dto.setCurrentDeveloperId(s.getCurrentDeveloperId());
        dto.setCurrentDeveloperName(s.getCurrentDeveloperName());
        dto.setCurrentCostPerHour(s.getCurrentCostPerHour());
        dto.setCurrentMilestoneCost(s.getCurrentMilestoneCost());
        dto.setCurrentDeliveryDate(s.getCurrentDeliveryDate());
        dto.setCurrentSkillMatchPercentage(s.getCurrentSkillMatchPercentage());
        dto.setProposedDeveloperId(s.getProposedDeveloperId());
        dto.setProposedDeveloperName(s.getProposedDeveloperName());
        dto.setProposedCostPerHour(s.getProposedCostPerHour());
        dto.setSimulatedMilestoneCost(s.getSimulatedMilestoneCost());
        dto.setSimulatedDeliveryDate(s.getSimulatedDeliveryDate());
        dto.setDeliveryDaysDelta(s.getDeliveryDaysDelta());
        dto.setSimulatedSkillMatchPercentage(s.getSimulatedSkillMatchPercentage());
        dto.setMilestoneCostDifference(s.getMilestoneCostDifference());
        dto.setCostDifferencePerHour(s.getCostDifferencePerHour());
        dto.setAllocationPercentage(s.getAllocationPercentage());
        dto.setSimulatedConflicts(s.getSimulatedConflicts());
        dto.setScenarioVerdict(s.getScenarioVerdict());
        dto.setRecommendation(s.getRecommendation());
        return dto;
    }

    public boolean applySimulation(int id) throws SQLException {
        WhatIfScenario scenario = SIMULATION_CACHE.get(id);
        if (scenario == null) return false;

        // Persist scenario record
        simulationDAO.insert(scenario);

        // Update real database allocation
        // Remove existing allocation on milestone for current developer
        allocationDAO.deleteByMilestoneAndDeveloper(scenario.getMilestoneId(), scenario.getCurrentDeveloperId());

        // Insert new allocation for proposed developer
        Milestone m = milestoneDAO.findById(scenario.getMilestoneId());
        Developer propDev = developerDAO.findById(scenario.getProposedDeveloperId());
        if (m != null && propDev != null) {
            Allocation newAlloc = new Allocation();
            newAlloc.setDeveloperId(propDev.getId());
            newAlloc.setProjectId(m.getProjectId());
            newAlloc.setMilestoneId(m.getId());
            newAlloc.setAllocationPercentage(scenario.getAllocationPercentage());
            newAlloc.setStartDate(m.getStartDate());
            newAlloc.setEndDate(m.getEndDate());
            newAlloc.setHourlyCostSnapshot(propDev.getHourlyCost());
            newAlloc.setEstimatedHours(m.getEstimatedEffortHours());
            allocationDAO.insert(newAlloc);
            return true;
        }

        return false;
    }
}
