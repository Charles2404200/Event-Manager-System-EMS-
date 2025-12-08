package org.ems.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;

/**
 * @author <your group number>
 *
 * Database configuration with HikariCP connection pooling for optimized performance
 */
public class DatabaseConfig {

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;
    private static HikariDataSource dataSource;

    static {
        try {
            // Load PostgreSQL driver FIRST
            Class.forName("org.postgresql.Driver");
            System.out.println("✓ PostgreSQL driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("✗ PostgreSQL driver not found: " + e.getMessage());
            throw new RuntimeException("PostgreSQL driver not found in classpath", e);
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
            System.err.println("✗ Error loading application.properties: " + e.getMessage());
            throw new RuntimeException("Failed to load database configuration", e);
        }

        // Supabase Database Configuration
        // Convert postgresql:// URL to jdbc:postgresql:// for JDBC
        String rawUrl = props.getProperty("db.url", "postgresql://postgres:Pricepc24_04@db.ntygvfgxymvdyxezdpvv.supabase.co:5432/postgres");

        // Convert format: postgresql://user:pass@host/db → jdbc:postgresql://host/db
        if (rawUrl.startsWith("postgresql://")) {
            // Parse the URL
            String urlWithoutProtocol = rawUrl.substring("postgresql://".length());
            int atIndex = urlWithoutProtocol.lastIndexOf("@");
            String credentials = urlWithoutProtocol.substring(0, atIndex);
            String hostAndDb = urlWithoutProtocol.substring(atIndex + 1);

            String[] credParts = credentials.split(":");
            USER = credParts[0];
            PASSWORD = credParts.length > 1 ? credParts[1] : "";

            // Build JDBC URL
            URL = "jdbc:postgresql://" + hostAndDb;
        } else {
            // Already JDBC format
            URL = rawUrl;
            USER = props.getProperty("db.username", "postgres");
            PASSWORD = props.getProperty("db.password", "Pricepc24_04");
        }

        System.out.println("✓ Database Config Loaded (SUPABASE):");
        System.out.println("   JDBC URL: " + URL);
        System.out.println("   User: " + USER);

        // Initialize HikariCP connection pool
        initializeHikariPool();
    }

    /**
     * Initialize HikariCP connection pool with optimized settings for Supabase
     */
    private static void initializeHikariPool() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(URL);
            config.setUsername(USER);
            config.setPassword(PASSWORD);
            config.setDriverClassName("org.postgresql.Driver");

            // Connection pool settings for optimal performance with Supabase
            config.setMaximumPoolSize(10);           // Maximum connections in pool
            config.setMinimumIdle(2);                // Minimum idle connections
            config.setConnectionTimeout(30000);      // 30 seconds timeout
            config.setIdleTimeout(600000);           // 10 minutes idle timeout
            config.setMaxLifetime(1800000);          // 30 minutes max lifetime
            config.setAutoCommit(true);              // Auto-commit enabled
            config.setPoolName("EventManagerSupabasePool");  // Pool name for monitoring

            // Supabase-specific properties
            config.addDataSourceProperty("sslmode", "require");           // Force SSL/TLS
            config.addDataSourceProperty("ssl", "true");                  // Enable SSL

            dataSource = new HikariDataSource(config);
            System.out.println("✓ HikariCP Connection Pool initialized for SUPABASE:");
            System.out.println("   Max Pool Size: " + config.getMaximumPoolSize());
            System.out.println("   Min Idle: " + config.getMinimumIdle());
            System.out.println("   SSL Mode: require");
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize HikariCP: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize connection pool", e);
        }
    }

    /**
     * Get connection from HikariCP pool
     * @return Connection from Supabase database
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        try {
            if (dataSource == null) {
                throw new SQLException("HikariDataSource not initialized");
            }
            Connection conn = dataSource.getConnection();
            System.out.println("✓ Got connection from Supabase pool");
            return conn;
        } catch (SQLException e) {
            System.err.println("✗ Failed to get connection from Supabase pool: " + e.getMessage());
            System.err.println("   Please verify Supabase database connection string in application.properties");
            System.err.println("   Connection URL: " + URL);
            throw e;
        }
    }

    /**
     * Close the connection pool (call on application shutdown)
     */
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("✓ HikariCP Connection Pool closed");
        }
    }

    /**
     * Get current pool statistics for monitoring
     * @return Pool statistics string
     */
    public static String getPoolStats() {
        if (dataSource != null) {
            return String.format("✓ Supabase Pool Stats - Active: %d, Idle: %d, Waiting: %d",
                    dataSource.getHikariPoolMXBean().getActiveConnections(),
                    dataSource.getHikariPoolMXBean().getIdleConnections(),
                    dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        }
        return "✗ Pool not initialized";
    }
}
