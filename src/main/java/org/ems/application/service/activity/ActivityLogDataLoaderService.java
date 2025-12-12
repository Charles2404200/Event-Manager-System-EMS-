package org.ems.application.service.activity;

import org.ems.application.dto.activity.ActivityLogDisplayDTO;

import java.util.List;

/**
 * Service for loading activity logs
 * Implements Single Responsibility Principle - only handles data loading
 * @author <your group number>
 */
public interface ActivityLogDataLoaderService {

    /**
     * Load all activity logs and convert to display DTOs
     * @return List of activity log display DTOs
     * @throws ActivityLogException if loading fails
     */
    List<ActivityLogDisplayDTO> loadAllLogs() throws ActivityLogException;

    /**
     * Custom exception for activity log loading
     */
    class ActivityLogException extends Exception {
        public ActivityLogException(String message) {
            super(message);
        }

        public ActivityLogException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

