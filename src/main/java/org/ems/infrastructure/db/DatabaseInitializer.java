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
            // Load schema.sql from resources
            try (InputStream in = DatabaseInitializer.class.getClassLoader()
                    .getResourceAsStream("db/schema.sql")) {
                if (in == null) {
                    throw new RuntimeException(" schema.sql not found at /resources/db/schema.sql");
                }
                String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                try (Statement st = conn.createStatement()) {
                    st.execute(sql);
                }
            }
            System.out.println(" Database schema initialized successfully.");

            // Seed admin user only (no sample data)
            DataSeeder.seedAdminUser();
            // DataSeeder.seedSampleData(); // DISABLED - no more automatic data seeding

        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                System.out.println("⚠ Database tables already exist, skipping initialization.");
                DataSeeder.seedAdminUser();
                // DataSeeder.seedSampleData(); // DISABLED - no more automatic data seeding
            } else {
                throw new RuntimeException(" Error initializing database: " + e.getMessage(), e);
            }
        }
    }
}
