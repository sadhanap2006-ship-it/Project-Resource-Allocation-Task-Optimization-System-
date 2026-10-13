package dao;

import model.EmployeeSkill;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeSkillDAO {

    public List<EmployeeSkill> findByDeveloperId(int developerId) throws SQLException {
        String sql = "SELECT ds.developer_id, ds.skill_id, s.name AS skill_name, s.category, " +
                     "ds.proficiency, ds.years_experience, ds.status, ds.source, " +
                     "ds.interested_in_future, ds.endorsements_count, ds.created_at " +
                     "FROM developer_skills ds " +
                     "JOIN skills s ON ds.skill_id = s.id " +
                     "WHERE ds.developer_id = ? " +
                     "ORDER BY ds.status ASC, s.name ASC";

        List<EmployeeSkill> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, developerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<EmployeeSkill> findAllPending() throws SQLException {
        String sql = "SELECT ds.developer_id, ds.skill_id, s.name AS skill_name, s.category, " +
                     "ds.proficiency, ds.years_experience, ds.status, ds.source, " +
                     "ds.interested_in_future, ds.endorsements_count, ds.created_at, d.name AS dev_name " +
                     "FROM developer_skills ds " +
                     "JOIN skills s ON ds.skill_id = s.id " +
                     "JOIN developers d ON ds.developer_id = d.id " +
                     "WHERE ds.status = 'PENDING' " +
                     "ORDER BY ds.created_at DESC";

        List<EmployeeSkill> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                EmployeeSkill es = mapRow(rs);
                list.add(es);
            }
        }
        return list;
    }

    public boolean saveOrUpdate(EmployeeSkill es) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM developer_skills WHERE developer_id = ? AND skill_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setInt(1, es.getDeveloperId());
            checkPs.setInt(2, es.getSkillId());
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    // Update
                    String updateSql = "UPDATE developer_skills SET proficiency = ?, years_experience = ?, " +
                                       "status = ?, source = ?, interested_in_future = ? WHERE developer_id = ? AND skill_id = ?";
                    try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                        updatePs.setString(1, es.getProficiency());
                        updatePs.setInt(2, es.getYearsExperience());
                        updatePs.setString(3, es.getStatus());
                        updatePs.setString(4, es.getSource());
                        updatePs.setBoolean(5, es.isInterestedInFuture());
                        updatePs.setInt(6, es.getDeveloperId());
                        updatePs.setInt(7, es.getSkillId());
                        return updatePs.executeUpdate() > 0;
                    }
                } else {
                    // Insert
                    String insertSql = "INSERT INTO developer_skills (developer_id, skill_id, proficiency, " +
                                       "years_experience, status, source, interested_in_future, endorsements_count) " +
                                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                        insertPs.setInt(1, es.getDeveloperId());
                        insertPs.setInt(2, es.getSkillId());
                        insertPs.setString(3, es.getProficiency() != null ? es.getProficiency() : "INTERMEDIATE");
                        insertPs.setInt(4, es.getYearsExperience() > 0 ? es.getYearsExperience() : 1);
                        insertPs.setString(5, es.getStatus() != null ? es.getStatus() : "PENDING");
                        insertPs.setString(6, es.getSource() != null ? es.getSource() : "MANUAL");
                        insertPs.setBoolean(7, es.isInterestedInFuture());
                        insertPs.setInt(8, es.getEndorsementsCount());
                        return insertPs.executeUpdate() > 0;
                    }
                }
            }
        }
    }

    public boolean updateStatus(int developerId, int skillId, String status) throws SQLException {
        String sql = "UPDATE developer_skills SET status = ? WHERE developer_id = ? AND skill_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, developerId);
            ps.setInt(3, skillId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean incrementEndorsement(int developerId, int skillId) throws SQLException {
        String sql = "UPDATE developer_skills SET endorsements_count = endorsements_count + 1 WHERE developer_id = ? AND skill_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, developerId);
            ps.setInt(2, skillId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int developerId, int skillId) throws SQLException {
        String sql = "DELETE FROM developer_skills WHERE developer_id = ? AND skill_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, developerId);
            ps.setInt(2, skillId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Counts how many completed milestones assigned to this developer required this skill.
     * Used for auto-promotion to VERIFIED_TRACK_RECORD when threshold >= 3.
     */
    public int countCompletedMilestonesWithSkill(int developerId, int skillId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT m.id) " +
                     "FROM resource_allocations ra " +
                     "JOIN milestones m ON ra.milestone_id = m.id " +
                     "JOIN milestone_skills ms ON m.id = ms.milestone_id " +
                     "WHERE ra.developer_id = ? AND ms.skill_id = ? AND m.status = 'COMPLETED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, developerId);
            ps.setInt(2, skillId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private EmployeeSkill mapRow(ResultSet rs) throws SQLException {
        EmployeeSkill es = new EmployeeSkill();
        es.setDeveloperId(rs.getInt("developer_id"));
        es.setSkillId(rs.getInt("skill_id"));
        es.setSkillName(rs.getString("skill_name"));
        es.setCategory(rs.getString("category"));
        es.setProficiency(rs.getString("proficiency"));
        es.setYearsExperience(rs.getInt("years_experience"));
        es.setStatus(rs.getString("status"));
        es.setSource(rs.getString("source"));
        es.setInterestedInFuture(rs.getBoolean("interested_in_future"));
        es.setEndorsementsCount(rs.getInt("endorsements_count"));
        es.setCreatedAt(rs.getTimestamp("created_at"));
        return es;
    }
}
