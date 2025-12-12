package org.ems.application.service.activity;

import org.ems.application.dto.activity.ActivityLogDisplayDTO;

import java.util.List;

/**
 * Implementation of ActivityLogStatisticsService
 * Handles statistical calculations for activity logs
 * @author <your group number>
 */
public class ActivityLogStatisticsServiceImpl implements ActivityLogStatisticsService {

    /**
     * Get total number of logs
     * @param logs List of logs
     * @return Total count
     */
    @Override
    public int getTotalCount(List<ActivityLogDisplayDTO> logs) {
        return logs != null ? logs.size() : 0;
    }

    /**
     * Get timestamp of last activity (most recent)
     * @param logs List of logs (should be sorted by timestamp descending)
     * @return Last activity timestamp, or "No activity" if empty
     */
    @Override
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
    @Override
    public int getCountByAction(List<ActivityLogDisplayDTO> logs, String action) {
        if (logs == null || action == null) {
            return 0;
        }

        return (int) logs.stream()
                .filter(log -> log.getAction().equals(action))
                .count();
    }
}

