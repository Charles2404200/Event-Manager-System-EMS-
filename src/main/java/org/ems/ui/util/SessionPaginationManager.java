package org.ems.ui.util;

import javafx.scene.control.Label;
import org.ems.domain.model.Session;

import java.util.List;

/**
 * SessionPaginationManager - Manages pagination logic for session display
 * Single Responsibility: Handle pagination calculations and state
 *
 * @author EMS Team
 */
public class SessionPaginationManager {

    private static final int PAGE_SIZE = 20;

    private int currentPage = 0;
    private int totalPages = 1;
    private List<Session> allSessionsCache;
    private Label pageInfoLabel;

    /**
     * Constructor
     */
    public SessionPaginationManager() {
        this(null);
    }

    /**
     * Constructor with page info label
     */
    public SessionPaginationManager(Label pageInfoLabel) {
        this.pageInfoLabel = pageInfoLabel;
        updatePageInfo();
    }

    /**
     * Set page info label for updates
     */
    public void setPageInfoLabel(Label pageInfoLabel) {
        this.pageInfoLabel = pageInfoLabel;
        updatePageInfo();
    }

    /**
     * Initialize pagination with cached sessions
     */
    public void initializePagination(List<Session> sessions) {
        long start = System.currentTimeMillis();
        System.out.println("📄 [SessionPaginationManager] Initializing pagination...");

        this.allSessionsCache = sessions;
        this.currentPage = 0;
        this.totalPages = Math.max(1, (int) Math.ceil(sessions.size() / (double) PAGE_SIZE));

        updatePageInfo();
        System.out.println("  ✓ Pagination initialized in " + (System.currentTimeMillis() - start) + " ms");
        System.out.println("  ℹ Total pages: " + totalPages + ", Total sessions: " + sessions.size());
    }

    /**
     * Get current page data
     */
    public List<Session> getCurrentPageData() {
        if (allSessionsCache == null || allSessionsCache.isEmpty()) {
            System.out.println("⚠️ No cached sessions available");
            return List.of();
        }

        int offset = currentPage * PAGE_SIZE;
        int endIndex = Math.min(offset + PAGE_SIZE, allSessionsCache.size());

        return allSessionsCache.subList(offset, endIndex);
    }

    /**
     * Go to next page
     */
    public boolean nextPage() {
        if (currentPage + 1 < totalPages) {
            currentPage++;
            updatePageInfo();
            System.out.println("📄 [SessionPaginationManager] Moving to page " + (currentPage + 1));
            return true;
        }
        return false;
    }

    /**
     * Go to previous page
     */
    public boolean previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updatePageInfo();
            System.out.println("📄 [SessionPaginationManager] Moving to page " + (currentPage + 1));
            return true;
        }
        return false;
    }

    /**
     * Reset pagination to first page
     */
    public void reset() {
        this.currentPage = 0;
        this.totalPages = 1;
        this.allSessionsCache = null;
        updatePageInfo();
        System.out.println("🔄 [SessionPaginationManager] Pagination reset");
    }

    /**
     * Update page info label
     */
    private void updatePageInfo() {
        if (pageInfoLabel != null) {
            pageInfoLabel.setText("Page " + (currentPage + 1) + " / " + totalPages);
        }
    }

    // Getters
    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    public boolean canGoNext() {
        return currentPage + 1 < totalPages;
    }

    public boolean canGoPrevious() {
        return currentPage > 0;
    }
}

