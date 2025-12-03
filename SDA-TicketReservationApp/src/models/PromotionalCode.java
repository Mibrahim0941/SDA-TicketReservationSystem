package models;

import java.time.LocalDate;

public class PromotionalCode {
    private String code;
    private LocalDate validity;
    private double percentage;
    private boolean isActive;
    
    public PromotionalCode() {
        this.isActive = true;
    }
    
    public PromotionalCode(String code, LocalDate validity, double percentage) {
        this.code = code;
        this.validity = validity;
        this.percentage = percentage;
        this.isActive = true;
    }

    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public LocalDate getValidity() {
        return validity;
    }
    
    public void setValidity(LocalDate validity) {
        this.validity = validity;
    }
    
    public double getPercentage() {
        return percentage;
    }
    
    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public boolean checkValidity() {
        if (!isActive) {
            return false;
        }
        return LocalDate.now().isBefore(validity) || LocalDate.now().isEqual(validity);
    }
    
    public double calculateDiscount(double amount) {
        if (!checkValidity()) {
            return 0.0;
        }
        return amount * (percentage / 100);
    }
    
    public double applyDiscount(double amount) {
        if (!checkValidity()) {
            return amount;
        }
        return amount - calculateDiscount(amount);
    }
    
    @Override
    public String toString() {
        String status = isActive ? "Active" : "Inactive";
        String valid = checkValidity() ? "Valid" : "Expired";
        return code + " (" + percentage + "% off) - " + status + " - " + valid + " until " + validity;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PromotionalCode that = (PromotionalCode) obj;
        return code.equals(that.code);
    }
    
    @Override
    public int hashCode() {
        return code.hashCode();
    }
}