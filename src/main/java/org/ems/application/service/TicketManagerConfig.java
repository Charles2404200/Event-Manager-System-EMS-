package org.ems.application.service;

/**
 * TicketManagerConfig - Centralized configuration for ticket manager
 * @author EMS Team
 */
public class TicketManagerConfig {
    // Pagination
    public static final int PAGE_SIZE = 20;
    public static final int MAX_VISIBLE_PAGES = 5;

    // Timeouts & Delays
    public static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes
    public static final long ASYNC_TIMEOUT_MS = 30 * 1000; // 30 seconds

    // UI Styling
    public static final String BUTTON_ACTIVE_STYLE = "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;";
    public static final String BUTTON_INACTIVE_STYLE = "-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50;";
    public static final String ELLIPSIS_STYLE = "-fx-font-size: 11; -fx-text-fill: #666;";

    // Display Formats
    public static final String UUID_DISPLAY_LENGTH = "8"; // Show first 8 chars of UUID
}

