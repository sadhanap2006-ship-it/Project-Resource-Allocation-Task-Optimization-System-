package dao;

import model.Skill;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SkillDAO {

    public List<Skill> findAll() throws SQLException {
        List<Skill> list = new ArrayList<>();
        String sql = "SELECT id, name, category FROM skills ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Skill(rs.getInt("id"), rs.getString("name"), rs.getString("category")));
            }
        }
        return list;
    }

    public Skill findById(int id) throws SQLException {
        String sql = "SELECT id, name, category FROM skills WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Skill(rs.getInt("id"), rs.getString("name"), rs.getString("category"));
                }
            }
        }
        return null;
    }

    public Skill findByName(String name) throws SQLException {
        String sql = "SELECT id, name, category FROM skills WHERE LOWER(name) = LOWER(?) LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name != null ? name.trim() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Skill(rs.getInt("id"), rs.getString("name"), rs.getString("category"));
                }
            }
        }
        return null;
    }

    public int insert(Skill skill) throws SQLException {
        String sql = "INSERT INTO skills (name, category) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, skill.getName());
            ps.setString(2, skill.getCategory());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<String> findSkillsByDeveloperId(int developerId) throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT s.name FROM skills s "
                   + "JOIN developer_skills ds ON s.id = ds.skill_id "
                   + "WHERE ds.developer_id = ? ORDER BY s.name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, developerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("name"));
                }
            }
        }
        return list;
    }

    public List<String> findSkillsByMilestoneId(int milestoneId) throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT s.name FROM skills s "
                   + "JOIN milestone_skills ms ON s.id = ms.skill_id "
                   + "WHERE ms.milestone_id = ? ORDER BY s.name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, milestoneId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("name"));
                }
            }
        }
        return list;
    }

    public List<String> findSkillsByProjectId(int projectId) throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT s.name FROM skills s "
                   + "JOIN project_skills ps ON s.id = ps.skill_id "
                   + "WHERE ps.project_id = ? ORDER BY s.name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("name"));
                }
            }
        }
        return list;
    }

    public void setProjectSkills(int projectId, List<Integer> skillIds) throws SQLException {
        String delSql = "DELETE FROM project_skills WHERE project_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(delSql)) {
            ps.setInt(1, projectId);
            ps.executeUpdate();
        }
        if (skillIds != null && !skillIds.isEmpty()) {
            String insSql = "INSERT INTO project_skills (project_id, skill_id) VALUES (?, ?)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(insSql)) {
                for (int sId : skillIds) {
                    ps.setInt(1, projectId);
                    ps.setInt(2, sId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    public void setMilestoneSkills(int milestoneId, List<Integer> skillIds) throws SQLException {
        String delSql = "DELETE FROM milestone_skills WHERE milestone_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(delSql)) {
            ps.setInt(1, milestoneId);
            ps.executeUpdate();
        }
        if (skillIds != null && !skillIds.isEmpty()) {
            String insSql = "INSERT INTO milestone_skills (milestone_id, skill_id) VALUES (?, ?)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(insSql)) {
                for (int sId : skillIds) {
                    ps.setInt(1, milestoneId);
                    ps.setInt(2, sId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }
}
