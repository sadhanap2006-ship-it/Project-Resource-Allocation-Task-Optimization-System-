package com.sist.rms.service;

import com.sist.rms.entity.Milestone;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * DELIVERY DATE FORMULA (simple and deterministic - no machine learning)
 *
 * Every milestone stores estimatedEffortDays, measured in DEVELOPER-DAYS.
 * "20 developer-days" means one full-time developer would need 20 days.
 *
 *   capacity      = (sum of allocation percentages on the milestone) / 100
 *   duration_days = ceil( estimatedEffortDays / capacity )
 *   delivery_date = milestone.startDate + duration_days - 1
 *
 * Effect of capacity, exactly as required:
 *   capacity goes DOWN  ->  duration goes UP    ->  delivery is LATER
 *   capacity goes UP    ->  duration goes DOWN  ->  delivery is EARLIER
 *
 * Worked examples for effort = 20 developer-days:
 *   one developer at 100 %   -> capacity 1.0 -> 20 days
 *   one developer at  50 %   -> capacity 0.5 -> 40 days
 *   two developers at 100 %  -> capacity 2.0 -> 10 days
 *
 * When nobody is allocated the capacity is 0, so no date can be estimated
 * and the methods return null (never a division by zero).
 */
@Service
public class DeliveryEstimateService {

    /**
     * Number of calendar days the milestone needs at this total allocation.
     * Returns null when totalAllocationPercentage is 0 or less.
     */
    public Integer estimateDurationDays(int estimatedEffortDays, int totalAllocationPercentage) {
        if (totalAllocationPercentage <= 0 || estimatedEffortDays <= 0) {
            return null;
        }
        double capacity = totalAllocationPercentage / 100.0;
        double days = estimatedEffortDays / capacity;
        return (int) Math.ceil(days);
    }

    /** Estimated finish date, or null when nobody is allocated. */
    public LocalDate estimateDeliveryDate(LocalDate startDate, int estimatedEffortDays, int totalAllocationPercentage) {
        Integer duration = estimateDurationDays(estimatedEffortDays, totalAllocationPercentage);
        if (duration == null || startDate == null) {
            return null;
        }
        return startDate.plusDays(duration - 1L);
    }

    /** Convenience overload used by the services. */
    public LocalDate estimateDeliveryDate(Milestone milestone, int totalAllocationPercentage) {
        if (milestone == null) {
            return null;
        }
        return estimateDeliveryDate(milestone.getStartDate(),
                                    milestone.getEstimatedEffortDays() == null ? 0 : milestone.getEstimatedEffortDays(),
                                    totalAllocationPercentage);
    }
}
