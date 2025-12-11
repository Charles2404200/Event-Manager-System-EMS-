package org.ems.application.dto;

/**
 * DTO for displaying user information in the ManageUsers table
 * Immutable representation of user data for UI layer
 * @author <your group number>
 */
public class UserDisplayRowDTO {
    private final String id;
    private final String username;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String role;
    private final String dateOfBirth;
    private final String createdDate;

    public UserDisplayRowDTO(String id, String username, String fullName, String email,
                             String phone, String role, String dateOfBirth, String createdDate) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.dateOfBirth = dateOfBirth;
        this.createdDate = createdDate;
    }

    // Getters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getCreatedDate() { return createdDate; }

    @Override
    public String toString() {
        return "UserDisplayRowDTO{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}

