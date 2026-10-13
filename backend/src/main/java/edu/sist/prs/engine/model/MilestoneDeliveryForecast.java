package edu.sist.prs.engine.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MilestoneDeliveryForecast(
        Long milestoneId,
        BigDecimal estimatedEffortHours,
        BigDecimal remainingEffortHours,
        LocalDate forecastFinishDate,
        long delayWorkingDays,
        String forecastStatus,
        String message,
        boolean extended
) {}
