package org.ems.infrastructure.repository.jdbc;

import org.ems.domain.model.ActivityLog;
import org.ems.domain.repository.ActivityLogRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JdbcActivityLogRepository implements ActivityLogRepository {

    private final Connection connection;

    public JdbcActivityLogRepository(Connection connection) {
        this.connection = connection;
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS activity_logs (
                id VARCHAR(36) PRIMARY KEY,
                user_id VARCHAR(50),
                action VARCHAR(50),
                resource VARCHAR(100),
                description VARCHAR(255),
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                ip_address VARCHAR(45),
                user_agent VARCHAR(255)
            )
        """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("[ActivityLogRepository] activity_logs table created/verified");
        } catch (SQLException e) {
            System.err.println("Error creating activity_logs table: " + e.getMessage());
        }
    }


    @Override
    public void save(ActivityLog log) {
        String sql = """
            INSERT INTO activity_logs (id, user_id, action, resource, description, timestamp, ip_address, user_agent)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, log.getId().toString());
            pstmt.setString(2, log.getUserId());
            pstmt.setString(3, log.getAction());
            pstmt.setString(4, log.getResource());
            pstmt.setString(5, log.getDescription());
            pstmt.setTimestamp(6, Timestamp.valueOf(log.getTimestamp()));
            pstmt.setString(7, log.getIpAddress());
            pstmt.setString(8, log.getUserAgent());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving activity log: " + e.getMessage());
        }
    }

    @Override
    public ActivityLog findById(UUID id) {
        String sql = "SELECT * FROM activity_logs WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToActivityLog(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding activity log: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<ActivityLog> findAll() {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM activity_logs ORDER BY timestamp DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(mapResultSetToActivityLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all activity logs: " + e.getMessage());
        }
        return logs;
    }

    @Override
    public List<ActivityLog> findPage(int offset, int pageSize) {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, pageSize);
            pstmt.setInt(2, offset);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToActivityLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding activity logs page: " + e.getMessage());
        }
        return logs;
    }

    @Override
    public List<ActivityLog> findByAction(String action) {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM activity_logs WHERE action = ? ORDER BY timestamp DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, action);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToActivityLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding activity logs by action: " + e.getMessage());
        }
        return logs;
    }

    @Override
    public List<ActivityLog> findByUserId(String userId) {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM activity_logs WHERE user_id = ? ORDER BY timestamp DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToActivityLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding activity logs by user: " + e.getMessage());
        }
        return logs;
    }

    @Override
    public List<ActivityLog> findByResource(String resource) {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM activity_logs WHERE resource = ? ORDER BY timestamp DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, resource);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToActivityLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding activity logs by resource: " + e.getMessage());
        }
        return logs;
    }

    @Override
    public List<ActivityLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM activity_logs WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(startDate));
            pstmt.setTimestamp(2, Timestamp.valueOf(endDate));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToActivityLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding activity logs by date range: " + e.getMessage());
        }
        return logs;
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM activity_logs";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting activity logs: " + e.getMessage());
        }
        return 0L;
    }

    @Override
    public void deleteById(UUID id) {
        String sql = "DELETE FROM activity_logs WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting activity log: " + e.getMessage());
        }
    }

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM activity_logs";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Error deleting all activity logs: " + e.getMessage());
        }
    }

    @Override
    public void deleteByDateBefore(LocalDateTime date) {
        String sql = "DELETE FROM activity_logs WHERE timestamp < ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(date));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting activity logs before date: " + e.getMessage());
        }
    }

    private ActivityLog mapResultSetToActivityLog(ResultSet rs) throws SQLException {
        ActivityLog log = new ActivityLog();
        log.setId(UUID.fromString(rs.getString("id")));
        log.setUserId(rs.getString("user_id"));
        log.setAction(rs.getString("action"));
        log.setResource(rs.getString("resource"));
        log.setDescription(rs.getString("description"));
        log.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        log.setIpAddress(rs.getString("ip_address"));
        log.setUserAgent(rs.getString("user_agent"));
        return log;
    }
}

