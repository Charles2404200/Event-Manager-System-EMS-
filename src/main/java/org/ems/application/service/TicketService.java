package org.ems.application.service;

import org.ems.domain.model.Ticket;
import org.ems.domain.model.enums.TicketType;

import java.util.List;
import java.util.UUID;

public interface TicketService {
    /**
     * Issue ticket for an event (not session-specific)
     * Attendee can later register for multiple sessions with this ticket
     *
     * @param attendeeId UUID of attendee
     * @param eventId UUID of event
     * @return Ticket object
     */
    Ticket issueTicket(UUID attendeeId, UUID eventId);

    Ticket updateTicket(Ticket t);
    boolean deleteTicket(UUID id);
    List<Ticket> getTickets();
    List<Ticket> getTicketsByType(TicketType type);

    /**
     * Register a ticket for a session within the event
     *
     * @param ticketId UUID of ticket
     * @param sessionId UUID of session (must be in same event)
     * @return true if registration successful
     */
    boolean registerTicketForSession(UUID ticketId, UUID sessionId);

    /**
     * Upload QR code image for a ticket.
     *
     * @param filePath Path to the QR code image file
     * @param ticketId Ticket UUID
     * @return true if upload successful, false otherwise
     */
    boolean uploadTicketQRCode(String filePath, UUID ticketId);

    /**
     * Store QR code data as binary image.
     *
     * @param qrCodeData Binary QR code image data
     * @param ticketId Ticket UUID
     * @return true if storage successful, false otherwise
     */
    boolean storeQRCodeImage(byte[] qrCodeData, UUID ticketId);
}
