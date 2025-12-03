package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import models.Customer;
import models.Notification;
import config.DatabaseConfig;
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
    
    private Customer currentCustomer;
    private List<Notification> notifications = new ArrayList<>();
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupEventHandlers();
    }
    
    public void setUserData(String username, Customer customer) {
        this.currentCustomer = customer;
        loadNotifications();
    }
    
    private void setupUI() {
        filterCombo.getItems().addAll("All Notifications", "Unread Only", "Booking Related", "Payment Related", "System");
        filterCombo.setValue("All Notifications");
        
        if (backButton != null) {
            backButton.setOnAction(e -> goBackToDashboard());
        }
    }
    
    private void setupEventHandlers() {
        if (markAllReadButton != null) {
            markAllReadButton.setOnAction(e -> markAllAsRead());
        }
        
        if (refreshButton != null) {
            refreshButton.setOnAction(e -> loadNotifications());
        }
        
        filterCombo.valueProperty().addListener((obs, oldVal, newVal) -> filterNotifications());
    }
    
    private void loadNotifications() {
        setLoading(true);
        
        // Load notifications in background thread
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
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            if (currentCustomer == null || currentCustomer.getUserID() == null) {
                System.err.println("Customer or UserID is null");
                return notificationsList;
            }
            
            stmt.setString(1, currentCustomer.getUserID());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                try {
                    Notification notification = new Notification(
                        rs.getInt("NotificationID"),
                        rs.getString("UserID"),
                        rs.getString("Title"),
                        rs.getString("Message"),
                        rs.getString("Type"),
                        rs.getBoolean("IsRead"),
                        rs.getTimestamp("CreatedAt"),
                        rs.getString("RelatedID")
                    );
                    notificationsList.add(notification);
                } catch (Exception e) {
                    System.err.println("Error creating notification from ResultSet: " + e.getMessage());
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching notifications: " + e.getMessage());
            e.printStackTrace();
        }
        
        return notificationsList;
    }
    
    private void displayNotifications(List<Notification> notificationsToDisplay) {
        notificationsContainer.getChildren().clear();
        
        if (notificationsToDisplay == null || notificationsToDisplay.isEmpty()) {
            Label noNotifications = new Label("📭 No notifications to display.");
            noNotifications.getStyleClass().add("empty-label");
            noNotifications.setAlignment(Pos.CENTER);
            noNotifications.setPadding(new Insets(40));
            notificationsContainer.getChildren().add(noNotifications);
            return;
        }
        
        for (Notification notification : notificationsToDisplay) {
            VBox notificationCard = createNotificationCard(notification);
            notificationsContainer.getChildren().add(notificationCard);
        }
    }
    
    private VBox createNotificationCard(Notification notification) {
        VBox card = new VBox(10);
        card.getStyleClass().add("notification-card");
        
        if (notification != null && !notification.isRead()) {
            card.getStyleClass().add("unread");
        }
        
        // Header with title and time
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(notification != null ? notification.getTitle() : "Notification");
        titleLabel.getStyleClass().add("notification-title");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label timeLabel = new Label(notification != null ? formatTime(notification.getCreatedAt()) : "Recently");
        timeLabel.getStyleClass().add("notification-time");
        
        header.getChildren().addAll(titleLabel, spacer, timeLabel);
        
        // Message
        Label messageLabel = new Label(notification != null ? notification.getMessage() : "");
        messageLabel.getStyleClass().add("notification-message");
        messageLabel.setWrapText(true);
        
        // Footer with type and actions
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        
        if (notification != null) {
            Label typeLabel = new Label(notification.getType().toString());
            typeLabel.getStyleClass().add("notification-type-" + notification.getType().toString().toLowerCase());
            
            HBox actionButtons = new HBox(10);
            actionButtons.setAlignment(Pos.CENTER_RIGHT);
            
            if (!notification.isRead()) {
                Button markReadBtn = new Button("Mark as Read");
                markReadBtn.getStyleClass().add("btn-small");
                markReadBtn.setOnAction(e -> markAsRead(notification, card));
                actionButtons.getChildren().add(markReadBtn);
            }
            
            Region footerSpacer = new Region();
            HBox.setHgrow(footerSpacer, Priority.ALWAYS);
            
            footer.getChildren().addAll(typeLabel, footerSpacer, actionButtons);
            
            // Click to mark as read
            card.setOnMouseClicked(e -> {
                if (!notification.isRead()) {
                    markAsRead(notification, card);
                }
            });
        }
        
        card.getChildren().addAll(header, messageLabel, footer);
        
        return card;
    }
    
    private String formatTime(Date date) {
        if (date == null) return "Recently";
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime notificationTime = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            
            long minutes = java.time.Duration.between(notificationTime, now).toMinutes();
            
            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + " minutes ago";
            
            long hours = minutes / 60;
            if (hours < 24) return hours + " hours ago";
            
            long days = hours / 24;
            if (days < 7) return days + " days ago";
            
            return notificationTime.format(DATE_FORMAT);
        } catch (Exception e) {
            return "Recently";
        }
    }
    
    private void markAsRead(Notification notification, VBox card) {
        if (notification == null) return;
        
        boolean success = updateNotificationReadStatus(notification.getNotificationID(), true);
        if (success) {
            notification.setRead(true);
            card.getStyleClass().remove("unread");
            
            // Refresh the display
            filterNotifications();
        }
    }
    
    private void markAllAsRead() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Mark All as Read");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("This will mark all notifications as read.");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = markAllNotificationsAsRead();
            if (success) {
                // Update local notifications
                for (Notification notification : notifications) {
                    notification.setRead(true);
                }
                // Refresh display
                filterNotifications();
            }
        }
    }
    
    private boolean markAllNotificationsAsRead() {
        if (currentCustomer == null || currentCustomer.getUserID() == null) {
            System.err.println("Customer or UserID is null");
            return false;
        }
        
        String query = "UPDATE Notifications SET IsRead = 1 WHERE UserID = ? AND IsRead = 0";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, currentCustomer.getUserID());
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Marked " + rowsAffected + " notifications as read");
            return rowsAffected >= 0;
            
        } catch (SQLException e) {
            System.err.println("Error marking all as read: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean updateNotificationReadStatus(int notificationId, boolean isRead) {
        String query = "UPDATE Notifications SET IsRead = ? WHERE NotificationID = ?";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setBoolean(1, isRead);
            stmt.setInt(2, notificationId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating notification status: " + e.getMessage());
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
        
        switch (filter) {
            case "Unread Only":
                for (Notification n : notifications) {
                    if (n != null && !n.isRead()) filtered.add(n);
                }
                break;
                
            case "Booking Related":
                for (Notification n : notifications) {
                    if (n != null && n.getType() != null && 
                        n.getType().toString().equalsIgnoreCase("booking")) {
                        filtered.add(n);
                    }
                }
                break;
                
            case "Payment Related":
                for (Notification n : notifications) {
                    if (n != null && n.getType() != null && 
                        n.getType().toString().equalsIgnoreCase("payment")) {
                        filtered.add(n);
                    }
                }
                break;
                
            case "System":
                for (Notification n : notifications) {
                    if (n != null && n.getType() != null && 
                        n.getType().toString().equalsIgnoreCase("system")) {
                        filtered.add(n);
                    }
                }
                break;
        }
        
        displayNotifications(filtered);
    }
    
    private void goBackToDashboard() {
        // This will be handled by the dashboard navigation
        // The dashboard controller will show home content
    }
    
    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        notificationsContainer.setVisible(!loading);
        markAllReadButton.setDisable(loading);
        refreshButton.setDisable(loading);
        filterCombo.setDisable(loading);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}