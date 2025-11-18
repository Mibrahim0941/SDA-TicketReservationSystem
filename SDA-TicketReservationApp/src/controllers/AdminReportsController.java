package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Admin;

import java.io.IOException;

public class AdminReportsController {

    private String currentUsername;
    private Admin currentAdmin;

    @FXML private Text welcomeTitle;
    @FXML private Text userGreeting;
    
    @FXML private TextArea reportArea;
    
    @FXML private Button generateBookingReportButton;
    @FXML private Button generateRevenueReportButton;
    @FXML private Button generateRouteReportButton;
    @FXML private Button exportReportButton;
    @FXML private Button backButton;

    public static void show(Stage stage, String username, Admin admin) {
        try {
            FXMLLoader loader = new FXMLLoader(AdminReportsController.class.getResource("/ui/admin-reports.fxml"));
            Parent root = loader.load();
            
            AdminReportsController controller = loader.getController();
            controller.setAdminData(username, admin);
            
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(AdminReportsController.class.getResource("/ui/admin-reports.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Reports");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to load reports page: " + e.getMessage());
        }
    }

    public void setAdminData(String username, Admin admin) {
        this.currentUsername = username;
        this.currentAdmin = admin;
        
        if (userGreeting != null) {
            userGreeting.setText("Reports Dashboard - " + username);
        }
    }

    @FXML
    public void initialize() {
        System.out.println("AdminReportsController initialized");
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        if (generateBookingReportButton != null) {
            generateBookingReportButton.setOnAction(e -> generateBookingReport());
        }
        if (generateRevenueReportButton != null) {
            generateRevenueReportButton.setOnAction(e -> generateRevenueReport());
        }
        if (generateRouteReportButton != null) {
            generateRouteReportButton.setOnAction(e -> generateRouteReport());
        }
        if (exportReportButton != null) {
            exportReportButton.setOnAction(e -> exportReport());
        }
        if (backButton != null) {
            backButton.setOnAction(e -> handleBack());
        }
    }

    private void generateBookingReport() {
        if (reportArea != null) {
            StringBuilder report = new StringBuilder();
            report.append("=== BOOKING STATISTICS REPORT ===\n\n");
            report.append("Report Generated: ").append(java.time.LocalDateTime.now()).append("\n\n");
            report.append("Total Bookings: 1,234\n");
            report.append("Confirmed Bookings: 1,100\n");
            report.append("Cancelled Bookings: 134\n");
            report.append("Pending Bookings: 25\n\n");
            report.append("This Week: 234 bookings\n");
            report.append("This Month: 890 bookings\n");
            report.append("This Year: 5,678 bookings\n\n");
            report.append("Popular Routes:\n");
            report.append("1. Lahore → Karachi: 345 bookings\n");
            report.append("2. Lahore → Islamabad: 289 bookings\n");
            report.append("3. Karachi → Islamabad: 234 bookings\n");
            
            reportArea.setText(report.toString());
        }
        showSuccess("Booking report generated successfully");
    }

    private void generateRevenueReport() {
        if (reportArea != null) {
            StringBuilder report = new StringBuilder();
            report.append("=== REVENUE REPORT ===\n\n");
            report.append("Report Generated: ").append(java.time.LocalDateTime.now()).append("\n\n");
            report.append("Total Revenue: PKR 4,567,890\n");
            report.append("Average Booking Value: PKR 3,702\n\n");
            report.append("Revenue by Period:\n");
            report.append("Today: PKR 45,600\n");
            report.append("This Week: PKR 234,500\n");
            report.append("This Month: PKR 890,000\n");
            report.append("This Year: PKR 4,567,890\n\n");
            report.append("Revenue by Route:\n");
            report.append("1. Lahore → Karachi: PKR 1,207,500\n");
            report.append("2. Lahore → Islamabad: PKR 433,500\n");
            report.append("3. Karachi → Islamabad: PKR 936,000\n");
            
            reportArea.setText(report.toString());
        }
        showSuccess("Revenue report generated successfully");
    }

    private void generateRouteReport() {
        if (reportArea != null) {
            StringBuilder report = new StringBuilder();
            report.append("=== ROUTE PERFORMANCE REPORT ===\n\n");
            report.append("Report Generated: ").append(java.time.LocalDateTime.now()).append("\n\n");
            report.append("Total Active Routes: 25\n");
            report.append("Total Schedules: 156\n\n");
            report.append("Top Performing Routes:\n\n");
            report.append("1. Lahore → Karachi\n");
            report.append("   Base Price: PKR 3,500\n");
            report.append("   Total Bookings: 345\n");
            report.append("   Occupancy Rate: 87%\n");
            report.append("   Revenue: PKR 1,207,500\n\n");
            report.append("2. Lahore → Islamabad\n");
            report.append("   Base Price: PKR 1,500\n");
            report.append("   Total Bookings: 289\n");
            report.append("   Occupancy Rate: 92%\n");
            report.append("   Revenue: PKR 433,500\n\n");
            report.append("3. Karachi → Islamabad\n");
            report.append("   Base Price: PKR 4,000\n");
            report.append("   Total Bookings: 234\n");
            report.append("   Occupancy Rate: 78%\n");
            report.append("   Revenue: PKR 936,000\n");
            
            reportArea.setText(report.toString());
        }
        showSuccess("Route report generated successfully");
    }

    private void exportReport() {
        showInfo("Export Report", "Report export functionality will be available soon.\nYou can copy the report text for now.");
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

    private void showInfo(String title, String message) {
        showAlert(title, message, Alert.AlertType.INFORMATION);
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