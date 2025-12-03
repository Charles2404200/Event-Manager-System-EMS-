package org.ems.infrastructure.db;

/**
 * @author <your group number>
 */

import org.ems.config.DatabaseConfig;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            System.out.println("✓ Database connection successful!");

            // Load schema.sql from resources
            try (InputStream in = DatabaseInitializer.class.getClassLoader()
                    .getResourceAsStream("db/schema.sql")) {
                if (in == null) {
                    System.out.println("⚠ schema.sql not found - skipping schema initialization");
                    return;
                }
                String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                try (Statement st = conn.createStatement()) {
                    st.execute(sql);
                }
            }
            System.out.println("✓ Database schema initialized successfully.");

            // Seed admin user
            DataSeeder.seedAdminUser();

        } catch (Exception e) {
            System.err.println("\n╔════════════════════════════════════════════════════╗");
            System.err.println("║  ❌ DATABASE CONNECTION FAILED                     ║");
            System.err.println("╠════════════════════════════════════════════════════╣");
            System.err.println("║  Error: " + e.getMessage());
            System.err.println("║                                                    ║");
            System.err.println("║  To fix:                                           ║");
            System.err.println("║  1. Check application.properties in               ║");
            System.err.println("║     src/main/resources/                           ║");
            System.err.println("║  2. Verify database URL, username, password       ║");
            System.err.println("║  3. Ensure database is running/accessible         ║");
            System.err.println("║                                                    ║");
            System.err.println("║  Continuing with UI in demo mode...               ║");
            System.err.println("╚════════════════════════════════════════════════════╝\n");

            // Don't crash - let app continue in demo mode
            // In production, you might want to throw here
        }
    }
}

