package org.ems.application.service;

import org.ems.application.dto.ActivityLogDisplayDTO;
import org.ems.domain.model.ActivityLog;
import org.ems.domain.repository.ActivityLogRepository;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for loading activity logs
 * Implements Single Responsibility Principle - only handles data loading
 * @author <your group number>
 */
public class ActivityLogDataLoaderService {

    private final ActivityLogRepository activityLogRepo;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ActivityLogDataLoaderService(ActivityLogRepository activityLogRepo) {
        this.activityLogRepo = activityLogRepo;
    }

    /**
     * Load all activity logs and convert to display DTOs
     * @return List of activity log display DTOs
     * @throws ActivityLogException if loading fails
     */
    public List<ActivityLogDisplayDTO> loadAllLogs() throws ActivityLogException {
        long start = System.currentTimeMillis();
        System.out.println("📋 [ActivityLogDataLoaderService] Loading all activity logs...");

        try {
            if (activityLogRepo == null) {
                throw new ActivityLogException("ActivityLogRepository not available");
            }

            // Load all logs from repository
            List<ActivityLog> logs = activityLogRepo.findAll();
            System.out.println("  ✓ Fetched " + logs.size() + " logs from DB");

            // Convert to DTOs
            List<ActivityLogDisplayDTO> displayLogs = new ArrayList<>();
            for (ActivityLog log : logs) {
                displayLogs.add(convertToDTO(log));
            }

            System.out.println("  ✓ Converted to DTOs in " + (System.currentTimeMillis() - start) + "ms");
            return displayLogs;

        } catch (Exception e) {
            String message = "Failed to load activity logs: " + e.getMessage();
            System.err.println("✗ " + message);
            e.printStackTrace();
            throw new ActivityLogException(message, e);
        }
    }

    /**
     * Convert ActivityLog to ActivityLogDisplayDTO
     */
    private ActivityLogDisplayDTO convertToDTO(ActivityLog log) {
        return new ActivityLogDisplayDTO(
                log.getTimestamp().format(DATE_FORMATTER),
                log.getUserId(),
                log.getAction(),
                log.getResource(),
                log.getDescription()
        );
    }

    /**
     * Custom exception for activity log loading
     */
    public static class ActivityLogException extends Exception {
        public ActivityLogException(String message) {
            super(message);
        }

        public ActivityLogException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

