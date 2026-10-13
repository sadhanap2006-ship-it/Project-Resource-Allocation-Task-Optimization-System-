package dao;

import model.LeaveRequest;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeaveRequestDAO {

    public List<LeaveRequest> findByDeveloperId(int developerId) throws SQLException {
        String sql = "SELECT lr.id, lr.developer_id, d.name AS dev_name, lr.start_date, lr.end_date, " +
                     "lr.reason, lr.status, lr.reviewed_by, u.name AS reviewer_name, " +
                     "lr.reviewer_comments, lr.created_at " +
                     "FROM leave_requests lr " +
                     "JOIN developers d ON lr.developer_id = d.id " +
                     "LEFT JOIN users u ON lr.reviewed_by = u.id " +
                     "WHERE lr.developer_id = ? ORDER BY lr.start_date DESC";

        List<LeaveRequest> list = new ArrayList<>();
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

    public List<LeaveRequest> findAllPending() throws SQLException {
        String sql = "SELECT lr.id, lr.developer_id, d.name AS dev_name, lr.start_date, lr.end_date, " +
                     "lr.reason, lr.status, lr.reviewed_by, u.name AS reviewer_name, " +
                     "lr.reviewer_comments, lr.created_at " +
                     "FROM leave_requests lr " +
                     "JOIN developers d ON lr.developer_id = d.id " +
                     "LEFT JOIN users u ON lr.reviewed_by = u.id " +
                     "WHERE lr.status = 'PENDING' ORDER BY lr.created_at ASC";

        List<LeaveRequest> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<LeaveRequest> findApprovedInDateRange(int developerId, String startDate, String endDate) throws SQLException {
        String sql = "SELECT lr.id, lr.developer_id, d.name AS dev_name, lr.start_date, lr.end_date, " +
                     "lr.reason, lr.status, lr.reviewed_by, u.name AS reviewer_name, " +
                     "lr.reviewer_comments, lr.created_at " +
                     "FROM leave_requests lr " +
                     "JOIN developers d ON lr.developer_id = d.id " +
                     "LEFT JOIN users u ON lr.reviewed_by = u.id " +
                     "WHERE lr.developer_id = ? AND lr.status = 'APPROVED' " +
                     "AND lr.start_date <= ? AND lr.end_date >= ?";

        List<LeaveRequest> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, developerId);
            ps.setDate(2, Date.valueOf(endDate));
            ps.setDate(3, Date.valueOf(startDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public int insert(LeaveRequest req) throws SQLException {
        String sql = "INSERT INTO leave_requests (developer_id, start_date, end_date, reason, status) " +
                     "VALUES (?, ?, ?, ?, 'PENDING')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, req.getDeveloperId());
            ps.setDate(2, Date.valueOf(req.getStartDate()));
            ps.setDate(3, Date.valueOf(req.getEndDate()));
            ps.setString(4, req.getReason());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public boolean updateStatus(int id, String status, int reviewedBy, String comments) throws SQLException {
        String sql = "UPDATE leave_requests SET status = ?, reviewed_by = ?, reviewer_comments = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, reviewedBy);
            ps.setString(3, comments);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        }
    }

    private LeaveRequest mapRow(ResultSet rs) throws SQLException {
        LeaveRequest lr = new LeaveRequest();
        lr.setId(rs.getInt("id"));
        lr.setDeveloperId(rs.getInt("developer_id"));
        lr.setDeveloperName(rs.getString("dev_name"));
        Date s = rs.getDate("start_date");
        if (s != null) lr.setStartDate(s.toString());
        Date e = rs.getDate("end_date");
        if (e != null) lr.setEndDate(e.toString());
        lr.setReason(rs.getString("reason"));
        lr.setStatus(rs.getString("status"));
        int rb = rs.getInt("reviewed_by");
        if (!rs.wasNull()) lr.setReviewedBy(rb);
        lr.setReviewerName(rs.getString("reviewer_name"));
        lr.setReviewerComments(rs.getString("reviewer_comments"));
        lr.setCreatedAt(rs.getTimestamp("created_at"));
        return lr;
    }
}
