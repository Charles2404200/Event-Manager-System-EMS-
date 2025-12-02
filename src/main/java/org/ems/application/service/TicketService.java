package org.ems.application.service;

import org.ems.domain.model.Ticket;
import org.ems.domain.model.enums.TicketType;

import java.util.List;
import java.util.UUID;

public interface TicketService {
    Ticket issueTicket(UUID attendeeId, UUID sessionId);
    Ticket updateTicket(Ticket t);
    boolean deleteTicket(UUID id);
    List<Ticket> getTickets();
    List<Ticket> getTicketsByType(TicketType type);
}
