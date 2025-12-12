package org.ems.application.service.session;

import org.ems.application.service.event.EventService;
import org.ems.domain.model.Session;
import org.ems.infrastructure.config.AppContext;

import java.util.List;
import java.util.UUID;

/**
 * SessionManagementService - Handles session CRUD operations
 * Single Responsibility: Manage session business logic
 *
 * @author EMS Team
 */
public class SessionManagementService {

    private final EventService eventService;

    /**
     * Constructor with dependency injection
     */
    public SessionManagementService(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Convenience constructor using AppContext (for simple cases)
     */
    public SessionManagementService() {
        this(AppContext.get().eventService);
    }

    /**
     * Retrieve all sessions
     * @return List of all sessions
     */
    public List<Session> getAllSessions() {
        long start = System.currentTimeMillis();
        System.out.println("📋 [SessionManagementService] Loading all sessions...");

        try {
            List<Session> sessions = eventService.getSessions();
            System.out.println("  ✓ Loaded " + sessions.size() + " sessions in " +
                    (System.currentTimeMillis() - start) + " ms");
            return sessions;
        } catch (Exception e) {
            System.err.println("✗ Error loading sessions: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Create a new session
     * @param session The session to create
     */
    public void createSession(Session session) {
        long start = System.currentTimeMillis();
        System.out.println("➕ [SessionManagementService] Creating session: " + session.getTitle());

        try {
            eventService.createSession(session);
            System.out.println("  ✓ Session created in " + (System.currentTimeMillis() - start) + " ms");
        } catch (Exception e) {
            System.err.println("✗ Error creating session: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Update an existing session
     * @param session The session to update
     */
    public void updateSession(Session session) {
        long start = System.currentTimeMillis();
        System.out.println("✏️ [SessionManagementService] Updating session: " + session.getTitle());

        try {
            eventService.updateSession(session);
            System.out.println("  ✓ Session updated in " + (System.currentTimeMillis() - start) + " ms");
        } catch (Exception e) {
            System.err.println("✗ Error updating session: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Delete a session by ID
     * @param sessionId The ID of the session to delete
     */
    public void deleteSession(UUID sessionId) {
        long start = System.currentTimeMillis();
        System.out.println("🗑️ [SessionManagementService] Deleting session: " + sessionId);

        try {
            eventService.deleteSession(sessionId);
            System.out.println("  ✓ Session deleted in " + (System.currentTimeMillis() - start) + " ms");
        } catch (Exception e) {
            System.err.println("✗ Error deleting session: " + e.getMessage());
            throw e;
        }
    }
}

