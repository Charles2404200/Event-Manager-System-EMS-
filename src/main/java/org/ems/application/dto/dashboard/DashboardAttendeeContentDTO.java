package org.ems.application.dto.dashboard;

import org.ems.domain.model.Event;
import java.util.List;

/**
 * DTO for attendee dashboard content
 * @author <your group number>
 */
public class DashboardAttendeeContentDTO {
    private final List<Event> upcomingEvents;
    private final int totalTickets;
    private final int upcomingEventCount;

    public DashboardAttendeeContentDTO(List<Event> upcomingEvents, int totalTickets, int upcomingEventCount) {
        this.upcomingEvents = upcomingEvents;
        this.totalTickets = totalTickets;
        this.upcomingEventCount = upcomingEventCount;
    }

    public List<Event> getUpcomingEvents() { return upcomingEvents; }
    public int getTotalTickets() { return totalTickets; }
    public int getUpcomingEventCount() { return upcomingEventCount; }

    @Override
    public String toString() {
        return "DashboardAttendeeContentDTO{" +
                "upcomingEventCount=" + upcomingEventCount +
                ", totalTickets=" + totalTickets +
                '}';
    }
}

