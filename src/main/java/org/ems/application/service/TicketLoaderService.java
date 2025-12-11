package org.ems.application.service;

import org.ems.application.dto.TicketDisplayDTO;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Event;
import org.ems.domain.model.Ticket;
import org.ems.domain.repository.EventRepository;
import org.ems.domain.repository.TicketRepository;

import java.util.*;

/**
 * Service for loading ticket data
 * Implements Single Responsibility Principle - only handles data loading
 * Preserves batch loading optimization (single query per repository)
 * @author <your group number>
 */
public class TicketLoaderService {

    private final TicketRepository ticketRepo;
    private final EventRepository eventRepo;

    public TicketLoaderService(TicketRepository ticketRepo, EventRepository eventRepo) {
        this.ticketRepo = ticketRepo;
        this.eventRepo = eventRepo;
    }

    /**
     * Load all tickets for attendee - OPTIMIZED: Batch load with O(1) lookups
     * @param attendee The attendee user
     * @return List of tickets formatted for display
     * @throws TicketLoaderException if loading fails
     */
    public List<TicketDisplayDTO> loadAttendeeTickets(Attendee attendee) throws TicketLoaderException {
        long start = System.currentTimeMillis();
        System.out.println("📋 [TicketLoaderService] Loading tickets for " + attendee.getUsername());

        if (ticketRepo == null || eventRepo == null) {
            throw new TicketLoaderException("Repositories not available");
        }

        try {
            // Step 1: Load tickets (BATCH LOAD - single query)
            long ticketStart = System.currentTimeMillis();
            List<Ticket> allTicketsFromDb = ticketRepo.findByAttendee(attendee.getId());
            long ticketTime = System.currentTimeMillis() - ticketStart;
            System.out.println("  ✓ Tickets loaded in " + ticketTime + "ms: " + allTicketsFromDb.size());

            if (allTicketsFromDb.isEmpty()) {
                System.out.println("  ℹ No tickets found");
                return new ArrayList<>();
            }

            // Step 2: Load all events (BATCH LOAD - single query)
            long eventStart = System.currentTimeMillis();
            List<Event> allEvents = eventRepo.findAll();
            long eventTime = System.currentTimeMillis() - eventStart;
            System.out.println("  ✓ Events loaded in " + eventTime + "ms: " + allEvents.size());

            // Step 3: Create event map for O(1) lookup (no nested queries!)
            Map<UUID, Event> eventMap = new HashMap<>();
            for (Event event : allEvents) {
                eventMap.put(event.getId(), event);
            }

            // Step 4: Convert to DTOs with O(1) lookups
            long convertStart = System.currentTimeMillis();
            List<TicketDisplayDTO> displayTickets = new ArrayList<>();

            for (Ticket ticket : allTicketsFromDb) {
                String eventName = "Unknown";

                // O(1) lookup instead of DB query
                if (ticket.getEventId() != null && eventMap.containsKey(ticket.getEventId())) {
                    Event event = eventMap.get(ticket.getEventId());
                    if (event != null && event.getName() != null) {
                        eventName = event.getName();
                    }
                }

                displayTickets.add(new TicketDisplayDTO(
                        ticket.getId().toString().substring(0, 8),
                        eventName,
                        "Event Ticket",
                        ticket.getType() != null ? ticket.getType().name() : "N/A",
                        ticket.getPrice() != null ? "$" + ticket.getPrice() : "$0",
                        ticket.getTicketStatus() != null ? ticket.getTicketStatus().name() : "N/A",
                        "2025-12-03",
                        ticket.getQrCodeData() != null ? ticket.getQrCodeData() : "N/A"
                ));
            }

            long convertTime = System.currentTimeMillis() - convertStart;
            System.out.println("  ✓ Converted to DTOs in " + convertTime + "ms");

            long totalTime = System.currentTimeMillis() - start;
            System.out.println("  ✓ All tickets loaded in " + totalTime + "ms");

            return displayTickets;

        } catch (Exception e) {
            String message = "Failed to load tickets: " + e.getMessage();
            System.err.println("✗ " + message);
            e.printStackTrace();
            throw new TicketLoaderException(message, e);
        }
    }

    /**
     * Custom exception for ticket loading errors
     */
    public static class TicketLoaderException extends Exception {
        public TicketLoaderException(String message) {
            super(message);
        }

        public TicketLoaderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

