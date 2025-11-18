package models;

import java.util.ArrayList;

public class Route {
    private String routeID;
    private String source;
    private String destination;
    private double basePrice;
    private ArrayList<Schedule> schedules;
    
    public Route() {
        this.schedules = new ArrayList<>();
    }
    
    public Route(String routeID, String source, String destination, double basePrice) {
        this.routeID = routeID;
        this.source = source;
        this.destination = destination;
        this.basePrice = basePrice;
        this.schedules = new ArrayList<>();
    }
    
    // Getters and Setters
    public String getRouteID() {
        return routeID;
    }
    
    public void setRouteID(String routeID) {
        this.routeID = routeID;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public double getBasePrice() {
        return basePrice;
    }
    
    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }
    
    public ArrayList<Schedule> getSchedules() {
        return schedules;
    }
    
    public void setSchedules(ArrayList<Schedule> schedules) {
        this.schedules = schedules;
    }
    
    // Methods from class diagram
    public boolean verifySrcDst(String src, String dst) {
        return this.source.equalsIgnoreCase(src) && this.destination.equalsIgnoreCase(dst);
    }
    
    public boolean checkRoute(String src, String dst) {
        return verifySrcDst(src, dst);
    }
    
    public void addSchedule(Schedule schedule) {
        if (schedule != null && !schedules.contains(schedule)) {
            schedules.add(schedule);
        }
        
    }
    
    public ArrayList<Schedule> getAllSchedules() {
        return new ArrayList<>(schedules);
    }
    
    public Schedule getSchedule(String scheduleID) {
        for (Schedule schedule : schedules) {
            if (schedule.getScheduleID().equals(scheduleID)) {
                return schedule;
            }
        }
        return null;
    }
    
    public void setClassPercentage(String classType, double percentage) {
        for (Schedule schedule : schedules) {
            schedule.setTypePercentage(classType, percentage);
        }
    }
    
    @Override
    public String toString() {
        return source + " → " + destination + " (PKR " + basePrice + ")";
    }
}