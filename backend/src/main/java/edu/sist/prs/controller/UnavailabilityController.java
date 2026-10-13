package edu.sist.prs.controller;

import edu.sist.prs.dto.request.UnavailabilityRequest;
import edu.sist.prs.dto.response.ApiResponse;
import edu.sist.prs.entity.UnavailabilityEntity;
import edu.sist.prs.entity.UnavailabilityStatus;
import edu.sist.prs.service.UnavailabilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/unavailability")
public class UnavailabilityController {

    private final UnavailabilityService unavailabilityService;

    public UnavailabilityController(UnavailabilityService unavailabilityService) {
        this.unavailabilityService = unavailabilityService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UnavailabilityEntity>>> getUnavailability(
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) UnavailabilityStatus status
    ) {
        List<UnavailabilityEntity> list = unavailabilityService.getUnavailability(resourceId, from, to, status);
        return ResponseEntity.ok(ApiResponse.success(list, "Unavailability records retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UnavailabilityEntity>> createUnavailability(
            @Valid @RequestBody UnavailabilityRequest request,
            Authentication authentication
    ) {
        UnavailabilityEntity entity = unavailabilityService.createUnavailability(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(entity, "Unavailability created"));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<UnavailabilityEntity>> approveUnavailability(
            @PathVariable Long id,
            Authentication authentication
    ) {
        UnavailabilityEntity entity = unavailabilityService.approveUnavailability(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(entity, "Unavailability approved"));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<UnavailabilityEntity>> rejectUnavailability(
            @PathVariable Long id,
            Authentication authentication
    ) {
        UnavailabilityEntity entity = unavailabilityService.rejectUnavailability(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(entity, "Unavailability rejected"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUnavailability(
            @PathVariable Long id,
            Authentication authentication
    ) {
        unavailabilityService.deleteUnavailability(id, authentication.getName());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
