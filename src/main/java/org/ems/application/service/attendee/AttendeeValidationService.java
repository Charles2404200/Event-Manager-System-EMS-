package org.ems.application.service.attendee;

import org.ems.domain.model.Attendee;
import org.ems.domain.model.Event;
import org.ems.domain.model.Session;
import org.ems.domain.model.enums.EventStatus;
import org.ems.domain.repository.EventRepository;
import org.ems.domain.repository.SessionRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for validating attendee-related operations
 * Implements Single Responsibility Principle - only handles validation logic
 * Validates registrations, data consistency, and business rules
 * Implements Dependency Inversion Principle - depends on abstractions (repositories)
 *
 * @author <your group number>
 */
public class AttendeeValidationService {

    private final EventRepository eventRepo;
    private final SessionRepository sessionRepo;

    public AttendeeValidationService(EventRepository eventRepo, SessionRepository sessionRepo) {
        this.eventRepo = eventRepo;
        this.sessionRepo = sessionRepo;
    }

    /**
     * Validate if attendee data is complete and valid
     *
     * @param attendee Attendee to validate
     * @return List of validation errors (empty if valid)
     */
    public List<String> validateAttendeeData(Attendee attendee) {
        List<String> errors = new ArrayList<>();

        if (attendee == null) {
            errors.add("Attendee cannot be null");
            return errors;
        }

        if (attendee.getFullName() == null || attendee.getFullName().trim().isEmpty()) {
            errors.add("Full name is required");
        }

        if (attendee.getEmail() == null || attendee.getEmail().trim().isEmpty()) {
            errors.add("Email is required");
        } else if (!isValidEmail(attendee.getEmail())) {
            errors.add("Email format is invalid");
        }

        if (attendee.getUsername() == null || attendee.getUsername().trim().isEmpty()) {
            errors.add("Username is required");
        }

        if (attendee.getDateOfBirth() != null && attendee.getDateOfBirth().isAfter(LocalDate.now())) {
            errors.add("Date of birth cannot be in the future");
        }

        return errors;
    }

    /**
     * Validate if attendee can register for an event
     *
     * @param attendee Attendee attempting registration
     * @param eventId Event ID
     * @return List of validation errors (empty if can register)
     */
    public List<String> validateEventRegistration(Attendee attendee, UUID eventId) {
        List<String> errors = new ArrayList<>();

        if (attendee == null) {
            errors.add("Attendee cannot be null");
            return errors;
        }

        if (eventId == null) {
            errors.add("Event ID cannot be null");
            return errors;
        }

        // Check if event exists and is active
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            errors.add("Event not found");
            return errors;
        }

        if (event.getStatus() == EventStatus.CANCELLED) {
            errors.add("Event is cancelled");
        } else if (event.getStatus() == EventStatus.COMPLETED) {
            errors.add("Event is already completed");
        }

        // Check if already registered
        if (attendee.getRegisteredEventIds() != null && attendee.getRegisteredEventIds().contains(eventId)) {
            errors.add("Attendee is already registered for this event");
        }

        return errors;
    }

    /**
     * Validate if attendee can register for a session
     *
     * @param attendee Attendee attempting registration
     * @param sessionId Session ID
     * @return List of validation errors (empty if can register)
     */
    public List<String> validateSessionRegistration(Attendee attendee, UUID sessionId) {
        List<String> errors = new ArrayList<>();

        if (attendee == null) {
            errors.add("Attendee cannot be null");
            return errors;
        }

        if (sessionId == null) {
            errors.add("Session ID cannot be null");
            return errors;
        }

        // Check if session exists
        Session session = sessionRepo.findById(sessionId);
        if (session == null) {
            errors.add("Session not found");
            return errors;
        }

        // Check if already registered
        if (attendee.getRegisteredSessionIds() != null && attendee.getRegisteredSessionIds().contains(sessionId)) {
            errors.add("Attendee is already registered for this session");
        }

        // Check capacity
        if (session.getCapacity() > 0) {
            // TODO: Check actual attendance count
            // For now, simplified check
            System.out.println("  ℹ Session capacity check: " + session.getCapacity());
        }

        return errors;
    }

    /**
     * Validate if attendee can unregister from event
     *
     * @param attendee Attendee attempting unregistration
     * @param eventId Event ID
     * @return List of validation errors (empty if can unregister)
     */
    public List<String> validateEventUnregistration(Attendee attendee, UUID eventId) {
        List<String> errors = new ArrayList<>();

        if (attendee == null) {
            errors.add("Attendee cannot be null");
            return errors;
        }

        if (eventId == null) {
            errors.add("Event ID cannot be null");
            return errors;
        }

        // Check if registered
        if (attendee.getRegisteredEventIds() == null || !attendee.getRegisteredEventIds().contains(eventId)) {
            errors.add("Attendee is not registered for this event");
        }

        return errors;
    }

    /**
     * Validate if attendee can unregister from session
     *
     * @param attendee Attendee attempting unregistration
     * @param sessionId Session ID
     * @return List of validation errors (empty if can unregister)
     */
    public List<String> validateSessionUnregistration(Attendee attendee, UUID sessionId) {
        List<String> errors = new ArrayList<>();

        if (attendee == null) {
            errors.add("Attendee cannot be null");
            return errors;
        }

        if (sessionId == null) {
            errors.add("Session ID cannot be null");
            return errors;
        }

        // Check if registered
        if (attendee.getRegisteredSessionIds() == null || !attendee.getRegisteredSessionIds().contains(sessionId)) {
            errors.add("Attendee is not registered for this session");
        }

        return errors;
    }

    /**
     * Validate email format using simple regex
     *
     * @param email Email address to validate
     * @return true if email format is valid
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    /**
     * Check if validation errors exist
     *
     * @param errors List of validation errors
     * @return true if there are errors
     */
    public boolean hasErrors(List<String> errors) {
        return errors != null && !errors.isEmpty();
    }

    /**
     * Get formatted error message from validation errors
     *
     * @param errors List of validation errors
     * @return Formatted error string
     */
    public String formatErrors(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "";
        }
        return String.join("; ", errors);
    }
}

