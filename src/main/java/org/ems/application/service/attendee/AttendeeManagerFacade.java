package org.ems.application.service.attendee;

import org.ems.application.dto.attendee.AttendeeDisplayDTO;
import org.ems.application.dto.attendee.AttendeeProfileDTO;
import org.ems.application.dto.registration.RegistrationResultDTO;
import org.ems.domain.model.Attendee;
import org.ems.domain.repository.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Facade for attendee service package
 * Provides single entry point for all attendee operations
 * Implements Facade pattern for simplified client interface
 * Implements Dependency Injection pattern for service initialization
 *
 * @author <your group number>
 */
public class AttendeeManagerFacade {

    private final AttendeeLoaderService loaderService;
    private final AttendeeValidationService validationService;
    private final AttendeeRegistrationService registrationService;
    private final AttendeeProfileService profileService;
    private final AttendeeFilteringService filteringService;
    private final AttendeeScheduleExportService exportService;

    /**
     * Initialize facade with all required repositories
     */
    public AttendeeManagerFacade(AttendeeRepository attendeeRepo,
                               EventRepository eventRepo,
                               SessionRepository sessionRepo,
                               TicketRepository ticketRepo) {
        System.out.println("🚀 [AttendeeManagerFacade] Initializing attendee service facade...");

        // Initialize core services
        this.loaderService = new AttendeeLoaderService(attendeeRepo, eventRepo, sessionRepo, ticketRepo);
        this.validationService = new AttendeeValidationService(eventRepo, sessionRepo);
        this.registrationService = new AttendeeRegistrationService(attendeeRepo, validationService);
        this.profileService = new AttendeeProfileService(loaderService, attendeeRepo);
        this.filteringService = new AttendeeFilteringService(loaderService, ticketRepo);
        this.exportService = new AttendeeScheduleExportService(ticketRepo, eventRepo, sessionRepo);

        System.out.println("✓ Attendee service facade initialized successfully");
    }

    // ==================== LOADER OPERATIONS ====================

    /**
     * Load attendee by ID
     *
     * @param attendeeId Attendee ID
     * @return Attendee object or null
     */
    public Attendee loadAttendee(UUID attendeeId) {
        return loaderService.loadAttendeeById(attendeeId);
    }

    /**
     * Load all attendees
     *
     * @return List of all attendees
     */
    public List<Attendee> loadAllAttendees() {
        return loaderService.loadAllAttendees();
    }

    /**
     * Load complete attendee profile with all related data
     *
     * @param attendeeId Attendee ID
     * @return Map with attendee, events, sessions, and tickets
     */
    public Map<String, Object> loadCompleteProfile(UUID attendeeId) {
        return loaderService.loadCompleteAttendeeProfile(attendeeId);
    }

    /**
     * Get total attendee count
     *
     * @return Number of attendees in system
     */
    public long getTotalAttendeeCount() {
        return loaderService.countAttendees();
    }

    // ==================== VALIDATION OPERATIONS ====================

    /**
     * Validate attendee data
     *
     * @param attendee Attendee to validate
     * @return List of validation errors (empty if valid)
     */
    public List<String> validateAttendee(Attendee attendee) {
        return validationService.validateAttendeeData(attendee);
    }

    /**
     * Check if attendee data is valid
     *
     * @param attendee Attendee to check
     * @return true if valid
     */
    public boolean isAttendeeValid(Attendee attendee) {
        return !validationService.hasErrors(validationService.validateAttendeeData(attendee));
    }

    // ==================== REGISTRATION OPERATIONS ====================

    /**
     * Register attendee for event
     *
     * @param attendeeId Attendee ID
     * @param eventId Event ID
     * @return RegistrationResultDTO with operation result
     */
    public RegistrationResultDTO registerForEvent(UUID attendeeId, UUID eventId) {
        return registrationService.registerForEvent(attendeeId, eventId);
    }

    /**
     * Register attendee for session
     *
     * @param attendeeId Attendee ID
     * @param sessionId Session ID
     * @return RegistrationResultDTO with operation result
     */
    public RegistrationResultDTO registerForSession(UUID attendeeId, UUID sessionId) {
        return registrationService.registerForSession(attendeeId, sessionId);
    }

    /**
     * Unregister attendee from event
     *
     * @param attendeeId Attendee ID
     * @param eventId Event ID
     * @return RegistrationResultDTO with operation result
     */
    public RegistrationResultDTO unregisterFromEvent(UUID attendeeId, UUID eventId) {
        return registrationService.unregisterFromEvent(attendeeId, eventId);
    }

    /**
     * Unregister attendee from session
     *
     * @param attendeeId Attendee ID
     * @param sessionId Session ID
     * @return RegistrationResultDTO with operation result
     */
    public RegistrationResultDTO unregisterFromSession(UUID attendeeId, UUID sessionId) {
        return registrationService.unregisterFromSession(attendeeId, sessionId);
    }

    // ==================== PROFILE OPERATIONS ====================

    /**
     * Get detailed attendee profile
     *
     * @param attendeeId Attendee ID
     * @return AttendeeProfileDTO with complete profile
     */
    public AttendeeProfileDTO getProfile(UUID attendeeId) {
        return profileService.getAttendeeProfile(attendeeId);
    }

    /**
     * Update attendee profile
     *
     * @param attendee Updated attendee object
     * @return true if update successful
     */
    public boolean updateProfile(Attendee attendee) {
        return profileService.updateAttendeeProfile(attendee);
    }

    /**
     * Record activity for attendee
     *
     * @param attendeeId Attendee ID
     * @param activity Activity description
     * @return true if recorded successfully
     */
    public boolean recordActivity(UUID attendeeId, String activity) {
        return profileService.recordActivity(attendeeId, activity);
    }

    /**
     * Get activity history for attendee
     *
     * @param attendeeId Attendee ID
     * @return List of activities
     */
    public List<String> getActivityHistory(UUID attendeeId) {
        return profileService.getActivityHistory(attendeeId);
    }

    // ==================== FILTERING OPERATIONS ====================

    /**
     * Search attendees by name
     *
     * @param searchTerm Name search term
     * @return List of matching attendees
     */
    public List<Attendee> searchByName(String searchTerm) {
        return filteringService.filterByName(searchTerm);
    }

    /**
     * Find attendee by email
     *
     * @param email Email address
     * @return Attendee if found, null otherwise
     */
    public Attendee findByEmail(String email) {
        return filteringService.filterByEmail(email);
    }

    /**
     * Find attendee by username
     *
     * @param username Username
     * @return Attendee if found, null otherwise
     */
    public Attendee findByUsername(String username) {
        return filteringService.filterByUsername(username);
    }

    /**
     * Get attendees with minimum event registrations
     *
     * @param minEvents Minimum event count
     * @return List of matching attendees
     */
    public List<Attendee> getAttendeesByMinimumEvents(int minEvents) {
        return filteringService.filterByMinimumEvents(minEvents);
    }

    /**
     * Get attendees with active tickets
     *
     * @return List of attendees with tickets
     */
    public List<Attendee> getAttendeesWithTickets() {
        return filteringService.filterWithActiveTickets();
    }

    /**
     * Get paginated list of attendees for display
     *
     * @param pageNumber Page number (starting from 1)
     * @param pageSize Number of items per page
     * @return List of AttendeeDisplayDTOs
     */
    public List<AttendeeDisplayDTO> getPaginatedAttendees(int pageNumber, int pageSize) {
        return filteringService.getPaginatedAttendees(pageNumber, pageSize);
    }

    /**
     * Convert attendee to display DTO
     *
     * @param attendee Attendee to convert
     * @return AttendeeDisplayDTO
     */
    public AttendeeDisplayDTO toDisplayDTO(Attendee attendee) {
        return filteringService.toDisplayDTO(attendee);
    }

    /**
     * Convert list of attendees to display DTOs
     *
     * @param attendees List of attendees
     * @return List of AttendeeDisplayDTOs
     */
    public List<AttendeeDisplayDTO> toDisplayDTOs(List<Attendee> attendees) {
        return filteringService.toDisplayDTOs(attendees);
    }

    // ==================== EXPORT OPERATIONS ====================

    /**
     * Export attendee schedule to CSV
     *
     * @param attendeeId Attendee ID
     * @param outputPath Output directory path
     * @return File path of exported file
     * @throws AttendeeScheduleExportService.ScheduleExportException if export fails
     */
    public String exportScheduleToCSV(UUID attendeeId, String outputPath)
            throws AttendeeScheduleExportService.ScheduleExportException {
        Attendee attendee = loadAttendee(attendeeId);
        if (attendee == null) {
            throw new AttendeeScheduleExportService.ScheduleExportException("Attendee not found: " + attendeeId);
        }
        return exportService.exportScheduleToCSV(attendee, outputPath);
    }

    /**
     * Export attendee schedule to Excel
     *
     * @param attendeeId Attendee ID
     * @param outputPath Output directory path
     * @return File path of exported file
     * @throws AttendeeScheduleExportService.ScheduleExportException if export fails
     */
    public String exportScheduleToExcel(UUID attendeeId, String outputPath)
            throws AttendeeScheduleExportService.ScheduleExportException {
        Attendee attendee = loadAttendee(attendeeId);
        if (attendee == null) {
            throw new AttendeeScheduleExportService.ScheduleExportException("Attendee not found: " + attendeeId);
        }
        return exportService.exportScheduleToExcel(attendee, outputPath);
    }

    /**
     * Export attendee schedule to PDF
     *
     * @param attendeeId Attendee ID
     * @param outputPath Output directory path
     * @return File path of exported file
     * @throws AttendeeScheduleExportService.ScheduleExportException if export fails
     */
    public String exportScheduleToPDF(UUID attendeeId, String outputPath)
            throws AttendeeScheduleExportService.ScheduleExportException {
        Attendee attendee = loadAttendee(attendeeId);
        if (attendee == null) {
            throw new AttendeeScheduleExportService.ScheduleExportException("Attendee not found: " + attendeeId);
        }
        return exportService.exportScheduleToPDF(attendee, outputPath);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get all attendee services (for advanced usage)
     *
     * @return Map of service name to service instance
     */
    public Map<String, Object> getServices() {
        return Map.of(
                "loader", loaderService,
                "validation", validationService,
                "registration", registrationService,
                "profile", profileService,
                "filtering", filteringService,
                "export", exportService
        );
    }

    /**
     * Print service status
     */
    public void printStatus() {
        System.out.println("\n========== Attendee Manager Facade Status ==========");
        System.out.println("✓ Loader Service: Ready");
        System.out.println("✓ Validation Service: Ready");
        System.out.println("✓ Registration Service: Ready");
        System.out.println("✓ Profile Service: Ready");
        System.out.println("✓ Filtering Service: Ready");
        System.out.println("✓ Export Service: Ready");
        System.out.println("Total Attendees: " + getTotalAttendeeCount());
        System.out.println("================================================\n");
    }
}

