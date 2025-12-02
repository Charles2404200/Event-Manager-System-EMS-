package org.ems.infrastructure.repository.jdbc;

import org.ems.domain.model.enums.PaymentStatus;
import org.ems.domain.model.enums.TicketStatus;
import org.ems.domain.model.enums.TicketType;
import org.ems.domain.model.Ticket;
import org.ems.domain.repository.TicketRepository;

import java.sql.*;
import java.util.*;

public class JdbcTicketRepository implements TicketRepository {

    private final Connection conn;

    public JdbcTicketRepository(Connection conn) {
        this.conn = conn;
    }

    // ---------------------------------------------------------
    // SAVE (INSERT OR UPDATE)
    // ---------------------------------------------------------
    @Override
    public Ticket save(Ticket t) {

        String sql = """
            INSERT INTO tickets
            (id, attendee_id, event_id, session_id, type, price, payment_status, ticket_status, qr_data)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                attendee_id    = EXCLUDED.attendee_id,
                event_id       = EXCLUDED.event_id,
                session_id     = EXCLUDED.session_id,
                type           = EXCLUDED.type,
                price          = EXCLUDED.price,
                payment_status = EXCLUDED.payment_status,
                ticket_status  = EXCLUDED.ticket_status,
                qr_data        = EXCLUDED.qr_data
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, t.getId());
            ps.setObject(2, t.getAttendeeId());
            ps.setObject(3, t.getEventId());
            ps.setObject(4, t.getSessionId());
            ps.setString(5, t.getType().name());
            ps.setBigDecimal(6, t.getPrice());
            ps.setString(7, t.getPaymentStatus().name());
            ps.setString(8, t.getTicketStatus().name());
            ps.setString(9, t.getQrCodeData());

            ps.executeUpdate();
            return t;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save ticket", e);
        }
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------
    @Override
    public void delete(UUID id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM tickets WHERE id=?"
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
    public Ticket findById(UUID id) {

        String sql = "SELECT * FROM tickets WHERE id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;

            return mapRow(rs);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------
    // FIND ALL
    // ---------------------------------------------------------
    @Override
    public List<Ticket> findAll() {

        List<Ticket> list = new ArrayList<>();

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM tickets")) {

            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // ---------------------------------------------------------
    // FIND BY ATTENDEE
    // ---------------------------------------------------------
    @Override
    public List<Ticket> findByAttendee(UUID attendeeId) {

        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE attendee_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, attendeeId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // ---------------------------------------------------------
    // FIND BY EVENT
    // ---------------------------------------------------------
    @Override
    public List<Ticket> findByEvent(UUID eventId) {

        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE event_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, eventId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // ---------------------------------------------------------
    // FIND BY SESSION
    // ---------------------------------------------------------
    @Override
    public List<Ticket> findBySession(UUID sessionId) {

        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE session_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, sessionId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // ---------------------------------------------------------
    // FIND BY TICKET TYPE
    // ---------------------------------------------------------
    @Override
    public List<Ticket> findByType(TicketType type) {

        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE type=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());

            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // ---------------------------------------------------------
    // FIND BY TICKET STATUS
    // ---------------------------------------------------------
    @Override
    public List<Ticket> findByStatus(TicketStatus status) {

        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE ticket_status=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());

            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // ---------------------------------------------------------
    // FIND BY PAYMENT STATUS
    // ---------------------------------------------------------
    @Override
    public List<Ticket> findByPaymentStatus(PaymentStatus status) {

        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE payment_status=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());

            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // ---------------------------------------------------------
    // MAPPER
    // ---------------------------------------------------------
    private Ticket mapRow(ResultSet rs) throws SQLException {

        Ticket t = new Ticket();

        t.setId((UUID) rs.getObject("id"));
        t.setAttendeeId((UUID) rs.getObject("attendee_id"));
        t.setEventId((UUID) rs.getObject("event_id"));
        t.setSessionId((UUID) rs.getObject("session_id"));
        t.setType(TicketType.valueOf(rs.getString("type")));
        t.setPrice(rs.getBigDecimal("price"));
        t.setPaymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")));
        t.setTicketStatus(TicketStatus.valueOf(rs.getString("ticket_status")));
        t.setQrCodeData(rs.getString("qr_data"));

        return t;
    }
}
