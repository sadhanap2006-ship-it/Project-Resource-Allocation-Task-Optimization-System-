package dao;

import model.BudgetTransaction;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BudgetTransactionDAO {

    public int insert(BudgetTransaction tx) throws SQLException {
        String sql = "INSERT INTO budget_transactions (project_id, amount, transaction_date, description) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, tx.getProjectId());
            stmt.setDouble(2, tx.getAmount());
            stmt.setString(3, tx.getTransactionDate());
            stmt.setString(4, tx.getDescription());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<BudgetTransaction> findByProjectId(int projectId) throws SQLException {
        String sql = "SELECT * FROM budget_transactions WHERE project_id = ? ORDER BY transaction_date DESC, id DESC";
        List<BudgetTransaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public List<BudgetTransaction> findRecentByProjectId(int projectId, int windowDays) throws SQLException {
        String sql = "SELECT * FROM budget_transactions WHERE project_id = ? AND transaction_date >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ORDER BY transaction_date DESC";
        List<BudgetTransaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            stmt.setInt(2, windowDays);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    private BudgetTransaction mapResultSet(ResultSet rs) throws SQLException {
        BudgetTransaction t = new BudgetTransaction();
        t.setId(rs.getInt("id"));
        t.setProjectId(rs.getInt("project_id"));
        t.setAmount(rs.getDouble("amount"));
        t.setTransactionDate(rs.getString("transaction_date"));
        t.setDescription(rs.getString("description"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            t.setCreatedAt(ts.toString());
        }
        return t;
    }
}
