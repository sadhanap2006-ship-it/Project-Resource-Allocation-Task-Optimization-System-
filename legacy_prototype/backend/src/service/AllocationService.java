package service;

import dao.AllocationDAO;
import dao.DeveloperDAO;
import dao.MilestoneDAO;
import dao.NotificationDAO;
import dao.ProjectDAO;
import dto.AllocationRequestDTO;
import dto.AllocationResponseDTO;
import model.Allocation;
import model.Developer;
import model.Milestone;
import model.Notification;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AllocationService {

    private final AllocationDAO allocationDAO = new AllocationDAO();
    private final DeveloperDAO developerDAO = new DeveloperDAO();
    private final MilestoneDAO milestoneDAO = new MilestoneDAO();
    private final ProjectDAO projectDAO = new ProjectDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    public List<AllocationResponseDTO> getAllAllocations() throws SQLException {
        List<Allocation> list = allocationDAO.findAll();
        List<AllocationResponseDTO> dtos = new ArrayList<>();
        for (Allocation a : list) {
            dtos.add(mapToDTO(a));
        }
        return dtos;
    }

    public AllocationResponseDTO getAllocationById(int id) throws SQLException {
        Allocation a = allocationDAO.findById(id);
        if (a == null) return null;
        return mapToDTO(a);
    }

    public AllocationResponseDTO createAllocation(AllocationRequestDTO req) throws SQLException {
        int developerId = req.getDeveloperId();
        int milestoneId = req.getMilestoneId();
        int allocationPercentage = req.getAllocationPercentage();

        Developer dev = developerDAO.findById(developerId);
        Milestone milestone = milestoneDAO.findById(milestoneId);
        if (dev == null || milestone == null) return null;

        Allocation a = new Allocation();
        a.setDeveloperId(developerId);
        a.setProjectId(milestone.getProjectId());
        a.setMilestoneId(milestoneId);
        a.setAllocationPercentage(allocationPercentage);
        a.setStartDate(milestone.getStartDate());
        a.setEndDate(milestone.getEndDate());
        a.setHourlyCostSnapshot(dev.getHourlyCost());
        a.setEstimatedHours(milestone.getEstimatedEffortHours());

        int allocId = allocationDAO.insert(a);
        if (allocId <= 0) return null;
        a.setId(allocId);

        // Check if this allocation pushes developer over 100%
        List<Allocation> allDevAllocs = allocationDAO.findByDeveloperId(developerId);
        int totalLoad = 0;
        for (Allocation item : allDevAllocs) {
            totalLoad += item.getAllocationPercentage();
        }

        if (totalLoad > 100) {
            Notification n = new Notification();
            n.setUserId(1); // PM User
            n.setTitle("Resource Over-Allocation Warning");
            n.setMessage("Developer " + dev.getName() + " reached " + totalLoad + "% cumulative allocation after milestone assignment.");
            n.setType("OVER_ALLOCATION");
            n.setSeverity("CRITICAL");
            n.setRead(false);
            notificationDAO.insert(n);
        }

        // Update Project Spend snapshot and log budget transaction
        double additionalCost = milestone.getEstimatedEffortHours() * dev.getHourlyCost();
        model.Project p = projectDAO.findById(milestone.getProjectId());
        if (p != null) {
            projectDAO.addSpend(p.getId(), additionalCost, "Allocation for " + dev.getName() + " on " + milestone.getName(), java.time.LocalDate.now().toString());
        }

        return mapToDTO(a);
    }

    public boolean deleteAllocation(int id) throws SQLException {
        return allocationDAO.delete(id);
    }

    private AllocationResponseDTO mapToDTO(Allocation a) {
        AllocationResponseDTO dto = new AllocationResponseDTO();
        dto.setId(a.getId());
        dto.setDeveloperId(a.getDeveloperId());
        dto.setDeveloperName(a.getDeveloperName());
        dto.setProjectId(a.getProjectId());
        dto.setProjectName(a.getProjectName());
        dto.setMilestoneId(a.getMilestoneId());
        dto.setMilestoneName(a.getMilestoneName());
        dto.setAllocationPercentage(a.getAllocationPercentage());
        dto.setStartDate(a.getStartDate());
        dto.setEndDate(a.getEndDate());
        dto.setHourlyCostSnapshot(a.getHourlyCostSnapshot());
        dto.setEstimatedHours(a.getEstimatedHours());
        return dto;
    }
}
