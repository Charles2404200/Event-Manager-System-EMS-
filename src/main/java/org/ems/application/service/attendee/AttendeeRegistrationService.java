package org.ems.application.service.attendee;

import org.ems.application.dto.registration.RegistrationResultDTO;
import org.ems.domain.model.Attendee;
import org.ems.domain.repository.AttendeeRepository;

import java.util.UUID;

/**
 * Service for managing attendee registrations for events and sessions
 * Implements Single Responsibility Principle - only handles registration operations
 * Delegates validation to AttendeeValidationService
 * Implements Dependency Inversion Principle - depends on abstractions (repositories)
 *
 * @author <your group number>
 */
public class AttendeeRegistrationService {

    private final AttendeeRepository attendeeRepo;
    private final AttendeeValidationService validationService;

    public AttendeeRegistrationService(AttendeeRepository attendeeRepo,
                                     AttendeeValidationService validationService) {
        this.attendeeRepo = attendeeRepo;
        this.validationService = validationService;
    }

    /**
     * Register attendee for an event
     *
     * @param attendeeId ID of attendee
     * @param eventId ID of event
     * @return RegistrationResultDTO with operation result
     */
    public RegistrationResultDTO registerForEvent(UUID attendeeId, UUID eventId) {
        System.out.println("📋 [AttendeeRegistrationService] Registering attendee for event");

        try {
            // Load attendee
            Attendee attendee = attendeeRepo.findById(attendeeId);
            if (attendee == null) {
                String error = "Attendee not found: " + attendeeId;
                System.err.println("✗ " + error);
                return new RegistrationResultDTO(false, error, "EVENT_REGISTRATION",
                        attendeeId.toString(), eventId.toString(), error);
            }

            // Validate registration
            var errors = validationService.validateEventRegistration(attendee, eventId);
            if (validationService.hasErrors(errors)) {
                String errorMsg = validationService.formatErrors(errors);
                System.err.println("✗ Validation failed: " + errorMsg);
                return new RegistrationResultDTO(false, "Validation failed", "EVENT_REGISTRATION",
                        attendeeId.toString(), eventId.toString(), errorMsg);
            }

            // Register via repository
            attendeeRepo.registerEvent(attendeeId, eventId);
            attendee.addEvent(eventId);

            String successMsg = "Successfully registered for event";
            System.out.println("✓ " + successMsg);
            return new RegistrationResultDTO(true, successMsg, "EVENT_REGISTRATION",
                    attendeeId.toString(), eventId.toString());

        } catch (Exception e) {
            String errorMsg = "Error during event registration: " + e.getMessage();
            System.err.println("✗ " + errorMsg);
            return new RegistrationResultDTO(false, "Operation failed", "EVENT_REGISTRATION",
                    attendeeId.toString(), eventId.toString(), errorMsg);
        }
    }

    /**
     * Register attendee for a session
     *
     * @param attendeeId ID of attendee
     * @param sessionId ID of session
     * @return RegistrationResultDTO with operation result
     */
    public RegistrationResultDTO registerForSession(UUID attendeeId, UUID sessionId) {
        System.out.println("📋 [AttendeeRegistrationService] Registering attendee for session");

        try {
            // Load attendee
            Attendee attendee = attendeeRepo.findById(attendeeId);
            if (attendee == null) {
                String error = "Attendee not found: " + attendeeId;
                System.err.println("✗ " + error);
                return new RegistrationResultDTO(false, error, "SESSION_REGISTRATION",
                        attendeeId.toString(), sessionId.toString(), error);
            }

            // Validate registration
            var errors = validationService.validateSessionRegistration(attendee, sessionId);
            if (validationService.hasErrors(errors)) {
                String errorMsg = validationService.formatErrors(errors);
                System.err.println("✗ Validation failed: " + errorMsg);
                return new RegistrationResultDTO(false, "Validation failed", "SESSION_REGISTRATION",
                        attendeeId.toString(), sessionId.toString(), errorMsg);
            }

            // Register via repository
            attendeeRepo.registerSession(attendeeId, sessionId);
            attendee.addSession(sessionId);

            String successMsg = "Successfully registered for session";
            System.out.println("✓ " + successMsg);
            return new RegistrationResultDTO(true, successMsg, "SESSION_REGISTRATION",
                    attendeeId.toString(), sessionId.toString());

        } catch (Exception e) {
            String errorMsg = "Error during session registration: " + e.getMessage();
            System.err.println("✗ " + errorMsg);
            return new RegistrationResultDTO(false, "Operation failed", "SESSION_REGISTRATION",
                    attendeeId.toString(), sessionId.toString(), errorMsg);
        }
    }

    /**
     * Unregister attendee from event
     *
     * @param attendeeId ID of attendee
     * @param eventId ID of event
     * @return RegistrationResultDTO with operation result
     */
    public RegistrationResultDTO unregisterFromEvent(UUID attendeeId, UUID eventId) {
        System.out.println("📋 [AttendeeRegistrationService] Unregistering attendee from event");

        try {
            // Load attendee
            Attendee attendee = attendeeRepo.findById(attendeeId);
            if (attendee == null) {
                String error = "Attendee not found: " + attendeeId;
                System.err.println("✗ " + error);
                return new RegistrationResultDTO(false, error, "EVENT_UNREGISTRATION",
                        attendeeId.toString(), eventId.toString(), error);
            }

            // Validate unregistration
            var errors = validationService.validateEventUnregistration(attendee, eventId);
            if (validationService.hasErrors(errors)) {
                String errorMsg = validationService.formatErrors(errors);
                System.err.println("✗ Validation failed: " + errorMsg);
                return new RegistrationResultDTO(false, "Validation failed", "EVENT_UNREGISTRATION",
                        attendeeId.toString(), eventId.toString(), errorMsg);
            }

            // Unregister via repository
            attendeeRepo.unregisterEvent(attendeeId, eventId);
            attendee.removeEvent(eventId);

            String successMsg = "Successfully unregistered from event";
            System.out.println("✓ " + successMsg);
            return new RegistrationResultDTO(true, successMsg, "EVENT_UNREGISTRATION",
                    attendeeId.toString(), eventId.toString());

        } catch (Exception e) {
            String errorMsg = "Error during event unregistration: " + e.getMessage();
            System.err.println("✗ " + errorMsg);
            return new RegistrationResultDTO(false, "Operation failed", "EVENT_UNREGISTRATION",
                    attendeeId.toString(), eventId.toString(), errorMsg);
        }
    }

    /**
     * Unregister attendee from session
     *
     * @param attendeeId ID of attendee
     * @param sessionId ID of session
     * @return RegistrationResultDTO with operation result
     */
    public RegistrationResultDTO unregisterFromSession(UUID attendeeId, UUID sessionId) {
        System.out.println("📋 [AttendeeRegistrationService] Unregistering attendee from session");

        try {
            // Load attendee
            Attendee attendee = attendeeRepo.findById(attendeeId);
            if (attendee == null) {
                String error = "Attendee not found: " + attendeeId;
                System.err.println("✗ " + error);
                return new RegistrationResultDTO(false, error, "SESSION_UNREGISTRATION",
                        attendeeId.toString(), sessionId.toString(), error);
            }

            // Validate unregistration
            var errors = validationService.validateSessionUnregistration(attendee, sessionId);
            if (validationService.hasErrors(errors)) {
                String errorMsg = validationService.formatErrors(errors);
                System.err.println("✗ Validation failed: " + errorMsg);
                return new RegistrationResultDTO(false, "Validation failed", "SESSION_UNREGISTRATION",
                        attendeeId.toString(), sessionId.toString(), errorMsg);
            }

            // Unregister via repository
            attendeeRepo.unregisterSession(attendeeId, sessionId);
            attendee.removeSession(sessionId);

            String successMsg = "Successfully unregistered from session";
            System.out.println("✓ " + successMsg);
            return new RegistrationResultDTO(true, successMsg, "SESSION_UNREGISTRATION",
                    attendeeId.toString(), sessionId.toString());

        } catch (Exception e) {
            String errorMsg = "Error during session unregistration: " + e.getMessage();
            System.err.println("✗ " + errorMsg);
            return new RegistrationResultDTO(false, "Operation failed", "SESSION_UNREGISTRATION",
                    attendeeId.toString(), sessionId.toString(), errorMsg);
        }
    }
}

