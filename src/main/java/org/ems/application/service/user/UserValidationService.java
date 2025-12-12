package org.ems.application.service.user;

import org.ems.application.dto.user.UserCreateRequestDTO;
import org.ems.application.dto.validation.ValidationResultDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for validating user input
 * Implements Single Responsibility Principle - only handles validation logic
 * @author <your group number>
 */
public class UserValidationService {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    /**
     * Validate new user creation request
     * @param request The user creation request to validate
     * @return ValidationResultDTO containing validation status and errors
     */
    public ValidationResultDTO validateNewUser(UserCreateRequestDTO request) {
        long start = System.currentTimeMillis();
        System.out.println("🔍 [UserValidationService] Validating new user: " + request.getUsername());

        List<String> errors = new ArrayList<>();

        // Validate full name
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            errors.add("Full name is required");
        }

        // Validate email
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            errors.add("Email is required");
        } else if (!isValidEmail(request.getEmail())) {
            errors.add("Email format is invalid");
        }

        // Validate username
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            errors.add("Username is required");
        }

        // Validate password
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            errors.add("Password is required");
        } else if (request.getPassword().length() < MIN_PASSWORD_LENGTH) {
            errors.add("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        // Validate role
        if (request.getRole() == null || request.getRole().isBlank()) {
            errors.add("Role is required");
        } else if (!isValidRole(request.getRole())) {
            errors.add("Invalid role: " + request.getRole());
        }

        ValidationResultDTO result = errors.isEmpty()
            ? ValidationResultDTO.success()
            : ValidationResultDTO.failure(errors);

        System.out.println("  ✓ Validation completed in " + (System.currentTimeMillis() - start) + " ms: " + result.isValid());
        return result;
    }

    /**
     * Validate email format
     * @param email The email to validate
     * @return true if email is valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate role
     * @param role The role to validate
     * @return true if role is valid, false otherwise
     */
    private boolean isValidRole(String role) {
        return role.equals("ATTENDEE") || role.equals("PRESENTER");
    }
}

