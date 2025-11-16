package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.SupportQuery;
import models.SupportStaff;
import catalogs.QueryCatalog;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class SupportStaffDashboardController {

    private QueryCatalog queryCatalog = new QueryCatalog();
    private String staffUsername;
    private SupportStaff currentStaff;

    // Dashboard Elements
    @FXML private Text welcomeTitle;
    @FXML private Text userGreeting;
    
    @FXML private VBox viewQueriesCard;
    @FXML private VBox answerQueriesCard;
    
    @FXML private TableView<SupportQuery> queriesTable;
    @FXML private TableColumn<SupportQuery, String> queryIdColumn;
    @FXML private TableColumn<SupportQuery, String> customerNameColumn;
    @FXML private TableColumn<SupportQuery, String> queryTypeColumn;
    @FXML private TableColumn<SupportQuery, String> statusColumn;
    @FXML private TableColumn<SupportQuery, String> dateColumn;
    
    @FXML private TextArea queryDetailsArea;
    @FXML private TextArea responseArea;
    
    @FXML private Button sendResponseButton;
    @FXML private Button markResolvedButton;
    @FXML private Button escalateButton;
    @FXML private Button refreshButton;
    @FXML private Button logoutButton;
    @FXML private Button mainPageButton;
    
    @FXML private Label pendingQueriesLabel;
    @FXML private Label resolvedTodayLabel;
    @FXML private Label responseRateLabel;
    @FXML private Label avgResponseTimeLabel;

    // SHOW METHOD - Updated for your classes
    public static void show(Stage stage, String username) {
        try {
            System.out.println("Loading Support Staff Dashboard...");
            
            FXMLLoader loader = new FXMLLoader(SupportStaffDashboardController.class.getResource("/ui/support-staff-dashboard.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set staff data
            SupportStaffDashboardController controller = loader.getController();
            if (controller != null) {
                controller.setStaffData(username);
            }
            
            Scene scene = new Scene(root, 1200, 800);
            
            // Try to load CSS
            try {
                scene.getStylesheets().add(SupportStaffDashboardController.class.getResource("/ui/support-staff-dashboard.css").toExternalForm());
                System.out.println("CSS loaded successfully");
            } catch (Exception cssEx) {
                System.out.println("CSS not loaded: " + cssEx.getMessage());
            }
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Support Staff Dashboard");
            stage.centerOnScreen();
            stage.show();
            
            System.out.println("Support Staff Dashboard loaded successfully");
            
        } catch (IOException e) {
            System.err.println("Error loading support staff dashboard: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Failed to load support staff dashboard: " + e.getMessage());
        }
    }

    public void setStaffData(String username) {
        this.staffUsername = username;
        // In a real app, you'd load the actual SupportStaff object from your catalog
        // For now, create a temporary staff object
        this.currentStaff = new SupportStaff("STAFF001", "Support Agent", "password", username, "staff@ticketgenie.com", "1234567890");
        
        if (userGreeting != null) {
            userGreeting.setText("Hello, " + username + "! 👋");
        }
    }

    @FXML
    public void initialize() {
        System.out.println("SupportStaffDashboardController initialized");
        setupEventHandlers();
        initializeTable();
        loadSupportStats();
    }

    private void setupEventHandlers() {
        // View Queries Card
        if (viewQueriesCard != null) {
            viewQueriesCard.setOnMouseClicked(e -> showAllQueries());
        }
        
        // Answer Queries Card
        if (answerQueriesCard != null) {
            answerQueriesCard.setOnMouseClicked(e -> focusOnResponse());
        }
        
        // Buttons
        if (sendResponseButton != null) {
            sendResponseButton.setOnAction(e -> sendResponse());
        }
        if (markResolvedButton != null) {
            markResolvedButton.setOnAction(e -> markAsResolved());
        }
        if (escalateButton != null) {
            escalateButton.setOnAction(e -> escalateQuery());
        }
        if (refreshButton != null) {
            refreshButton.setOnAction(e -> refreshData());
        }
        if (logoutButton != null) {
            logoutButton.setOnAction(e -> logout());
        }
        if (mainPageButton != null) {
            mainPageButton.setOnAction(e -> goToMainPage());
        }
    }

    private void initializeTable() {
        if (queriesTable != null) {
            // Initialize table columns to match your SupportQuery class
            queryIdColumn.setCellValueFactory(new PropertyValueFactory<>("queryID"));
            
            // For customer name - we need to extract from Customer object
            customerNameColumn.setCellValueFactory(cellData -> {
                SupportQuery query = cellData.getValue();
                if (query.getCustomer() != null) {
                    return new javafx.beans.property.SimpleStringProperty(query.getCustomer().getName());
                }
                return new javafx.beans.property.SimpleStringProperty("Unknown");
            });
            
            // For query type - using text as query type
            queryTypeColumn.setCellValueFactory(cellData -> {
                SupportQuery query = cellData.getValue();
                String text = query.getText();
                // Extract first few words as "query type"
                if (text != null && text.length() > 30) {
                    return new javafx.beans.property.SimpleStringProperty(text.substring(0, 30) + "...");
                }
                return new javafx.beans.property.SimpleStringProperty(text != null ? text : "No description");
            });
            
            // Status column
            statusColumn.setCellValueFactory(cellData -> {
                SupportQuery query = cellData.getValue();
                String status = query.isStatus() ? "Resolved" : "Pending";
                if (query.getSupportStaff() == null) {
                    status = "Unassigned";
                }
                return new javafx.beans.property.SimpleStringProperty(status);
            });
            
            // Date column
            dateColumn.setCellValueFactory(cellData -> {
                SupportQuery query = cellData.getValue();
                if (query.getAskedOn() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
                    return new javafx.beans.property.SimpleStringProperty(sdf.format(query.getAskedOn()));
                }
                return new javafx.beans.property.SimpleStringProperty("Unknown");
            });
            
            // Add selection listener
            queriesTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> showQueryDetails(newSelection)
            );
            
            // Load initial data
            loadQueriesData();
        }
    }

    private void loadQueriesData() {
        if (queryCatalog != null && queriesTable != null) {
            ArrayList<SupportQuery> queries = queryCatalog.getAllQueries();
            queriesTable.getItems().setAll(queries);
        }
    }

    private void showAllQueries() {
        refreshData();
        showSuccess("Loaded all customer queries");
    }

    private void focusOnResponse() {
        if (queriesTable != null && queriesTable.getSelectionModel().getSelectedItem() != null) {
            if (responseArea != null) {
                responseArea.requestFocus();
            }
        } else {
            showError("Please select a query first");
        }
    }

    private void sendResponse() {
        if (responseArea == null) return;
        
        String response = responseArea.getText().trim();
        if (response.isEmpty()) {
            showError("Please enter a response");
            return;
        }

        SupportQuery selectedQuery = queriesTable.getSelectionModel().getSelectedItem();
        if (selectedQuery == null) {
            showError("Please select a query to respond to");
            return;
        }

        try {
            // Update the query using your catalog method
            boolean success = queryCatalog.updateQueryResponse(selectedQuery.getQueryID(), response);
            
            if (success) {
                // Assign this staff member to the query
                queryCatalog.assignSupportStaff(selectedQuery.getQueryID(), currentStaff);
                
                showSuccess("Response sent successfully!");
                responseArea.clear();
                refreshData();
            } else {
                showError("Failed to send response. Please try again.");
            }
        } catch (Exception e) {
            showError("Error sending response: " + e.getMessage());
        }
    }

    private void markAsResolved() {
        SupportQuery selectedQuery = queriesTable.getSelectionModel().getSelectedItem();
        if (selectedQuery == null) {
            showError("Please select a query to mark as resolved");
            return;
        }

        try {
            // If no response exists, create an empty one to mark as resolved
            if (selectedQuery.getResponse() == null || selectedQuery.getResponse().isEmpty()) {
                boolean success = queryCatalog.updateQueryResponse(selectedQuery.getQueryID(), "Issue resolved by support staff.");
                if (success) {
                    queryCatalog.assignSupportStaff(selectedQuery.getQueryID(), currentStaff);
                    showSuccess("Query marked as resolved!");
                } else {
                    showError("Failed to mark query as resolved.");
                }
            } else {
                // Query already has response, just update status
                selectedQuery.setStatus(true);
                queryCatalog.updateQueryInDatabase(selectedQuery);
                showSuccess("Query marked as resolved!");
            }
            refreshData();
        } catch (Exception e) {
            showError("Error marking query as resolved: " + e.getMessage());
        }
    }

    private void escalateQuery() {
        SupportQuery selectedQuery = queriesTable.getSelectionModel().getSelectedItem();
        if (selectedQuery == null) {
            showError("Please select a query to escalate");
            return;
        }

        try {
            // For escalation, we'll add a note to the response
            String escalationNote = "\n\n[ESCALATED TO SENIOR SUPPORT - " + new java.util.Date() + "]";
            String newResponse = (selectedQuery.getResponse() != null ? selectedQuery.getResponse() : "") + escalationNote;
            
            boolean success = queryCatalog.updateQueryResponse(selectedQuery.getQueryID(), newResponse);
            if (success) {
                queryCatalog.assignSupportStaff(selectedQuery.getQueryID(), currentStaff);
                showSuccess("Query escalated to senior support!");
                refreshData();
            } else {
                showError("Failed to escalate query.");
            }
        } catch (Exception e) {
            showError("Error escalating query: " + e.getMessage());
        }
    }

    private void refreshData() {
        loadQueriesData();
        loadSupportStats();
        showSuccess("Data refreshed successfully");
    }

    private void loadSupportStats() {
        if (queryCatalog != null) {
            int pendingCount = queryCatalog.getPendingQueryCount();
            int totalCount = queryCatalog.getQueryCount();
            int resolvedCount = totalCount - pendingCount;
            
            // Calculate response rate (simplified)
            double responseRate = totalCount > 0 ? ((double) resolvedCount / totalCount) * 100 : 0;
            
            if (pendingQueriesLabel != null) pendingQueriesLabel.setText(String.valueOf(pendingCount));
            if (resolvedTodayLabel != null) resolvedTodayLabel.setText(String.valueOf(resolvedCount));
            if (responseRateLabel != null) responseRateLabel.setText(String.format("%.1f%%", responseRate));
            if (avgResponseTimeLabel != null) avgResponseTimeLabel.setText("24h"); // Simplified
        }
    }

    private void showQueryDetails(SupportQuery query) {
        if (query != null && queryDetailsArea != null) {
            StringBuilder details = new StringBuilder();
            
            details.append("Query ID: ").append(query.getQueryID()).append("\n");
            
            if (query.getCustomer() != null) {
                details.append("Customer: ").append(query.getCustomer().getName()).append("\n");
                details.append("Email: ").append(query.getCustomer().getEmail()).append("\n");
            } else {
                details.append("Customer: Unknown\n");
            }
            
            details.append("Status: ").append(query.isStatus() ? "Resolved" : "Pending").append("\n");
            
            if (query.getAskedOn() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm");
                details.append("Asked On: ").append(sdf.format(query.getAskedOn())).append("\n");
            }
            
            details.append("\nQuery Description:\n").append(query.getText()).append("\n");
            
            if (query.getResponse() != null && !query.getResponse().isEmpty()) {
                details.append("\nPrevious Response:\n").append(query.getResponse());
            }
            
            queryDetailsArea.setText(details.toString());
            
            // Clear response area for new responses
            if (!query.isStatus()) {
                responseArea.clear();
            } else {
                responseArea.setText(query.getResponse());
            }
        }
    }

    private void logout() {
        try {
            Stage currentStage = (Stage) logoutButton.getScene().getWindow();
            LoginTypePage.show(currentStage);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to logout: " + e.getMessage());
        }
    }

    private void goToMainPage() {
        try {
            Stage currentStage = (Stage) mainPageButton.getScene().getWindow();
            MainPage.show(currentStage);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load main page: " + e.getMessage());
        }
    }

    // Helper methods
    private void showError(String message) {
        System.err.println("Error: " + message);
        showAlert("Error", message);
    }

    private void showSuccess(String message) {
        System.out.println("Success: " + message);
        showAlert("Success", message);
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

    // ADD THIS HELPER METHOD
    private static void showErrorAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Method to get support queries (for testing/demonstration)
    public ArrayList<SupportQuery> getSupportQueries() {
        return queryCatalog.getAllQueries();
    }
}