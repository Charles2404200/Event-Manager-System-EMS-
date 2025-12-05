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
            (id, attendee_id, event_id, session_id, type, price, payment_status, status, qr_code_data)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                attendee_id    = EXCLUDED.attendee_id,
                event_id       = EXCLUDED.event_id,
                session_id     = EXCLUDED.session_id,
                type           = EXCLUDED.type,
                price          = EXCLUDED.price,
                payment_status = EXCLUDED.payment_status,
                status         = EXCLUDED.status,
                qr_code_data   = EXCLUDED.qr_code_data
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
        String sql = "SELECT * FROM tickets WHERE status=?";

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
    // FIND TEMPLATES
    // ---------------------------------------------------------
    @Override
    public List<Ticket> findTemplates() {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE attendee_id IS NULL";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // ---------------------------------------------------------
    // FIND ASSIGNED
    // ---------------------------------------------------------
    @Override
    public List<Ticket> findAssigned() {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE attendee_id IS NOT NULL";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // ---------------------------------------------------------
    // FIND TEMPLATES PAGE
    // ---------------------------------------------------------
    @Override
    public List<Ticket> findTemplatesPage(int offset, int limit) {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE attendee_id IS NULL ORDER BY created_at DESC NULLS LAST, id DESC LIMIT ? OFFSET ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // ---------------------------------------------------------
    // FIND ASSIGNED PAGE
    // ---------------------------------------------------------
    @Override
    public List<Ticket> findAssignedPage(int offset, int limit) {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE attendee_id IS NOT NULL ORDER BY created_at DESC NULLS LAST, id DESC LIMIT ? OFFSET ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // ---------------------------------------------------------
    // COUNT
    // ---------------------------------------------------------
    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM tickets";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count tickets", e);
        }
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

        // Handle TicketType safely
        try {
            String typeStr = rs.getString("type");
            if (typeStr != null) {
                t.setType(TicketType.valueOf(typeStr.toUpperCase()));
            } else {
                t.setType(TicketType.GENERAL); // Default
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown ticket type: " + rs.getString("type") + ", using GENERAL");
            t.setType(TicketType.GENERAL); // Fallback
        }

        t.setPrice(rs.getBigDecimal("price"));

        // Handle PaymentStatus safely
        try {
            String paymentStr = rs.getString("payment_status");
            if (paymentStr != null) {
                t.setPaymentStatus(PaymentStatus.valueOf(paymentStr.toUpperCase()));
            } else {
                t.setPaymentStatus(PaymentStatus.UNPAID); // Default
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown payment status: " + rs.getString("payment_status") + ", using UNPAID");
            t.setPaymentStatus(PaymentStatus.UNPAID); // Fallback
        }

        // Handle TicketStatus safely
        try {
            String statusStr = rs.getString("status");
            if (statusStr != null) {
                t.setTicketStatus(TicketStatus.valueOf(statusStr.toUpperCase()));
            } else {
                t.setTicketStatus(TicketStatus.ACTIVE); // Default
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown ticket status: " + rs.getString("status") + ", using ACTIVE");
            t.setTicketStatus(TicketStatus.ACTIVE); // Fallback
        }

        t.setQrCodeData(rs.getString("qr_code_data"));

        return t;
    }
}
