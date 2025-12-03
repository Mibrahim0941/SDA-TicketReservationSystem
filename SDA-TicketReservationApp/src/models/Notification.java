package models;

import java.util.Date;

public class Notification {
    private int notificationID;
    private String userID;
    private String title;
    private String message;
    private NotificationType type;
    private boolean isRead;
    private Date createdAt;
    private String relatedID; // BookingID, PaymentID, etc.
    
    // Enum for notification types
    public enum NotificationType {
        BOOKING("booking"),
        PAYMENT("payment"),
        REMINDER("reminder"),
        SYSTEM("system"),
        PROMOTION("promotion");
        
        private final String value;
        
        NotificationType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static NotificationType fromString(String text) {
            for (NotificationType type : NotificationType.values()) {
                if (type.value.equalsIgnoreCase(text)) {
                    return type;
                }
            }
            return SYSTEM; // Default
        }
    }
    
    // Constructor for creating new notifications
    public Notification(String userID, String title, String message, NotificationType type) {
        this.userID = userID;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = false;
        this.createdAt = new Date();
    }
    
    // Constructor for loading from database
    public Notification(int notificationID, String userID, String title, String message, 
                       String type, boolean isRead, Date createdAt, String relatedID) {
        this.notificationID = notificationID;
        this.userID = userID;
        this.title = title;
        this.message = message;
        this.type = NotificationType.fromString(type);
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.relatedID = relatedID;
    }
    
    // Full constructor
    public Notification(int notificationID, String userID, String title, String message, 
                       NotificationType type, boolean isRead, Date createdAt, String relatedID) {
        this.notificationID = notificationID;
        this.userID = userID;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.relatedID = relatedID;
    }
    
    // Getters and Setters
    public int getNotificationID() { 
        return notificationID; 
    }
    
    public void setNotificationID(int notificationID) { 
        this.notificationID = notificationID; 
    }
    
    public String getUserID() { 
        return userID; 
    }
    
    public void setUserID(String userID) { 
        this.userID = userID; 
    }
    
    public String getTitle() { 
        return title; 
    }
    
    public void setTitle(String title) { 
        this.title = title; 
    }
    
    public String getMessage() { 
        return message; 
    }
    
    public void setMessage(String message) { 
        this.message = message; 
    }
    
    public NotificationType getType() { 
        return type; 
    }
    
    public void setType(NotificationType type) { 
        this.type = type; 
    }
    
    // For database compatibility
    public String getTypeString() {
        return type.getValue();
    }
    
    public boolean isRead() { 
        return isRead; 
    }
    
    public void setRead(boolean read) { 
        isRead = read; 
    }
    
    public Date getCreatedAt() { 
        return createdAt; 
    }
    
    public void setCreatedAt(Date createdAt) { 
        this.createdAt = createdAt; 
    }
    
    public String getRelatedID() { 
        return relatedID; 
    }
    
    public void setRelatedID(String relatedID) { 
        this.relatedID = relatedID; 
    }
    
    // Helper method to get formatted time
    public String getFormattedTime() {
        if (createdAt == null) return "Recently";
        
        long diff = System.currentTimeMillis() - createdAt.getTime();
        long minutes = diff / (1000 * 60);
        
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minutes ago";
        
        long hours = minutes / 60;
        if (hours < 24) return hours + " hours ago";
        
        long days = hours / 24;
        if (days < 7) return days + " days ago";
        
        // For older notifications, show date
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy");
        return sdf.format(createdAt);
    }
    
    // Helper method to get CSS style class based on type
    public String getStyleClass() {
        switch (type) {
            case BOOKING:
                return "notification-type-booking";
            case PAYMENT:
                return "notification-type-payment";
            case REMINDER:
                return "notification-type-reminder";
            case SYSTEM:
                return "notification-type-system";
            case PROMOTION:
                return "notification-type-promotion";
            default:
                return "notification-type-system";
        }
    }
    
    // Helper method to get icon based on type
    public String getIcon() {
        switch (type) {
            case BOOKING:
                return "🎫";
            case PAYMENT:
                return "💳";
            case REMINDER:
                return "⏰";
            case SYSTEM:
                return "📢";
            case PROMOTION:
                return "🎁";
            default:
                return "📢";
        }
    }
    
    @Override
    public String toString() {
        return String.format("Notification{id=%d, user='%s', title='%s', type='%s', read=%s}",
            notificationID, userID, title, type, isRead);
    }
    
    // Factory methods for common notification types
    
    public static Notification createBookingNotification(String userID, String bookingId, String route) {
        String title = "🎫 Booking Confirmed!";
        String message = String.format(
            "Your booking #%s for %s has been confirmed. Please complete payment from 'My Bookings' page.",
            bookingId, route
        );
        return new Notification(userID, title, message, NotificationType.BOOKING);
    }
    
    public static Notification createPaymentNotification(String userID, String bookingId, double amount) {
        String title = "💳 Payment Successful!";
        String message = String.format(
            "Payment of PKR %.2f for booking #%s has been processed. Your booking is now confirmed.",
            amount, bookingId
        );
        return new Notification(userID, title, message, NotificationType.PAYMENT);
    }
    
    public static Notification createDepartureReminder(String userID, String bookingId, String route, String departureTime) {
        String title = "⏰ Departure Reminder!";
        String message = String.format(
            "Your trip for %s is departing soon (%s). Please arrive at the station at least 30 minutes before departure.",
            route, departureTime
        );
        return new Notification(userID, title, message, NotificationType.REMINDER);
    }
    
    public static Notification createCancellationNotification(String userID, String bookingId) {
        String title = "❌ Booking Cancelled";
        String message = String.format(
            "Your booking #%s has been cancelled. Seats have been released. Refund will be processed within 3-5 business days.",
            bookingId
        );
        return new Notification(userID, title, message, NotificationType.BOOKING);
    }
    
    public static Notification createWelcomeNotification(String userID) {
        String title = "🎉 Welcome to TicketGenie!";
        String message = "Thank you for registering with TicketGenie. Enjoy seamless bus ticket booking experience.";
        return new Notification(userID, title, message, NotificationType.SYSTEM);
    }
    
    public static Notification createPromotionNotification(String userID, String code, double discount) {
        String title = "🎁 Special Offer!";
        String message = String.format(
            "Use promo code %s to get %.0f%% off on your next booking. Valid for 7 days!",
            code, discount
        );
        return new Notification(userID, title, message, NotificationType.PROMOTION);
    }
}