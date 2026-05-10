package com.equipment.ui.controller;

import com.equipment.model.User;
import com.equipment.service.AuthService;
import com.equipment.ui.MainApp;
import com.equipment.ui.SessionManager;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * LoginController - Handles UC: Login
 *
 * BACKEND CONNECTION:
 *   Uses AuthService.login(username, password)
 *   No changes needed in AuthService.
 */
public class LoginController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Label errorLabel;
    @FXML private VBox loginCard;
    @FXML private Label subtitleLabel;

    // ── Backend service (your existing class, zero changes) ──
    private final AuthService authService = new AuthService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        errorLabel.setVisible(false);

        // Entrance animation
        loginCard.setOpacity(0);
        loginCard.setTranslateY(30);
        FadeTransition fade = new FadeTransition(Duration.millis(600), loginCard);
        fade.setFromValue(0); fade.setToValue(1); fade.play();
        TranslateTransition slide = new TranslateTransition(Duration.millis(600), loginCard);
        slide.setFromY(30); slide.setToY(0); slide.play();

        // Allow Enter key to submit
        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        errorLabel.setVisible(false);

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password.");
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("Signing in...");

        try {
            // ── Calls your existing AuthService ──
            User user = authService.login(username, password);

            if (user == null) {
                showError("Invalid username or password.");
                loginBtn.setDisable(false);
                loginBtn.setText("Sign In");
                return;
            }

            // Store in session
            SessionManager.setCurrentUser(user);

            // Navigate based on role
            switch (user.getRole()) {
                case TEACHER      -> MainApp.navigateTo("dashboard_teacher");
                case LAB_MANAGER  -> MainApp.navigateTo("dashboard_manager");
                case TECHNICIAN   -> MainApp.navigateTo("dashboard_technician");
            }

        } 
        catch (Exception ex) 
        {
            ex.printStackTrace();
            showError("Connection error: " + ex.getMessage());
            loginBtn.setDisable(false);
            loginBtn.setText("Sign In");
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        // Shake animation
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), loginCard);
        shake.setByX(10); shake.setCycleCount(4); shake.setAutoReverse(true); shake.play();
    }
}
