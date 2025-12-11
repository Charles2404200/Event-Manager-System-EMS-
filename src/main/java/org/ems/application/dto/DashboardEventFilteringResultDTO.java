package org.ems.application.dto;

import org.ems.domain.model.Event;
import java.util.List;

/**
 * DTO for dashboard event filtering results
 * @author <your group number>
 */
public class DashboardEventFilteringResultDTO {
    private final List<Event> upcomingEvents;
    private final int totalTickets;
    private final int upcomingEventCount;

    public DashboardEventFilteringResultDTO(List<Event> upcomingEvents, int totalTickets, int upcomingEventCount) {
        this.upcomingEvents = upcomingEvents;
        this.totalTickets = totalTickets;
        this.upcomingEventCount = upcomingEventCount;
    }

    public List<Event> getUpcomingEvents() { return upcomingEvents; }
    public int getTotalTickets() { return totalTickets; }
    public int getUpcomingEventCount() { return upcomingEventCount; }

    @Override
    public String toString() {
        return "DashboardEventFilteringResultDTO{" +
                "upcomingEventCount=" + upcomingEventCount +
                ", totalTickets=" + totalTickets +
                '}';
    }
}

