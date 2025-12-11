package org.ems.application.dto;

/**
 * EventFilterCriteriaDTO - Filter and search criteria for event listing
 *
 * @author EMS Team
 */
public class EventFilterCriteriaDTO {
    public String searchTerm;
    public String typeFilter;
    public String statusFilter;
    public int pageNumber;
    public int pageSize;

    public EventFilterCriteriaDTO(String searchTerm, String typeFilter, String statusFilter,
                                   int pageNumber, int pageSize) {
        this.searchTerm = searchTerm != null ? searchTerm.toLowerCase() : "";
        this.typeFilter = typeFilter != null ? typeFilter : "ALL";
        this.statusFilter = statusFilter != null ? statusFilter : "ALL";
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    @Override
    public String toString() {
        return "EventFilterCriteriaDTO{" +
                "searchTerm='" + searchTerm + '\'' +
                ", typeFilter='" + typeFilter + '\'' +
                ", statusFilter='" + statusFilter + '\'' +
                ", pageNumber=" + pageNumber +
                ", pageSize=" + pageSize +
                '}';
    }
}

