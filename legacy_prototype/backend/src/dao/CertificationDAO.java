package dao;

import model.Certification;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CertificationDAO {

    public List<Certification> findByDeveloperId(int developerId) throws SQLException {
        String sql = "SELECT id, developer_id, name, issuing_body, issue_date, credential_id, " +
                     "file_name, confidence_score, verified, created_at " +
                     "FROM certifications WHERE developer_id = ? ORDER BY issue_date DESC";

        List<Certification> list = new ArrayList<>();
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

    public Certification findById(int id) throws SQLException {
        String sql = "SELECT id, developer_id, name, issuing_body, issue_date, credential_id, " +
                     "file_name, confidence_score, verified, created_at FROM certifications WHERE id = ?";
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

    public int insert(Certification cert) throws SQLException {
        String sql = "INSERT INTO certifications (developer_id, name, issuing_body, issue_date, " +
                     "credential_id, file_name, file_data, confidence_score, verified) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, cert.getDeveloperId());
            ps.setString(2, cert.getName());
            ps.setString(3, cert.getIssuingBody());
            ps.setDate(4, Date.valueOf(cert.getIssueDate()));
            ps.setString(5, cert.getCredentialId());
            ps.setString(6, cert.getFileName());
            ps.setString(7, cert.getFileData());
            ps.setDouble(8, cert.getConfidenceScore() > 0 ? cert.getConfidenceScore() : 0.95);
            ps.setBoolean(9, cert.isVerified());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private Certification mapRow(ResultSet rs) throws SQLException {
        Certification c = new Certification();
        c.setId(rs.getInt("id"));
        c.setDeveloperId(rs.getInt("developer_id"));
        c.setName(rs.getString("name"));
        c.setIssuingBody(rs.getString("issuing_body"));
        Date d = rs.getDate("issue_date");
        if (d != null) c.setIssueDate(d.toString());
        c.setCredentialId(rs.getString("credential_id"));
        c.setFileName(rs.getString("file_name"));
        c.setConfidenceScore(rs.getDouble("confidence_score"));
        c.setVerified(rs.getBoolean("verified"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        return c;
    }
}
