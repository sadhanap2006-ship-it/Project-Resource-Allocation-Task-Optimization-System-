package service;

import dao.AuditLogDAO;
import dao.EmployeeSkillDAO;
import dao.SkillDAO;
import model.ProfileAuditLog;
import model.Skill;

import java.sql.SQLException;
import java.util.*;

public class SkillTaxonomyService {

    public static final int TRACK_RECORD_THRESHOLD = 3;

    private static final Map<String, String> SYNONYM_MAP = new HashMap<>();

    static {
        // Master taxonomy synonym mapping
        registerSynonyms("React", "react", "react.js", "reactjs", "react native", "react-native");
        registerSynonyms("Java", "java", "core java", "java 17", "java 21", "java se", "java ee");
        registerSynonyms("MySQL", "mysql", "mysql 8", "mariadb", "mysql database");
        registerSynonyms("REST API", "rest", "restful", "rest api", "restful api", "web api", "api design");
        registerSynonyms("Docker", "docker", "docker compose", "containerization", "containers");
        registerSynonyms("AWS", "aws", "amazon web services", "ec2", "s3", "lambda", "cloud architecture");
        registerSynonyms("Python", "python", "python3", "py", "django", "fastapi", "flask");
        registerSynonyms("Security", "security", "infosec", "oauth2", "jwt", "cybersecurity", "rbac");
        registerSynonyms("Kafka", "kafka", "apache kafka", "event streaming", "message queue");
        registerSynonyms("Kubernetes", "kubernetes", "k8s", "container orchestration");
        registerSynonyms("TypeScript", "typescript", "ts");
        registerSynonyms("PostgreSQL", "postgresql", "postgres", "psql");
        registerSynonyms("GraphQL", "graphql", "gql");
        registerSynonyms("Microservices", "microservices", "microservice", "distributed systems");
    }

    private static void registerSynonyms(String canonical, String... synonyms) {
        for (String syn : synonyms) {
            SYNONYM_MAP.put(syn.toLowerCase().trim(), canonical);
        }
    }

    private final SkillDAO skillDAO = new SkillDAO();
    private final EmployeeSkillDAO employeeSkillDAO = new EmployeeSkillDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    /**
     * Resolves an extracted or input skill term to its canonical taxonomy name.
     */
    public String resolveCanonicalSkill(String skillName) {
        if (skillName == null) return null;
        String normalized = skillName.trim().toLowerCase();
        return SYNONYM_MAP.getOrDefault(normalized, capitalizeWords(skillName.trim()));
    }

    /**
     * Checks if the canonical name matches the approved master taxonomy.
     */
    public boolean isKnownTaxonomySkill(String canonicalName) {
        if (canonicalName == null) return false;
        return SYNONYM_MAP.containsValue(canonicalName);
    }

    /**
     * Searches skills from master taxonomy matching query string.
     */
    public List<Map<String, String>> searchTaxonomy(String query) throws SQLException {
        List<Skill> allSkills = skillDAO.findAll();
        List<Map<String, String>> results = new ArrayList<>();
        String q = query != null ? query.toLowerCase().trim() : "";

        Set<String> added = new HashSet<>();
        for (Skill s : allSkills) {
            if (q.isEmpty() || s.getName().toLowerCase().contains(q)) {
                Map<String, String> item = new HashMap<>();
                item.put("id", String.valueOf(s.getId()));
                item.put("name", s.getName());
                item.put("category", s.getCategory());
                results.add(item);
                added.add(s.getName().toLowerCase());
            }
        }

        // Add additional recognized synonyms
        for (String canonical : SYNONYM_MAP.values()) {
            if (!added.contains(canonical.toLowerCase()) && (q.isEmpty() || canonical.toLowerCase().contains(q))) {
                Map<String, String> item = new HashMap<>();
                item.put("name", canonical);
                item.put("category", "General");
                results.add(item);
                added.add(canonical.toLowerCase());
            }
        }

        return results;
    }

    /**
     * Feature 3c: Evaluates completed task history. If developer has completed
     * >= TRACK_RECORD_THRESHOLD (3) tasks requiring this skill, auto-promotes to VERIFIED_TRACK_RECORD.
     */
    public boolean checkAndAutoVerifyTrackRecord(int developerId, int skillId) throws SQLException {
        int completedCount = employeeSkillDAO.countCompletedMilestonesWithSkill(developerId, skillId);
        if (completedCount >= TRACK_RECORD_THRESHOLD) {
            boolean updated = employeeSkillDAO.updateStatus(developerId, skillId, "VERIFIED_TRACK_RECORD");
            if (updated) {
                ProfileAuditLog log = new ProfileAuditLog(
                    0, developerId, 1, "SKILL_AUTO_VERIFIED",
                    "PENDING",
                    "VERIFIED_TRACK_RECORD (" + completedCount + " completed milestones)"
                );
                auditLogDAO.insert(log);
                return true;
            }
        }
        return false;
    }

    private String capitalizeWords(String str) {
        if (str.isEmpty()) return str;
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
