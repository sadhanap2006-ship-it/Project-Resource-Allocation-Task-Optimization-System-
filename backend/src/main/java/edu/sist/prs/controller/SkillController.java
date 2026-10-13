package edu.sist.prs.controller;

import edu.sist.prs.dto.request.SkillRequest;
import edu.sist.prs.dto.response.ApiResponse;
import edu.sist.prs.dto.response.SkillResponse;
import edu.sist.prs.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillResponse>>> getAllSkills() {
        List<SkillResponse> skills = skillService.getAllSkills();
        return ResponseEntity.ok(ApiResponse.success(skills, "Skills retrieved"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PM')")
    public ResponseEntity<ApiResponse<SkillResponse>> createSkill(@Valid @RequestBody SkillRequest request) {
        SkillResponse skill = skillService.createSkill(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(skill, "Skill created successfully"));
    }
}
