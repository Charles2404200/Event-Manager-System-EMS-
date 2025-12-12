package org.ems.application.service;

import java.util.UUID;

/**
 * @author <your group number>
 *
 * Service for generating presenter statistics
 */
public interface PresenterStatisticsService {

    /**
     * Generate comprehensive statistics for a presenter
     * Uses optimized queries to avoid N+1 problem
     *
     * @param presenterId UUID of presenter
     * @return PresenterStatistics object with all metrics
     */
    PresenterStatistics generateStatistics(UUID presenterId);
}