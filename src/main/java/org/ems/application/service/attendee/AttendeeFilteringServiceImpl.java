package org.ems.application.service.attendee;

import org.ems.application.dto.attendee.AttendeeDisplayDTO;
import org.ems.domain.model.Attendee;
import org.ems.domain.repository.TicketRepository;

import java.util.List;

/**
 * Implementation example and test class for AttendeeFilteringService
 * Demonstrates how to use the filtering service in real scenarios
 *
 * @author <your group number>
 */
public class AttendeeFilteringServiceImpl {

    private final AttendeeFilteringService filteringService;
    private final AttendeeLoaderService loaderService;

    /**
     * Initialize with services
     */
    public AttendeeFilteringServiceImpl(AttendeeLoaderService loaderService,
                                     TicketRepository ticketRepo) {
        this.loaderService = loaderService;
        this.filteringService = new AttendeeFilteringService(loaderService, ticketRepo);
    }

    /**
     * Example 1: Search attendees by name
     */
    public void exampleSearchByName(String name) {
        System.out.println("\n=== EXAMPLE 1: Search by Name ===");
        System.out.println("Searching for: " + name);

        List<Attendee> results = filteringService.filterByName(name);

        System.out.println("Results found: " + results.size());
        for (Attendee attendee : results) {
            System.out.println("  - " + attendee.getFullName() + " (" + attendee.getEmail() + ")");
        }
    }

    /**
     * Example 2: Find attendee by email
     */
    public void exampleFindByEmail(String email) {
        System.out.println("\n=== EXAMPLE 2: Find by Email ===");
        System.out.println("Searching for email: " + email);

        Attendee attendee = filteringService.filterByEmail(email);

        if (attendee != null) {
            System.out.println("Found: " + attendee.getFullName());
            System.out.println("  Email: " + attendee.getEmail());
            System.out.println("  Phone: " + attendee.getPhone());
            System.out.println("  Username: " + attendee.getUsername());
        } else {
            System.out.println("No attendee found with email: " + email);
        }
    }

    /**
     * Example 3: Find attendee by username
     */
    public void exampleFindByUsername(String username) {
        System.out.println("\n=== EXAMPLE 3: Find by Username ===");
        System.out.println("Searching for username: " + username);

        Attendee attendee = filteringService.filterByUsername(username);

        if (attendee != null) {
            System.out.println("Found: " + attendee.getFullName());
            System.out.println("  Username: " + attendee.getUsername());
            System.out.println("  Email: " + attendee.getEmail());
        } else {
            System.out.println("No attendee found with username: " + username);
        }
    }

    /**
     * Example 4: Get attendees with minimum event registrations
     */
    public void exampleFilterByMinimumEvents(int minEvents) {
        System.out.println("\n=== EXAMPLE 4: Filter by Minimum Events ===");
        System.out.println("Finding attendees with at least " + minEvents + " event registrations");

        List<Attendee> results = filteringService.filterByMinimumEvents(minEvents);

        System.out.println("Results found: " + results.size());
        for (Attendee attendee : results) {
            int eventCount = attendee.getRegisteredEventIds() != null ?
                    attendee.getRegisteredEventIds().size() : 0;
            System.out.println("  - " + attendee.getFullName() + " (" + eventCount + " events)");
        }
    }

    /**
     * Example 5: Get attendees with active tickets
     */
    public void exampleFilterWithActiveTickets() {
        System.out.println("\n=== EXAMPLE 5: Filter with Active Tickets ===");

        List<Attendee> results = filteringService.filterWithActiveTickets();

        System.out.println("Attendees with active tickets: " + results.size());
        for (Attendee attendee : results) {
            System.out.println("  - " + attendee.getFullName());
        }
    }

    /**
     * Example 6: Convert to display DTO
     */
    public void exampleConvertToDisplayDTO(Attendee attendee) {
        System.out.println("\n=== EXAMPLE 6: Convert to Display DTO ===");
        System.out.println("Converting attendee to display DTO");

        AttendeeDisplayDTO displayDTO = filteringService.toDisplayDTO(attendee);

        if (displayDTO != null) {
            System.out.println("Display DTO:");
            System.out.println("  ID: " + displayDTO.getAttendeeId());
            System.out.println("  Name: " + displayDTO.getFullName());
            System.out.println("  Email: " + displayDTO.getEmail());
            System.out.println("  Events: " + displayDTO.getRegisteredEventCount());
            System.out.println("  Sessions: " + displayDTO.getRegisteredSessionCount());
            System.out.println("  Tickets: " + displayDTO.getTicketCount());
        }
    }

    /**
     * Example 7: Convert list to display DTOs
     */
    public void exampleConvertListToDisplayDTOs(List<Attendee> attendees) {
        System.out.println("\n=== EXAMPLE 7: Convert List to Display DTOs ===");
        System.out.println("Converting " + attendees.size() + " attendees to DTOs");

        List<AttendeeDisplayDTO> displayDTOs = filteringService.toDisplayDTOs(attendees);

        System.out.println("Converted: " + displayDTOs.size() + " DTOs");
        for (AttendeeDisplayDTO dto : displayDTOs) {
            System.out.println("  - " + dto.getFullName() + " (" + dto.getTicketCount() + " tickets)");
        }
    }

    /**
     * Example 8: Get paginated attendees
     */
    public void exampleGetPaginatedAttendees(int pageNumber, int pageSize) {
        System.out.println("\n=== EXAMPLE 8: Get Paginated Attendees ===");
        System.out.println("Page: " + pageNumber + ", Size: " + pageSize);

        List<AttendeeDisplayDTO> page = filteringService.getPaginatedAttendees(pageNumber, pageSize);

        System.out.println("Page results: " + page.size() + " items");
        for (int i = 0; i < page.size(); i++) {
            AttendeeDisplayDTO dto = page.get(i);
            System.out.println("  " + (i + 1) + ". " + dto.getFullName() +
                    " (" + dto.getTicketCount() + " tickets)");
        }
    }

    /**
     * Example 9: Complex filtering scenario
     */
    public void exampleComplexFiltering() {
        System.out.println("\n=== EXAMPLE 9: Complex Filtering Scenario ===");
        System.out.println("Finding active attendees with multiple registrations and tickets");

        // Step 1: Get attendees with minimum 2 event registrations
        List<Attendee> activeAttendees = filteringService.filterByMinimumEvents(2);
        System.out.println("Step 1: Found " + activeAttendees.size() + " attendees with 2+ events");

        // Step 2: Filter those with active tickets
        List<Attendee> withTickets = filteringService.filterWithActiveTickets();
        System.out.println("Step 2: " + withTickets.size() + " have active tickets");

        // Step 3: Convert to DTOs for display
        List<AttendeeDisplayDTO> displayDTOs = filteringService.toDisplayDTOs(activeAttendees);
        System.out.println("Step 3: Converted to " + displayDTOs.size() + " display DTOs");

        // Display results
        System.out.println("\nResults:");
        for (AttendeeDisplayDTO dto : displayDTOs) {
            System.out.println("  - " + dto.getFullName());
            System.out.println("    Events: " + dto.getRegisteredEventCount());
            System.out.println("    Sessions: " + dto.getRegisteredSessionCount());
            System.out.println("    Tickets: " + dto.getTicketCount());
        }
    }

    /**
     * Example 10: Pagination with search
     */
    public void examplePaginationWithSearch(String searchTerm, int pageSize) {
        System.out.println("\n=== EXAMPLE 10: Pagination with Search ===");
        System.out.println("Searching for: '" + searchTerm + "', Page size: " + pageSize);

        // Search
        List<Attendee> searchResults = filteringService.filterByName(searchTerm);
        System.out.println("Search results: " + searchResults.size() + " attendees found");

        // Calculate pages
        int totalPages = (int) Math.ceil((double) searchResults.size() / pageSize);
        System.out.println("Total pages: " + totalPages);

        // Show first page
        if (!searchResults.isEmpty()) {
            List<Attendee> firstPage = searchResults.subList(
                    0,
                    Math.min(pageSize, searchResults.size())
            );
            List<AttendeeDisplayDTO> displayDTOs = filteringService.toDisplayDTOs(firstPage);

            System.out.println("\nFirst page (" + displayDTOs.size() + " items):");
            for (AttendeeDisplayDTO dto : displayDTOs) {
                System.out.println("  - " + dto.getFullName() + " (" + dto.getEmail() + ")");
            }
        }
    }

    /**
     * Run all examples
     */
    public void runAllExamples() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("AttendeeFilteringService Implementation Examples");
        System.out.println("=".repeat(60));

        try {
            // Example 1: Search by name
            exampleSearchByName("John");

            // Example 2: Find by email
            exampleFindByEmail("john@example.com");

            // Example 3: Find by username
            exampleFindByUsername("johndoe");

            // Example 4: Filter by minimum events
            exampleFilterByMinimumEvents(2);

            // Example 5: Filter with active tickets
            exampleFilterWithActiveTickets();

            // Example 8: Pagination
            exampleGetPaginatedAttendees(1, 10);

            // Example 9: Complex filtering
            exampleComplexFiltering();

            // Example 10: Pagination with search
            examplePaginationWithSearch("John", 5);

            System.out.println("\n" + "=".repeat(60));
            System.out.println("All examples completed successfully!");
            System.out.println("=".repeat(60) + "\n");

        } catch (Exception e) {
            System.err.println("Error running examples: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

