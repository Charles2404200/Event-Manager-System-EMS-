package org.ems.domain.dto;

/**
 * TicketRow - Data Transfer Object cho ticket display trong table
 * @author EMS Team
 */
public class TicketRow {
    private String id;
    private String attendee;
    private String event;
    private String session;
    private String type;
    private String price;
    private String status;

    public TicketRow(String id, String attendee, String event, String session, String type, String price, String status) {
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
}

