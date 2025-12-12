package org.ems.infrastructure.util;

import org.ems.infrastructure.config.AppContext;
import org.ems.domain.model.ActivityLog;
import org.ems.domain.repository.ActivityLogRepository;

public class ActivityLogger {

    private static ActivityLogger instance;
    private ActivityLogRepository activityLogRepo;
    private String currentUserId = "SYSTEM";

    private ActivityLogger() {
        try {
            AppContext context = AppContext.get();
            this.activityLogRepo = context.activityLogRepo;
        } catch (Exception e) {
            System.err.println("Error initializing ActivityLogger: " + e.getMessage());
        }
    }

    public static ActivityLogger getInstance() {
        if (instance == null) {
            instance = new ActivityLogger();
        }
        return instance;
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId != null ? userId : "SYSTEM";
    }

    public void log(String action, String resource, String description) {
        log(action, resource, description, null, null);
    }

    public void log(String action, String resource, String description, String ipAddress, String userAgent) {
        if (activityLogRepo == null) {
            System.err.println("[ActivityLogger] ActivityLogRepository is NULL, cannot log");
            return;
        }

        try {
            ActivityLog log = new ActivityLog(currentUserId, action, resource, description);
            log.setIpAddress(ipAddress != null ? ipAddress : "127.0.0.1");
            log.setUserAgent(userAgent != null ? userAgent : "JavaFX");

            activityLogRepo.save(log);
            System.out.println("[ActivityLogger] Logged: " + action + " - " + resource + " - " + description);
        } catch (Exception e) {
            System.err.println("[ActivityLogger] Error logging activity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void logCreate(String resource, String description) {
        log("CREATE", resource, description);
    }

    public void logUpdate(String resource, String description) {
        log("UPDATE", resource, description);
    }

    public void logDelete(String resource, String description) {
        log("DELETE", resource, description);
    }

    public void logView(String resource, String description) {
        log("VIEW", resource, description);
    }

    public void logExport(String resource, String description) {
        log("EXPORT", resource, description);
    }

    public void logLogin(String userId, String description) {
        log("LOGIN", "Authentication", userId + ": " + description);
    }

    public void logLogout(String userId, String description) {
        log("LOGOUT", "Authentication", userId + ": " + description);
    }
}

