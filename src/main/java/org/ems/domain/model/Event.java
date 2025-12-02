package org.ems.domain.model;

import org.ems.domain.model.enums.EventStatus;
import org.ems.domain.model.enums.EventType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Event {

    private UUID id;

    private String name;
    private EventType type;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private EventStatus status;
    private String imagePath;

    private List<UUID> sessionIds = new ArrayList<>();

    public Event() {
        this.id = UUID.randomUUID();
    }

    public Event(String name, EventType type, String location,
                 LocalDate start, LocalDate end,
                 EventStatus status, String imagePath) {

        this.id = UUID.randomUUID();
        this.name = name;
        this.type = type;
        this.location = location;
        this.startDate = start;
        this.endDate = end;
        this.status = status;
        this.imagePath = imagePath;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public List<UUID> getSessionIds() { return sessionIds; }

    public void addSession(UUID sessionId) {
        sessionIds.add(sessionId);
    }
}
