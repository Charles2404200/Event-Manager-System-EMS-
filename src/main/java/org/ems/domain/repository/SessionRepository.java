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

    List<Session> findByEvent(UUID eventId);

    List<Session> findByDate(LocalDate date);

    // Presenter assignment
    void assignPresenter(UUID sessionId, UUID presenterId);

    void clearPresenters(UUID sessionId);

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
}
