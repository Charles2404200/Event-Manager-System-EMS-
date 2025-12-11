package org.ems.application.service;

import org.ems.application.dto.ActivityLogDisplayDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for paginating activity logs
 * Implements Single Responsibility Principle - only handles pagination logic
 * @author <your group number>
 */
public class ActivityLogPaginationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private final int pageSize;

    public ActivityLogPaginationService() {
        this.pageSize = DEFAULT_PAGE_SIZE;
    }

    public ActivityLogPaginationService(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Get paginated slice of logs
     * @param logs Full list of logs
     * @param pageNumber Current page number (0-based)
     * @return Logs for this page
     */
    public List<ActivityLogDisplayDTO> getPage(List<ActivityLogDisplayDTO> logs, int pageNumber) {
        if (logs == null || logs.isEmpty()) {
            return new ArrayList<>();
        }

        int start = pageNumber * pageSize;
        int end = Math.min(start + pageSize, logs.size());

        if (start >= logs.size()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(logs.subList(start, end));
    }

    /**
     * Get total number of pages
     * @param totalItems Total number of items
     * @return Total pages
     */
    public int getTotalPages(int totalItems) {
        return Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
    }

    /**
     * Get page label for display
     * @param currentPage Current page number (0-based)
     * @param totalPages Total pages
     * @return Label like "Page 1 / 5"
     */
    public String getPageLabel(int currentPage, int totalPages) {
        return "Page " + (currentPage + 1) + " / " + totalPages;
    }

    /**
     * Check if can go to next page
     * @param currentPage Current page number (0-based)
     * @param totalPages Total pages
     * @return True if next page exists
     */
    public boolean canGoNextPage(int currentPage, int totalPages) {
        return (currentPage + 1) < totalPages;
    }

    /**
     * Check if can go to previous page
     * @param currentPage Current page number (0-based)
     * @return True if previous page exists
     */
    public boolean canGoPreviousPage(int currentPage) {
        return currentPage > 0;
    }
}

