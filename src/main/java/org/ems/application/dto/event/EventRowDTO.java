package org.ems.application.dto.event;

import java.util.UUID;

/**
 * EventRowDTO - UI DTO for event listing table rows
 * Extracted from ViewEventsController.EventRow inner class
 *
 * @author EMS Team
 */
public class EventRowDTO {
    public UUID eventId;
    public String name;
    public String type;
    public String location;
    public String startDate;
    public String endDate;
    public String status;
    public int sessionCount;
    public boolean isRegistered;
    public String imagePath;

    public EventRowDTO(UUID eventId, String name, String type, String location,
                       String startDate, String endDate, String status, int sessionCount,
                       boolean isRegistered, String imagePath) {
        this.eventId = eventId;
        this.name = name;
        this.type = type;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.sessionCount = sessionCount;
        this.isRegistered = isRegistered;
        this.imagePath = imagePath;
    }

    @Override
    public String toString() {
        return "EventRowDTO{" +
                "eventId=" + eventId +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}

