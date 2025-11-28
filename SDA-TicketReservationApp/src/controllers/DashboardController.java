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
    
    // REMOVED: statusLabel, totalBookingsLabel, etc. are no longer needed
    
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
            // Display ONLY username and email
            String displayName = currentCustomer.getUsername() != null ? 
                currentCustomer.getUsername() : "User";
            welcomeTitle.setText("Welcome, " + displayName + "!");
            
            // Set username and email sidebar labels
            customerIdLabel.setText("Username: " + displayName);
            
            if (currentCustomer.getEmail() != null) {
                memberSinceLabel.setText("Email: " + currentCustomer.getEmail());
            } else {
                memberSinceLabel.setText("Email: Not provided");
            }
            
            // CRITICAL FIX: Removed code that tries to set text on deleted labels (statusLabel, etc.)
        }
    }
    
    private void setupEventHandlers() {
        // Navigation button handlers
        if(homeButton != null) homeButton.setOnAction(e -> showHome());
        if(bookTicketsButton != null) bookTicketsButton.setOnAction(e -> showBookTickets());
        if(myBookingsButton != null) myBookingsButton.setOnAction(e -> showMyBookings());
        if(historyButton != null) historyButton.setOnAction(e -> showHistory());
        if(profileButton != null) profileButton.setOnAction(e -> showProfile());
        if(settingsButton != null) settingsButton.setOnAction(e -> showSettings());
        if(supportButton != null) supportButton.setOnAction(e -> showSupport());
        if(logoutButton != null) logoutButton.setOnAction(e -> handleLogout());
    }
    
    @FXML
    private void showHome() {
        clearContentArea();
        contentArea.getChildren().add(homeContent);
        homeContent.setVisible(true);
        homeContent.setManaged(true);
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
        try {
            if (currentCustomer == null) {
                showAlert("Error", "Customer information not available.");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/bookTickets.fxml"));
            Parent bookTicketsContent = loader.load();
            
            // Note: Ensure BookTicketsController class exists and has setUserData
            // If you don't have this controller yet, remove this block
             Object controller = loader.getController();
             // Reflection check to avoid crash if class missing
             try {
                 java.lang.reflect.Method method = controller.getClass().getMethod("setUserData", String.class, Customer.class);
                 method.invoke(controller, currentUsername, currentCustomer);
             } catch (Exception ex) {
                 System.out.println("Controller does not have setUserData method or class mismatch: " + ex.getMessage());
             }
            
            contentArea.getChildren().add(bookTicketsContent);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Feature Unavailable", "Book Tickets page is not ready yet.");
            showHome();
        }
    }
    
    @FXML
    private void showMyBookings() {
        clearContentArea();
        try {
            if (currentCustomer == null) {
                showAlert("Error", "Customer information not available.");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/myBookings.fxml"));
            Parent myBookingsContent = loader.load();
            
            Object controller = loader.getController();
            if (controller instanceof MyBookingsController) {
                ((MyBookingsController) controller).setUserData(currentUsername, currentCustomer);
            }
            
            contentArea.getChildren().add(myBookingsContent);
            
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
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/booking-history.fxml"));
            Parent historyContent = loader.load();
            
            Object controller = loader.getController();
            if (controller instanceof BookingHistoryController) {
                ((BookingHistoryController) controller).setUserData(currentUsername, currentCustomer);
            }
            
            contentArea.getChildren().add(historyContent);
            
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
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/update-profile.fxml"));
            Parent profileContent = loader.load();
            
            Object controller = loader.getController();
            // Using reflection or strict casting depending on your exact Controller class name
            // Assuming class name is UpdateProfileController
             if (controller instanceof UpdateProfileController) {
                 ((UpdateProfileController) controller).setUserData(currentUsername, primaryStage, currentCustomer);
             }
            
            contentArea.getChildren().add(profileContent);
            
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
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/customer-support.fxml"));
            Parent supportContent = loader.load();
            
            Object controller = loader.getController();
            // Assuming class name is CustomerSupportController
             try {
                 java.lang.reflect.Method method = controller.getClass().getMethod("setCustomerData", String.class, Customer.class);
                 method.invoke(controller, currentUsername, currentCustomer);
             } catch (Exception ex) {
                 // Ignore if method missing
             }
            
            contentArea.getChildren().add(supportContent);
            
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
        showHome();
    }
    
    private void clearContentArea() {
        contentArea.getChildren().clear();
    }
    
    @FXML
    private void handleLogout() {
        try {
            System.out.println("Logging out user: " + currentUsername);
            // Assuming CustomerLoginController exists and has a show method
            // If class name is different, adjust here.
            Class<?> loginControllerClass = Class.forName("controllers.CustomerLoginController");
            java.lang.reflect.Method showMethod = loginControllerClass.getMethod("show", Stage.class);
            showMethod.invoke(null, primaryStage);
            
        } catch (Exception e) {
            System.err.println("Error during logout: " + e.getMessage());
            e.printStackTrace();
            // Fallback if reflection fails
            try {
                 FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/customer_login.fxml"));
                 Parent root = loader.load();
                 primaryStage.setScene(new Scene(root));
                 primaryStage.show();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
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