package org.ems.domain.model;

import org.ems.domain.model.enums.Role;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Attendee extends Person {

    private List<UUID> registeredEventIds = new ArrayList<>();
    private List<UUID> registeredSessionIds = new ArrayList<>();
    private List<UUID> ticketIds = new ArrayList<>();
    private List<String> activityHistory = new ArrayList<>();

    public Attendee() {
        super();
        setRole(Role.ATTENDEE);
    }

    public Attendee(String fullName, LocalDate dob,
                    String email, String phone,
                    String username, String passwordHash) {
        super(fullName, dob, email, phone, username, passwordHash, Role.ATTENDEE);
    }

    public List<UUID> getRegisteredEventIds() { return registeredEventIds; }
    public void addEvent(UUID eventId) { registeredEventIds.add(eventId); }
    public void removeEvent(UUID eventId) { registeredEventIds.remove(eventId); }

    public List<UUID> getRegisteredSessionIds() { return registeredSessionIds; }
    public void addSession(UUID sessionId) { registeredSessionIds.add(sessionId); }
    public void removeSession(UUID sessionId) { registeredSessionIds.remove(sessionId); }

    public List<UUID> getTicketIds() { return ticketIds; }
    public void addTicket(UUID ticketId) { ticketIds.add(ticketId); }

    public void addActivity(String description) {
        activityHistory.add(description);
    }

    public List<String> getActivityHistory() {
        return activityHistory;
    }
}
