package org.ems.application.impl;

import org.ems.application.service.PresenterStatisticsService;
import org.ems.domain.dto.PresenterStatistics;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author <your group number>
 *
 * Optimized implementation using batch queries to avoid N+1 problem
 */
public class PresenterStatisticsServiceImpl implements PresenterStatisticsService {

    private final Connection connection;

    public PresenterStatisticsServiceImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public PresenterStatistics generateStatistics(UUID presenterId) {
        PresenterStatistics stats = new PresenterStatistics();

        try {
            // SINGLE OPTIMIZED QUERY: Get all metrics in one go
            String sql = """
                WITH presenter_sessions AS (
                    SELECT 
                        s.id AS session_id,
                        s.title AS session_title,
                        s.capacity,
                        s.start_time,
                        e.type AS event_type,
                        e.status AS event_status
                    FROM sessions s
                    JOIN presenter_session ps ON s.id = ps.session_id
                    JOIN events e ON s.event_id = e.id
                    WHERE ps.presenter_id = ?
                ),
                session_attendees AS (
                    SELECT 
                        ps.session_id,
                        COUNT(ats.attendee_id) AS attendee_count
                    FROM presenter_sessions ps
                    LEFT JOIN attendee_session ats ON ps.session_id = ats.session_id
                    GROUP BY ps.session_id
                )
                SELECT 
                    ps.session_id,
                    ps.session_title,
                    ps.capacity,
                    ps.start_time,
                    ps.event_type,
                    ps.event_status,
                    COALESCE(sa.attendee_count, 0) AS attendee_count
                FROM presenter_sessions ps
                LEFT JOIN session_attendees sa ON ps.session_id = sa.session_id
                ORDER BY ps.start_time DESC
            """;

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setObject(1, presenterId);

                ResultSet rs = ps.executeQuery();

                int totalAttendees = 0;
                Map<String, Integer> eventTypeCounts = new HashMap<>();
                Map<String, Integer> engagementMap = new LinkedHashMap<>();
                LocalDateTime now = LocalDateTime.now();

                while (rs.next()) {
                    stats.totalSessions++;

                    int attendeeCount = rs.getInt("attendee_count");
                    totalAttendees += attendeeCount;

                    // Event type distribution
                    String eventType = rs.getString("event_type");
                    eventTypeCounts.put(eventType, eventTypeCounts.getOrDefault(eventType, 0) + 1);

                    // Session engagement trends (top 10 sessions by attendance)
                    String sessionTitle = rs.getString("session_title");
                    engagementMap.put(sessionTitle, attendeeCount);

                    // Upcoming vs completed
                    LocalDateTime startTime = rs.getObject("start_time", LocalDateTime.class);
                    String eventStatus = rs.getString("event_status");

                    if (startTime != null && startTime.isAfter(now)) {
                        stats.upcomingSessions++;
                    } else if ("COMPLETED".equals(eventStatus)) {
                        stats.completedSessions++;
                    }
                }

                stats.totalAttendees = totalAttendees;
                stats.averageAudienceSize = stats.totalSessions > 0
                        ? totalAttendees / stats.totalSessions
                        : 0;

                stats.eventTypeDistribution = eventTypeCounts;

                // Keep only top 10 sessions by engagement
                stats.sessionEngagementTrends = engagementMap.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(10)
                        .collect(
                                java.util.LinkedHashMap::new,
                                (m, e) -> m.put(e.getKey(), e.getValue()),
                                java.util.LinkedHashMap::putAll
                        );
            }

        } catch (SQLException e) {
            System.err.println("Error generating presenter statistics: " + e.getMessage());
            e.printStackTrace();
        }

        return stats;
    }
}