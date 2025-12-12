package org.ems.application.service.attendee;

import org.ems.application.dto.attendee.AttendeeProfileDTO;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Event;
import org.ems.domain.model.Session;
import org.ems.domain.model.Ticket;
import org.ems.domain.repository.AttendeeRepository;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing and displaying attendee profiles
 * Implements Single Responsibility Principle - only handles profile operations
 * Delegates data loading to AttendeeLoaderService
 * Implements Dependency Inversion Principle - depends on abstractions (repositories)
 *
 * @author <your group number>
 */
public class AttendeeProfileService {

    private final AttendeeLoaderService loaderService;
    private final AttendeeRepository attendeeRepo;

    public AttendeeProfileService(AttendeeLoaderService loaderService,
                                 AttendeeRepository attendeeRepo) {
        this.loaderService = loaderService;
        this.attendeeRepo = attendeeRepo;
    }

    /**
     * Get detailed attendee profile as DTO
     *
     * @param attendeeId ID of attendee
     * @return AttendeeProfileDTO with complete profile information
     */
    public AttendeeProfileDTO getAttendeeProfile(UUID attendeeId) {
        System.out.println("👤 [AttendeeProfileService] Loading profile for attendee: " + attendeeId);

        try {
            // Load complete attendee data
            Map<String, Object> profileData = loaderService.loadCompleteAttendeeProfile(attendeeId);
            Attendee attendee = (Attendee) profileData.get("attendee");

            if (attendee == null) {
                System.out.println("  ✗ Attendee not found");
                return null;
            }

            // Extract loaded data
            @SuppressWarnings("unchecked")
            List<Event> events = (List<Event>) profileData.get("events");
            @SuppressWarnings("unchecked")
            List<Session> sessions = (List<Session>) profileData.get("sessions");
            @SuppressWarnings("unchecked")
            List<Ticket> tickets = (List<Ticket>) profileData.get("tickets");

            // Format data for display
            List<String> eventNames = events != null ?
                    events.stream().map(Event::getName).collect(Collectors.toList()) :
                    new ArrayList<>();

            List<String> sessionTitles = sessions != null ?
                    sessions.stream().map(Session::getTitle).collect(Collectors.toList()) :
                    new ArrayList<>();

            int totalTickets = tickets != null ? tickets.size() : 0;

            // Get activity info
            String lastActivityDate = getLastActivityDate(attendee);
            List<String> activityHistory = attendee.getActivityHistory() != null ?
                    new ArrayList<>(attendee.getActivityHistory()) :
                    new ArrayList<>();

            String memberSinceDate = formatDate(new Date()); // TODO: Get actual registration date

            // Build DTO
            AttendeeProfileDTO profileDTO = new AttendeeProfileDTO(
                    attendee.getId().toString(),
                    attendee.getFullName(),
                    attendee.getDateOfBirth(),
                    attendee.getEmail(),
                    attendee.getPhone(),
                    attendee.getUsername(),
                    eventNames,
                    sessionTitles,
                    totalTickets,
                    lastActivityDate,
                    activityHistory,
                    memberSinceDate
            );

            System.out.println("  ✓ Profile loaded successfully");
            return profileDTO;

        } catch (Exception e) {
            System.err.println("  ✗ Error loading profile: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Update attendee profile information
     *
     * @param attendee Attendee to update
     * @return true if update successful
     */
    public boolean updateAttendeeProfile(Attendee attendee) {
        System.out.println("✏️ [AttendeeProfileService] Updating profile for: " + attendee.getFullName());

        try {
            // Save updated attendee
            Attendee updated = attendeeRepo.save(attendee);

            if (updated != null) {
                System.out.println("  ✓ Profile updated successfully");
                attendee.addActivity("Profile updated");
                return true;
            } else {
                System.out.println("  ✗ Failed to update profile");
                return false;
            }

        } catch (Exception e) {
            System.err.println("  ✗ Error updating profile: " + e.getMessage());
            return false;
        }
    }

    /**
     * Record activity for attendee
     *
     * @param attendeeId ID of attendee
     * @param activity Activity description
     * @return true if recorded successfully
     */
    public boolean recordActivity(UUID attendeeId, String activity) {
        System.out.println("📝 [AttendeeProfileService] Recording activity for attendee: " + attendeeId);

        try {
            Attendee attendee = attendeeRepo.findById(attendeeId);
            if (attendee == null) {
                System.out.println("  ✗ Attendee not found");
                return false;
            }

            attendee.addActivity(activity);
            attendeeRepo.save(attendee);

            System.out.println("  ✓ Activity recorded: " + activity);
            return true;

        } catch (Exception e) {
            System.err.println("  ✗ Error recording activity: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get activity history for attendee
     *
     * @param attendeeId ID of attendee
     * @return List of recent activities
     */
    public List<String> getActivityHistory(UUID attendeeId) {
        System.out.println("📜 [AttendeeProfileService] Getting activity history for: " + attendeeId);

        try {
            Attendee attendee = attendeeRepo.findById(attendeeId);
            if (attendee == null) {
                System.out.println("  ✗ Attendee not found");
                return new ArrayList<>();
            }

            List<String> history = attendee.getActivityHistory();
            System.out.println("  ✓ Retrieved " + (history != null ? history.size() : 0) + " activities");

            return history != null ? new ArrayList<>(history) : new ArrayList<>();

        } catch (Exception e) {
            System.err.println("  ✗ Error getting activity history: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get last activity timestamp for attendee
     *
     * @param attendee The attendee
     * @return Formatted last activity date or "No activity"
     */
    private String getLastActivityDate(Attendee attendee) {
        List<String> history = attendee.getActivityHistory();
        if (history == null || history.isEmpty()) {
            return "No activity";
        }
        return formatDate(new Date()); // TODO: Track actual timestamps
    }

    /**
     * Format date for display
     *
     * @param date Date to format
     * @return Formatted date string
     */
    private String formatDate(Date date) {
        if (date == null) return "N/A";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(date);
    }
}

