package models;
import java.util.Date;

public class Booking {
    private String bookingID;
    private String customerID;
    private Reservation reservation;
    private Date bookingDateTime;
    private Payment payment;
    private String status; // Confirmed, Cancelled, Completed
    private double totalAmount;

    public Booking(String bookingID, String customerID, Reservation reservation, Date bookingDateTime) {
        this.bookingID = bookingID;
        this.customerID = customerID;
        this.reservation = reservation;
        this.bookingDateTime = bookingDateTime;
        this.payment = null; 
        this.status = "Confirmed";
        this.totalAmount = calculateTotalAmount();
    }

    public String getBookingID() {
        return bookingID;
    }

    public String getCustomerID() {
        return customerID;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public Date getBookingDateTime() {
        return bookingDateTime;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public boolean hasPayment() {
        return payment != null;
    }

    public boolean isPaid() {
        return hasPayment() && payment.isPaid();
    }

    public String getPaymentStatus() {
        if (!hasPayment()) {
            return "Unpaid";
        }
        return payment.getStatus();
    }

    public ETicket generateETicket() {
        if (!isPaid()) {
            throw new IllegalStateException("Cannot generate E-Ticket for unpaid booking");
        }
        String ticketID = "TKT" + bookingID;
        return new ETicket(ticketID, this);
    }

    public void proceedToPayment() {
        if (hasPayment() && payment.isPaid()) {
            System.out.println("Payment already completed for booking: " + bookingID);
            return;
        }

        if (!hasPayment()) {
            this.payment = new Payment("PAY" + bookingID, this, totalAmount, "Credit Card");
        }
        
        payment.initiatePayment();
    }

    public boolean matchCriteria(String filter) {
        return bookingID.contains(filter) || customerID.contains(filter);
    }

    public boolean hasETicket() {
        return isPaid();
    }

    public ETicket getETicket() {
        return generateETicket();
    }

    public void getBookingDetails() {
        System.out.println("Booking ID: " + bookingID);
        System.out.println("Customer ID: " + customerID);
        System.out.println("Booking Date & Time: " + bookingDateTime);
        System.out.println("Status: " + status);
        System.out.println("Payment Status: " + getPaymentStatus());
        System.out.println("Total Amount: " + totalAmount);
    }

    public void retrieveBookingInfo() {
        getBookingDetails();
    }

    private double calculateTotalAmount() {
        double basePrice = reservation.getRoute().getBasePrice();
        String seatClass = reservation.getSeatClass();
        switch (seatClass.toLowerCase()) {
            case "business":
                return basePrice * 1.5;
            case "first":
                return basePrice * 2.0;
            default:
                return basePrice;
        }
    }
}