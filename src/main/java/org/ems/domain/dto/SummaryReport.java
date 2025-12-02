package org.ems.domain.dto;

import java.util.Map;

public class SummaryReport {

    public int totalEvents;
    public int totalSessions;
    public int totalAttendees;
    public int totalTickets;

    public double totalRevenue;
    public Map<String, Double> revenueByEvent;
    public Map<String, Double> revenueByType;

    public double ticketUsedPercent;
    public Map<String, Long> topEvents;
}

