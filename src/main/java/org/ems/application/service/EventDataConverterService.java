package org.ems.application.service;

import org.ems.application.dto.EventRowDTO;
import org.ems.domain.model.Event;
import org.ems.domain.repository.SessionRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * EventDataConverterService - Converts Event domain models to UI DTOs
 * Single Responsibility: Event → EventRowDTO conversion with session counting
 *
 * @author EMS Team
 */
public class EventDataConverterService {

    private final SessionRepository sessionRepo;
    private final Connection dbConnection;

    public EventDataConverterService(SessionRepository sessionRepo, Connection dbConnection) {
        this.sessionRepo = sessionRepo;
        this.dbConnection = dbConnection;
    }

    /**
     * Convert list of events to EventRowDTO with optimized session counting
     */
    public List<EventRowDTO> convertEventsToRows(List<Event> events) {
        long start = System.currentTimeMillis();
        System.out.println("[EventDataConverterService] convertEventsToRows(" + events.size() + " events) starting...");

        try {
            // Load session counts in one query
            Map<UUID, Integer> sessionCounts = loadSessionCounts(events);

            List<EventRowDTO> rows = new ArrayList<>();
            for (Event event : events) {
                int sessionCount = sessionCounts.getOrDefault(event.getId(), 0);
                rows.add(convertToDTO(event, sessionCount));
            }

            System.out.println("✓ convertEventsToRows completed in " + (System.currentTimeMillis() - start) + " ms");
            return rows;

        } catch (Exception e) {
            System.err.println("Error converting events: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Convert single event to EventRowDTO
     */
    public EventRowDTO convertToDTO(Event event, int sessionCount) {
        return new EventRowDTO(
                event.getId(),  // UUID eventId
                event.getName(),
                event.getType() != null ? event.getType().name() : "N/A",
                event.getLocation(),
                event.getStartDate() != null ? event.getStartDate().toString() : "N/A",
                event.getEndDate() != null ? event.getEndDate().toString() : "N/A",
                event.getStatus() != null ? event.getStatus().name() : "N/A",
                sessionCount,
                false,  // isRegistered - default false for admin view
                event.getImagePath()  // imagePath
        );
    }

    /**
     * Load session counts for all events (optimized: single SQL GROUP BY query)
     */
    private Map<UUID, Integer> loadSessionCounts(List<Event> events) {
        Map<UUID, Integer> sessionCounts = new HashMap<>();

        if (events.isEmpty()) {
            return sessionCounts;
        }

        try {
            // Try optimized SQL query first
            if (dbConnection != null) {
                String sql = "SELECT event_id, COUNT(*) AS cnt FROM sessions GROUP BY event_id";
                try (PreparedStatement ps = dbConnection.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID eventId = (UUID) rs.getObject("event_id");
                        int cnt = rs.getInt("cnt");
                        sessionCounts.put(eventId, cnt);
                    }
                    System.out.println("[EventDataConverterService] Session counts loaded via SQL query");
                    return sessionCounts;
                }
            }

            // Fallback: use sessionRepo if available
            if (sessionRepo != null) {
                List<UUID> eventIds = events.stream()
                        .map(Event::getId)
                        .toList();
                Map<UUID, Integer> counts = sessionRepo.countByEventIds(eventIds);
                sessionCounts.putAll(counts);
                System.out.println("[EventDataConverterService] Session counts loaded via SessionRepository");
            }

        } catch (Exception e) {
            System.err.println("Warning: Could not load session counts: " + e.getMessage());
        }

        return sessionCounts;
    }
}

