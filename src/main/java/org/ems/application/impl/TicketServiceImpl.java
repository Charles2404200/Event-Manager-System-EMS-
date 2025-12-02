package org.ems.application.impl;

import org.ems.application.service.TicketService;
import org.ems.domain.model.enums.PaymentStatus;
import org.ems.domain.model.enums.TicketStatus;
import org.ems.domain.model.enums.TicketType;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Session;
import org.ems.domain.model.Ticket;
import org.ems.domain.repository.AttendeeRepository;
import org.ems.domain.repository.SessionRepository;
import org.ems.domain.repository.TicketRepository;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepo;
    private final SessionRepository sessionRepo;
    private final AttendeeRepository attendeeRepo;

    public TicketServiceImpl(
            TicketRepository ticketRepo,
            SessionRepository sessionRepo,
            AttendeeRepository attendeeRepo
    ) {
        this.ticketRepo = ticketRepo;
        this.sessionRepo = sessionRepo;
        this.attendeeRepo = attendeeRepo;
    }

    /**
     * Auto-issue ticket whenever attendee registers to a session
     */
    @Override
    public Ticket issueTicket(UUID attendeeId, UUID sessionId) {

        Attendee attendee = attendeeRepo.findById(attendeeId);
        Session session = sessionRepo.findById(sessionId);

        if (attendee == null) {
            throw new IllegalArgumentException("Attendee not found.");
        }
        if (session == null) {
            throw new IllegalArgumentException("Session not found.");
        }

        Ticket t = new Ticket();
        t.setId(UUID.randomUUID());
        t.setAttendeeId(attendeeId);
        t.setSessionId(sessionId);
        t.setEventId(session.getEventId());

        // auto assign type
        t.setType(TicketType.GENERAL);
        t.setTicketStatus(TicketStatus.ACTIVE);
        t.setPaymentStatus(PaymentStatus.PAID);

        // price rule (you can modify this formula freely)
        t.setPrice(BigDecimal.valueOf(49.99));

        // generate QR code payload
        t.setQrCodeData(generateQr(attendeeId, sessionId));

        // persist to repo
        return ticketRepo.save(t);
    }

    private String generateQr(UUID attendeeId, UUID sessionId) {
        String raw = attendeeId + "|" + sessionId + "|" + System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }

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
}
