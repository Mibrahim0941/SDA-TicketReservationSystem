package models;

import java.util.Date;

public class SupportQuery {
    private String queryID;
    private String text;
    private Date askedOn;
    private boolean status;
    private String response;
    private Customer customer;
    private SupportStaff supportStaff;

    // Constructors
    public SupportQuery() {
        this.askedOn = new Date();
        this.status = false;
    }

    public SupportQuery(String queryID, String text, Customer customer) {
        this();
        this.queryID = queryID;
        this.text = text;
        this.customer = customer;
    }

    // Getters and Setters
    public String getQueryID() { return queryID; }
    public void setQueryID(String queryID) { this.queryID = queryID; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Date getAskedOn() { return askedOn; }
    public void setAskedOn(Date askedOn) { this.askedOn = askedOn; }

    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public SupportStaff getSupportStaff() { return supportStaff; }
    public void setSupportStaff(SupportStaff supportStaff) { this.supportStaff = supportStaff; }

    // Methods from your schema
    public boolean matchID(String id) {
        return this.queryID.equals(id);
    }

    public void selfResponse(String response) {
        this.response = response;
        this.status = true;
    }

}