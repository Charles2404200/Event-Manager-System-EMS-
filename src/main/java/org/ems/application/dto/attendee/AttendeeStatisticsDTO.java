package org.ems.application.dto.attendee;

import java.util.Map;

/**
 * DTO for attendee statistics and analytics
 * Contains aggregate data for reporting and dashboard display
 * Implements Data Transfer Object pattern
 *
 * @author <your group number>
 */
public class AttendeeStatisticsDTO {
    private final int totalAttendees;
    private final int activeAttendees; // with registrations in current month
    private final int totalRegistrations;
    private final int totalTicketsPurchased;
    private final double averageEventsPerAttendee;
    private final double averageSessionsPerAttendee;
    private final Map<String, Integer> eventTypeDistribution;
    private final Map<String, Integer> registrationTrendByMonth;
    private final String reportGeneratedDate;

    public AttendeeStatisticsDTO(int totalAttendees, int activeAttendees,
                                int totalRegistrations, int totalTicketsPurchased,
                                double averageEventsPerAttendee, double averageSessionsPerAttendee,
                                Map<String, Integer> eventTypeDistribution,
                                Map<String, Integer> registrationTrendByMonth,
                                String reportGeneratedDate) {
        this.totalAttendees = totalAttendees;
        this.activeAttendees = activeAttendees;
        this.totalRegistrations = totalRegistrations;
        this.totalTicketsPurchased = totalTicketsPurchased;
        this.averageEventsPerAttendee = averageEventsPerAttendee;
        this.averageSessionsPerAttendee = averageSessionsPerAttendee;
        this.eventTypeDistribution = eventTypeDistribution;
        this.registrationTrendByMonth = registrationTrendByMonth;
        this.reportGeneratedDate = reportGeneratedDate;
    }

    // Getters
    public int getTotalAttendees() { return totalAttendees; }
    public int getActiveAttendees() { return activeAttendees; }
    public int getTotalRegistrations() { return totalRegistrations; }
    public int getTotalTicketsPurchased() { return totalTicketsPurchased; }
    public double getAverageEventsPerAttendee() { return averageEventsPerAttendee; }
    public double getAverageSessionsPerAttendee() { return averageSessionsPerAttendee; }
    public Map<String, Integer> getEventTypeDistribution() { return eventTypeDistribution; }
    public Map<String, Integer> getRegistrationTrendByMonth() { return registrationTrendByMonth; }
    public String getReportGeneratedDate() { return reportGeneratedDate; }

    @Override
    public String toString() {
        return "AttendeeStatisticsDTO{" +
                "totalAttendees=" + totalAttendees +
                ", activeAttendees=" + activeAttendees +
                ", totalRegistrations=" + totalRegistrations +
                ", averageEventsPerAttendee=" + String.format("%.2f", averageEventsPerAttendee) +
                ", reportGeneratedDate='" + reportGeneratedDate + '\'' +
                '}';
    }
}

