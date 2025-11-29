package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Admin;
import models.Route;
import catalogs.RouteCatalog;

import java.io.IOException;

public class ManagePricingController {

    private RouteCatalog routeCatalog = RouteCatalog.getInstance();
    private String currentUsername;
    private Admin currentAdmin;

    @FXML private Text welcomeTitle;
    @FXML private Text userGreeting;
    
    @FXML private TableView<Route> routesTable;
    @FXML private TableColumn<Route, String> routeIdColumn;
    @FXML private TableColumn<Route, String> sourceColumn;
    @FXML private TableColumn<Route, String> destinationColumn;
    @FXML private TableColumn<Route, Double> basePriceColumn;
    
    @FXML private TextField newPriceField;
    @FXML private TextField percentageField;
    
    @FXML private Button updatePriceButton;
    @FXML private Button applyPercentageButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    
    @FXML private TextArea priceDetailsArea;

    public static void show(Stage stage, String username, Admin admin) {
        try {
            FXMLLoader loader = new FXMLLoader(ManagePricingController.class.getResource("/ui/manage-pricing.fxml"));
            Parent root = loader.load();
            
            ManagePricingController controller = loader.getController();
            controller.setAdminData(username, admin);
            
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(ManagePricingController.class.getResource("/ui/manage-pricing.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Manage Pricing");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to load pricing management page: " + e.getMessage());
        }
    }

    public void setAdminData(String username, Admin admin) {
        this.currentUsername = username;
        this.currentAdmin = admin;
        
        if (userGreeting != null) {
            userGreeting.setText("Manage Pricing - " + username);
        }
        
        Platform.runLater(this::loadRoutesData);
    }

    @FXML
    public void initialize() {
        System.out.println("ManagePricingController initialized");
        setupEventHandlers();
        initializeTable();
    }

    private void setupEventHandlers() {
        if (updatePriceButton != null) {
            updatePriceButton.setOnAction(e -> handleUpdatePrice());
        }
        if (applyPercentageButton != null) {
            applyPercentageButton.setOnAction(e -> handleApplyPercentage());
        }
        if (refreshButton != null) {
            refreshButton.setOnAction(e -> handleRefresh());
        }
        if (backButton != null) {
            backButton.setOnAction(e -> handleBack());
        }
    }

    private void initializeTable() {
        if (routesTable != null) {
            routeIdColumn.setCellValueFactory(new PropertyValueFactory<>("routeID"));
            sourceColumn.setCellValueFactory(new PropertyValueFactory<>("source"));
            destinationColumn.setCellValueFactory(new PropertyValueFactory<>("destination"));
            basePriceColumn.setCellValueFactory(new PropertyValueFactory<>("basePrice"));
            
            basePriceColumn.setCellFactory(column -> new TableCell<Route, Double>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("PKR %.2f", item));
                    }
                }
            });
            
            routesTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> showRouteDetails(newSelection)
            );
        }
    }

    private void loadRoutesData() {
        if (routeCatalog != null && routesTable != null) {
            routeCatalog.refresh();
            routesTable.setItems(FXCollections.observableArrayList(routeCatalog.getAllRoutes()));
        }
    }

    private void showRouteDetails(Route route) {
        if (route != null) {
            if (priceDetailsArea != null) {
                StringBuilder details = new StringBuilder();
                details.append("=== PRICING DETAILS ===\n\n");
                details.append("Route ID: ").append(route.getRouteID()).append("\n");
                details.append("Source: ").append(route.getSource()).append("\n");
                details.append("Destination: ").append(route.getDestination()).append("\n");
                details.append("Current Base Price: PKR ").append(String.format("%.2f", route.getBasePrice())).append("\n");
                details.append("Number of Schedules: ").append(route.getSchedules().size()).append("\n\n");
                
                if (!route.getSchedules().isEmpty()) {
                    details.append("Schedule Classes:\n");
                    for (models.Schedule schedule : route.getSchedules()) {
                        double schedulePrice = route.getBasePrice() * (schedule.getTypePercentage() / 100);
                        details.append("• ").append(schedule.getScheduleClass())
                              .append(": PKR ").append(String.format("%.2f", schedulePrice))
                              .append(" (").append(schedule.getTypePercentage()).append("%)\n");
                    }
                } else {
                    details.append("⚠ No schedules configured for this route.\n");
                }
                
                priceDetailsArea.setText(details.toString());
            }
        }
    }

    private void handleUpdatePrice() {
        Route selectedRoute = routesTable.getSelectionModel().getSelectedItem();
        String priceText = newPriceField.getText().trim();
        
        if (selectedRoute == null) {
            showError("Please select a route to update");
            return;
        }
        
        if (priceText.isEmpty()) {
            showError("Please enter new price");
            return;
        }
        
        try {
            double newPrice = Double.parseDouble(priceText);
            
            if (newPrice <= 0) {
                showError("Price must be greater than 0");
                return;
            }
            
            Route currentRoute = routeCatalog.getRoute(selectedRoute.getRouteID());
            if (currentRoute != null) {
                currentRoute.setBasePrice(newPrice);
                
                if (routeCatalog.updateRoute(currentRoute)) {
                    showSuccess("Price updated successfully!\nNew base price: PKR " + String.format("%.2f", newPrice));
                    loadRoutesData();
                    newPriceField.clear();
                } else {
                    showError("Failed to update price");
                }
            } else {
                showError("Route not found in catalog");
            }
            
        } catch (NumberFormatException e) {
            showError("Invalid price format. Please enter a valid number.");
        }
    }

    private void handleApplyPercentage() {
        String percentageText = percentageField.getText().trim();
        
        if (percentageText.isEmpty()) {
            showError("Please enter percentage");
            return;
        }
        
        try {
            double percentage = Double.parseDouble(percentageText);
            
            if (percentage < -100) {
                showError("Percentage cannot be less than -100%");
                return;
            }
            
            boolean allUpdated = true;
            int updatedCount = 0;
            
            for (Route route : routesTable.getItems()) {
                Route currentRoute = routeCatalog.getRoute(route.getRouteID());
                if (currentRoute != null) {
                    double newPrice = currentRoute.getBasePrice() * (1 + percentage/100);
                    newPrice = Math.round(newPrice * 100.0) / 100.0; // Round to 2 decimal places
                    currentRoute.setBasePrice(newPrice);
                    
                    if (routeCatalog.updateRoute(currentRoute)) {
                        updatedCount++;
                    } else {
                        allUpdated = false;
                    }
                }
            }
            
            if (updatedCount > 0) {
                showSuccess("Applied " + percentage + "% change to " + updatedCount + " routes!");
                loadRoutesData();
                percentageField.clear();
            } else {
                showError("No routes were updated");
            }
            
        } catch (NumberFormatException e) {
            showError("Invalid percentage format");
        }
    }

    private void handleRefresh() {
        loadRoutesData();
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