package org.ems.application.service.activity;

import org.ems.application.dto.activity.ActivityLogDisplayDTO;

import java.util.List;

/**
 * Service for calculating activity log statistics
 * Implements Single Responsibility Principle - only handles calculations
 * @author <your group number>
 */
public interface ActivityLogStatisticsService {

    /**
     * Get total number of logs
     * @param logs List of logs
     * @return Total count
     */
    int getTotalCount(List<ActivityLogDisplayDTO> logs);

    /**
     * Get timestamp of last activity (most recent)
     * @param logs List of logs (should be sorted by timestamp descending)
     * @return Last activity timestamp, or "No activity" if empty
     */
    String getLastActivityTimestamp(List<ActivityLogDisplayDTO> logs);

    /**
     * Get count of logs by action type
     * @param logs List of logs
     * @param action Action type
     * @return Count of logs with this action
     */
    int getCountByAction(List<ActivityLogDisplayDTO> logs, String action);
}

