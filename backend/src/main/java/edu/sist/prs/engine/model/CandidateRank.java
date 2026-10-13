package edu.sist.prs.engine.model;

import java.util.List;

/**
 * Candidate ranking result for assigning developers to milestones.
 */
public record CandidateRank(
        Long resourceId,
        String resourceName,
        double matchPct,
        double skillScore,
        double fitScore,
        double cheapnessScore,
        double totalScore,
        int minRemaining,
        int peakLoad30d,
        boolean wouldOverload,
        List<String> missingSkills
) {}
