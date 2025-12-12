package org.ems.application.service.activity;

import org.ems.application.dto.activity.ActivityLogDisplayDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ActivityLogFilterService
 * Handles filtering activity logs based on various criteria
 * @author <your group number>
 */
public class ActivityLogFilterServiceImpl implements ActivityLogFilterService {

    /**
     * Filter logs by search term (searches all fields)
     * @param logs List of logs to filter
     * @param searchTerm Search keyword
     * @return Filtered list
     */
    @Override
    public List<ActivityLogDisplayDTO> filterBySearchTerm(List<ActivityLogDisplayDTO> logs, String searchTerm) {
        long start = System.currentTimeMillis();
        System.out.println("🔎 [ActivityLogFilterServiceImpl] Filtering by search term: '" + searchTerm + "'");

        if (logs == null || logs.isEmpty()) {
            return new ArrayList<>();
        }

        List<ActivityLogDisplayDTO> filtered = new ArrayList<>();
        String lowerSearch = searchTerm != null ? searchTerm.toLowerCase() : "";

        if (lowerSearch.isEmpty()) {
            return new ArrayList<>(logs);
        }

        for (ActivityLogDisplayDTO log : logs) {
            if (log.getUserId().toLowerCase().contains(lowerSearch) ||
                log.getAction().toLowerCase().contains(lowerSearch) ||
                log.getResource().toLowerCase().contains(lowerSearch) ||
                log.getDescription().toLowerCase().contains(lowerSearch)) {
                filtered.add(log);
            }
        }

        System.out.println("  ✓ Filtered to " + filtered.size() + "/" + logs.size() +
                         " logs in " + (System.currentTimeMillis() - start) + "ms");
        return filtered;
    }

    /**
     * Filter logs by action type
     * @param logs List of logs to filter
     * @param action Action type to filter ("All Actions" means no filter)
     * @return Filtered list
     */
    @Override
    public List<ActivityLogDisplayDTO> filterByAction(List<ActivityLogDisplayDTO> logs, String action) {
        long start = System.currentTimeMillis();
        System.out.println("🔎 [ActivityLogFilterServiceImpl] Filtering by action: " + action);

        if (logs == null || logs.isEmpty() || "All Actions".equals(action)) {
            return new ArrayList<>(logs);
        }

        List<ActivityLogDisplayDTO> filtered = new ArrayList<>();
        for (ActivityLogDisplayDTO log : logs) {
            if (log.getAction().equals(action)) {
                filtered.add(log);
            }
        }

        System.out.println("  ✓ Filtered to " + filtered.size() + " logs in " + (System.currentTimeMillis() - start) + "ms");
        return filtered;
    }
}

