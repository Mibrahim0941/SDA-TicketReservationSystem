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
import models.PromotionalCode;
import catalogs.PromoCodeCatalog;
import java.io.IOException;
import java.time.LocalDate;

public class ManagePromoCodesController {
    private PromoCodeCatalog promoCatalog = new PromoCodeCatalog();
    private String currentUsername;
    private Admin currentAdmin;

    @FXML private Text welcomeTitle;
    @FXML private Text userGreeting;
    @FXML private TableView<PromotionalCode> promoTable;
    @FXML private TableColumn<PromotionalCode, String> codeColumn;
    @FXML private TableColumn<PromotionalCode, LocalDate> validityColumn;
    @FXML private TableColumn<PromotionalCode, Double> percentageColumn;
    @FXML private TableColumn<PromotionalCode, Boolean> statusColumn;
    @FXML private TextField codeField;
    @FXML private DatePicker validityField;
    @FXML private TextField percentageField;
    @FXML private Button addPromoButton;
    @FXML private Button toggleStatusButton;
    @FXML private Button deletePromoButton;
    @FXML private Button refreshButton;
    @FXML private Button clearButton;
    @FXML private Button backButton;
    @FXML private TextArea promoDetailsArea;

    public static void show(Stage stage, String username, Admin admin) {
        try {
            FXMLLoader loader = new FXMLLoader(ManagePromoCodesController.class.getResource("/ui/manage-promo-codes.fxml"));
            Parent root = loader.load();
            ManagePromoCodesController controller = loader.getController();
            controller.setAdminData(username, admin);
            Scene scene = new Scene(root, 1200, 800);
            try {
                scene.getStylesheets().add(ManagePromoCodesController.class.getResource("/ui/manage-promocodes.css").toExternalForm());
            } catch (NullPointerException e) {
                System.out.println("Stylesheet not found, continuing without it");
            }
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Manage Promo Codes");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to load promo codes management page: " + e.getMessage());
        }
    }

    public void setAdminData(String username, Admin admin) {
        this.currentUsername = username;
        this.currentAdmin = admin;
        if (userGreeting != null) {
            userGreeting.setText("Manage Promo Codes - " + username);
        }
        Platform.runLater(this::loadPromoCodesData);
    }

    @FXML
    public void initialize() {
        System.out.println("ManagePromoCodesController initialized");
        setupEventHandlers();
        initializeTable();
    }

    private void setupEventHandlers() {
        if (addPromoButton != null) addPromoButton.setOnAction(e -> handleAddPromo());
        if (toggleStatusButton != null) toggleStatusButton.setOnAction(e -> handleToggleStatus());
        if (deletePromoButton != null) deletePromoButton.setOnAction(e -> handleDeletePromo());
        if (refreshButton != null) refreshButton.setOnAction(e -> handleRefresh());
        if (clearButton != null) clearButton.setOnAction(e -> handleClear());
        if (backButton != null) backButton.setOnAction(e -> handleBack());
    }

    private void initializeTable() {
        if (promoTable != null) {
            codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
            validityColumn.setCellValueFactory(new PropertyValueFactory<>("validity"));
            percentageColumn.setCellValueFactory(new PropertyValueFactory<>("percentage"));
            statusColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
            
            statusColumn.setCellFactory(column -> new TableCell<PromotionalCode, Boolean>() {
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item ? "Active" : "Inactive");
                        setStyle(item ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;" 
                                      : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            });
            
            percentageColumn.setCellFactory(column -> new TableCell<PromotionalCode, Double>() {
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
            
            promoTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> showPromoDetails(newSelection)
            );
        }
    }

    private void loadPromoCodesData() {
        if (promoCatalog != null && promoTable != null) {
            try {
                promoCatalog.refresh();
                promoTable.setItems(FXCollections.observableArrayList(promoCatalog.getAllPromoCodes()));
                System.out.println("Loaded " + promoCatalog.getAllPromoCodes().size() + " promo codes");
            } catch (Exception e) {
                System.err.println("Error loading promo codes: " + e.getMessage());
                showError("Failed to load promo codes: " + e.getMessage());
            }
        }
    }

    private void showPromoDetails(PromotionalCode promo) {
        if (promo != null) {
            codeField.setText(promo.getCode());
            validityField.setValue(promo.getValidity());
            percentageField.setText(String.valueOf(promo.getPercentage()));
            
            if (promoDetailsArea != null) {
                StringBuilder details = new StringBuilder();
                details.append("=== PROMO CODE DETAILS ===\n\n");
                details.append("Code: ").append(promo.getCode()).append("\n");
                details.append("Discount: ").append(String.format("%.1f%%", promo.getPercentage())).append("\n");
                details.append("Valid Until: ").append(promo.getValidity()).append("\n");
                details.append("Status: ").append(promo.isActive() ? "✓ Active" : "✗ Inactive").append("\n");
                details.append("Currently Valid: ").append(promo.checkValidity() ? "✓ Yes" : "✗ Expired").append("\n\n");
                
                if (!promo.checkValidity()) {
                    details.append("⚠ This promo code has expired.\n");
                } else if (promo.isActive()) {
                    details.append("✓ This promo code is active and can be used.\n");
                } else {
                    details.append("⚠ This promo code is inactive.\n");
                }
                promoDetailsArea.setText(details.toString());
            }
        }
    }

    private void handleAddPromo() {
        String code = codeField.getText().trim().toUpperCase();
        LocalDate validity = validityField.getValue();
        String percentageText = percentageField.getText().trim();
        
        if (code.isEmpty() || validity == null || percentageText.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }
        
        if (code.length() < 3) {
            showError("Promo code must be at least 3 characters long");
            return;
        }
        
        try {
            double percentage = Double.parseDouble(percentageText);
            
            if (percentage <= 0 || percentage > 100) {
                showError("Percentage must be between 1 and 100");
                return;
            }
            
            if (validity.isBefore(LocalDate.now())) {
                showError("Validity date must be in the future");
                return;
            }
            
            PromotionalCode newPromo = new PromotionalCode(code, validity, percentage);
            
            if (promoCatalog.addToCatalog(newPromo)) {
                showSuccess("Promo code '" + code + "' added successfully!");
                handleClear();
                loadPromoCodesData();
            } else {
                showError("Failed to add promo code. Code '" + code + "' may already exist.");
            }
        } catch (NumberFormatException e) {
            showError("Invalid percentage format. Please enter a valid number.");
        }
    }

    private void handleToggleStatus() {
        PromotionalCode selectedPromo = promoTable.getSelectionModel().getSelectedItem();
        
        if (selectedPromo == null) {
            showError("Please select a promo code");
            return;
        }
        
        boolean newStatus = !selectedPromo.isActive();
        selectedPromo.setActive(newStatus);
        
        if (promoCatalog.updatePromoCode(selectedPromo)) {
            promoTable.refresh();
            showPromoDetails(selectedPromo);
            showSuccess("Promo code '" + selectedPromo.getCode() + "' is now " + 
                       (newStatus ? "ACTIVE" : "INACTIVE"));
        } else {
            selectedPromo.setActive(!newStatus);
            showError("Failed to update promo code status");
        }
    }

    private void handleDeletePromo() {
        PromotionalCode selectedPromo = promoTable.getSelectionModel().getSelectedItem();
        
        if (selectedPromo == null) {
            showError("Please select a promo code to delete");
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Promo Code");
        confirmation.setContentText("Are you sure you want to delete promo code: " + 
                                   selectedPromo.getCode() + "?\n\nDiscount: " + 
                                   selectedPromo.getPercentage() + "%\nValid Until: " + 
                                   selectedPromo.getValidity());
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (promoCatalog.deletePromoCode(selectedPromo.getCode())) {
                    showSuccess("Promo code '" + selectedPromo.getCode() + "' deleted successfully!");
                    handleClear();
                    loadPromoCodesData();
                } else {
                    showError("Failed to delete promo code");
                }
            }
        });
    }

    private void handleRefresh() {
        loadPromoCodesData();
        showSuccess("Promo codes refreshed successfully");
    }

    private void handleClear() {
        codeField.clear();
        validityField.setValue(null);
        percentageField.clear();
        if (promoDetailsArea != null) {
            promoDetailsArea.clear();
        }
        promoTable.getSelectionModel().clearSelection();
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