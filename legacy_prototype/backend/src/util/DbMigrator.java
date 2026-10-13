package util;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;

public class DbMigrator {
    public static void main(String[] args) {
        System.out.println("Starting Database Migration...");
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Schema Tables
            runScript(stmt, new File("database/schema.sql"));

            // 2. Sample Data
            runScript(stmt, new File("database/sample_data.sql"));

            System.out.println("[SUCCESS] Database schema and sample data migrated successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runScript(Statement stmt, File file) throws Exception {
        if (!file.exists()) {
            System.err.println("File not found: " + file.getAbsolutePath());
            return;
        }
        System.out.println("Executing script: " + file.getName());
        String content = Files.readString(file.toPath());

        // Strip single line comments (-- ...)
        String noLineComments = content.replaceAll("(?m)^\\s*--.*$", "");
        // Strip block comments (/* ... */)
        String cleaned = noLineComments.replaceAll("/\\*.*?\\*/", "");

        // Split by semicolon
        String[] statements = cleaned.split(";");
        for (String raw : statements) {
            String sql = raw.trim();
            if (sql.isEmpty() || sql.equalsIgnoreCase("USE resource_allocation")) {
                continue;
            }
            try {
                stmt.execute(sql);
            } catch (Exception ex) {
                // Table already exists or duplicate key is fine
                String msg = ex.getMessage();
                if (!msg.contains("already exists") && !msg.contains("Duplicate entry")) {
                    System.out.println("  [Notice] " + msg);
                }
            }
        }
    }
}
