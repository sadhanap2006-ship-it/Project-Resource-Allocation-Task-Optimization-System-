package edu.sist.prs.controller;

import edu.sist.prs.dto.request.ProjectRequest;
import edu.sist.prs.dto.response.ApiResponse;
import edu.sist.prs.dto.response.ProjectResponse;
import edu.sist.prs.entity.ProjectStatus;
import edu.sist.prs.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@PreAuthorize("hasRole('PM')")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getAllProjects(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) String q) {
        List<ProjectResponse> projects = projectService.getAllProjects(status, q);
        return ResponseEntity.ok(ApiResponse.success(projects, "Projects retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(@PathVariable Long id) {
        ProjectResponse project = projectService.getProjectById(id);
        return ResponseEntity.ok(ApiResponse.success(project, "Project retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {
        ProjectResponse project = projectService.createProject(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(project, "Project created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request) {
        ProjectResponse project = projectService.updateProject(id, request);
        return ResponseEntity.ok(ApiResponse.success(project, "Project updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable Long id) {
        projectService.archiveProject(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Project archived successfully"));
    }
}
