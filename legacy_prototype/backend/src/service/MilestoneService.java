package service;

import dao.AllocationDAO;
import dao.MilestoneDAO;
import dao.SkillDAO;
import dto.AllocationResponseDTO;
import dto.MilestoneRequestDTO;
import dto.MilestoneResponseDTO;
import model.Allocation;
import model.Milestone;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MilestoneService {

    private final MilestoneDAO milestoneDAO = new MilestoneDAO();
    private final AllocationDAO allocationDAO = new AllocationDAO();
    private final SkillDAO skillDAO = new SkillDAO();

    public List<MilestoneResponseDTO> getAllMilestones() throws SQLException {
        List<Milestone> list = milestoneDAO.findAll();
        List<MilestoneResponseDTO> dtos = new ArrayList<>();
        for (Milestone m : list) {
            dtos.add(mapToDTO(m));
        }
        return dtos;
    }

    public List<MilestoneResponseDTO> getMilestonesByProject(int projectId) throws SQLException {
        List<Milestone> list = milestoneDAO.findByProjectId(projectId);
        List<MilestoneResponseDTO> dtos = new ArrayList<>();
        for (Milestone m : list) {
            dtos.add(mapToDTO(m));
        }
        return dtos;
    }

    public MilestoneResponseDTO getMilestoneById(int id) throws SQLException {
        Milestone m = milestoneDAO.findById(id);
        if (m == null) return null;
        return mapToDTO(m);
    }

    public MilestoneResponseDTO createMilestone(MilestoneRequestDTO req) throws SQLException {
        Milestone m = new Milestone();
        m.setProjectId(req.getProjectId());
        m.setName(req.getName());
        m.setDescription(req.getDescription());
        m.setStartDate(req.getStartDate());
        m.setEndDate(req.getEndDate());
        m.setEstimatedEffortHours(req.getEstimatedEffortHours() > 0 ? req.getEstimatedEffortHours() : 40.0);
        m.setProgressPercentage(req.getProgressPercentage());
        m.setStatus(req.getStatus() != null ? req.getStatus() : "NOT_STARTED");

        int newId = milestoneDAO.insert(m);
        m.setId(newId);

        if (req.getRequiredSkillIds() != null && !req.getRequiredSkillIds().isEmpty()) {
            skillDAO.setMilestoneSkills(newId, req.getRequiredSkillIds());
        }

        return mapToDTO(m);
    }

    public MilestoneResponseDTO updateMilestone(int id, MilestoneRequestDTO req) throws SQLException {
        Milestone existing = milestoneDAO.findById(id);
        if (existing == null) return null;

        if (req.getName() != null) existing.setName(req.getName());
        if (req.getDescription() != null) existing.setDescription(req.getDescription());
        if (req.getStartDate() != null) existing.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) existing.setEndDate(req.getEndDate());
        if (req.getEstimatedEffortHours() > 0) existing.setEstimatedEffortHours(req.getEstimatedEffortHours());
        if (req.getProgressPercentage() >= 0) existing.setProgressPercentage(req.getProgressPercentage());
        if (req.getStatus() != null) existing.setStatus(req.getStatus());

        milestoneDAO.update(existing);

        if (req.getRequiredSkillIds() != null) {
            skillDAO.setMilestoneSkills(id, req.getRequiredSkillIds());
        }

        return mapToDTO(existing);
    }

    public boolean deleteMilestone(int id) throws SQLException {
        return milestoneDAO.delete(id);
    }

    private MilestoneResponseDTO mapToDTO(Milestone m) throws SQLException {
        MilestoneResponseDTO dto = new MilestoneResponseDTO();
        dto.setId(m.getId());
        dto.setProjectId(m.getProjectId());
        dto.setProjectName(m.getProjectName());
        dto.setName(m.getName());
        dto.setDescription(m.getDescription());
        dto.setStartDate(m.getStartDate());
        dto.setEndDate(m.getEndDate());
        dto.setEstimatedEffortHours(m.getEstimatedEffortHours());
        dto.setProgressPercentage(m.getProgressPercentage());
        dto.setStatus(m.getStatus());
        dto.setRequiredSkillNames(m.getRequiredSkillNames() != null ? m.getRequiredSkillNames() : new ArrayList<>());
        dto.setRequiredSkills(m.getRequiredSkillNames() != null ? String.join(", ", m.getRequiredSkillNames()) : "");
        dto.setRequiredSkillIds(m.getRequiredSkillIds() != null ? m.getRequiredSkillIds() : new ArrayList<>());
        List<Allocation> allocs = allocationDAO.findByMilestoneId(m.getId());
        List<AllocationResponseDTO> allocDTOs = new ArrayList<>();
        for (Allocation a : allocs) {
            AllocationResponseDTO aDTO = new AllocationResponseDTO();
            aDTO.setId(a.getId());
            aDTO.setDeveloperId(a.getDeveloperId());
            aDTO.setDeveloperName(a.getDeveloperName());
            aDTO.setProjectId(a.getProjectId());
            aDTO.setProjectName(a.getProjectName());
            aDTO.setMilestoneId(a.getMilestoneId());
            aDTO.setMilestoneName(a.getMilestoneName());
            aDTO.setAllocationPercentage(a.getAllocationPercentage());
            aDTO.setStartDate(a.getStartDate());
            aDTO.setEndDate(a.getEndDate());
            aDTO.setHourlyCostSnapshot(a.getHourlyCostSnapshot());
            aDTO.setEstimatedHours(a.getEstimatedHours());
            allocDTOs.add(aDTO);
        }
        dto.setAllocations(allocDTOs);
        return dto;
    }
}
