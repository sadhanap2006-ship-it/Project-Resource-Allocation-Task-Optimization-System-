package edu.sist.prs.engine;

import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.engine.model.CandidateRank;
import edu.sist.prs.engine.model.MilestoneSnapshot;
import edu.sist.prs.engine.model.ResourceSkillSnapshot;
import edu.sist.prs.engine.model.ResourceSnapshot;
import edu.sist.prs.engine.model.SkillRequirementSnapshot;
import edu.sist.prs.engine.model.UnavailabilitySnapshot;
import edu.sist.prs.entity.ResourceStatus;
import edu.sist.prs.entity.ResourceType;
import edu.sist.prs.util.DateRanges;
import edu.sist.prs.util.WorkingDays;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Pure calculation engine for skill matching, skill gaps, and candidate ranking.
 * Implements formulas from Section 9.3 and Section 16.1.
 */
public final class SkillMatcher {

    private SkillMatcher() {}

    /**
     * Credit of resource r on skill requirement s:
     * credit(r, s) = 0 if r lacks s
     *              = min(1.0, proficiency(r, s) / min_proficiency(s)) otherwise
     */
    public static double calculateCredit(ResourceSnapshot resource, SkillRequirementSnapshot req) {
        if (resource.skills() == null || resource.skills().isEmpty()) {
            return 0.0;
        }

        Optional<ResourceSkillSnapshot> match = resource.skills().stream()
                .filter(rs -> rs.skillId().equals(req.skillId()))
                .findFirst();

        if (match.isEmpty()) {
            return 0.0;
        }

        int proficiency = match.get().proficiency();
        double ratio = (double) proficiency / (double) req.minProficiency();
        return Math.min(1.0, ratio);
    }

    /**
     * Skill match percentage of resource r on milestone m:
     * match(r, m) = (Σ_s credit(r, s) / |S|) * 100; match = 100 when S is empty.
     */
    public static double calculateMatchPercentage(ResourceSnapshot resource, MilestoneSnapshot milestone) {
        List<SkillRequirementSnapshot> reqs = milestone.skillRequirements();
        if (reqs == null || reqs.isEmpty()) {
            return 100.0;
        }

        double totalCredit = 0.0;
        for (SkillRequirementSnapshot req : reqs) {
            totalCredit += calculateCredit(resource, req);
        }

        return (totalCredit / (double) reqs.size()) * 100.0;
    }

    /**
     * List of skill requirements not met at required min_proficiency by developer.
     */
    public static List<String> findMissingSkills(ResourceSnapshot resource, MilestoneSnapshot milestone) {
        List<String> missing = new ArrayList<>();
        List<SkillRequirementSnapshot> reqs = milestone.skillRequirements();
        if (reqs == null || reqs.isEmpty()) {
            return missing;
        }

        for (SkillRequirementSnapshot req : reqs) {
            Optional<ResourceSkillSnapshot> match = resource.skills() == null
                    ? Optional.empty()
                    : resource.skills().stream().filter(rs -> rs.skillId().equals(req.skillId())).findFirst();

            if (match.isEmpty() || match.get().proficiency() < req.minProficiency()) {
                missing.add(req.skillName());
            }
        }
        return missing;
    }

    /**
     * Determines whether developer qualifies as a candidate:
     * Active developer, and if mandatory skills exist, credit > 0 on at least one mandatory skill.
     */
    public static boolean isEligibleCandidate(ResourceSnapshot resource, MilestoneSnapshot milestone) {
        if (resource.type() != ResourceType.DEVELOPER || resource.status() != ResourceStatus.ACTIVE) {
            return false;
        }

        List<SkillRequirementSnapshot> mandatoryReqs = milestone.skillRequirements().stream()
                .filter(SkillRequirementSnapshot::mandatory)
                .toList();

        if (mandatoryReqs.isEmpty()) {
            return true;
        }

        double totalMandatoryCredit = 0.0;
        for (SkillRequirementSnapshot req : mandatoryReqs) {
            totalMandatoryCredit += calculateCredit(resource, req);
        }

        return totalMandatoryCredit > 0.0;
    }

    /**
     * Candidate ranking per Section 9.3:
     * P = allocation percentage target (1 to 100)
     * Candidates: ACTIVE DEVELOPER resources, excluding those with credit 0 on every mandatory skill.
     * skill = match / 100.0
     * minRemaining = min of remaining(r, d) over working days d of the milestone where capacity(r, d) > 0;
     *                if no such days, fit = 0.0
     * fit = clamp(minRemaining / P, 0.0, 1.0)
     * costNorm = (rate - minRate) / (maxRate - minRate); 0 when maxRate == minRate
     * cheapness = 1.0 - costNorm
     * score = w_skill * skill + w_fit * fit + w_cost * cheapness
     * wouldOverload = minRemaining < P
     * Sort by score desc, then name asc.
     */
    public static List<CandidateRank> rankCandidates(
            MilestoneSnapshot milestone,
            int targetPct,
            Collection<ResourceSnapshot> allResources,
            Collection<AllocationSnapshot> allAllocations,
            Collection<UnavailabilitySnapshot> allUnavailabilities,
            double weightSkill,
            double weightFit,
            double weightCost,
            LocalDate today
    ) {
        List<ResourceSnapshot> candidates = allResources.stream()
                .filter(r -> isEligibleCandidate(r, milestone))
                .toList();

        if (candidates.isEmpty()) {
            return List.of();
        }

        BigDecimal minCost = candidates.stream()
                .map(ResourceSnapshot::costRate)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxCost = candidates.stream()
                .map(ResourceSnapshot::costRate)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        double costRange = maxCost.subtract(minCost).doubleValue();

        List<CandidateRank> results = new ArrayList<>();

        for (ResourceSnapshot cand : candidates) {
            double matchPct = calculateMatchPercentage(cand, milestone);
            double skillScore = matchPct / 100.0;

            // Calculate minRemaining across working days of milestone where capacity > 0
            LocalDate current = milestone.startDate();
            LocalDate end = milestone.endDate();
            int minRem = Integer.MAX_VALUE;
            boolean hasWorkingDay = false;

            while (!current.isAfter(end)) {
                if (WorkingDays.isWorkingDay(current)) {
                    int cap = CapacityCalculator.calculateCapacity(cand, current, allUnavailabilities);
                    if (cap > 0) {
                        hasWorkingDay = true;
                        int rem = CapacityCalculator.calculateRemaining(cand, current, allAllocations, allUnavailabilities);
                        if (rem < minRem) {
                            minRem = rem;
                        }
                    }
                }
                current = current.plusDays(1);
            }

            int finalMinRemaining = hasWorkingDay ? minRem : 0;
            double fitScore;
            if (!hasWorkingDay) {
                fitScore = 0.0;
            } else {
                fitScore = DateRanges.clamp((double) finalMinRemaining / (double) targetPct, 0.0, 1.0);
            }

            double cheapnessScore;
            if (costRange <= 0.0) {
                cheapnessScore = 1.0;
            } else {
                double costNorm = (cand.costRate().subtract(minCost).doubleValue()) / costRange;
                cheapnessScore = 1.0 - costNorm;
            }

            double totalScore = (weightSkill * skillScore) + (weightFit * fitScore) + (weightCost * cheapnessScore);
            boolean wouldOverload = finalMinRemaining < targetPct;
            List<String> missingSkills = findMissingSkills(cand, milestone);
            int peakLoad30d = CapacityCalculator.calculatePeakLoad30Days(cand.id(), today, allAllocations);

            results.add(new CandidateRank(
                    cand.id(),
                    cand.name(),
                    matchPct,
                    skillScore,
                    fitScore,
                    cheapnessScore,
                    totalScore,
                    finalMinRemaining,
                    peakLoad30d,
                    wouldOverload,
                    missingSkills
            ));
        }

        // Sort by totalScore desc, then name asc
        results.sort(Comparator.comparingDouble(CandidateRank::totalScore).reversed()
                .thenComparing(CandidateRank::resourceName));

        return results;
    }
}
