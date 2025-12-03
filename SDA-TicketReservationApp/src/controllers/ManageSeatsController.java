package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageSeatsController {

    private RouteCatalog routeCatalog = RouteCatalog.getInstance();
    private String currentUsername;
    private Admin currentAdmin;
    private List<Seat> currentSeats = new ArrayList<>();
    private Map<String, Double> seatTypePercentages = new HashMap<>();

    @FXML private Text welcomeTitle;
    @FXML private Text userGreeting;
    
    @FXML private ComboBox<Route> routeComboBox;
    @FXML private ComboBox<Schedule> scheduleComboBox;
    @FXML private GridPane seatsGrid;
    @FXML private TextArea seatDetailsArea;
    
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    
    // New UI elements for seat type management
    @FXML private HBox seatTypeButtonsContainer;
    @FXML private VBox seatTypeManagementPane;
    @FXML private Label seatTypeInfoLabel;
    
    // Seat type percentage controls
    @FXML private TextField typeAPercentage;
    @FXML private TextField typeBPercentage;
    @FXML private TextField typeCPercentage;
    @FXML private TextField typeDPercentage;
    
    @FXML private Button applyPercentagesButton;
    @FXML private Button generateSeatsButton;

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
        
        Platform.runLater(() -> {
            loadRoutesData();
            initializeSeatTypePercentages();
        });
    }

    @FXML
    public void initialize() {
        System.out.println("ManageSeatsController initialized");
        setupEventHandlers();
        initializeComboBoxes();
        initializeSeatTypeManagement();
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
        if (applyPercentagesButton != null) {
            applyPercentagesButton.setOnAction(e -> applySeatTypePercentages());
        }
        if (generateSeatsButton != null) {
            generateSeatsButton.setOnAction(e -> generateSeatsForSchedule());
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

    private void initializeSeatTypeManagement() {
        // Initialize seat type percentage fields with default values
        typeAPercentage.setText("0.0");
        typeBPercentage.setText("10.0");
        typeCPercentage.setText("20.0");
        typeDPercentage.setText("30.0");
        
        // Create seat type buttons
        createSeatTypeButtons();
    }

    private void initializeSeatTypePercentages() {
        // Default percentages for each seat type
        seatTypePercentages.put("A", 0.0);    // Standard
        seatTypePercentages.put("B", 10.0);   // Premium (+10%)
        seatTypePercentages.put("C", 20.0);   // Business (+20%)
        seatTypePercentages.put("D", 30.0);   // First Class (+30%)
    }

    private void createSeatTypeButtons() {
        if (seatTypeButtonsContainer == null) return;
        
        seatTypeButtonsContainer.getChildren().clear();
        
        String[] seatTypes = {"A", "B", "C", "D"};
        String[] seatTypeNames = {"Standard", "Premium", "Business", "First Class"};
        String[] colors = {"#3498db", "#2ecc71", "#e67e22", "#9b59b6"};
        
        for (int i = 0; i < seatTypes.length; i++) {
            String seatType = seatTypes[i];
            String seatTypeName = seatTypeNames[i];
            String color = colors[i];
            
            Button seatTypeButton = new Button(seatType + " - " + seatTypeName);
            seatTypeButton.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand;",
                color
            ));
            
            seatTypeButton.setOnAction(e -> {
                updateSeatTypeInfo(seatType, seatTypeName, color);
                loadSeatsByType(seatType);
            });
            
            seatTypeButton.setOnMouseEntered(e -> {
                seatTypeButton.setStyle(String.format(
                    "-fx-background-color: derive(%s, -20%%); -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);",
                    color
                ));
            });
            
            seatTypeButton.setOnMouseExited(e -> {
                seatTypeButton.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand;",
                    color
                ));
            });
            
            seatTypeButtonsContainer.getChildren().add(seatTypeButton);
        }
    }

    private void updateSeatTypeInfo(String seatType, String seatTypeName, String color) {
        if (seatTypeInfoLabel != null) {
            double percentage = seatTypePercentages.getOrDefault(seatType, 0.0);
            seatTypeInfoLabel.setText(String.format(
                "🎯 Currently Viewing: %s Seats (%s)\n💰 Price Premium: +%.1f%%",
                seatTypeName, seatType, percentage
            ));
            seatTypeInfoLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
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
                
                // Reset seat type info
                if (seatTypeInfoLabel != null) {
                    seatTypeInfoLabel.setText("Select a seat type to view specific seats");
                    seatTypeInfoLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
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
            loadSeatTypePercentagesFromDB(selectedSchedule.getScheduleID());
        }
    }

    private void loadSeatsByType(String seatType) {
        seatsGrid.getChildren().clear();
        
        int row = 0;
        int col = 0;
        int totalSeats = 0;
        int availableSeats = 0;
        
        for (Seat seat : currentSeats) {
            if (seat.getSeatType().equals(seatType)) {
                // Create seat button for this seat type
                // Note: We need seat ID from database - this requires a different approach
                // For now, we'll create buttons without seat ID
                Button seatButton = new Button(seat.getSeatNo());
                
                String baseStyle = "-fx-font-weight: bold; -fx-font-size: 10px; -fx-min-width: 35px; -fx-min-height: 35px; " +
                                  "-fx-border-radius: 5px; -fx-background-radius: 5px; -fx-cursor: hand; " +
                                  "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 3, 0, 0, 1);";
                
                if (seat.isAvailability()) {
                    seatButton.setStyle(baseStyle + " -fx-background-color: #27ae60; -fx-text-fill: white;");
                } else {
                    seatButton.setStyle(baseStyle + " -fx-background-color: #e74c3c; -fx-text-fill: white;");
                }
                
                seatButton.setOnAction(e -> showSeatDetails(seat));
                
                seatsGrid.add(seatButton, col, row);
                
                col++;
                if (col >= 4) {
                    col = 0;
                    row++;
                }
                
                totalSeats++;
                if (seat.isAvailability()) {
                    availableSeats++;
                }
            }
        }
        
        if (seatDetailsArea != null && totalSeats > 0) {
            double occupancyRate = totalSeats > 0 ? (double) (totalSeats - availableSeats) / totalSeats * 100 : 0;
            seatDetailsArea.setText(String.format(
                "📊 %s Seats Statistics:\n\n" +
                "Total %s Seats: %d\n" +
                "Available: %d\n" +
                "Occupied: %d\n" +
                "Occupancy Rate: %.1f%%\n\n" +
                "💡 Click on any seat to view details",
                seatType, seatType, totalSeats, availableSeats, totalSeats - availableSeats, occupancyRate
            ));
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

    private void loadSeatTypePercentagesFromDB(String scheduleID) {
        String sql = "SELECT TypePercentage FROM Schedule WHERE ScheduleID = ?";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, scheduleID);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                double typePercentage = rs.getDouble("TypePercentage");
                
                // Load percentages from database or use defaults
                // You might need a separate table for seat type percentages
                // For now, we'll use the UI fields
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading seat type percentages: " + e.getMessage());
        }
    }

    private void showSeatDetails(Seat seat) {
        Alert detailsDialog = new Alert(Alert.AlertType.INFORMATION);
        detailsDialog.setTitle("Seat Details");
        detailsDialog.setHeaderText("Seat Information");
        
        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 20;");
        
        Label seatNumberLabel = new Label("Seat Number: " + seat.getSeatNo());
        seatNumberLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label seatTypeLabel = new Label("Seat Type: " + seat.getSeatType());
        seatTypeLabel.setStyle("-fx-font-size: 14px;");
        
        Label priceLabel = new Label("Price: PKR " + String.format("%.2f", seat.getPrice()));
        priceLabel.setStyle("-fx-font-size: 14px;");
        
        Label statusLabel = new Label("Status: " + (seat.isAvailability() ? "AVAILABLE" : "OCCUPIED"));
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (seat.isAvailability() ? "#27ae60" : "#e74c3c") + "; -fx-font-weight: bold;");
        
        content.getChildren().addAll(seatNumberLabel, seatTypeLabel, priceLabel, statusLabel);
        
        detailsDialog.getDialogPane().setContent(content);
        detailsDialog.showAndWait();
    }

    private void applySeatTypePercentages() {
        try {
            double typeA = Double.parseDouble(typeAPercentage.getText());
            double typeB = Double.parseDouble(typeBPercentage.getText());
            double typeC = Double.parseDouble(typeCPercentage.getText());
            double typeD = Double.parseDouble(typeDPercentage.getText());
            
            // Validate percentages
            if (typeA < 0 || typeB < 0 || typeC < 0 || typeD < 0) {
                showError("Percentages cannot be negative");
                return;
            }
            
            seatTypePercentages.put("A", typeA);
            seatTypePercentages.put("B", typeB);
            seatTypePercentages.put("C", typeC);
            seatTypePercentages.put("D", typeD);
            
            showSuccess("Seat type percentages updated successfully!");
            
            // Update prices for existing seats if schedule is selected
            Schedule selectedSchedule = scheduleComboBox.getValue();
            if (selectedSchedule != null) {
                updateSeatPrices(selectedSchedule.getScheduleID());
            }
            
        } catch (NumberFormatException e) {
            showError("Please enter valid percentage numbers");
        }
    }

    private void updateSeatPrices(String scheduleID) {
        Route selectedRoute = routeComboBox.getValue();
        if (selectedRoute == null) {
            showError("Please select a route first");
            return;
        }
        
        double basePrice = selectedRoute.getBasePrice();
        
        // Update prices for each seat type
        for (Map.Entry<String, Double> entry : seatTypePercentages.entrySet()) {
            String seatType = entry.getKey();
            double percentage = entry.getValue();
            
            double seatPrice = basePrice + (basePrice * percentage / 100);
            
            String updateSql = "UPDATE Seat SET Price = ? WHERE ScheduleID = ? AND SeatType = ?";
            
            try (Connection conn = DriverManager.getConnection(
                    DatabaseConfig.getDbUrl(), 
                    DatabaseConfig.getDbUser(), 
                    DatabaseConfig.getDbPassword());
                 PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                
                pstmt.setDouble(1, seatPrice);
                pstmt.setString(2, scheduleID);
                pstmt.setString(3, seatType);
                
                int rowsAffected = pstmt.executeUpdate();
                System.out.println("Updated " + rowsAffected + " " + seatType + " seats");
                
            } catch (SQLException e) {
                System.err.println("Error updating seat prices: " + e.getMessage());
                showError("Failed to update seat prices: " + e.getMessage());
            }
        }
        
        // Refresh the view
        loadSeatsFromDatabase(scheduleID);
        showSuccess("Seat prices updated successfully!");
    }

    private void generateSeatsForSchedule() {
        Schedule selectedSchedule = scheduleComboBox.getValue();
        if (selectedSchedule == null) {
            showError("Please select a schedule first");
            return;
        }
        
        Route selectedRoute = routeComboBox.getValue();
        if (selectedRoute == null) {
            showError("Please select a route first");
            return;
        }
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Generate Seats");
        confirmDialog.setHeaderText("Generate Seats for Schedule");
        confirmDialog.setContentText("This will generate seats for the selected schedule. Existing seats will be deleted. Continue?");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (deleteExistingSeats(selectedSchedule.getScheduleID())) {
                    if (createSeatsForSchedule(selectedSchedule.getScheduleID(), selectedRoute.getBasePrice())) {
                        showSuccess("Seats generated successfully!");
                        loadSeatsFromDatabase(selectedSchedule.getScheduleID());
                    } else {
                        showError("Failed to generate seats");
                    }
                } else {
                    showError("Failed to clear existing seats");
                }
            }
        });
    }

    private boolean deleteExistingSeats(String scheduleID) {
        String deleteSql = "DELETE FROM Seat WHERE ScheduleID = ?";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            
            pstmt.setString(1, scheduleID);
            pstmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error deleting seats: " + e.getMessage());
            return false;
        }
    }

    private boolean createSeatsForSchedule(String scheduleID, double basePrice) {
        // Define seat configuration (you can modify this as needed)
        String[] rows = {"A", "B", "C", "D", "E", "F", "G", "H"};
        int seatsPerRow = 4;
        String[] seatTypes = {"A", "B", "C", "D"}; // Rotate seat types
        
        String insertSql = "INSERT INTO Seat (ScheduleID, SeatNumber, SeatType, Price, Availability) VALUES (?, ?, ?, ?, 1)";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            
            int seatTypeIndex = 0;
            
            for (String row : rows) {
                for (int seatNum = 1; seatNum <= seatsPerRow; seatNum++) {
                    String seatNumber = row + seatNum;
                    String seatType = seatTypes[seatTypeIndex % seatTypes.length];
                    
                    // Calculate price based on seat type percentage
                    double percentage = seatTypePercentages.getOrDefault(seatType, 0.0);
                    double seatPrice = basePrice + (basePrice * percentage / 100);
                    
                    pstmt.setString(1, scheduleID);
                    pstmt.setString(2, seatNumber);
                    pstmt.setString(3, seatType);
                    pstmt.setDouble(4, seatPrice);
                    pstmt.addBatch();
                    
                    seatTypeIndex++;
                }
            }
            
            pstmt.executeBatch();
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error creating seats: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void updateStatistics(int totalSeats, int availableSeats, int occupiedSeats) {
        double occupancyRate = totalSeats > 0 ? (double) occupiedSeats / totalSeats * 100 : 0;
        double availabilityRate = totalSeats > 0 ? (double) availableSeats / totalSeats * 100 : 0;
        
        if (seatDetailsArea != null) {
            StringBuilder stats = new StringBuilder();
            stats.append("=== SEAT MANAGEMENT ===\n\n");
            stats.append("📊 OVERALL STATISTICS:\n");
            stats.append("Total Seats: ").append(totalSeats).append("\n");
            stats.append("Available Seats: ").append(availableSeats).append(" (").append(String.format("%.1f", availabilityRate)).append("%)\n");
            stats.append("Occupied Seats: ").append(occupiedSeats).append(" (").append(String.format("%.1f", occupancyRate)).append("%)\n\n");
            
            // Add seat type statistics
            Map<String, Integer> typeCounts = new HashMap<>();
            Map<String, Integer> typeAvailable = new HashMap<>();
            
            for (Seat seat : currentSeats) {
                String type = seat.getSeatType();
                typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
                if (seat.isAvailability()) {
                    typeAvailable.put(type, typeAvailable.getOrDefault(type, 0) + 1);
                }
            }
            
            if (!typeCounts.isEmpty()) {
                stats.append("🎫 SEAT TYPE DISTRIBUTION:\n");
                for (String type : typeCounts.keySet()) {
                    int count = typeCounts.get(type);
                    int available = typeAvailable.getOrDefault(type, 0);
                    double typeOccupancyRate = count > 0 ? (double) (count - available) / count * 100 : 0;
                    stats.append(String.format("Type %s: %d seats (%.1f%% occupied)\n", 
                        type, count, typeOccupancyRate));
                }
                stats.append("\n");
            }
            
            stats.append("🎯 INSTRUCTIONS:\n");
            stats.append("• Use A/B/C/D buttons to filter by seat type\n");
            stats.append("• Set percentages and click 'Apply' to update prices\n");
            stats.append("• Click 'Generate Seats' to create new seat layout\n");
            stats.append("• Click on any seat to view details\n\n");
            
            stats.append("💡 LEGEND:\n");
            stats.append("🟢 Available - Can be booked\n");
            stats.append("🔴 Occupied - Cannot be booked\n");
            stats.append("🔷 Type A - Standard (+" + seatTypePercentages.getOrDefault("A", 0.0) + "%)\n");
            stats.append("🔶 Type B - Premium (+" + seatTypePercentages.getOrDefault("B", 0.0) + "%)\n");
            stats.append("🔸 Type C - Business (+" + seatTypePercentages.getOrDefault("C", 0.0) + "%)\n");
            stats.append("💎 Type D - First Class (+" + seatTypePercentages.getOrDefault("D", 0.0) + "%)\n");
            
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
            
            // Reset seat type info
            if (seatTypeInfoLabel != null) {
                seatTypeInfoLabel.setText("Select a seat type to view specific seats");
                seatTypeInfoLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
            }
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