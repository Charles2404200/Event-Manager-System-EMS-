package org.ems.application.dto.ticket;

import java.util.List;

/**
 * DTO for ticket filtering results
 * @author <your group number>
 */
public class TicketFilterResultDTO {
    private final List<TicketDisplayDTO> filteredTickets;
    private final int totalCount;
    private final double totalValue;

    public TicketFilterResultDTO(List<TicketDisplayDTO> filteredTickets, int totalCount, double totalValue) {
        this.filteredTickets = filteredTickets;
        this.totalCount = totalCount;
        this.totalValue = totalValue;
    }

    public List<TicketDisplayDTO> getFilteredTickets() { return filteredTickets; }
    public int getTotalCount() { return totalCount; }
    public double getTotalValue() { return totalValue; }

    @Override
    public String toString() {
        return "TicketFilterResultDTO{" +
                "totalCount=" + totalCount +
                ", totalValue=" + totalValue +
                '}';
    }
}

