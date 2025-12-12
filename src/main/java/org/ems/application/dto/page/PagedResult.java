package org.ems.application.dto.page;

import java.util.List;

/**
 * Generic paged result wrapper for application layer.
 * Used to return paginated data from services to controllers.
 */
public class PagedResult<T> {
    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long totalItems;
    private final int totalPages;

    public PagedResult(List<T> items, int page, int pageSize, long totalItems) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean hasNext() {
        return page + 1 < totalPages;
    }

    public boolean hasPrevious() {
        return page > 0;
    }
}

