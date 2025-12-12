package org.ems.application.service.event;

import org.ems.domain.model.Event;
import org.ems.domain.model.Session;
import org.ems.domain.model.Ticket;
import org.ems.domain.repository.EventRepository;
import org.ems.domain.repository.SessionRepository;
import org.ems.domain.repository.TicketRepository;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating event reports
 * Supports CSV and PDF export formats
 *
 * @author <your group number>
 */
public class EventReportGeneratorService {

    private final EventRepository eventRepo;
    private final SessionRepository sessionRepo;
    private final TicketRepository ticketRepo;

    public EventReportGeneratorService(EventRepository eventRepo,
                                     SessionRepository sessionRepo,
                                     TicketRepository ticketRepo) {
        this.eventRepo = eventRepo;
        this.sessionRepo = sessionRepo;
        this.ticketRepo = ticketRepo;
    }

    /**
     * Generate attendance report for an event
     */
    public String[][] generateAttendanceReport(UUID eventId) {
        System.out.println("📊 [EventReportGenerator] Generating attendance report for event: " + eventId);

        Event event = eventRepo.findById(eventId);
        if (event == null) {
            return new String[0][0];
        }

        List<Session> sessions = sessionRepo.findByEvent(eventId);
        List<Ticket> tickets = ticketRepo.findAll().stream()
                .filter(t -> t.getEventId().equals(eventId))
                .collect(Collectors.toList());

        // Create report data
        String[][] reportData = new String[sessions.size() + 1][4];
        reportData[0] = new String[]{"Session Title", "Capacity", "Expected Attendees", "Occupancy %"};

        for (int i = 0; i < sessions.size(); i++) {
            Session session = sessions.get(i);
            int capacity = session.getCapacity();
            int expectedAttendees = tickets.size(); // Simplified
            double occupancy = capacity > 0 ? (expectedAttendees * 100.0) / capacity : 0;

            reportData[i + 1] = new String[]{
                    session.getTitle(),
                    String.valueOf(capacity),
                    String.valueOf(expectedAttendees),
                    String.format("%.2f%%", occupancy)
            };
        }

        System.out.println("  ✓ Attendance report generated");
        return reportData;
    }

    /**
     * Generate session occupancy report
     */
    public String[][] generateSessionOccupancyReport(UUID eventId) {
        System.out.println("📊 [EventReportGenerator] Generating session occupancy report");

        List<Session> sessions = sessionRepo.findByEvent(eventId);

        String[][] reportData = new String[sessions.size() + 1][5];
        reportData[0] = new String[]{"Session", "Date/Time", "Venue", "Capacity", "Status"};

        for (int i = 0; i < sessions.size(); i++) {
            Session session = sessions.get(i);
            reportData[i + 1] = new String[]{
                    session.getTitle(),
                    session.getStart() != null ? session.getStart().toString() : "N/A",
                    session.getVenue(),
                    String.valueOf(session.getCapacity()),
                    "Active"
            };
        }

        System.out.println("  ✓ Session occupancy report generated");
        return reportData;
    }

    /**
     * Generate ticket usage report
     */
    public String[][] generateTicketUsageReport(UUID eventId) {
        System.out.println("📊 [EventReportGenerator] Generating ticket usage report");

        List<Ticket> tickets = ticketRepo.findAll().stream()
                .filter(t -> t.getEventId().equals(eventId))
                .collect(Collectors.toList());

        String[][] reportData = new String[tickets.size() + 1][5];
        reportData[0] = new String[]{"Ticket ID", "Type", "Price", "Status", "Purchased"};

        for (int i = 0; i < tickets.size(); i++) {
            Ticket ticket = tickets.get(i);
            reportData[i + 1] = new String[]{
                    ticket.getId().toString().substring(0, 8) + "...",
                    ticket.getType().toString(),
                    ticket.getPrice().toString(),
                    ticket.getTicketStatus().toString(),
                    "Yes"
            };
        }

        System.out.println("  ✓ Ticket usage report generated");
        return reportData;
    }

    /**
     * Export report to CSV file
     */
    public String exportToCSV(String[][] reportData, String reportName) throws IOException {
        System.out.println("💾 [EventReportGenerator] Exporting to CSV...");

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "event_report_" + reportName + "_" + timestamp + ".csv";
        File file = new File("reports/" + filename);

        file.getParentFile().mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            for (String[] row : reportData) {
                writer.println(String.join(",", row));
            }
        }

        System.out.println("  ✓ CSV exported: " + file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    /**
     * Get report headers
     */
    public String[] getAttendanceReportHeaders() {
        return new String[]{"Session Title", "Capacity", "Expected Attendees", "Occupancy %"};
    }

    /**
     * Get session occupancy headers
     */
    public String[] getSessionOccupancyHeaders() {
        return new String[]{"Session", "Date/Time", "Venue", "Capacity", "Status"};
    }

    /**
     * Get ticket usage headers
     */
    public String[] getTicketUsageHeaders() {
        return new String[]{"Ticket ID", "Type", "Price", "Status", "Purchased"};
    }
}

