package com.sist.rms.service;

import com.sist.rms.dto.AllocationRequest;
import com.sist.rms.dto.AllocationResponse;
import com.sist.rms.dto.ConflictResponse;
import com.sist.rms.entity.*;
import com.sist.rms.exception.ResourceNotFoundException;
import com.sist.rms.exception.ValidationException;
import com.sist.rms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * RESOURCE ALLOCATION
 *
 * Saving an allocation runs three checks:
 *
 *   1. hard validation - unknown ids, bad dates, percentage outside 1-100,
 *      dates outside the project or milestone. These REJECT the request.
 *
 *   2. skill check - if the developer is missing a skill the milestone
 *      requires, the allocation is still saved (the manager may want a
 *      trainee), but a warning is returned AND a SKILL_GAP notification is
 *      stored, so the assignment is never silent.
 *
 *   3. conflict check - after saving, conflict detection runs again so any
 *      new over-allocation or leave conflict appears as a warning and as a
 *      notification straight away.
 */
@Service
public class AllocationService {

    private final ResourceAllocationRepository allocationRepository;
    private final DeveloperRepository developerRepository;
    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final SkillGapService skillGapService;
    private final ConflictService conflictService;
    private final NotificationService notificationService;

    public AllocationService(ResourceAllocationRepository allocationRepository,
                             DeveloperRepository developerRepository,
                             ProjectRepository projectRepository,
                             MilestoneRepository milestoneRepository,
                             SkillGapService skillGapService,
                             ConflictService conflictService,
                             NotificationService notificationService) {
        this.allocationRepository = allocationRepository;
        this.developerRepository = developerRepository;
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.skillGapService = skillGapService;
        this.conflictService = conflictService;
        this.notificationService = notificationService;
    }

    public List<AllocationResponse> findAll() {
        List<AllocationResponse> responses = new ArrayList<>();
        for (ResourceAllocation allocation : allocationRepository.findAllByOrderByStartDateAsc()) {
            responses.add(toResponse(allocation));
        }
        return responses;
    }

    public List<AllocationResponse> findByDeveloper(Long developerId) {
        if (!developerRepository.existsById(developerId)) {
            throw new ResourceNotFoundException("Developer", developerId);
        }
        List<AllocationResponse> responses = new ArrayList<>();
        for (ResourceAllocation allocation : allocationRepository.findByDeveloperId(developerId)) {
            responses.add(toResponse(allocation));
        }
        return responses;
    }

    public List<AllocationResponse> findByProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", projectId);
        }
        List<AllocationResponse> responses = new ArrayList<>();
        for (ResourceAllocation allocation : allocationRepository.findByProjectId(projectId)) {
            responses.add(toResponse(allocation));
        }
        return responses;
    }

    public AllocationResponse findById(Long id) {
        return toResponse(getAllocationOrThrow(id));
    }

    @Transactional
    public AllocationResponse create(AllocationRequest request) {
        ResourceAllocation allocation = new ResourceAllocation();
        applyRequest(allocation, request);
        allocation = allocationRepository.save(allocation);
        return toResponseWithWarnings(allocation);
    }

    @Transactional
    public AllocationResponse update(Long id, AllocationRequest request) {
        ResourceAllocation allocation = getAllocationOrThrow(id);
        applyRequest(allocation, request);
        allocation = allocationRepository.save(allocation);
        return toResponseWithWarnings(allocation);
    }

    @Transactional
    public void delete(Long id) {
        allocationRepository.delete(getAllocationOrThrow(id));
    }

    // -----------------------------------------------------------------

    public ResourceAllocation getAllocationOrThrow(Long id) {
        return allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", id));
    }

    /** Step 1: reject anything that is plainly wrong. */
    private void applyRequest(ResourceAllocation allocation, AllocationRequest request) {
        Developer developer = developerRepository.findById(request.getDeveloperId())
                .orElseThrow(() -> new ResourceNotFoundException("Developer", request.getDeveloperId()));
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", request.getProjectId()));

        Milestone milestone = null;
        if (request.getMilestoneId() != null) {
            milestone = milestoneRepository.findById(request.getMilestoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Milestone", request.getMilestoneId()));
            if (!milestone.getProject().getId().equals(project.getId())) {
                throw new ValidationException("The milestone does not belong to the selected project");
            }
        }

        List<String> errors = new ArrayList<>();
        if (request.getAllocationPercentage() == null
                || request.getAllocationPercentage() < 1 || request.getAllocationPercentage() > 100) {
            errors.add("Allocation percentage must be between 1 and 100");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            errors.add("End date cannot be before start date");
        }
        if (request.getStartDate().isBefore(project.getStartDate())
                || request.getEndDate().isAfter(project.getEndDate())) {
            errors.add("Allocation dates must be inside the project dates ("
                    + project.getStartDate() + " to " + project.getEndDate() + ")");
        }
        if (milestone != null && (request.getStartDate().isBefore(milestone.getStartDate())
                || request.getEndDate().isAfter(milestone.getEndDate()))) {
            errors.add("Allocation dates must be inside the milestone dates ("
                    + milestone.getStartDate() + " to " + milestone.getEndDate() + ")");
        }
        if (developer.getStatus() == DeveloperStatus.INACTIVE) {
            errors.add(developer.getName() + " is INACTIVE and cannot be allocated");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        allocation.setDeveloper(developer);
        allocation.setProject(project);
        allocation.setMilestone(milestone);
        allocation.setStartDate(request.getStartDate());
        allocation.setEndDate(request.getEndDate());
        allocation.setAllocationPercentage(request.getAllocationPercentage());
    }

    /** Steps 2 and 3: collect warnings and raise notifications. */
    private AllocationResponse toResponseWithWarnings(ResourceAllocation allocation) {
        AllocationResponse response = toResponse(allocation);
        List<String> warnings = new ArrayList<>();

        // step 2 - missing skills for this milestone
        if (allocation.getMilestone() != null) {
            List<String> missing = skillGapService.missingSkillsForDeveloper(
                    allocation.getDeveloper().getId(), allocation.getMilestone().getId());
            if (!missing.isEmpty()) {
                String message = "SKILL GAP: " + allocation.getDeveloper().getName()
                        + " does not have the required skill(s) " + String.join(", ", missing)
                        + " for milestone " + allocation.getMilestone().getName() + ".";
                warnings.add(message);
                notificationService.createIfNew(NotificationType.SKILL_GAP,
                        NotificationSeverity.WARNING, message,
                        "MILESTONE", allocation.getMilestone().getId(),
                        allocation.getDeveloper().getId());
            }
        }

        // step 3 - conflicts this allocation takes part in
        for (ConflictResponse conflict :
                conflictService.findConflictsForDeveloper(allocation.getDeveloper().getId())) {
            if (!conflict.getStartDate().isAfter(allocation.getEndDate())
                    && !conflict.getEndDate().isBefore(allocation.getStartDate())) {
                warnings.add(conflict.getMessage());
                NotificationType type = "OVER_ALLOCATION".equals(conflict.getType())
                        ? NotificationType.OVER_ALLOCATION : NotificationType.LEAVE_CONFLICT;
                NotificationSeverity severity = "OVER_ALLOCATION".equals(conflict.getType())
                        ? NotificationSeverity.CRITICAL : NotificationSeverity.WARNING;
                notificationService.createIfNew(type, severity, conflict.getMessage(),
                        "DEVELOPER", conflict.getDeveloperId(), conflict.getDeveloperId());
            }
        }

        response.setWarnings(warnings);
        return response;
    }

    public AllocationResponse toResponse(ResourceAllocation allocation) {
        AllocationResponse response = new AllocationResponse();
        response.setId(allocation.getId());
        response.setDeveloperId(allocation.getDeveloper().getId());
        response.setDeveloperName(allocation.getDeveloper().getName());
        response.setProjectId(allocation.getProject().getId());
        response.setProjectName(allocation.getProject().getName());
        if (allocation.getMilestone() != null) {
            response.setMilestoneId(allocation.getMilestone().getId());
            response.setMilestoneName(allocation.getMilestone().getName());
        }
        response.setStartDate(allocation.getStartDate());
        response.setEndDate(allocation.getEndDate());
        response.setAllocationPercentage(allocation.getAllocationPercentage());
        return response;
    }
}
