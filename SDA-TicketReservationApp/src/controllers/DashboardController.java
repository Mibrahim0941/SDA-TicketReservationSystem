package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Customer;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Text welcomeTitle;
    @FXML private Text userGreeting;
    
    // Sidebar elements
    @FXML private Label customerIdLabel;
    @FXML private Label memberSinceLabel;
    @FXML private Label statusLabel;
    @FXML private Label totalBookingsLabel;
    @FXML private Label upcomingTripsLabel;
    @FXML private Label pointsLabel;
    
    // Navigation buttons
    @FXML private Button homeButton;
    @FXML private Button bookTicketsButton;
    @FXML private Button myBookingsButton;
    @FXML private Button historyButton;
    @FXML private Button profileButton;
    @FXML private Button settingsButton;
    @FXML private Button supportButton;
    @FXML private Button logoutButton;
    
    // Content area
    @FXML private VBox contentArea;
    @FXML private VBox homeContent;
    @FXML private ScrollPane contentScrollPane;
    
    private String currentUsername;
    private Stage primaryStage;
    private Customer currentCustomer;

    public static void show(Stage stage, String username, Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(DashboardController.class.getResource("/ui/dashboard.fxml"));
            Parent root = loader.load();
            
            DashboardController controller = loader.getController();
            controller.setUserData(username, stage, customer);
            
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(DashboardController.class.getResource("/ui/dashboard.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Dashboard");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to load dashboard: " + e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DashboardController initialized");
        setupEventHandlers();
        
        // Configure scroll pane
        if (contentScrollPane != null) {
            contentScrollPane.setFitToWidth(true);
            contentScrollPane.setFitToHeight(true);
            contentScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            contentScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }
    }
    
    public void setUserData(String username, Stage stage, Customer customer) {
        this.currentUsername = username;
        this.primaryStage = stage;
        this.currentCustomer = customer;
        updateUserGreeting();
        loadCustomerData();
        showHome(); // Show home content by default
    }
    
    private void updateUserGreeting() {
        if (currentUsername != null && !currentUsername.isEmpty()) {
            userGreeting.setText("Hello, " + currentUsername + "! Ready to book your next journey? 🚌");
        }
    }
    
    private void loadCustomerData() {
        if (currentCustomer != null) {
            // Display ONLY username and email - nothing else
            String displayName = currentCustomer.getUsername() != null ? 
                currentCustomer.getUsername() : "User";
            welcomeTitle.setText("Welcome, " + displayName + "!");
            
            // Only set username and email - remove all other data
            customerIdLabel.setText("Username: " + displayName);
            
            if (currentCustomer.getEmail() != null) {
                memberSinceLabel.setText("Email: " + currentCustomer.getEmail());
            } else {
                memberSinceLabel.setText("Email: Not provided");
            }
            
            // Remove status and stats since you said NOTHING ELSE
            statusLabel.setText("");
            totalBookingsLabel.setText("");
            upcomingTripsLabel.setText("");
            pointsLabel.setText("");
        }
    }
    
    private void setupEventHandlers() {
        // Navigation button handlers
        homeButton.setOnAction(e -> showHome());
        bookTicketsButton.setOnAction(e -> showBookTickets());
        myBookingsButton.setOnAction(e -> showMyBookings());
        historyButton.setOnAction(e -> showHistory());
        profileButton.setOnAction(e -> showProfile());
        settingsButton.setOnAction(e -> showSettings());
        supportButton.setOnAction(e -> showSupport());
        logoutButton.setOnAction(e -> handleLogout());
    }
    
    @FXML
    private void showHome() {
        clearContentArea();
        contentArea.getChildren().add(homeContent);
        homeContent.setVisible(true);
        homeContent.setManaged(true);
        
        // Update home content with actual user data
        updateHomeContent();
    }
    
    private void updateHomeContent() {
        if (currentCustomer != null) {
            String displayName = currentCustomer.getUsername() != null ? 
                currentCustomer.getUsername() : "User";
            welcomeTitle.setText("Welcome Back, " + displayName + "!");
            userGreeting.setText("Hello, " + displayName + "! Ready to book your next journey? 🚌");
        }
    }
    
    @FXML
    private void showBookTickets() {
        clearContentArea();
        showAlert("Feature Coming Soon", "Book Tickets feature will be available soon!");
        showHome(); // Fall back to home
    }
    
    @FXML
    private void showMyBookings() {
        clearContentArea();
        try {
            if (currentCustomer == null) {
                showAlert("Error", "Customer information not available.");
                return;
            }
            
            // Load the FXML file directly into content area
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/myBookings.fxml"));
            Parent myBookingsContent = loader.load();
            
            // Pass data to controller if it exists
            Object controller = loader.getController();
            if (controller instanceof MyBookingsController) {
                ((MyBookingsController) controller).setUserData(currentUsername,currentCustomer);
            }
            
            contentArea.getChildren().add(myBookingsContent);
            System.out.println("My Bookings loaded successfully in content area!");
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load My Bookings: " + e.getMessage());
            showHome();
        }
    }
    
    @FXML
    private void showHistory() {
        clearContentArea();
        try {
            if (currentCustomer == null) {
                showAlert("Error", "Customer information not available.");
                return;
            }
            
            // Load the FXML file directly
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/booking-history.fxml"));
            Parent historyContent = loader.load();
            
            // Pass data to controller if it exists
            Object controller = loader.getController();
            if (controller instanceof BookingHistoryController) {
                ((BookingHistoryController) controller).setUserData(currentCustomer);
            }
            
            contentArea.getChildren().add(historyContent);
            System.out.println("Booking History loaded successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load Booking History: " + e.getMessage());
            showHome();
        }
    }
    
    @FXML
    private void showProfile() {
        clearContentArea();
        try {
            if (currentCustomer == null) {
                showAlert("Error", "Customer information not available.");
                return;
            }
            
            // Load the FXML file directly
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/update-profile.fxml"));
            Parent profileContent = loader.load();
            
            // Pass data to controller if it exists
            Object controller = loader.getController();
            if (controller instanceof UpdateProfileController) {
                ((UpdateProfileController) controller).setUserData(currentUsername, primaryStage, currentCustomer);
            }
            
            contentArea.getChildren().add(profileContent);
            System.out.println("Profile loaded successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load Profile: " + e.getMessage());
            showHome();
        }
    }
    
    @FXML
    private void showSupport() {
        clearContentArea();
        try {
            if (currentCustomer == null) {
                showAlert("Error", "Customer information not available.");
                return;
            }
            
            // Load the FXML file directly
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/customer-support.fxml"));
            Parent supportContent = loader.load();
            
            // Pass data to controller if it exists
            Object controller = loader.getController();
            if (controller instanceof CustomerSupportController) {
                ((CustomerSupportController) controller).setCustomerData(currentUsername, currentCustomer);
            }
            
            contentArea.getChildren().add(supportContent);
            System.out.println("Support loaded successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load Support: " + e.getMessage());
            showHome();
        }
    }
    
    @FXML
    private void showSettings() {
        clearContentArea();
        showAlert("Feature Coming Soon", "Settings feature will be available soon!");
        showHome(); // Fall back to home
    }
    
    private void clearContentArea() {
        contentArea.getChildren().clear();
    }
    
    @FXML
    private void handleLogout() {
        try {
            System.out.println("Logging out user: " + currentUsername);
            CustomerLoginController.show(primaryStage);
        } catch (Exception e) {
            System.err.println("Error during logout: " + e.getMessage());
            e.printStackTrace();
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