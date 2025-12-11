package org.ems.application.dto;

/**
 * DTO for event admin dashboard content
 * @author <your group number>
 */
public class DashboardEventAdminContentDTO {
    private final int totalEvents;
    private final int totalSessions;
    private final int totalTickets;

    public DashboardEventAdminContentDTO(int totalEvents, int totalSessions, int totalTickets) {
        this.totalEvents = totalEvents;
        this.totalSessions = totalSessions;
        this.totalTickets = totalTickets;
    }

    public int getTotalEvents() { return totalEvents; }
    public int getTotalSessions() { return totalSessions; }
    public int getTotalTickets() { return totalTickets; }

    @Override
    public String toString() {
        return "DashboardEventAdminContentDTO{" +
                "totalEvents=" + totalEvents +
                ", totalSessions=" + totalSessions +
                ", totalTickets=" + totalTickets +
                '}';
    }
}

