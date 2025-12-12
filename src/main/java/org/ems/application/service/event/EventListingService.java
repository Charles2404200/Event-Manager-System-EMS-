package org.ems.application.service.event;

import org.ems.application.dto.event.EventFilterCriteriaDTO;
import org.ems.application.dto.event.EventRowDTO;
import org.ems.application.dto.page.PagedResult;
import org.ems.domain.model.Event;
import org.ems.domain.repository.EventRepository;
import org.ems.domain.repository.SessionRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * EventListingService - Handles event listing, filtering, and pagination
 * Single Responsibility: Event filtering + pagination logic
 *
 * @author EMS Team
 */
public class EventListingService {

    private final EventRepository eventRepo;
    private final SessionRepository sessionRepo;
    private List<Event> cachedAllEvents = null;
    private final Set<UUID> userRegisteredEvents;
    private static final int DEFAULT_PAGE_SIZE = 10;

    public EventListingService(EventRepository eventRepo, SessionRepository sessionRepo,
                               Set<UUID> userRegisteredEvents) {
        this.eventRepo = eventRepo;
        this.sessionRepo = sessionRepo;
        this.userRegisteredEvents = userRegisteredEvents != null ? userRegisteredEvents : new HashSet<>();
    }

    /**
     * Load and filter events with pagination
     */
    public PagedResult<EventRowDTO> loadEventPage(EventFilterCriteriaDTO criteria) {
        long start = System.currentTimeMillis();
        System.out.println("[EventListingService] loadEventPage(" + criteria + ") starting...");

        try {
            // Load all events once and cache (OPTIMIZED)
            if (cachedAllEvents == null) {
                long allStart = System.currentTimeMillis();
                System.out.println("  Loading all events (first time)...");
                cachedAllEvents = eventRepo.findAll();
                System.out.println("  ✓ Loaded " + cachedAllEvents.size() + " events in " +
                        (System.currentTimeMillis() - allStart) + " ms");
            }

            // Filter in-memory (fast)
            long filterStart = System.currentTimeMillis();
            List<Event> filtered = filterEvents(cachedAllEvents, criteria);
            System.out.println("  ✓ Filtered to " + filtered.size() + " events in " +
                    (System.currentTimeMillis() - filterStart) + " ms");

            // Paginate
            int totalItems = filtered.size();
            int pageSize = criteria.pageSize > 0 ? criteria.pageSize : DEFAULT_PAGE_SIZE;
            int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);
            int safePage = Math.max(0, Math.min(criteria.pageNumber, totalPages - 1));

            int offset = safePage * pageSize;
            int end = Math.min(offset + pageSize, filtered.size());
            List<Event> pageEvents = filtered.subList(offset, end);

            System.out.println("  ✓ Paginated: page " + (safePage + 1) + "/" + totalPages +
                    ", offset " + offset + ", showing " + pageEvents.size() + " items");

            // Load session counts for page events
            long sessionStart = System.currentTimeMillis();
            Map<UUID, Integer> sessionCounts = loadSessionCounts(pageEvents);
            System.out.println("  ✓ Session counts loaded in " +
                    (System.currentTimeMillis() - sessionStart) + " ms");

            // Convert to DTOs
            List<EventRowDTO> rows = pageEvents.stream()
                    .map(e -> convertToDTO(e, sessionCounts.getOrDefault(e.getId(), 0)))
                    .collect(Collectors.toList());

            System.out.println("✓ loadEventPage completed in " + (System.currentTimeMillis() - start) + " ms");

            return new PagedResult<>(rows, safePage + 1, totalPages, (long) totalItems);

        } catch (Exception e) {
            System.err.println("✗ Error loading events: " + e.getMessage());
            e.printStackTrace();
            return new PagedResult<>(Collections.emptyList(), 1, 0, 0L);
        }
    }

    /**
     * Filter events by criteria
     */
    private List<Event> filterEvents(List<Event> events, EventFilterCriteriaDTO criteria) {
        return events.stream()
                .filter(e -> "ALL".equals(criteria.typeFilter) ||
                           e.getType() != null && e.getType().name().equals(criteria.typeFilter))
                .filter(e -> "ALL".equals(criteria.statusFilter) ||
                           e.getStatus() != null && e.getStatus().name().equals(criteria.statusFilter))
                .filter(e -> criteria.searchTerm.isEmpty() ||
                           e.getName() != null && e.getName().toLowerCase().contains(criteria.searchTerm) ||
                           e.getLocation() != null && e.getLocation().toLowerCase().contains(criteria.searchTerm))
                .collect(Collectors.toList());
    }

    /**
     * Load session counts for events (batch query)
     */
    private Map<UUID, Integer> loadSessionCounts(List<Event> events) {
        if (events.isEmpty() || sessionRepo == null) {
            return new HashMap<>();
        }

        try {
            List<UUID> eventIds = events.stream()
                    .map(Event::getId)
                    .collect(Collectors.toList());
            return sessionRepo.countByEventIds(eventIds);
        } catch (Exception e) {
            System.err.println("Warning: Could not load session counts: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Convert Event model to EventRowDTO
     */
    private EventRowDTO convertToDTO(Event event, int sessionCount) {
        return new EventRowDTO(
                event.getId(),
                event.getName(),
                event.getType() != null ? event.getType().name() : "N/A",
                event.getLocation(),
                event.getStartDate() != null ? event.getStartDate().toString() : "N/A",
                event.getEndDate() != null ? event.getEndDate().toString() : "N/A",
                event.getStatus() != null ? event.getStatus().name() : "N/A",
                sessionCount,
                userRegisteredEvents.contains(event.getId()),
                event.getImagePath()
        );
    }

    /**
     * Invalidate cache (call when events change)
     */
    public void invalidateCache() {
        cachedAllEvents = null;
        System.out.println("[EventListingService] Cache invalidated");
    }

    /**
     * Get total event count
     */
    public long getTotalEventCount() {
        if (cachedAllEvents == null && eventRepo != null) {
            cachedAllEvents = eventRepo.findAll();
        }
        return cachedAllEvents != null ? cachedAllEvents.size() : 0L;
    }
}

