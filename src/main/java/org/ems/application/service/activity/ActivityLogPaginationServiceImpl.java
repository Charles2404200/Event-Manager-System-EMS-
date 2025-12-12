package org.ems.application.service.activity;

import org.ems.application.dto.activity.ActivityLogDisplayDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ActivityLogPaginationService
 * Handles pagination logic for activity logs
 * @author <your group number>
 */
public class ActivityLogPaginationServiceImpl implements ActivityLogPaginationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private final int pageSize;

    public ActivityLogPaginationServiceImpl() {
        this.pageSize = DEFAULT_PAGE_SIZE;
    }

    public ActivityLogPaginationServiceImpl(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Get paginated slice of logs
     * @param logs Full list of logs
     * @param pageNumber Current page number (0-based)
     * @return Logs for this page
     */
    @Override
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
    @Override
    public int getTotalPages(int totalItems) {
        return Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
    }

    /**
     * Get page label for display
     * @param currentPage Current page number (0-based)
     * @param totalPages Total pages
     * @return Label like "Page 1 / 5"
     */
    @Override
    public String getPageLabel(int currentPage, int totalPages) {
        return "Page " + (currentPage + 1) + " / " + totalPages;
    }

    /**
     * Check if can go to next page
     * @param currentPage Current page number (0-based)
     * @param totalPages Total pages
     * @return True if next page exists
     */
    @Override
    public boolean canGoNextPage(int currentPage, int totalPages) {
        return (currentPage + 1) < totalPages;
    }

    /**
     * Check if can go to previous page
     * @param currentPage Current page number (0-based)
     * @return True if previous page exists
     */
    @Override
    public boolean canGoPreviousPage(int currentPage) {
        return currentPage > 0;
    }
}

