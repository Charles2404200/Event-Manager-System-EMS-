package org.ems.domain.model;

import org.ems.domain.model.enums.Role;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Base domain entity for all types of users.
 */
public abstract class Person {

    // Unique identifier
    private UUID id;
    private String fullName;
    private LocalDate dateOfBirth;
    private String email;
    private String phone;

    // A2 - Login system fields
    private String username;
    private String passwordHash;
    private Role role;

    protected Person() {
        this.id = UUID.randomUUID();
    }

    protected Person(String fullName, LocalDate dateOfBirth,
                     String email, String phone,
                     String username, String passwordHash,
                     Role role) {

        this.id = UUID.randomUUID();
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
