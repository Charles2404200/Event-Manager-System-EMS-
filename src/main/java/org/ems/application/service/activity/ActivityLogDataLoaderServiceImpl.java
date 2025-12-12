package org.ems.application.service.activity;

import org.ems.application.dto.activity.ActivityLogDisplayDTO;
import org.ems.domain.model.ActivityLog;
import org.ems.domain.repository.ActivityLogRepository;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ActivityLogDataLoaderService
 * Handles loading and converting activity logs from repository
 * @author <your group number>
 */
public class ActivityLogDataLoaderServiceImpl implements ActivityLogDataLoaderService {

    private final ActivityLogRepository activityLogRepo;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ActivityLogDataLoaderServiceImpl(ActivityLogRepository activityLogRepo) {
        this.activityLogRepo = activityLogRepo;
    }

    /**
     * Load all activity logs and convert to display DTOs
     * @return List of activity log display DTOs
     * @throws ActivityLogDataLoaderService.ActivityLogException if loading fails
     */
    @Override
    public List<ActivityLogDisplayDTO> loadAllLogs() throws ActivityLogDataLoaderService.ActivityLogException {
        long start = System.currentTimeMillis();
        System.out.println("📋 [ActivityLogDataLoaderServiceImpl] Loading all activity logs...");

        try {
            if (this.activityLogRepo == null) {
                throw new ActivityLogDataLoaderService.ActivityLogException("ActivityLogRepository not available");
            }

            // Load all logs from repository
            List<ActivityLog> logs = this.activityLogRepo.findAll();
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
            throw new ActivityLogDataLoaderService.ActivityLogException(message, e);
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
}

