package dao;

import model.Milestone;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MilestoneDAO {

    private final SkillDAO skillDAO = new SkillDAO();

    public List<Milestone> findAll() throws SQLException {
        List<Milestone> list = new ArrayList<>();
        String sql = "SELECT m.id, m.project_id, p.name as project_name, m.name, m.description, "
                   + "m.start_date, m.end_date, m.estimated_effort_hours, m.progress_percentage, m.status "
                   + "FROM milestones m "
                   + "JOIN projects p ON m.project_id = p.id ORDER BY m.start_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Milestone m = mapRow(rs);
                m.setRequiredSkillNames(skillDAO.findSkillsByMilestoneId(m.getId()));
                list.add(m);
            }
        }
        return list;
    }

    public List<Milestone> findByProjectId(int projectId) throws SQLException {
        List<Milestone> list = new ArrayList<>();
        String sql = "SELECT m.id, m.project_id, p.name as project_name, m.name, m.description, "
                   + "m.start_date, m.end_date, m.estimated_effort_hours, m.progress_percentage, m.status "
                   + "FROM milestones m "
                   + "JOIN projects p ON m.project_id = p.id WHERE m.project_id = ? ORDER BY m.start_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Milestone m = mapRow(rs);
                    m.setRequiredSkillNames(skillDAO.findSkillsByMilestoneId(m.getId()));
                    list.add(m);
                }
            }
        }
        return list;
    }

    public Milestone findById(int id) throws SQLException {
        String sql = "SELECT m.id, m.project_id, p.name as project_name, m.name, m.description, "
                   + "m.start_date, m.end_date, m.estimated_effort_hours, m.progress_percentage, m.status "
                   + "FROM milestones m "
                   + "JOIN projects p ON m.project_id = p.id WHERE m.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Milestone m = mapRow(rs);
                    m.setRequiredSkillNames(skillDAO.findSkillsByMilestoneId(m.getId()));
                    return m;
                }
            }
        }
        return null;
    }

    public int insert(Milestone m) throws SQLException {
        String sql = "INSERT INTO milestones (project_id, name, description, start_date, end_date, estimated_effort_hours, progress_percentage, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getProjectId());
            ps.setString(2, m.getName());
            ps.setString(3, m.getDescription());
            ps.setDate(4, Date.valueOf(m.getStartDate()));
            ps.setDate(5, Date.valueOf(m.getEndDate()));
            ps.setDouble(6, m.getEstimatedEffortHours());
            ps.setInt(7, m.getProgressPercentage());
            ps.setString(8, m.getStatus() != null ? m.getStatus() : "NOT_STARTED");

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int mId = rs.getInt(1);
                    if (m.getRequiredSkillIds() != null && !m.getRequiredSkillIds().isEmpty()) {
                        mapSkills(conn, mId, m.getRequiredSkillIds());
                    }
                    return mId;
                }
            }
        }
        return 0;
    }

    public boolean update(Milestone m) throws SQLException {
        String sql = "UPDATE milestones SET name = ?, description = ?, start_date = ?, end_date = ?, "
                   + "estimated_effort_hours = ?, progress_percentage = ?, status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getDescription());
            ps.setDate(3, Date.valueOf(m.getStartDate()));
            ps.setDate(4, Date.valueOf(m.getEndDate()));
            ps.setDouble(5, m.getEstimatedEffortHours());
            ps.setInt(6, m.getProgressPercentage());
            ps.setString(7, m.getStatus());
            ps.setInt(8, m.getId());
            int rows = ps.executeUpdate();
            if (rows > 0 && m.getRequiredSkillIds() != null && !m.getRequiredSkillIds().isEmpty()) {
                mapSkills(conn, m.getId(), m.getRequiredSkillIds());
            }
            return rows > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM milestones WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private void mapSkills(Connection conn, int milestoneId, List<Integer> skillIds) throws SQLException {
        String delSql = "DELETE FROM milestone_skills WHERE milestone_id = ?";
        try (PreparedStatement delPs = conn.prepareStatement(delSql)) {
            delPs.setInt(1, milestoneId);
            delPs.executeUpdate();
        }

        String insSql = "INSERT INTO milestone_skills (milestone_id, skill_id) VALUES (?, ?)";
        try (PreparedStatement insPs = conn.prepareStatement(insSql)) {
            for (Integer sId : skillIds) {
                insPs.setInt(1, milestoneId);
                insPs.setInt(2, sId);
                insPs.addBatch();
            }
            insPs.executeBatch();
        }
    }

    private Milestone mapRow(ResultSet rs) throws SQLException {
        Milestone m = new Milestone();
        m.setId(rs.getInt("id"));
        m.setProjectId(rs.getInt("project_id"));
        m.setProjectName(rs.getString("project_name"));
        m.setName(rs.getString("name"));
        m.setDescription(rs.getString("description"));
        m.setStartDate(rs.getDate("start_date").toString());
        m.setEndDate(rs.getDate("end_date").toString());
        m.setEstimatedEffortHours(rs.getDouble("estimated_effort_hours"));
        m.setProgressPercentage(rs.getInt("progress_percentage"));
        m.setStatus(rs.getString("status"));
        return m;
    }
}
