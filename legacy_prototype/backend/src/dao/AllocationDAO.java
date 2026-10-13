package dao;

import model.Allocation;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AllocationDAO {

    public List<Allocation> findAll() throws SQLException {
        List<Allocation> list = new ArrayList<>();
        String sql = "SELECT a.id, a.developer_id, d.name as developer_name, a.project_id, p.name as project_name, "
                   + "a.milestone_id, m.name as milestone_name, a.allocation_percentage, a.start_date, a.end_date, "
                   + "a.hourly_cost_snapshot, a.estimated_hours "
                   + "FROM resource_allocations a "
                   + "JOIN developers d ON a.developer_id = d.id "
                   + "JOIN projects p ON a.project_id = p.id "
                   + "JOIN milestones m ON a.milestone_id = m.id "
                   + "ORDER BY a.start_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Allocation> findByDeveloperId(int developerId) throws SQLException {
        List<Allocation> list = new ArrayList<>();
        String sql = "SELECT a.id, a.developer_id, d.name as developer_name, a.project_id, p.name as project_name, "
                   + "a.milestone_id, m.name as milestone_name, a.allocation_percentage, a.start_date, a.end_date, "
                   + "a.hourly_cost_snapshot, a.estimated_hours "
                   + "FROM resource_allocations a "
                   + "JOIN developers d ON a.developer_id = d.id "
                   + "JOIN projects p ON a.project_id = p.id "
                   + "JOIN milestones m ON a.milestone_id = m.id "
                   + "WHERE a.developer_id = ? ORDER BY a.start_date ASC";
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

    public List<Allocation> findByMilestoneId(int milestoneId) throws SQLException {
        List<Allocation> list = new ArrayList<>();
        String sql = "SELECT a.id, a.developer_id, d.name as developer_name, a.project_id, p.name as project_name, "
                   + "a.milestone_id, m.name as milestone_name, a.allocation_percentage, a.start_date, a.end_date, "
                   + "a.hourly_cost_snapshot, a.estimated_hours "
                   + "FROM resource_allocations a "
                   + "JOIN developers d ON a.developer_id = d.id "
                   + "JOIN projects p ON a.project_id = p.id "
                   + "JOIN milestones m ON a.milestone_id = m.id "
                   + "WHERE a.milestone_id = ? ORDER BY a.start_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, milestoneId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public Allocation findById(int id) throws SQLException {
        String sql = "SELECT a.id, a.developer_id, d.name as developer_name, a.project_id, p.name as project_name, "
                   + "a.milestone_id, m.name as milestone_name, a.allocation_percentage, a.start_date, a.end_date, "
                   + "a.hourly_cost_snapshot, a.estimated_hours "
                   + "FROM resource_allocations a "
                   + "JOIN developers d ON a.developer_id = d.id "
                   + "JOIN projects p ON a.project_id = p.id "
                   + "JOIN milestones m ON a.milestone_id = m.id "
                   + "WHERE a.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public int insert(Allocation a) throws SQLException {
        String sql = "INSERT INTO resource_allocations (developer_id, project_id, milestone_id, allocation_percentage, start_date, end_date, hourly_cost_snapshot, estimated_hours) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getDeveloperId());
            ps.setInt(2, a.getProjectId());
            ps.setInt(3, a.getMilestoneId());
            ps.setInt(4, a.getAllocationPercentage());
            ps.setDate(5, Date.valueOf(a.getStartDate()));
            ps.setDate(6, Date.valueOf(a.getEndDate()));
            ps.setDouble(7, a.getHourlyCostSnapshot());
            ps.setDouble(8, a.getEstimatedHours());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public boolean update(Allocation a) throws SQLException {
        String sql = "UPDATE resource_allocations SET developer_id = ?, allocation_percentage = ?, start_date = ?, end_date = ?, hourly_cost_snapshot = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, a.getDeveloperId());
            ps.setInt(2, a.getAllocationPercentage());
            ps.setDate(3, Date.valueOf(a.getStartDate()));
            ps.setDate(4, Date.valueOf(a.getEndDate()));
            ps.setDouble(5, a.getHourlyCostSnapshot());
            ps.setInt(6, a.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM resource_allocations WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteByMilestoneAndDeveloper(int milestoneId, int developerId) throws SQLException {
        String sql = "DELETE FROM resource_allocations WHERE milestone_id = ? AND developer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, milestoneId);
            ps.setInt(2, developerId);
            return ps.executeUpdate() > 0;
        }
    }

    private Allocation mapRow(ResultSet rs) throws SQLException {
        Allocation a = new Allocation();
        a.setId(rs.getInt("id"));
        a.setDeveloperId(rs.getInt("developer_id"));
        a.setDeveloperName(rs.getString("developer_name"));
        a.setProjectId(rs.getInt("project_id"));
        a.setProjectName(rs.getString("project_name"));
        a.setMilestoneId(rs.getInt("milestone_id"));
        a.setMilestoneName(rs.getString("milestone_name"));
        a.setAllocationPercentage(rs.getInt("allocation_percentage"));
        a.setStartDate(rs.getDate("start_date").toString());
        a.setEndDate(rs.getDate("end_date").toString());
        a.setHourlyCostSnapshot(rs.getDouble("hourly_cost_snapshot"));
        a.setEstimatedHours(rs.getDouble("estimated_hours"));
        return a;
    }
}
