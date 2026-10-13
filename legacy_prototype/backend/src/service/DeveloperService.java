package service;

import dao.AllocationDAO;
import dao.DeveloperDAO;
import dto.DeveloperRequestDTO;
import dto.DeveloperResponseDTO;
import model.Allocation;
import model.Developer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DeveloperService {

    private final DeveloperDAO developerDAO = new DeveloperDAO();
    private final AllocationDAO allocationDAO = new AllocationDAO();

    public List<DeveloperResponseDTO> getAllDevelopers() throws SQLException {
        List<Developer> list = developerDAO.findAll();
        List<DeveloperResponseDTO> dtos = new ArrayList<>();
        for (Developer d : list) {
            dtos.add(mapToDTO(d));
        }
        return dtos;
    }

    public DeveloperResponseDTO getDeveloperById(int id) throws SQLException {
        Developer d = developerDAO.findById(id);
        if (d == null) return null;
        return mapToDTO(d);
    }

    public DeveloperResponseDTO createDeveloper(DeveloperRequestDTO req) throws SQLException {
        Developer d = new Developer();
        d.setName(req.getName());
        d.setEmail(req.getEmail());
        d.setRoleTitle(req.getRoleTitle() != null ? req.getRoleTitle() : "Developer");
        d.setDepartment(req.getDepartment() != null ? req.getDepartment() : "Engineering");
        d.setHourlyCost(req.getHourlyCost());
        d.setAvailabilityPercentage(req.getAvailabilityPercentage() > 0 ? req.getAvailabilityPercentage() : 100);
        d.setStatus(req.getStatus() != null ? req.getStatus() : "ACTIVE");
        if (req.getSkillIds() != null) {
            d.setSkillIds(req.getSkillIds());
        }

        int newId = developerDAO.insert(d);
        d.setId(newId);
        return mapToDTO(d);
    }

    public DeveloperResponseDTO updateDeveloper(int id, DeveloperRequestDTO req) throws SQLException {
        Developer existing = developerDAO.findById(id);
        if (existing == null) return null;

        if (req.getName() != null) existing.setName(req.getName());
        if (req.getEmail() != null) existing.setEmail(req.getEmail());
        if (req.getRoleTitle() != null) existing.setRoleTitle(req.getRoleTitle());
        if (req.getDepartment() != null) existing.setDepartment(req.getDepartment());
        if (req.getHourlyCost() > 0) existing.setHourlyCost(req.getHourlyCost());
        if (req.getAvailabilityPercentage() > 0) existing.setAvailabilityPercentage(req.getAvailabilityPercentage());
        if (req.getStatus() != null) existing.setStatus(req.getStatus());

        developerDAO.update(existing);
        return mapToDTO(existing);
    }

    public boolean deleteDeveloper(int id) throws SQLException {
        return developerDAO.delete(id);
    }

    private DeveloperResponseDTO mapToDTO(Developer d) throws SQLException {
        List<Allocation> allocs = allocationDAO.findByDeveloperId(d.getId());
        int totalLoad = 0;
        for (Allocation a : allocs) totalLoad += a.getAllocationPercentage();

        DeveloperResponseDTO dto = new DeveloperResponseDTO();
        dto.setId(d.getId());
        dto.setUserId(d.getUserId());
        dto.setName(d.getName());
        dto.setEmail(d.getEmail());
        dto.setRoleTitle(d.getRoleTitle());
        dto.setDepartment(d.getDepartment());
        dto.setHourlyCost(d.getHourlyCost());
        dto.setAvailabilityPercentage(d.getAvailabilityPercentage());
        dto.setStatus(d.getStatus());
        dto.setSkills(d.getSkills() != null ? d.getSkills() : new ArrayList<>());
        dto.setSkillIds(d.getSkillIds() != null ? d.getSkillIds() : new ArrayList<>());
        dto.setCurrentAllocation(totalLoad);
        dto.setRemainingCapacity(Math.max(0, d.getAvailabilityPercentage() - totalLoad));
        return dto;
    }
}
