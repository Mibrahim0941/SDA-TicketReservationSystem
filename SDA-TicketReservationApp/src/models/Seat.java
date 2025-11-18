package models;

public class Seat {
    private String seatNo;
    private boolean availability;
    private String seatType;
    private double price;
    
    public Seat() {
        this.availability = true;
    }
    
    public Seat(String seatNo, String seatType, double price) {
        this.seatNo = seatNo;
        this.seatType = seatType;
        this.price = price;
        this.availability = true;
    }
    
    // Getters and Setters
    public String getSeatNo() {
        return seatNo;
    }
    
    public void setSeatNo(String seatNo) {
        this.seatNo = seatNo;
    }
    
    public boolean isAvailability() {
        return availability;
    }
    
    public void setAvailability(boolean availability) {
        this.availability = availability;
    }
    
    public String getSeatType() {
        return seatType;
    }
    
    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    // Methods from class diagram
    public boolean checkAvailability() {
        return availability;
    }
    
    
    public void reserveSeat() {
        this.availability = false;
    }
    
    public void updateSeatStatus(boolean status) {
        this.availability = status;
    }
    
    @Override
    public String toString() {
        return seatNo + " (" + seatType + ") - " + (availability ? "Available" : "Booked");
    }
}