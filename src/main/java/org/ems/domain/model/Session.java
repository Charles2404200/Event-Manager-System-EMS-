package org.ems.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Session {

    private UUID id;
    private UUID eventId;

    private String title;
    private String description;

    private LocalDateTime start;
    private LocalDateTime end;
    private String venue;
    private int capacity;

    // presenters
    private List<UUID> presenterIds = new ArrayList<>();
    private List<String> materialPaths = new ArrayList<>();

    public Session() {
        this.id = UUID.randomUUID();
    }

    public Session(UUID eventId, String title, String description,
                   LocalDateTime start, LocalDateTime end,
                   String venue, int capacity) {

        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.start = start;
        this.end = end;
        this.venue = venue;
        this.capacity = capacity;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getStart() { return start; }
    public void setStart(LocalDateTime start) { this.start = start; }

    public LocalDateTime getEnd() { return end; }
    public void setEnd(LocalDateTime end) { this.end = end; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public List<UUID> getPresenterIds() { return presenterIds; }
    public void addPresenter(UUID presenterId) { presenterIds.add(presenterId); }

    public List<String> getMaterialPaths() { return materialPaths; }
    public void addMaterial(String path) { materialPaths.add(path); }
}
