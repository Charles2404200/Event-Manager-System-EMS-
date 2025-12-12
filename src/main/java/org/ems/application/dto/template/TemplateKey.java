package org.ems.application.dto.template;

import org.ems.domain.model.enums.TicketType;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * TemplateKey - Composite key for template statistics cache
 * Used as key in templateAssignedCountCache map
 * Identifies unique template by: eventId + sessionId + type + price
 *
 * @author EMS Team
 */
public final class TemplateKey {
    private final UUID eventId;
    private final UUID sessionId;
    private final TicketType type;
    private final BigDecimal price;

    public TemplateKey(UUID eventId, UUID sessionId, TicketType type, BigDecimal price) {
        this.eventId = eventId;
        this.sessionId = sessionId;
        this.type = type;
        this.price = price;
    }

    public UUID getEventId() { return eventId; }
    public UUID getSessionId() { return sessionId; }
    public TicketType getType() { return type; }
    public BigDecimal getPrice() { return price; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemplateKey key)) return false;
        return Objects.equals(eventId, key.eventId)
                && Objects.equals(sessionId, key.sessionId)
                && type == key.type
                && Objects.equals(price, key.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, sessionId, type, price);
    }

    @Override
    public String toString() {
        return "TemplateKey{" +
                "eventId=" + eventId +
                ", sessionId=" + sessionId +
                ", type=" + type +
                ", price=" + price +
                '}';
    }
}

