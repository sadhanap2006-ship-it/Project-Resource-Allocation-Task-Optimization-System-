package service;

import dao.AllocationDAO;
import dao.AvailabilityDAO;
import dao.DeveloperDAO;
import dao.MilestoneDAO;
import model.Allocation;
import model.Availability;
import model.Developer;
import model.Milestone;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class ConflictDetectionService {

    private final DeveloperDAO developerDAO = new DeveloperDAO();
    private final AllocationDAO allocationDAO = new AllocationDAO();
    private final AvailabilityDAO availabilityDAO = new AvailabilityDAO();
    private final MilestoneDAO milestoneDAO = new MilestoneDAO();
    private final SkillGapService skillGapService = new SkillGapService();

    public List<Map<String, Object>> detectAllConflicts() throws SQLException {
        List<Map<String, Object>> conflicts = new ArrayList<>();
        List<Developer> developers = developerDAO.findAll();

        for (Developer dev : developers) {
            List<Allocation> allocations = allocationDAO.findByDeveloperId(dev.getId());
            List<Availability> leaves = availabilityDAO.findByDeveloperId(dev.getId());

            // 1. Check Over-Allocation via Boundary Sweep-Line Analysis
            detectDeveloperOverAllocation(dev, allocations, conflicts);

            // 2. Check Leave Conflicts
            detectDeveloperLeaveConflicts(dev, allocations, leaves, conflicts);
        }

        // 3. Check Skill Gaps
        List<Milestone> milestones = milestoneDAO.findAll();
        for (Milestone m : milestones) {
            List<Allocation> mAllocs = allocationDAO.findByMilestoneId(m.getId());
            for (Allocation a : mAllocs) {
                Developer d = developerDAO.findById(a.getDeveloperId());
                if (d != null) {
                    List<String> missing = skillGapService.getMissingSkills(m.getId(), d.getId());
                    if (!missing.isEmpty()) {
                        Map<String, Object> gap = new HashMap<>();
                        gap.put("conflictType", "SKILL_GAP_ALERT");
                        gap.put("severity", "WARNING");
                        gap.put("developerId", d.getId());
                        gap.put("developerName", d.getName());
                        gap.put("milestoneId", m.getId());
                        gap.put("milestoneName", m.getName());
                        gap.put("projectName", m.getProjectName());
                        gap.put("description", "Assigned developer " + d.getName() + " lacks required skill(s): " 
                                + String.join(", ", missing) + " on milestone '" + m.getName() + "'.");
                        gap.put("recommendation", "Assign developer with required competencies (" + String.join(", ", missing) + ") or conduct training.");
                        conflicts.add(gap);
                    }
                }
            }
        }

        return conflicts;
    }

    public List<dto.ConflictResponseDTO> detectAllConflictDTOs() throws SQLException {
        List<Map<String, Object>> raw = detectAllConflicts();
        List<dto.ConflictResponseDTO> list = new ArrayList<>();
        int counter = 1;
        for (Map<String, Object> m : raw) {
            dto.ConflictResponseDTO dto = new dto.ConflictResponseDTO();
            dto.setId("CONF-" + (counter++));
            String cType = (String) m.get("conflictType");
            dto.setType(cType);
            dto.setConflictType(cType);
            dto.setSeverity((String) m.get("severity"));
            if (m.containsKey("developerId")) dto.setDeveloperId(((Number) m.get("developerId")).intValue());
            if (m.containsKey("developerName")) dto.setDeveloperName((String) m.get("developerName"));
            dto.setTitle(cType);
            dto.setDescription((String) m.get("description"));
            dto.setRecommendation((String) m.get("recommendation"));
            if (m.containsKey("peakAllocation")) dto.setPeakAllocationPercentage(((Number) m.get("peakAllocation")).intValue());
            if (m.containsKey("milestoneId")) dto.setMilestoneId(((Number) m.get("milestoneId")).intValue());
            if (m.containsKey("milestoneName")) dto.setMilestoneName((String) m.get("milestoneName"));
            if (m.containsKey("projectName")) dto.setProjectName((String) m.get("projectName"));
            list.add(dto);
        }
        return list;
    }

    private void detectDeveloperOverAllocation(Developer dev, List<Allocation> allocations, List<Map<String, Object>> conflicts) {
        if (allocations == null || allocations.size() < 2) return;

        TreeSet<LocalDate> boundaryDates = new TreeSet<>();
        for (Allocation a : allocations) {
            boundaryDates.add(LocalDate.parse(a.getStartDate()));
            boundaryDates.add(LocalDate.parse(a.getEndDate()));
        }

        int maxCumulative = 0;
        LocalDate worstDate = null;
        List<String> overlappingMilestones = new ArrayList<>();

        for (LocalDate date : boundaryDates) {
            int currentLoad = 0;
            List<String> activeOnDate = new ArrayList<>();
            for (Allocation a : allocations) {
                LocalDate start = LocalDate.parse(a.getStartDate());
                LocalDate end = LocalDate.parse(a.getEndDate());
                if (!date.isBefore(start) && !date.isAfter(end)) {
                    currentLoad += a.getAllocationPercentage();
                    activeOnDate.add(a.getMilestoneName() + " (" + a.getAllocationPercentage() + "%)");
                }
            }

            if (currentLoad > maxCumulative) {
                maxCumulative = currentLoad;
                worstDate = date;
                overlappingMilestones = activeOnDate;
            }
        }

        if (maxCumulative > 100) {
            Map<String, Object> c = new HashMap<>();
            c.put("conflictType", "OVER_ALLOCATION_CONFLICT");
            c.put("severity", "CRITICAL");
            c.put("developerId", dev.getId());
            c.put("developerName", dev.getName());
            c.put("peakAllocation", maxCumulative);
            if (allocations != null && !allocations.isEmpty()) {
                c.put("milestoneId", allocations.get(0).getMilestoneId());
                c.put("milestoneName", allocations.get(0).getMilestoneName());
                c.put("projectName", allocations.get(0).getProjectName());
            }
            c.put("description", dev.getName() + " is over-allocated to " + maxCumulative 
                    + "% peak load across overlapping milestones: " + String.join(", ", overlappingMilestones) + ".");
            c.put("recommendation", "Reduce allocation percentages or reassign milestone to another developer to bring total load <= 100%.");
            conflicts.add(c);
        }
    }

    private void detectDeveloperLeaveConflicts(Developer dev, List<Allocation> allocations, List<Availability> leaves, List<Map<String, Object>> conflicts) {
        if (allocations == null || leaves == null) return;

        for (Availability leave : leaves) {
            LocalDate lStart = LocalDate.parse(leave.getStartDate());
            LocalDate lEnd = LocalDate.parse(leave.getEndDate());

            for (Allocation alloc : allocations) {
                LocalDate aStart = LocalDate.parse(alloc.getStartDate());
                LocalDate aEnd = LocalDate.parse(alloc.getEndDate());

                // Inclusive date interval overlap: !(aStart > lEnd || lStart > aEnd)
                if (!aStart.isAfter(lEnd) && !lStart.isAfter(aEnd)) {
                    Map<String, Object> c = new HashMap<>();
                    c.put("conflictType", "LEAVE_CONFLICT");
                    c.put("severity", "WARNING");
                    c.put("developerId", dev.getId());
                    c.put("developerName", dev.getName());
                    c.put("milestoneId", alloc.getMilestoneId());
                    c.put("milestoneName", alloc.getMilestoneName());
                    c.put("projectName", alloc.getProjectName());
                    c.put("description", dev.getName() + " has approved leave (" + leave.getStartDate() 
                            + " to " + leave.getEndDate() + ") overlapping active milestone assignment '" 
                            + alloc.getMilestoneName() + "'. Effective availability is 0%.");
                    c.put("recommendation", "Adjust milestone schedule or assign temporary backup coverage during leave.");
                    conflicts.add(c);
                }
            }
        }
    }
}
