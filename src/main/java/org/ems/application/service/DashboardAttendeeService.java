package org.ems.application.service;

import org.ems.application.dto.DashboardAttendeeContentDTO;
import org.ems.application.dto.DashboardEventFilteringResultDTO;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Event;
import org.ems.domain.model.Ticket;
import org.ems.domain.repository.EventRepository;
import org.ems.domain.repository.TicketRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for loading attendee-specific dashboard content
 * Implements Single Responsibility Principle - only handles attendee content
 * @author <your group number>
 */
public class DashboardAttendeeService {

    private final TicketRepository ticketRepo;
    private final EventRepository eventRepo;
    private final DashboardEventFilteringService eventFilteringService;

    public DashboardAttendeeService(TicketRepository ticketRepo, EventRepository eventRepo) {
        this.ticketRepo = ticketRepo;
        this.eventRepo = eventRepo;
        this.eventFilteringService = new DashboardEventFilteringService();
    }

    /**
     * Load attendee dashboard content (upcoming events, tickets)
     * @param attendee The attendee user
     * @return DTO with attendee content
     * @throws DashboardException if loading fails
     */
    public DashboardAttendeeContentDTO loadAttendeeContent(Attendee attendee) throws DashboardException {
        long start = System.currentTimeMillis();
        System.out.println("📋 [DashboardAttendeeService] Loading content for " + attendee.getUsername());

        try {
            if (ticketRepo == null || eventRepo == null) {
                throw new DashboardException("Repositories not available for attendee content");
            }

            // Step 1: Load tickets
            long ticketStart = System.currentTimeMillis();
            List<Ticket> tickets = ticketRepo.findByAttendee(attendee.getId());
            long ticketTime = System.currentTimeMillis() - ticketStart;
            System.out.println("  ✓ Tickets loaded in " + ticketTime + "ms: " + tickets.size());

            // Step 2: Load all events
            long eventStart = System.currentTimeMillis();
            List<Event> allEvents = eventRepo.findAll();
            long eventTime = System.currentTimeMillis() - eventStart;
            System.out.println("  ✓ Events loaded in " + eventTime + "ms: " + allEvents.size());

            // Step 3: Filter upcoming events
            DashboardEventFilteringResultDTO filterResult =
                    eventFilteringService.filterUpcomingEventsForAttendee(tickets, allEvents);

            long totalTime = System.currentTimeMillis() - start;
            System.out.println("  ✓ Attendee content loaded in " + totalTime + "ms");

            return new DashboardAttendeeContentDTO(
                    filterResult.getUpcomingEvents(),
                    filterResult.getTotalTickets(),
                    filterResult.getUpcomingEventCount()
            );

        } catch (Exception e) {
            String message = "Failed to load attendee content: " + e.getMessage();
            System.err.println("✗ " + message);
            e.printStackTrace();
            throw new DashboardException(message, e);
        }
    }

    /**
     * Custom exception for dashboard errors
     */
    public static class DashboardException extends Exception {
        public DashboardException(String message) {
            super(message);
        }

        public DashboardException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

