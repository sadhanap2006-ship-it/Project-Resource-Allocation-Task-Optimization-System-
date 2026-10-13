package dao;

import model.TaskFeedback;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FeedbackDAO {

    public List<TaskFeedback> findByDeveloperId(int developerId) throws SQLException {
        String sql = "SELECT tf.id, tf.milestone_id, m.name AS milestone_name, tf.developer_id, " +
                     "d.name AS dev_name, tf.manager_id, u.name AS manager_name, " +
                     "tf.rating, tf.comment, tf.created_at " +
                     "FROM task_feedback tf " +
                     "JOIN milestones m ON tf.milestone_id = m.id " +
                     "JOIN developers d ON tf.developer_id = d.id " +
                     "JOIN users u ON tf.manager_id = u.id " +
                     "WHERE tf.developer_id = ? ORDER BY tf.created_at DESC";

        List<TaskFeedback> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, developerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TaskFeedback tf = new TaskFeedback();
                    tf.setId(rs.getInt("id"));
                    tf.setMilestoneId(rs.getInt("milestone_id"));
                    tf.setMilestoneName(rs.getString("milestone_name"));
                    tf.setDeveloperId(rs.getInt("developer_id"));
                    tf.setDeveloperName(rs.getString("dev_name"));
                    tf.setManagerId(rs.getInt("manager_id"));
                    tf.setManagerName(rs.getString("manager_name"));
                    tf.setRating(rs.getInt("rating"));
                    tf.setComment(rs.getString("comment"));
                    tf.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(tf);
                }
            }
        }
        return list;
    }

    public int insert(TaskFeedback tf) throws SQLException {
        String sql = "INSERT INTO task_feedback (milestone_id, developer_id, manager_id, rating, comment) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tf.getMilestoneId());
            ps.setInt(2, tf.getDeveloperId());
            ps.setInt(3, tf.getManagerId());
            ps.setInt(4, tf.getRating());
            ps.setString(5, tf.getComment());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }
}
