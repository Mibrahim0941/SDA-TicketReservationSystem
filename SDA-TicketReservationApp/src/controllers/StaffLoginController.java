package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.net.URL;
import java.util.ResourceBundle;

import catalogs.UserCatalog;

public class StaffLoginController implements Initializable {

    private UserCatalog staff = new UserCatalog();

    @FXML private Text staffTitle;
    @FXML private Text staffSubtitle;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Text statusMessage;
    @FXML private Button loginButton;
    @FXML private Button backButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("StaffLoginController initialized");
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        // Login button handler
        loginButton.setOnAction(e -> handleStaffLogin());
        
        // Back button handler
        backButton.setOnAction(e -> handleBackToLoginTypes());
        
        // Enter key support for password field
        passwordField.setOnAction(e -> handleStaffLogin());
    }

    @FXML
    private void handleStaffLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showStatusMessage("Please enter both username and password", "error");
        } else if (staff.authenticateUser(username, password)) {
            showStatusMessage("Staff login successful! Redirecting...", "success");
            
            // Redirect to staff dashboard after short delay
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                javafx.application.Platform.runLater(() -> {
                    showStaffDashboard(username);
                });
            }).start();
        } else {
            showStatusMessage("Invalid staff credentials", "error");
        }
    }

    @FXML
    private void handleBackToLoginTypes() {
        try {
            Stage currentStage = (Stage) backButton.getScene().getWindow();
            MainController.showoptions(currentStage);
        } catch (Exception e) {
            System.err.println("Error navigating back to login types: " + e.getMessage());
            e.printStackTrace();
            showAlert("Navigation Error", "Failed to load login types page");
        }
    }

    private void showStaffDashboard(String username) {
        try {
            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            DashboardController.show(currentStage, username);
        } catch (Exception e) {
            System.err.println("Error loading staff dashboard: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to load staff dashboard");
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

    // Static show method for navigation
    public static void show(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(StaffLoginController.class.getResource("/ui/stafflogin.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 900,700);
            scene.getStylesheets().add(StaffLoginController.class.getResource("/ui/stafflogin.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Staff Login");
            stage.centerOnScreen();
            
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Failed to load staff login page: " + e.getMessage());
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