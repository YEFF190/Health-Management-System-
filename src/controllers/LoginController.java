package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import models.User;
import Services.UserService;
import utils.SceneSwitcher;
import utils.Session;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         statusLabel;

    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Email and password are required.");
            return;
        }

        User user = UserService.login(email, password);
        if (user == null) {
            statusLabel.setText("Invalid email or password.");
            return;
        }

        Session.setCurrentUser(user);

        switch (user.getRole().toLowerCase()) {
            case "admin"   -> SceneSwitcher.switchScene("/views/admin_dashboard.fxml",   "Admin Dashboard — HealthAssist");
            case "doctor"  -> SceneSwitcher.switchScene("/views/doctor_dashboard.fxml",  "Doctor Dashboard — HealthAssist");
            case "patient" -> SceneSwitcher.switchScene("/views/patient_dashboard.fxml", "Patient Portal — HealthAssist");
            default        -> statusLabel.setText("Unknown role. Contact your administrator.");
        }
    }

    @FXML
    private void goToRegister() {
    	
        SceneSwitcher.switchScene("/views/register.fxml", "Create Admin Account — HealthAssist");
    }
}