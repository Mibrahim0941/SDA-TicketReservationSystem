package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Customer;
import models.SupportQuery;
import catalogs.QueryCatalog;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class CustomerSupportController implements Initializable {

    private QueryCatalog queryCatalog = new QueryCatalog();
    private Customer currentCustomer;
    private String currentUsername;

    // UI Elements
    @FXML private Text welcomeTitle;
    @FXML private Text userGreeting;
    
    @FXML private TableView<SupportQuery> queriesTable;
    @FXML private TableColumn<SupportQuery, String> queryIdColumn;
    @FXML private TableColumn<SupportQuery, String> queryTextColumn;
    @FXML private TableColumn<SupportQuery, String> statusColumn;
    @FXML private TableColumn<SupportQuery, String> dateColumn;
    @FXML private TableColumn<SupportQuery, String> responseColumn;
    
    @FXML private TextArea queryDetailsArea;
    @FXML private TextArea newQueryArea;
    
    @FXML private Button submitQueryButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    @FXML private Button logoutButton;

    // Show method to launch the support page
    public static void show(Stage stage, String username, Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(CustomerSupportController.class.getResource("/ui/customer-support.fxml"));
            Parent root = loader.load();
            
            CustomerSupportController controller = loader.getController();
            controller.setCustomerData(username, customer);
            
            Scene scene = new Scene(root, 1000, 700);
            
            try {
                scene.getStylesheets().add(CustomerSupportController.class.getResource("/ui/customer-support.css").toExternalForm());
            } catch (Exception cssEx) {
                System.out.println("CSS not loaded: " + cssEx.getMessage());
            }
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Customer Support");
            stage.centerOnScreen();
            stage.show();
            
        } catch (IOException e) {
            System.err.println("Error loading customer support page: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Failed to load support page: " + e.getMessage());
        }
    }

    public void setCustomerData(String username, Customer customer) {
        this.currentUsername = username;
        this.currentCustomer = customer;
        
        if (userGreeting != null) {
            userGreeting.setText("Hello, " + username + "! 👋");
        }
        
        // Load customer's queries
        Platform.runLater(() -> refreshData());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("CustomerSupportController initialized");
        setupEventHandlers();
        initializeTable();
    }

    private void setupEventHandlers() {
        submitQueryButton.setOnAction(e -> submitNewQuery());
        refreshButton.setOnAction(e -> refreshData());
        backButton.setOnAction(e -> goBackToDashboard());
        logoutButton.setOnAction(e -> logout());
    }

    private void initializeTable() {
        if (queriesTable != null) {
            // Initialize table columns
            queryIdColumn.setCellValueFactory(new PropertyValueFactory<>("queryID"));
            
            queryTextColumn.setCellValueFactory(cellData -> {
                SupportQuery query = cellData.getValue();
                String text = query.getText();
                if (text != null && text.length() > 50) {
                    return new javafx.beans.property.SimpleStringProperty(text.substring(0, 50) + "...");
                }
                return new javafx.beans.property.SimpleStringProperty(text != null ? text : "No description");
            });
            
            statusColumn.setCellValueFactory(cellData -> {
                SupportQuery query = cellData.getValue();
                String status = query.isStatus() ? "✅ Resolved" : "⏳ Pending";
                if (query.getSupportStaff() == null) {
                    status = "🔄 Unassigned";
                }
                return new javafx.beans.property.SimpleStringProperty(status);
            });
            
            dateColumn.setCellValueFactory(cellData -> {
                SupportQuery query = cellData.getValue();
                if (query.getAskedOn() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
                    return new javafx.beans.property.SimpleStringProperty(sdf.format(query.getAskedOn()));
                }
                return new javafx.beans.property.SimpleStringProperty("Unknown");
            });
            
            responseColumn.setCellValueFactory(cellData -> {
                SupportQuery query = cellData.getValue();
                if (query.getResponse() != null && !query.getResponse().isEmpty()) {
                    return new javafx.beans.property.SimpleStringProperty("📨 View Response");
                }
                return new javafx.beans.property.SimpleStringProperty("No response yet");
            });
            
            // Set column widths
            queryIdColumn.setPrefWidth(80);
            queryTextColumn.setPrefWidth(200);
            statusColumn.setPrefWidth(120);
            dateColumn.setPrefWidth(100);
            responseColumn.setPrefWidth(120);
            
            // Add selection listener
            queriesTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> showQueryDetails(newSelection)
            );
            
            // Load initial data
            loadCustomerQueries();
        }
    }

    private void loadCustomerQueries() {
        if (queryCatalog != null && queriesTable != null && currentCustomer != null) {
            ArrayList<SupportQuery> customerQueries = queryCatalog.getQueriesByCustomer(currentCustomer);
            queriesTable.getItems().setAll(customerQueries);
            
            System.out.println("Loaded " + customerQueries.size() + " queries for customer: " + currentCustomer.getName());
        }
    }

    private void showQueryDetails(SupportQuery query) {
        if (query != null && queryDetailsArea != null) {
            StringBuilder details = new StringBuilder();
            
            details.append("Query ID: ").append(query.getQueryID()).append("\n");
            details.append("Status: ").append(query.isStatus() ? "✅ Resolved" : "⏳ Pending").append("\n");
            
            if (query.getAskedOn() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm");
                details.append("Asked On: ").append(sdf.format(query.getAskedOn())).append("\n");
            }
            
            if (query.getSupportStaff() != null) {
                details.append("Assigned To: ").append(query.getSupportStaff().getName()).append("\n");
            }
            
            details.append("\n--- Your Query ---\n").append(query.getText()).append("\n");
            
            if (query.getResponse() != null && !query.getResponse().isEmpty()) {
                details.append("\n--- Support Response ---\n").append(query.getResponse());
            } else {
                details.append("\n--- Support Response ---\nNo response yet. Our team will get back to you soon!");
            }
            
            queryDetailsArea.setText(details.toString());
        }
    }

    private void submitNewQuery() {
        if (newQueryArea == null) return;
        
        String queryText = newQueryArea.getText().trim();
        if (queryText.isEmpty()) {
            showError("Please enter your question or issue");
            return;
        }

        if (queryText.length() < 10) {
            showError("Please provide more details (at least 10 characters)");
            return;
        }

        try {
            // Generate a unique query ID
            String queryID = "QUERY_" + System.currentTimeMillis();
            
            // Create new support query
            SupportQuery newQuery = new SupportQuery(
                queryText,
                new java.util.Date(),
                queryID,
                null, // No staff assigned yet
                currentCustomer
            );
            
            // Add to catalog
            boolean success = queryCatalog.addToCatalog(newQuery);
            
            if (success) {
                showSuccess("Your query has been submitted successfully! Query ID: " + queryID);
                newQueryArea.clear();
                refreshData();
                
                // Select the new query in the table
                queriesTable.getSelectionModel().select(newQuery);
            } else {
                showError("Failed to submit your query. Please try again.");
            }
            
        } catch (Exception e) {
            showError("Error submitting query: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshData() {
        loadCustomerQueries();
        showSuccess("Data refreshed successfully");
    }

    private void goBackToDashboard() {
        try {
            Stage currentStage = (Stage) backButton.getScene().getWindow();
            DashboardController.show(currentStage, currentUsername, currentCustomer);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to go back to dashboard: " + e.getMessage());
        }
    }

    private void logout() {
        try {
            Stage currentStage = (Stage) logoutButton.getScene().getWindow();
            CustomerLoginController.show(currentStage);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to logout: " + e.getMessage());
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