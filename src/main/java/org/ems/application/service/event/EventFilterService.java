package org.ems.application.service.event;

import org.ems.domain.model.Event;
import org.ems.domain.repository.EventRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EventFilterService - Handles event filtering and searching
 * Single Responsibility: Filter, search, and paginate events only
 *
 * @author EMS Team
 */
public class EventFilterService {

    private final EventRepository eventRepo;
    private List<Event> cachedAllEvents = null;

    public EventFilterService(EventRepository eventRepo) {
        this.eventRepo = eventRepo;
    }

    /**
     * Load all events (cached)
     */
    private List<Event> loadAllEvents() {
        if (cachedAllEvents == null) {
            long start = System.currentTimeMillis();
            cachedAllEvents = eventRepo.findAll();
            System.out.println("[EventFilterService] Loaded " + cachedAllEvents.size() +
                    " events in " + (System.currentTimeMillis() - start) + " ms");
        }
        return cachedAllEvents;
    }

    /**
     * Filter events by type and search term
     */
    public List<Event> filterEvents(String searchTerm, String typeFilter) {
        List<Event> all = loadAllEvents();
        String search = searchTerm != null ? searchTerm.toLowerCase() : "";
        String type = typeFilter != null ? typeFilter : "ALL";

        return all.stream()
                .filter(e -> "ALL".equals(type) ||
                           (e.getType() != null && e.getType().name().equals(type)))
                .filter(e -> search.isEmpty() ||
                           (e.getName() != null && e.getName().toLowerCase().contains(search)) ||
                           (e.getLocation() != null && e.getLocation().toLowerCase().contains(search)))
                .collect(Collectors.toList());
    }

    /**
     * Get all events (reload cache if needed)
     */
    public List<Event> getAllEvents() {
        return new ArrayList<>(loadAllEvents());
    }

    /**
     * Invalidate cache when events change
     */
    public void invalidateCache() {
        cachedAllEvents = null;
        System.out.println("[EventFilterService] Cache invalidated");
    }
}

