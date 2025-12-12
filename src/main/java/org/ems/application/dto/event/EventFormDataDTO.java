package org.ems.application.dto.event;

import java.time.LocalDate;

/**
 * EventFormDataDTO - Data transfer object for event create/edit form
 * Holds all form input values before validation and processing
 *
 * @author EMS Team
 */
public class EventFormDataDTO {
    public String name;
    public String type;
    public String location;
    public LocalDate startDate;
    public LocalDate endDate;
    public String status;
    public String imagePath;

    public EventFormDataDTO(String name, String type, String location,
                           LocalDate startDate, LocalDate endDate, String status, String imagePath) {
        this.name = name;
        this.type = type;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.imagePath = imagePath;
    }

    @Override
    public String toString() {
        return "EventFormDataDTO{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", location='" + location + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status='" + status + '\'' +
                '}';
    }
}

