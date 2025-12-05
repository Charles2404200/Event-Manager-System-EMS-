package org.ems.domain.dto;

import java.util.Map;

/**
 * @author <your group number>
 */
public class PresenterStatisticsDTO {

    // Number of sessions presented
    private int totalSessions;

    // Total audience across all sessions
    private int totalAudience;

    // Average audience per session
    private double averageAudiencePerSession;

    // Event type distribution (EventType -> count)
    private Map<String, Integer> eventTypeDistribution;

    // Session engagement trends (SessionID -> attendee count)
    private Map<String, Integer> sessionEngagement;

    // Upcoming sessions count
    private int upcomingSessions;

    // Completed sessions count
    private int completedSessions;

    public PresenterStatisticsDTO() {
    }

    public PresenterStatisticsDTO(int totalSessions, int totalAudience,
                                  double averageAudiencePerSession,
                                  Map<String, Integer> eventTypeDistribution,
                                  Map<String, Integer> sessionEngagement,
                                  int upcomingSessions, int completedSessions) {
        this.totalSessions = totalSessions;
        this.totalAudience = totalAudience;
        this.averageAudiencePerSession = averageAudiencePerSession;
        this.eventTypeDistribution = eventTypeDistribution;
        this.sessionEngagement = sessionEngagement;
        this.upcomingSessions = upcomingSessions;
        this.completedSessions = completedSessions;
    }

    // Getters and Setters
    public int getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
    }

    public int getTotalAudience() {
        return totalAudience;
    }

    public void setTotalAudience(int totalAudience) {
        this.totalAudience = totalAudience;
    }

    public double getAverageAudiencePerSession() {
        return averageAudiencePerSession;
    }

    public void setAverageAudiencePerSession(double averageAudiencePerSession) {
        this.averageAudiencePerSession = averageAudiencePerSession;
    }

    public Map<String, Integer> getEventTypeDistribution() {
        return eventTypeDistribution;
    }

    public void setEventTypeDistribution(Map<String, Integer> eventTypeDistribution) {
        this.eventTypeDistribution = eventTypeDistribution;
    }

    public Map<String, Integer> getSessionEngagement() {
        return sessionEngagement;
    }

    public void setSessionEngagement(Map<String, Integer> sessionEngagement) {
        this.sessionEngagement = sessionEngagement;
    }

    public int getUpcomingSessions() {
        return upcomingSessions;
    }

    public void setUpcomingSessions(int upcomingSessions) {
        this.upcomingSessions = upcomingSessions;
    }

    public int getCompletedSessions() {
        return completedSessions;
    }

    public void setCompletedSessions(int completedSessions) {
        this.completedSessions = completedSessions;
    }
}