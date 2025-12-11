package org.ems.application.service;

import org.ems.domain.dto.TemplateKey;
import org.ems.domain.model.Ticket;
import org.ems.domain.repository.TicketRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * TicketStatisticsService - Calculates ticket statistics
 * Single Responsibility: Compute revenue, counts, and status aggregations
 *
 * @author EMS Team
 */
public class TicketStatisticsService {

    private final TicketRepository ticketRepo;
    private final TicketCacheManager cacheManager;

    public TicketStatisticsService(TicketRepository ticketRepo, TicketCacheManager cacheManager) {
        this.ticketRepo = ticketRepo;
        this.cacheManager = cacheManager;
    }

    /**
     * Get assigned count for a template (with caching)
     */
    public long getTemplateAssignedCount(TemplateKey templateKey) {
        long start = System.currentTimeMillis();

        // Check cache first
        Map<TemplateKey, Long> cache = cacheManager.getTemplateAssignedCountCache();
        if (cache != null && cache.containsKey(templateKey)) {
            System.out.println("  ℹ Template count found in cache");
            return cache.get(templateKey);
        }

        // Load from DB
        System.out.println("📊 [TicketStatisticsService] Loading template assigned count...");
        try {
            // Use findAssignedStatsForTemplates() to get aggregate data
            List<TicketRepository.TemplateAssignmentStats> stats = ticketRepo.findAssignedStatsForTemplates();

            long count = 0;
            for (TicketRepository.TemplateAssignmentStats stat : stats) {
                if (stat.getEventId().equals(templateKey.getEventId()) &&
                    stat.getSessionId().equals(templateKey.getSessionId()) &&
                    stat.getType().equals(templateKey.getType()) &&
                    stat.getPrice().equals(templateKey.getPrice())) {
                    count = stat.getAssignedCount();
                    break;
                }
            }

            System.out.println("  ✓ Count loaded in " + (System.currentTimeMillis() - start) + " ms: " + count);
            return count;
        } catch (Exception e) {
            System.err.println("✗ Error loading template count: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get total assigned count
     */
    public long getTotalAssignedCount() {
        long start = System.currentTimeMillis();
        System.out.println("📊 [TicketStatisticsService] Loading total assigned count...");

        try {
            long count = ticketRepo.countAssigned();
            System.out.println("  ✓ Total assigned count loaded in " + (System.currentTimeMillis() - start) + " ms: " + count);
            return count;
        } catch (Exception e) {
            System.err.println("✗ Error loading total assigned count: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get total revenue from assigned tickets
     */
    public BigDecimal getTotalRevenue() {
        long start = System.currentTimeMillis();
        System.out.println("💰 [TicketStatisticsService] Loading total revenue...");

        try {
            List<Ticket> assignedTickets = ticketRepo.findAssigned();
            BigDecimal totalRevenue = BigDecimal.ZERO;

            for (Ticket ticket : assignedTickets) {
                if (ticket.getPrice() != null) {
                    totalRevenue = totalRevenue.add(ticket.getPrice());
                }
            }

            System.out.println("  ✓ Revenue loaded in " + (System.currentTimeMillis() - start) + " ms: " + totalRevenue);
            return totalRevenue;
        } catch (Exception e) {
            System.err.println("✗ Error loading revenue: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Invalidate statistics cache
     */
    public void invalidateCache() {
        System.out.println("🧹 [TicketStatisticsService] Invalidating statistics cache");
        cacheManager.clearStatistics();
    }
}

