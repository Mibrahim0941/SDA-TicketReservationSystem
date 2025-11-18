package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Admin;

import java.io.IOException;

public class AdminSettingsController {

    private String currentUsername;
    private Admin currentAdmin;

    @FXML private Text welcomeTitle;
    @FXML private Text userGreeting;
    
    @FXML private TextField systemNameField;
    @FXML private TextField adminEmailField;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    
    @FXML private CheckBox emailNotificationsCheck;
    @FXML private CheckBox autoBackupCheck;
    @FXML private CheckBox maintenanceModeCheck;
    
    @FXML private Button saveSettingsButton;
    @FXML private Button changePasswordButton;
    @FXML private Button resetButton;
    @FXML private Button backButton;

    public static void show(Stage stage, String username, Admin admin) {
        try {
            FXMLLoader loader = new FXMLLoader(AdminSettingsController.class.getResource("/ui/admin-settings.fxml"));
            Parent root = loader.load();
            
            AdminSettingsController controller = loader.getController();
            controller.setAdminData(username, admin);
            
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(AdminSettingsController.class.getResource("/ui/admin-settings.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Admin Settings");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to load settings page: " + e.getMessage());
        }
    }

    public void setAdminData(String username, Admin admin) {
        this.currentUsername = username;
        this.currentAdmin = admin;
        
        if (userGreeting != null) {
            userGreeting.setText("System Settings - " + username);
        }
        
        Platform.runLater(this::loadSettings);
    }

    @FXML
    public void initialize() {
        System.out.println("AdminSettingsController initialized");
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        if (saveSettingsButton != null) {
            saveSettingsButton.setOnAction(e -> handleSaveSettings());
        }
        if (changePasswordButton != null) {
            changePasswordButton.setOnAction(e -> handleChangePassword());
        }
        if (resetButton != null) {
            resetButton.setOnAction(e -> handleReset());
        }
        if (backButton != null) {
            backButton.setOnAction(e -> handleBack());
        }
    }

    private void loadSettings() {
        // Load current settings
        systemNameField.setText("TicketGenie Booking System");
        adminEmailField.setText("admin@ticketgenie.com");
        emailNotificationsCheck.setSelected(true);
        autoBackupCheck.setSelected(true);
        maintenanceModeCheck.setSelected(false);
    }

    private void handleSaveSettings() {
        // Save system settings logic
        showSuccess("System settings saved successfully!");
    }

    private void handleChangePassword() {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all password fields");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showError("New passwords do not match");
            return;
        }
        
        // Password change logic would go here
        showSuccess("Password changed successfully!");
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    private void handleReset() {
        loadSettings();
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        showSuccess("Settings reset to default");
    }

    private void handleBack() {
        try {
            Stage currentStage = (Stage) backButton.getScene().getWindow();
            AdminDashboardController.show(currentStage, currentUsername, currentAdmin);
        } catch (Exception e) {
            showError("Failed to go back: " + e.getMessage());
        }
    }

    private void showError(String message) {
        showAlert("Error", message, Alert.AlertType.ERROR);
    }

    private void showSuccess(String message) {
        showAlert("Success", message, Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private static void showErrorAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}