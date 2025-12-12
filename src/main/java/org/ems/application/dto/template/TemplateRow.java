package org.ems.application.dto.template;

/**
 * TemplateRow - UI DTO for displaying ticket templates in table
 * Separates UI concerns from domain model
 *
 * @author EMS Team
 */
public class TemplateRow {
    private final String event;
    private final String session;
    private final String type;
    private final String price;
    private final String available;

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

    @Override
    public String toString() {
        return "TemplateRow{" +
                "event='" + event + '\'' +
                ", type='" + type + '\'' +
                ", price='" + price + '\'' +
                '}';
    }
}

