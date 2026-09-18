package com.sist.rms.controller;

import com.sist.rms.dto.AllocationRequest;
import com.sist.rms.dto.AllocationResponse;
import com.sist.rms.service.AllocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST API for resource allocations. */
@RestController
@RequestMapping("/api/allocations")
public class AllocationController {

    private final AllocationService allocationService;

    public AllocationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    /** Optional filters: ?developerId=1 or ?projectId=2 */
    @GetMapping
    public List<AllocationResponse> getAll(@RequestParam(required = false) Long developerId,
                                           @RequestParam(required = false) Long projectId) {
        if (developerId != null) {
            return allocationService.findByDeveloper(developerId);
        }
        if (projectId != null) {
            return allocationService.findByProject(projectId);
        }
        return allocationService.findAll();
    }

    @GetMapping("/{id}")
    public AllocationResponse getOne(@PathVariable Long id) {
        return allocationService.findById(id);
    }

    /** The response carries a "warnings" list when the save raised a skill gap or a conflict. */
    @PostMapping
    public ResponseEntity<AllocationResponse> create(@Valid @RequestBody AllocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(allocationService.create(request));
    }

    @PutMapping("/{id}")
    public AllocationResponse update(@PathVariable Long id, @Valid @RequestBody AllocationRequest request) {
        return allocationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        allocationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
