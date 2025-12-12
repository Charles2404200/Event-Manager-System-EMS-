package org.ems.application.service.user;

import org.ems.application.dto.user.UserCreateRequestDTO;
import org.ems.application.dto.user.UserDisplayRowDTO;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Presenter;
import org.ems.domain.repository.AttendeeRepository;
import org.ems.domain.repository.PresenterRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Main service for user management operations
 * Orchestrates user CRUD operations and delegates to appropriate repositories
 * Implements Facade pattern - provides simplified interface to complex subsystems
 * @author <your group number>
 */
public class UserManagementService {

    private final AttendeeRepository attendeeRepo;
    private final PresenterRepository presenterRepo;
    private final UserDataLoaderService dataLoaderService;
    private final UserValidationService validationService;

    public UserManagementService(AttendeeRepository attendeeRepo, PresenterRepository presenterRepo) {
        this.attendeeRepo = attendeeRepo;
        this.presenterRepo = presenterRepo;
        this.dataLoaderService = new UserDataLoaderService(attendeeRepo, presenterRepo);
        this.validationService = new UserValidationService();
    }

    /**
     * Load all users from all sources
     * @return List of all users
     * @throws UserManagementException if loading fails
     */
    public List<UserDisplayRowDTO> loadAllUsers() throws UserManagementException {
        long start = System.currentTimeMillis();
        System.out.println("📋 [UserManagementService] loadAllUsers() starting...");

        try {
            List<UserDisplayRowDTO> users = dataLoaderService.loadAllUsers();
            System.out.println("✓ loadAllUsers() completed in " + (System.currentTimeMillis() - start) + " ms");
            return users;
        } catch (UserDataLoaderService.UserDataLoadException e) {
            String message = "Failed to load users: " + e.getMessage();
            System.err.println("✗ " + message);
            throw new UserManagementException(message, e);
        }
    }

    /**
     * Load all users with progress callback
     * @param progressCallback Callback to track loading progress
     * @return List of all users
     * @throws UserManagementException if loading fails
     */
    public List<UserDisplayRowDTO> loadAllUsersWithProgress(UserDataLoaderService.ProgressCallback progressCallback)
            throws UserManagementException {
        long start = System.currentTimeMillis();
        System.out.println("📋 [UserManagementService] loadAllUsersWithProgress() starting...");

        try {
            dataLoaderService.setProgressCallback(progressCallback);
            List<UserDisplayRowDTO> users = dataLoaderService.loadAllUsers();
            System.out.println("✓ loadAllUsersWithProgress() completed in " + (System.currentTimeMillis() - start) + " ms");
            return users;
        } catch (UserDataLoaderService.UserDataLoadException e) {
            String message = "Failed to load users: " + e.getMessage();
            System.err.println("✗ " + message);
            throw new UserManagementException(message, e);
        }
    }

    /**
     * Create a new user (Attendee or Presenter)
     * @param request The user creation request
     * @return Created user display data
     * @throws UserManagementException if creation fails
     */
    public UserDisplayRowDTO createUser(UserCreateRequestDTO request) throws UserManagementException {
        long start = System.currentTimeMillis();
        System.out.println("➕ [UserManagementService] createUser() - " + request.getUsername());

        // Validate input
        var validationResult = validationService.validateNewUser(request);
        if (!validationResult.isValid()) {
            String error = "User creation validation failed: " + validationResult.getAllErrorsAsString();
            System.err.println("✗ " + error);
            throw new UserManagementException(error);
        }

        try {
            if ("ATTENDEE".equals(request.getRole())) {
                Attendee attendee = new Attendee(
                        request.getFullName(),
                        LocalDate.now(),
                        request.getEmail(),
                        request.getPhone(),
                        request.getUsername(),
                        request.getPassword()
                );
                Attendee savedAttendee = attendeeRepo.save(attendee);
                System.out.println("  ✓ Attendee created in " + (System.currentTimeMillis() - start) + " ms");
                return convertToDisplayRow(savedAttendee);

            } else if ("PRESENTER".equals(request.getRole())) {
                Presenter presenter = new Presenter(
                        request.getFullName(),
                        LocalDate.now(),
                        request.getEmail(),
                        request.getPhone(),
                        request.getUsername(),
                        request.getPassword()
                );
                Presenter savedPresenter = presenterRepo.save(presenter);
                System.out.println("  ✓ Presenter created in " + (System.currentTimeMillis() - start) + " ms");
                return convertToDisplayRow(savedPresenter);

            } else {
                throw new UserManagementException("Invalid role: " + request.getRole());
            }

        } catch (Exception e) {
            String message = "Failed to create user: " + e.getMessage();
            System.err.println("✗ " + message);
            e.printStackTrace();
            throw new UserManagementException(message, e);
        }
    }

    /**
     * Delete a user by ID
     * @param userId The user ID to delete
     * @param role The role of the user to delete
     * @throws UserManagementException if deletion fails
     */
    public void deleteUser(UUID userId, String role) throws UserManagementException {
        long start = System.currentTimeMillis();
        System.out.println("🗑️ [UserManagementService] deleteUser() - " + userId + " (" + role + ")");

        try {
            if ("ATTENDEE".equals(role)) {
                if (attendeeRepo != null) {
                    attendeeRepo.delete(userId);
                    System.out.println("  ✓ Attendee deleted in " + (System.currentTimeMillis() - start) + " ms");
                } else {
                    throw new UserManagementException("AttendeeRepository is not available");
                }

            } else if ("PRESENTER".equals(role)) {
                if (presenterRepo != null) {
                    presenterRepo.delete(userId);
                    System.out.println("  ✓ Presenter deleted in " + (System.currentTimeMillis() - start) + " ms");
                } else {
                    throw new UserManagementException("PresenterRepository is not available");
                }

            } else if ("SYSTEM_ADMIN".equals(role) || "EVENT_ADMIN".equals(role)) {
                throw new UserManagementException("Cannot delete admin users from application");

            } else {
                throw new UserManagementException("Invalid role: " + role);
            }

        } catch (Exception e) {
            String message = "Failed to delete user: " + e.getMessage();
            System.err.println("✗ " + message);
            e.printStackTrace();
            throw new UserManagementException(message, e);
        }
    }

    /**
     * Convert Attendee to UserDisplayRowDTO
     */
    private UserDisplayRowDTO convertToDisplayRow(Attendee attendee) {
        return new UserDisplayRowDTO(
                attendee.getId().toString(),
                attendee.getUsername(),
                attendee.getFullName(),
                attendee.getEmail(),
                attendee.getPhone(),
                "ATTENDEE",
                attendee.getDateOfBirth() != null ? attendee.getDateOfBirth().toString() : "N/A",
                "N/A"
        );
    }

    /**
     * Convert Presenter to UserDisplayRowDTO
     */
    private UserDisplayRowDTO convertToDisplayRow(Presenter presenter) {
        return new UserDisplayRowDTO(
                presenter.getId().toString(),
                presenter.getUsername(),
                presenter.getFullName(),
                presenter.getEmail(),
                presenter.getPhone(),
                "PRESENTER",
                presenter.getDateOfBirth() != null ? presenter.getDateOfBirth().toString() : "N/A",
                "N/A"
        );
    }

    /**
     * Custom exception for user management errors
     */
    public static class UserManagementException extends Exception {
        public UserManagementException(String message) {
            super(message);
        }

        public UserManagementException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

