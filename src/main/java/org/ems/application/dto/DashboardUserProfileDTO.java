package org.ems.application.dto;

/**
 * DTO for user profile information displayed in dashboard
 * @author <your group number>
 */
public class DashboardUserProfileDTO {
    private final String fullName;
    private final String email;
    private final String roleDisplayName;
    private final String role;

    public DashboardUserProfileDTO(String fullName, String email, String roleDisplayName, String role) {
        this.fullName = fullName;
        this.email = email;
        this.roleDisplayName = roleDisplayName;
        this.role = role;
    }

    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRoleDisplayName() { return roleDisplayName; }
    public String getRole() { return role; }

    @Override
    public String toString() {
        return "DashboardUserProfileDTO{" +
                "fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}

