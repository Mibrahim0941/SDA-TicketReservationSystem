package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Admin;
import models.Route;
import models.Schedule;
import models.Seat;
import catalogs.RouteCatalog;
import config.DatabaseConfig;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ManageSeatsController {

    private RouteCatalog routeCatalog = RouteCatalog.getInstance();
    private String currentUsername;
    private Admin currentAdmin;
    private List<Seat> currentSeats = new ArrayList<>();

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
        initializeComboBoxes();
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

    private void initializeComboBoxes() {
        routeComboBox.setCellFactory(param -> new ListCell<Route>() {
            @Override
            protected void updateItem(Route item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getSource() + " → " + item.getDestination() + " (PKR " + item.getBasePrice() + ")");
                }
            }
        });

        routeComboBox.setButtonCell(new ListCell<Route>() {
            @Override
            protected void updateItem(Route item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getSource() + " → " + item.getDestination() + " (PKR " + item.getBasePrice() + ")");
                }
            }
        });

        scheduleComboBox.setCellFactory(param -> new ListCell<Schedule>() {
            @Override
            protected void updateItem(Schedule item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDate() + " - " + item.getDepartureTime() + " to " + item.getArrivalTime() + " (" + item.getScheduleClass() + ")");
                }
            }
        });

        scheduleComboBox.setButtonCell(new ListCell<Schedule>() {
            @Override
            protected void updateItem(Schedule item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDate() + " - " + item.getDepartureTime() + " to " + item.getArrivalTime() + " (" + item.getScheduleClass() + ")");
                }
            }
        });
    }

    private void loadRoutesData() {
        if (routeCatalog != null && routeComboBox != null) {
            try {
                routeCatalog.refresh();
                routeComboBox.getItems().setAll(routeCatalog.getAllRoutes());
                System.out.println("Loaded " + routeCatalog.getAllRoutes().size() + " routes");
            } catch (Exception e) {
                System.err.println("Error loading routes: " + e.getMessage());
                showError("Failed to load routes data: " + e.getMessage());
            }
        }
    }

    private void handleRouteSelection() {
        Route selectedRoute = routeComboBox.getValue();
        if (selectedRoute != null && scheduleComboBox != null) {
            try {
                Route currentRoute = routeCatalog.getRoute(selectedRoute.getRouteID());
                if (currentRoute != null) {
                    scheduleComboBox.getItems().setAll(currentRoute.getSchedules());
                    System.out.println("Loaded " + currentRoute.getSchedules().size() + " schedules for route " + currentRoute.getRouteID());
                }
                
                seatsGrid.getChildren().clear();
                currentSeats.clear();
                if (seatDetailsArea != null) {
                    seatDetailsArea.clear();
                }
            } catch (Exception e) {
                System.err.println("Error loading schedules: " + e.getMessage());
                showError("Failed to load schedules: " + e.getMessage());
            }
        }
    }

    private void handleScheduleSelection() {
        Schedule selectedSchedule = scheduleComboBox.getValue();
        if (selectedSchedule != null) {
            loadSeatsFromDatabase(selectedSchedule.getScheduleID());
        }
    }

    private void loadSeatsFromDatabase(String scheduleID) {
        currentSeats.clear();
        seatsGrid.getChildren().clear();
        
        String sql = "SELECT * FROM Seat WHERE ScheduleID = ? ORDER BY SeatNumber";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, scheduleID);
            ResultSet rs = pstmt.executeQuery();
            
            int row = 0;
            int col = 0;
            int totalSeats = 0;
            int availableSeats = 0;
            int occupiedSeats = 0;
            
            while (rs.next()) {
                String seatNumber = rs.getString("SeatNumber");
                String seatType = rs.getString("SeatType");
                double price = rs.getDouble("Price");
                boolean availability = rs.getBoolean("Availability");
                int seatID = rs.getInt("SeatID");
                
                Seat seat = new Seat(seatNumber, seatType, price);
                seat.setAvailability(availability);
                currentSeats.add(seat);
                
                Button seatButton = createSeatButton(seat, seatID, scheduleID);
                
                seatsGrid.add(seatButton, col, row);
                
                col++;
                if (col >= 4) {
                    col = 0;
                    row++;
                }
                
                totalSeats++;
                if (availability) {
                    availableSeats++;
                } else {
                    occupiedSeats++;
                }
            }
            
            updateStatistics(totalSeats, availableSeats, occupiedSeats);
            showSuccess("Loaded " + totalSeats + " seats for selected schedule");
            
        } catch (SQLException e) {
            System.err.println("Error loading seats from database: " + e.getMessage());
            showError("Failed to load seats: " + e.getMessage());
        }
    }

    private Button createSeatButton(Seat seat, int seatID, String scheduleID) {
        Button seatButton = new Button(seat.getSeatNo());
        
        String baseStyle = "-fx-font-weight: bold; -fx-font-size: 10px; -fx-min-width: 35px; -fx-min-height: 35px; " +
                          "-fx-border-radius: 5px; -fx-background-radius: 5px; -fx-cursor: hand; " +
                          "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 3, 0, 0, 1);";
        
        // Set initial color based on availability
        updateSeatButtonStyle(seatButton, seat.isAvailability());
        
        seatButton.setOnMouseEntered(e -> {
            if (seat.isAvailability()) {
                seatButton.setStyle(baseStyle + " -fx-background-color: #2ecc71; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 2);");
            } else {
                seatButton.setStyle(baseStyle + " -fx-background-color: #c0392b; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 2);");
            }
        });
        
        seatButton.setOnMouseExited(e -> {
            updateSeatButtonStyle(seatButton, seat.isAvailability());
        });
        
        seatButton.setOnAction(e -> toggleSeatStatus(seat, seatID, scheduleID, seatButton));
        
        return seatButton;
    }

    private void updateSeatButtonStyle(Button button, boolean isAvailable) {
        String baseStyle = "-fx-font-weight: bold; -fx-font-size: 10px; -fx-min-width: 35px; -fx-min-height: 35px; " +
                          "-fx-border-radius: 5px; -fx-background-radius: 5px; -fx-cursor: hand; " +
                          "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 3, 0, 0, 1);";
        
        if (isAvailable) {
            button.setStyle(baseStyle + " -fx-background-color: #27ae60; -fx-text-fill: white;");
        } else {
            button.setStyle(baseStyle + " -fx-background-color: #e74c3c; -fx-text-fill: white;");
        }
    }

    private void toggleSeatStatus(Seat seat, int seatID, String scheduleID, Button seatButton) {
        boolean newStatus = !seat.isAvailability();
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Change Seat Status");
        confirmation.setHeaderText("Change Seat Availability");
        confirmation.setContentText("Are you sure you want to change seat " + seat.getSeatNo() + " status to " + 
                                  (newStatus ? "AVAILABLE" : "OCCUPIED") + "?");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (updateSeatStatusInDatabase(seatID, newStatus)) {
                    seat.setAvailability(newStatus);
                    updateSeatButtonStyle(seatButton, newStatus);
                    refreshSeatStatistics();
                    showSuccess("Seat " + seat.getSeatNo() + " status changed to " + 
                               (newStatus ? "AVAILABLE" : "OCCUPIED"));
                } else {
                    showError("Failed to update seat status");
                }
            }
        });
    }

    private boolean updateSeatStatusInDatabase(int seatID, boolean newStatus) {
        String sql = "UPDATE Seat SET Availability = ? WHERE SeatID = ?";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBoolean(1, newStatus);
            pstmt.setInt(2, seatID);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating seat status: " + e.getMessage());
            return false;
        }
    }

    private void refreshSeatStatistics() {
        int totalSeats = currentSeats.size();
        int availableSeats = (int) currentSeats.stream().filter(Seat::isAvailability).count();
        int occupiedSeats = totalSeats - availableSeats;
        
        updateStatistics(totalSeats, availableSeats, occupiedSeats);
    }

    private void updateStatistics(int totalSeats, int availableSeats, int occupiedSeats) {
        double occupancyRate = totalSeats > 0 ? (double) occupiedSeats / totalSeats * 100 : 0;
        double availabilityRate = totalSeats > 0 ? (double) availableSeats / totalSeats * 100 : 0;
        
        if (seatDetailsArea != null) {
            StringBuilder stats = new StringBuilder();
            stats.append("=== SEAT MANAGEMENT ===\n\n");
            stats.append("📊 STATISTICS:\n");
            stats.append("Total Seats: ").append(totalSeats).append("\n");
            stats.append("Available Seats: ").append(availableSeats).append(" (").append(String.format("%.1f", availabilityRate)).append("%)\n");
            stats.append("Occupied Seats: ").append(occupiedSeats).append(" (").append(String.format("%.1f", occupancyRate)).append("%)\n\n");
            
            stats.append("🎯 INSTRUCTIONS:\n");
            stats.append("• Click on any seat to toggle its status\n");
            stats.append("• Green = Available\n");
            stats.append("• Red = Occupied\n");
            stats.append("• Confirmation required for status changes\n\n");
            
            stats.append("💡 LEGEND:\n");
            stats.append("🟢 Available - Can be booked\n");
            stats.append("🔴 Occupied - Cannot be booked\n");
            
            seatDetailsArea.setText(stats.toString());
        }
        
        System.out.println("Seat Statistics - Total: " + totalSeats + 
                          ", Available: " + availableSeats + 
                          ", Occupied: " + occupiedSeats + 
                          ", Occupancy: " + String.format("%.1f", occupancyRate) + "%");
    }

    private void handleRefresh() {
        Schedule selectedSchedule = scheduleComboBox.getValue();
        if (selectedSchedule != null) {
            loadSeatsFromDatabase(selectedSchedule.getScheduleID());
        } else {
            loadRoutesData();
            seatsGrid.getChildren().clear();
            if (seatDetailsArea != null) {
                seatDetailsArea.clear();
            }
            routeComboBox.setValue(null);
            scheduleComboBox.setValue(null);
            scheduleComboBox.getItems().clear();
        }
        showSuccess("Data refreshed successfully");
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