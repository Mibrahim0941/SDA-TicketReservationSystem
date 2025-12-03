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
import helpers.IDGenerator;
import java.io.IOException;

public class ManageRoutesController {
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
    @FXML private TextField sourceField;
    @FXML private TextField destinationField;
    @FXML private TextField basePriceField;
    @FXML private Button addRouteButton;
    @FXML private Button updateRouteButton;
    @FXML private Button deleteRouteButton;
    @FXML private Button refreshButton;
    @FXML private Button clearButton;
    @FXML private Button backButton;
    @FXML private TextArea routeDetailsArea;

    public static void show(Stage stage, String username, Admin admin) {
        try {
            FXMLLoader loader = new FXMLLoader(ManageRoutesController.class.getResource("/ui/manage-routes.fxml"));
            Parent root = loader.load();
            ManageRoutesController controller = loader.getController();
            controller.setAdminData(username, admin);
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(ManageRoutesController.class.getResource("/ui/manage-routes.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Manage Routes");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to load route management page: " + e.getMessage());
        }
    }

    public void setAdminData(String username, Admin admin) {
        this.currentUsername = username;
        this.currentAdmin = admin;
        if (userGreeting != null) {
            userGreeting.setText("Manage Routes - " + username);
        }
        Platform.runLater(this::loadRoutesData);
    }

    @FXML
    public void initialize() {
        System.out.println("ManageRoutesController initialized");
        setupEventHandlers();
        initializeTable();
    }

    private void setupEventHandlers() {
        if (addRouteButton != null) addRouteButton.setOnAction(e -> handleAddRoute());
        if (updateRouteButton != null) updateRouteButton.setOnAction(e -> handleUpdateRoute());
        if (deleteRouteButton != null) deleteRouteButton.setOnAction(e -> handleDeleteRoute());
        if (refreshButton != null) refreshButton.setOnAction(e -> handleRefresh());
        if (clearButton != null) clearButton.setOnAction(e -> handleClear());
        if (backButton != null) backButton.setOnAction(e -> handleBack());
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
            try {
                routeCatalog.refresh();
                routesTable.setItems(FXCollections.observableArrayList(routeCatalog.getAllRoutes()));
                System.out.println("Loaded " + routeCatalog.getAllRoutes().size() + " routes");
            } catch (Exception e) {
                System.err.println("Error loading routes: " + e.getMessage());
                showError("Failed to load routes: " + e.getMessage());
            }
        }
    }

    private void showRouteDetails(Route route) {
        if (route != null) {
            sourceField.setText(route.getSource());
            destinationField.setText(route.getDestination());
            basePriceField.setText(String.valueOf(route.getBasePrice()));
            
            if (routeDetailsArea != null) {
                StringBuilder details = new StringBuilder();
                details.append("=== ROUTE DETAILS ===\n\n");
                details.append("Route ID: ").append(route.getRouteID()).append("\n");
                details.append("Source: ").append(route.getSource()).append("\n");
                details.append("Destination: ").append(route.getDestination()).append("\n");
                details.append("Base Price: PKR ").append(String.format("%.2f", route.getBasePrice())).append("\n");
                details.append("Number of Schedules: ").append(route.getSchedules().size()).append("\n\n");
                
                if (route.getSchedules().isEmpty()) {
                    details.append("⚠ No schedules configured for this route.\n");
                } else {
                    details.append("✓ Route has active schedules.\n");
                }
                routeDetailsArea.setText(details.toString());
            }
        }
    }

    private void handleAddRoute() {
        String source = sourceField.getText().trim();
        String destination = destinationField.getText().trim();
        String priceText = basePriceField.getText().trim();
        
        if (source.isEmpty() || destination.isEmpty() || priceText.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }
        
        if (source.equalsIgnoreCase(destination)) {
            showError("Source and destination cannot be the same");
            return;
        }
        
        try {
            double basePrice = Double.parseDouble(priceText);
            
            if (basePrice <= 0) {
                showError("Base price must be greater than 0");
                return;
            }
            
            boolean routeExists = routeCatalog.getAllRoutes().stream()
                .anyMatch(r -> r.getSource().equalsIgnoreCase(source) && 
                              r.getDestination().equalsIgnoreCase(destination));
            
            if (routeExists) {
                showError("A route from " + source + " to " + destination + " already exists");
                return;
            }
            
            String routeID = IDGenerator.generateRouteID();
            Route newRoute = new Route(routeID, source, destination, basePrice);
            
            if (routeCatalog.addRoute(newRoute)) {
                showSuccess("Route added successfully!\nRoute ID: " + routeID);
                handleClear();
                loadRoutesData();
            } else {
                showError("Failed to add route");
            }
        } catch (NumberFormatException e) {
            showError("Invalid price format. Please enter a valid number.");
        }
    }

    private void handleUpdateRoute() {
        Route selectedRoute = routesTable.getSelectionModel().getSelectedItem();
        
        if (selectedRoute == null) {
            showError("Please select a route to update");
            return;
        }
        
        String source = sourceField.getText().trim();
        String destination = destinationField.getText().trim();
        String priceText = basePriceField.getText().trim();
        
        if (source.isEmpty() || destination.isEmpty() || priceText.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }
        
        if (source.equalsIgnoreCase(destination)) {
            showError("Source and destination cannot be the same");
            return;
        }
        
        try {
            double basePrice = Double.parseDouble(priceText);
            
            if (basePrice <= 0) {
                showError("Base price must be greater than 0");
                return;
            }
            Route currentRoute = routeCatalog.getRoute(selectedRoute.getRouteID());
            if (currentRoute != null) {
                currentRoute.setSource(source);
                currentRoute.setDestination(destination);
                currentRoute.setBasePrice(basePrice);
                
                if (routeCatalog.updateRoute(currentRoute)) {
                    showSuccess("Route updated successfully!");
                    loadRoutesData();
                } else {
                    showError("Failed to update route");
                }
            } else {
                showError("Route not found in catalog");
            }
        } catch (NumberFormatException e) {
            showError("Invalid price format. Please enter a valid number.");
        }
    }

    private void handleDeleteRoute() {
        Route selectedRoute = routesTable.getSelectionModel().getSelectedItem();
        
        if (selectedRoute == null) {
            showError("Please select a route to delete");
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Route");
        confirmation.setContentText("Are you sure you want to delete route from\n" + 
                                    selectedRoute.getSource() + " → " + 
                                    selectedRoute.getDestination() + "?\n\n" +
                                    "⚠ This will also delete all associated schedules (" + 
                                    selectedRoute.getSchedules().size() + " schedules).\n\n" +
                                    "This action cannot be undone.");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (routeCatalog.deleteRoute(selectedRoute.getRouteID())) {
                    showSuccess("Route deleted successfully!");
                    handleClear();
                    loadRoutesData();
                } else {
                    showError("Failed to delete route");
                }
            }
        });
    }

    private void handleRefresh() {
        loadRoutesData();
        showSuccess("Routes refreshed successfully");
    }

    private void handleClear() {
        sourceField.clear();
        destinationField.clear();
        basePriceField.clear();
        if (routeDetailsArea != null) {
            routeDetailsArea.clear();
        }
        routesTable.getSelectionModel().clearSelection();
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