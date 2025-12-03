package catalogs;

import models.Notification;
import config.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationCatalog {
    private static NotificationCatalog instance;
    
    private NotificationCatalog() {
        // Private constructor for singleton
    }
    
    public static synchronized NotificationCatalog getInstance() {
        if (instance == null) {
            instance = new NotificationCatalog();
        }
        return instance;
    }
    
    // Add a notification
    public boolean addNotification(Notification notification) {
        String query = "INSERT INTO Notifications (UserID, Title, Message, Type, RelatedID) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, notification.getUserID());
            stmt.setString(2, notification.getTitle());
            stmt.setString(3, notification.getMessage());
            stmt.setString(4, notification.getTypeString());
            stmt.setString(5, notification.getRelatedID());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error adding notification: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Get notifications for a user
    public List<Notification> getNotificationsByUser(String userID) {
        return getNotificationsByUser(userID, 50); // Default limit 50
    }
    
    public List<Notification> getNotificationsByUser(String userID, int limit) {
        List<Notification> notifications = new ArrayList<>();
        String query = "SELECT TOP (?) * FROM Notifications WHERE UserID = ? ORDER BY CreatedAt DESC";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, limit);
            stmt.setString(2, userID);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching notifications: " + e.getMessage());
            e.printStackTrace();
        }
        
        return notifications;
    }
    
    // Get unread notifications count for a user
    public int getUnreadCount(String userID) {
        String query = "SELECT COUNT(*) as count FROM Notifications WHERE UserID = ? AND IsRead = 0";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, userID);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count");
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting unread count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    // Mark notification as read
    public boolean markAsRead(int notificationID) {
        String query = "UPDATE Notifications SET IsRead = 1 WHERE NotificationID = ?";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, notificationID);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error marking notification as read: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Mark all notifications as read for a user
    public boolean markAllAsRead(String userID) {
        String query = "UPDATE Notifications SET IsRead = 1 WHERE UserID = ?";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, userID);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Marked " + rowsAffected + " notifications as read for user " + userID);
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error marking all notifications as read: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Delete a notification
    public boolean deleteNotification(int notificationID) {
        String query = "DELETE FROM Notifications WHERE NotificationID = ?";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, notificationID);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting notification: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Delete all notifications for a user
    public boolean deleteAllNotifications(String userID) {
        String query = "DELETE FROM Notifications WHERE UserID = ?";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, userID);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Deleted " + rowsAffected + " notifications for user " + userID);
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting all notifications: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Get notifications by type
    public List<Notification> getNotificationsByType(String userID, String type) {
        List<Notification> notifications = new ArrayList<>();
        String query = "SELECT TOP 20 * FROM Notifications WHERE UserID = ? AND Type = ? ORDER BY CreatedAt DESC";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, userID);
            stmt.setString(2, type);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching notifications by type: " + e.getMessage());
            e.printStackTrace();
        }
        
        return notifications;
    }
    
    // Get recent notifications (last 7 days)
    public List<Notification> getRecentNotifications(String userID, int days) {
        List<Notification> notifications = new ArrayList<>();
        String query = "SELECT * FROM Notifications WHERE UserID = ? AND CreatedAt >= DATEADD(day, -?, GETDATE()) ORDER BY CreatedAt DESC";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), 
                DatabaseConfig.getDbUser(), 
                DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, userID);
            stmt.setInt(2, days);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching recent notifications: " + e.getMessage());
            e.printStackTrace();
        }
        
        return notifications;
    }
    
    // Map ResultSet to Notification object
    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        return new Notification(
            rs.getInt("NotificationID"),
            rs.getString("UserID"),
            rs.getString("Title"),
            rs.getString("Message"),
            rs.getString("Type"),
            rs.getBoolean("IsRead"),
            rs.getTimestamp("CreatedAt"),
            rs.getString("RelatedID")
        );
    }
}