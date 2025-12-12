package org.ems.application.service.attendee;

import org.ems.application.dto.attendee.AttendeeDisplayDTO;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Ticket;
import org.ems.domain.repository.TicketRepository;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for filtering and searching attendee data
 * Implements Single Responsibility Principle - only handles filtering logic
 * Delegates data loading to AttendeeLoaderService
 * Implements Dependency Inversion Principle - depends on abstractions (repositories)
 *
 * @author <your group number>
 */
public class AttendeeFilteringService {

    private final AttendeeLoaderService loaderService;
    private final TicketRepository ticketRepo;

    public AttendeeFilteringService(AttendeeLoaderService loaderService,
                                   TicketRepository ticketRepo) {
        this.loaderService = loaderService;
        this.ticketRepo = ticketRepo;
    }

    /**
     * Filter attendees by name
     *
     * @param searchTerm Name search term
     * @return List of matching attendees
     */
    public List<Attendee> filterByName(String searchTerm) {
        System.out.println("🔍 [AttendeeFilteringService] Filtering by name: " + searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Attendee> allAttendees = loaderService.loadAllAttendees();
        String lowerSearchTerm = searchTerm.toLowerCase();

        List<Attendee> filtered = allAttendees.stream()
                .filter(a -> a.getFullName() != null && a.getFullName().toLowerCase().contains(lowerSearchTerm))
                .collect(Collectors.toList());

        System.out.println("  ✓ Found " + filtered.size() + " attendees matching: " + searchTerm);
        return filtered;
    }

    /**
     * Filter attendees by email
     *
     * @param email Email address
     * @return Attendee if found, null otherwise
     */
    public Attendee filterByEmail(String email) {
        System.out.println("🔍 [AttendeeFilteringService] Filtering by email: " + email);

        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        List<Attendee> allAttendees = loaderService.loadAllAttendees();

        Optional<Attendee> found = allAttendees.stream()
                .filter(a -> a.getEmail() != null && a.getEmail().equalsIgnoreCase(email))
                .findFirst();

        if (found.isPresent()) {
            System.out.println("  ✓ Found attendee with email: " + email);
            return found.get();
        } else {
            System.out.println("  ✓ No attendee found with email: " + email);
            return null;
        }
    }

    /**
     * Filter attendees by username
     *
     * @param username Username
     * @return Attendee if found, null otherwise
     */
    public Attendee filterByUsername(String username) {
        System.out.println("🔍 [AttendeeFilteringService] Filtering by username: " + username);

        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        List<Attendee> allAttendees = loaderService.loadAllAttendees();

        Optional<Attendee> found = allAttendees.stream()
                .filter(a -> a.getUsername() != null && a.getUsername().equalsIgnoreCase(username))
                .findFirst();

        if (found.isPresent()) {
            System.out.println("  ✓ Found attendee with username: " + username);
            return found.get();
        } else {
            System.out.println("  ✓ No attendee found with username: " + username);
            return null;
        }
    }

    /**
     * Filter attendees by minimum registered events count
     *
     * @param minEvents Minimum number of registered events
     * @return List of attendees with at least minEvents registrations
     */
    public List<Attendee> filterByMinimumEvents(int minEvents) {
        System.out.println("🔍 [AttendeeFilteringService] Filtering by minimum events: " + minEvents);

        List<Attendee> allAttendees = loaderService.loadAllAttendees();

        List<Attendee> filtered = allAttendees.stream()
                .filter(a -> a.getRegisteredEventIds() != null && a.getRegisteredEventIds().size() >= minEvents)
                .collect(Collectors.toList());

        System.out.println("  ✓ Found " + filtered.size() + " attendees with at least " + minEvents + " events");
        return filtered;
    }

    /**
     * Filter attendees with active tickets
     *
     * @return List of attendees with at least one ticket
     */
    public List<Attendee> filterWithActiveTickets() {
        System.out.println("🔍 [AttendeeFilteringService] Filtering attendees with active tickets");

        List<Attendee> allAttendees = loaderService.loadAllAttendees();

        List<Attendee> filtered = allAttendees.stream()
                .filter(a -> {
                    List<Ticket> tickets = ticketRepo.findByAttendee(a.getId());
                    return tickets != null && !tickets.isEmpty();
                })
                .collect(Collectors.toList());

        System.out.println("  ✓ Found " + filtered.size() + " attendees with active tickets");
        return filtered;
    }

    /**
     * Convert attendee to display DTO
     *
     * @param attendee Attendee to convert
     * @return AttendeeDisplayDTO
     */
    public AttendeeDisplayDTO toDisplayDTO(Attendee attendee) {
        if (attendee == null) {
            return null;
        }

        int eventCount = attendee.getRegisteredEventIds() != null ? attendee.getRegisteredEventIds().size() : 0;
        int sessionCount = attendee.getRegisteredSessionIds() != null ? attendee.getRegisteredSessionIds().size() : 0;
        int ticketCount = attendee.getTicketIds() != null ? attendee.getTicketIds().size() : 0;

        return new AttendeeDisplayDTO(
                attendee.getId().toString(),
                attendee.getFullName(),
                attendee.getEmail(),
                attendee.getPhone(),
                attendee.getUsername(),
                eventCount,
                sessionCount,
                ticketCount,
                formatDate(new Date())
        );
    }

    /**
     * Convert list of attendees to display DTOs
     *
     * @param attendees List of attendees
     * @return List of AttendeeDisplayDTOs
     */
    public List<AttendeeDisplayDTO> toDisplayDTOs(List<Attendee> attendees) {
        if (attendees == null) {
            return new ArrayList<>();
        }

        return attendees.stream()
                .map(this::toDisplayDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get paginated list of attendees
     *
     * @param pageNumber Page number (starting from 1)
     * @param pageSize Number of items per page
     * @return List of attendees for the specified page
     */
    public List<AttendeeDisplayDTO> getPaginatedAttendees(int pageNumber, int pageSize) {
        System.out.println("📄 [AttendeeFilteringService] Getting page " + pageNumber + " (size: " + pageSize + ")");

        if (pageNumber < 1 || pageSize < 1) {
            System.out.println("  ✗ Invalid page parameters");
            return new ArrayList<>();
        }

        List<Attendee> allAttendees = loaderService.loadAllAttendees();
        int startIdx = (pageNumber - 1) * pageSize;
        int endIdx = Math.min(startIdx + pageSize, allAttendees.size());

        if (startIdx >= allAttendees.size()) {
            System.out.println("  ✗ Page number out of range");
            return new ArrayList<>();
        }

        List<Attendee> pageAttendees = allAttendees.subList(startIdx, endIdx);
        List<AttendeeDisplayDTO> displayDTOs = toDisplayDTOs(pageAttendees);

        System.out.println("  ✓ Retrieved " + displayDTOs.size() + " attendees");
        return displayDTOs;
    }

    /**
     * Format date for display
     *
     * @param date Date to format
     * @return Formatted date string
     */
    private String formatDate(Date date) {
        if (date == null) return "N/A";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }
}

