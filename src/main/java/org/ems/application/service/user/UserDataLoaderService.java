package org.ems.application.service.user;

import org.ems.application.dto.user.UserDisplayRowDTO;
import org.ems.infrastructure.config.DatabaseConfig;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Presenter;
import org.ems.domain.repository.AttendeeRepository;
import org.ems.domain.repository.PresenterRepository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for loading user data from repositories
 * Handles complex multi-source data loading logic
 * Implements Single Responsibility Principle - only handles data loading
 * @author <your group number>
 */
public class UserDataLoaderService {

    private final AttendeeRepository attendeeRepo;
    private final PresenterRepository presenterRepo;
    private ProgressCallback progressCallback;

    public interface ProgressCallback {
        void onProgress(int percent);
    }

    public UserDataLoaderService(AttendeeRepository attendeeRepo, PresenterRepository presenterRepo) {
        this.attendeeRepo = attendeeRepo;
        this.presenterRepo = presenterRepo;
    }

    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    /**
     * Load all users from all sources (attendees, presenters, admin users)
     * @return List of UserDisplayRowDTO containing all users
     * @throws UserDataLoadException if loading fails
     */
    public List<UserDisplayRowDTO> loadAllUsers() throws UserDataLoadException {
        long taskStart = System.currentTimeMillis();
        System.out.println("🔄 [UserDataLoaderService] Loading all users...");

        List<UserDisplayRowDTO> users = new ArrayList<>();

        try {
            // Step 1: Load attendees
            long attendeeStart = System.currentTimeMillis();
            users.addAll(loadAttendees());
            long attendeeTime = System.currentTimeMillis() - attendeeStart;
            System.out.println("  ✓ Attendees loaded in " + attendeeTime + " ms");
            updateProgress(30);

            // Step 2: Load presenters
            long presenterStart = System.currentTimeMillis();
            users.addAll(loadPresenters());
            long presenterTime = System.currentTimeMillis() - presenterStart;
            System.out.println("  ✓ Presenters loaded in " + presenterTime + " ms");
            updateProgress(60);

            // Step 3: Load admin users
            long adminStart = System.currentTimeMillis();
            users.addAll(loadAdminUsers());
            long adminTime = System.currentTimeMillis() - adminStart;
            System.out.println("  ✓ Admin users loaded in " + adminTime + " ms");
            updateProgress(90);

            long totalTime = System.currentTimeMillis() - taskStart;
            System.out.println("  ✓ Total users loaded: " + users.size() + " in " + totalTime + " ms");
            updateProgress(100);

            return users;

        } catch (Exception e) {
            System.err.println("  ✗ Error loading users: " + e.getMessage());
            e.printStackTrace();
            throw new UserDataLoadException("Failed to load users: " + e.getMessage(), e);
        }
    }

    /**
     * Load attendees from repository
     * @return List of attendee UserDisplayRowDTOs
     */
    private List<UserDisplayRowDTO> loadAttendees() {
        List<UserDisplayRowDTO> attendeeRows = new ArrayList<>();

        if (attendeeRepo == null) {
            System.out.println("    ⚠️ AttendeeRepository is null");
            return attendeeRows;
        }

        try {
            List<Attendee> attendees = attendeeRepo.findAll();
            for (Attendee attendee : attendees) {
                attendeeRows.add(new UserDisplayRowDTO(
                        attendee.getId().toString(),
                        attendee.getUsername(),
                        attendee.getFullName(),
                        attendee.getEmail(),
                        attendee.getPhone(),
                        "ATTENDEE",
                        attendee.getDateOfBirth() != null ? attendee.getDateOfBirth().toString() : "N/A",
                        "N/A"
                ));
            }
            System.out.println("    ℹ findAll(attendees) returned " + attendees.size() + " records");
        } catch (Exception e) {
            System.err.println("    ⚠️ Error loading attendees: " + e.getMessage());
            // Log but don't throw - allow partial loading to continue
        }

        return attendeeRows;
    }

    /**
     * Load presenters from repository
     * @return List of presenter UserDisplayRowDTOs
     */
    private List<UserDisplayRowDTO> loadPresenters() {
        List<UserDisplayRowDTO> presenterRows = new ArrayList<>();

        if (presenterRepo == null) {
            System.out.println("    ⚠️ PresenterRepository is null");
            return presenterRows;
        }

        try {
            List<Presenter> presenters = presenterRepo.findAll();
            for (Presenter presenter : presenters) {
                presenterRows.add(new UserDisplayRowDTO(
                        presenter.getId().toString(),
                        presenter.getUsername(),
                        presenter.getFullName(),
                        presenter.getEmail(),
                        presenter.getPhone(),
                        "PRESENTER",
                        presenter.getDateOfBirth() != null ? presenter.getDateOfBirth().toString() : "N/A",
                        "N/A"
                ));
            }
            System.out.println("    ℹ findAll(presenters) returned " + presenters.size() + " records");
        } catch (Exception e) {
            System.err.println("    ⚠️ Error loading presenters: " + e.getMessage());
            // Log but don't throw - allow partial loading to continue
        }

        return presenterRows;
    }

    /**
     * Load admin users (SYSTEM_ADMIN, EVENT_ADMIN) from database
     * @return List of admin user UserDisplayRowDTOs
     */
    private List<UserDisplayRowDTO> loadAdminUsers() {
        List<UserDisplayRowDTO> adminRows = new ArrayList<>();

        try {
            Connection conn = DatabaseConfig.getConnection();
            String adminQuery = "SELECT * FROM persons WHERE role IN ('SYSTEM_ADMIN', 'EVENT_ADMIN')";

            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(adminQuery);
                while (rs.next()) {
                    adminRows.add(new UserDisplayRowDTO(
                            rs.getString("id"),
                            rs.getString("username"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("role"),
                            rs.getString("dob") != null ? rs.getString("dob") : "N/A",
                            rs.getString("created_at") != null ? rs.getString("created_at") : "N/A"
                    ));
                }
                System.out.println("    ℹ Admin query returned " + adminRows.size() + " records");
            }
        } catch (Exception e) {
            System.err.println("    ⚠️ Error loading admin users: " + e.getMessage());
            // Log but don't throw - allow partial loading to continue
        }

        return adminRows;
    }

    private void updateProgress(int percent) {
        if (progressCallback != null) {
            progressCallback.onProgress(percent);
        }
    }

    /**
     * Custom exception for user data loading errors
     */
    public static class UserDataLoadException extends Exception {
        public UserDataLoadException(String message) {
            super(message);
        }

        public UserDataLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

