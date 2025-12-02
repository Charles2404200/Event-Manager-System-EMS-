package org.ems.application.impl;

import org.ems.application.service.ReportingService;
import org.ems.domain.dto.SummaryReport;
import org.ems.domain.model.enums.PaymentStatus;
import org.ems.domain.model.enums.TicketStatus;
import org.ems.domain.model.Event;
import org.ems.domain.model.Ticket;
import org.ems.domain.repository.*;

import java.util.*;
import java.util.stream.Collectors;

public class ReportingServiceImpl implements ReportingService {

    private final EventRepository eventRepo;
    private final SessionRepository sessionRepo;
    private final AttendeeRepository attendeeRepo;
    private final TicketRepository ticketRepo;

    public ReportingServiceImpl(
            EventRepository eventRepo,
            SessionRepository sessionRepo,
            AttendeeRepository attendeeRepo,
            TicketRepository ticketRepo
    ) {
        this.eventRepo = eventRepo;
        this.sessionRepo = sessionRepo;
        this.attendeeRepo = attendeeRepo;
        this.ticketRepo = ticketRepo;
    }

    @Override
    public SummaryReport generateSummary() {

        SummaryReport r = new SummaryReport();

        List<Event> events = eventRepo.findAll();
        List<Ticket> tickets = ticketRepo.findAll();

        // BASIC COUNTS
        r.totalEvents = events.size();
        r.totalSessions = sessionRepo.findAll().size();
        r.totalAttendees = attendeeRepo.findAll().size();
        r.totalTickets = tickets.size();

        // TOTAL REVENUE
        r.totalRevenue = tickets.stream()
                .filter(t -> t.getPaymentStatus() == PaymentStatus.PAID)
                .mapToDouble(t -> t.getPrice().doubleValue())
                .sum();

        // REVENUE BY EVENT
        r.revenueByEvent = tickets.stream()
                .filter(t -> t.getPaymentStatus() == PaymentStatus.PAID)
                .filter(t -> t.getEventId() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getEventId().toString(),
                        Collectors.summingDouble(t -> t.getPrice().doubleValue())
                ));

        // REVENUE BY TYPE
        r.revenueByType = tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getType().name(),
                        Collectors.summingDouble(t -> t.getPrice().doubleValue())
                ));

        // TICKET USAGE PERCENTAGE
        long used = tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.USED)
                .count();

        r.ticketUsedPercent = tickets.isEmpty() ? 0 : (used * 100.0 / tickets.size());

        // TOP EVENTS BY TICKET COUNT
        Map<String, Long> top = tickets.stream()
                .filter(t -> t.getEventId() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getEventId().toString(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        r.topEvents = top;

        return r;
    }
}

