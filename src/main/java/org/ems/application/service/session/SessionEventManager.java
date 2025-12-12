package org.ems.application.service.session;

import org.ems.application.service.event.EventService;
import org.ems.domain.model.Event;
import org.ems.infrastructure.config.AppContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SessionEventManager - Handles event-related operations
 * Single Responsibility: Manage event data for sessions
 *
 * @author EMS Team
 */
public class SessionEventManager {

    private final EventService eventService;
    private final SessionCacheManager cacheManager;

    /**
     * Constructor with dependency injection
     */
    public SessionEventManager(EventService eventService, SessionCacheManager cacheManager) {
        this.eventService = eventService;
        this.cacheManager = cacheManager;
    }

    /**
     * Convenience constructor using AppContext
     */
    public SessionEventManager(SessionCacheManager cacheManager) {
        this(AppContext.get().eventService, cacheManager);
    }

    /**
     * Get all events (with caching)
     */
    public List<Event> getAllEvents() {
        long start = System.currentTimeMillis();

        // Check cache first
        if (cacheManager.isEventsCacheValid()) {
            List<Event> cached = cacheManager.getEvents();
            if (cached != null) {
                System.out.println("  ℹ Using cached events (" + cached.size() + " events)");
                return new ArrayList<>(cached);
            }
        }

        // Load from DB
        System.out.println("📦 [SessionEventManager] Loading events...");
        try {
            List<Event> events = eventService.getEvents();
            cacheManager.setEvents(events);
            System.out.println("  ✓ Loaded " + events.size() + " events in " +
                    (System.currentTimeMillis() - start) + " ms");
            return new ArrayList<>(events);
        } catch (Exception e) {
            System.err.println("✗ Error loading events: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Build event name mapping (UUID -> Event Name)
     */
    public Map<UUID, String> buildEventNameMap() {
        long start = System.currentTimeMillis();
        System.out.println("🗺️ [SessionEventManager] Building event name mapping...");

        List<Event> events = getAllEvents();
        Map<UUID, String> eventNameMap = new HashMap<>();

        for (Event e : events) {
            eventNameMap.put(e.getId(), e.getName());
        }

        System.out.println("  ✓ Event name mapping built (" + eventNameMap.size() + " events) in " +
                (System.currentTimeMillis() - start) + " ms");
        return eventNameMap;
    }

    /**
     * Get event name by ID
     */
    public String getEventName(UUID eventId) {
        List<Event> events = getAllEvents();

        for (Event e : events) {
            if (e.getId().equals(eventId)) {
                return e.getName();
            }
        }

        return "N/A";
    }

    /**
     * Clear event cache
     */
    public void clearCache() {
        System.out.println("🧹 [SessionEventManager] Clearing event cache...");
        cacheManager.clearEvents();
    }
}

