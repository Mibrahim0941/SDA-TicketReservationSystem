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
import models.CancellationPolicy;
import catalogs.PolicyCatalog;
import helpers.IDGenerator;

import java.io.IOException;

public class ManagePoliciesController {

    private PolicyCatalog policyCatalog = new PolicyCatalog();
    private String currentUsername;
    private Admin currentAdmin;

    @FXML private Text welcomeTitle;
    @FXML private Text userGreeting;
    
    @FXML private TableView<CancellationPolicy> policyTable;
    @FXML private TableColumn<CancellationPolicy, String> policyIdColumn;
    @FXML private TableColumn<CancellationPolicy, Integer> timeColumn;
    @FXML private TableColumn<CancellationPolicy, Double> refundColumn;
    @FXML private TableColumn<CancellationPolicy, String> descriptionColumn;
    
    @FXML private TextField timeField;
    @FXML private TextField refundField;
    @FXML private TextArea descriptionArea;
    
    @FXML private Button addPolicyButton;
    @FXML private Button updatePolicyButton;
    @FXML private Button deletePolicyButton;
    @FXML private Button refreshButton;
    @FXML private Button clearButton;
    @FXML private Button backButton;
    
    @FXML private TextArea policyDetailsArea;

    public static void show(Stage stage, String username, Admin admin) {
        try {
            FXMLLoader loader = new FXMLLoader(ManagePoliciesController.class.getResource("/ui/manage-policies.fxml"));
            Parent root = loader.load();
            
            ManagePoliciesController controller = loader.getController();
            controller.setAdminData(username, admin);
            
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(ManagePoliciesController.class.getResource("/ui/manage-policies.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Manage Cancellation Policies");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to load policies management page: " + e.getMessage());
        }
    }

    public void setAdminData(String username, Admin admin) {
        this.currentUsername = username;
        this.currentAdmin = admin;
        
        if (userGreeting != null) {
            userGreeting.setText("Manage Policies - " + username);
        }
        
        Platform.runLater(this::loadPoliciesData);
    }

    @FXML
    public void initialize() {
        System.out.println("ManagePoliciesController initialized");
        setupEventHandlers();
        initializeTable();
    }

    private void setupEventHandlers() {
        if (addPolicyButton != null) {
            addPolicyButton.setOnAction(e -> handleAddPolicy());
        }
        if (updatePolicyButton != null) {
            updatePolicyButton.setOnAction(e -> handleUpdatePolicy());
        }
        if (deletePolicyButton != null) {
            deletePolicyButton.setOnAction(e -> handleDeletePolicy());
        }
        if (refreshButton != null) {
            refreshButton.setOnAction(e -> handleRefresh());
        }
        if (clearButton != null) {
            clearButton.setOnAction(e -> handleClear());
        }
        if (backButton != null) {
            backButton.setOnAction(e -> handleBack());
        }
    }

    private void initializeTable() {
        if (policyTable != null) {
            policyIdColumn.setCellValueFactory(new PropertyValueFactory<>("policyID"));
            timeColumn.setCellValueFactory(new PropertyValueFactory<>("timeBeforeDeparture"));
            refundColumn.setCellValueFactory(new PropertyValueFactory<>("amountToBeRefunded"));
            descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
            
            refundColumn.setCellFactory(column -> new TableCell<CancellationPolicy, Double>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("%.1f%%", item));
                    }
                }
            });
            
            policyTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> showPolicyDetails(newSelection)
            );
        }
    }

    private void loadPoliciesData() {
        if (policyCatalog != null && policyTable != null) {
            policyCatalog.refresh();
            policyTable.setItems(FXCollections.observableArrayList(policyCatalog.getAllPolicies()));
        }
    }

    private void showPolicyDetails(CancellationPolicy policy) {
        if (policy != null) {
            timeField.setText(String.valueOf(policy.getTimeBeforeDeparture()));
            refundField.setText(String.valueOf(policy.getAmountToBeRefunded()));
            descriptionArea.setText(policy.getDescription());
            
            if (policyDetailsArea != null) {
                StringBuilder details = new StringBuilder();
                details.append("=== CANCELLATION POLICY DETAILS ===\n\n");
                details.append("Policy ID: ").append(policy.getPolicyID()).append("\n");
                details.append("Time Before Departure: ").append(policy.getTimeBeforeDeparture()).append(" hours\n");
                details.append("Refund Amount: ").append(policy.getAmountToBeRefunded()).append("%\n");
                details.append("Description: ").append(policy.getDescription()).append("\n\n");
                details.append("Summary: ").append(policy.getData());
                
                policyDetailsArea.setText(details.toString());
            }
        }
    }

    private void handleAddPolicy() {
        String timeText = timeField.getText().trim();
        String refundText = refundField.getText().trim();
        String description = descriptionArea.getText().trim();
        
        if (timeText.isEmpty() || refundText.isEmpty() || description.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }
        
        try {
            int timeBeforeDeparture = Integer.parseInt(timeText);
            double refundAmount = Double.parseDouble(refundText);
            
            if (timeBeforeDeparture < 0) {
                showError("Time before departure must be positive");
                return;
            }
            
            if (refundAmount < 0 || refundAmount > 100) {
                showError("Refund amount must be between 0 and 100");
                return;
            }
            
            if (policyCatalog.getPolicyByTimeFrame(timeBeforeDeparture) != null) {
                showError("A policy already exists for " + timeBeforeDeparture + " hours time frame");
                return;
            }
            
            String policyID = IDGenerator.generatePolicyID();
            CancellationPolicy newPolicy = new CancellationPolicy(policyID, refundAmount, timeBeforeDeparture, description);
            
            if (policyCatalog.addToCatalog(newPolicy)) {
                showSuccess("Cancellation policy added successfully!\nPolicy ID: " + policyID);
                handleClear();
                loadPoliciesData();
            } else {
                showError("Failed to add policy. A policy for this time frame may already exist.");
            }
            
        } catch (NumberFormatException e) {
            showError("Invalid number format");
        }
    }

    private void handleUpdatePolicy() {
        CancellationPolicy selectedPolicy = policyTable.getSelectionModel().getSelectedItem();
        
        if (selectedPolicy == null) {
            showError("Please select a policy to update");
            return;
        }
        
        String timeText = timeField.getText().trim();
        String refundText = refundField.getText().trim();
        String description = descriptionArea.getText().trim();
        
        if (timeText.isEmpty() || refundText.isEmpty() || description.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }
        
        try {
            int timeBeforeDeparture = Integer.parseInt(timeText);
            double refundAmount = Double.parseDouble(refundText);
            
            if (timeBeforeDeparture < 0) {
                showError("Time before departure must be positive");
                return;
            }
            
            if (refundAmount < 0 || refundAmount > 100) {
                showError("Refund amount must be between 0 and 100");
                return;
            }
            
            // Check if another policy already has this time frame
            CancellationPolicy existingPolicy = policyCatalog.getPolicyByTimeFrame(timeBeforeDeparture);
            if (existingPolicy != null && !existingPolicy.getPolicyID().equals(selectedPolicy.getPolicyID())) {
                showError("Another policy already exists for " + timeBeforeDeparture + " hours time frame");
                return;
            }
            
            selectedPolicy.setTimeBeforeDeparture(timeBeforeDeparture);
            selectedPolicy.setAmountToBeRefunded(refundAmount);
            selectedPolicy.setDescription(description);
            
            if (policyCatalog.updatePolicy(selectedPolicy)) {
                showSuccess("Policy updated successfully!");
                loadPoliciesData();
            } else {
                showError("Failed to update policy");
            }
            
        } catch (NumberFormatException e) {
            showError("Invalid number format");
        }
    }

    private void handleDeletePolicy() {
        CancellationPolicy selectedPolicy = policyTable.getSelectionModel().getSelectedItem();
        
        if (selectedPolicy == null) {
            showError("Please select a policy to delete");
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Cancellation Policy");
        confirmation.setContentText("Are you sure you want to delete this policy?\n\n" +
                                   "Time Frame: " + selectedPolicy.getTimeBeforeDeparture() + " hours\n" +
                                   "Refund Amount: " + selectedPolicy.getAmountToBeRefunded() + "%\n" +
                                   "Description: " + selectedPolicy.getDescription());
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (policyCatalog.deletePolicy(selectedPolicy.getPolicyID())) {
                    showSuccess("Policy deleted successfully!");
                    handleClear();
                    loadPoliciesData();
                } else {
                    showError("Failed to delete policy");
                }
            }
        });
    }

    private void handleRefresh() {
        loadPoliciesData();
        showSuccess("Policies refreshed successfully");
    }

    private void handleClear() {
        timeField.clear();
        refundField.clear();
        descriptionArea.clear();
        if (policyDetailsArea != null) {
            policyDetailsArea.clear();
        }
        policyTable.getSelectionModel().clearSelection();
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