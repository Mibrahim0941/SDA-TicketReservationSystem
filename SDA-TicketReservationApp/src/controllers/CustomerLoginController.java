package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.User;
import models.Customer;
import helpers.IDGenerator;
import catalogs.UserCatalog;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class CustomerLoginController {

    private UserCatalog users = new UserCatalog("Customer");
    
    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    // Login Form Elements (from login.fxml)
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button clearButton;
    @FXML private Button registerButton;
    @FXML private Text statusMessage;

    // Register Form Elements (from register.fxml - will be null in login.fxml)
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneNumField;
    @FXML private TextField registerUsernameField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField confirmPasswordField;
    
    @FXML private Button createAccountButton;
    @FXML private Button clearRegisterButton;
    @FXML private Button backToLoginButton;

    // ADD THIS SHOW METHOD
    public static void show(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(CustomerLoginController.class.getResource("/ui/customerlogin.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 900, 700);
            scene.getStylesheets().add(CustomerLoginController.class.getResource("/ui/style.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Login");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to load login page: " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {
        System.out.println("LoginController initialized");
    }

    // Login functionality
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
        } else if (authenticateUser(username, password)) {
            showSuccess("Login successful! Redirecting...");
            
            // Get the logged-in user
            User loggedInUser = users.getUserByUsername(username);
            Customer loggedInCustomer = (Customer)loggedInUser;
            
            // Simulate brief delay before redirect
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                Platform.runLater(() -> showDashboard(loggedInUser.getUsername(),loggedInCustomer));
            }).start();
        } else {
            showError("Invalid username or password");
        }
    }

    // ADD THIS METHOD TO SHOW DASHBOARD
    private void showDashboard(String username,Customer loggedInUser) {
        try {
            Stage currentStage = (Stage) usernameField.getScene().getWindow();
            DashboardController.show(currentStage, username, loggedInUser);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load dashboard");
        }
    }

    @FXML
    private void handleClear() {
        if (usernameField != null) usernameField.clear();
        if (passwordField != null) passwordField.clear();
        if (statusMessage != null) statusMessage.setText("");
    }

    // Register navigation - goes to register.fxml
    @FXML
    private void handleRegister() {
        try {
            System.out.println("Attempting to load register.fxml...");
            
            Stage currentStage = (Stage) usernameField.getScene().getWindow();
            
            // Load the register FXML with a NEW controller instance
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/customerregister.fxml"));
            
            Parent root = loader.load();
            
            // Create new scene
            Scene scene = new Scene(root, 900, 700);
            currentStage.setResizable(true);
            // Load CSS
            try {
                String cssPath = getClass().getResource("/ui/style.css").toExternalForm();
                scene.getStylesheets().add(cssPath);
                System.out.println("CSS loaded successfully");
            } catch (Exception cssEx) {
                System.err.println("Failed to load CSS: " + cssEx.getMessage());
            }
            
            // Set the stage
            currentStage.setScene(scene);
            currentStage.setTitle("TicketGenie - Register");
            currentStage.centerOnScreen();
            
            System.out.println("Register page loaded successfully");
            
        } catch (Exception e) {
            System.err.println("Error loading register page: " + e.getMessage());
            e.printStackTrace();
            showAlert("Navigation Error", "Failed to load registration page: " + e.getMessage());
        }
    }

    // Register functionality - called from register.fxml
    @FXML
    private void handleCreateAccount() {
        System.out.println("handleCreateAccount called");
        
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String username = registerUsernameField.getText().trim();
        String password = registerPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String phoneNum = phoneNumField.getText().trim();

        // Validation
        if (fullName.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phoneNum.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        if (username.length() < 3) {
            showError("Username must be at least 3 characters long");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters long");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        if (isUsernameTaken(username)) {
            showError("Username already exists. Please choose a different one.");
            return;
        }

        // Register new user
        String UserID = IDGenerator.generateCustomerID();
        User newUser = new Customer(UserID, fullName, password, username, email, phoneNum);
        users.addToCatalog(newUser);
        System.out.println(users.getUsers() + " users in catalog after registration.");
        showSuccess("Registration successful! Redirecting to login...");
        
        // Redirect to login after short delay
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(this::handleBackToLogin);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleClearRegister() {
        if (fullNameField != null) fullNameField.clear();
        if (emailField != null) emailField.clear();
        if (registerUsernameField != null) registerUsernameField.clear();
        if (registerPasswordField != null) registerPasswordField.clear();
        if (confirmPasswordField != null) confirmPasswordField.clear();
        if (statusMessage != null) statusMessage.setText("");
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Stage currentStage;
            
            // Get the current stage from any available component
            if (fullNameField != null) {
                currentStage = (Stage) fullNameField.getScene().getWindow();
            } else if (usernameField != null) {
                currentStage = (Stage) usernameField.getScene().getWindow();
            } else {
                showError("Cannot navigate to login");
                return;
            }
            
            // Use the static show method
            show(currentStage);
            
        } catch (Exception e) {
            System.err.println("Error loading login page: " + e.getMessage());
            e.printStackTrace();
            showAlert("Navigation Error", "Failed to load login page: " + e.getMessage());
        }
    }

    // Helper methods
    private boolean authenticateUser(String username, String password) {
        return users.authenticateUser(username, password);
    }

    private boolean isUsernameTaken(String username) {
        return users.isUsernameTaken(username);
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private void showError(String message) {
        if (statusMessage != null) {
            statusMessage.getStyleClass().setAll("error-text");
            statusMessage.setText(message);
        } else {
            // Fallback: show alert if statusMessage is not available
            showAlert("Error", message);
        }
    }

    private void showSuccess(String message) {
        if (statusMessage != null) {
            statusMessage.getStyleClass().setAll("success-text");
            statusMessage.setText(message);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ADD THIS HELPER METHOD
    private static void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Method to get users (for testing/demonstration)
    public List<User> getUsers() {
        return users.getUsers();
    }

    @FXML
    private void handleBackToMain() {
        try {
            Stage currentStage;
            
            // Get the current stage from any available component
            if (fullNameField != null) {
                currentStage = (Stage) fullNameField.getScene().getWindow();
            } else if (usernameField != null) {
                currentStage = (Stage) usernameField.getScene().getWindow();
            } else {
                showAlert("Error", "Cannot navigate to main page");
                return;
            }
            
            // Use the static show method from MainController
            MainController.show(currentStage);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load main page: " + e.getMessage());
        }
    }
}