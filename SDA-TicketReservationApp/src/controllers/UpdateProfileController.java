package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Customer;
import catalogs.UserCatalog;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class UpdateProfileController implements Initializable {

    private UserCatalog userCatalog = new UserCatalog("Customer");
    
    @FXML private Text pageTitle;
    @FXML private Text userGreeting;
    
    @FXML private TextField nameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField currentPasswordField;
    @FXML private TextField newPasswordField;
    @FXML private TextField confirmPasswordField;
    
    @FXML private Button updateProfileButton;
    @FXML private Button changePasswordButton;
    @FXML private Button backButton;
    
    private String currentUsername;
    private Stage primaryStage;
    private Customer currentCustomer;
    public static void show(Stage stage, String username, Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(UpdateProfileController.class.getResource("/ui/update-profile.fxml"));
            Parent root = loader.load();
            
            UpdateProfileController controller = loader.getController();
            controller.setUserData(username, stage, customer);
            
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(UpdateProfileController.class.getResource("/ui/update-profile.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Update Profile");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to load update profile page: " + e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("UpdateProfileController initialized");
        setupEventHandlers();
    }
    
    public void setUserData(String username, Stage stage, Customer customer) {
        this.currentUsername = username;
        this.primaryStage = stage;
        this.currentCustomer = customer;
        
        if (userGreeting != null) {
            userGreeting.setText("Hello, " + username + "! 👋");
        }
        
        loadUserData();
    }
    
    private void setupEventHandlers() {
        updateProfileButton.setOnAction(e -> updateProfile());
        changePasswordButton.setOnAction(e -> changePassword());
        backButton.setOnAction(e -> goBackToDashboard());
    }
    
    private void loadUserData() {
        if (currentCustomer != null) {
            nameField.setText(currentCustomer.getName());
            usernameField.setText(currentCustomer.getUsername());
            emailField.setText(currentCustomer.getEmail());
            phoneField.setText(currentCustomer.getPhoneNum());
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        }
    }
    
    private void updateProfile() {
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        if (name.isEmpty() || username.isEmpty() || email.isEmpty()) {
            showAlert("Error", "Please fill in all required fields (Name, Username, Email)");
            return;
        }
        
        if (!isValidEmail(email)) {
            showAlert("Error", "Please enter a valid email address");
            return;
        }
        
        if (!currentCustomer.getUsername().equals(username)) {
            if (userCatalog.isUsernameTaken(username)) {
                showAlert("Error", "Username is already taken. Please choose a different username.");
                return;
            }
        }
        
        if (!currentCustomer.getEmail().equals(email)) {
            if (userCatalog.isEmailTaken(email)) {
                showAlert("Error", "Email is already taken. Please use a different email address.");
                return;
            }
        }
        
        try {
            currentCustomer.setName(name);
            currentCustomer.setUsername(username);
            currentCustomer.setEmail(email);
            currentCustomer.setPhoneNum(phone);
            boolean success = updateUserInCatalog();
            
            if (success) {
                showAlert("Success", "Profile updated successfully!");
                this.currentUsername = username;
                userGreeting.setText("Hello, " + username + "! 👋");
            } else {
                showAlert("Error", "Failed to update profile. Please try again.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error updating profile: " + e.getMessage());
        }
    }
    
    private void changePassword() {
        String currentPassword = currentPasswordField.getText().trim();
        String newPassword = newPasswordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showAlert("Error", "Please fill in all password fields");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showAlert("Error", "New passwords do not match");
            return;
        }
        
        if (newPassword.length() < 6) {
            showAlert("Error", "New password must be at least 6 characters long");
            return;
        }
        if (!currentCustomer.getPassword().equals(currentPassword)) {
            showAlert("Error", "Current password is incorrect");
            return;
        }
        
        try {
            boolean success = userCatalog.updateUserPassword(currentCustomer.getUserID(), newPassword);
        
            if (success) {
                currentCustomer.setPassword(newPassword);
                showAlert("Success", "Password changed successfully!");
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();
            } else {
                showAlert("Error", "Failed to change password. Please try again.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error changing password: " + e.getMessage());
        }
    }
    
    private boolean updateUserInCatalog() {
        return userCatalog.updateUser(currentCustomer);
    }
    
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
    
    private void goBackToDashboard() {
        try {
            Stage currentStage = (Stage) backButton.getScene().getWindow();
            DashboardController.show(currentStage, currentUsername, currentCustomer);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to go back to dashboard: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private static void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}