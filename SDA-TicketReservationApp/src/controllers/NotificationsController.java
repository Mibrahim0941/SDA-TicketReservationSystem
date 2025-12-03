package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Customer;
import models.Notification;
import config.DatabaseConfig;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;

public class NotificationsController implements Initializable {

    @FXML private Text pageTitle;
    @FXML private Text subTitle; 
    @FXML private Button backButton;
    @FXML private VBox notificationsContainer;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Button markAllReadButton;
    @FXML private Button refreshButton;
    @FXML private ComboBox<String> filterCombo;
    
    private String currentUsername; 
    private Customer currentCustomer;
    private List<Notification> notifications = new ArrayList<>();
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
    public static void show(Stage stage, String username, Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(NotificationsController.class.getResource("/ui/notifications.fxml"));
            Parent root = loader.load();
            
            NotificationsController controller = loader.getController();
            controller.setUserData(username, customer);
            
            Scene scene = new Scene(root, 1000, 700);
            URL stylesheet = NotificationsController.class.getResource("/ui/notifications.css");
            if (stylesheet != null) {
                scene.getStylesheets().add(stylesheet.toExternalForm());
            }
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Notifications");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            showErrorAlert("Failed to load Notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupEventHandlers();
    }
    
    public void setUserData(String username, Customer customer) {
        this.currentUsername = username;
        this.currentCustomer = customer;
        loadNotifications();
    }
    
    private void setupUI() {
        filterCombo.getItems().addAll("All Notifications", "Unread Only", "Booking Related", "Payment Related", "System");
        filterCombo.setValue("All Notifications");
    }
    
    private void setupEventHandlers() {
        if (backButton != null) {
            backButton.setOnAction(e -> goBackToDashboard());
        }
        if (markAllReadButton != null) {
            markAllReadButton.setOnAction(e -> markAllAsRead());
        }
        if (refreshButton != null) {
            refreshButton.setOnAction(e -> loadNotifications());
        }
        filterCombo.valueProperty().addListener((obs, oldVal, newVal) -> filterNotifications());
    }
    
    private void goBackToDashboard() {
        try {
            Stage currentStage = (Stage) backButton.getScene().getWindow();
            DashboardController.show(currentStage, currentUsername, currentCustomer);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error navigating back to dashboard");
        }
    }

    private void loadNotifications() {
        setLoading(true);
        new Thread(() -> {
            try {
                List<Notification> loadedNotifications = fetchNotificationsFromDatabase();
                javafx.application.Platform.runLater(() -> {
                    notifications = loadedNotifications;
                    displayNotifications(notifications);
                    setLoading(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    showAlert("Error", "Failed to load notifications: " + e.getMessage());
                    setLoading(false);
                });
            }
        }).start();
    }
    
    private List<Notification> fetchNotificationsFromDatabase() {
        List<Notification> notificationsList = new ArrayList<>();
        String query = "SELECT * FROM Notifications WHERE UserID = ? ORDER BY CreatedAt DESC";
        
        try (Connection conn = DriverManager.getConnection(DatabaseConfig.getDbUrl(), DatabaseConfig.getDbUser(), DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            if (currentCustomer == null || currentCustomer.getUserID() == null) {
                return notificationsList;
            }
            
            stmt.setString(1, currentCustomer.getUserID());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                notificationsList.add(new Notification(
                    rs.getInt("NotificationID"),
                    rs.getString("UserID"),
                    rs.getString("Title"),
                    rs.getString("Message"),
                    rs.getString("Type"),
                    rs.getBoolean("IsRead"),
                    rs.getTimestamp("CreatedAt"),
                    rs.getString("RelatedID")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notificationsList;
    }

    private VBox createNotificationCard(Notification notification) {
        VBox card = new VBox(15);
        card.getStyleClass().add("notification-card");
        
        if (notification != null && !notification.isRead()) {
            card.getStyleClass().add("unread");
        }
        
        HBox header = new HBox(10);
        header.getStyleClass().add("notification-header");
        
        Label titleLabel = new Label(notification != null ? notification.getTitle() : "Notification");
        titleLabel.getStyleClass().add("notification-title");
        titleLabel.setWrapText(true);
        
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        HBox rightSection = new HBox(10);
        rightSection.setAlignment(Pos.CENTER_RIGHT);
        
        Label timeLabel = new Label(notification != null ? formatTime(notification.getCreatedAt()) : "Recently");
        timeLabel.getStyleClass().add("notification-time");
        
        if (notification != null) {
            Label typeLabel = new Label(notification.getType().toString());
            typeLabel.getStyleClass().addAll("notification-type-badge", 
                "notification-type-" + notification.getType().toString().toLowerCase());
            rightSection.getChildren().addAll(typeLabel, timeLabel);
        } else {
            rightSection.getChildren().add(timeLabel);
        }
        
        header.getChildren().addAll(titleLabel, spacer, rightSection);
        Label messageLabel = new Label(notification != null ? notification.getMessage() : "");
        messageLabel.getStyleClass().add("notification-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        HBox details = new HBox(10);
        details.getStyleClass().add("notification-details");
        
        VBox leftDetails = new VBox(5);
        leftDetails.getStyleClass().add("details-left");
        if (notification != null && notification.getRelatedID() != null && !notification.getRelatedID().isEmpty()) {
            Label relatedLabel = new Label("Related to: " + notification.getRelatedID());
            relatedLabel.getStyleClass().add("related-info");
            leftDetails.getChildren().add(relatedLabel);
        }
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);
        
        if (notification != null && !notification.isRead()) {
            Button markReadBtn = new Button("Mark as Read");
            markReadBtn.getStyleClass().add("btn-primary");
            markReadBtn.setOnAction(e -> markAsRead(notification, card));
            actionButtons.getChildren().add(markReadBtn);
        }
        
        HBox detailSpacer = new HBox();
        HBox.setHgrow(detailSpacer, Priority.ALWAYS);
        details.getChildren().addAll(leftDetails, detailSpacer, actionButtons);
        if (notification != null) {
            card.setOnMouseClicked(e -> {
                if (!notification.isRead()) {
                    markAsRead(notification, card);
                }
            });
        }
        
        card.getChildren().addAll(header, messageLabel, details);
        
        return card;
    }

    private void displayNotifications(List<Notification> notificationsToDisplay) {
        notificationsContainer.getChildren().clear();
        
        if (notificationsToDisplay == null || notificationsToDisplay.isEmpty()) {
            VBox emptyContainer = new VBox();
            emptyContainer.setAlignment(Pos.CENTER);
            emptyContainer.setPadding(new Insets(40));
            
            Label noNotifications = new Label("📭 No notifications to display.");
            noNotifications.getStyleClass().add("empty-label");
            noNotifications.setAlignment(Pos.CENTER);
            
            emptyContainer.getChildren().add(noNotifications);
            notificationsContainer.getChildren().add(emptyContainer);
            return;
        }
        
        for (Notification notification : notificationsToDisplay) {
            VBox notificationCard = createNotificationCard(notification);
            notificationsContainer.getChildren().add(notificationCard);
        }
    }

    private String formatTime(Date date) {
        if (date == null) return "Recently";
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime notificationTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            long minutes = java.time.Duration.between(notificationTime, now).toMinutes();
            
            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + " minutes ago";
            long hours = minutes / 60;
            if (hours < 24) return hours + " hours ago";
            long days = hours / 24;
            if (days < 7) return days + " days ago";
            
            return notificationTime.format(DATE_FORMAT);
        } catch (Exception e) { return "Recently"; }
    }
    
    private void markAsRead(Notification notification, VBox card) {
        if (notification == null) return;
        if (updateNotificationReadStatus(notification.getNotificationID(), true)) {
            notification.setRead(true);
            card.getStyleClass().remove("unread");
        }
    }
    
    private void markAllAsRead() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Mark All as Read");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("This will mark all notifications as read.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (markAllNotificationsAsRead()) {
                    for (Notification n : notifications) n.setRead(true);
                    filterNotifications();
                }
            }
        });
    }
    
    private boolean markAllNotificationsAsRead() {
        String query = "UPDATE Notifications SET IsRead = 1 WHERE UserID = ? AND IsRead = 0";
        try (Connection conn = DriverManager.getConnection(DatabaseConfig.getDbUrl(), DatabaseConfig.getDbUser(), DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, currentCustomer.getUserID());
            return stmt.executeUpdate() >= 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean updateNotificationReadStatus(int notificationId, boolean isRead) {
        String query = "UPDATE Notifications SET IsRead = ? WHERE NotificationID = ?";
        try (Connection conn = DriverManager.getConnection(DatabaseConfig.getDbUrl(), DatabaseConfig.getDbUser(), DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setBoolean(1, isRead);
            stmt.setInt(2, notificationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private void filterNotifications() {
        String filter = filterCombo.getValue();
        if (filter == null || filter.equals("All Notifications")) {
            displayNotifications(notifications);
            return;
        }
        
        List<Notification> filtered = new ArrayList<>();
        for (Notification n : notifications) {
            if (n == null) continue;
            boolean matches = false;
            
            switch (filter) {
                case "Unread Only": matches = !n.isRead(); break;
                case "Booking Related": matches = n.getType().toString().equalsIgnoreCase("booking"); break;
                case "Payment Related": matches = n.getType().toString().equalsIgnoreCase("payment"); break;
                case "System": matches = n.getType().toString().equalsIgnoreCase("system"); break;
            }
            if (matches) filtered.add(n);
        }
        displayNotifications(filtered);
    }
    
    private void setLoading(boolean loading) {
        if(loadingIndicator != null) loadingIndicator.setVisible(loading);
        if(notificationsContainer != null) notificationsContainer.setVisible(!loading);
        if(markAllReadButton != null) markAllReadButton.setDisable(loading);
        if(refreshButton != null) refreshButton.setDisable(loading);
        if(filterCombo != null) filterCombo.setDisable(loading);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private static void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}