package org.ems.application.service.dashboard;

import org.ems.application.dto.dashboard.DashboardEventAdminContentDTO;
import org.ems.domain.repository.EventRepository;
import org.ems.domain.repository.SessionRepository;
import org.ems.domain.repository.TicketRepository;

/**
 * Service for loading event admin-specific dashboard content
 * Implements Single Responsibility Principle - only handles event admin content
 * @author <your group number>
 */
public class DashboardEventAdminService {

    private final EventRepository eventRepo;
    private final SessionRepository sessionRepo;
    private final TicketRepository ticketRepo;

    public DashboardEventAdminService(EventRepository eventRepo, SessionRepository sessionRepo,
                                     TicketRepository ticketRepo) {
        this.eventRepo = eventRepo;
        this.sessionRepo = sessionRepo;
        this.ticketRepo = ticketRepo;
    }

    /**
     * Load event admin dashboard content (statistics)
     * @return DTO with event admin content
     * @throws DashboardEventAdminException if loading fails
     */
    public DashboardEventAdminContentDTO loadEventAdminContent()
            throws DashboardEventAdminException {

        long start = System.currentTimeMillis();
        System.out.println("📊 [DashboardEventAdminService] Loading event admin content");

        try {
            if (eventRepo == null || sessionRepo == null || ticketRepo == null) {
                throw new DashboardEventAdminException("Repositories not available for event admin content");
            }

            // Load statistics
            long eventStart = System.currentTimeMillis();
            int totalEvents = eventRepo.findAll().size();
            long eventTime = System.currentTimeMillis() - eventStart;
            System.out.println("  ✓ Events counted in " + eventTime + "ms: " + totalEvents);

            long sessionStart = System.currentTimeMillis();
            int totalSessions = sessionRepo.findAll().size();
            long sessionTime = System.currentTimeMillis() - sessionStart;
            System.out.println("  ✓ Sessions counted in " + sessionTime + "ms: " + totalSessions);

            long ticketStart = System.currentTimeMillis();
            int totalTickets = ticketRepo.findAll().size();
            long ticketTime = System.currentTimeMillis() - ticketStart;
            System.out.println("  ✓ Tickets counted in " + ticketTime + "ms: " + totalTickets);

            long elapsed = System.currentTimeMillis() - start;
            System.out.println("  ✓ Event admin content loaded in " + elapsed + "ms");

            return new DashboardEventAdminContentDTO(totalEvents, totalSessions, totalTickets);

        } catch (Exception e) {
            String message = "Failed to load event admin content: " + e.getMessage();
            System.err.println("✗ " + message);
            e.printStackTrace();
            throw new DashboardEventAdminException(message, e);
        }
    }

    /**
     * Custom exception for event admin dashboard errors
     */
    public static class DashboardEventAdminException extends Exception {
        public DashboardEventAdminException(String message) {
            super(message);
        }

        public DashboardEventAdminException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

