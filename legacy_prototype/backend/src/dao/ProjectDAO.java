package dao;

import model.Project;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectDAO {

    public List<Project> findAll() throws SQLException {
        List<Project> list = new ArrayList<>();
        String sql = "SELECT p.id, p.name, p.description, p.start_date, p.end_date, p.total_budget, p.current_spend, p.status, "
                   + "(SELECT COUNT(*) FROM milestones m WHERE m.project_id = p.id) as milestone_count "
                   + "FROM projects p ORDER BY p.id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Project p = mapRow(rs);
                p.setMilestoneCount(rs.getInt("milestone_count"));
                if (p.getTotalBudget() > 0) {
                    p.setBudgetUtilization((int) Math.round((p.getCurrentSpend() / p.getTotalBudget()) * 100));
                }
                list.add(p);
            }
        }
        return list;
    }

    public Project findById(int id) throws SQLException {
        String sql = "SELECT p.id, p.name, p.description, p.start_date, p.end_date, p.total_budget, p.current_spend, p.status, "
                   + "(SELECT COUNT(*) FROM milestones m WHERE m.project_id = p.id) as milestone_count "
                   + "FROM projects p WHERE p.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Project p = mapRow(rs);
                    p.setMilestoneCount(rs.getInt("milestone_count"));
                    if (p.getTotalBudget() > 0) {
                        p.setBudgetUtilization((int) Math.round((p.getCurrentSpend() / p.getTotalBudget()) * 100));
                    }
                    return p;
                }
            }
        }
        return null;
    }

    public int insert(Project p) throws SQLException {
        String sql = "INSERT INTO projects (name, description, start_date, end_date, total_budget, current_spend, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setDate(3, Date.valueOf(p.getStartDate()));
            ps.setDate(4, Date.valueOf(p.getEndDate()));
            ps.setDouble(5, p.getTotalBudget());
            ps.setDouble(6, p.getCurrentSpend());
            ps.setString(7, p.getStatus() != null ? p.getStatus() : "PLANNED");

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int pId = rs.getInt(1);
                    // Also create budget record
                    String bSql = "INSERT INTO budgets (project_id, total_budget, current_spend) VALUES (?, ?, ?)";
                    try (PreparedStatement bPs = conn.prepareStatement(bSql)) {
                        bPs.setInt(1, pId);
                        bPs.setDouble(2, p.getTotalBudget());
                        bPs.setDouble(3, p.getCurrentSpend());
                        bPs.executeUpdate();
                    }
                    return pId;
                }
            }
        }
        return 0;
    }

    public boolean update(Project p) throws SQLException {
        String sql = "UPDATE projects SET name = ?, description = ?, start_date = ?, end_date = ?, "
                   + "total_budget = ?, current_spend = ?, status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setDate(3, Date.valueOf(p.getStartDate()));
            ps.setDate(4, Date.valueOf(p.getEndDate()));
            ps.setDouble(5, p.getTotalBudget());
            ps.setDouble(6, p.getCurrentSpend());
            ps.setString(7, p.getStatus());
            ps.setInt(8, p.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM projects WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public void updateSpend(int projectId, double newSpend) throws SQLException {
        Project existing = findById(projectId);
        double oldSpend = existing != null ? existing.getCurrentSpend() : 0.0;
        
        String sql = "UPDATE projects SET current_spend = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newSpend);
            ps.setInt(2, projectId);
            ps.executeUpdate();
        }
        String bSql = "UPDATE budgets SET current_spend = ? WHERE project_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(bSql)) {
            ps.setDouble(1, newSpend);
            ps.setInt(2, projectId);
            ps.executeUpdate();
        }

        if (newSpend > oldSpend) {
            double delta = newSpend - oldSpend;
            String tSql = "INSERT INTO budget_transactions (project_id, amount, transaction_date, description) VALUES (?, ?, CURRENT_DATE, ?)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(tSql)) {
                ps.setInt(1, projectId);
                ps.setDouble(2, delta);
                ps.setString(3, "Spend adjustment logged");
                ps.executeUpdate();
            }
        }
    }

    public void addSpend(int projectId, double amount, String description, String transactionDate) throws SQLException {
        if (amount <= 0) return;
        String tDate = (transactionDate != null && !transactionDate.isEmpty()) ? transactionDate : java.time.LocalDate.now().toString();
        String desc = (description != null && !description.isEmpty()) ? description : "Direct spend logged";

        String tSql = "INSERT INTO budget_transactions (project_id, amount, transaction_date, description) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(tSql)) {
            ps.setInt(1, projectId);
            ps.setDouble(2, amount);
            ps.setString(3, tDate);
            ps.setString(4, desc);
            ps.executeUpdate();
        }

        String sql = "UPDATE projects SET current_spend = current_spend + ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, projectId);
            ps.executeUpdate();
        }
        String bSql = "UPDATE budgets SET current_spend = current_spend + ? WHERE project_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(bSql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, projectId);
            ps.executeUpdate();
        }
    }

    private Project mapRow(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setStartDate(rs.getDate("start_date").toString());
        p.setEndDate(rs.getDate("end_date").toString());
        p.setTotalBudget(rs.getDouble("total_budget"));
        p.setCurrentSpend(rs.getDouble("current_spend"));
        p.setStatus(rs.getString("status"));
        return p;
    }
}
