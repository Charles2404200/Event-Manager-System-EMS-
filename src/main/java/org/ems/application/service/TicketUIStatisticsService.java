package org.ems.application.service;

import org.ems.application.dto.TicketRow;

import java.math.BigDecimal;
import java.util.List;

/**
 * TicketUIStatisticsService - Calculates statistics for assigned tickets
 * Single Responsibility: Statistics calculation from ticket data
 *
 * @author EMS Team
 */
public class TicketUIStatisticsService {

    /**
     * Calculate statistics from a list of ticket rows
     */
    public Statistics calculateStatistics(List<TicketRow> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return new Statistics(0, 0, BigDecimal.ZERO);
        }

        long startTime = System.currentTimeMillis();

        int total = tickets.size();
        int active = 0;
        BigDecimal revenue = BigDecimal.ZERO;

        // Single pass through the list for all calculations
        for (TicketRow t : tickets) {
            if (t.getStatus() != null && t.getStatus().equals("ACTIVE")) {
                active++;
            }

            // Parse price and add to revenue
            try {
                String priceStr = t.getPrice().replace("$", "").trim();
                if (!priceStr.isEmpty()) {
                    revenue = revenue.add(new BigDecimal(priceStr));
                }
            } catch (Exception e) {
                // Skip invalid prices
            }
        }

        long elapsedMs = System.currentTimeMillis() - startTime;
        System.out.println("[TicketUIStatisticsService] Calculated stats for " + total +
                " tickets in " + elapsedMs + " ms");

        return new Statistics(total, active, revenue);
    }

    /**
     * Statistics result class
     */
    public static class Statistics {
        public final int total;
        public final int active;
        public final BigDecimal revenue;

        public Statistics(int total, int active, BigDecimal revenue) {
            this.total = total;
            this.active = active;
            this.revenue = revenue;
        }

        @Override
        public String toString() {
            return "Statistics{" +
                    "total=" + total +
                    ", active=" + active +
                    ", revenue=$" + revenue +
                    '}';
        }
    }
}

