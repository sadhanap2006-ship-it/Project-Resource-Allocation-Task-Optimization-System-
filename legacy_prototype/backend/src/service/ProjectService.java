package service;

import dao.MilestoneDAO;
import dao.ProjectDAO;
import dao.SkillDAO;
import dto.ProjectRequestDTO;
import dto.ProjectResponseDTO;
import model.Milestone;
import model.Project;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProjectService {

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final MilestoneDAO milestoneDAO = new MilestoneDAO();
    private final SkillDAO skillDAO = new SkillDAO();

    public List<ProjectResponseDTO> getAllProjects() throws SQLException {
        List<Project> list = projectDAO.findAll();
        List<ProjectResponseDTO> dtos = new ArrayList<>();
        for (Project p : list) {
            dtos.add(mapToDTO(p));
        }
        return dtos;
    }

    public ProjectResponseDTO getProjectById(int id) throws SQLException {
        Project p = projectDAO.findById(id);
        if (p == null) return null;
        return mapToDTO(p);
    }

    public ProjectResponseDTO createProject(ProjectRequestDTO req) throws SQLException {
        Project p = new Project();
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setTotalBudget(req.getTotalBudget());
        p.setCurrentSpend(req.getCurrentSpend());
        p.setStatus(req.getStatus() != null ? req.getStatus() : "PLANNED");

        int newId = projectDAO.insert(p);
        p.setId(newId);

        if (req.getRequiredSkillIds() != null && !req.getRequiredSkillIds().isEmpty()) {
            skillDAO.setProjectSkills(newId, req.getRequiredSkillIds());
        }

        return mapToDTO(p);
    }

    public ProjectResponseDTO updateProject(int id, ProjectRequestDTO req) throws SQLException {
        Project existing = projectDAO.findById(id);
        if (existing == null) return null;

        if (req.getName() != null) existing.setName(req.getName());
        if (req.getDescription() != null) existing.setDescription(req.getDescription());
        if (req.getStartDate() != null) existing.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) existing.setEndDate(req.getEndDate());
        if (req.getTotalBudget() >= 0) existing.setTotalBudget(req.getTotalBudget());
        if (req.getCurrentSpend() >= 0) existing.setCurrentSpend(req.getCurrentSpend());
        if (req.getStatus() != null) existing.setStatus(req.getStatus());

        projectDAO.update(existing);

        if (req.getRequiredSkillIds() != null) {
            skillDAO.setProjectSkills(id, req.getRequiredSkillIds());
        }

        return mapToDTO(existing);
    }

    public boolean deleteProject(int id) throws SQLException {
        return projectDAO.delete(id);
    }

    private ProjectResponseDTO mapToDTO(Project p) throws SQLException {
        ProjectResponseDTO dto = new ProjectResponseDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setStartDate(p.getStartDate());
        dto.setEndDate(p.getEndDate());
        dto.setTotalBudget(p.getTotalBudget());
        dto.setCurrentSpend(p.getCurrentSpend());
        dto.setRemainingBudget(Math.max(0, p.getTotalBudget() - p.getCurrentSpend()));
        dto.setStatus(p.getStatus());
        dto.setMilestoneCount(p.getMilestoneCount());

        List<String> skills = skillDAO.findSkillsByProjectId(p.getId());
        dto.setRequiredSkills(skills);

        List<Milestone> milestones = milestoneDAO.findByProjectId(p.getId());
        if (!milestones.isEmpty()) {
            double totalProg = 0;
            for (Milestone m : milestones) totalProg += m.getProgressPercentage();
            dto.setProgressPercentage(Math.round((totalProg / milestones.size()) * 10.0) / 10.0);
        } else {
            dto.setProgressPercentage(0.0);
        }

        return dto;
    }
}
