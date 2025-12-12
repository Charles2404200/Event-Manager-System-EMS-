package org.ems.application.service.activity;

import org.ems.application.dto.activity.ActivityLogDisplayDTO;

import java.util.List;

/**
 * Service for paginating activity logs
 * Implements Single Responsibility Principle - only handles pagination logic
 * @author <your group number>
 */
public interface ActivityLogPaginationService {

    /**
     * Get paginated slice of logs
     * @param logs Full list of logs
     * @param pageNumber Current page number (0-based)
     * @return Logs for this page
     */
    List<ActivityLogDisplayDTO> getPage(List<ActivityLogDisplayDTO> logs, int pageNumber);

    /**
     * Get total number of pages
     * @param totalItems Total number of items
     * @return Total pages
     */
    int getTotalPages(int totalItems);

    /**
     * Get page label for display
     * @param currentPage Current page number (0-based)
     * @param totalPages Total pages
     * @return Label like "Page 1 / 5"
     */
    String getPageLabel(int currentPage, int totalPages);

    /**
     * Check if can go to next page
     * @param currentPage Current page number (0-based)
     * @param totalPages Total pages
     * @return True if next page exists
     */
    boolean canGoNextPage(int currentPage, int totalPages);

    /**
     * Check if can go to previous page
     * @param currentPage Current page number (0-based)
     * @return True if previous page exists
     */
    boolean canGoPreviousPage(int currentPage);
}

