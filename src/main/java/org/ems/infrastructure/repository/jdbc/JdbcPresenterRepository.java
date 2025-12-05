package org.ems.infrastructure.repository.jdbc;

import org.ems.domain.model.enums.PresenterType;
import org.ems.domain.model.enums.Role;
import org.ems.domain.model.Presenter;
import org.ems.domain.repository.PresenterRepository;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class JdbcPresenterRepository implements PresenterRepository {

    private final Connection conn;

    public JdbcPresenterRepository(Connection conn) {
        this.conn = conn;
    }

    // -------------------------------------------------------
    // SAVE (INSERT OR UPDATE)
    // -------------------------------------------------------
    @Override
    public Presenter save(Presenter p) {

        try {
            // First save to persons table
            String personSql = """
                INSERT INTO persons
                (id, full_name, dob, email, phone, username, password_hash, role, bio)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    full_name      = EXCLUDED.full_name,
                    dob            = EXCLUDED.dob,
                    email          = EXCLUDED.email,
                    phone          = EXCLUDED.phone,
                    username       = EXCLUDED.username,
                    password_hash  = EXCLUDED.password_hash,
                    role           = EXCLUDED.role,
                    bio            = EXCLUDED.bio
            """;

            try (PreparedStatement ps = conn.prepareStatement(personSql)) {
                ps.setObject(1, p.getId());
                ps.setString(2, p.getFullName());
                ps.setObject(3, p.getDateOfBirth());
                ps.setString(4, p.getEmail());
                ps.setString(5, p.getPhone());
                ps.setString(6, p.getUsername());
                ps.setString(7, p.getPasswordHash());
                ps.setString(8, Role.PRESENTER.name());
                ps.setString(9, p.getBio());
                ps.executeUpdate();
            }

            // Then save to presenters table
            String presenterSql = """
                INSERT INTO presenters (id, presenter_type, material_paths)
                VALUES (?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    presenter_type = EXCLUDED.presenter_type,
                    material_paths = EXCLUDED.material_paths
            """;

            try (PreparedStatement ps = conn.prepareStatement(presenterSql)) {
                ps.setObject(1, p.getId());
                ps.setString(2, p.getPresenterType().name());
                ps.setString(3, "[]"); // Empty material paths
                ps.executeUpdate();
            }

            saveSessionLinks(p);

            return p;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save presenter", e);
        }
    }

    // -------------------------------------------------------
    // DELETE
    // -------------------------------------------------------
    @Override
    public void delete(UUID id) {

        try {
            // Remove mapping first
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM presenter_session WHERE presenter_id=?"
            )) {
                ps.setObject(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM presenters WHERE id=?"
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
    public Presenter findById(UUID id) {

        String sql = """
            SELECT p.*, pr.presenter_type, pr.material_paths FROM persons p
            JOIN presenters pr ON p.id = pr.id
            WHERE p.id=?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;

            Presenter p = mapRow(rs);

            loadSessionIds(p);

            return p;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------
    // FIND BY USERNAME
    // -------------------------------------------------------
    @Override
    public Presenter findByUsername(String username) {

        String sql = """
            SELECT p.*, pr.presenter_type, pr.material_paths FROM persons p
            JOIN presenters pr ON p.id = pr.id
            WHERE p.username=?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;

            Presenter p = mapRow(rs);
            loadSessionIds(p);

            return p;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------
    // FIND ALL
    // -------------------------------------------------------
    @Override
    public List<Presenter> findAll() {

        List<Presenter> list = new ArrayList<>();

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("""
                 SELECT p.*, pr.presenter_type, pr.material_paths FROM persons p
                 JOIN presenters pr ON p.id = pr.id
                 """)) {

            while (rs.next()) {
                Presenter p = mapRow(rs);
                loadSessionIds(p);
                list.add(p);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // -------------------------------------------------------
    // FIND PRESENTERS BY EVENT (reverse lookup)
    // -------------------------------------------------------
    @Override
    public List<Presenter> findByEvent(UUID eventId) {

        List<Presenter> list = new ArrayList<>();

        String sql = """
            SELECT p.*, pr.presenter_type, pr.material_paths FROM persons p
            JOIN presenters pr ON p.id = pr.id
            JOIN presenter_session ps ON p.id = ps.presenter_id
            JOIN sessions s ON s.id = ps.session_id
            WHERE s.event_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, eventId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Presenter p = mapRow(rs);
                loadSessionIds(p);
                list.add(p);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // -------------------------------------------------------
    // FIND PRESENTERS BY SESSION
    // -------------------------------------------------------
    @Override
    public List<Presenter> findBySession(UUID sessionId) {

        List<Presenter> list = new ArrayList<>();

        String sql = """
            SELECT p.*, pr.presenter_type, pr.material_paths FROM persons p
            JOIN presenters pr ON p.id = pr.id
            JOIN presenter_session ps ON p.id = ps.presenter_id
            WHERE ps.session_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, sessionId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Presenter p = mapRow(rs);
                loadSessionIds(p);
                list.add(p);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // -------------------------------------------------------
    // ASSIGN PRESENTERS TO SESSION
    // -------------------------------------------------------
    @Override
    public void assignToSession(UUID presenterId, UUID sessionId) {

        String sql = """
            INSERT INTO presenter_session (presenter_id, session_id)
            VALUES (?,?)
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

    @Override
    public void removeFromSession(UUID presenterId, UUID sessionId) {

        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM presenter_session WHERE presenter_id=? AND session_id=?"
        )) {
            ps.setObject(1, presenterId);
            ps.setObject(2, sessionId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void clearSessions(UUID presenterId) {

        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM presenter_session WHERE presenter_id=?"
        )) {
            ps.setObject(1, presenterId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------
    // COUNT PRESENTERS
    // -------------------------------------------------------
    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM presenters";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count presenters", e);
        }
    }

    // -------------------------------------------------------
    // INTERNAL METHODS
    // -------------------------------------------------------
    private void saveSessionLinks(Presenter p) throws SQLException {

        clearSessions(p.getId());

        if (p.getSessionIds() == null) return;

        String sql = """
            INSERT INTO presenter_session (presenter_id, session_id)
            VALUES (?,?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            for (UUID sessionId : p.getSessionIds()) {
                ps.setObject(1, p.getId());
                ps.setObject(2, sessionId);
                ps.executeUpdate();
            }
        }
    }

    private void loadSessionIds(Presenter p) throws SQLException {

        String sql = "SELECT session_id FROM presenter_session WHERE presenter_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, p.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                p.getSessionIds().add((UUID) rs.getObject("session_id"));
            }
        }
    }

    // -------------------------------------------------------
    // ROW MAPPER
    // -------------------------------------------------------
    private Presenter mapRow(ResultSet rs) throws SQLException {

        Presenter p = new Presenter();

        p.setId((UUID) rs.getObject("id"));
        p.setFullName(rs.getString("full_name"));
        p.setDateOfBirth(rs.getObject("dob", LocalDate.class));
        p.setEmail(rs.getString("email"));
        p.setPhone(rs.getString("phone"));
        p.setUsername(rs.getString("username"));
        p.setPasswordHash(rs.getString("password_hash"));
        p.setRole(Role.PRESENTER);

        // Handle presenter_type - use role column if presenter_type is null
        String presenterType = rs.getString("presenter_type");
        if (presenterType == null || presenterType.isEmpty()) {
            presenterType = rs.getString("role");
        }
        if (presenterType == null || presenterType.isEmpty()) {
            presenterType = "Keynote Speaker"; // Default
        }
        try {
            p.setPresenterType(PresenterType.valueOf(presenterType.toUpperCase().replace(" ", "_")));
        } catch (IllegalArgumentException e) {
            p.setPresenterType(PresenterType.valueOf("KEYNOTE_SPEAKER")); // Fallback
        }

        p.setBio(rs.getString("bio"));

        return p;
    }
}
