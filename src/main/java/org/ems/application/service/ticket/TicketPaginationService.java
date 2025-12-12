package org.ems.application.service.ticket;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * TicketPaginationService - Manages pagination state for ticket lists
 * Handles keyset pagination cursors and page tracking
 *
 * @author EMS Team
 */
public class TicketPaginationService {

    private static final int PAGE_SIZE = TicketManagerConfig.PAGE_SIZE;

    // Template pagination state
    private Timestamp lastTemplateCreatedAt = null;
    private UUID lastTemplateId = null;

    // Assigned tickets pagination state
    private Timestamp lastAssignedCreatedAt = null;
    private UUID lastAssignedId = null;
    private int currentAssignedPage = 1;
    private long totalAssignedCount = 0;
    private int totalAssignedPages = 1;

    // ====== Template Pagination ======

    public void resetTemplatePagination() {
        this.lastTemplateCreatedAt = null;
        this.lastTemplateId = null;
        System.out.println("📄 [TicketPaginationService] Template pagination reset");
    }

    public void setTemplateKeyset(Timestamp createdAt, UUID id) {
        this.lastTemplateCreatedAt = createdAt;
        this.lastTemplateId = id;
    }

    public Timestamp getLastTemplateCreatedAt() { return lastTemplateCreatedAt; }
    public UUID getLastTemplateId() { return lastTemplateId; }
    public int getPageSize() { return PAGE_SIZE; }

    // ====== Assigned Tickets Pagination ======

    public void resetAssignedPagination() {
        this.lastAssignedCreatedAt = null;
        this.lastAssignedId = null;
        this.currentAssignedPage = 1;
        this.totalAssignedCount = 0;
        this.totalAssignedPages = 1;
        System.out.println("📄 [TicketPaginationService] Assigned pagination reset");
    }

    public void setAssignedKeyset(Timestamp createdAt, UUID id) {
        this.lastAssignedCreatedAt = createdAt;
        this.lastAssignedId = id;
    }

    public Timestamp getLastAssignedCreatedAt() { return lastAssignedCreatedAt; }
    public UUID getLastAssignedId() { return lastAssignedId; }

    public int getCurrentAssignedPage() { return currentAssignedPage; }
    public void setCurrentAssignedPage(int page) { this.currentAssignedPage = page; }

    public long getTotalAssignedCount() { return totalAssignedCount; }
    public void setTotalAssignedCount(long count) { this.totalAssignedCount = count; }

    public int getTotalAssignedPages() { return totalAssignedPages; }
    public void setTotalAssignedPages(int pages) { this.totalAssignedPages = pages; }

    public boolean canGoNextAssigned() {
        return currentAssignedPage < totalAssignedPages;
    }

    public boolean canGoPreviousAssigned() {
        return currentAssignedPage > 1;
    }

    public void nextAssignedPage() {
        if (canGoNextAssigned()) {
            currentAssignedPage++;
            System.out.println("📄 [TicketPaginationService] Next assigned page: " + currentAssignedPage);
        }
    }

    public void previousAssignedPage() {
        if (canGoPreviousAssigned()) {
            currentAssignedPage--;
            // Reset cursor if going back to first page
            if (currentAssignedPage == 1) {
                resetAssignedPagination();
                currentAssignedPage = 1;
            }
            System.out.println("📄 [TicketPaginationService] Prev assigned page: " + currentAssignedPage);
        }
    }

    public void goToAssignedPage(int pageNumber) {
        if (pageNumber >= 1 && pageNumber <= totalAssignedPages) {
            currentAssignedPage = pageNumber;
            // Reset cursor if going to first page
            if (pageNumber == 1) {
                lastAssignedCreatedAt = null;
                lastAssignedId = null;
            }
            System.out.println("📄 [TicketPaginationService] Go to assigned page: " + pageNumber);
        }
    }

    /**
     * Calculate total pages from count
     */
    public int calculatePages(long totalCount) {
        return Math.max(1, (int) Math.ceil(totalCount / (double) PAGE_SIZE));
    }
}

