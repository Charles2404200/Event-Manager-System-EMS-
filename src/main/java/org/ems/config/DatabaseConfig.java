package org.ems.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    // Database configuration from environment variables with fallback defaults
    private static final String DB_URL = getEnvOrDefault("DB_URL", 
            "jdbc:h2:mem:emsdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
    private static final String DB_USER = getEnvOrDefault("DB_USER", "sa");
    private static final String DB_PASSWORD = getEnvOrDefault("DB_PASSWORD", "");
    
    private static final boolean USE_H2 = DB_URL.startsWith("jdbc:h2:");

    static {
        loadDrivers();
    }
    
    private static void loadDrivers() {
        // Load H2 driver
        try {
            Class.forName("org.h2.Driver");
            System.out.println(" H2 driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println(" H2 driver not found: " + e.getMessage());
        }
        
        // Load PostgreSQL driver
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println(" PostgreSQL driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println(" PostgreSQL driver not found: " + e.getMessage());
        }
    }
    
    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            if (USE_H2) {
                System.out.println(" Connected to H2 in-memory database (development mode)");
            } else {
                System.out.println(" Connected to PostgreSQL database");
            }
            return conn;
        } catch (SQLException e) {
            System.err.println(" Failed to connect to database: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Returns true if using H2 embedded database (development mode)
     */
    public static boolean isUsingH2() {
        return USE_H2;
    }
}
