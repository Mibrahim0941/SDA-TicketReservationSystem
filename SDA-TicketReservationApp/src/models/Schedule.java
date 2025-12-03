package models;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

public class Schedule {
    private String scheduleID;
    private LocalDate date;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private ArrayList<Seat> seats;
    private String scheduleClass;
    private double typePercentage;
    
    public Schedule() {
        this.seats = new ArrayList<>();
    }
    
    public Schedule(String scheduleID, LocalDate date, LocalTime departureTime, 
                   LocalTime arrivalTime, String scheduleClass) {
        this.scheduleID = scheduleID;
        this.date = date;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.scheduleClass = scheduleClass;
        this.seats = new ArrayList<>();
        this.typePercentage = 100.0; 
    }

    public String getScheduleID() {
        return scheduleID;
    }
    
    public void setScheduleID(String scheduleID) {
        this.scheduleID = scheduleID;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public LocalTime getDepartureTime() {
        return departureTime;
    }
    
    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }
    
    public LocalTime getArrivalTime() {
        return arrivalTime;
    }
    
    public void setArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }
    
    public ArrayList<Seat> getSeats() {
        return seats;
    }
    
    public void setSeats(ArrayList<Seat> seats) {
        this.seats = seats;
    }
    
    public String getScheduleClass() {
        return scheduleClass;
    }
    
    public void setScheduleClass(String scheduleClass) {
        this.scheduleClass = scheduleClass;
    }
    
    public double getTypePercentage() {
        return typePercentage;
    }
    
    public void setTypePercentage(double typePercentage) {
        this.typePercentage = typePercentage;
    }
    
    public boolean checkSchedule() {
        return date != null && departureTime != null && arrivalTime != null && 
               arrivalTime.isAfter(departureTime);
    }
    
    public boolean matchSchedule(String scheduleID) {
        return this.scheduleID.equals(scheduleID);
    }
    
    public void setTypePercentage(String type, double percentage) {
        if (this.scheduleClass.equalsIgnoreCase(type)) {
            this.typePercentage = percentage;
        }
    }
    
    public int getSeatCount() {
        return seats != null ? seats.size() : 0;
    }
    
    public boolean matchClass(String scheduleClass) {
        return this.scheduleClass.equalsIgnoreCase(scheduleClass);
    }
    
    @Override
    public String toString() {
        return date + " " + departureTime + " - " + arrivalTime + " (" + scheduleClass + ")";
    }
}