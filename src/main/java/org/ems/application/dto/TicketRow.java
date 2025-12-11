package org.ems.application.dto;

/**
 * TicketRow - UI DTO for displaying assigned tickets in table
 * Separates UI concerns from domain model
 *
 * @author EMS Team
 */
public class TicketRow {
    private final String id;
    private final String attendee;
    private final String event;
    private final String session;
    private final String type;
    private final String price;
    private final String status;

    public TicketRow(String id, String attendee, String event, String session,
                     String type, String price, String status) {
        this.id = id;
        this.attendee = attendee;
        this.event = event;
        this.session = session;
        this.type = type;
        this.price = price;
        this.status = status;
    }

    public String getId() { return id; }
    public String getAttendee() { return attendee; }
    public String getEvent() { return event; }
    public String getSession() { return session; }
    public String getType() { return type; }
    public String getPrice() { return price; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return "TicketRow{" +
                "id='" + id + '\'' +
                ", attendee='" + attendee + '\'' +
                ", event='" + event + '\'' +
                ", type='" + type + '\'' +
                ", price='" + price + '\'' +
                '}';
    }
}

