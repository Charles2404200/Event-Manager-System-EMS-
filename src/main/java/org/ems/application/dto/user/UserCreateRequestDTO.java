package org.ems.application.dto.user;

/**
 * DTO for creating a new user
 * Contains all information needed to create an Attendee or Presenter
 * @author <your group number>
 */
public class UserCreateRequestDTO {
    private final String fullName;
    private final String email;
    private final String username;
    private final String password;
    private final String phone;
    private final String role;

    public UserCreateRequestDTO(String fullName, String email, String username,
                                String password, String phone, String role) {
        this.fullName = fullName;
        this.email = email;
        this.username = username;
        this.password = password;
        this.phone = phone;
        this.role = role;
    }

    // Getters
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }

    @Override
    public String toString() {
        return "UserCreateRequestDTO{" +
                "username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}

