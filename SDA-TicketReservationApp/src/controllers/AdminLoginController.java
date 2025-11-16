package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.net.URL;
import java.util.ResourceBundle;

import catalogs.UserCatalog;

public class AdminLoginController implements Initializable {

    UserCatalog admin = new UserCatalog("Admin");

    @FXML private Text adminTitle;
    @FXML private Text adminSubtitle;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Text statusMessage;
    @FXML private Button loginButton;
    @FXML private Button backButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("AdminLoginController initialized");
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        // Login button handler
        loginButton.setOnAction(e -> handleLogin());
        
        // Back button handler
        backButton.setOnAction(e -> handleBack());
        
        // Enter key support for password field
        passwordField.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showStatusMessage("Please enter both username and password", "error");
        } else if (admin.authenticateUser(username, password)) {
            showStatusMessage("Admin login successful! Redirecting...", "success");
            
            // Redirect to admin dashboard after short delay
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                javafx.application.Platform.runLater(() -> {
                    showAdminDashboard(username);
                });
            }).start();
        } else {
            showStatusMessage("Invalid admin credentials", "error");
        }
    }

    @FXML
    private void handleBack() {
        try {
            Stage currentStage = (Stage) backButton.getScene().getWindow();
            MainController.show(currentStage);
        } catch (Exception e) {
            System.err.println("Error navigating back to login types: " + e.getMessage());
            e.printStackTrace();
            showAlert("Navigation Error", "Failed to load login types page");
        }
    }

    private void showAdminDashboard(String username) {
        try {
            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            DashboardController.show(currentStage, username, null); // Pass null for customer as it's admin
            
            // You can create a separate AdminDashboardController if needed
            // AdminDashboardController.show(currentStage, username);
            
        } catch (Exception e) {
            System.err.println("Error loading admin dashboard: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to load admin dashboard");
        }
    }

    private void showStatusMessage(String message, String type) {
        statusMessage.getStyleClass().clear();
        
        switch (type.toLowerCase()) {
            case "success":
                statusMessage.getStyleClass().add("success-text");
                break;
            case "error":
                statusMessage.getStyleClass().add("error-text");
                break;
            case "warning":
                statusMessage.getStyleClass().add("warning-text");
                break;
            default:
                statusMessage.getStyleClass().add("info-text");
                break;
        }
        
        statusMessage.setText(message);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Static show method for navigation from other controllers
    public static void show(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(AdminLoginController.class.getResource("/ui/adminlogin.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 900, 700);
            scene.getStylesheets().add(AdminLoginController.class.getResource("/ui/adminlogin.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Admin Login");
            stage.centerOnScreen();
            
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Failed to load admin login page: " + e.getMessage());
        }
    }

    private static void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}