package service;

import dao.CertificationDAO;
import dao.DeveloperDAO;
import dao.EmployeeSkillDAO;
import model.Certification;
import model.Developer;
import model.EmployeeSkill;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileCompletenessService {

    private final DeveloperDAO developerDAO = new DeveloperDAO();
    private final EmployeeSkillDAO employeeSkillDAO = new EmployeeSkillDAO();
    private final CertificationDAO certificationDAO = new CertificationDAO();

    public Map<String, Object> calculateCompleteness(int developerId) throws SQLException {
        Developer dev = developerDAO.findById(developerId);
        List<EmployeeSkill> skills = employeeSkillDAO.findByDeveloperId(developerId);
        List<Certification> certs = certificationDAO.findByDeveloperId(developerId);

        int score = 0;
        List<String> nudges = new ArrayList<>();
        List<String> completedItems = new ArrayList<>();

        // 1. Basic Profile (25 pts)
        if (dev != null && dev.getName() != null && !dev.getName().isEmpty() &&
            dev.getRoleTitle() != null && !dev.getRoleTitle().isEmpty()) {
            score += 25;
            completedItems.add("Basic profile details completed");
        } else {
            nudges.add("Add your official role title and department.");
        }

        // 2. Skills Count (30 pts)
        int skillCount = skills != null ? skills.size() : 0;
        if (skillCount >= 4) {
            score += 30;
            completedItems.add("Added 4+ verified/extracted skills");
        } else if (skillCount > 0) {
            score += (skillCount * 7);
            int needed = 4 - skillCount;
            nudges.add("Add " + needed + " more skill" + (needed > 1 ? "s" : "") + " to reach 100% profile completeness.");
        } else {
            nudges.add("Add your core skills or extract skills from a project description.");
        }

        // 3. Certifications (20 pts)
        int certCount = certs != null ? certs.size() : 0;
        if (certCount >= 1) {
            score += 20;
            completedItems.add("Uploaded professional certificate(s)");
        } else {
            nudges.add("Upload a professional certificate (e.g. AWS, Oracle, CKA) to boost your credibility.");
        }

        // 4. Future Interest Signals (15 pts)
        boolean hasInterest = false;
        if (skills != null) {
            for (EmployeeSkill s : skills) {
                if (s.isInterestedInFuture()) {
                    hasInterest = true;
                    break;
                }
            }
        }
        if (hasInterest) {
            score += 15;
            completedItems.add("Indicated future learning interests");
        } else {
            nudges.add("Toggle 'Actively looking to use this' on at least one skill to signal upcoming task preference.");
        }

        // 5. Verification Status (10 pts)
        boolean hasVerified = false;
        if (skills != null) {
            for (EmployeeSkill s : skills) {
                if ("VERIFIED_MANAGER".equals(s.getStatus()) || "VERIFIED_TRACK_RECORD".equals(s.getStatus())) {
                    hasVerified = true;
                    break;
                }
            }
        }
        if (hasVerified) {
            score += 10;
            completedItems.add("Has verified skills on record");
        } else {
            nudges.add("Submit pending skills for manager review to achieve verified status.");
        }

        if (score > 100) score = 100;

        Map<String, Object> result = new HashMap<>();
        result.put("developerId", developerId);
        result.put("completenessPercentage", score);
        result.put("nudges", nudges);
        result.put("completedMilestones", completedItems);
        result.put("isComplete", score >= 90);

        return result;
    }
}
