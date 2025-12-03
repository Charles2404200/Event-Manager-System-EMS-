package org.ems.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;

/**
 * @author <your group number>
 */
public class DatabaseConfig {

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");
            System.out.println(" PostgreSQL driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println(" PostgreSQL driver not found: " + e.getMessage());
        }

        // Load properties from application.properties
        Properties props = new Properties();
        try (InputStream input = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                System.err.println("⚠️  application.properties not found!");
                throw new RuntimeException("application.properties file not found in resources/");
            }
            props.load(input);
        } catch (Exception e) {
            System.err.println(" Error loading application.properties: " + e.getMessage());
            throw new RuntimeException("Failed to load database configuration", e);
        }

        // Get values from properties
        URL = props.getProperty("db.url", "jdbc:postgresql://localhost:5432/event_manager");
        USER = props.getProperty("db.username", "postgres");
        PASSWORD = props.getProperty("db.password", "postgres");

        System.out.println(" Database Config Loaded:");
        System.out.println("   URL: " + URL);
        System.out.println("   User: " + USER);
    }

    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println(" Connected to PostgreSQL database");
            return conn;
        } catch (SQLException e) {
            System.err.println(" Failed to connect to database: " + e.getMessage());
            System.err.println(" Please verify your database configuration in src/main/resources/application.properties");
            throw e;
        }
    }
}
