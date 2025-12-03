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

    public static void show(Stage stage, String username, SupportStaff staff) {
        try {
            System.out.println("Loading Support Staff Dashboard...");
            
            FXMLLoader loader = new FXMLLoader(SupportStaffDashboardController.class.getResource("/ui/support-staff-dashboard.fxml"));
            Parent root = loader.load();
            
            SupportStaffDashboardController controller = loader.getController();
            if (controller != null) {
                controller.setStaffData(username, staff);
            }
            
            Scene scene = new Scene(root, 1200, 800);
            
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

    public void setStaffData(String username, SupportStaff staff) {
        this.staffUsername = username;
        this.currentStaff = staff;
        
        if (userGreeting != null) {
            userGreeting.setText("Hello, " + username + "! 👋");
        }
        Platform.runLater(() -> {
            loadQueriesData();
            loadSupportStats();
        });
    }

    @FXML
    public void initialize() {
        System.out.println("SupportStaffDashboardController initialized");
        setupEventHandlers();
        initializeTable();
    }

    private void setupEventHandlers() {
        if (viewQueriesCard != null) {
            viewQueriesCard.setOnMouseClicked(e -> showAllQueries());
        }
        
        if (answerQueriesCard != null) {
            answerQueriesCard.setOnMouseClicked(e -> focusOnResponse());
        }
        
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
            System.out.println("Initializing table with columns...");
            queryIdColumn.setCellValueFactory(new PropertyValueFactory<>("queryID"));
            
            customerNameColumn.setCellValueFactory(cellData -> {
                SupportQuery query = cellData.getValue();
                if (query.getCustomer() != null && query.getCustomer().getName() != null) {
                    return new javafx.beans.property.SimpleStringProperty(query.getCustomer().getName());
                }
                return new javafx.beans.property.SimpleStringProperty("Unknown");
            });
            
            queryTypeColumn.setCellValueFactory(cellData -> {
                SupportQuery query = cellData.getValue();
                String text = query.getText();
                if (text != null && text.length() > 30) {
                    return new javafx.beans.property.SimpleStringProperty(text.substring(0, 30) + "...");
                }
                return new javafx.beans.property.SimpleStringProperty(text != null ? text : "No description");
            });
            
            statusColumn.setCellValueFactory(cellData -> {
                SupportQuery query = cellData.getValue();
                String status = query.isStatus() ? "Resolved" : "Pending";
                if (query.getSupportStaff() == null) {
                    status = "Unassigned";
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

            queriesTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    System.out.println("Table selection changed to: " + (newSelection != null ? newSelection.getQueryID() : "null"));
                    showQueryDetails(newSelection);
                }
            );
            queriesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            
            System.out.println("Table initialization complete");
        } else {
            System.err.println("queriesTable is null during initialization!");
        }
    }

    private void loadQueriesData() {
        if (queryCatalog != null && queriesTable != null) {
            try {
                ArrayList<SupportQuery> queries = queryCatalog.getAllQueries();
                System.out.println("Loading " + queries.size() + " queries into table");
                for (SupportQuery query : queries) {
                    System.out.println("Adding to table: " + query.getQueryID() + " - " + 
                                     (query.getCustomer() != null ? query.getCustomer().getName() : "No Customer"));
                }
                
                queriesTable.getItems().setAll(queries);
                queriesTable.refresh();
                
                System.out.println("Table now has " + queriesTable.getItems().size() + " items");
                
                if (queries.isEmpty()) {
                    System.out.println("No queries found in the database");
                    queriesTable.setPlaceholder(new Label("No customer queries found"));
                }
                
            } catch (Exception e) {
                System.err.println("Error loading queries: " + e.getMessage());
                e.printStackTrace();
                queriesTable.setPlaceholder(new Label("Error loading queries: " + e.getMessage()));
            }
        } else {
            System.err.println("QueryCatalog or queriesTable is null!");
            if (queriesTable != null) {
                queriesTable.setPlaceholder(new Label("Unable to load queries"));
            }
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
                showSuccess("Ready to respond to selected query");
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
            boolean success = queryCatalog.updateQueryResponse(selectedQuery.getQueryID(), response);
            
            if (success) {
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
            if (selectedQuery.getResponse() == null || selectedQuery.getResponse().isEmpty()) {
                boolean success = queryCatalog.updateQueryResponse(selectedQuery.getQueryID(), "Issue resolved by support staff.");
                if (success) {
                    queryCatalog.assignSupportStaff(selectedQuery.getQueryID(), currentStaff);
                    showSuccess("Query marked as resolved!");
                } else {
                    showError("Failed to mark query as resolved.");
                }
            } else {
                selectedQuery.setStatus(true);
                queryCatalog.updateQuery(selectedQuery);
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
        System.out.println("Refreshing data...");
        loadQueriesData();
        loadSupportStats();
        showSuccess("Data refreshed successfully");
    }

    private void loadSupportStats() {
        if (queryCatalog != null) {
            int pendingCount = queryCatalog.getPendingQueryCount();
            int totalCount = queryCatalog.getQueryCount();
            int resolvedCount = totalCount - pendingCount;
            
            double responseRate = totalCount > 0 ? ((double) resolvedCount / totalCount) * 100 : 0;
            
            if (pendingQueriesLabel != null) pendingQueriesLabel.setText(String.valueOf(pendingCount));
            if (resolvedTodayLabel != null) resolvedTodayLabel.setText(String.valueOf(resolvedCount));
            if (responseRateLabel != null) responseRateLabel.setText(String.format("%.1f%%", responseRate));
            if (avgResponseTimeLabel != null) avgResponseTimeLabel.setText("24h");
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
            StaffLoginController.show(currentStage);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to logout: " + e.getMessage());
        }
    }

    private void goToMainPage() {
        try {
            Stage currentStage = (Stage) mainPageButton.getScene().getWindow();
            MainController.show(currentStage);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load main page: " + e.getMessage());
        }
    }

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

    public ArrayList<SupportQuery> getSupportQueries() {
        return queryCatalog.getAllQueries();
    }
}