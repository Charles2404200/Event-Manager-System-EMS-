package org.ems.ui.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.ems.application.service.IdentityService;
import org.ems.config.AppContext;
import org.ems.config.DatabaseConfig;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Presenter;
import org.ems.ui.stage.SceneManager;

import java.time.LocalDate;

/**
 * @author <your group number>
 */
public class SignupController {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private final IdentityService identityService = AppContext.get().identityService;

    @FXML
    public void initialize() {
        // Initialize role dropdown
        roleCombo.setItems(FXCollections.observableArrayList(
                "ATTENDEE",
                "PRESENTER"
        ));
        roleCombo.setValue("ATTENDEE");
    }

    @FXML
    public void onSignup() {
        errorLabel.setText("");
        successLabel.setText("");

        // Validate inputs
        String fullName = fullNameField.getText();
        String email = emailField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String phone = phoneField.getText();
        String role = roleCombo.getValue();

        if (fullName == null || fullName.isBlank()) {
            errorLabel.setText(" Full name is required.");
            return;
        }

        if (email == null || email.isBlank() || !email.contains("@")) {
            errorLabel.setText(" Valid email is required.");
            return;
        }

        if (username == null || username.isBlank() || username.length() < 3) {
            errorLabel.setText(" Username must be at least 3 characters.");
            return;
        }

        if (password == null || password.isBlank() || password.length() < 6) {
            errorLabel.setText(" Password must be at least 6 characters.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            errorLabel.setText(" Passwords do not match.");
            return;
        }

        // Check if username already exists
        try {
            java.sql.Connection conn = org.ems.config.DatabaseConfig.getConnection();
            String checkQuery = "SELECT id FROM persons WHERE username = ?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(checkQuery)) {
                ps.setString(1, username);
                java.sql.ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    errorLabel.setText(" Username already exists. Please choose another username.");
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking username: " + e.getMessage());
        }

        // Check if email already exists
        try {
            java.sql.Connection conn = org.ems.config.DatabaseConfig.getConnection();
            String checkQuery = "SELECT id FROM persons WHERE email = ?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(checkQuery)) {
                ps.setString(1, email);
                java.sql.ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    errorLabel.setText(" Email already exists. Please use another email.");
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking email: " + e.getMessage());
        }

        // Check if service is available
        if (identityService == null) {
            errorLabel.setText(" Database connection failed. Please try again later.");
            return;
        }

        try {
            // Create user based on role
            org.ems.domain.model.Person newUser = null;

            if (role.equals("ATTENDEE")) {
                Attendee attendee = new Attendee(
                        fullName,
                        LocalDate.now(),
                        email,
                        phone,
                        username,
                        password
                );
                newUser = identityService.createAttendee(attendee);
            } else if (role.equals("PRESENTER")) {
                Presenter presenter = new Presenter(
                        fullName,
                        LocalDate.now(),
                        email,
                        phone,
                        username,
                        password
                );
                newUser = identityService.createPresenter(presenter);
            }

            // Auto-login the user
            if (newUser != null) {
                AppContext appContext = AppContext.get();
                appContext.currentUser = newUser;
                appContext.currentUserRole = newUser.getRole().name();

                successLabel.setText(" Account created successfully! Redirecting to dashboard...");

                // Redirect to dashboard after 2 seconds
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        javafx.application.Platform.runLater(() ->
                            SceneManager.switchTo("dashboard.fxml", "Event Manager System - Dashboard")
                        );
                    } catch (InterruptedException e) {
                        e.printStackTrace(System.err);
                    }
                }).start();
            } else {
                errorLabel.setText(" Error creating account. Please try again.");
            }

        } catch (Exception e) {
            String errorMessage = e.getMessage();

            // Check for specific database errors
            if (errorMessage != null && errorMessage.contains("duplicate key")) {
                if (errorMessage.contains("username")) {
                    errorLabel.setText(" Username already exists. Please choose another username.");
                } else if (errorMessage.contains("email")) {
                    errorLabel.setText(" Email already exists. Please use another email.");
                } else {
                    errorLabel.setText(" This account information already exists in the system.");
                }
            } else {
                errorLabel.setText(" Error creating account: " + errorMessage);
            }
            System.err.println("Signup error: " + errorMessage);
            e.printStackTrace(System.err);
        }
    }

    @FXML
    public void onBackToHome() {
        SceneManager.switchTo("home.fxml", "Event Manager System - Home");
    }
}

