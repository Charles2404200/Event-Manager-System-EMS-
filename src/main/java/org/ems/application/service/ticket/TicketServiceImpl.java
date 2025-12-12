package org.ems.application.service.ticket;

import org.ems.application.service.image.ImageService;
import org.ems.domain.model.enums.PaymentStatus;
import org.ems.domain.model.enums.TicketStatus;
import org.ems.domain.model.enums.TicketType;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Session;
import org.ems.domain.model.Ticket;
import org.ems.domain.repository.AttendeeRepository;
import org.ems.domain.repository.EventRepository;
import org.ems.domain.repository.SessionRepository;
import org.ems.domain.repository.TicketRepository;
import org.ems.infrastructure.util.QRCodeUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepo;
    private final SessionRepository sessionRepo;
    private final AttendeeRepository attendeeRepo;
    private final EventRepository eventRepo;
    private final ImageService imageService;

    public TicketServiceImpl(
            TicketRepository ticketRepo,
            SessionRepository sessionRepo,
            AttendeeRepository attendeeRepo,
            EventRepository eventRepo,
            ImageService imageService
    ) {
        this.ticketRepo = ticketRepo;
        this.sessionRepo = sessionRepo;
        this.attendeeRepo = attendeeRepo;
        this.eventRepo = eventRepo;
        this.imageService = imageService;
    }

    /**
     * Issue ticket for event (not session-specific)
     * Attendee can later register for sessions in the event with this ticket
     *
     * @param attendeeId UUID of attendee
     * @param eventId UUID of event
     * @return Ticket object
     */
    @Override
    public Ticket issueTicket(UUID attendeeId, UUID eventId) {

        Attendee attendee = attendeeRepo.findById(attendeeId);
        // Note: No session check - tickets are event-level

        if (attendee == null) {
            throw new IllegalArgumentException("Attendee not found.");
        }


        Ticket t = new Ticket();
        t.setId(UUID.randomUUID());
        t.setAttendeeId(attendeeId);
        t.setEventId(eventId);
        // Note: sessionId not set - tickets are now event-level

        // auto assign type
        t.setType(TicketType.GENERAL);
        t.setTicketStatus(TicketStatus.ACTIVE);
        t.setPaymentStatus(PaymentStatus.PAID);

        // price rule (you can modify this formula freely)
        t.setPrice(BigDecimal.valueOf(49.99));

        // Generate unique QR code data (base64 encoded)
        // Each ticket gets a random, unique QR code
        String qrCodeData = QRCodeUtil.generateQRCodeData(t.getId(), attendeeId, eventId);
        t.setQrCodeData(qrCodeData);

        System.out.println("✓ Ticket created (Event-level, not session-specific)");
        System.out.println("  Ticket ID: " + t.getId());
        System.out.println("  Event ID: " + eventId);
        System.out.println("  Attendee ID: " + attendeeId);
        System.out.println("  QR Code Data: " + qrCodeData);

        // persist to repo
        return ticketRepo.save(t);
    }

    // ... other methods ...

    // ... other methods ...

    @Override
    public Ticket updateTicket(Ticket t) {
        return ticketRepo.save(t);
    }

    @Override
    public boolean deleteTicket(UUID id) {
        ticketRepo.delete(id);
        return true;
    }

    @Override
    public List<Ticket> getTickets() {
        return ticketRepo.findAll();
    }

    @Override
    public List<Ticket> getTicketsByType(TicketType type) {
        return ticketRepo.findByType(type);
    }

    /**
     * Register a ticket holder for a session within the event
     * Attendee can register for multiple sessions using their ticket
     *
     * @param ticketId UUID of ticket (for event)
     * @param sessionId UUID of session (must be in same event)
     * @return true if registration successful
     */
    public boolean registerTicketForSession(UUID ticketId, UUID sessionId) {
        try {
            Ticket ticket = ticketRepo.findById(ticketId);
            if (ticket == null) {
                System.err.println("Ticket not found: " + ticketId);
                return false;
            }

            Session session = sessionRepo.findById(sessionId);
            if (session == null) {
                System.err.println("Session not found: " + sessionId);
                return false;
            }

            // Verify ticket is for the same event as session
            if (!ticket.getEventId().equals(session.getEventId())) {
                System.err.println("Ticket is for different event than session");
                return false;
            }

            // Verify ticket is still active
            if (ticket.getTicketStatus() != TicketStatus.ACTIVE) {
                System.err.println("Ticket is not active, status: " + ticket.getTicketStatus());
                return false;
            }

            // TODO: Insert into ticket_session_registration table
            // For now, we'll just mark the session as registered in attendee's session list
            if (ticket.getAttendeeId() != null) {
                Attendee attendee = attendeeRepo.findById(ticket.getAttendeeId());
                if (attendee != null) {
                    attendee.addSession(sessionId);
                    attendeeRepo.save(attendee);
                    System.out.println("✓ Ticket registered for session");
                    System.out.println("  Ticket: " + ticketId);
                    System.out.println("  Session: " + sessionId);
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            System.err.println("Error registering ticket for session: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // QR CODE IMAGE UPLOAD
    @Override
    public boolean uploadTicketQRCode(String filePath, UUID ticketId) {
        try {
            Ticket ticket = ticketRepo.findById(ticketId);
            if (ticket == null) {
                System.err.println("Ticket not found: " + ticketId);
                return false;
            }

            // Upload QR code image
            String uploadedPath = imageService.uploadTicketQRCode(filePath, ticketId);
            if (uploadedPath == null) {
                System.err.println("Failed to upload ticket QR code");
                return false;
            }

            System.out.println("Ticket QR code uploaded successfully for ticket: " + ticketId);
            System.out.println("QR code stored at: " + uploadedPath);
            return true;

        } catch (Exception e) {
            System.err.println("Error uploading ticket QR code: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean storeQRCodeImage(byte[] qrCodeData, UUID ticketId) {
        try {
            Ticket ticket = ticketRepo.findById(ticketId);
            if (ticket == null) {
                System.err.println("Ticket not found: " + ticketId);
                return false;
            }

            if (qrCodeData == null || qrCodeData.length == 0) {
                System.err.println("Invalid QR code data");
                return false;
            }

            // Store QR code as binary image
            String uploadedPath = imageService.uploadBinaryImage(
                    qrCodeData,
                    ticketId,
                    "TICKET",
                    "qrcode_" + ticketId + ".png"
            );

            if (uploadedPath == null) {
                System.err.println("Failed to store QR code image");
                return false;
            }

            System.out.println("Ticket QR code image stored successfully for ticket: " + ticketId);
            System.out.println("QR code image stored at: " + uploadedPath);
            return true;

        } catch (Exception e) {
            System.err.println("Error storing QR code image: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
