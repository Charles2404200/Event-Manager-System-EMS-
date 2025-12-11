package org.ems.application.service;

import org.ems.domain.model.enums.Role;

/**
 * Service for user profile information and role display
 * Implements Single Responsibility Principle - only handles profile/role info
 * @author <your group number>
 */
public class UserProfileService {

    /**
     * Get human-readable role name
     * @param role The user role
     * @return Display name for the role
     */
    public String getRoleDisplayName(Role role) {
        long start = System.currentTimeMillis();
        String displayName = switch (role) {
            case ATTENDEE -> "Event Attendee";
            case PRESENTER -> "Presenter";
            case EVENT_ADMIN -> "Event Administrator";
            case SYSTEM_ADMIN -> "System Administrator";
            default -> "Unknown";
        };
        System.out.println("✓ getRoleDisplayName(" + role + ") = " + displayName + " (" + (System.currentTimeMillis() - start) + "ms)");
        return displayName;
    }

    /**
     * Check if role is attendee
     */
    public boolean isAttendee(Role role) {
        return Role.ATTENDEE == role;
    }

    /**
     * Check if role is presenter
     */
    public boolean isPresenter(Role role) {
        return Role.PRESENTER == role;
    }

    /**
     * Check if role is event admin
     */
    public boolean isEventAdmin(Role role) {
        return Role.EVENT_ADMIN == role;
    }

    /**
     * Check if role is system admin
     */
    public boolean isSystemAdmin(Role role) {
        return Role.SYSTEM_ADMIN == role;
    }
}

