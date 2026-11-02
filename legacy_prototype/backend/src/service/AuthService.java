package service;

import dao.CertificationDAO;
import dao.DeveloperDAO;
import dao.EmployeeSkillDAO;
import dao.SkillDAO;
import dao.UserDAO;
import dto.LoginRequestDTO;
import dto.LoginResponseDTO;
import model.Certification;
import model.Developer;
import model.EmployeeSkill;
import model.Skill;
import model.User;
import util.JwtUtil;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final DeveloperDAO developerDAO = new DeveloperDAO();
    private final EmployeeSkillDAO employeeSkillDAO = new EmployeeSkillDAO();
    private final CertificationDAO certificationDAO = new CertificationDAO();
    private final SkillDAO skillDAO = new SkillDAO();

    public LoginResponseDTO login(LoginRequestDTO req) throws SQLException {
        if (req == null || req.getEmail() == null || req.getPassword() == null) {
            return null;
        }
        User user = userDAO.authenticate(req.getEmail(), req.getPassword());
        if (user == null) {
            return null;
        }

        // Canonical role mapping: normalize DEVELOPER -> EMPLOYEE, PROJECT_MANAGER -> MANAGER
        String role = user.getRole();
        if ("DEVELOPER".equalsIgnoreCase(role)) role = "EMPLOYEE";
        if ("PROJECT_MANAGER".equalsIgnoreCase(role)) role = "MANAGER";

        Integer devId = null;
        if ("EMPLOYEE".equalsIgnoreCase(role) || "DEVELOPER".equalsIgnoreCase(user.getRole())) {
            Developer dev = developerDAO.findByUserId(user.getId());
            devId = dev != null ? dev.getId() : user.getId();
        }

        String token = JwtUtil.generateToken(user.getId(), user.getEmail(), role, devId);
        String refreshToken = JwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        return new LoginResponseDTO(
            user.getId(),
            user.getName(),
            user.getEmail(),
            role,
            devId,
            token,
            refreshToken,
            user.isProfileCompleted()
        );
    }

    public String generatePasswordResetToken(String email) throws SQLException {
        User user = userDAO.findByEmail(email);
        if (user == null) return null;

        String resetToken = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Timestamp expiry = new Timestamp(System.currentTimeMillis() + 15 * 60 * 1000L); // 15 mins
        boolean saved = userDAO.saveResetToken(email, resetToken, expiry);
        return saved ? resetToken : null;
    }

    public boolean resetPassword(String resetToken, String newPassword) throws SQLException {
        User user = userDAO.findByResetToken(resetToken);
        if (user == null) return false;
        return userDAO.resetPassword(user.getId(), newPassword);
    }

    public LoginResponseDTO refreshSession(String refreshToken) throws SQLException {
        Map<String, Object> claims = JwtUtil.validateAndExtractClaims(refreshToken);
        if (claims == null) return null;

        Object userIdObj = claims.get("userId");
        if (userIdObj == null) return null;
        int userId = userIdObj instanceof Number ? ((Number) userIdObj).intValue() : Integer.parseInt(userIdObj.toString());

        User user = userDAO.findById(userId);
        if (user == null) return null;

        String role = user.getRole();
        if ("DEVELOPER".equalsIgnoreCase(role)) role = "EMPLOYEE";
        if ("PROJECT_MANAGER".equalsIgnoreCase(role)) role = "MANAGER";

        Integer devId = null;
        if ("EMPLOYEE".equalsIgnoreCase(role) || "DEVELOPER".equalsIgnoreCase(user.getRole())) {
            Developer dev = developerDAO.findByUserId(user.getId());
            devId = dev != null ? dev.getId() : user.getId();
        }

        String newToken = JwtUtil.generateToken(user.getId(), user.getEmail(), role, devId);
        String newRefreshToken = JwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        return new LoginResponseDTO(
            user.getId(),
            user.getName(),
            user.getEmail(),
            role,
            devId,
            newToken,
            newRefreshToken,
            user.isProfileCompleted()
        );
    }

    public boolean completeOnboarding(int userId, int devId, List<?> skills, List<?> certs) throws SQLException {
        if (skills != null) {
            for (Object item : skills) {
                if (item == null) continue;
                int skillId = -1;
                String proficiency = "INTERMEDIATE";
                int exp = 1;
                boolean interested = false;

                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> s = (Map<String, Object>) item;
                    Object skillIdObj = s.get("skillId");
                    if (skillIdObj instanceof Number) {
                        skillId = ((Number) skillIdObj).intValue();
                    } else if (skillIdObj != null) {
                        try {
                            skillId = Integer.parseInt(skillIdObj.toString());
                        } catch (NumberFormatException ignored) {}
                    }

                    if (skillId <= 0 && s.get("skillName") != null) {
                        String sName = s.get("skillName").toString().trim();
                        Skill found = skillDAO.findByName(sName);
                        if (found != null) {
                            skillId = found.getId();
                        } else {
                            skillId = skillDAO.insert(new Skill(0, sName, "Technical"));
                        }
                    }

                    proficiency = (String) s.getOrDefault("proficiency", "INTERMEDIATE");
                    Object expObj = s.get("yearsExperience");
                    exp = expObj instanceof Number ? ((Number) expObj).intValue() : 1;
                    interested = Boolean.TRUE.equals(s.get("interestedInFuture"));
                } else if (item instanceof String) {
                    String sName = ((String) item).trim();
                    if (!sName.isEmpty()) {
                        Skill found = skillDAO.findByName(sName);
                        if (found != null) {
                            skillId = found.getId();
                        } else {
                            skillId = skillDAO.insert(new Skill(0, sName, "Technical"));
                        }
                    }
                }

                if (skillId > 0) {
                    EmployeeSkill es = new EmployeeSkill();
                    es.setDeveloperId(devId);
                    es.setSkillId(skillId);
                    es.setProficiency(proficiency);
                    es.setYearsExperience(exp);
                    es.setStatus("PENDING");
                    es.setSource("MANUAL");
                    es.setInterestedInFuture(interested);
                    employeeSkillDAO.saveOrUpdate(es);
                }
            }
        }

        if (certs != null) {
            for (Object item : certs) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> c = (Map<String, Object>) item;
                    Certification cert = new Certification();
                    cert.setDeveloperId(devId);
                    cert.setName((String) c.get("name"));
                    cert.setIssuingBody((String) c.get("issuingBody"));
                    cert.setIssueDate((String) c.getOrDefault("issueDate", "2026-01-01"));
                    cert.setConfidenceScore(0.95);
                    cert.setVerified(true);
                    certificationDAO.insert(cert);
                }
            }
        }

        return userDAO.setProfileCompleted(userId, true);
    }
}
