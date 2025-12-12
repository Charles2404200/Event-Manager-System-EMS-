package org.ems.application.service.attendee;

import org.ems.application.dto.attendee.AttendeeStatisticsDTO;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Event;
import org.ems.domain.model.Ticket;
import org.ems.domain.repository.EventRepository;
import org.ems.domain.repository.TicketRepository;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating attendee statistics and analytics
 * Implements Single Responsibility Principle - only handles statistics
 * Delegates data loading to AttendeeLoaderService
 * Implements Dependency Inversion Principle - depends on abstractions (repositories)
 *
 * @author <your group number>
 */
public class AttendeeStatisticsService {

    private final AttendeeLoaderService loaderService;
    private final EventRepository eventRepo;
    private final TicketRepository ticketRepo;

    public AttendeeStatisticsService(AttendeeLoaderService loaderService,
                                    EventRepository eventRepo,
                                    TicketRepository ticketRepo) {
        this.loaderService = loaderService;
        this.eventRepo = eventRepo;
        this.ticketRepo = ticketRepo;
    }

    /**
     * Generate comprehensive attendee statistics
     *
     * @return AttendeeStatisticsDTO with aggregated data
     */
    public AttendeeStatisticsDTO generateStatistics() {
        System.out.println("📊 [AttendeeStatisticsService] Generating attendee statistics...");

        long startTime = System.currentTimeMillis();

        // Load all attendees
        List<Attendee> allAttendees = loaderService.loadAllAttendees();
        int totalAttendees = allAttendees.size();

        // Count active attendees (with registrations)
        int activeAttendees = (int) allAttendees.stream()
                .filter(a -> a.getRegisteredEventIds() != null && !a.getRegisteredEventIds().isEmpty())
                .count();

        // Calculate total registrations
        int totalRegistrations = allAttendees.stream()
                .mapToInt(a -> a.getRegisteredEventIds() != null ? a.getRegisteredEventIds().size() : 0)
                .sum();

        // Load all events for distribution analysis
        List<Event> allEvents = eventRepo.findAll();
        if (allEvents == null) {
            allEvents = new ArrayList<>();
        }

        // Generate event type distribution
        Map<String, Integer> eventTypeDistribution = generateEventTypeDistribution(allAttendees, allEvents);

        // Calculate ticket statistics
        int totalTickets = countTotalTickets(allAttendees);

        // Calculate averages
        double avgEventsPerAttendee = totalAttendees > 0 ?
                (double) totalRegistrations / totalAttendees : 0;

        int totalSessionRegistrations = allAttendees.stream()
                .mapToInt(a -> a.getRegisteredSessionIds() != null ? a.getRegisteredSessionIds().size() : 0)
                .sum();

        double avgSessionsPerAttendee = totalAttendees > 0 ?
                (double) totalSessionRegistrations / totalAttendees : 0;

        // Generate registration trend
        Map<String, Integer> registrationTrend = generateRegistrationTrend(allAttendees);

        long duration = System.currentTimeMillis() - startTime;
        String reportDate = formatDate(new Date());

        System.out.println("  ✓ Statistics generated in " + duration + "ms");
        System.out.println("    - Total Attendees: " + totalAttendees);
        System.out.println("    - Active Attendees: " + activeAttendees);
        System.out.println("    - Total Registrations: " + totalRegistrations);

        return new AttendeeStatisticsDTO(
                totalAttendees,
                activeAttendees,
                totalRegistrations,
                totalTickets,
                avgEventsPerAttendee,
                avgSessionsPerAttendee,
                eventTypeDistribution,
                registrationTrend,
                reportDate
        );
    }

    /**
     * Get attendee registration statistics by event type
     *
     * @return Map of event type to count
     */
    private Map<String, Integer> generateEventTypeDistribution(List<Attendee> attendees,
                                                               List<Event> events) {
        Map<String, Integer> distribution = new HashMap<>();

        if (events == null || events.isEmpty()) {
            return distribution;
        }

        for (Attendee attendee : attendees) {
            if (attendee.getRegisteredEventIds() == null) {
                continue;
            }

            for (UUID eventId : attendee.getRegisteredEventIds()) {
                Event event = events.stream()
                        .filter(e -> e.getId().equals(eventId))
                        .findFirst()
                        .orElse(null);

                if (event != null && event.getType() != null) {
                    String eventType = event.getType().name();
                    distribution.put(eventType, distribution.getOrDefault(eventType, 0) + 1);
                }
            }
        }

        System.out.println("    ✓ Event type distribution generated: " + distribution);
        return distribution;
    }

    /**
     * Generate registration trend over months
     *
     * @return Map of month to registration count
     */
    private Map<String, Integer> generateRegistrationTrend(List<Attendee> attendees) {
        Map<String, Integer> trend = new LinkedHashMap<>();

        // Initialize last 12 months
        Calendar cal = Calendar.getInstance();
        for (int i = 11; i >= 0; i--) {
            cal.add(Calendar.MONTH, -1);
            String monthKey = new SimpleDateFormat("yyyy-MM").format(cal.getTime());
            trend.put(monthKey, 0);
        }

        // TODO: Implement actual registration date tracking
        // For now, distribute registrations evenly across recent months
        if (!attendees.isEmpty()) {
            int perMonth = Math.max(1, attendees.size() / 12);
            trend.values().forEach(v -> trend.put(
                    trend.keySet().iterator().next(),
                    perMonth
            ));
        }

        System.out.println("    ✓ Registration trend calculated");
        return trend;
    }

    /**
     * Count total tickets for all attendees
     *
     * @param attendees List of attendees
     * @return Total ticket count
     */
    private int countTotalTickets(List<Attendee> attendees) {
        int totalTickets = 0;

        for (Attendee attendee : attendees) {
            List<Ticket> tickets = ticketRepo.findByAttendee(attendee.getId());
            if (tickets != null) {
                totalTickets += tickets.size();
            }
        }

        return totalTickets;
    }

    /**
     * Get statistics for single attendee
     *
     * @param attendeeId Attendee ID
     * @return Statistics for the attendee
     */
    public Map<String, Object> getAttendeeStatistics(UUID attendeeId) {
        System.out.println("📊 [AttendeeStatisticsService] Getting statistics for attendee: " + attendeeId);

        Attendee attendee = loaderService.loadAttendeeById(attendeeId);
        if (attendee == null) {
            System.out.println("  ✗ Attendee not found");
            return new HashMap<>();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("attendeeId", attendeeId.toString());
        stats.put("registeredEvents", attendee.getRegisteredEventIds() != null ?
                attendee.getRegisteredEventIds().size() : 0);
        stats.put("registeredSessions", attendee.getRegisteredSessionIds() != null ?
                attendee.getRegisteredSessionIds().size() : 0);

        List<Ticket> tickets = ticketRepo.findByAttendee(attendeeId);
        stats.put("totalTickets", tickets != null ? tickets.size() : 0);

        List<String> activities = attendee.getActivityHistory();
        stats.put("totalActivities", activities != null ? activities.size() : 0);

        System.out.println("  ✓ Statistics retrieved");
        return stats;
    }

    /**
     * Get top attendees by registration count
     *
     * @param limit Number of top attendees to return
     * @return List of top attendees with their registration counts
     */
    public List<Map<String, Object>> getTopAttendees(int limit) {
        System.out.println("🏆 [AttendeeStatisticsService] Getting top " + limit + " attendees...");

        List<Attendee> allAttendees = loaderService.loadAllAttendees();

        return allAttendees.stream()
                .map(a -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("attendee", a);
                    data.put("registrationCount", a.getRegisteredEventIds() != null ?
                            a.getRegisteredEventIds().size() : 0);
                    return data;
                })
                .sorted((a, b) -> Integer.compare(
                        (int) b.get("registrationCount"),
                        (int) a.get("registrationCount")
                ))
                .limit(limit)
                .collect(Collectors.toList());
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

