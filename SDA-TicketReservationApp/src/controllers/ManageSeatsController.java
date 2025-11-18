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

import java.io.IOException;

public class ManageSeatsController {

    private RouteCatalog routeCatalog = RouteCatalog.getInstance();
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
                    setText(item.getDate() + " - " + item.getDepartureTime() + " to " + item.getArrivalTime());
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
                    setText(item.getDate() + " - " + item.getDepartureTime() + " to " + item.getArrivalTime());
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
            displaySeats(selectedSchedule);
        }
    }

    private void displaySeats(Schedule schedule) {
        seatsGrid.getChildren().clear();
        
        int totalSeats = 40;
        int availableSeats = 0;
        int occupiedSeats = 0;
        
        for (int i = 0; i < totalSeats; i++) {
            String seatNumber;
            String seatType;
            double price;
            
            if (i < 8) {
                seatNumber = "A" + (i + 1);
                seatType = "Business";
                price = 2000 + (i * 50);
            } else if (i < 24) {
                seatNumber = "B" + (i - 7);
                seatType = "Economy";
                price = 1000 + (i * 30);
            } else {
                seatNumber = "C" + (i - 23);
                seatType = "Standard";
                price = 800 + (i * 20);
            }
            
            Seat seat = new Seat(seatNumber, seatType, price);
            boolean isAvailable = Math.random() > 0.3;
            
            if (isAvailable) {
                availableSeats++;
            } else {
                occupiedSeats++;
            }
            
            Button seatButton = createSeatButton(seatNumber, isAvailable, seat);
            
            int row = i / 4;
            int col = i % 4;
            seatsGrid.add(seatButton, col, row);
        }
        
        updateStatistics(totalSeats, availableSeats, occupiedSeats);
        showSuccess("Displaying " + totalSeats + " seats for selected schedule");
    }

    private Button createSeatButton(String seatNumber, boolean isAvailable, Seat seat) {
        Button seatButton = new Button(seatNumber);
        
        String baseStyle = "-fx-font-weight: bold; -fx-font-size: 10px; -fx-min-width: 35px; -fx-min-height: 35px; " +
                          "-fx-border-radius: 5px; -fx-background-radius: 5px; -fx-cursor: hand; " +
                          "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 3, 0, 0, 1);";
        
        if (isAvailable) {
            seatButton.setStyle(baseStyle + " -fx-background-color: #27ae60; -fx-text-fill: white;");
        } else {
            seatButton.setStyle(baseStyle + " -fx-background-color: #e74c3c; -fx-text-fill: white;");
        }
        
        seatButton.setOnMouseEntered(e -> {
            if (isAvailable) {
                seatButton.setStyle(baseStyle + " -fx-background-color: #2ecc71; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 2);");
            } else {
                seatButton.setStyle(baseStyle + " -fx-background-color: #c0392b; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 2);");
            }
        });
        
        seatButton.setOnMouseExited(e -> {
            if (isAvailable) {
                seatButton.setStyle(baseStyle + " -fx-background-color: #27ae60; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 3, 0, 0, 1);");
            } else {
                seatButton.setStyle(baseStyle + " -fx-background-color: #e74c3c; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 3, 0, 0, 1);");
            }
        });
        
        seatButton.setOnAction(e -> showSeatDetails(seat));
        
        return seatButton;
    }

    private void updateStatistics(int totalSeats, int availableSeats, int occupiedSeats) {
        double occupancyRate = (double) occupiedSeats / totalSeats * 100;
        System.out.println("Seat Statistics - Total: " + totalSeats + 
                          ", Available: " + availableSeats + 
                          ", Occupied: " + occupiedSeats + 
                          ", Occupancy: " + String.format("%.1f", occupancyRate) + "%");
    }

    private void showSeatDetails(Seat seat) {
        if (seatDetailsArea != null) {
            StringBuilder details = new StringBuilder();
            details.append("=== SEAT DETAILS ===\n\n");
            details.append("Seat Number: ").append(seat.getSeatNo()).append("\n");
            details.append("Seat Type: ").append(seat.getSeatType()).append("\n");
            details.append("Price: PKR ").append(String.format("%.2f", seat.getPrice())).append("\n");
            details.append("Status: ").append(seat.isAvailability() ? "✅ Available" : "❌ Occupied").append("\n");
            details.append("\n");
            
            if (seat.isAvailability()) {
                details.append("This seat is available for booking.\n");
                details.append("Passengers can select this seat during booking.");
            } else {
                details.append("This seat is currently occupied.\n");
                details.append("It cannot be selected for new bookings.");
            }
            
            seatDetailsArea.setText(details.toString());
        }
    }

    private void handleRefresh() {
        loadRoutesData();
        seatsGrid.getChildren().clear();
        if (seatDetailsArea != null) {
            seatDetailsArea.clear();
        }
        routeComboBox.setValue(null);
        scheduleComboBox.setValue(null);
        scheduleComboBox.getItems().clear();
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