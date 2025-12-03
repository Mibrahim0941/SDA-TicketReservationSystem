package models;

public class CancellationPolicy {
    private String policyID;
    private double amountToBeRefunded;
    private int timeBeforeDeparture;
    private String description;
    
    public CancellationPolicy() {}
    
    public CancellationPolicy(String policyID, double amountToBeRefunded, int timeBeforeDeparture, String description) {
        this.policyID = policyID;
        this.amountToBeRefunded = amountToBeRefunded;
        this.timeBeforeDeparture = timeBeforeDeparture;
        this.description = description;
    }
    
    public String getPolicyID() {
        return policyID;
    }
    
    public void setPolicyID(String policyID) {
        this.policyID = policyID;
    }
    
    public double getAmountToBeRefunded() {
        return amountToBeRefunded;
    }
    
    public void setAmountToBeRefunded(double amountToBeRefunded) {
        this.amountToBeRefunded = amountToBeRefunded;
    }
    
    public int getTimeBeforeDeparture() {
        return timeBeforeDeparture;
    }
    
    public void setTimeBeforeDeparture(int timeBeforeDeparture) {
        this.timeBeforeDeparture = timeBeforeDeparture;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean match(int hoursBeforeDeparture) {
        return hoursBeforeDeparture >= timeBeforeDeparture;
    }
    
    public String getData() {
        return "If cancelled " + timeBeforeDeparture + " hours before departure: " + 
               amountToBeRefunded + "% refund - " + description;
    }
    
    @Override
    public String toString() {
        return getData();
    }
}