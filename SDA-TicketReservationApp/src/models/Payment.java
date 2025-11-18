package models;

import java.util.Date;

public class Payment {
    private String paymentID;
    private Booking booking; // Reference to booking instead of bookingID
    private String status; // Pending, Completed, Failed, Refunded
    private Date confirmedOn;
    private double amount;
    private String paymentMethod;

    public Payment(String paymentID, Booking booking, double amount, String paymentMethod) {
        this.paymentID = paymentID;
        this.booking = booking;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = "Pending";
        this.confirmedOn = null;
    }

    // Getters and Setters
    public String getPaymentID() {
        return paymentID;
    }

    public Booking getBooking() {
        return booking;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getConfirmedOn() {
        return confirmedOn;
    }

    public void setConfirmedOn(Date confirmedOn) {
        this.confirmedOn = confirmedOn;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    // Methods from UML diagram
    public double calculateTotal(double taxRate) {
        return amount * (1 + taxRate);
    }

    public boolean initiatePayment() {
        System.out.println("Initiating payment for booking: " + booking.getBookingID());
        this.status = "Processing";
        return true;
    }

    public boolean confirmPayment() {
        System.out.println("Confirming payment for booking: " + booking.getBookingID());
        this.status = "Completed";
        this.confirmedOn = new Date();
        return true;
    }

    public boolean applyPromoCodes(String promoCode) {
        System.out.println("Applying promo code: " + promoCode);
        // Simulate promo code application - 10% discount for demo
        if ("WELCOME10".equals(promoCode)) {
            this.amount = this.amount * 0.9;
            return true;
        }
        return false;
    }

    public boolean isPaid() {
        return "Completed".equals(status);
    }

    public boolean isPending() {
        return "Pending".equals(status) || "Processing".equals(status);
    }

    public boolean isFailed() {
        return "Failed".equals(status);
    }
}