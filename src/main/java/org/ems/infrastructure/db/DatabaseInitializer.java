package org.ems.infrastructure.db;

/**
 * @author <your group number>
 */

import org.ems.config.DatabaseConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseInitializer {

    public static void initialize() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Choose schema file based on database type
            String schemaFile = DatabaseConfig.isUsingH2() ? "db/schema-h2.sql" : "db/schema.sql";
            
            // Load schema.sql from resources
            try (InputStream in = DatabaseInitializer.class.getClassLoader()
                    .getResourceAsStream(schemaFile)) {
                if (in == null) {
                    throw new RuntimeException(" Schema file not found at /resources/" + schemaFile);
                }
                String sql = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));
                
                // Execute each statement separately for compatibility
                try (Statement st = conn.createStatement()) {
                    for (String statement : sql.split(";")) {
                        // Remove comment lines and clean up
                        String cleaned = statement.lines()
                                .filter(line -> !line.trim().startsWith("--"))
                                .collect(Collectors.joining("\n"))
                                .trim();
                        
                        // Skip if nothing substantial left after removing comments
                        if (!cleaned.isEmpty()) {
                            st.execute(cleaned);
                        }
                    }
                }
            }
            System.out.println(" Database schema initialized successfully.");

            // Seed admin user and sample data
            DataSeeder.seedAdminUser();
            DataSeeder.seedSampleData();

        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                System.out.println("⚠ Database tables already exist, skipping initialization.");
                DataSeeder.seedAdminUser();
                DataSeeder.seedSampleData();
            } else {
                // Log the error but don't crash the application
                System.err.println(" Error initializing database: " + e.getMessage());
                System.err.println("️ Application will run in offline mode");
            }
        }
    }
}
