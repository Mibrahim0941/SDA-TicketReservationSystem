package models;

public class Admin extends User {
    public Admin(String userID, String name, String password, String username, String email, String phoneNum) {
        super(userID, name, password, username, email, phoneNum);
    }

    public void updateRoute() {
        // Implementation for updating routes
    }

    public void deleteRoute() {
        // Implementation for deleting routes
    }

    public void updateSchedule() {
        // Implementation for updating schedules
    }

    public void manageSeatAvailability() {
        // Implementation for managing seat availability
    }

    public void generateReports() {
        // Implementation for generating reports
    }

    public void setCancellationPolicy() {
        // Implementation for setting cancellation policy
    }

    public void managePromotionalCodes() {
        // Implementation for managing promotional codes
    }
}

