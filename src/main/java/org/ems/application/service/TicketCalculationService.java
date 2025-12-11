package org.ems.application.service;

import org.ems.application.dto.TicketDisplayDTO;
import java.util.List;

/**
 * Service for calculating ticket-related statistics
 * Implements Single Responsibility Principle - only handles calculations
 * @author <your group number>
 */
public class TicketCalculationService {

    /**
     * Calculate total value of tickets
     * @param tickets List of tickets to calculate
     * @return Total value
     */
    public double calculateTotalValue(List<TicketDisplayDTO> tickets) {
        long start = System.currentTimeMillis();
        System.out.println("💰 [TicketCalculationService] Calculating total value...");

        double total = 0.0;

        if (tickets == null || tickets.isEmpty()) {
            System.out.println("  ℹ No tickets to calculate");
            return 0.0;
        }

        try {
            for (TicketDisplayDTO ticket : tickets) {
                try {
                    String priceStr = ticket.getPrice().replace("$", "").trim();
                    if (!priceStr.isEmpty()) {
                        total += Double.parseDouble(priceStr);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("  ⚠️ Invalid price format for ticket " + ticket.getTicketId() + ": " + ticket.getPrice());
                }
            }

            System.out.println("  ✓ Total value calculated: $" + String.format("%.2f", total) +
                             " in " + (System.currentTimeMillis() - start) + "ms");
            return total;

        } catch (Exception e) {
            System.err.println("  ✗ Error calculating total: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Format price for display
     * @param price Raw price value
     * @return Formatted price string
     */
    public String formatPrice(Double price) {
        if (price == null) {
            return "$0.00";
        }
        try {
            return "$" + String.format("%.2f", price);
        } catch (Exception e) {
            return "$0.00";
        }
    }
}

