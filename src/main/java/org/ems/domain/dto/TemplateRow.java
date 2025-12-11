package org.ems.domain.dto;

/**
 * TemplateRow - Data Transfer Object cho ticket template display trong table
 * @author EMS Team
 */
public class TemplateRow {
    private String event;
    private String session;
    private String type;
    private String price;
    private String available;

    public TemplateRow(String event, String session, String type, String price, String available) {
        this.event = event;
        this.session = session;
        this.type = type;
        this.price = price;
        this.available = available;
    }

    public String getEvent() { return event; }
    public String getSession() { return session; }
    public String getType() { return type; }
    public String getPrice() { return price; }
    public String getAvailable() { return available; }
}

