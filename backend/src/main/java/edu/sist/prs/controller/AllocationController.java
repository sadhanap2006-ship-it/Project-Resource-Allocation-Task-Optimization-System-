package edu.sist.prs.controller;

import edu.sist.prs.dto.request.AllocationRequest;
import edu.sist.prs.dto.response.AllocationResponse;
import edu.sist.prs.dto.response.ApiResponse;
import edu.sist.prs.service.AllocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/allocations")
@PreAuthorize("hasRole('PM')")
public class AllocationController {

    private final AllocationService allocationService;

    public AllocationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AllocationResponse>>> getAllocations(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) Long milestoneId) {
        List<AllocationResponse> list = allocationService.getAllocations(projectId, resourceId, milestoneId);
        return ResponseEntity.ok(ApiResponse.success(list, "Allocations retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AllocationResponse>> getAllocationById(@PathVariable Long id) {
        AllocationResponse allocation = allocationService.getAllocationById(id);
        return ResponseEntity.ok(ApiResponse.success(allocation, "Allocation retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AllocationResponse>> createAllocation(@Valid @RequestBody AllocationRequest request) {
        AllocationResponse allocation = allocationService.createAllocation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(allocation, "Allocation created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AllocationResponse>> updateAllocation(
            @PathVariable Long id,
            @Valid @RequestBody AllocationRequest request) {
        AllocationResponse allocation = allocationService.updateAllocation(id, request);
        return ResponseEntity.ok(ApiResponse.success(allocation, "Allocation updated successfully"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteAllocation(@PathVariable Long id) {
        allocationService.deleteAllocation(id);
        return ResponseEntity.noContent().build();
    }
}
