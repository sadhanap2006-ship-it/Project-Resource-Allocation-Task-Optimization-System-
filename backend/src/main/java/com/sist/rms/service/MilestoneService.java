package com.sist.rms.service;

import com.sist.rms.dto.MilestoneRequest;
import com.sist.rms.dto.MilestoneResponse;
import com.sist.rms.entity.*;
import com.sist.rms.exception.ResourceNotFoundException;
import com.sist.rms.exception.ValidationException;
import com.sist.rms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Create, read, update and delete milestones and their required skills. */
@Service
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final MilestoneSkillRepository milestoneSkillRepository;
    private final ResourceAllocationRepository allocationRepository;
    private final ProjectRepository projectRepository;
    private final SkillService skillService;
    private final DeliveryEstimateService deliveryEstimateService;

    public MilestoneService(MilestoneRepository milestoneRepository,
                            MilestoneSkillRepository milestoneSkillRepository,
                            ResourceAllocationRepository allocationRepository,
                            ProjectRepository projectRepository,
                            SkillService skillService,
                            DeliveryEstimateService deliveryEstimateService) {
        this.milestoneRepository = milestoneRepository;
        this.milestoneSkillRepository = milestoneSkillRepository;
        this.allocationRepository = allocationRepository;
        this.projectRepository = projectRepository;
        this.skillService = skillService;
        this.deliveryEstimateService = deliveryEstimateService;
    }

    public List<MilestoneResponse> findAll() {
        List<MilestoneResponse> responses = new ArrayList<>();
        for (Milestone milestone : milestoneRepository.findAllByOrderByStartDateAsc()) {
            responses.add(toResponse(milestone));
        }
        return responses;
    }

    public List<MilestoneResponse> findByProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", projectId);
        }
        List<MilestoneResponse> responses = new ArrayList<>();
        for (Milestone milestone : milestoneRepository.findByProjectIdOrderByStartDateAsc(projectId)) {
            responses.add(toResponse(milestone));
        }
        return responses;
    }

    public MilestoneResponse findById(Long id) {
        return toResponse(getMilestoneOrThrow(id));
    }

    @Transactional
    public MilestoneResponse create(MilestoneRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", request.getProjectId()));
        validate(request, project);

        Milestone milestone = new Milestone();
        milestone.setProject(project);
        applyRequest(milestone, request);
        milestone = milestoneRepository.save(milestone);

        replaceRequiredSkills(milestone, request.getRequiredSkills());
        return toResponse(milestone);
    }

    @Transactional
    public MilestoneResponse update(Long id, MilestoneRequest request) {
        Milestone milestone = getMilestoneOrThrow(id);
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", request.getProjectId()));
        validate(request, project);

        milestone.setProject(project);
        applyRequest(milestone, request);
        milestone = milestoneRepository.save(milestone);

        if (request.getRequiredSkills() != null) {
            replaceRequiredSkills(milestone, request.getRequiredSkills());
        }
        return toResponse(milestone);
    }

    @Transactional
    public void delete(Long id) {
        milestoneRepository.delete(getMilestoneOrThrow(id));
    }

    @Transactional
    public MilestoneResponse addRequiredSkill(Long milestoneId, String skillName) {
        Milestone milestone = getMilestoneOrThrow(milestoneId);
        Skill skill = skillService.findOrCreateByName(skillName);
        if (milestoneSkillRepository.findByMilestoneIdAndSkillId(milestoneId, skill.getId()).isEmpty()) {
            MilestoneSkill link = new MilestoneSkill();
            link.setMilestone(milestone);
            link.setSkill(skill);
            milestoneSkillRepository.save(link);
        }
        return toResponse(milestone);
    }

    @Transactional
    public MilestoneResponse removeRequiredSkill(Long milestoneId, Long skillId) {
        Milestone milestone = getMilestoneOrThrow(milestoneId);
        MilestoneSkill link = milestoneSkillRepository
                .findByMilestoneIdAndSkillId(milestoneId, skillId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "This milestone does not require the skill with id " + skillId));
        milestoneSkillRepository.delete(link);
        return toResponse(milestone);
    }

    // -----------------------------------------------------------------

    public Milestone getMilestoneOrThrow(Long id) {
        return milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", id));
    }

    /** Sum of the allocation percentages of everybody on this milestone. */
    public int totalAllocationOnMilestone(Long milestoneId) {
        int total = 0;
        for (ResourceAllocation allocation : allocationRepository.findByMilestoneId(milestoneId)) {
            total += allocation.getAllocationPercentage();
        }
        return total;
    }

    public MilestoneResponse toResponse(Milestone milestone) {
        MilestoneResponse response = new MilestoneResponse();
        response.setId(milestone.getId());
        response.setProjectId(milestone.getProject().getId());
        response.setProjectName(milestone.getProject().getName());
        response.setName(milestone.getName());
        response.setDescription(milestone.getDescription());
        response.setStartDate(milestone.getStartDate());
        response.setEndDate(milestone.getEndDate());
        response.setProgressPercentage(milestone.getProgressPercentage());
        response.setStatus(milestone.getStatus().name());
        response.setEstimatedEffortDays(milestone.getEstimatedEffortDays());

        List<String> required = new ArrayList<>();
        for (MilestoneSkill link : milestoneSkillRepository.findByMilestoneId(milestone.getId())) {
            required.add(link.getSkill().getName());
        }
        response.setRequiredSkills(required);

        List<String> assigned = new ArrayList<>();
        int total = 0;
        for (ResourceAllocation allocation : allocationRepository.findByMilestoneId(milestone.getId())) {
            assigned.add(allocation.getDeveloper().getName()
                    + " (" + allocation.getAllocationPercentage() + "%)");
            total += allocation.getAllocationPercentage();
        }
        response.setAssignedDevelopers(assigned);
        response.setTotalAllocationPercentage(total);
        response.setEstimatedDeliveryDate(deliveryEstimateService.estimateDeliveryDate(milestone, total));
        return response;
    }

    private void applyRequest(Milestone milestone, MilestoneRequest request) {
        milestone.setName(request.getName().trim());
        milestone.setDescription(request.getDescription());
        milestone.setStartDate(request.getStartDate());
        milestone.setEndDate(request.getEndDate());
        milestone.setProgressPercentage(request.getProgressPercentage() == null ? 0 : request.getProgressPercentage());
        milestone.setStatus(parseStatus(request.getStatus()));
        milestone.setEstimatedEffortDays(request.getEstimatedEffortDays());
    }

    private void replaceRequiredSkills(Milestone milestone, List<String> skillNames) {
        milestoneSkillRepository.deleteAll(milestoneSkillRepository.findByMilestoneId(milestone.getId()));
        if (skillNames == null) {
            return;
        }
        for (String skillName : skillNames) {
            if (skillName == null || skillName.trim().isEmpty()) {
                continue;
            }
            Skill skill = skillService.findOrCreateByName(skillName);
            if (milestoneSkillRepository.findByMilestoneIdAndSkillId(milestone.getId(), skill.getId()).isEmpty()) {
                MilestoneSkill link = new MilestoneSkill();
                link.setMilestone(milestone);
                link.setSkill(skill);
                milestoneSkillRepository.save(link);
            }
        }
    }

    private void validate(MilestoneRequest request, Project project) {
        List<String> errors = new ArrayList<>();
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            errors.add("Milestone name is required");
        }
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            errors.add("End date cannot be before start date");
        }
        if (request.getEstimatedEffortDays() != null && request.getEstimatedEffortDays() < 1) {
            errors.add("Estimated effort must be at least 1 developer-day");
        }
        if (request.getStartDate() != null && request.getStartDate().isBefore(project.getStartDate())) {
            errors.add("Milestone cannot start before its project starts on " + project.getStartDate());
        }
        if (request.getEndDate() != null && request.getEndDate().isAfter(project.getEndDate())) {
            errors.add("Milestone cannot end after its project ends on " + project.getEndDate());
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private MilestoneStatus parseStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MilestoneStatus.PLANNED;
        }
        try {
            return MilestoneStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(
                    "Status must be one of PLANNED, IN_PROGRESS, COMPLETED, DELAYED or AT_RISK");
        }
    }
}
