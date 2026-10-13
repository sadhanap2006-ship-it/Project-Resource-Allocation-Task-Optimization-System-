package dao;

import model.ProfileAuditLog;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    public List<ProfileAuditLog> findByDeveloperId(int developerId) throws SQLException {
        String sql = "SELECT pal.id, pal.developer_id, pal.actor_user_id, u.name AS actor_name, " +
                     "pal.action_type, pal.old_value, pal.new_value, pal.created_at " +
                     "FROM profile_audit_logs pal " +
                     "JOIN users u ON pal.actor_user_id = u.id " +
                     "WHERE pal.developer_id = ? ORDER BY pal.created_at DESC";

        List<ProfileAuditLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, developerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProfileAuditLog log = new ProfileAuditLog();
                    log.setId(rs.getInt("id"));
                    log.setDeveloperId(rs.getInt("developer_id"));
                    log.setActorUserId(rs.getInt("actor_user_id"));
                    log.setActorName(rs.getString("actor_name"));
                    log.setActionType(rs.getString("action_type"));
                    log.setOldValue(rs.getString("old_value"));
                    log.setNewValue(rs.getString("new_value"));
                    log.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(log);
                }
            }
        }
        return list;
    }

    public void insert(ProfileAuditLog log) throws SQLException {
        String sql = "INSERT INTO profile_audit_logs (developer_id, actor_user_id, action_type, old_value, new_value) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, log.getDeveloperId());
            ps.setInt(2, log.getActorUserId());
            ps.setString(3, log.getActionType());
            ps.setString(4, log.getOldValue());
            ps.setString(5, log.getNewValue());
            ps.executeUpdate();
        }
    }
}
