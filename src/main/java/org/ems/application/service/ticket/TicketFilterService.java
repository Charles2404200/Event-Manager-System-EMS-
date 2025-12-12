package org.ems.application.service.ticket;

import org.ems.application.dto.ticket.TicketDisplayDTO;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for filtering tickets
 * Implements Single Responsibility Principle - only handles filtering logic
 * @author <your group number>
 */
public class TicketFilterService {

    /**
     * Filter tickets based on search term, status, and type
     * @param tickets List of all tickets
     * @param searchTerm Search keyword
     * @param statusFilter Status filter ("ALL" means no filter)
     * @param typeFilter Type filter ("ALL" means no filter)
     * @return Filtered list of tickets
     */
    public List<TicketDisplayDTO> filterTickets(List<TicketDisplayDTO> tickets, String searchTerm,
                                                String statusFilter, String typeFilter) {
        long start = System.currentTimeMillis();
        System.out.println("🔎 [TicketFilterService] Filtering tickets - search: '" + searchTerm +
                         "', status: " + statusFilter + ", type: " + typeFilter);

        if (tickets == null || tickets.isEmpty()) {
            System.out.println("  ℹ No tickets to filter");
            return new ArrayList<>();
        }

        List<TicketDisplayDTO> filtered = new ArrayList<>();
        String lowerSearchTerm = searchTerm != null ? searchTerm.toLowerCase() : "";

        for (TicketDisplayDTO ticket : tickets) {
            // Apply status filter
            if (!statusFilter.equals("ALL") && !ticket.getStatus().equals(statusFilter)) {
                continue;
            }

            // Apply type filter
            if (!typeFilter.equals("ALL") && !ticket.getType().equals(typeFilter)) {
                continue;
            }

            // Apply search filter
            if (lowerSearchTerm.isEmpty() ||
                ticket.getTicketId().toLowerCase().contains(lowerSearchTerm) ||
                ticket.getEventName().toLowerCase().contains(lowerSearchTerm) ||
                ticket.getSessionName().toLowerCase().contains(lowerSearchTerm)) {
                filtered.add(ticket);
            }
        }

        System.out.println("  ✓ Filtered " + filtered.size() + "/" + tickets.size() +
                         " tickets in " + (System.currentTimeMillis() - start) + "ms");
        return filtered;
    }
}

