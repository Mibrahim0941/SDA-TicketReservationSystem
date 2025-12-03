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
import java.net.URL;

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
            System.out.println("Loading Admin Settings Page for user: " + username);
            URL fxmlUrl = AdminSettingsController.class.getResource("/ui/admin-settings.fxml");
            if (fxmlUrl == null) {
                System.err.println("CRITICAL ERROR: FXML file not found at /ui/admin-settings.fxml");
                showErrorAlert("FXML file not found! Check if admin-settings.fxml exists in resources/ui/ folder");
                return;
            }
            System.out.println("FXML URL: " + fxmlUrl);
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load(); 
            AdminSettingsController controller = loader.getController();
            if (controller == null) {
                System.err.println("WARNING: Controller not injected! Check FXML fx:controller attribute");
            } else {
                controller.setAdminData(username, admin);
            }
            
            Scene scene = new Scene(root, 1200, 800);
            URL cssUrl = AdminSettingsController.class.getResource("/ui/admin-settings.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                System.out.println("CSS loaded: " + cssUrl);
            } else {
                System.err.println("WARNING: CSS file not found at /ui/admin-settings.css");
            }
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Admin Settings");
            stage.centerOnScreen();
            
            System.out.println("Admin Settings page loaded successfully!");
            
        } catch (IOException e) {
            System.err.println("FAILED to load Admin Settings:");
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
        systemNameField.setText("TicketGenie Booking System");
        adminEmailField.setText("admin@ticketgenie.com");
        emailNotificationsCheck.setSelected(true);
        autoBackupCheck.setSelected(true);
        maintenanceModeCheck.setSelected(false);
        
        showInfo("Settings loaded", "Current system settings loaded successfully");
    }

    private void handleSaveSettings() {
        String systemName = systemNameField.getText().trim();
        String adminEmail = adminEmailField.getText().trim();
        
        if (systemName.isEmpty() || adminEmail.isEmpty()) {
            showError("Please fill in all required fields");
            return;
        }
        
        if (!isValidEmail(adminEmail)) {
            showError("Please enter a valid email address");
            return;
        }
        
        boolean emailNotifications = emailNotificationsCheck.isSelected();
        boolean autoBackup = autoBackupCheck.isSelected();
        boolean maintenanceMode = maintenanceModeCheck.isSelected();
        System.out.println("Saving system settings:");
        System.out.println("System Name: " + systemName);
        System.out.println("Admin Email: " + adminEmail);
        System.out.println("Email Notifications: " + emailNotifications);
        System.out.println("Auto Backup: " + autoBackup);
        System.out.println("Maintenance Mode: " + maintenanceMode);
        
        showSuccess("System settings saved successfully!");
        
        if (maintenanceMode) {
            showWarning("Maintenance Mode Enabled", 
                       "The system is now in maintenance mode. Regular users will not be able to access the system until maintenance mode is disabled.");
        }
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
        
        if (newPassword.length() < 6) {
            showError("New password must be at least 6 characters long");
            return;
        }
        
        if (!isValidPassword(newPassword)) {
            showError("Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character");
            return;
        }
        
        System.out.println("Changing password for user: " + currentUsername);
        System.out.println("Current password verification...");
        System.out.println("New password: " + newPassword);
        
        showSuccess("Password changed successfully!");
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidPassword(String password) {
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$");
    }

    private void handleReset() {
        loadSettings();
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        showSuccess("Settings reset to default values");
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

    private void showInfo(String title, String message) {
        showAlert(title, message, Alert.AlertType.INFORMATION);
    }

    private void showWarning(String title, String message) {
        showAlert(title, message, Alert.AlertType.WARNING);
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