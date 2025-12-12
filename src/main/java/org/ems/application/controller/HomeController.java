package org.ems.application.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.ems.infrastructure.config.AppContext;
import org.ems.domain.model.Person;
import org.ems.ui.stage.SceneManager;

/**
 * @author <your group number>
 */
public class HomeController {

    @FXML private VBox centerBox;
    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        try {
            AppContext context = AppContext.get();
            Person currentUser = context.currentUser;

            // If user is logged in, show personalized message
            if (currentUser != null) {
                if (welcomeLabel != null) {
                    welcomeLabel.setText("Welcome, " + currentUser.getFullName() + "!");
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing home: " + e.getMessage());
        }
    }

    @FXML
    public void onLogin() {
        SceneManager.switchTo("login.fxml", "Event Manager System - Login");
    }

    @FXML
    public void onSignup() {
        SceneManager.switchTo("signup.fxml", "Event Manager System - Sign Up");
    }

    @FXML
    public void onViewEvents() {
        SceneManager.switchTo("view_events_home.fxml", "Event Manager System - Browse Events");
    }

    @FXML
    public void onBrowsePresenters() {
        // TODO: Implement browse presenters functionality
        System.out.println("Browse Presenters clicked");
    }

    @FXML
    public void onLogout() {
        try {
            AppContext context = AppContext.get();
            context.currentUser = null;
            SceneManager.switchTo("home.fxml", "Event Manager System - Home");
        } catch (Exception e) {
            System.err.println("Error logging out: " + e.getMessage());
        }
    }
}

