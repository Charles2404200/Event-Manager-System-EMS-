package org.ems.infrastructure.repository.jdbc;

import org.ems.domain.model.Attendee;
import org.ems.domain.model.enums.Role;
import org.ems.domain.repository.AttendeeRepository;

import java.sql.*;
import java.util.*;

public class JdbcAttendeeRepository implements AttendeeRepository {

    private final Connection conn;

    public JdbcAttendeeRepository(Connection conn) {
        this.conn = conn;
    }

    @Override
    public Attendee save(Attendee a) {

        try {
            //  save to persons table
            String personSql = """
                INSERT INTO persons
                (id, full_name, dob, email, phone, username, password_hash, role, bio)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                  full_name = EXCLUDED.full_name,
                  dob = EXCLUDED.dob,
                  email = EXCLUDED.email,
                  phone = EXCLUDED.phone,
                  username = EXCLUDED.username,
                  password_hash = EXCLUDED.password_hash,
                  role = EXCLUDED.role,
                  bio = EXCLUDED.bio
            """;

            try (PreparedStatement ps = conn.prepareStatement(personSql)) {
                ps.setObject(1, a.getId());
                ps.setString(2, a.getFullName());
                ps.setObject(3, a.getDateOfBirth());
                ps.setString(4, a.getEmail());
                ps.setString(5, a.getPhone());
                ps.setString(6, a.getUsername());
                ps.setString(7, a.getPasswordHash());
                ps.setString(8, Role.ATTENDEE.name());
                ps.setString(9, null);
                ps.executeUpdate();
            }

            // save to attendees table
            String attendeeSql = """
                INSERT INTO attendees (id, activity_history)
                VALUES (?, ?)
                ON CONFLICT (id) DO UPDATE SET
                  activity_history = EXCLUDED.activity_history
            """;

            try (PreparedStatement ps = conn.prepareStatement(attendeeSql)) {
                ps.setObject(1, a.getId());
                ps.setString(2, "[]"); // Empty activity history
                ps.executeUpdate();
            }

            return a;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save attendee", e);
        }
    }

    @Override
    public void delete(UUID id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM attendees WHERE id = ?"
        )) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Attendee findById(UUID id) {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                SELECT p.*, a.activity_history FROM persons p
                JOIN attendees a ON p.id = a.id
                WHERE p.id = ?
                """
        )) {
            ps.setObject(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            Attendee a = mapRow(rs);

            loadEventMapping(a);
            loadSessionMapping(a);

            return a;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Attendee> findAll() {
        List<Attendee> list = new ArrayList<>();

        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(
                    """
                    SELECT p.*, a.activity_history FROM persons p
                    JOIN attendees a ON p.id = a.id
                    """
            );

            while (rs.next()) {
                Attendee a = mapRow(rs);
                loadEventMapping(a);
                loadSessionMapping(a);
                list.add(a);
            }

            return list;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM attendees";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count attendees", e);
        }
    }

    private Attendee mapRow(ResultSet rs) throws SQLException {
        Attendee a = new Attendee();
        a.setId((UUID) rs.getObject("id"));
        a.setFullName(rs.getString("full_name"));
        a.setDateOfBirth(rs.getObject("dob", java.time.LocalDate.class));
        a.setEmail(rs.getString("email"));
        a.setPhone(rs.getString("phone"));
        a.setUsername(rs.getString("username"));
        a.setPasswordHash(rs.getString("password_hash"));
        return a;
    }

    private void loadEventMapping(Attendee a) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT event_id FROM attendee_event WHERE attendee_id=?"
        )) {
            ps.setObject(1, a.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                a.getRegisteredEventIds().add((UUID) rs.getObject("event_id"));
            }
        }
    }

    private void loadSessionMapping(Attendee a) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT session_id FROM attendee_session WHERE attendee_id=?"
        )) {
            ps.setObject(1, a.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                a.getRegisteredSessionIds().add((UUID) rs.getObject("session_id"));
            }
        }
    }

    /**
     * OPTIMIZED: Load event mappings for multiple attendees in one query
     * Instead of N queries (one per attendee), load all in 1 query
     */
    private void loadEventMappingOptimized(Map<UUID, Attendee> attendeeMap) throws SQLException {
        if (attendeeMap.isEmpty()) return;

        String attendeeIds = attendeeMap.keySet().stream()
                .map(id -> "'" + id.toString() + "'")
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        String sql = "SELECT attendee_id, event_id FROM attendee_event WHERE attendee_id IN (" + attendeeIds + ")";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                UUID attendeeId = (UUID) rs.getObject("attendee_id");
                UUID eventId = (UUID) rs.getObject("event_id");

                Attendee attendee = attendeeMap.get(attendeeId);
                if (attendee != null) {
                    attendee.getRegisteredEventIds().add(eventId);
                }
            }
        }
    }

    /**
     * OPTIMIZED: Load session mappings for multiple attendees in one query
     * Instead of N queries (one per attendee), load all in 1 query
     */
    private void loadSessionMappingOptimized(Map<UUID, Attendee> attendeeMap) throws SQLException {
        if (attendeeMap.isEmpty()) return;

        String attendeeIds = attendeeMap.keySet().stream()
                .map(id -> "'" + id.toString() + "'")
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        String sql = "SELECT attendee_id, session_id FROM attendee_session WHERE attendee_id IN (" + attendeeIds + ")";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                UUID attendeeId = (UUID) rs.getObject("attendee_id");
                UUID sessionId = (UUID) rs.getObject("session_id");

                Attendee attendee = attendeeMap.get(attendeeId);
                if (attendee != null) {
                    attendee.getRegisteredSessionIds().add(sessionId);
                }
            }
        }
    }

    @Override
    public void registerEvent(UUID attendeeId, UUID eventId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO attendee_event (attendee_id, event_id) VALUES (?,?) ON CONFLICT DO NOTHING"
        )) {
            ps.setObject(1, attendeeId);
            ps.setObject(2, eventId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void registerSession(UUID attendeeId, UUID sessionId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO attendee_session (attendee_id, session_id) VALUES (?,?) ON CONFLICT DO NOTHING"
        )) {
            ps.setObject(1, attendeeId);
            ps.setObject(2, sessionId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unregisterEvent(UUID attendeeId, UUID eventId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM attendee_event WHERE attendee_id=? AND event_id=?"
        )) {
            ps.setObject(1, attendeeId);
            ps.setObject(2, eventId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unregisterSession(UUID attendeeId, UUID sessionId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM attendee_session WHERE attendee_id=? AND session_id=?"
        )) {
            ps.setObject(1, attendeeId);
            ps.setObject(2, sessionId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
