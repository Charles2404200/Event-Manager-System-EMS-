package org.ems.application.dto.attendee;

/**
 * DTO for displaying attendee information in tables/lists
 * Immutable representation of attendee data for UI layer
 * Implements Data Transfer Object pattern
 *
 * @author <your group number>
 */
public class AttendeeDisplayDTO {
    private final String attendeeId;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String username;
    private final int registeredEventCount;
    private final int registeredSessionCount;
    private final int ticketCount;
    private final String registrationDate;

    public AttendeeDisplayDTO(String attendeeId, String fullName, String email,
                             String phone, String username, int registeredEventCount,
                             int registeredSessionCount, int ticketCount, String registrationDate) {
        this.attendeeId = attendeeId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.registeredEventCount = registeredEventCount;
        this.registeredSessionCount = registeredSessionCount;
        this.ticketCount = ticketCount;
        this.registrationDate = registrationDate;
    }

    // Getters
    public String getAttendeeId() { return attendeeId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getUsername() { return username; }
    public int getRegisteredEventCount() { return registeredEventCount; }
    public int getRegisteredSessionCount() { return registeredSessionCount; }
    public int getTicketCount() { return ticketCount; }
    public String getRegistrationDate() { return registrationDate; }

    @Override
    public String toString() {
        return "AttendeeDisplayDTO{" +
                "attendeeId='" + attendeeId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", registeredEventCount=" + registeredEventCount +
                ", ticketCount=" + ticketCount +
                '}';
    }
}


