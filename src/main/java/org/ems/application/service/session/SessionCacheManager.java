package org.ems.application.service.session;

import org.ems.domain.model.Event;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.Session;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SessionCacheManager - Thread-safe centralized cache management
 * Manages caches for sessions, events, and presenters
 *
 * @author EMS Team
 */
public class SessionCacheManager {

    private List<Session> sessionsCache;
    private List<Event> eventsCache;
    private Map<UUID, Presenter> presentersCache;

    private final ReadWriteLock sessionsLock = new ReentrantReadWriteLock();
    private final ReadWriteLock eventsLock = new ReentrantReadWriteLock();
    private final ReadWriteLock presentersLock = new ReentrantReadWriteLock();

    // Cache validity tracking
    private long sessionsCacheTime = 0;
    private long eventsCacheTime = 0;
    private long presentersCacheTime = 0;
    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes

    /**
     * Get cached sessions (thread-safe)
     */
    public List<Session> getSessions() {
        sessionsLock.readLock().lock();
        try {
            return sessionsCache != null ? new ArrayList<>(sessionsCache) : null;
        } finally {
            sessionsLock.readLock().unlock();
        }
    }

    /**
     * Set sessions cache (thread-safe)
     */
    public void setSessions(List<Session> sessions) {
        sessionsLock.writeLock().lock();
        try {
            this.sessionsCache = sessions != null ? new ArrayList<>(sessions) : null;
            this.sessionsCacheTime = System.currentTimeMillis();
        } finally {
            sessionsLock.writeLock().unlock();
        }
    }

    /**
     * Clear sessions cache
     */
    public void clearSessions() {
        sessionsLock.writeLock().lock();
        try {
            this.sessionsCache = null;
            this.sessionsCacheTime = 0;
        } finally {
            sessionsLock.writeLock().unlock();
        }
    }

    /**
     * Check if sessions cache is valid
     */
    public boolean isSessionsCacheValid() {
        sessionsLock.readLock().lock();
        try {
            return sessionsCache != null &&
                   (System.currentTimeMillis() - sessionsCacheTime) < CACHE_VALIDITY_MS;
        } finally {
            sessionsLock.readLock().unlock();
        }
    }

    /**
     * Get cached events (thread-safe)
     */
    public List<Event> getEvents() {
        eventsLock.readLock().lock();
        try {
            return eventsCache != null ? new ArrayList<>(eventsCache) : null;
        } finally {
            eventsLock.readLock().unlock();
        }
    }

    /**
     * Set events cache (thread-safe)
     */
    public void setEvents(List<Event> events) {
        eventsLock.writeLock().lock();
        try {
            this.eventsCache = events != null ? new ArrayList<>(events) : null;
            this.eventsCacheTime = System.currentTimeMillis();
        } finally {
            eventsLock.writeLock().unlock();
        }
    }

    /**
     * Clear events cache
     */
    public void clearEvents() {
        eventsLock.writeLock().lock();
        try {
            this.eventsCache = null;
            this.eventsCacheTime = 0;
        } finally {
            eventsLock.writeLock().unlock();
        }
    }

    /**
     * Check if events cache is valid
     */
    public boolean isEventsCacheValid() {
        eventsLock.readLock().lock();
        try {
            return eventsCache != null &&
                   (System.currentTimeMillis() - eventsCacheTime) < CACHE_VALIDITY_MS;
        } finally {
            eventsLock.readLock().unlock();
        }
    }

    /**
     * Get cached presenters (thread-safe)
     */
    public Map<UUID, Presenter> getPresenters() {
        presentersLock.readLock().lock();
        try {
            return presentersCache != null ? new HashMap<>(presentersCache) : null;
        } finally {
            presentersLock.readLock().unlock();
        }
    }

    /**
     * Set presenters cache (thread-safe)
     */
    public void setPresenters(Map<UUID, Presenter> presenters) {
        presentersLock.writeLock().lock();
        try {
            this.presentersCache = presenters != null ? new HashMap<>(presenters) : null;
            this.presentersCacheTime = System.currentTimeMillis();
        } finally {
            presentersLock.writeLock().unlock();
        }
    }

    /**
     * Add or update single presenter in cache
     */
    public void addPresenterToCache(Presenter presenter) {
        presentersLock.writeLock().lock();
        try {
            if (presentersCache == null) {
                presentersCache = new HashMap<>();
            }
            presentersCache.put(presenter.getId(), presenter);
            this.presentersCacheTime = System.currentTimeMillis();
        } finally {
            presentersLock.writeLock().unlock();
        }
    }

    /**
     * Clear presenters cache
     */
    public void clearPresenters() {
        presentersLock.writeLock().lock();
        try {
            this.presentersCache = null;
            this.presentersCacheTime = 0;
        } finally {
            presentersLock.writeLock().unlock();
        }
    }

    /**
     * Check if presenters cache is valid
     */
    public boolean isPresentersCacheValid() {
        presentersLock.readLock().lock();
        try {
            return presentersCache != null && !presentersCache.isEmpty() &&
                   (System.currentTimeMillis() - presentersCacheTime) < CACHE_VALIDITY_MS;
        } finally {
            presentersLock.readLock().unlock();
        }
    }

    /**
     * Clear all caches
     */
    public void clearAll() {
        sessionsLock.writeLock().lock();
        eventsLock.writeLock().lock();
        presentersLock.writeLock().lock();
        try {
            this.sessionsCache = null;
            this.eventsCache = null;
            this.presentersCache = null;
            this.sessionsCacheTime = 0;
            this.eventsCacheTime = 0;
            this.presentersCacheTime = 0;
        } finally {
            presentersLock.writeLock().unlock();
            eventsLock.writeLock().unlock();
            sessionsLock.writeLock().unlock();
        }
    }
}

