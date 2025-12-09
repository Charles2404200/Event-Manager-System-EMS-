package org.ems.infrastructure.repository.jdbc;

import org.ems.domain.model.enums.EventStatus;
import org.ems.domain.model.enums.EventType;
import org.ems.domain.model.Event;
import org.ems.domain.repository.EventRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class JdbcEventRepository implements EventRepository {

    private final Connection conn;
    private final DataSource dataSource;  // ADDED: For getting fresh connections

    public JdbcEventRepository(Connection conn) {
        this.conn = conn;
        this.dataSource = null;  // Legacy: single connection mode
    }

    public JdbcEventRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        this.conn = null;  // DataSource mode
    }

    /**
     * Get a database connection - either from pool or reuse existing
     */
    private Connection getConnection() throws SQLException {
        if (dataSource != null) {
            return dataSource.getConnection();  // Fresh connection from pool
        }
        return conn;  // Fallback to injected connection
    }

    /**
     * Close connection only if from dataSource pool
     */
    private void closeConnection(Connection connection) {
        if (dataSource != null && connection != null) {
            try {
                connection.close();  // Return to pool
            } catch (SQLException e) {
                // Ignore
            }
        }
    }

    // ---------------------------------------------------------
    // SAVE (INSERT OR UPDATE)
    // ---------------------------------------------------------
    @Override
    public Event save(Event e) {

        String sql = """
            INSERT INTO events 
            (id, name, type, location, start_date, end_date, status, image_path)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                type = EXCLUDED.type,
                location = EXCLUDED.location,
                start_date = EXCLUDED.start_date,
                end_date = EXCLUDED.end_date,
                status = EXCLUDED.status,
                image_path = EXCLUDED.image_path
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, e.getId());
            ps.setString(2, e.getName());
            ps.setString(3, e.getType().name());
            ps.setString(4, e.getLocation());
            ps.setObject(5, e.getStartDate());
            ps.setObject(6, e.getEndDate());
            ps.setString(7, e.getStatus().name());
            ps.setString(8, e.getImagePath());

            ps.executeUpdate();

            return e;

        } catch (SQLException ex) {
            throw new RuntimeException("Failed to save event", ex);
        }
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------
    @Override
    public void delete(UUID id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM events WHERE id = ?"
        )) {
            ps.setObject(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------
    // FIND BY ID
    // ---------------------------------------------------------
    @Override
    public Event findById(UUID id) {
        String sql = "SELECT * FROM events WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            Event e = mapRow(rs);

            // Load sessions belonging to this event
            loadSessionIds(e);

            return e;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------
    // FIND ALL (with session IDs)
    // ---------------------------------------------------------
    @Override
    public List<Event> findAll() {
        List<Event> list = new ArrayList<>();
        Connection connection = null;

        try {
            connection = getConnection();

            // Step 1: Load all events
            long eventStart = System.currentTimeMillis();
            List<Event> events = new ArrayList<>();
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM events")) {
                while (rs.next()) {
                    events.add(mapRow(rs));
                }
            }
            long eventTime = System.currentTimeMillis() - eventStart;
            System.out.println("    ✓ Loaded " + events.size() + " events in " + eventTime + " ms");

            if (events.isEmpty()) {
                return events;
            }

            // Step 2: Load ALL session IDs in ONE batch query (not N queries!)
            long sessionStart = System.currentTimeMillis();
            Map<java.util.UUID, List<java.util.UUID>> sessionMap = new HashMap<>();
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery("SELECT event_id, id FROM sessions")) {
                while (rs.next()) {
                    java.util.UUID eventId = (java.util.UUID) rs.getObject("event_id");
                    java.util.UUID sessionId = (java.util.UUID) rs.getObject("id");
                    sessionMap.computeIfAbsent(eventId, k -> new ArrayList<>()).add(sessionId);
                }
            }
            long sessionTime = System.currentTimeMillis() - sessionStart;
            System.out.println("    ✓ Batch loaded all sessions in " + sessionTime + " ms");

            // Step 3: Assign session IDs to events (in-memory, no DB queries)
            long assignStart = System.currentTimeMillis();
            for (Event event : events) {
                List<java.util.UUID> sessionIds = sessionMap.getOrDefault(event.getId(), new ArrayList<>());
                event.getSessionIds().addAll(sessionIds);
            }
            long assignTime = System.currentTimeMillis() - assignStart;
            System.out.println("    ✓ Assigned sessions to events in " + assignTime + " ms");

            list.addAll(events);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeConnection(connection);
        }

        return list;
    }


    // ---------------------------------------------------------
    // FIND BY TYPE
    // ---------------------------------------------------------
    @Override
    public List<Event> findByType(EventType type) {
        List<Event> list = new ArrayList<>();

        String sql = "SELECT * FROM events WHERE type=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Event e = mapRow(rs);
                loadSessionIds(e);
                list.add(e);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // ---------------------------------------------------------
    // FIND BY STATUS
    // ---------------------------------------------------------
    public List<Event> findByStatus(EventStatus status) {
        List<Event> list = new ArrayList<>();

        String sql = "SELECT * FROM events WHERE status=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Event e = mapRow(rs);
                loadSessionIds(e);
                list.add(e);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // ---------------------------------------------------------
    // FIND BY DATE
    // ---------------------------------------------------------
    public List<Event> findByDate(LocalDate date) {

        List<Event> list = new ArrayList<>();

        String sql = """
            SELECT * FROM events 
            WHERE start_date <= ? AND end_date >= ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, date);
            ps.setObject(2, date);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Event e = mapRow(rs);
                loadSessionIds(e);
                list.add(e);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // ---------------------------------------------------------
    // COUNT METHODS
    // ---------------------------------------------------------
    /**
     * Returns total number of events using SELECT COUNT(*).
     * FIXED: Get fresh connection to avoid "Connection is closed" error in async operations
     */
    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM events";
        Connection connection = null;
        try {
            connection = getConnection();
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0L;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count events", e);
        } finally {
            closeConnection(connection);
        }
    }

    /**
     * Returns number of active events (SCHEDULED or ONGOING).
     */
    public long countActiveEvents() {
        String sql = "SELECT COUNT(*) FROM events WHERE status IN ('SCHEDULED','ONGOING')";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count active events", e);
        }
    }

    // ---------------------------------------------------------
    // PAGINATION
    // ---------------------------------------------------------
    /**
     * Returns a paginated list of events.
     * FIXED: Use getConnection() for fresh connection from pool
     */
    public List<Event> findPage(int offset, int limit) {
        List<Event> list = new ArrayList<>();
        String sql = "SELECT * FROM events ORDER BY start_date DESC NULLS LAST, id DESC LIMIT ? OFFSET ?";

        Connection connection = null;
        try {
            connection = getConnection();
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, limit);
                ps.setInt(2, offset);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to page events", e);
        } finally {
            closeConnection(connection);
        }

        return list;
    }

    // ---------------------------------------------------------
    // MAPPER
    // ---------------------------------------------------------
    private Event mapRow(ResultSet rs) throws SQLException {

        Event e = new Event();

        e.setId((UUID) rs.getObject("id"));
        e.setName(rs.getString("name"));
        e.setType(EventType.valueOf(rs.getString("type")));
        e.setLocation(rs.getString("location"));
        e.setStartDate(rs.getObject("start_date", LocalDate.class));
        e.setEndDate(rs.getObject("end_date", LocalDate.class));
        e.setStatus(EventStatus.valueOf(rs.getString("status")));
        e.setImagePath(rs.getString("image_path"));

        return e;
    }

    // ---------------------------------------------------------
    // LOAD sessionIds from "sessions" table
    // ---------------------------------------------------------
    private void loadSessionIds(Event e) throws SQLException {
        Connection connection = null;
        try {
            connection = getConnection();
            loadSessionIds(e, connection);
        } finally {
            closeConnection(connection);
        }
    }

    /**
     * Load session IDs using provided connection (to reuse same connection)
     */
    private void loadSessionIds(Event e, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM sessions WHERE event_id=?"
        )) {
            ps.setObject(1, e.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                UUID sessionId = (UUID) rs.getObject("id");
                e.getSessionIds().add(sessionId);
            }
        }
    }
}

