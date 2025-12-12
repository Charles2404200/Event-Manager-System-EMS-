package org.ems.application.service.session;

import org.ems.application.service.event.EventService;
import org.ems.application.service.identity.IdentityService;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.Session;
import org.ems.infrastructure.config.AppContext;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SessionPresenterManager - Handles presenter-related operations for sessions
 * Single Responsibility: Manage presenter assignments and data
 *
 * @author EMS Team
 */
public class SessionPresenterManager {

    private final EventService eventService;
    private final IdentityService identityService;
    private final SessionCacheManager cacheManager;

    /**
     * Constructor with dependency injection
     */
    public SessionPresenterManager(EventService eventService,
                                  IdentityService identityService,
                                  SessionCacheManager cacheManager) {
        this.eventService = eventService;
        this.identityService = identityService;
        this.cacheManager = cacheManager;
    }

    /**
     * Convenience constructor using AppContext
     */
    public SessionPresenterManager(SessionCacheManager cacheManager) {
        this(AppContext.get().eventService,
             AppContext.get().identityService,
             cacheManager);
    }

    /**
     * Get all presenters (with caching)
     */
    public List<Presenter> getAllPresenters() {
        long start = System.currentTimeMillis();
        System.out.println("👥 [SessionPresenterManager] Loading all presenters...");

        try {
            List<Presenter> presenters = identityService.getAllPresenters();
            System.out.println("  ✓ Loaded " + presenters.size() + " presenters in " +
                    (System.currentTimeMillis() - start) + " ms");
            return presenters;
        } catch (Exception e) {
            System.err.println("✗ Error loading presenters: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get all presenters as cached map (UUID -> Presenter)
     */
    public Map<UUID, Presenter> getAllPresentersAsMap() {
        long start = System.currentTimeMillis();

        // Check cache first
        if (cacheManager.isPresentersCacheValid()) {
            Map<UUID, Presenter> cached = cacheManager.getPresenters();
            if (cached != null) {
                System.out.println("  ℹ Using cached presenters (" + cached.size() + " presenters)");
                return cached;
            }
        }

        // Load from DB
        System.out.println("👥 [SessionPresenterManager] Loading presenters (cache miss)...");
        List<Presenter> presenters = getAllPresenters();

        Map<UUID, Presenter> presenterMap = presenters.stream()
                .collect(Collectors.toMap(Presenter::getId, p -> p));

        cacheManager.setPresenters(presenterMap);
        System.out.println("  ✓ Presenter cache built (" + presenterMap.size() + " presenters) in " +
                (System.currentTimeMillis() - start) + " ms");

        return presenterMap;
    }

    /**
     * Get single presenter by ID
     */
    public Presenter getPresenterById(UUID presenterId) {
        long start = System.currentTimeMillis();

        // Check cache first
        Map<UUID, Presenter> cached = cacheManager.getPresenters();
        if (cached != null && cached.containsKey(presenterId)) {
            System.out.println("  ℹ Presenter found in cache");
            return cached.get(presenterId);
        }

        // Load from DB
        Presenter presenter = (Presenter) identityService.getUserById(presenterId);
        if (presenter != null) {
            cacheManager.addPresenterToCache(presenter);
            System.out.println("  ✓ Presenter loaded and cached in " +
                    (System.currentTimeMillis() - start) + " ms");
        }
        return presenter;
    }

    /**
     * Get presenter names for a session
     */
    public List<String> getPresentersForSession(Session session) {
        long start = System.currentTimeMillis();
        System.out.println("🎭 [SessionPresenterManager] Getting presenters for session: " + session.getTitle());

        List<UUID> presenterIds = session.getPresenterIds();

        if (presenterIds == null || presenterIds.isEmpty()) {
            System.out.println("  ℹ No presenters assigned to this session");
            return List.of();
        }

        // Load presenter map
        Map<UUID, Presenter> presenterMap = getAllPresentersAsMap();

        List<String> presenterNames = new ArrayList<>();
        for (UUID pid : presenterIds) {
            Presenter p = presenterMap.get(pid);
            presenterNames.add(p != null ? p.getFullName() : "Unknown");
        }

        System.out.println("  ✓ Retrieved " + presenterNames.size() + " presenter names in " +
                (System.currentTimeMillis() - start) + " ms");
        return presenterNames;
    }

    /**
     * Add presenter to session (with conflict checking)
     */
    public boolean addPresenterToSession(UUID presenterId, UUID sessionId) {
        long start = System.currentTimeMillis();
        System.out.println("🎭 [SessionPresenterManager] Adding presenter to session...");

        try {
            boolean success = eventService.addPresenterToSession(presenterId, sessionId);
            System.out.println("  ✓ Operation completed in " + (System.currentTimeMillis() - start) + " ms");
            return success;
        } catch (Exception e) {
            System.err.println("✗ Error adding presenter: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Clear presenter cache
     */
    public void clearCache() {
        System.out.println("🧹 [SessionPresenterManager] Clearing presenter cache...");
        cacheManager.clearPresenters();
    }
}

