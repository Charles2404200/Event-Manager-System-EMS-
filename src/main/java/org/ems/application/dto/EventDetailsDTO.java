package org.ems.application.dto;

import org.ems.domain.model.Event;

import java.util.UUID;

/**
 * EventDetailsDTO - DTO for event detail view
 * Contains all information needed to display event details dialog
 *
 * @author EMS Team
 */
public class EventDetailsDTO {
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
    public String description;  // Event description/about
    public Event domainEvent;   // Original domain model for additional data

    public EventDetailsDTO(UUID eventId, String name, String type, String location,
                           String startDate, String endDate, String status, int sessionCount,
                           boolean isRegistered, String imagePath, String description, Event domainEvent) {
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
        this.description = description;
        this.domainEvent = domainEvent;
    }
}

