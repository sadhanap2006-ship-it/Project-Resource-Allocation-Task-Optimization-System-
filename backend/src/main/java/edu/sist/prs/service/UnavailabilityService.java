package edu.sist.prs.service;

import edu.sist.prs.dto.request.UnavailabilityRequest;
import edu.sist.prs.entity.*;
import edu.sist.prs.exception.ApiException;
import edu.sist.prs.repository.ResourceRepository;
import edu.sist.prs.repository.UnavailabilityRepository;
import edu.sist.prs.repository.UserRepository;
import edu.sist.prs.util.DateRanges;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class UnavailabilityService {

    private final UnavailabilityRepository unavailabilityRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ConflictDetectionService conflictDetectionService;

    public UnavailabilityService(
            UnavailabilityRepository unavailabilityRepository,
            ResourceRepository resourceRepository,
            UserRepository userRepository,
            ConflictDetectionService conflictDetectionService
    ) {
        this.unavailabilityRepository = unavailabilityRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.conflictDetectionService = conflictDetectionService;
    }

    @Transactional
    public UnavailabilityEntity createUnavailability(UnavailabilityRequest req, String userEmail) {
        UserEntity currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));

        if (req.endDate().isBefore(req.startDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "End date cannot be before start date");
        }

        ResourceEntity resource = resourceRepository.findById(req.resourceId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found"));

        // Type validations
        if (req.type() == UnavailabilityType.MAINTENANCE) {
            if (resource.getType() != ResourceType.HARDWARE) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Maintenance can only be created for HARDWARE resources");
            }
            if (currentUser.getRole() != UserRole.PM && currentUser.getRole() != UserRole.ADMIN) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only PM or ADMIN can create hardware maintenance");
            }
        } else if (req.type() == UnavailabilityType.LEAVE) {
            if (resource.getType() != ResourceType.DEVELOPER) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Leave can only be requested for DEVELOPER resources");
            }
            if (currentUser.getRole() == UserRole.DEVELOPER) {
                if (currentUser.getResourceId() == null || !currentUser.getResourceId().equals(resource.getId())) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Developers can only request leave for themselves");
                }
            }
        }

        // Duplicate check (Section 9.5: overlapping APPROVED or REQUESTED leave for the same resource -> 409 DUPLICATE_LEAVE)
        List<UnavailabilityEntity> existing = unavailabilityRepository.findByResourceId(resource.getId());
        for (UnavailabilityEntity u : existing) {
            if (u.getStatus() == UnavailabilityStatus.APPROVED || u.getStatus() == UnavailabilityStatus.REQUESTED) {
                if (DateRanges.overlaps(req.startDate(), req.endDate(), u.getStartDate(), u.getEndDate())) {
                    throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_LEAVE", "An overlapping leave or maintenance already exists");
                }
            }
        }

        UnavailabilityStatus initialStatus;
        Long decidedBy = null;

        if (req.type() == UnavailabilityType.MAINTENANCE) {
            initialStatus = UnavailabilityStatus.APPROVED;
            decidedBy = currentUser.getId();
        } else {
            // LEAVE
            if (currentUser.getRole() == UserRole.PM && Boolean.TRUE.equals(req.approve())) {
                initialStatus = UnavailabilityStatus.APPROVED;
                decidedBy = currentUser.getId();
            } else {
                initialStatus = UnavailabilityStatus.REQUESTED;
            }
        }

        UnavailabilityEntity entity = new UnavailabilityEntity(
                resource.getId(),
                req.startDate(),
                req.endDate(),
                req.type(),
                initialStatus,
                req.reason(),
                currentUser.getId(),
                decidedBy
        );

        UnavailabilityEntity saved = unavailabilityRepository.save(entity);

        if (saved.getStatus() == UnavailabilityStatus.APPROVED) {
            conflictDetectionService.reevaluateScope(Set.of(resource.getId()), null);
        }

        return saved;
    }

    @Transactional
    public UnavailabilityEntity approveUnavailability(Long id, String userEmail) {
        UserEntity currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));

        if (currentUser.getRole() != UserRole.PM) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only PM can approve unavailability");
        }

        UnavailabilityEntity unavail = unavailabilityRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Unavailability record not found"));

        if (unavail.getStatus() != UnavailabilityStatus.REQUESTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Can only approve REQUESTED unavailability");
        }

        unavail.setStatus(UnavailabilityStatus.APPROVED);
        unavail.setDecidedBy(currentUser.getId());
        UnavailabilityEntity saved = unavailabilityRepository.save(unavail);

        conflictDetectionService.reevaluateScope(Set.of(saved.getResourceId()), null);
        return saved;
    }

    @Transactional
    public UnavailabilityEntity rejectUnavailability(Long id, String userEmail) {
        UserEntity currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));

        if (currentUser.getRole() != UserRole.PM) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only PM can reject unavailability");
        }

        UnavailabilityEntity unavail = unavailabilityRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Unavailability record not found"));

        if (unavail.getStatus() != UnavailabilityStatus.REQUESTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Can only reject REQUESTED unavailability");
        }

        unavail.setStatus(UnavailabilityStatus.REJECTED);
        unavail.setDecidedBy(currentUser.getId());
        UnavailabilityEntity saved = unavailabilityRepository.save(unavail);

        conflictDetectionService.reevaluateScope(Set.of(saved.getResourceId()), null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<UnavailabilityEntity> getUnavailability(Long resourceId, LocalDate from, LocalDate to, UnavailabilityStatus status) {
        List<UnavailabilityEntity> all = unavailabilityRepository.findAll();
        return all.stream().filter(u -> {
            if (resourceId != null && !u.getResourceId().equals(resourceId)) return false;
            if (status != null && u.getStatus() != status) return false;
            if (from != null && u.getEndDate().isBefore(from)) return false;
            if (to != null && u.getStartDate().isAfter(to)) return false;
            return true;
        }).toList();
    }

    @Transactional
    public void deleteUnavailability(Long id, String userEmail) {
        UserEntity currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));

        UnavailabilityEntity unavail = unavailabilityRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Unavailability record not found"));

        if (unavail.getStatus() != UnavailabilityStatus.REQUESTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Only REQUESTED unavailability can be deleted");
        }

        if (currentUser.getRole() != UserRole.PM && !unavail.getRequestedBy().equals(currentUser.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You can only delete your own requested leave");
        }

        unavailabilityRepository.delete(unavail);
    }
}
