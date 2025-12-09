package org.ems.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.ems.application.service.IdentityService;
import org.ems.config.AppContext;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.enums.PresenterType;
import org.ems.ui.stage.SceneManager;

import java.time.LocalDate;

public class PresenterProfileController {

    @FXML
    private TextField txtUsername;

    @FXML
    private TextField txtFullname;

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtPhone;

    @FXML
    private DatePicker dob;

    @FXML
    private ComboBox<PresenterType> presenterRole;

    @FXML
    private TextArea txtBio;

    @FXML
    private Label errorLabel;

    private final AppContext appContext = AppContext.get();
    private final IdentityService identityService = appContext.identityService;
    Presenter presenter;

    public void initialize() {
        // Just for presenter
        if (!(appContext.currentUser instanceof Presenter)) {
            errorLabel.setText("Current user is not a presenter.");
            return;
        }

        presenter = (Presenter) appContext.currentUser;

        // Take the presenter role
        presenterRole.getItems().setAll(PresenterType.values());

        // input the data to UI
        txtFullname.setText(presenter.getFullName());
        txtEmail.setText(presenter.getEmail());
        txtPhone.setText(presenter.getPhone());
        txtUsername.setText(presenter.getUsername());
        dob.setValue(presenter.getDateOfBirth());
        if (presenter.getPresenterType() != null) {
            presenterRole.setValue(presenter.getPresenterType());
        }
        txtBio.setText(presenter.getBio());
    }

    @FXML
    private void onSave() {
        errorLabel.setText("");

        // take data from UI
        String username = txtUsername.getText().trim();
        String fullName = txtFullname.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        LocalDate birth = dob.getValue();
        PresenterType type = presenterRole.getValue();
        String bio = txtBio.getText();

        // validate

        if (username.isEmpty()) {
            errorLabel.setText("Username is empty.");
            return;
        }

        if (fullName.isEmpty()) {
            errorLabel.setText("Full name is required.");
            return;
        }

        if (phone.isEmpty() || !phone.matches("^[0-9]{10}$")) {
            errorLabel.setText("Phone number is required at the length of 10 digits only");
            return;
        }

        if (birth == null || birth.isAfter(LocalDate.now())) {
            errorLabel.setText("Invalid date of birth.");
            return;
        }

        try {
            // update the presenter profile
            presenter.setUsername(username);
            presenter.setFullName(fullName);
            presenter.setEmail(email);
            presenter.setPhone(phone);
            presenter.setDateOfBirth(birth);
            presenter.setBio(bio);
            if (type != null) {
                presenter.setPresenterType(type);
            }

            // save to database
            Presenter updated = identityService.updatePresenter(presenter);

            // update the information
            appContext.currentUser = updated;

            // Successfully save data
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText("Profile updated successfully.");
            alert.showAndWait();

            // navigate back to dashboard
            SceneManager.switchTo("dashboard.fxml", "Event Manager System - Dashboard");

        } catch (Exception e) {
            e.printStackTrace(System.err);
            errorLabel.setText("Failed to update profile: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        SceneManager.switchTo("dashboard.fxml", "Event Manager System - Dashboard");
    }
}

