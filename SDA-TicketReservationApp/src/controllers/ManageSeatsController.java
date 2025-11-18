package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Admin;
import models.Route;
import models.Schedule;
import models.Seat;
import catalogs.RouteCatalog;

import java.io.IOException;

public class ManageSeatsController {

    private RouteCatalog routeCatalog = new RouteCatalog();
    private String currentUsername;
    private Admin currentAdmin;

    @FXML private Text welcomeTitle;
    @FXML private Text userGreeting;
    
    @FXML private ComboBox<Route> routeComboBox;
    @FXML private ComboBox<Schedule> scheduleComboBox;
    @FXML private GridPane seatsGrid;
    @FXML private TextArea seatDetailsArea;
    
    @FXML private Button refreshButton;
    @FXML private Button backButton;

    public static void show(Stage stage, String username, Admin admin) {
        try {
            FXMLLoader loader = new FXMLLoader(ManageSeatsController.class.getResource("/ui/manage-seats.fxml"));
            Parent root = loader.load();
            
            ManageSeatsController controller = loader.getController();
            controller.setAdminData(username, admin);
            
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(ManageSeatsController.class.getResource("/ui/manage-seats.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Manage Seats");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to load seat management page: " + e.getMessage());
        }
    }

    public void setAdminData(String username, Admin admin) {
        this.currentUsername = username;
        this.currentAdmin = admin;
        
        if (userGreeting != null) {
            userGreeting.setText("Manage Seats - " + username);
        }
        
        Platform.runLater(this::loadRoutesData);
    }

    @FXML
    public void initialize() {
        System.out.println("ManageSeatsController initialized");
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        if (routeComboBox != null) {
            routeComboBox.setOnAction(e -> handleRouteSelection());
        }
        if (scheduleComboBox != null) {
            scheduleComboBox.setOnAction(e -> handleScheduleSelection());
        }
        if (refreshButton != null) {
            refreshButton.setOnAction(e -> handleRefresh());
        }
        if (backButton != null) {
            backButton.setOnAction(e -> handleBack());
        }
    }

    private void loadRoutesData() {
        if (routeCatalog != null && routeComboBox != null) {
            routeCatalog.refresh();
            routeComboBox.getItems().setAll(routeCatalog.getAllRoutes());
        }
    }

    private void handleRouteSelection() {
        Route selectedRoute = routeComboBox.getValue();
        if (selectedRoute != null && scheduleComboBox != null) {
            scheduleComboBox.getItems().setAll(selectedRoute.getSchedules());
        }
    }

    private void handleScheduleSelection() {
        Schedule selectedSchedule = scheduleComboBox.getValue();
        if (selectedSchedule != null) {
            displaySeats(selectedSchedule);
        }
    }

    private void displaySeats(Schedule schedule) {
        seatsGrid.getChildren().clear();
        
        // Create sample seats for demonstration
        for (int i = 0; i < 20; i++) {
            Seat seat = new Seat("A" + (i + 1), "Economy", 1000 + (i * 50));
            seat.setAvailability(Math.random() > 0.3); // 70% available
            
            Button seatButton = new Button(seat.getSeatNo());
            seatButton.getStyleClass().add("seat-button");
            if (!seat.isAvailability()) {
                seatButton.getStyleClass().add("seat-occupied");
            }
            
            final int seatIndex = i;
            seatButton.setOnAction(e -> showSeatDetails(seat));
            
            seatsGrid.add(seatButton, i % 5, i / 5);
        }
    }

    private void showSeatDetails(Seat seat) {
        if (seatDetailsArea != null) {
            StringBuilder details = new StringBuilder();
            details.append("Seat Number: ").append(seat.getSeatNo()).append("\n");
            details.append("Seat Type: ").append(seat.getSeatType()).append("\n");
            details.append("Price: PKR ").append(seat.getPrice()).append("\n");
            details.append("Status: ").append(seat.isAvailability() ? "Available" : "Occupied").append("\n");
            
            seatDetailsArea.setText(details.toString());
        }
    }

    private void handleRefresh() {
        loadRoutesData();
        showSuccess("Data refreshed");
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