package controllers;

import javafx.application.Platform;
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
import models.Admin;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

import config.DatabaseConfig;

public class AdminDashboardController implements Initializable {

    @FXML private Text welcomeTitle;
    @FXML private Text userGreeting;
    
    @FXML private VBox manageRoutesCard;
    @FXML private VBox manageSchedulesCard;
    @FXML private VBox managePricingCard;
    @FXML private VBox manageSeatsCard;
    @FXML private VBox reportsCard;
    @FXML private VBox policiesCard;
    @FXML private VBox promoCodesCard;
    @FXML private VBox settingsCard;
    
    @FXML private Label totalRoutesLabel;
    @FXML private Label activeSchedulesLabel;
    @FXML private Label totalBookingsLabel;
    @FXML private Label revenueLabel;
    
    @FXML private Button logoutButton;
    @FXML private Button mainPageButton;
    @FXML private Button refreshButton;
    
    private String currentUsername;
    private Admin currentAdmin;
    private Stage primaryStage;

    public static void show(Stage stage, String username, Admin admin) {
        try {
            FXMLLoader loader = new FXMLLoader(AdminDashboardController.class.getResource("/ui/admin-dashboard.fxml"));
            Parent root = loader.load();
            
            AdminDashboardController controller = loader.getController();
            controller.setAdminData(username, stage, admin);
            
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(AdminDashboardController.class.getResource("/ui/admin-dashboard.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Admin Dashboard");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to load admin dashboard: " + e.getMessage());
        }
    }

    public void setAdminData(String username, Stage stage, Admin admin) {
        this.currentUsername = username;
        this.primaryStage = stage;
        this.currentAdmin = admin;
        
        if (userGreeting != null) {
            userGreeting.setText("Welcome, Administrator " + username + "! 👋");
        }
        
        Platform.runLater(this::loadStatistics);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("AdminDashboardController initialized");
        setupEventHandlers();
        setupCardHoverEffects();
    }

    private void setupEventHandlers() {
        if (manageRoutesCard != null) {
            manageRoutesCard.setOnMouseClicked(e -> navigateToRouteManagement());
        }
        if (manageSchedulesCard != null) {
            manageSchedulesCard.setOnMouseClicked(e -> navigateToScheduleManagement());
        }
        if (managePricingCard != null) {
            managePricingCard.setOnMouseClicked(e -> navigateToPricingManagement());
        }
        if (manageSeatsCard != null) {
            manageSeatsCard.setOnMouseClicked(e -> navigateToSeatManagement());
        }
        if (reportsCard != null) {
            reportsCard.setOnMouseClicked(e -> navigateToReports());
        }
        if (policiesCard != null) {
            policiesCard.setOnMouseClicked(e -> navigateToPolicies());
        }
        if (promoCodesCard != null) {
            promoCodesCard.setOnMouseClicked(e -> navigateToPromoCodes());
        }
        if (settingsCard != null) {
            settingsCard.setOnMouseClicked(e -> navigateToSettings());
        }
        
        if (logoutButton != null) {
            logoutButton.setOnAction(e -> handleLogout());
        }
        if (mainPageButton != null) {
            mainPageButton.setOnAction(e -> navigateToMainPage());
        }
        if (refreshButton != null) {
            refreshButton.setOnAction(e -> loadStatistics());
        }
    }

    private void setupCardHoverEffects() {
        VBox[] cards = {manageRoutesCard, manageSchedulesCard, managePricingCard, 
                        manageSeatsCard, reportsCard, policiesCard, promoCodesCard, settingsCard};
        
        for (VBox card : cards) {
            if (card != null) {
                card.setOnMouseEntered(e -> {
                    card.setStyle("-fx-background-color: #f0f9ff; -fx-border-color: #0ea5e9; -fx-border-radius: 8; -fx-background-radius: 8;");
                });
                
                card.setOnMouseExited(e -> {
                    card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8;");
                });
            }
        }
    }

    private void loadStatistics() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            catalogs.RouteCatalog routeCatalog = catalogs.RouteCatalog.getInstance();
            int totalRoutes = routeCatalog.getAllRoutes().size();
            if (totalRoutesLabel != null) totalRoutesLabel.setText(String.valueOf(totalRoutes));
            conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword()
            );
            String activeSchedulesQuery = 
                "SELECT COUNT(*) as active_schedules FROM Schedule " +
                "WHERE Date >= CAST(GETDATE() AS DATE) AND IsActive = 1";
            
            stmt = conn.prepareStatement(activeSchedulesQuery);
            rs = stmt.executeQuery();
            int activeSchedules = 0;
            if (rs.next()) {
                activeSchedules = rs.getInt("active_schedules");
            }
            if (activeSchedulesLabel != null) activeSchedulesLabel.setText(String.valueOf(activeSchedules));
            rs.close();
            stmt.close();
            String totalBookingsQuery = 
                "SELECT COUNT(*) as total_bookings FROM Booking " +
                "WHERE Status = 'Confirmed'";
            
            stmt = conn.prepareStatement(totalBookingsQuery);
            rs = stmt.executeQuery();
            int totalBookings = 0;
            if (rs.next()) {
                totalBookings = rs.getInt("total_bookings");
            }
            if (totalBookingsLabel != null) totalBookingsLabel.setText(formatNumber(totalBookings));
            rs.close();
            stmt.close();
            String revenueQuery = 
                "SELECT COALESCE(SUM(p.Amount), 0) as total_revenue " +
                "FROM Payment p " +
                "INNER JOIN Booking b ON p.PaymentID = b.PaymentID " +
                "WHERE p.PaymentStatus = 'Completed' AND b.Status = 'Confirmed'";
            
            stmt = conn.prepareStatement(revenueQuery);
            rs = stmt.executeQuery();
            double totalRevenue = 0;
            if (rs.next()) {
                totalRevenue = rs.getDouble("total_revenue");
            }
            if (revenueLabel != null) revenueLabel.setText("PKR " + formatCurrency(totalRevenue));
        } catch (SQLException e) {
            System.err.println("Database error loading statistics: " + e.getMessage());
            e.printStackTrace();
            catalogs.RouteCatalog routeCatalog = catalogs.RouteCatalog.getInstance();
            if (totalRoutesLabel != null) totalRoutesLabel.setText(String.valueOf(routeCatalog.getAllRoutes().size()));
            if (activeSchedulesLabel != null) activeSchedulesLabel.setText("0");
            if (totalBookingsLabel != null) totalBookingsLabel.setText("0");
            if (revenueLabel != null) revenueLabel.setText("PKR 0.00");
            
        } catch (Exception e) {
            System.err.println("Error loading statistics: " + e.getMessage());
            e.printStackTrace();
            if (totalRoutesLabel != null) totalRoutesLabel.setText("N/A");
            if (activeSchedulesLabel != null) activeSchedulesLabel.setText("N/A");
            if (totalBookingsLabel != null) totalBookingsLabel.setText("N/A");
            if (revenueLabel != null) revenueLabel.setText("PKR N/A");
            
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private String formatNumber(int number) {
        try {
            return String.format("%,d", number);
        } catch (Exception e) {
            return String.valueOf(number);
        }
    }

    private String formatCurrency(double amount) {
        try {
            return String.format("%,.2f", amount);
        } catch (Exception e) {
            return String.format("%.2f", amount);
        }
    }

    private void navigateToRouteManagement() {
        try {
            ManageRoutesController.show(primaryStage, currentUsername, currentAdmin);
        } catch (Exception e) {
            showAlert("Error", "Failed to load route management: " + e.getMessage());
        }
    }

    private void navigateToScheduleManagement() {
        try {
            ManageSchedulesController.show(primaryStage, currentUsername, currentAdmin);
        } catch (Exception e) {
            showAlert("Error", "Failed to load schedule management: " + e.getMessage());
        }
    }

    private void navigateToPricingManagement() {
        try {
            ManagePricingController.show(primaryStage, currentUsername, currentAdmin);
        } catch (Exception e) {
            showAlert("Error", "Failed to load pricing management: " + e.getMessage());
        }
    }

    private void navigateToSeatManagement() {
        try {
            ManageSeatsController.show(primaryStage, currentUsername, currentAdmin);
        } catch (Exception e) {
            showAlert("Error", "Failed to load seat management: " + e.getMessage());
        }
    }

    private void navigateToReports() {
        try {
            AdminReportsController.show(primaryStage, currentUsername, currentAdmin);
        } catch (Exception e) {
            showAlert("Error", "Failed to load reports: " + e.getMessage());
        }
    }

    private void navigateToPolicies() {
        try {
            ManagePoliciesController.show(primaryStage, currentUsername, currentAdmin);
        } catch (Exception e) {
            showAlert("Error", "Failed to load policies management: " + e.getMessage());
        }
    }

    private void navigateToPromoCodes() {
    try {
        ManagePromoCodesController.show(primaryStage, currentUsername, currentAdmin);
    } catch (Exception e) {
        showAlert("Error", "Failed to load promo codes management: " + e.getMessage());
    }
}

    private void navigateToSettings() {
        try {
            AdminSettingsController.show(primaryStage, currentUsername, currentAdmin);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load settings: " + e.getMessage());
        }
    }

    private void handleLogout() {
        try {
            AdminLoginController.show(primaryStage);
        } catch (Exception e) {
            showAlert("Error", "Failed to logout: " + e.getMessage());
        }
    }

    private void navigateToMainPage() {
        try {
            MainController.show(primaryStage);
        } catch (Exception e) {
            showAlert("Error", "Failed to load main page: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
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