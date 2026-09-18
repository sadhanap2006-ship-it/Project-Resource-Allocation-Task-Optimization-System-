package com.sist.rms.service;

import com.sist.rms.dto.ProjectRequest;
import com.sist.rms.dto.ProjectResponse;
import com.sist.rms.entity.Project;
import com.sist.rms.entity.ProjectStatus;
import com.sist.rms.exception.ResourceNotFoundException;
import com.sist.rms.exception.ValidationException;
import com.sist.rms.repository.MilestoneRepository;
import com.sist.rms.repository.ProjectRepository;
import com.sist.rms.repository.ResourceAllocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Create, read, update and delete projects. */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final ResourceAllocationRepository allocationRepository;
    private final BudgetService budgetService;

    public ProjectService(ProjectRepository projectRepository,
                          MilestoneRepository milestoneRepository,
                          ResourceAllocationRepository allocationRepository,
                          BudgetService budgetService) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.allocationRepository = allocationRepository;
        this.budgetService = budgetService;
    }

    public List<ProjectResponse> findAll() {
        List<ProjectResponse> responses = new ArrayList<>();
        for (Project project : projectRepository.findAllByOrderByStartDateAsc()) {
            responses.add(toResponse(project));
        }
        return responses;
    }

    public ProjectResponse findById(Long id) {
        return toResponse(getProjectOrThrow(id));
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        validate(request);
        Project project = new Project();
        applyRequest(project, request);
        project = projectRepository.save(project);

        // every project automatically gets its budget row
        budgetService.createOrUpdateBudgetFor(project);
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        validate(request);
        Project project = getProjectOrThrow(id);
        applyRequest(project, request);
        project = projectRepository.save(project);
        budgetService.createOrUpdateBudgetFor(project);
        return toResponse(project);
    }

    @Transactional
    public void delete(Long id) {
        projectRepository.delete(getProjectOrThrow(id));
    }

    public Project getProjectOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    public ProjectResponse toResponse(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setStartDate(project.getStartDate());
        response.setEndDate(project.getEndDate());
        response.setBudget(project.getBudget());
        response.setStatus(project.getStatus().name());
        response.setMilestoneCount(milestoneRepository.findByProjectIdOrderByStartDateAsc(project.getId()).size());
        response.setAllocationCount(allocationRepository.findByProjectId(project.getId()).size());
        return response;
    }

    private void applyRequest(Project project, ProjectRequest request) {
        project.setName(request.getName().trim());
        project.setDescription(request.getDescription());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setBudget(request.getBudget());
        project.setStatus(parseStatus(request.getStatus()));
    }

    private void validate(ProjectRequest request) {
        List<String> errors = new ArrayList<>();
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            errors.add("Project name is required");
        }
        if (request.getBudget() != null && request.getBudget().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Budget cannot be negative");
        }
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            errors.add("End date cannot be before start date");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private ProjectStatus parseStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ProjectStatus.PLANNED;
        }
        try {
            return ProjectStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(
                    "Status must be one of PLANNED, IN_PROGRESS, COMPLETED or AT_RISK");
        }
    }
}
