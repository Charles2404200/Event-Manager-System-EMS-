package org.ems.application.service;

import org.ems.application.dto.EventDetailsDTO;
import org.ems.domain.model.Event;
import org.ems.domain.repository.EventRepository;
import org.ems.domain.repository.SessionRepository;

import java.util.*;

/**
 * EventDetailService - Loads and prepares event details for display
 * Single Responsibility: Load event + prepare details DTO
 *
 * @author EMS Team
 */
public class EventDetailService {

    private final EventRepository eventRepo;
    private final SessionRepository sessionRepo;
    private final Map<UUID, Event> eventCache = new HashMap<>();
    private final Set<UUID> userRegisteredEvents;

    public EventDetailService(EventRepository eventRepo, SessionRepository sessionRepo,
                             Set<UUID> userRegisteredEvents) {
        this.eventRepo = eventRepo;
        this.sessionRepo = sessionRepo;
        this.userRegisteredEvents = userRegisteredEvents != null ? userRegisteredEvents : new HashSet<>();
    }

    /**
     * Load event details
     */
    public EventDetailsDTO loadEventDetails(UUID eventId) {
        long start = System.currentTimeMillis();
        System.out.println("[EventDetailService] loadEventDetails(" + eventId + ") starting...");

        try {
            // Check cache
            Event event = eventCache.get(eventId);
            if (event == null && eventRepo != null) {
                event = eventRepo.findById(eventId);
                if (event != null) {
                    eventCache.put(eventId, event);
                }
            }

            if (event == null) {
                System.err.println("✗ Event not found: " + eventId);
                return null;
            }

            // Load session count
            int sessionCount = 0;
            if (sessionRepo != null) {
                sessionCount = sessionRepo.countByEventIds(List.of(eventId))
                        .getOrDefault(eventId, 0);
            }

            // Create DTO
            EventDetailsDTO dto = new EventDetailsDTO(
                    event.getId(),
                    event.getName(),
                    event.getType() != null ? event.getType().name() : "N/A",
                    event.getLocation(),
                    event.getStartDate() != null ? event.getStartDate().toString() : "N/A",
                    event.getEndDate() != null ? event.getEndDate().toString() : "N/A",
                    event.getStatus() != null ? event.getStatus().name() : "N/A",
                    sessionCount,
                    userRegisteredEvents.contains(eventId),
                    event.getImagePath(),
                    event.getName(),  // Use name as description
                    event
            );

            System.out.println("✓ loadEventDetails completed in " + (System.currentTimeMillis() - start) + " ms");
            return dto;

        } catch (Exception e) {
            System.err.println("✗ Error loading event details: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Invalidate cache
     */
    public void invalidateCache() {
        eventCache.clear();
    }
}

