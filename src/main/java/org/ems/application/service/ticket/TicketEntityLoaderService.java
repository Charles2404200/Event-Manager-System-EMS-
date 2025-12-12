package org.ems.application.service.ticket;

import org.ems.domain.model.Event;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Ticket;
import org.ems.domain.repository.EventRepository;
import org.ems.domain.repository.AttendeeRepository;

import java.util.*;

/**
 * TicketEntityLoaderService - Loads and caches entities for UI display
 * Single Responsibility: Batch load events/attendees and cache them
 * Ensures entity names are always available before conversion to UI DTOs
 *
 * @author EMS Team
 */
public class TicketEntityLoaderService {

    private final TicketCacheManager cacheManager;
    private final EventRepository eventRepo;
    private final AttendeeRepository attendeeRepo;

    public TicketEntityLoaderService(TicketCacheManager cacheManager,
                                     EventRepository eventRepo,
                                     AttendeeRepository attendeeRepo) {
        this.cacheManager = cacheManager;
        this.eventRepo = eventRepo;
        this.attendeeRepo = attendeeRepo;
    }

    /**
     * Load and cache all events if tickets exist
     * Should be called BEFORE converting tickets to UI DTOs
     *
     * @param tickets source tickets that reference events
     */
    public void preloadEventsForTickets(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return;
        }

        // Collect unique event IDs
        Set<UUID> eventIds = new HashSet<>();
        for (Ticket ticket : tickets) {
            if (ticket.getEventId() != null) {
                eventIds.add(ticket.getEventId());
            }
        }

        if (!eventIds.isEmpty() && eventRepo != null) {
            try {
                long start = System.currentTimeMillis();
                List<Event> allEvents = eventRepo.findAll();
                cacheManager.cacheAllEvents(allEvents);
                System.out.println("[TicketEntityLoaderService] Preloaded " + allEvents.size() +
                        " events in " + (System.currentTimeMillis() - start) + " ms");
            } catch (Exception e) {
                System.err.println("[TicketEntityLoaderService] Error preloading events: " + e.getMessage());
            }
        }
    }

    /**
     * Load and cache all attendees if tickets exist
     * Should be called BEFORE converting tickets to UI DTOs
     *
     * @param tickets source tickets that reference attendees
     */
    public void preloadAttendeesForTickets(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return;
        }

        // Collect unique attendee IDs
        Set<UUID> attendeeIds = new HashSet<>();
        for (Ticket ticket : tickets) {
            if (ticket.getAttendeeId() != null) {
                attendeeIds.add(ticket.getAttendeeId());
            }
        }

        if (!attendeeIds.isEmpty() && attendeeRepo != null) {
            try {
                long start = System.currentTimeMillis();
                List<Attendee> allAttendees = attendeeRepo.findAll();
                cacheManager.cacheAllAttendees(allAttendees);
                System.out.println("[TicketEntityLoaderService] Preloaded " + allAttendees.size() +
                        " attendees in " + (System.currentTimeMillis() - start) + " ms");
            } catch (Exception e) {
                System.err.println("[TicketEntityLoaderService] Error preloading attendees: " + e.getMessage());
            }
        }
    }

    /**
     * Load and cache all events and attendees for tickets
     * Convenience method to preload both in one call
     *
     * @param tickets source tickets
     */
    public void preloadAllEntitiesForTickets(List<Ticket> tickets) {
        preloadEventsForTickets(tickets);
        preloadAttendeesForTickets(tickets);
    }
}

