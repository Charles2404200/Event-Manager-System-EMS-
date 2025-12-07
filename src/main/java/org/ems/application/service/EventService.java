package org.ems.application.service;

import org.ems.domain.model.Event;
import org.ems.domain.model.Session;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EventService {

    // Event
    Event createEvent(Event e);
    Event updateEvent(Event e);
    Event getEvent(UUID id);
    List<Event> getEvents();
    List<Event> getEventsByType(String type);
    boolean deleteEvent(UUID id);

    // Session
    Session createSession(Session s);
    Session updateSession(Session s);
    Session getSession(UUID id);
    List<Session> getSessions();
    List<Session> getSessionsByDate(LocalDate date);
    boolean deleteSession(UUID id);

    // Linking
    boolean addPresenterToSession(UUID presenterId, UUID sessionId);
    boolean registerAttendeeToSession(UUID attendeeId, UUID sessionId);

    // Image Upload
    /**
     * Upload an image for an event.
     *
     * @param filePath Path to the image file
     * @param eventId Event UUID
     * @return true if upload successful, false otherwise
     */
    boolean uploadEventImage(String filePath, UUID eventId);

    /**
     * Upload session materials (images, documents, etc.).
     *
     * @param filePath Path to the material file
     * @param sessionId Session UUID
     * @return true if upload successful, false otherwise
     */
    boolean uploadSessionMaterial(String filePath, UUID sessionId);
}
