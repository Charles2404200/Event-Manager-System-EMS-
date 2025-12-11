package org.ems.application.service;

import org.ems.application.dto.DashboardEventFilteringResultDTO;
import org.ems.domain.model.Event;
import org.ems.domain.model.Ticket;

import java.time.LocalDate;
import java.util.*;

/**
 * Service for filtering and formatting events for dashboard
 * Implements Single Responsibility Principle - only handles event filtering/sorting
 * @author <your group number>
 */
public class DashboardEventFilteringService {

    /**
     * Filter upcoming events for attendee based on tickets and date
     * @param tickets User's tickets
     * @param allEvents All available events
     * @return DTO with filtered upcoming events and statistics
     */
    public DashboardEventFilteringResultDTO filterUpcomingEventsForAttendee(
            List<Ticket> tickets, List<Event> allEvents) {

        long start = System.currentTimeMillis();
        System.out.println("🔎 [DashboardEventFilteringService] Filtering upcoming events");

        if (tickets == null || tickets.isEmpty()) {
            System.out.println("  ℹ No tickets found");
            return new DashboardEventFilteringResultDTO(new ArrayList<>(), 0, 0);
        }

        if (allEvents == null || allEvents.isEmpty()) {
            System.out.println("  ℹ No events available");
            return new DashboardEventFilteringResultDTO(new ArrayList<>(), 0, 0);
        }

        // Create event ID -> Event map for O(1) lookup
        Map<UUID, Event> eventMap = new HashMap<>();
        for (Event event : allEvents) {
            eventMap.put(event.getId(), event);
        }

        LocalDate today = LocalDate.now();
        List<Event> upcomingEvents = new ArrayList<>();
        Set<UUID> ticketedEventIds = new HashSet<>();

        // Filter upcoming events from tickets
        for (Ticket ticket : tickets) {
            if (ticket.getEventId() != null && !ticketedEventIds.contains(ticket.getEventId())) {
                Event event = eventMap.get(ticket.getEventId());
                if (event != null && event.getStartDate() != null && event.getStartDate().isAfter(today)) {
                    upcomingEvents.add(event);
                    ticketedEventIds.add(event.getId());
                }
            }
        }

        // Sort by date
        upcomingEvents.sort((e1, e2) -> {
            if (e1.getStartDate() == null || e2.getStartDate() == null) return 0;
            return e1.getStartDate().compareTo(e2.getStartDate());
        });

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("  ✓ Filtered " + upcomingEvents.size() + " upcoming events from " + tickets.size() +
                         " tickets in " + elapsed + "ms");

        return new DashboardEventFilteringResultDTO(upcomingEvents, tickets.size(), upcomingEvents.size());
    }

    /**
     * Format event for display
     * @param event The event to format
     * @return Formatted event information
     */
    public String formatEventForDisplay(Event event) {
        if (event == null) {
            return "Unknown Event";
        }
        return (event.getName() != null ? event.getName() : "Unknown Event") +
               " (" + (event.getStartDate() != null ? event.getStartDate() : "TBD") + ")";
    }
}

