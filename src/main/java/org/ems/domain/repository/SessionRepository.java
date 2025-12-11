package org.ems.domain.repository;

import org.ems.domain.model.Session;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SessionRepository {

    Session save(Session session);

    void delete(UUID id);

    Session findById(UUID id);

    List<Session> findAll();

    /**
     * Find all sessions with optimized presenter loading (batch query instead of N+1)
     * ⚡ Much faster for large datasets
     */
    List<Session> findAllOptimized();

    List<Session> findByEvent(UUID eventId);

    List<Session> findByDate(LocalDate date);

    // Presenter assignment
    void assignPresenter(UUID sessionId, UUID presenterId);

    List<Session> findByPresenter(UUID presenterId, int offset, int limit);

    void clearPresenters(UUID sessionId);

    long countByPresenter(UUID presenterId);

    /**
     * Returns total number of sessions.
     */
    long count();

    /**
     * Phân trang session: trả về danh sách theo offset/limit.
     */
    List<Session> findPage(int offset, int limit);

    /**
     * Batch count sessions for multiple events using GROUP BY.
     */
    java.util.Map<java.util.UUID, Integer> countByEventIds(java.util.List<java.util.UUID> eventIds);

    /**
     * Register attendee for a session
     */
    void registerAttendeeForSession(UUID attendeeId, UUID sessionId);

    /**
     * Unregister attendee from a session
     */
    void unregisterAttendeeFromSession(UUID attendeeId, UUID sessionId);

    /**
     * Get all sessions registered for an attendee
     */
    List<Session> findSessionsForAttendee(UUID attendeeId);

    /**
     * Find all sessions that an attendee has registered for in a specific event
     */
    List<Session> findSessionsByAttendeeAndEvent(UUID attendeeId, UUID eventId);

    /**
     * Cancel an attendee's registration for a session
     */
    void cancelAttendeeSession(UUID attendeeId, UUID sessionId);
}
