package dao;

import model.Developer;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeveloperDAO {

    private final SkillDAO skillDAO = new SkillDAO();

    public List<Developer> findAll() throws SQLException {
        List<Developer> list = new ArrayList<>();
        String sql = "SELECT id, user_id, name, email, role_title, department, hourly_cost, availability_percentage, status "
                   + "FROM developers ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Developer d = mapRow(rs);
                d.setSkills(skillDAO.findSkillsByDeveloperId(d.getId()));
                list.add(d);
            }
        }
        return list;
    }

    public Developer findById(int id) throws SQLException {
        String sql = "SELECT id, user_id, name, email, role_title, department, hourly_cost, availability_percentage, status "
                   + "FROM developers WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Developer d = mapRow(rs);
                    d.setSkills(skillDAO.findSkillsByDeveloperId(d.getId()));
                    return d;
                }
            }
        }
        return null;
    }

    public Developer findByUserId(int userId) throws SQLException {
        String sql = "SELECT id, user_id, name, email, role_title, department, hourly_cost, availability_percentage, status "
                   + "FROM developers WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Developer d = mapRow(rs);
                    d.setSkills(skillDAO.findSkillsByDeveloperId(d.getId()));
                    return d;
                }
            }
        }
        return null;
    }

    public int insert(Developer d) throws SQLException {
        String sql = "INSERT INTO developers (user_id, name, email, role_title, department, hourly_cost, availability_percentage, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (d.getUserId() != null) ps.setInt(1, d.getUserId());
            else ps.setNull(1, Types.INTEGER);
            ps.setString(2, d.getName());
            ps.setString(3, d.getEmail());
            ps.setString(4, d.getRoleTitle());
            ps.setString(5, d.getDepartment() != null ? d.getDepartment() : "Engineering");
            ps.setDouble(6, d.getHourlyCost());
            ps.setInt(7, d.getAvailabilityPercentage() > 0 ? d.getAvailabilityPercentage() : 100);
            ps.setString(8, d.getStatus() != null ? d.getStatus() : "ACTIVE");

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int devId = rs.getInt(1);
                    if (d.getSkillIds() != null && !d.getSkillIds().isEmpty()) {
                        mapSkills(conn, devId, d.getSkillIds());
                    }
                    return devId;
                }
            }
        }
        return 0;
    }

    public boolean update(Developer d) throws SQLException {
        String sql = "UPDATE developers SET name = ?, role_title = ?, department = ?, hourly_cost = ?, "
                   + "availability_percentage = ?, status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getName());
            ps.setString(2, d.getRoleTitle());
            ps.setString(3, d.getDepartment());
            ps.setDouble(4, d.getHourlyCost());
            ps.setInt(5, d.getAvailabilityPercentage());
            ps.setString(6, d.getStatus());
            ps.setInt(7, d.getId());
            int rows = ps.executeUpdate();
            if (rows > 0 && d.getSkillIds() != null && !d.getSkillIds().isEmpty()) {
                mapSkills(conn, d.getId(), d.getSkillIds());
            }
            return rows > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM developers WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private void mapSkills(Connection conn, int devId, List<Integer> skillIds) throws SQLException {
        String delSql = "DELETE FROM developer_skills WHERE developer_id = ?";
        try (PreparedStatement delPs = conn.prepareStatement(delSql)) {
            delPs.setInt(1, devId);
            delPs.executeUpdate();
        }

        String insSql = "INSERT INTO developer_skills (developer_id, skill_id) VALUES (?, ?)";
        try (PreparedStatement insPs = conn.prepareStatement(insSql)) {
            for (Integer sId : skillIds) {
                insPs.setInt(1, devId);
                insPs.setInt(2, sId);
                insPs.addBatch();
            }
            insPs.executeBatch();
        }
    }

    private Developer mapRow(ResultSet rs) throws SQLException {
        Developer d = new Developer();
        d.setId(rs.getInt("id"));
        int uId = rs.getInt("user_id");
        if (!rs.wasNull()) d.setUserId(uId);
        d.setName(rs.getString("name"));
        d.setEmail(rs.getString("email"));
        d.setRoleTitle(rs.getString("role_title"));
        d.setDepartment(rs.getString("department"));
        d.setHourlyCost(rs.getDouble("hourly_cost"));
        d.setAvailabilityPercentage(rs.getInt("availability_percentage"));
        d.setStatus(rs.getString("status"));
        return d;
    }
}
