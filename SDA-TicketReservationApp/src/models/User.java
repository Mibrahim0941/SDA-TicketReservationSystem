package models;

public class User {
    private String userID;
    private String name;
    private String password;
    private String username;
    private ContactInfo contactInfo;

    public User(String userID, String name, String password, String username, String email, String phoneNum) {
        this.userID = userID;
        this.name = name;
        this.password = password;
        this.username = username;
        this.contactInfo = new ContactInfo(email, phoneNum); 
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ContactInfo getContactInfo() {
        return contactInfo;
    }

    public String getEmail() {
        return contactInfo.getEmail();
    }

    public void setEmail(String email) {
        contactInfo.setEmail(email);
    }

    public String getPhoneNum() {
        return contactInfo.getPhoneNum();
    }

    public void setPhoneNum(String phoneNum) {
        contactInfo.setPhoneNum(phoneNum);
    }

    public boolean isMatched(String email) {
        return contactInfo.isMatched(email);
    }
}

class ContactInfo {
    private String email;
    private String phoneNum;

    public ContactInfo(String email, String phoneNum) {
        this.email = email;
        this.phoneNum = phoneNum;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public boolean isMatched(String email) {
        return this.email.equals(email);
    }
}

