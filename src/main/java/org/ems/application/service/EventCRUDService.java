package org.ems.application.service;

import org.ems.domain.model.Event;
import org.ems.domain.model.enums.EventStatus;
import org.ems.domain.model.enums.EventType;
import org.ems.domain.repository.EventRepository;

import java.util.UUID;

/**
 * EventCRUDService - Handles all CRUD operations for events
 * Single Responsibility: Event create, update, delete operations only
 *
 * @author EMS Team
 */
public class EventCRUDService {

    private final EventRepository eventRepo;
    private final ImageService imageService;

    public EventCRUDService(EventRepository eventRepo, ImageService imageService) {
        this.eventRepo = eventRepo;
        this.imageService = imageService;
    }

    /**
     * Create new event in database
     */
    public Event createEvent(String name, String type, String location,
                            java.time.LocalDate startDate, java.time.LocalDate endDate,
                            String status, String imagePath) {
        long start = System.currentTimeMillis();
        System.out.println("[EventCRUDService] createEvent(" + name + ") starting...");

        try {
            Event event = new Event();
            event.setId(UUID.randomUUID());
            event.setName(name);
            event.setType(EventType.valueOf(type));
            event.setLocation(location);
            event.setStartDate(startDate);
            event.setEndDate(endDate);
            event.setStatus(EventStatus.valueOf(status));
            event.setImagePath(imagePath);

            eventRepo.save(event);
            System.out.println("✓ createEvent completed in " + (System.currentTimeMillis() - start) + " ms");
            return event;

        } catch (Exception e) {
            System.err.println("✗ Error creating event: " + e.getMessage());
            throw new RuntimeException("Failed to create event: " + e.getMessage(), e);
        }
    }

    /**
     * Update existing event
     */
    public Event updateEvent(UUID eventId, String name, String type, String location,
                            java.time.LocalDate startDate, java.time.LocalDate endDate,
                            String status, String imagePath) {
        long start = System.currentTimeMillis();
        System.out.println("[EventCRUDService] updateEvent(" + eventId + ") starting...");

        try {
            Event event = eventRepo.findById(eventId);
            if (event == null) {
                throw new IllegalArgumentException("Event not found: " + eventId);
            }

            event.setName(name);
            event.setType(EventType.valueOf(type));
            event.setLocation(location);
            event.setStartDate(startDate);
            event.setEndDate(endDate);
            event.setStatus(EventStatus.valueOf(status));
            if (imagePath != null) {
                event.setImagePath(imagePath);
            }

            eventRepo.save(event);
            System.out.println("✓ updateEvent completed in " + (System.currentTimeMillis() - start) + " ms");
            return event;

        } catch (Exception e) {
            System.err.println("✗ Error updating event: " + e.getMessage());
            throw new RuntimeException("Failed to update event: " + e.getMessage(), e);
        }
    }

    /**
     * Delete event from database
     */
    public void deleteEvent(UUID eventId) {
        long start = System.currentTimeMillis();
        System.out.println("[EventCRUDService] deleteEvent(" + eventId + ") starting...");

        try {
            eventRepo.delete(eventId);
            System.out.println("✓ deleteEvent completed in " + (System.currentTimeMillis() - start) + " ms");

        } catch (Exception e) {
            System.err.println("✗ Error deleting event: " + e.getMessage());
            throw new RuntimeException("Failed to delete event: " + e.getMessage(), e);
        }
    }

    /**
     * Get event by ID
     */
    public Event getEvent(UUID eventId) {
        try {
            return eventRepo.findById(eventId);
        } catch (Exception e) {
            System.err.println("✗ Error getting event: " + e.getMessage());
            return null;
        }
    }
}

