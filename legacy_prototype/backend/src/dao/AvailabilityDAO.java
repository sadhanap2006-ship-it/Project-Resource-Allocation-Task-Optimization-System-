package dao;

import model.Availability;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AvailabilityDAO {

    public List<Availability> findAll() throws SQLException {
        List<Availability> list = new ArrayList<>();
        String sql = "SELECT da.id, da.developer_id, d.name as developer_name, da.start_date, da.end_date, da.reason, da.status "
                   + "FROM developer_availability da "
                   + "JOIN developers d ON da.developer_id = d.id ORDER BY da.start_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Availability> findByDeveloperId(int developerId) throws SQLException {
        List<Availability> list = new ArrayList<>();
        String sql = "SELECT da.id, da.developer_id, d.name as developer_name, da.start_date, da.end_date, da.reason, da.status "
                   + "FROM developer_availability da "
                   + "JOIN developers d ON da.developer_id = d.id "
                   + "WHERE da.developer_id = ? ORDER BY da.start_date ASC";
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

    public int insert(Availability a) throws SQLException {
        String sql = "INSERT INTO developer_availability (developer_id, start_date, end_date, reason, status) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getDeveloperId());
            ps.setDate(2, Date.valueOf(a.getStartDate()));
            ps.setDate(3, Date.valueOf(a.getEndDate()));
            ps.setString(4, a.getReason() != null ? a.getReason() : "Planned Leave");
            ps.setString(5, a.getStatus() != null ? a.getStatus() : "APPROVED");

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM developer_availability WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Availability mapRow(ResultSet rs) throws SQLException {
        Availability a = new Availability();
        a.setId(rs.getInt("id"));
        a.setDeveloperId(rs.getInt("developer_id"));
        a.setDeveloperName(rs.getString("developer_name"));
        a.setStartDate(rs.getDate("start_date").toString());
        a.setEndDate(rs.getDate("end_date").toString());
        a.setReason(rs.getString("reason"));
        a.setStatus(rs.getString("status"));
        return a;
    }
}
