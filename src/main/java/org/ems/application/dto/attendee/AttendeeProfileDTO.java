package org.ems.application.dto.attendee;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for displaying detailed attendee profile information
 * Includes personal details, registration statistics, and activity history
 * Implements Data Transfer Object pattern
 *
 * @author <your group number>
 */
public class AttendeeProfileDTO {
    private final String attendeeId;
    private final String fullName;
    private final LocalDate dateOfBirth;
    private final String email;
    private final String phone;
    private final String username;
    private final List<String> registeredEvents;
    private final List<String> registeredSessions;
    private final int totalTickets;
    private final String lastActivityDate;
    private final List<String> activityHistory;
    private final String memberSinceDate;

    public AttendeeProfileDTO(String attendeeId, String fullName, LocalDate dateOfBirth,
                             String email, String phone, String username,
                             List<String> registeredEvents, List<String> registeredSessions,
                             int totalTickets, String lastActivityDate,
                             List<String> activityHistory, String memberSinceDate) {
        this.attendeeId = attendeeId;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.registeredEvents = registeredEvents;
        this.registeredSessions = registeredSessions;
        this.totalTickets = totalTickets;
        this.lastActivityDate = lastActivityDate;
        this.activityHistory = activityHistory;
        this.memberSinceDate = memberSinceDate;
    }

    // Getters
    public String getAttendeeId() { return attendeeId; }
    public String getFullName() { return fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getUsername() { return username; }
    public List<String> getRegisteredEvents() { return registeredEvents; }
    public List<String> getRegisteredSessions() { return registeredSessions; }
    public int getTotalTickets() { return totalTickets; }
    public String getLastActivityDate() { return lastActivityDate; }
    public List<String> getActivityHistory() { return activityHistory; }
    public String getMemberSinceDate() { return memberSinceDate; }

    @Override
    public String toString() {
        return "AttendeeProfileDTO{" +
                "attendeeId='" + attendeeId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", registeredEvents=" + (registeredEvents != null ? registeredEvents.size() : 0) +
                ", registeredSessions=" + (registeredSessions != null ? registeredSessions.size() : 0) +
                ", totalTickets=" + totalTickets +
                '}';
    }
}

