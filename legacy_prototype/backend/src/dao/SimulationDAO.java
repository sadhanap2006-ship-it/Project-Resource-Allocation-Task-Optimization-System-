package dao;

import model.WhatIfScenario;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SimulationDAO {

    public int insert(WhatIfScenario s) throws SQLException {
        String sql = "INSERT INTO what_if_scenarios (milestone_id, current_developer_id, proposed_developer_id, "
                   + "allocation_percentage, cost_difference, delivery_days_delta, scenario_verdict, recommendation, notes) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getMilestoneId());
            ps.setInt(2, s.getCurrentDeveloperId());
            ps.setInt(3, s.getProposedDeveloperId());
            ps.setInt(4, s.getAllocationPercentage());
            ps.setDouble(5, s.getCostDifferencePerHour());
            ps.setInt(6, s.getDeliveryDaysDelta());
            ps.setString(7, s.getScenarioVerdict());
            ps.setString(8, s.getRecommendation());
            ps.setString(9, s.getNotes());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}
