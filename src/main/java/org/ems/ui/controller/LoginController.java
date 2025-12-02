package org.ems.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.ems.application.service.IdentityService;
import org.ems.domain.model.Person;
import org.ems.config.AppContext;
import org.ems.ui.stage.SceneManager;

/**
 * @author <your group number>
 */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final IdentityService identityService = AppContext.get().identityService;

    @FXML
    public void onLogin() {
        String email = emailField.getText();
        String pass = passwordField.getText();

        if (email == null || email.isBlank() || pass == null || pass.isBlank()) {
            errorLabel.setText(" Please enter email/username and password.");
            return;
        }

        // Check if identity service is available
        if (identityService == null) {
            errorLabel.setText(" Database connection failed. Please check your connection and try again.");
            return;
        }

        try {
            // Login with credentials
            Person user = identityService.login(email, pass);

            if (user == null) {
                errorLabel.setText(" Invalid email/username or password.");
                return;
            }

            // Save user to AppContext
            AppContext appContext = AppContext.get();
            appContext.currentUser = user;
            appContext.currentUserRole = user.getRole().name();

            // Check user role and redirect accordingly
            String role = user.getRole().name();

            if (role.equals("SYSTEM_ADMIN") || role.equals("EVENT_ADMIN")) {
                // Admin user - redirect to admin dashboard
                System.out.println(" Admin login successful: " + user.getUsername());
                SceneManager.switchTo("admin_dashboard.fxml", "Event Manager System - Admin Dashboard");
            } else if (role.equals("ATTENDEE") || role.equals("PRESENTER")) {
                // Participant user - redirect to user dashboard (handles both attendee and presenter)
                System.out.println(" " + role + " login successful: " + user.getUsername());
                SceneManager.switchTo("dashboard.fxml", "Event Manager System - Dashboard");
            } else {
                // Unknown role
                errorLabel.setText(" Unknown user role.");
            }

        } catch (Exception e) {
            errorLabel.setText(" Login error: " + e.getMessage());
            System.err.println("Login error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
