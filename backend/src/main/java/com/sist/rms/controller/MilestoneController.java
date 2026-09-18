package com.sist.rms.controller;

import com.sist.rms.dto.MilestoneRequest;
import com.sist.rms.dto.MilestoneResponse;
import com.sist.rms.service.MilestoneService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** REST API for milestones. */
@RestController
@RequestMapping("/api/milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;

    public MilestoneController(MilestoneService milestoneService) {
        this.milestoneService = milestoneService;
    }

    /** GET /api/milestones           -> every milestone
     *  GET /api/milestones?projectId=1 -> only that project's milestones */
    @GetMapping
    public List<MilestoneResponse> getAll(@RequestParam(required = false) Long projectId) {
        return projectId == null ? milestoneService.findAll() : milestoneService.findByProject(projectId);
    }

    @GetMapping("/{id}")
    public MilestoneResponse getOne(@PathVariable Long id) {
        return milestoneService.findById(id);
    }

    @PostMapping
    public ResponseEntity<MilestoneResponse> create(@Valid @RequestBody MilestoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(milestoneService.create(request));
    }

    @PutMapping("/{id}")
    public MilestoneResponse update(@PathVariable Long id, @Valid @RequestBody MilestoneRequest request) {
        return milestoneService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        milestoneService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Body: { "skillName": "AWS" } */
    @PostMapping("/{id}/skills")
    public MilestoneResponse addRequiredSkill(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return milestoneService.addRequiredSkill(id, body.get("skillName"));
    }

    @DeleteMapping("/{id}/skills/{skillId}")
    public MilestoneResponse removeRequiredSkill(@PathVariable Long id, @PathVariable Long skillId) {
        return milestoneService.removeRequiredSkill(id, skillId);
    }
}
