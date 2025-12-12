package org.ems.application.service.attendee;

/**
 * Configuration class for attendee service manager
 * Contains constants and settings for attendee operations
 * Implements Dependency Injection pattern
 *
 * @author <your group number>
 */
public class AttendeeManagerConfig {

    // Pagination settings
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MIN_PAGE_SIZE = 1;

    // Export settings
    public static final String EXPORT_DIR = "exports/attendee";
    public static final String CSV_EXTENSION = ".csv";
    public static final String EXCEL_EXTENSION = ".xlsx";
    public static final String PDF_EXTENSION = ".pdf";

    // Validation settings
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_EMAIL_LENGTH = 255;
    public static final int MAX_FULL_NAME_LENGTH = 100;

    // Activity tracking
    public static final boolean TRACK_ACTIVITY = true;
    public static final int MAX_ACTIVITY_HISTORY = 100;

    // Cache settings
    public static final boolean ENABLE_CACHING = true;
    public static final long CACHE_EXPIRY_MS = 3600000; // 1 hour
    public static final int MAX_CACHE_SIZE = 1000;

    // Query optimization
    public static final int BATCH_LOAD_SIZE = 100;
    public static final boolean USE_BATCH_LOADING = true;

    // Timeout settings
    public static final long OPERATION_TIMEOUT_MS = 30000; // 30 seconds

    // Export settings
    public static final boolean INCLUDE_ACTIVITY_IN_EXPORT = true;
    public static final boolean INCLUDE_SENSITIVE_DATA = false;

    /**
     * Get default page size for pagination
     *
     * @return Default page size
     */
    public static int getDefaultPageSize() {
        return DEFAULT_PAGE_SIZE;
    }

    /**
     * Validate page size
     *
     * @param pageSize Size to validate
     * @return Validated page size (clamped to min/max)
     */
    public static int validatePageSize(int pageSize) {
        if (pageSize < MIN_PAGE_SIZE) return MIN_PAGE_SIZE;
        if (pageSize > MAX_PAGE_SIZE) return MAX_PAGE_SIZE;
        return pageSize;
    }

    /**
     * Get export directory path
     *
     * @return Export directory path
     */
    public static String getExportDirectory() {
        return EXPORT_DIR;
    }

    /**
     * Check if activity tracking is enabled
     *
     * @return true if activity tracking is enabled
     */
    public static boolean isActivityTrackingEnabled() {
        return TRACK_ACTIVITY;
    }

    /**
     * Check if caching is enabled
     *
     * @return true if caching is enabled
     */
    public static boolean isCachingEnabled() {
        return ENABLE_CACHING;
    }
}

