package org.ems.infrastructure.db;

import org.ems.infrastructure.config.DatabaseConfig;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Database initialization class
 * Handles schema creation and optional data seeding
 *
 * @author <your group number>
 */
public class DatabaseInitializer {
    // Flag to control whether to seed data on initialization
    private static final boolean AUTO_SEED_DATA = false;  // Set to true to auto-seed on startup

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

                // Split by semicolon and execute each statement separately
                String[] statements = sql.split(";");
                int successCount = 0;

                try (Statement st = conn.createStatement()) {
                    for (String statement : statements) {
                        String trimmed = statement.trim();
                        if (trimmed.isEmpty()) {
                            continue;
                        }
                        try {
                            st.execute(trimmed);
                            successCount++;
                        } catch (Exception e) {
                            // Log but continue - some statements might fail (e.g., IF NOT EXISTS that already exist)
                            System.out.println("  ⚠ Statement skipped: " + trimmed.substring(0, Math.min(50, trimmed.length())) + "... (" + e.getMessage() + ")");
                        }
                    }
                }
                System.out.println("✓ Database schema initialized successfully (" + successCount + " statements executed).");
            }

            // Seed admin user (always seed to ensure admin exists)
            DataSeeder.seedAdminUser();

            // Seed sample data only if AUTO_SEED_DATA is enabled
            if (AUTO_SEED_DATA) {
                System.out.println("\n✓ Auto-seeding data (AUTO_SEED_DATA = true)");
                DataSeeder.seedSampleData();
            } else {
                System.out.println("\n ℹ Data seeding disabled (AUTO_SEED_DATA = false)");
                System.out.println("   To seed data manually, call: DataSeeder.seedSampleData()");
            }

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

    /**
     * Manually seed sample data to database
     * Call this to populate database with test data
     */
    public static void seedSampleData() {
        DataSeeder.seedSampleData();
    }

    /**
     * Check if should auto-seed data on startup
     */
    public static boolean isAutoSeedEnabled() {
        return AUTO_SEED_DATA;
    }
}
