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
import java.util.ResourceBundle;

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

    private void loadStatistics() 
    {
        try {
            catalogs.RouteCatalog routeCatalog = catalogs.RouteCatalog.getInstance();
            catalogs.PromoCodeCatalog promoCatalog = new catalogs.PromoCodeCatalog();
            catalogs.PolicyCatalog policyCatalog = new catalogs.PolicyCatalog();
            
            if (totalRoutesLabel != null) totalRoutesLabel.setText(String.valueOf(routeCatalog.getAllRoutes().size()));
            if (activeSchedulesLabel != null) activeSchedulesLabel.setText("48");
            if (totalBookingsLabel != null) totalBookingsLabel.setText("1,234"); 
            if (revenueLabel != null) revenueLabel.setText("PKR 45,678"); s
            
        } catch (Exception e) {
            System.err.println("Error loading statistics: " + e.getMessage());
            if (totalRoutesLabel != null) totalRoutesLabel.setText("25");
            if (activeSchedulesLabel != null) activeSchedulesLabel.setText("48");
            if (totalBookingsLabel != null) totalBookingsLabel.setText("1,234");
            if (revenueLabel != null) revenueLabel.setText("PKR 45,678");
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