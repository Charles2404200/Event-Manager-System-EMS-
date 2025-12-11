package org.ems.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * TicketPurchaseRequestDTO - DTO for ticket purchase workflow
 *
 * @author EMS Team
 */
public class TicketPurchaseRequestDTO {
    public UUID eventId;
    public UUID attendeeId;
    public UUID templateTicketId;  // Template ticket to copy from
    public String ticketType;      // For display
    public BigDecimal price;       // For display

    public TicketPurchaseRequestDTO(UUID eventId, UUID attendeeId, UUID templateTicketId,
                                     String ticketType, BigDecimal price) {
        this.eventId = eventId;
        this.attendeeId = attendeeId;
        this.templateTicketId = templateTicketId;
        this.ticketType = ticketType;
        this.price = price;
    }

    @Override
    public String toString() {
        return "TicketPurchaseRequestDTO{" +
                "eventId=" + eventId +
                ", attendeeId=" + attendeeId +
                ", ticketType='" + ticketType + '\'' +
                ", price=$" + price +
                '}';
    }
}

