package org.ems.infrastructure.repository.jdbc;

import org.ems.domain.model.enums.EventStatus;
import org.ems.domain.model.enums.EventType;
import org.ems.domain.model.Event;
import org.ems.domain.repository.EventRepository;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class JdbcEventRepository implements EventRepository {

    private final Connection conn;

    public JdbcEventRepository(Connection conn) {
        this.conn = conn;
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
    // FIND ALL
    // ---------------------------------------------------------
    @Override
    public List<Event> findAll() {

        List<Event> list = new ArrayList<>();

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM events")) {

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
        try (PreparedStatement ps = conn.prepareStatement(
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
