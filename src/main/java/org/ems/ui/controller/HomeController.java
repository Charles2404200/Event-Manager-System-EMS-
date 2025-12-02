package org.ems.ui.controller;

import javafx.fxml.FXML;
import org.ems.ui.stage.SceneManager;

/**
 * @author <your group number>
 */
public class HomeController {

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
}

