package services;

import models.Customer;
import models.Notification;
import catalogs.NotificationCatalog;

public class NotificationService {
    
    private static NotificationService instance;
    private NotificationCatalog notificationCatalog;
    
    private NotificationService() {
        this.notificationCatalog = NotificationCatalog.getInstance();
    }
    
    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }
    
    public void sendBookingSuccessNotification(Customer customer, String bookingId, String route) {
        Notification notification = Notification.createBookingNotification(
            customer.getUserID(), bookingId, route
        );
        notification.setRelatedID(bookingId);
        
        if (notificationCatalog.addNotification(notification)) {
            System.out.println("Booking success notification sent to user: " + customer.getUserID());
        }
    }
    
    public void sendPaymentSuccessNotification(Customer customer, String bookingId, double amount) {
        Notification notification = Notification.createPaymentNotification(
            customer.getUserID(), bookingId, amount
        );
        notification.setRelatedID(bookingId);
        
        if (notificationCatalog.addNotification(notification)) {
            System.out.println("Payment success notification sent to user: " + customer.getUserID());
        }
    }
    
    public void sendDepartureReminder(Customer customer, String bookingId, String route, String departureTime) {
        Notification notification = Notification.createDepartureReminder(
            customer.getUserID(), bookingId, route, departureTime
        );
        notification.setRelatedID(bookingId);
        
        if (notificationCatalog.addNotification(notification)) {
            System.out.println("Departure reminder sent to user: " + customer.getUserID());
        }
    }
    
    public void sendCancellationNotification(Customer customer, String bookingId) {
        Notification notification = Notification.createCancellationNotification(
            customer.getUserID(), bookingId
        );
        notification.setRelatedID(bookingId);
        
        if (notificationCatalog.addNotification(notification)) {
            System.out.println("Cancellation notification sent to user: " + customer.getUserID());
        }
    }
    
    public void sendWelcomeNotification(Customer customer) {
        Notification notification = Notification.createWelcomeNotification(customer.getUserID());
        
        if (notificationCatalog.addNotification(notification)) {
            System.out.println("Welcome notification sent to user: " + customer.getUserID());
        }
    }
    
    public void sendPromotionNotification(Customer customer, String code, double discount) {
        Notification notification = Notification.createPromotionNotification(
            customer.getUserID(), code, discount
        );
        
        if (notificationCatalog.addNotification(notification)) {
            System.out.println("Promotion notification sent to user: " + customer.getUserID());
        }
    }
    
    public void sendSystemNotification(Customer customer, String title, String message) {
        Notification notification = new Notification(
            customer.getUserID(), 
            title, 
            message, 
            Notification.NotificationType.SYSTEM
        );
        
        if (notificationCatalog.addNotification(notification)) {
            System.out.println("System notification sent to user: " + customer.getUserID());
        }
    }
    
    public int getUnreadCount(String userID) {
        return notificationCatalog.getUnreadCount(userID);
    }
    
    public java.util.List<Notification> getRecentNotifications(String userID) {
        return notificationCatalog.getNotificationsByUser(userID, 10); // Last 10 notifications
    }
}