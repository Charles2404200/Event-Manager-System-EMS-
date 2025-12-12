package org.ems.application.dto.schedule;

import org.ems.domain.model.Attendee;
import org.ems.domain.model.Event;
import org.ems.domain.model.Session;
import org.ems.domain.model.Ticket;

import java.util.Date;
import java.util.List;

/**
 * DTO for attendee schedule export data
 * Encapsulates all data needed for schedule export
 * Implements Data Transfer Object pattern
 *
 * @author <your group number>
 */
public class ScheduleExportDTO {
    private final Attendee attendee;
    private final List<Ticket> tickets;
    private final List<Event> registeredEvents;
    private final List<Session> registeredSessions;
    private final Date exportDate;

    public ScheduleExportDTO(Attendee attendee,
                            List<Ticket> tickets,
                            List<Event> registeredEvents,
                            List<Session> registeredSessions,
                            Date exportDate) {
        this.attendee = attendee;
        this.tickets = tickets;
        this.registeredEvents = registeredEvents;
        this.registeredSessions = registeredSessions;
        this.exportDate = exportDate;
    }

    // Getters
    public Attendee getAttendee() {
        return attendee;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public List<Event> getRegisteredEvents() {
        return registeredEvents;
    }

    public List<Session> getRegisteredSessions() {
        return registeredSessions;
    }

    public Date getExportDate() {
        return exportDate;
    }

    // Summary methods
    public int getTotalEvents() {
        return registeredEvents != null ? registeredEvents.size() : 0;
    }

    public int getTotalSessions() {
        return registeredSessions != null ? registeredSessions.size() : 0;
    }

    public int getTotalTickets() {
        return tickets != null ? tickets.size() : 0;
    }

    @Override
    public String toString() {
        return "ScheduleExportDTO{" +
                "attendee=" + (attendee != null ? attendee.getFullName() : "N/A") +
                ", tickets=" + getTotalTickets() +
                ", registeredEvents=" + getTotalEvents() +
                ", registeredSessions=" + getTotalSessions() +
                ", exportDate=" + exportDate +
                '}';
    }
}

