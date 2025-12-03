package models;

public class Customer extends User {
    public Customer(String userID, String name, String password, String username, String email, String phoneNum) {
        super(userID, name, password, username, email, phoneNum);
    }
}