package org.ems.application.service;

import org.ems.application.dto.TemplateRow;
import org.ems.application.dto.TicketRow;
import org.ems.domain.model.Event;
import org.ems.domain.model.Ticket;

import java.util.UUID;

/**
 * TicketDataConverterService - Converts domain models to UI DTOs
 * Single Responsibility: Transform data models into UI-friendly format
 * Separates UI concerns from domain logic
 *
 * @author EMS Team
 */
public class TicketDataConverterService {

    private final TicketCacheManager cacheManager;

    public TicketDataConverterService(TicketCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Convert Ticket (domain model) to TemplateRow (UI DTO)
     * Format: Event Name | Session Name | Type | Price | Available Count
     */
    public TemplateRow convertToTemplateRow(Ticket ticket, long assignedCount) {
        if (ticket == null) {
            return null;
        }

        UUID eventId = ticket.getEventId();
        String eventName = "Unknown";
        String sessionName = "N/A";

        // Try to get event name from cache
        Event evt = eventId != null ? cacheManager.getEventFromCache(eventId) : null;
        if (evt != null && evt.getName() != null) {
            eventName = evt.getName();
        }

        // Calculate available = total 100 - assigned count
        long available = 100 - assignedCount;

        String priceStr = ticket.getPrice() != null ? "$" + ticket.getPrice() : "$0";
        String typeStr = ticket.getType() != null ? ticket.getType().name() : "N/A";

        return new TemplateRow(
                eventName,
                sessionName,
                typeStr,
                priceStr,
                String.valueOf(available)
        );
    }

    /**
     * Convert Ticket (domain model) to TicketRow (UI DTO)
     * Format: ID | Attendee | Event | Session | Type | Price | Status
     */
    public TicketRow convertToTicketRow(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        UUID attendeeId = ticket.getAttendeeId();
        UUID eventId = ticket.getEventId();

        String attendeeName = getAttendeeName(attendeeId);
        String eventName = getEventName(eventId);
        String sessionName = "N/A"; // Sessions removed from ticket design

        String idStr = ticket.getId() != null ? ticket.getId().toString().substring(0, 8) : "N/A";
        String typeStr = ticket.getType() != null ? ticket.getType().name() : "N/A";
        String priceStr = ticket.getPrice() != null ? "$" + ticket.getPrice() : "$0";
        String statusStr = ticket.getTicketStatus() != null ? ticket.getTicketStatus().name() : "N/A";

        return new TicketRow(
                idStr,
                attendeeName,
                eventName,
                sessionName,
                typeStr,
                priceStr,
                statusStr
        );
    }

    /**
     * Get attendee name from cache, fallback to "Unknown"
     */
    private String getAttendeeName(UUID attendeeId) {
        if (attendeeId == null) {
            return "Unknown";
        }
        var attendee = cacheManager.getAttendeeFromCache(attendeeId);
        return attendee != null && attendee.getFullName() != null ?
                attendee.getFullName() : "Unknown";
    }

    /**
     * Get event name from cache, fallback to "Unknown"
     */
    private String getEventName(UUID eventId) {
        if (eventId == null) {
            return "Unknown";
        }
        var evt = cacheManager.getEventFromCache(eventId);
        return evt != null && evt.getName() != null ? evt.getName() : "Unknown";
    }
}

