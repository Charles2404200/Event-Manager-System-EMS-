package org.ems.infrastructure.db;

import org.ems.config.DatabaseConfig;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for database initialization with H2 fallback
 */
class DatabaseInitializerTest {

    @Test
    void testH2DatabaseConnection() throws SQLException {
        // Given H2 is the default database (no environment variables set)
        assertTrue(DatabaseConfig.isUsingH2(), "Should be using H2 database");
        
        // When getting a connection
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Then connection should be valid
            assertNotNull(conn);
            assertFalse(conn.isClosed());
        }
    }

    @Test
    void testDatabaseInitializationCreatesSchema() throws SQLException {
        // When initializing the database
        DatabaseInitializer.initialize();
        
        // Then core tables should exist
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Check persons table exists
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM persons")) {
                assertTrue(rs.next(), "Should be able to query persons table");
            }
            
            // Check events table exists
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM events")) {
                assertTrue(rs.next(), "Should be able to query events table");
            }
            
            // Check sessions table exists
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM sessions")) {
                assertTrue(rs.next(), "Should be able to query sessions table");
            }
        }
    }

    @Test
    void testAdminUserSeeded() throws SQLException {
        // Given database is initialized
        DatabaseInitializer.initialize();
        
        // When checking for admin user
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT username, role FROM persons WHERE username = 'admin'")) {
            
            // Then admin user should exist
            assertTrue(rs.next(), "Admin user should exist");
            assertEquals("admin", rs.getString("username"));
            assertEquals("SYSTEM_ADMIN", rs.getString("role"));
        }
    }
}
