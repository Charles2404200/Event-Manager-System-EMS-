package org.ems.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final String URL =
            "jdbc:postgresql://db.ntygvfgxymvdyxezdpvv.supabase.co:5432/postgres";

    private static final String USER = "postgres";
    private static final String PASSWORD = "Pricepc24_04";

    static {
        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");
            System.out.println(" PostgreSQL driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println(" PostgreSQL driver not found: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println(" Connected to PostgreSQL database");
            return conn;
        } catch (SQLException e) {
            System.err.println(" Failed to connect to database: " + e.getMessage());
            throw e;
        }
    }
}
