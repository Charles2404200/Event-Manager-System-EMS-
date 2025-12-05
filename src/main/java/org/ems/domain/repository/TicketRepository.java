package org.ems.domain.repository;

import org.ems.domain.model.Ticket;
import org.ems.domain.model.enums.TicketType;
import org.ems.domain.model.enums.TicketStatus;
import org.ems.domain.model.enums.PaymentStatus;

import java.util.List;
import java.util.UUID;

public interface TicketRepository {

    Ticket save(Ticket ticket);

    void delete(UUID id);

    Ticket findById(UUID id);

    List<Ticket> findAll();

    List<Ticket> findByAttendee(UUID attendeeId);

    List<Ticket> findByEvent(UUID eventId);

    List<Ticket> findBySession(UUID sessionId);

    List<Ticket> findByType(TicketType type);

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByPaymentStatus(PaymentStatus status);

    /**
     * Returns total number of tickets.
     */
    long count();

    /**
     * Find ticket templates: tickets chưa gán attendee (attendee_id IS NULL).
     */
    List<Ticket> findTemplates();

    /**
     * Find assigned tickets: tickets đã gán attendee (attendee_id IS NOT NULL).
     */
    List<Ticket> findAssigned();

    /**
     * Templates phân trang.
     */
    List<Ticket> findTemplatesPage(int offset, int limit);

    /**
     * Assigned tickets phân trang.
     */
    List<Ticket> findAssignedPage(int offset, int limit);
}
