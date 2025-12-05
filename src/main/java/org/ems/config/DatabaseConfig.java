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

        // NEON Database Configuration
        // Convert postgresql:// URL to jdbc:postgresql:// for JDBC
        String rawUrl = props.getProperty("db.url", "postgresql://neondb_owner:npg_R2ZHtbvASw7T@ep-mute-pond-a11bdd5p-pooler.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require");

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
            USER = props.getProperty("db.username", "neondb_owner");
            PASSWORD = props.getProperty("db.password", "npg_R2ZHtbvASw7T");
        }

        System.out.println("✓ Database Config Loaded (NEON):");
        System.out.println("   JDBC URL: " + URL);
        System.out.println("   User: " + USER);
        System.out.println("   Region: ap-southeast-1 (AWS Singapore)");

        // Initialize HikariCP connection pool
        initializeHikariPool();
    }

    /**
     * Initialize HikariCP connection pool with optimized settings for NEON
     */
    private static void initializeHikariPool() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(URL);
            config.setUsername(USER);
            config.setPassword(PASSWORD);
            config.setDriverClassName("org.postgresql.Driver");

            // Connection pool settings for optimal performance with NEON
            config.setMaximumPoolSize(10);           // Maximum connections in pool
            config.setMinimumIdle(2);                // Minimum idle connections
            config.setConnectionTimeout(30000);      // 30 seconds timeout
            config.setIdleTimeout(600000);           // 10 minutes idle timeout
            config.setMaxLifetime(1800000);          // 30 minutes max lifetime
            config.setAutoCommit(true);              // Auto-commit enabled
            config.setPoolName("EventManagerNeonPool");  // Pool name for monitoring

            // NEON-specific properties
            config.addDataSourceProperty("sslmode", "require");           // Force SSL/TLS
            config.addDataSourceProperty("channel_binding", "require");   // Channel binding for security
            config.addDataSourceProperty("ssl", "true");                  // Enable SSL

            dataSource = new HikariDataSource(config);
            System.out.println("✓ HikariCP Connection Pool initialized for NEON:");
            System.out.println("   Max Pool Size: " + config.getMaximumPoolSize());
            System.out.println("   Min Idle: " + config.getMinimumIdle());
            System.out.println("   SSL Mode: require");
            System.out.println("   Channel Binding: require");
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize HikariCP: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize connection pool", e);
        }
    }

    /**
     * Get connection from HikariCP pool
     * @return Connection from NEON database
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        try {
            if (dataSource == null) {
                throw new SQLException("HikariDataSource not initialized");
            }
            Connection conn = dataSource.getConnection();
            System.out.println("✓ Got connection from NEON pool");
            return conn;
        } catch (SQLException e) {
            System.err.println("✗ Failed to get connection from NEON pool: " + e.getMessage());
            System.err.println("   Please verify NEON database connection string in application.properties");
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
            return String.format("✓ NEON Pool Stats - Active: %d, Idle: %d, Waiting: %d",
                    dataSource.getHikariPoolMXBean().getActiveConnections(),
                    dataSource.getHikariPoolMXBean().getIdleConnections(),
                    dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        }
        return "✗ Pool not initialized";
    }
}
