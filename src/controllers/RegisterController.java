package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import models.User;
import Services.UserService;
import utils.SceneSwitcher;

public class RegisterController {

    @FXML private TextField     nameField;
    @FXML private TextField     emailField;
    @FXML private TextField     phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         statusLabel;

    @FXML
    private void handleRegister() {
        statusLabel.setStyle("-fx-text-fill: red;");

        if (nameField.getText().trim().isEmpty() ||
            emailField.getText().trim().isEmpty() ||
            passwordField.getText().isEmpty()) {
            statusLabel.setText("Name, email and password are required.");
            return;
        }
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            statusLabel.setText("Passwords do not match.");
            return;
        }
        if (passwordField.getText().length() < 6) {
            statusLabel.setText("Password must be at least 6 characters.");
            return;
        }

        User user = new User();
        user.setName(nameField.getText().trim());
        user.setEmail(emailField.getText().trim());
        user.setPasswordHash(passwordField.getText());
        user.setPhone(phoneField.getText().trim());

        boolean success = UserService.selfRegister(user);
        if (success) {
            statusLabel.setStyle("-fx-text-fill: #00b09b;");
            statusLabel.setText("Admin account created! Redirecting to login…");
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (Exception ignored) {}
                javafx.application.Platform.runLater(() ->
                    SceneSwitcher.switchScene("/views/login.fxml", "Login — HealthAssist")
                );
            }).start();
        } else {
            statusLabel.setText("Registration failed. Email may already be in use.");
        }
    }

    @FXML
    private void goToLogin() {
        SceneSwitcher.switchScene("/views/login.fxml", "Login — HealthAssist");
    }
}