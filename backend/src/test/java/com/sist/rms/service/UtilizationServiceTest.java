package com.sist.rms.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The rule that colours the resource utilization table. */
class UtilizationServiceTest {

    private final UtilizationService service = new UtilizationService(null, null, null, null);

    @Test
    @DisplayName("110% against 100% availability is a Conflict")
    void overAllocatedIsAConflict() {
        assertEquals("Conflict", service.decideStatus(110, 100));
    }

    @Test
    @DisplayName("100% against 100% availability is Fully Allocated")
    void exactlyFull() {
        assertEquals("Fully Allocated", service.decideStatus(100, 100));
    }

    @Test
    @DisplayName("80% against 100% availability is High Load")
    void highLoad() {
        assertEquals("High Load", service.decideStatus(80, 100));
    }

    @Test
    @DisplayName("60% against 100% availability is Available")
    void available() {
        assertEquals("Available", service.decideStatus(60, 100));
    }

    @Test
    @DisplayName("A developer with no allocations is Available")
    void noWorkIsAvailable() {
        assertEquals("Available", service.decideStatus(0, 100));
    }
}
