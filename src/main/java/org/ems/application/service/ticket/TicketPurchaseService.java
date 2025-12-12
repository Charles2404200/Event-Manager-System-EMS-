package org.ems.application.service.ticket;

import org.ems.application.dto.ticket.TicketPurchaseRequestDTO;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Ticket;
import org.ems.domain.model.enums.PaymentStatus;
import org.ems.domain.model.enums.TicketStatus;
import org.ems.domain.repository.TicketRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TicketPurchaseService - Handles ticket purchase workflow
 * Single Responsibility: Load available tickets + create purchased ticket
 *
 * @author EMS Team
 */
public class TicketPurchaseService {

    private final TicketRepository ticketRepo;

    public TicketPurchaseService(TicketRepository ticketRepo) {
        this.ticketRepo = ticketRepo;
    }

    /**
     * Load available tickets (not assigned to anyone) for an event
     */
    public List<Ticket> loadAvailableTickets(UUID eventId) {
        long start = System.currentTimeMillis();
        System.out.println("[TicketPurchaseService] loadAvailableTickets(" + eventId + ") starting...");

        try {
            if (ticketRepo == null) {
                System.err.println("✗ ticketRepo is null!");
                return Collections.emptyList();
            }

            List<Ticket> allTickets = ticketRepo.findByEvent(eventId);
            System.out.println("  ✓ Loaded " + allTickets.size() + " tickets for event");

            // Filter: only tickets without attendee assignment (available for purchase)
            List<Ticket> available = allTickets.stream()
                    .filter(t -> t.getAttendeeId() == null)
                    .collect(Collectors.toList());

            System.out.println("  ✓ " + available.size() + " available tickets in " +
                    (System.currentTimeMillis() - start) + " ms");
            return available;

        } catch (Exception e) {
            System.err.println("✗ Error loading available tickets: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Create and save new ticket from template
     */
    public Ticket purchaseTicket(TicketPurchaseRequestDTO request, Attendee attendee) {
        long start = System.currentTimeMillis();
        System.out.println("[TicketPurchaseService] purchaseTicket(" + request + ") starting...");

        try {
            if (ticketRepo == null) {
                throw new IllegalStateException("ticketRepo is null");
            }

            // Get template ticket
            Ticket template = ticketRepo.findById(request.templateTicketId);
            if (template == null) {
                throw new IllegalArgumentException("Template ticket not found: " + request.templateTicketId);
            }

            // Create new ticket from template
            Ticket newTicket = new Ticket();
            newTicket.setId(UUID.randomUUID());
            newTicket.setAttendeeId(attendee.getId());
            newTicket.setEventId(template.getEventId());
            newTicket.setType(template.getType());
            newTicket.setPrice(template.getPrice());
            newTicket.setTicketStatus(TicketStatus.ACTIVE);
            newTicket.setPaymentStatus(PaymentStatus.PAID);

            // Generate QR code data
            String qrCodeData = "QR-" + newTicket.getId().toString().substring(0, 12).toUpperCase();
            newTicket.setQrCodeData(qrCodeData);

            // Save to DB
            long saveStart = System.currentTimeMillis();
            ticketRepo.save(newTicket);
            System.out.println("  ✓ Ticket saved in " + (System.currentTimeMillis() - saveStart) + " ms");

            System.out.println("✓ purchaseTicket completed in " + (System.currentTimeMillis() - start) + " ms");
            return newTicket;

        } catch (Exception e) {
            System.err.println("✗ Error purchasing ticket: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to purchase ticket: " + e.getMessage(), e);
        }
    }
}

