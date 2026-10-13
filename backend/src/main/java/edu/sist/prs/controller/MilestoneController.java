package edu.sist.prs.controller;

import edu.sist.prs.dto.request.MilestoneProgressRequest;
import edu.sist.prs.dto.request.MilestoneRequest;
import edu.sist.prs.dto.request.MilestoneSkillRequirementInput;
import edu.sist.prs.dto.response.ApiResponse;
import edu.sist.prs.dto.response.MilestoneResponse;
import edu.sist.prs.dto.response.MilestoneSkillRequirementDto;
import edu.sist.prs.service.MilestoneService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MilestoneController {

    private final MilestoneService milestoneService;

    public MilestoneController(MilestoneService milestoneService) {
        this.milestoneService = milestoneService;
    }

    @GetMapping("/milestones")
    @PreAuthorize("hasAnyRole('PM', 'ADMIN', 'DEVELOPER')")
    public ResponseEntity<ApiResponse<List<MilestoneResponse>>> getAllMilestones() {
        List<MilestoneResponse> milestones = milestoneService.getAllMilestones();
        return ResponseEntity.ok(ApiResponse.success(milestones, "All milestones retrieved"));
    }

    @GetMapping("/projects/{projectId}/milestones")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<List<MilestoneResponse>>> getMilestonesByProject(@PathVariable Long projectId) {
        List<MilestoneResponse> milestones = milestoneService.getMilestonesByProjectId(projectId);
        return ResponseEntity.ok(ApiResponse.success(milestones, "Milestones retrieved"));
    }

    @PostMapping("/projects/{projectId}/milestones")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<MilestoneResponse>> createMilestone(
            @PathVariable Long projectId,
            @Valid @RequestBody MilestoneRequest request) {
        MilestoneResponse milestone = milestoneService.createMilestone(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(milestone, "Milestone created successfully"));
    }

    @PutMapping("/milestones/{id}")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<MilestoneResponse>> updateMilestone(
            @PathVariable Long id,
            @Valid @RequestBody MilestoneRequest request) {
        MilestoneResponse milestone = milestoneService.updateMilestone(id, request);
        return ResponseEntity.ok(ApiResponse.success(milestone, "Milestone updated successfully"));
    }

    @DeleteMapping("/milestones/{id}")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(@PathVariable Long id) {
        milestoneService.deleteMilestone(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Milestone deleted successfully"));
    }

    @PatchMapping("/milestones/{id}/progress")
    @PreAuthorize("hasAnyRole('PM', 'DEVELOPER')")
    public ResponseEntity<ApiResponse<MilestoneResponse>> updateProgress(
            @PathVariable Long id,
            @Valid @RequestBody MilestoneProgressRequest request,
            Authentication authentication) {
        MilestoneResponse milestone = milestoneService.updateProgress(id, request.progressPct(), authentication);
        return ResponseEntity.ok(ApiResponse.success(milestone, "Milestone progress updated successfully"));
    }

    @GetMapping("/milestones/{id}/skills")
    @PreAuthorize("hasAnyRole('PM', 'ADMIN', 'DEVELOPER')")
    public ResponseEntity<ApiResponse<List<MilestoneSkillRequirementDto>>> getSkills(@PathVariable Long id) {
        List<MilestoneSkillRequirementDto> skills = milestoneService.getMilestoneSkills(id);
        return ResponseEntity.ok(ApiResponse.success(skills, "Milestone skill requirements retrieved"));
    }

    @PutMapping("/milestones/{id}/skills")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<List<MilestoneSkillRequirementDto>>> updateSkills(
            @PathVariable Long id,
            @Valid @RequestBody List<MilestoneSkillRequirementInput> requirements) {
        List<MilestoneSkillRequirementDto> updated = milestoneService.updateSkills(id, requirements);
        return ResponseEntity.ok(ApiResponse.success(updated, "Milestone skill requirements updated successfully"));
    }

    @GetMapping("/milestones/{id}/forecast")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<edu.sist.prs.engine.model.MilestoneDeliveryForecast>> getMilestoneForecast(@PathVariable Long id) {
        edu.sist.prs.engine.model.MilestoneDeliveryForecast forecast = milestoneService.getMilestoneForecast(id);
        return ResponseEntity.ok(ApiResponse.success(forecast, "Milestone forecast retrieved"));
    }

    @GetMapping("/milestones/{id}/candidates")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<List<edu.sist.prs.engine.model.CandidateRank>>> getCandidates(
            @PathVariable Long id,
            @RequestParam(defaultValue = "60") int pct) {
        List<edu.sist.prs.engine.model.CandidateRank> candidates = milestoneService.getCandidates(id, pct);
        return ResponseEntity.ok(ApiResponse.success(candidates, "Candidates ranked"));
    }
}
