package org.ems.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.ems.application.service.IdentityService;
import org.ems.domain.model.Person;
import org.ems.config.AppContext;
import org.ems.ui.stage.SceneManager;
import org.ems.ui.util.AsyncTaskService;
import org.ems.ui.util.LoadingDialog;

/**
 * @author <your group number>
 */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final IdentityService identityService = AppContext.get().identityService;
    private LoadingDialog loadingDialog;

    @FXML
    public void onLogin() {
        String email = emailField.getText();
        String pass = passwordField.getText();

        if (email == null || email.isBlank() || pass == null || pass.isBlank()) {
            errorLabel.setText("⚠ Please enter email/username and password.");
            return;
        }

        // Check if identity service is available
        if (identityService == null) {
            errorLabel.setText("✗ Database connection failed. Please check your connection.");
            return;
        }

        // Show loading dialog - safely get stage
        javafx.stage.Stage primaryStage = null;
        try {
            if (errorLabel != null && errorLabel.getScene() != null) {
                primaryStage = (javafx.stage.Stage) errorLabel.getScene().getWindow();
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not get stage for loading dialog");
        }

        if (primaryStage != null) {
            loadingDialog = new LoadingDialog(primaryStage, "Authenticating...");
            loadingDialog.show();
        }

        // Authenticate on background thread
        AsyncTaskService.runAsync(
                // Background task: Authenticate user
                () -> identityService.login(email, pass),

                // Success callback: Handle successful login
                user -> {
                    if (loadingDialog != null) {
                        loadingDialog.close();
                    }

                    if (user == null) {
                        errorLabel.setText("✗ Invalid email/username or password.");
                        return;
                    }

                    // Save user to AppContext
                    AppContext appContext = AppContext.get();
                    appContext.currentUser = user;
                    appContext.currentUserRole = user.getRole().name();

                    // Check user role and redirect accordingly
                    String role = user.getRole().name();

                    if (role.equals("SYSTEM_ADMIN") || role.equals("EVENT_ADMIN")) {
                        System.out.println("✓ Admin login successful: " + user.getUsername());
                        SceneManager.switchTo("admin_dashboard.fxml", "Event Manager System - Admin Dashboard");
                    } else if (role.equals("ATTENDEE") || role.equals("PRESENTER")) {
                        System.out.println("✓ " + role + " login successful: " + user.getUsername());
                        SceneManager.switchTo("dashboard.fxml", "Event Manager System - Dashboard");
                    } else {
                        errorLabel.setText("✗ Unknown user role.");
                    }
                },

                // Error callback: Handle authentication error
                error -> {
                    if (loadingDialog != null) {
                        loadingDialog.close();
                    }
                    errorLabel.setText("✗ Login error: " + error.getMessage());
                    System.err.println("✗ Login error: " + error.getMessage());
                    error.printStackTrace(System.err);
                }
        );
    }

    @FXML
    public void onSignup() {
        SceneManager.switchTo("signup.fxml", "Event Manager System - Sign Up");
    }
}
