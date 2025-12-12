package org.ems.application.service.activity;

import org.ems.application.dto.activity.ActivityLogDisplayDTO;

import java.util.List;

/**
 * Service for filtering activity logs
 * Implements Single Responsibility Principle - only handles filtering logic
 * @author <your group number>
 */
public interface ActivityLogFilterService {

    /**
     * Filter logs by search term (searches all fields)
     * @param logs List of logs to filter
     * @param searchTerm Search keyword
     * @return Filtered list
     */
    List<ActivityLogDisplayDTO> filterBySearchTerm(List<ActivityLogDisplayDTO> logs, String searchTerm);

    /**
     * Filter logs by action type
     * @param logs List of logs to filter
     * @param action Action type to filter ("All Actions" means no filter)
     * @return Filtered list
     */
    List<ActivityLogDisplayDTO> filterByAction(List<ActivityLogDisplayDTO> logs, String action);
}

