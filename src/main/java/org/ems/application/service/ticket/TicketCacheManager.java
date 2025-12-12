package org.ems.application.service.ticket;

import org.ems.application.dto.template.TemplateKey;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Event;
import org.ems.domain.model.Session;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * TicketCacheManager - Manages all caches for ticket manager
 * Thread-safe cache management for events, sessions, attendees, and statistics
 *
 * @author EMS Team
 */
public class TicketCacheManager {

    // Entity caches
    private final Map<UUID, Event> eventCacheById = new HashMap<>();
    private final Map<UUID, Session> sessionCacheById = new HashMap<>();
    private final Map<UUID, Attendee> attendeeCacheById = new HashMap<>();

    // Name mapping caches
    private final Map<String, UUID> eventMap = new HashMap<>();
    private final Map<String, UUID> sessionMap = new HashMap<>();
    private final Map<String, UUID> attendeeMap = new HashMap<>();
    private final Map<String, UUID> templateMap = new HashMap<>();

    // Statistics cache
    private Map<TemplateKey, Long> templateAssignedCountCache = null;

    // Thread safety
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private long lastStatsCacheTime = 0;
    private static final long CACHE_VALIDITY_MS = TicketManagerConfig.CACHE_VALIDITY_MS;

    /**
     * Clear all caches
     */
    public void clearAll() {
        lock.writeLock().lock();
        try {
            eventCacheById.clear();
            sessionCacheById.clear();
            attendeeCacheById.clear();
            eventMap.clear();
            sessionMap.clear();
            attendeeMap.clear();
            templateMap.clear();
            templateAssignedCountCache = null;
            lastStatsCacheTime = 0;
            System.out.println("🧹 [TicketCacheManager] All caches cleared");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clear event caches
     */
    public void clearEvents() {
        lock.writeLock().lock();
        try {
            eventCacheById.clear();
            eventMap.clear();
            System.out.println("🧹 [TicketCacheManager] Event caches cleared");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clear session caches
     */
    public void clearSessions() {
        lock.writeLock().lock();
        try {
            sessionCacheById.clear();
            sessionMap.clear();
            System.out.println("🧹 [TicketCacheManager] Session caches cleared");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clear attendee caches
     */
    public void clearAttendees() {
        lock.writeLock().lock();
        try {
            attendeeCacheById.clear();
            attendeeMap.clear();
            System.out.println("🧹 [TicketCacheManager] Attendee caches cleared");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clear statistics cache
     */
    public void clearStatistics() {
        lock.writeLock().lock();
        try {
            templateAssignedCountCache = null;
            lastStatsCacheTime = 0;
            System.out.println("🧹 [TicketCacheManager] Statistics cache cleared");
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Event cache methods
    public void addEvent(Event event) {
        lock.writeLock().lock();
        try {
            eventCacheById.put(event.getId(), event);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Event getEvent(UUID eventId) {
        lock.readLock().lock();
        try {
            return eventCacheById.get(eventId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, UUID> getEventMap() {
        lock.readLock().lock();
        try {
            return new HashMap<>(eventMap);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void putEventMapping(String display, UUID eventId) {
        lock.writeLock().lock();
        try {
            eventMap.put(display, eventId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Session cache methods
    public void addSession(Session session) {
        lock.writeLock().lock();
        try {
            sessionCacheById.put(session.getId(), session);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Session getSession(UUID sessionId) {
        lock.readLock().lock();
        try {
            return sessionCacheById.get(sessionId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<UUID, Session> getSessionCacheById() {
        lock.readLock().lock();
        try {
            return new HashMap<>(sessionCacheById);
        } finally {
            lock.readLock().unlock();
        }
    }

    // Attendee cache methods
    public void addAttendee(Attendee attendee) {
        lock.writeLock().lock();
        try {
            attendeeCacheById.put(attendee.getId(), attendee);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Attendee getAttendee(UUID attendeeId) {
        lock.readLock().lock();
        try {
            return attendeeCacheById.get(attendeeId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, UUID> getAttendeeMap() {
        lock.readLock().lock();
        try {
            return new HashMap<>(attendeeMap);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void putAttendeeMapping(String display, UUID attendeeId) {
        lock.writeLock().lock();
        try {
            attendeeMap.put(display, attendeeId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Statistics cache methods
    public Map<TemplateKey, Long> getTemplateAssignedCountCache() {
        lock.readLock().lock();
        try {
            if (templateAssignedCountCache != null &&
                (System.currentTimeMillis() - lastStatsCacheTime) < CACHE_VALIDITY_MS) {
                return new HashMap<>(templateAssignedCountCache);
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTemplateAssignedCountCache(Map<TemplateKey, Long> cache) {
        lock.writeLock().lock();
        try {
            this.templateAssignedCountCache = cache != null ? new HashMap<>(cache) : null;
            this.lastStatsCacheTime = System.currentTimeMillis();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<String, UUID> getTemplateMap() {
        lock.readLock().lock();
        try {
            return new HashMap<>(templateMap);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void putTemplateMapping(String display, UUID templateId) {
        lock.writeLock().lock();
        try {
            templateMap.put(display, templateId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Accessor methods for UI components
    /**
     * Get event from cache (nullable)
     */
    public Event getEventFromCache(UUID eventId) {
        return getEvent(eventId);
    }

    /**
     * Get attendee from cache (nullable)
     */
    public Attendee getAttendeeFromCache(UUID attendeeId) {
        return getAttendee(attendeeId);
    }

    /**
     * Get session from cache (nullable)
     */
    public Session getSessionFromCache(UUID sessionId) {
        return getSession(sessionId);
    }

    /**
     * Cache all events at once (batch operation)
     */
    public void cacheAllEvents(List<Event> events) {
        if (events == null) return;
        lock.writeLock().lock();
        try {
            for (Event evt : events) {
                eventCacheById.put(evt.getId(), evt);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Cache all attendees at once (batch operation)
     */
    public void cacheAllAttendees(List<Attendee> attendees) {
        if (attendees == null) return;
        lock.writeLock().lock();
        try {
            for (Attendee att : attendees) {
                attendeeCacheById.put(att.getId(), att);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Cache all sessions at once (batch operation)
     */
    public void cacheAllSessions(List<Session> sessions) {
        if (sessions == null) return;
        lock.writeLock().lock();
        try {
            for (Session sess : sessions) {
                sessionCacheById.put(sess.getId(), sess);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}

