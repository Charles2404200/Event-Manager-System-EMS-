package org.ems.application.service.attendee;

import org.ems.domain.model.Attendee;
import org.ems.domain.model.Event;
import org.ems.domain.model.Session;
import org.ems.domain.model.Ticket;
import org.ems.domain.repository.AttendeeRepository;
import org.ems.domain.repository.EventRepository;
import org.ems.domain.repository.SessionRepository;
import org.ems.domain.repository.TicketRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for loading and retrieving attendee data with batch optimization
 * Implements Single Responsibility Principle - only handles data loading
 * Uses batch loading pattern to minimize database queries
 * Implements Dependency Inversion Principle - depends on abstractions (repositories)
 *
 * @author <your group number>
 */
public class AttendeeLoaderService {

    private final AttendeeRepository attendeeRepo;
    private final EventRepository eventRepo;
    private final SessionRepository sessionRepo;
    private final TicketRepository ticketRepo;

    public AttendeeLoaderService(AttendeeRepository attendeeRepo,
                               EventRepository eventRepo,
                               SessionRepository sessionRepo,
                               TicketRepository ticketRepo) {
        this.attendeeRepo = attendeeRepo;
        this.eventRepo = eventRepo;
        this.sessionRepo = sessionRepo;
        this.ticketRepo = ticketRepo;
    }

    /**
     * Load single attendee by ID
     *
     * @param attendeeId ID of attendee to load
     * @return Attendee object or null if not found
     */
    public Attendee loadAttendeeById(UUID attendeeId) {
        return attendeeRepo.findById(attendeeId);
    }

    /**
     * Load all attendees with batch optimization
     *
     * @return List of all attendees
     */
    public List<Attendee> loadAllAttendees() {
        System.out.println("📥 [AttendeeLoaderService] Loading all attendees...");
        List<Attendee> attendees = attendeeRepo.findAll();
        if (attendees == null) {
            attendees = new ArrayList<>();
        }
        System.out.println("  ✓ Loaded " + attendees.size() + " attendees");
        return attendees;
    }

    /**
     * Load attendees by IDs with batch optimization
     * O(1) map lookup instead of individual queries
     *
     * @param attendeeIds List of attendee IDs
     * @return Map of attendeeId -> Attendee
     */
    public Map<UUID, Attendee> loadAttendeesByIdBatch(List<UUID> attendeeIds) {
        if (attendeeIds == null || attendeeIds.isEmpty()) {
            return new HashMap<>();
        }

        System.out.println("📥 [AttendeeLoaderService] Batch loading " + attendeeIds.size() + " attendees...");
        List<Attendee> allAttendees = loadAllAttendees();

        Map<UUID, Attendee> attendeeMap = allAttendees.stream()
                .filter(a -> attendeeIds.contains(a.getId()))
                .collect(Collectors.toMap(Attendee::getId, a -> a));

        System.out.println("  ✓ Batch loaded " + attendeeMap.size() + " attendees");
        return attendeeMap;
    }

    /**
     * Load events for an attendee
     *
     * @param attendee The attendee
     * @return List of events the attendee is registered for
     */
    public List<Event> loadAttendeeEvents(Attendee attendee) {
        if (attendee == null || attendee.getRegisteredEventIds() == null) {
            return new ArrayList<>();
        }

        System.out.println("📥 [AttendeeLoaderService] Loading events for " + attendee.getFullName());
        List<Event> allEvents = eventRepo.findAll();
        if (allEvents == null) {
            return new ArrayList<>();
        }

        List<UUID> registeredEventIds = attendee.getRegisteredEventIds();
        List<Event> attendeeEvents = allEvents.stream()
                .filter(e -> registeredEventIds.contains(e.getId()))
                .collect(Collectors.toList());

        System.out.println("  ✓ Loaded " + attendeeEvents.size() + " events");
        return attendeeEvents;
    }

    /**
     * Load sessions for an attendee
     *
     * @param attendee The attendee
     * @return List of sessions the attendee is registered for
     */
    public List<Session> loadAttendeeSessions(Attendee attendee) {
        if (attendee == null || attendee.getRegisteredSessionIds() == null) {
            return new ArrayList<>();
        }

        System.out.println("📥 [AttendeeLoaderService] Loading sessions for " + attendee.getFullName());
        List<Session> allSessions = sessionRepo.findAll();
        if (allSessions == null) {
            return new ArrayList<>();
        }

        List<UUID> registeredSessionIds = attendee.getRegisteredSessionIds();
        List<Session> attendeeSessions = allSessions.stream()
                .filter(s -> registeredSessionIds.contains(s.getId()))
                .collect(Collectors.toList());

        System.out.println("  ✓ Loaded " + attendeeSessions.size() + " sessions");
        return attendeeSessions;
    }

    /**
     * Load tickets for an attendee
     *
     * @param attendee The attendee
     * @return List of tickets owned by attendee
     */
    public List<Ticket> loadAttendeeTickets(Attendee attendee) {
        if (attendee == null) {
            return new ArrayList<>();
        }

        System.out.println("📥 [AttendeeLoaderService] Loading tickets for " + attendee.getFullName());
        List<Ticket> tickets = ticketRepo.findByAttendee(attendee.getId());
        if (tickets == null) {
            tickets = new ArrayList<>();
        }
        System.out.println("  ✓ Loaded " + tickets.size() + " tickets");
        return tickets;
    }

    /**
     * Load complete attendee profile with all related data
     *
     * @param attendeeId ID of attendee
     * @return Map containing all loaded data with keys: "attendee", "events", "sessions", "tickets"
     */
    public Map<String, Object> loadCompleteAttendeeProfile(UUID attendeeId) {
        System.out.println("📥 [AttendeeLoaderService] Loading complete profile for attendee: " + attendeeId);
        Map<String, Object> profile = new HashMap<>();

        long startTime = System.currentTimeMillis();

        // Load attendee
        Attendee attendee = loadAttendeeById(attendeeId);
        if (attendee == null) {
            System.out.println("  ✗ Attendee not found");
            return profile;
        }
        profile.put("attendee", attendee);

        // Load related data
        List<Event> events = loadAttendeeEvents(attendee);
        List<Session> sessions = loadAttendeeSessions(attendee);
        List<Ticket> tickets = loadAttendeeTickets(attendee);

        profile.put("events", events);
        profile.put("sessions", sessions);
        profile.put("tickets", tickets);

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("  ✓ Complete profile loaded in " + duration + "ms");

        return profile;
    }

    /**
     * Count total attendees in system
     *
     * @return Total number of attendees
     */
    public long countAttendees() {
        return attendeeRepo.count();
    }
}

