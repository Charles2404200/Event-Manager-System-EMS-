package org.ems.application.service.dashboard;

import org.ems.application.dto.dashboard.DashboardPresenterContentDTO;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.Session;
import org.ems.domain.repository.SessionRepository;
import org.ems.application.service.image.ImageService;
import org.ems.application.service.image.ImageServiceImpl;

/**
 * Service for loading presenter-specific dashboard content
 * Implements Single Responsibility Principle - only handles presenter content
 * @author <your group number>
 */
public class DashboardPresenterService {

    private final SessionRepository sessionRepo;

    public DashboardPresenterService(SessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    /**
     * Load presenter dashboard content (assigned sessions count)
     * @param presenter The presenter user
     * @return DTO with presenter content
     * @throws DashboardPresenterException if loading fails
     */
    public DashboardPresenterContentDTO loadPresenterContent(Presenter presenter)
            throws DashboardPresenterException {

        long start = System.currentTimeMillis();
        System.out.println("📋 [DashboardPresenterService] Loading content for " + presenter.getUsername());

        try {
            if (sessionRepo == null) {
                throw new DashboardPresenterException("SessionRepository not available");
            }

            long assignedSessions = sessionRepo.countByPresenter(presenter.getId());

            long elapsed = System.currentTimeMillis() - start;
            System.out.println("  ✓ Presenter content loaded in " + elapsed + "ms: " +
                             assignedSessions + " assigned sessions");

            return new DashboardPresenterContentDTO(assignedSessions);

        } catch (Exception e) {
            String message = "Failed to load presenter content: " + e.getMessage();
            System.err.println("✗ " + message);
            e.printStackTrace();
            throw new DashboardPresenterException(message, e);
        }
    }

    /**
     * Upload session material and update session in DB
     * @param sessionId Session ID
     * @param filePath Path to material file
     * @return Uploaded material path
     * @throws DashboardPresenterException if upload fails
     */
    public String uploadSessionMaterial(String sessionId, String filePath) throws DashboardPresenterException {
        try {
            // Upload material lên R2 Cloudflare
            ImageService imageService = new ImageServiceImpl();
            java.util.UUID sessionUUID = java.util.UUID.fromString(sessionId);
            String materialPath = imageService.uploadSessionMaterial(filePath, sessionUUID);
            if (materialPath == null) {
                throw new DashboardPresenterException("Material upload failed");
            }
            // Update session in DB
            Session session = sessionRepo.findById(sessionUUID);
            if (session == null) {
                throw new DashboardPresenterException("Session not found");
            }
            session.setMaterialPath(materialPath);
            sessionRepo.save(session);
            return materialPath;
        } catch (Exception e) {
            throw new DashboardPresenterException("Failed to upload session material: " + e.getMessage(), e);
        }
    }

    /**
     * Custom exception for presenter dashboard errors
     */
    public static class DashboardPresenterException extends Exception {
        public DashboardPresenterException(String message) {
            super(message);
        }

        public DashboardPresenterException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
