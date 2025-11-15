package models;

public class SupportStaff extends User {
    public SupportStaff(String userID, String name, String password, String username, String email, String phoneNum) {
        super(userID, name, password, username, email, phoneNum);
    }

    public void viewQuery() {
        // Implementation for viewing queries
    }

    public void respond() {
        // Implementation for responding to queries
    }
}

