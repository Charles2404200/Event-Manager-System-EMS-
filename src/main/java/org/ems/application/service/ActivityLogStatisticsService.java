package org.ems.application.service;

import org.ems.application.dto.ActivityLogDisplayDTO;

import java.util.List;

/**
 * Service for calculating activity log statistics
 * Implements Single Responsibility Principle - only handles calculations
 * @author <your group number>
 */
public class ActivityLogStatisticsService {

    /**
     * Get total number of logs
     * @param logs List of logs
     * @return Total count
     */
    public int getTotalCount(List<ActivityLogDisplayDTO> logs) {
        return logs != null ? logs.size() : 0;
    }

    /**
     * Get timestamp of last activity (most recent)
     * @param logs List of logs (should be sorted by timestamp descending)
     * @return Last activity timestamp, or "No activity" if empty
     */
    public String getLastActivityTimestamp(List<ActivityLogDisplayDTO> logs) {
        if (logs == null || logs.isEmpty()) {
            return "No activity";
        }

        // Assuming logs are sorted with most recent first
        return logs.get(0).getTimestamp();
    }

    /**
     * Get count of logs by action type
     * @param logs List of logs
     * @param action Action type
     * @return Count of logs with this action
     */
    public int getCountByAction(List<ActivityLogDisplayDTO> logs, String action) {
        if (logs == null || action == null) {
            return 0;
        }

        return (int) logs.stream()
                .filter(log -> log.getAction().equals(action))
                .count();
    }
}

