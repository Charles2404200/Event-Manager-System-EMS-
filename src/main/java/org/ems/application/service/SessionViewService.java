package org.ems.application.service;

import org.ems.application.dto.SessionViewDTO;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.Session;
import org.ems.domain.repository.SessionRepository;
import org.ems.domain.repository.PresenterRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SessionViewService - Loads and prepares sessions for display
 * Single Responsibility: Load sessions + resolve presenters
 *
 * @author EMS Team
 */
public class SessionViewService {

    private final SessionRepository sessionRepo;
    private final PresenterRepository presenterRepo;
    private final Map<UUID, Presenter> presenterCache = new HashMap<>();
    private final Map<UUID, List<Session>> sessionCache = new HashMap<>();

    public SessionViewService(SessionRepository sessionRepo, PresenterRepository presenterRepo) {
        this.sessionRepo = sessionRepo;
        this.presenterRepo = presenterRepo;
    }

    /**
     * Load and prepare sessions for an event
     */
    public List<SessionViewDTO> loadSessions(UUID eventId) {
        long start = System.currentTimeMillis();
        System.out.println("[SessionViewService] loadSessions(" + eventId + ") starting...");

        try {
            // Check cache
            List<Session> sessions = sessionCache.getOrDefault(eventId, new ArrayList<>());

            if (sessions.isEmpty() && sessionRepo != null) {
                long dbStart = System.currentTimeMillis();
                sessions = sessionRepo.findByEvent(eventId);
                System.out.println("  ✓ Loaded " + sessions.size() + " sessions in " +
                        (System.currentTimeMillis() - dbStart) + " ms");
                sessionCache.put(eventId, sessions);
            }

            if (sessions.isEmpty()) {
                System.out.println("[SessionViewService] No sessions found for event: " + eventId);
                return Collections.emptyList();
            }

            // Batch load missing presenters
            long presStart = System.currentTimeMillis();
            preloadPresenters(sessions);
            System.out.println("  ✓ Presenters loaded in " + (System.currentTimeMillis() - presStart) + " ms");

            // Convert to DTOs
            List<SessionViewDTO> dtos = sessions.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            System.out.println("✓ loadSessions completed in " + (System.currentTimeMillis() - start) + " ms");
            return dtos;

        } catch (Exception e) {
            System.err.println("✗ Error loading sessions: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Preload all missing presenters (batch operation)
     */
    private void preloadPresenters(List<Session> sessions) {
        Set<UUID> missingPresenterIds = new HashSet<>();

        for (Session session : sessions) {
            if (session.getPresenterIds() != null) {
                for (UUID pid : session.getPresenterIds()) {
                    if (!presenterCache.containsKey(pid)) {
                        missingPresenterIds.add(pid);
                    }
                }
            }
        }

        if (!missingPresenterIds.isEmpty() && presenterRepo != null) {
            for (UUID pid : missingPresenterIds) {
                try {
                    Presenter p = presenterRepo.findById(pid);
                    if (p != null) {
                        presenterCache.put(pid, p);
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Could not load presenter " + pid + ": " + e.getMessage());
                }
            }
            System.out.println("[SessionViewService] Preloaded " + missingPresenterIds.size() + " presenters");
        }
    }

    /**
     * Convert Session to SessionViewDTO
     */
    private SessionViewDTO convertToDTO(Session session) {
        // Get presenter names from cache
        List<String> presenterNames = new ArrayList<>();
        if (session.getPresenterIds() != null) {
            for (UUID pid : session.getPresenterIds()) {
                Presenter p = presenterCache.get(pid);
                if (p != null) {
                    presenterNames.add(p.getFullName());
                }
            }
        }

        return new SessionViewDTO(
                session.getId(),
                session.getTitle(),
                session.getStart() != null ? session.getStart().toString() : "N/A",
                session.getEnd() != null ? session.getEnd().toString() : "N/A",
                session.getVenue(),
                session.getCapacity(),
                presenterNames
        );
    }

    /**
     * Clear caches
     */
    public void clearCaches() {
        sessionCache.clear();
        presenterCache.clear();
    }
}

