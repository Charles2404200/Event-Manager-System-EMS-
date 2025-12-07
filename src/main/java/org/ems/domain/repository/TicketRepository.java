package org.ems.domain.repository;

import org.ems.domain.model.Ticket;
import org.ems.domain.model.enums.TicketType;
import org.ems.domain.model.enums.TicketStatus;
import org.ems.domain.model.enums.PaymentStatus;

import java.sql.Timestamp;
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
     * Returns total number of template tickets (attendee_id IS NULL).
     */
    long countTemplates();

    /**
     * Returns total number of assigned tickets (attendee_id IS NOT NULL).
     */
    long countAssigned();

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

    /**
     * Aggregate số lượng vé đã assign theo template key (event, session, type, price).
     * Có thể dùng để tính "available" mà không cần load toàn bộ danh sách assigned vào memory.
     */
    List<TemplateAssignmentStats> findAssignedStatsForTemplates();

    /**
     * Projection đơn giản cho aggregate assigned tickets.
     */
    final class TemplateAssignmentStats {
        private final UUID eventId;
        private final UUID sessionId;
        private final TicketType type;
        private final java.math.BigDecimal price;
        private final long assignedCount;

        public TemplateAssignmentStats(UUID eventId, UUID sessionId,
                                       TicketType type, java.math.BigDecimal price,
                                       long assignedCount) {
            this.eventId = eventId;
            this.sessionId = sessionId;
            this.type = type;
            this.price = price;
            this.assignedCount = assignedCount;
        }

        public UUID getEventId() { return eventId; }
        public UUID getSessionId() { return sessionId; }
        public TicketType getType() { return type; }
        public java.math.BigDecimal getPrice() { return price; }
        public long getAssignedCount() { return assignedCount; }
    }

    /**
     * Find ticket templates by keyset cursor pagination.
     * Uses (created_at, id) as cursor for efficient pagination.
     *
     * @param lastCreatedAt Last created_at value from previous page (null for first page)
     * @param lastId Last id value from previous page (null for first page)
     * @param limit Number of records to fetch
     * @return List of template tickets
     */
    List<Ticket> findTemplatesByCursor(Timestamp lastCreatedAt, UUID lastId, int limit);

    /**
     * Find assigned tickets by keyset cursor pagination.
     * Uses (created_at, id) as cursor for efficient pagination.
     *
     * @param lastCreatedAt Last created_at value from previous page (null for first page)
     * @param lastId Last id value from previous page (null for first page)
     * @param limit Number of records to fetch
     * @return List of assigned tickets
     */
    List<Ticket> findAssignedByCursor(Timestamp lastCreatedAt, UUID lastId, int limit);
}
