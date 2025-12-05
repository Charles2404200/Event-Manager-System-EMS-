package org.ems.application.impl;

import org.ems.application.service.IdentityService;
import org.ems.config.AppContext;
import org.ems.config.DatabaseConfig;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Person;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.Session;
import org.ems.domain.model.Event;
import org.ems.domain.repository.AttendeeRepository;
import org.ems.domain.repository.PresenterRepository;
import org.ems.domain.dto.PresenterStatisticsDTO;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @author <your group number>
 */
public class IdentityServiceImpl implements IdentityService {

    private final AttendeeRepository attendeeRepo;
    private final PresenterRepository presenterRepo;

    public IdentityServiceImpl(
            AttendeeRepository attendeeRepo,
            PresenterRepository presenterRepo
    ) {
        this.attendeeRepo = attendeeRepo;
        this.presenterRepo = presenterRepo;
    }

    // ATTENDEE
    @Override
    public Attendee createAttendee(Attendee a) {
        return attendeeRepo.save(a);
    }

    @Override
    public Attendee updateAttendee(Attendee a) {
        return attendeeRepo.save(a);
    }

    @Override
    public Attendee getAttendee(UUID id) {
        return attendeeRepo.findById(id);
    }

    @Override
    public boolean deleteAttendee(UUID id) {
        attendeeRepo.delete(id);
        return true;
    }

    @Override
    public Person login(String email, String password) {
        // Try to match by email or username in attendees
        try {
            if (attendeeRepo != null) {
                List<Attendee> attendees = attendeeRepo.findAll();
                for (Attendee attendee : attendees) {
                    if (attendee.getEmail() != null && attendee.getEmail().equals(email)) {
                        if (attendee.getPasswordHash() != null && attendee.getPasswordHash().equals(password)) {
                            return attendee;
                        }
                    }
                    if (attendee.getUsername() != null && attendee.getUsername().equals(email)) {
                        if (attendee.getPasswordHash() != null && attendee.getPasswordHash().equals(password)) {
                            return attendee;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error querying attendees: " + e.getMessage());
        }

        // Try to match by email or username in presenters
        try {
            if (presenterRepo != null) {
                List<Presenter> presenters = presenterRepo.findAll();
                for (Presenter presenter : presenters) {
                    if (presenter.getEmail() != null && presenter.getEmail().equals(email)) {
                        if (presenter.getPasswordHash() != null && presenter.getPasswordHash().equals(password)) {
                            return presenter;
                        }
                    }
                    if (presenter.getUsername() != null && presenter.getUsername().equals(email)) {
                        if (presenter.getPasswordHash() != null && presenter.getPasswordHash().equals(password)) {
                            return presenter;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error querying presenters: " + e.getMessage());
        }

        // Fallback: Try to find admin or other system users directly from persons table
        try {
            String query = "SELECT * FROM persons WHERE (email = ? OR username = ?) AND password_hash = ?";
            java.sql.Connection conn = DatabaseConfig.getConnection();
            try (java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, email);
                ps.setString(2, email);
                ps.setString(3, password);
                java.sql.ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    // Create a generic Person object for admin/system users
                    Person p = new Person() {};
                    p.setId((java.util.UUID) rs.getObject("id"));
                    p.setFullName(rs.getString("full_name"));
                    p.setDateOfBirth(rs.getObject("dob", java.time.LocalDate.class));
                    p.setEmail(rs.getString("email"));
                    p.setPhone(rs.getString("phone"));
                    p.setUsername(rs.getString("username"));
                    p.setPasswordHash(rs.getString("password_hash"));
                    p.setRole(org.ems.domain.model.enums.Role.valueOf(rs.getString("role")));
                    return p;
                }
            }
        } catch (Exception e) {
            System.err.println("Error querying persons table for admin: " + e.getMessage());
        }

        return null; // Login failed
    }

    // PRESENTER
    @Override
    public Presenter createPresenter(Presenter p) {
        return presenterRepo.save(p);
    }

    @Override
    public Presenter updatePresenter(Presenter p) {
        return presenterRepo.save(p);
    }

    @Override
    public Presenter getPresenter(UUID id) {
        return presenterRepo.findById(id);
    }

    @Override
    public boolean deletePresenter(UUID id) {
        presenterRepo.delete(id);
        return true;
    }

    @Override
    public List<Presenter> searchPresenters(String name) {
        return presenterRepo.findAll().stream()
                .filter(p -> p.getFullName().toLowerCase().contains(name.toLowerCase()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Presenter> getAllPresenters() {
        return presenterRepo.findAll();
    }

    @Override
    public List<Attendee> getAllAttendees() {
        return attendeeRepo.findAll();
    }

    @Override
    public Person getUserById(UUID id) {
        // Try to get from presenters first
        Presenter presenter = presenterRepo.findById(id);
        if (presenter != null) {
            return presenter;
        }
        // Then try attendees
        Attendee attendee = attendeeRepo.findById(id);
        if (attendee != null) {
            return attendee;
        }
        return null;
    }

    // NEW: PRESENTER STATISTICS
    @Override
    public PresenterStatisticsDTO getPresenterStatistics(UUID presenterId) {
        try {
            AppContext ctx = AppContext.get();

            // Get presenter
            Presenter presenter = presenterRepo.findById(presenterId);
            if (presenter == null) {
                return new PresenterStatisticsDTO();
            }

            // Get all sessions for this presenter
            List<Session> allSessions = ctx.sessionRepo.findAll();
            List<Session> presenterSessions = new ArrayList<>();

            for (Session session : allSessions) {
                if (session.getPresenterIds() != null &&
                        session.getPresenterIds().contains(presenterId)) {
                    presenterSessions.add(session);
                }
            }

            // 1. Number of sessions presented
            int totalSessions = presenterSessions.size();

            // 2. Session audience sizes (total and average)
            int totalAudience = 0;
            Map<String, Integer> sessionEngagement = new HashMap<>();

            for (Session session : presenterSessions) {
                // Count attendees for this session
                int attendeeCount = countAttendeesForSession(session.getId());
                totalAudience += attendeeCount;

                // Store engagement data
                String sessionName = session.getTitle() + " (" +
                        session.getId().toString().substring(0, 8) + ")";
                sessionEngagement.put(sessionName, attendeeCount);
            }

            double averageAudience = totalSessions > 0 ?
                    (double) totalAudience / totalSessions : 0.0;

            // 3. Event-type distribution
            Map<String, Integer> eventTypeDistribution = new HashMap<>();

            for (Session session : presenterSessions) {
                if (session.getEventId() != null && ctx.eventRepo != null) {
                    Event event = ctx.eventRepo.findById(session.getEventId());
                    if (event != null) {
                        String eventType = event.getType().name();
                        eventTypeDistribution.put(eventType,
                                eventTypeDistribution.getOrDefault(eventType, 0) + 1);
                    }
                }
            }

            // 4. Session engagement trends (upcoming vs completed)
            int upcomingSessions = 0;
            int completedSessions = 0;
            LocalDateTime now = LocalDateTime.now();

            for (Session session : presenterSessions) {
                if (session.getStart() != null) {
                    if (session.getStart().isAfter(now)) {
                        upcomingSessions++;
                    } else if (session.getEnd() != null && session.getEnd().isBefore(now)) {
                        completedSessions++;
                    }
                }
            }

            return new PresenterStatisticsDTO(
                    totalSessions,
                    totalAudience,
                    averageAudience,
                    eventTypeDistribution,
                    sessionEngagement,
                    upcomingSessions,
                    completedSessions
            );

        } catch (Exception e) {
            System.err.println("Error calculating presenter statistics: " + e.getMessage());
            e.printStackTrace();
            return new PresenterStatisticsDTO();
        }
    }

    /**
     * Count number of attendees registered for a specific session
     */
    private int countAttendeesForSession(UUID sessionId) {
        try {
            AppContext ctx = AppContext.get();

            if (ctx.ticketRepo == null) {
                return 0;
            }

            // Count tickets for this session
            List<org.ems.domain.model.Ticket> tickets = ctx.ticketRepo.findBySession(sessionId);

            // Count unique attendees
            Set<UUID> uniqueAttendees = new HashSet<>();
            for (org.ems.domain.model.Ticket ticket : tickets) {
                if (ticket.getAttendeeId() != null) {
                    uniqueAttendees.add(ticket.getAttendeeId());
                }
            }

            return uniqueAttendees.size();

        } catch (Exception e) {
            System.err.println("Error counting attendees: " + e.getMessage());
            return 0;
        }
    }
}