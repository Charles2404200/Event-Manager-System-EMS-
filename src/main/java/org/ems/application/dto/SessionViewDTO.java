package org.ems.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * SessionViewDTO - DTO for displaying session with presenter information
 *
 * @author EMS Team
 */
public class SessionViewDTO {
    public UUID sessionId;
    public String title;
    public String startTime;
    public String endTime;
    public String venue;
    public int capacity;
    public List<String> presenterNames;

    public SessionViewDTO(UUID sessionId, String title, String startTime, String endTime,
                          String venue, int capacity, List<String> presenterNames) {
        this.sessionId = sessionId;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.venue = venue;
        this.capacity = capacity;
        this.presenterNames = presenterNames;
    }

    @Override
    public String toString() {
        return title + " (" + startTime + " - " + endTime + ") @ " + venue;
    }
}

