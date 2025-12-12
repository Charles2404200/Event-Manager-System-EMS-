package org.ems.domain.dto;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <your group number>
 *
 * Data Transfer Object for presenter statistics
 */
public class PresenterStatistics {

    // Basic counts
    public int totalSessions;
    public int totalAttendees;
    public int averageAudienceSize;

    // Event type distribution - Map<EventType, SessionCount>
    public Map<String, Integer> eventTypeDistribution;

    // Session engagement trends - Map<SessionTitle, AttendeeCount>
    public Map<String, Integer> sessionEngagementTrends;

    // Upcoming vs completed sessions
    public int upcomingSessions;
    public int completedSessions;

    public PresenterStatistics() {
        this.totalSessions = 0;
        this.totalAttendees = 0;
        this.averageAudienceSize = 0;
        this.eventTypeDistribution = new HashMap<>();
        this.sessionEngagementTrends = new LinkedHashMap<>();
        this.upcomingSessions = 0;
        this.completedSessions = 0;
    }
}