package service;

import dao.CertificationDAO;
import dao.DeveloperDAO;
import dao.EmployeeSkillDAO;
import dao.UserDAO;
import dto.LoginRequestDTO;
import dto.LoginResponseDTO;
import model.Certification;
import model.Developer;
import model.EmployeeSkill;
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

    public boolean completeOnboarding(int userId, int devId, List<Map<String, Object>> skills, List<Map<String, Object>> certs) throws SQLException {
        if (skills != null) {
            for (Map<String, Object> s : skills) {
                int skillId = s.get("skillId") instanceof Number ? ((Number) s.get("skillId")).intValue() : Integer.parseInt(s.get("skillId").toString());
                String proficiency = (String) s.getOrDefault("proficiency", "INTERMEDIATE");
                int exp = s.get("yearsExperience") instanceof Number ? ((Number) s.get("yearsExperience")).intValue() : 1;
                boolean interested = Boolean.TRUE.equals(s.get("interestedInFuture"));

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

        if (certs != null) {
            for (Map<String, Object> c : certs) {
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

        return userDAO.setProfileCompleted(userId, true);
    }
}
