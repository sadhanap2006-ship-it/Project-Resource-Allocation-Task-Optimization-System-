package com.sist.rms.controller;

import com.sist.rms.dto.*;
import com.sist.rms.service.AllocationService;
import com.sist.rms.service.DashboardService;
import com.sist.rms.service.DeveloperService;
import com.sist.rms.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST API for developers (the resource profiles). */
@RestController
@RequestMapping("/api/developers")
public class DeveloperController {

    private final DeveloperService developerService;
    private final AllocationService allocationService;
    private final LeaveService leaveService;
    private final DashboardService dashboardService;

    public DeveloperController(DeveloperService developerService,
                               AllocationService allocationService,
                               LeaveService leaveService,
                               DashboardService dashboardService) {
        this.developerService = developerService;
        this.allocationService = allocationService;
        this.leaveService = leaveService;
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public List<DeveloperResponse> getAll() {
        return developerService.findAll();
    }

    @GetMapping("/{id}")
    public DeveloperResponse getOne(@PathVariable Long id) {
        return developerService.findById(id);
    }

    @PostMapping
    public ResponseEntity<DeveloperResponse> create(@Valid @RequestBody DeveloperRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(developerService.create(request));
    }

    @PutMapping("/{id}")
    public DeveloperResponse update(@PathVariable Long id, @Valid @RequestBody DeveloperRequest request) {
        return developerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        developerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- skills ----------

    @PostMapping("/{id}/skills")
    public DeveloperResponse addSkill(@PathVariable Long id, @Valid @RequestBody DeveloperSkillRequest request) {
        return developerService.addSkill(id, request);
    }

    @DeleteMapping("/{id}/skills/{skillId}")
    public DeveloperResponse removeSkill(@PathVariable Long id, @PathVariable Long skillId) {
        return developerService.removeSkill(id, skillId);
    }

    // ---------- a developer's own data ----------

    @GetMapping("/{id}/allocations")
    public List<AllocationResponse> getAllocations(@PathVariable Long id) {
        return allocationService.findByDeveloper(id);
    }

    @GetMapping("/{id}/leaves")
    public List<LeaveResponse> getLeaves(@PathVariable Long id) {
        return leaveService.findByDeveloper(id);
    }

    @GetMapping("/{id}/dashboard")
    public DeveloperDashboardResponse getDashboard(@PathVariable Long id) {
        return dashboardService.getDeveloperDashboard(id);
    }
}
