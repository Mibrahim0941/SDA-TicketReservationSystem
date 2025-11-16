package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Text welcomeTitle;
    @FXML private Text userGreeting;
    
    @FXML private VBox bookTicketsCard;
    @FXML private VBox myBookingsCard;
    @FXML private VBox profileCard;
    @FXML private VBox historyCard;
    @FXML private VBox supportCard;
    @FXML private VBox settingsCard;
    
    @FXML private Button bookNowButton;
    @FXML private Button logoutButton;
    @FXML private Button mainPageButton;
    
    @FXML private Label totalBookingsLabel;
    @FXML private Label upcomingTripsLabel;
    @FXML private Label pointsLabel;
    
    private String currentUsername;
    private Stage primaryStage;

    // ADD THIS SHOW METHOD
    public static void show(Stage stage, String username) {
        try {
            FXMLLoader loader = new FXMLLoader(DashboardController.class.getResource("/ui/dashboard.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set user data
            DashboardController controller = loader.getController();
            controller.setUserData(username, stage);
            
            Scene scene = new Scene(root, 900, 700);
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
        loadUserData();
    }
    
    public void setUserData(String username, Stage stage) {
        this.currentUsername = username;
        this.primaryStage = stage;
        updateUserGreeting();
    }
    
    private void updateUserGreeting() {
        if (currentUsername != null && !currentUsername.isEmpty()) {
            userGreeting.setText("Hello, " + currentUsername + "! 👋");
        }
    }
    
    private void setupEventHandlers() {
        // Card click handlers
        bookTicketsCard.setOnMouseClicked(e -> navigateToBookingPage());
        myBookingsCard.setOnMouseClicked(e -> navigateToMyBookings());
        profileCard.setOnMouseClicked(e -> navigateToProfile());
        historyCard.setOnMouseClicked(e -> navigateToHistory());
        supportCard.setOnMouseClicked(e -> navigateToSupport());
        settingsCard.setOnMouseClicked(e -> navigateToSettings());
        
        // Button handlers
        bookNowButton.setOnAction(e -> navigateToBookingPage());
        logoutButton.setOnAction(e -> handleLogout());
        mainPageButton.setOnAction(e -> navigateToMainPage());
        
        // Add hover effects to cards
        setupCardHoverEffects();
    }
    
    private void setupCardHoverEffects() {
        VBox[] cards = {bookTicketsCard, myBookingsCard, profileCard, historyCard, supportCard, settingsCard};
        
        for (VBox card : cards) {
            card.setOnMouseEntered(e -> {
                card.setStyle("-fx-background-color: #f0f9ff; -fx-border-color: #bae6fd;");
            });
            
            card.setOnMouseExited(e -> {
                card.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0;");
            });
        }
    }
    
    private void loadUserData() {
        // Simulate loading user data - replace with actual database calls
        totalBookingsLabel.setText("12");
        upcomingTripsLabel.setText("3");
        pointsLabel.setText("450");
    }
    
    private void navigateToBookingPage() {
        System.out.println("Navigating to Booking Page...");
        showAlert("Feature Coming Soon", "Booking feature will be available soon!");
    }
    
    private void navigateToMyBookings() {
        System.out.println("Navigating to My Bookings...");
        showAlert("Feature Coming Soon", "My Bookings feature will be available soon!");
    }
    
    private void navigateToProfile() {
        System.out.println("Navigating to Profile...");
        showAlert("Feature Coming Soon", "Profile management feature will be available soon!");
    }
    
    private void navigateToHistory() {
        System.out.println("Navigating to Booking History...");
        showAlert("Feature Coming Soon", "Booking history feature will be available soon!");
    }
    
    private void navigateToSupport() {
        System.out.println("Navigating to Support...");
        showAlert("Feature Coming Soon", "Customer support feature will be available soon!");
    }
    
    private void navigateToSettings() {
        System.out.println("Navigating to Settings...");
        showAlert("Feature Coming Soon", "Settings feature will be available soon!");
    }
    
    private void navigateToMainPage() {
        try {
            // Use the static show method from MainController
            MainController.show(primaryStage);
            
        } catch (Exception e) {
            System.err.println("Error navigating to main page: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleLogout() {
        try {
            System.out.println("Logging out user: " + currentUsername);
            
            // Use the static show method from LoginController
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

    // ADD THIS HELPER METHOD
    private static void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}