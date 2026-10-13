package edu.sist.prs.engine.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record SimulationResult(
        Long projectId,
        String projectName,
        BigDecimal baselinePlanTotal,
        BigDecimal simulatedPlanTotal,
        LocalDate baselineForecastDelivery,
        LocalDate simulatedForecastDelivery,
        LocalDate baselineRunOutDate,
        LocalDate simulatedRunOutDate,
        List<DetectedConflict> conflictsRemoved,
        List<DetectedConflict> conflictsAdded,
        String recommendation,
        String recommendationReason,
        Map<Long, Map<String, Integer>> resourcePeakLoads,
        List<Map<String, Object>> allocationComparisons
) {}
