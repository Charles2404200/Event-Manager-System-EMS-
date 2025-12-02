package org.ems.infrastructure.repository.jdbc;

import org.ems.domain.model.Session;
import org.ems.domain.repository.SessionRepository;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class JdbcSessionRepository implements SessionRepository {

    private final Connection conn;

    public JdbcSessionRepository(Connection conn) {
        this.conn = conn;
    }

    // -------------------------------------------------------
    // SAVE (INSERT OR UPDATE)
    // -------------------------------------------------------
    @Override
    public Session save(Session s) {

        String sql = """
            INSERT INTO sessions
            (id, event_id, title, description, start_time, end_time, venue, capacity)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                event_id     = EXCLUDED.event_id,
                title        = EXCLUDED.title,
                description  = EXCLUDED.description,
                start_time   = EXCLUDED.start_time,
                end_time     = EXCLUDED.end_time,
                venue        = EXCLUDED.venue,
                capacity     = EXCLUDED.capacity
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, s.getId());
            ps.setObject(2, s.getEventId());
            ps.setString(3, s.getTitle());
            ps.setString(4, s.getDescription());
            ps.setObject(5, s.getStart());
            ps.setObject(6, s.getEnd());
            ps.setString(7, s.getVenue());
            ps.setInt(8, s.getCapacity());

            ps.executeUpdate();

            savePresenterLinks(s);

            return s;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save session", e);
        }
    }

    // -------------------------------------------------------
    // DELETE
    // -------------------------------------------------------
    @Override
    public void delete(UUID id) {
        try {
            // Clear presenter mapping first
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM presenter_session WHERE session_id=?"
            )) {
                ps.setObject(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM sessions WHERE id=?"
            )) {
                ps.setObject(1, id);
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------
    // FIND BY ID
    // -------------------------------------------------------
    @Override
    public Session findById(UUID id) {

        String sql = "SELECT * FROM sessions WHERE id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;

            Session s = mapRow(rs);

            loadPresenterIds(s);

            return s;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------
    // FIND ALL
    // -------------------------------------------------------
    @Override
    public List<Session> findAll() {

        List<Session> list = new ArrayList<>();

        try (Statement st = conn.createStatement()) {

            ResultSet rs = st.executeQuery("SELECT * FROM sessions");

            while (rs.next()) {
                Session s = mapRow(rs);
                loadPresenterIds(s);
                list.add(s);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // -------------------------------------------------------
    // FIND BY EVENT
    // -------------------------------------------------------
    @Override
    public List<Session> findByEvent(UUID eventId) {

        List<Session> list = new ArrayList<>();

        String sql = "SELECT * FROM sessions WHERE event_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, eventId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Session s = mapRow(rs);
                loadPresenterIds(s);
                list.add(s);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // -------------------------------------------------------
    // FIND BY DATE
    // -------------------------------------------------------
    @Override
    public List<Session> findByDate(LocalDate date) {

        List<Session> list = new ArrayList<>();

        String sql = """
            SELECT * FROM sessions
            WHERE DATE(start_time) = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, date);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Session s = mapRow(rs);
                loadPresenterIds(s);
                list.add(s);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // -------------------------------------------------------
    // ASSIGN PRESENTER
    // -------------------------------------------------------
    @Override
    public void assignPresenter(UUID sessionId, UUID presenterId) {

        String sql = """
            INSERT INTO presenter_session (presenter_id, session_id)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, presenterId);
            ps.setObject(2, sessionId);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    // -------------------------------------------------------
    // CLEAR PRESENTERS
    // -------------------------------------------------------
    @Override
    public void clearPresenters(UUID sessionId) {

        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM presenter_session WHERE session_id=?"
        )) {
            ps.setObject(1, sessionId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------
    // SAVE Presenter Links (called inside save())
    // -------------------------------------------------------
    private void savePresenterLinks(Session s) throws SQLException {

        clearPresenters(s.getId());

        if (s.getPresenterIds() == null) return;

        String sql = """
            INSERT INTO presenter_session (presenter_id, session_id)
            VALUES (?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (UUID pId : s.getPresenterIds()) {
                ps.setObject(1, pId);
                ps.setObject(2, s.getId());
                ps.executeUpdate();
            }
        }
    }

    // -------------------------------------------------------
    // LOAD presenter IDs for session
    // -------------------------------------------------------
    private void loadPresenterIds(Session s) throws SQLException {

        String sql = "SELECT presenter_id FROM presenter_session WHERE session_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, s.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                s.getPresenterIds()
                        .add((UUID) rs.getObject("presenter_id"));
            }
        }
    }

    // -------------------------------------------------------
    // MAPPER
    // -------------------------------------------------------
    private Session mapRow(ResultSet rs) throws SQLException {

        Session s = new Session();

        s.setId((UUID) rs.getObject("id"));
        s.setEventId((UUID) rs.getObject("event_id"));
        s.setTitle(rs.getString("title"));
        s.setDescription(rs.getString("description"));
        s.setStart(rs.getObject("start_time", LocalDateTime.class));
        s.setEnd(rs.getObject("end_time", LocalDateTime.class));
        s.setVenue(rs.getString("venue"));
        s.setCapacity(rs.getInt("capacity"));

        return s;
    }
}
