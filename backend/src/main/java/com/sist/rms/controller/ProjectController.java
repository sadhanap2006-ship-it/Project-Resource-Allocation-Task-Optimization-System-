package com.sist.rms.controller;

import com.sist.rms.dto.*;
import com.sist.rms.service.BudgetService;
import com.sist.rms.service.MilestoneService;
import com.sist.rms.service.ProjectService;
import com.sist.rms.service.TimelineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST API for projects, and for everything that hangs off a project. */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final MilestoneService milestoneService;
    private final BudgetService budgetService;
    private final TimelineService timelineService;

    public ProjectController(ProjectService projectService,
                             MilestoneService milestoneService,
                             BudgetService budgetService,
                             TimelineService timelineService) {
        this.projectService = projectService;
        this.milestoneService = milestoneService;
        this.budgetService = budgetService;
        this.timelineService = timelineService;
    }

    @GetMapping
    public List<ProjectResponse> getAll() {
        return projectService.findAll();
    }

    @GetMapping("/{id}")
    public ProjectResponse getOne(@PathVariable Long id) {
        return projectService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- things that belong to one project ----------

    @GetMapping("/{id}/milestones")
    public List<MilestoneResponse> getMilestones(@PathVariable Long id) {
        return milestoneService.findByProject(id);
    }

    @GetMapping("/{id}/timeline")
    public TimelineResponse getTimeline(@PathVariable Long id) {
        return timelineService.getTimelineForProject(id);
    }

    @GetMapping("/{id}/budget")
    public BudgetResponse getBudget(@PathVariable Long id) {
        return budgetService.getBudget(id);
    }

    @GetMapping("/{id}/budget/forecast")
    public BudgetForecastResponse getForecast(@PathVariable Long id) {
        return budgetService.getForecast(id);
    }

    @GetMapping("/{id}/budget/transactions")
    public List<BudgetTransactionResponse> getTransactions(@PathVariable Long id) {
        return budgetService.getTransactions(id);
    }

    @PostMapping("/{id}/budget/transactions")
    public ResponseEntity<BudgetTransactionResponse> addTransaction(
            @PathVariable Long id, @Valid @RequestBody BudgetTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.addTransaction(id, request));
    }
}
