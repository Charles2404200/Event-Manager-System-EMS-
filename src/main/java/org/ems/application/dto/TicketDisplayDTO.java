package org.ems.application.dto;

/**
 * DTO for displaying ticket information in MyTickets table
 * Immutable representation of ticket data for UI layer
 * @author <your group number>
 */
public class TicketDisplayDTO {
    private final String ticketId;
    private final String eventName;
    private final String sessionName;
    private final String type;
    private final String price;
    private final String status;
    private final String purchaseDate;
    private final String qrCode;

    public TicketDisplayDTO(String ticketId, String eventName, String sessionName,
                           String type, String price, String status, String purchaseDate, String qrCode) {
        this.ticketId = ticketId;
        this.eventName = eventName;
        this.sessionName = sessionName;
        this.type = type;
        this.price = price;
        this.status = status;
        this.purchaseDate = purchaseDate;
        this.qrCode = qrCode;
    }

    // Getters
    public String getTicketId() { return ticketId; }
    public String getEventName() { return eventName; }
    public String getSessionName() { return sessionName; }
    public String getType() { return type; }
    public String getPrice() { return price; }
    public String getStatus() { return status; }
    public String getPurchaseDate() { return purchaseDate; }
    public String getQrCode() { return qrCode; }

    @Override
    public String toString() {
        return "TicketDisplayDTO{" +
                "ticketId='" + ticketId + '\'' +
                ", eventName='" + eventName + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}

