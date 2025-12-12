package org.ems.application.service.auth;

import org.ems.domain.model.Person;
import org.ems.domain.model.enums.Role;
import java.util.HashSet;
import java.util.Set;

/**
 * Service for role-based authorization
 * Determines what features are available for each role
 *
 * @author <your group number>
 */
public class RoleAuthorizationService {

    /**
     * Check if current user can manage users
     */
    public static boolean canManageUsers(Person user) {
        if (user == null) return false;
        return user.getRole() == Role.SYSTEM_ADMIN;
    }

    /**
     * Check if current user can manage events
     */
    public static boolean canManageEvents(Person user) {
        if (user == null) return false;
        return user.getRole() == Role.SYSTEM_ADMIN || user.getRole() == Role.EVENT_ADMIN;
    }

    /**
     * Check if current user can manage sessions
     */
    public static boolean canManageSessions(Person user) {
        if (user == null) return false;
        return user.getRole() == Role.SYSTEM_ADMIN || user.getRole() == Role.EVENT_ADMIN;
    }

    /**
     * Check if current user can manage tickets
     */
    public static boolean canManageTickets(Person user) {
        if (user == null) return false;
        return user.getRole() == Role.SYSTEM_ADMIN || user.getRole() == Role.EVENT_ADMIN;
    }

    /**
     * Check if current user can manage presenters
     */
    public static boolean canManagePresenters(Person user) {
        if (user == null) return false;
        return user.getRole() == Role.SYSTEM_ADMIN;
    }

    /**
     * Check if current user can view reports
     */
    public static boolean canViewReports(Person user) {
        if (user == null) return false;
        return user.getRole() == Role.SYSTEM_ADMIN || user.getRole() == Role.EVENT_ADMIN;
    }

    /**
     * Check if current user can view activity logs
     */
    public static boolean canViewActivityLogs(Person user) {
        if (user == null) return false;
        return user.getRole() == Role.SYSTEM_ADMIN || user.getRole() == Role.EVENT_ADMIN;
    }

    /**
     * Check if current user can access settings
     */
    public static boolean canAccessSettings(Person user) {
        if (user == null) return false;
        return user.getRole() == Role.SYSTEM_ADMIN;
    }

    /**
     * Get user role display name
     */
    public static String getRoleDisplayName(Person user) {
        if (user == null) return "ANONYMOUS";
        return switch (user.getRole()) {
            case SYSTEM_ADMIN -> "SYSTEM ADMINISTRATOR";
            case EVENT_ADMIN -> "EVENT ADMINISTRATOR";
            case PRESENTER -> "PRESENTER";
            case ATTENDEE -> "ATTENDEE";
            case ANONYMOUS -> "ANONYMOUS";
        };
    }

    /**
     * Get allowed features for role
     */
    public static Set<String> getAllowedFeatures(Role role) {
        Set<String> features = new HashSet<>();

        switch (role) {
            case SYSTEM_ADMIN:
                // System Admin có tất cả features
                features.add("manage_users");
                features.add("manage_events");
                features.add("manage_sessions");
                features.add("manage_tickets");
                features.add("manage_presenters");
                features.add("view_reports");
                features.add("view_activity_logs");
                features.add("access_settings");
                features.add("manage_presenters_assignment");
                break;

            case EVENT_ADMIN:
                // Event Admin chỉ có event-related features
                features.add("manage_events");
                features.add("manage_sessions");
                features.add("manage_tickets");
                features.add("view_reports");
                features.add("view_activity_logs");
                // NOT: manage_users, manage_presenters, access_settings
                break;

            case PRESENTER:
            case ATTENDEE:
            case ANONYMOUS:
                // Các role khác không có admin features
                break;
        }

        return features;
    }

    /**
     * Check if feature is allowed
     */
    public static boolean isFeatureAllowed(Person user, String feature) {
        if (user == null) return false;
        Set<String> allowed = getAllowedFeatures(user.getRole());
        return allowed.contains(feature);
    }
}

