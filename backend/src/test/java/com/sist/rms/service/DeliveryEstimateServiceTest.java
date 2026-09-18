package com.sist.rms.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The delivery-date formula:
 *     duration = ceil(estimated effort in developer-days / capacity)
 *     capacity = total allocation percentage / 100
 */
class DeliveryEstimateServiceTest {

    private final DeliveryEstimateService service = new DeliveryEstimateService();

    @Test
    @DisplayName("One developer at 100% needs the full effort in days")
    void fullTimeDeveloper() {
        assertEquals(20, service.estimateDurationDays(20, 100));
    }

    @Test
    @DisplayName("Less capacity means a LONGER duration")
    void lessCapacityTakesLonger() {
        assertEquals(40, service.estimateDurationDays(20, 50));
    }

    @Test
    @DisplayName("More capacity means a SHORTER duration")
    void moreCapacityIsFaster() {
        assertEquals(10, service.estimateDurationDays(20, 200));
    }

    @Test
    @DisplayName("A part day is rounded up, never down")
    void roundsUp() {
        // 20 / 0.6 = 33.33 -> 34 days
        assertEquals(34, service.estimateDurationDays(20, 60));
    }

    @Test
    @DisplayName("Nobody allocated -> no estimate, and never a divide-by-zero")
    void noCapacityMeansNoEstimate() {
        assertNull(service.estimateDurationDays(20, 0));
        assertNull(service.estimateDeliveryDate(LocalDate.of(2026, 9, 1), 20, 0));
    }

    @Test
    @DisplayName("The delivery date is start + duration - 1, so the start day counts")
    void deliveryDateIncludesTheStartDay() {
        LocalDate delivery = service.estimateDeliveryDate(LocalDate.of(2026, 9, 1), 20, 100);
        assertEquals(LocalDate.of(2026, 9, 20), delivery);
    }

    @Test
    @DisplayName("Halving the capacity pushes the delivery date out")
    void capacityDropDelaysDelivery() {
        LocalDate full = service.estimateDeliveryDate(LocalDate.of(2026, 9, 1), 20, 100);
        LocalDate half = service.estimateDeliveryDate(LocalDate.of(2026, 9, 1), 20, 50);
        assertTrue(half.isAfter(full));
    }
}
