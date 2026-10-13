package edu.sist.prs.service;

import edu.sist.prs.dto.request.ProjectRequest;
import edu.sist.prs.dto.response.ProjectResponse;
import edu.sist.prs.entity.ProjectEntity;
import edu.sist.prs.entity.ProjectStatus;
import edu.sist.prs.entity.UserEntity;
import edu.sist.prs.exception.BadRequestException;
import edu.sist.prs.exception.ConflictException;
import edu.sist.prs.exception.ResourceNotFoundException;
import edu.sist.prs.repository.ProjectRepository;
import edu.sist.prs.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects(ProjectStatus status, String query) {
        List<ProjectEntity> list = status != null ? projectRepository.findByStatus(status) : projectRepository.findAll();
        if (query != null && !query.trim().isEmpty()) {
            String qLower = query.toLowerCase();
            list = list.stream()
                .filter(p -> p.getName().toLowerCase().contains(qLower) || p.getCode().toLowerCase().contains(qLower))
                .collect(Collectors.toList());
        }
        return list.stream().map(this::toProjectResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        ProjectEntity project = projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        return toProjectResponse(project);
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request, Authentication authentication) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("INVALID_DATE_RANGE", "Project end date must be on or after start date");
        }

        if (projectRepository.findByCode(request.code()).isPresent()) {
            throw new ConflictException("DUPLICATE_CODE", "Project with code '" + request.code() + "' already exists");
        }

        UserEntity pmUser = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("PM User not found"));

        ProjectEntity project = new ProjectEntity(
            request.code(),
            request.name(),
            request.description(),
            pmUser.getId(),
            request.startDate(),
            request.endDate(),
            request.status() != null ? request.status() : ProjectStatus.PLANNED,
            request.totalBudget()
        );

        ProjectEntity saved = projectRepository.save(project);
        return toProjectResponse(saved);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        ProjectEntity project = projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", id));

        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("INVALID_DATE_RANGE", "Project end date must be on or after start date");
        }

        if (!project.getCode().equalsIgnoreCase(request.code()) && projectRepository.findByCode(request.code()).isPresent()) {
            throw new ConflictException("DUPLICATE_CODE", "Project with code '" + request.code() + "' already exists");
        }

        project.setCode(request.code());
        project.setName(request.name());
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        if (request.status() != null) {
            project.setStatus(request.status());
        }
        project.setTotalBudget(request.totalBudget());

        return toProjectResponse(project);
    }

    @Transactional
    public void archiveProject(Long id) {
        ProjectEntity project = projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        // Section 20 Clarification 3: Projects are never hard-deleted: DELETE sets status ARCHIVED.
        project.setStatus(ProjectStatus.ARCHIVED);
    }

    public ProjectResponse toProjectResponse(ProjectEntity p) {
        String pmName = null;
        if (p.getPmUserId() != null) {
            var pm = userRepository.findById(p.getPmUserId()).orElse(null);
            if (pm != null) {
                pmName = pm.getName();
            }
        }
        return new ProjectResponse(
            p.getId(),
            p.getCode(),
            p.getName(),
            p.getDescription(),
            p.getPmUserId(),
            pmName,
            p.getStartDate(),
            p.getEndDate(),
            p.getStatus(),
            p.getTotalBudget(),
            "GREEN", // Baseline default, updated by health evaluator in Phase 4/8
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }
}
