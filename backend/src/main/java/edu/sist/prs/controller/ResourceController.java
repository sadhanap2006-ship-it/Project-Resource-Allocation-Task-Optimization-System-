package edu.sist.prs.controller;

import edu.sist.prs.dto.request.CreateResourceRequest;
import edu.sist.prs.dto.request.ResourceSkillInput;
import edu.sist.prs.dto.request.UpdateResourceRequest;
import edu.sist.prs.dto.response.ApiResponse;
import edu.sist.prs.dto.response.ResourceResponse;
import edu.sist.prs.dto.response.ResourceSkillDto;
import edu.sist.prs.entity.ResourceStatus;
import edu.sist.prs.entity.ResourceType;
import edu.sist.prs.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResourceResponse>>> getAllResources(
            @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) ResourceStatus status,
            @RequestParam(required = false) String q) {
        List<ResourceResponse> resources = resourceService.getAllResources(type, status, q);
        return ResponseEntity.ok(ApiResponse.success(resources, "Resources retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResourceById(@PathVariable Long id) {
        ResourceResponse resource = resourceService.getResourceById(id);
        return ResponseEntity.ok(ApiResponse.success(resource, "Resource retrieved"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PM')")
    public ResponseEntity<ApiResponse<ResourceResponse>> createResource(@Valid @RequestBody CreateResourceRequest request) {
        ResourceResponse resource = resourceService.createResource(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resource, "Resource created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PM')")
    public ResponseEntity<ApiResponse<ResourceResponse>> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody UpdateResourceRequest request) {
        ResourceResponse resource = resourceService.updateResource(id, request);
        return ResponseEntity.ok(ApiResponse.success(resource, "Resource updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PM')")
    public ResponseEntity<ApiResponse<Void>> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Resource set to INACTIVE"));
    }

    @PutMapping("/{id}/skills")
    @PreAuthorize("hasAnyRole('ADMIN', 'PM')")
    public ResponseEntity<ApiResponse<List<ResourceSkillDto>>> updateResourceSkills(
            @PathVariable Long id,
            @Valid @RequestBody List<ResourceSkillInput> skills) {
        List<ResourceSkillDto> updated = resourceService.updateResourceSkills(id, skills);
        return ResponseEntity.ok(ApiResponse.success(updated, "Resource skills updated successfully"));
    }
}
