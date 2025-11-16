package models;
import java.util.Date;

public class SupportQuery {
    private String text;
    private Date askedOn;
    private boolean status;
    private String queryID;
    private String response;
    
    // Associations
    private SupportStaff supportStaff;
    private Customer customer;
    
    // Constructors
    public SupportQuery() {
    }
    
    public SupportQuery(String text, Date askedOn, String queryID, SupportStaff supportStaff, Customer customer) {
        this.text = text;
        this.askedOn = askedOn;
        this.queryID = queryID;
        this.supportStaff = supportStaff;
        this.customer = customer;
        this.status = false; // default status
    }
    
    // Getters and Setters
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public Date getAskedOn() {
        return askedOn;
    }
    
    public void setAskedOn(Date askedOn) {
        this.askedOn = askedOn;
    }
    
    public boolean isStatus() {
        return status;
    }
    
    public void setStatus(boolean status) {
        this.status = status;
    }
    
    public String getQueryID() {
        return queryID;
    }
    
    public void setQueryID(String queryID) {
        this.queryID = queryID;
    }
    
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    public SupportStaff getSupportStaff() {
        return supportStaff;
    }
    
    public void setSupportStaff(SupportStaff supportStaff) {
        this.supportStaff = supportStaff;
    }
    
    public Customer getCustomer() {
        return customer;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    // Methods from schema
    public boolean matchID(String id) {
        return this.queryID.equals(id);
    }
    
    public void selfResponse(String response) {
        this.response = response;
        this.status = true; // Mark as resolved when response is provided
    }
}