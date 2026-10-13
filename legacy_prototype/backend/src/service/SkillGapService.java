package service;

import dao.AllocationDAO;
import dao.DeveloperDAO;
import dao.MilestoneDAO;
import dao.SkillDAO;
import model.Allocation;
import model.Developer;
import model.Milestone;

import java.sql.SQLException;
import java.util.*;

public class SkillGapService {

    private final SkillDAO skillDAO = new SkillDAO();
    private final MilestoneDAO milestoneDAO = new MilestoneDAO();
    private final DeveloperDAO developerDAO = new DeveloperDAO();
    private final AllocationDAO allocationDAO = new AllocationDAO();

    public List<String> getMissingSkills(int milestoneId, int developerId) throws SQLException {
        List<String> required = skillDAO.findSkillsByMilestoneId(milestoneId);
        List<String> held = skillDAO.findSkillsByDeveloperId(developerId);

        List<String> missing = new ArrayList<>();
        for (String r : required) {
            if (!held.contains(r)) {
                missing.add(r);
            }
        }
        return missing;
    }

    public List<Map<String, Object>> detectAllSkillGaps() throws SQLException {
        List<Map<String, Object>> gaps = new ArrayList<>();
        List<Milestone> milestones = milestoneDAO.findAll();

        for (Milestone m : milestones) {
            List<String> requiredSkills = m.getRequiredSkillNames();
            if (requiredSkills == null || requiredSkills.isEmpty()) continue;

            List<Allocation> allocs = allocationDAO.findByMilestoneId(m.getId());
            if (allocs.isEmpty()) {
                Map<String, Object> gap = new HashMap<>();
                gap.put("milestoneId", m.getId());
                gap.put("milestoneName", m.getName());
                gap.put("projectId", m.getProjectId());
                gap.put("projectName", m.getProjectName());
                gap.put("status", "UNASSIGNED");
                gap.put("requiredSkills", requiredSkills);
                gap.put("missingSkills", requiredSkills);
                gap.put("description", "Milestone '" + m.getName() + "' requires " + String.join(", ", requiredSkills) + " but has no assigned resources.");
                gaps.add(gap);
            } else {
                for (Allocation a : allocs) {
                    Developer d = developerDAO.findById(a.getDeveloperId());
                    if (d != null) {
                        List<String> missing = getMissingSkills(m.getId(), d.getId());
                        if (!missing.isEmpty()) {
                            Map<String, Object> gap = new HashMap<>();
                            gap.put("milestoneId", m.getId());
                            gap.put("milestoneName", m.getName());
                            gap.put("projectId", m.getProjectId());
                            gap.put("projectName", m.getProjectName());
                            gap.put("developerId", d.getId());
                            gap.put("developerName", d.getName());
                            gap.put("status", "SKILL_MISMATCH");
                            gap.put("requiredSkills", requiredSkills);
                            gap.put("heldSkills", d.getSkills());
                            gap.put("missingSkills", missing);
                            gap.put("description", d.getName() + " lacks required skill(s): " + String.join(", ", missing) + " on milestone '" + m.getName() + "'.");
                            gaps.add(gap);
                        }
                    }
                }
            }
        }
        return gaps;
    }

    public List<dto.SkillGapResponseDTO> detectAllSkillGapDTOs() throws SQLException {
        List<Map<String, Object>> raw = detectAllSkillGaps();
        List<dto.SkillGapResponseDTO> list = new ArrayList<>();
        int counter = 1;
        for (Map<String, Object> m : raw) {
            dto.SkillGapResponseDTO dto = new dto.SkillGapResponseDTO();
            dto.setId("GAP-" + (counter++));
            dto.setType("SKILL_GAP");
            dto.setSeverity("WARNING");
            if (m.containsKey("milestoneId")) dto.setMilestoneId(((Number) m.get("milestoneId")).intValue());
            if (m.containsKey("milestoneName")) dto.setMilestoneName((String) m.get("milestoneName"));
            if (m.containsKey("projectId")) dto.setProjectId(((Number) m.get("projectId")).intValue());
            if (m.containsKey("projectName")) dto.setProjectName((String) m.get("projectName"));
            if (m.containsKey("developerId")) dto.setDeveloperId(((Number) m.get("developerId")).intValue());
            if (m.containsKey("developerName")) dto.setDeveloperName((String) m.get("developerName"));
            if (m.containsKey("missingSkills")) {
                dto.setMissingSkills((List<String>) m.get("missingSkills"));
            }
            if (m.containsKey("requiredSkills")) {
                dto.setRequiredSkills((List<String>) m.get("requiredSkills"));
            }
            dto.setTitle("Skill Gap Alert");
            dto.setDescription((String) m.get("description"));
            dto.setRecommendation("Reassign milestone to engineer with matching skills or schedule training.");
            list.add(dto);
        }
        return list;
    }
}
