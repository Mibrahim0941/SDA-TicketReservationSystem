package catalogs;
import models.User;
import models.Admin;
import models.Customer;
import models.SupportStaff;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserCatalog {
    private java.util.ArrayList<User> users;

    public UserCatalog(String type) {
        this.users = new java.util.ArrayList<>();
        loadUsersFromDatabase(type); // Only called once at startup
    }

    // Load users from database ONLY at startup
    private void loadUsersFromDatabase(String type) {
        String query = "SELECT u.UserID, u.Name, u.Password, u.Username, u.UserType, c.Email, c.PhoneNum " +
                      "FROM Users u INNER JOIN ContactInfo c ON u.ContactID = c.ContactID where u.UserType = ?";
        

        // DON'T use try-with-resources for Connection - use regular try-catch
        try {
            java.sql.Connection conn = DatabaseConnection.getConnection();
            java.sql.PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, type);
            java.sql.ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                User user = createUserFromResultSet(rs);
                users.add(user);
            }
            
            // Close only statement and result set, NOT the connection
            rs.close();
            stmt.close();
            
            System.out.println("Loaded " + users.size() + " users from database.");
            
        } catch (java.sql.SQLException e) {
            System.err.println("Error loading users from database: " + e.getMessage());
            // Continue with empty list if DB fails
        }
    }

    // Add user to both local list AND database (for persistence)
    public boolean addToCatalog(User user) {
        // First add to local list (in-memory)
        users.add(user);
        
        // Then persist to database
        return persistUserToDatabase(user);
    }

    // Database persistence method (only called when adding/updating)
    private boolean persistUserToDatabase(User user) {
        java.sql.Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // Insert into ContactInfo
            String contactQuery = "INSERT INTO ContactInfo (Email, PhoneNum) VALUES (?, ?)";
            java.sql.PreparedStatement contactStmt = conn.prepareStatement(contactQuery, java.sql.Statement.RETURN_GENERATED_KEYS);
            contactStmt.setString(1, user.getEmail());
            contactStmt.setString(2, user.getPhoneNum());
            contactStmt.executeUpdate();
            
            // Get generated ContactID
            int contactId = -1;
            java.sql.ResultSet generatedKeys = contactStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                contactId = generatedKeys.getInt(1);
            }
            generatedKeys.close();
            contactStmt.close();
            
            if (contactId == -1) {
                throw new java.sql.SQLException("Failed to get ContactID");
            }
            
            // Determine user type
            String userType = getUserType(user);
            
            // Insert into Users
            String userQuery = "INSERT INTO Users (UserID, Name, Password, Username, ContactID, UserType) VALUES (?, ?, ?, ?, ?, ?)";
            java.sql.PreparedStatement userStmt = conn.prepareStatement(userQuery);
            userStmt.setString(1, user.getUserID());
            userStmt.setString(2, user.getName());
            userStmt.setString(3, user.getPassword());
            userStmt.setString(4, user.getUsername());
            userStmt.setInt(5, contactId);
            userStmt.setString(6, userType);
            userStmt.executeUpdate();
            userStmt.close();
            
            conn.commit();
            System.out.println("User persisted to database: " + user.getUsername());
            return true;
            
        } catch (java.sql.SQLException e) {
            if (conn != null) {
                try { 
                    conn.rollback(); 
                } catch (java.sql.SQLException ex) {
                    System.err.println("Error during rollback: " + ex.getMessage());
                }
            }
            System.err.println("Error persisting user to database: " + e.getMessage());
            // User is still in local list even if DB fails
            return false;
        } finally {
            if (conn != null) {
                try { 
                    conn.setAutoCommit(true); 
                } catch (java.sql.SQLException e) {
                    System.err.println("Error resetting auto-commit: " + e.getMessage());
                }
            }
        }
    }

    // Helper methods
    private User createUserFromResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        String userID = rs.getString("UserID");
        String name = rs.getString("Name");
        String password = rs.getString("Password");
        String username = rs.getString("Username");
        String userType = rs.getString("UserType");
        String email = rs.getString("Email");
        String phoneNum = rs.getString("PhoneNum");
        
        switch (userType) {
            case "Admin": 
                return new Admin(userID, name, password, username, email, phoneNum);
            case "SupportStaff": 
                return new SupportStaff(userID, name, password, username, email, phoneNum);
            case "Customer":
            default: 
                return new Customer(userID, name, password, username, email, phoneNum);
        }
    }

    private String getUserType(User user) {
        if (user instanceof Admin) return "Admin";
        if (user instanceof SupportStaff) return "SupportStaff";
        return "Customer";
    }

    // ALL these methods use ONLY the local list - no database calls!
    public boolean authenticateUser(String username, String password) {
        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }

    public User getUserByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    public User getUserByEmail(String email) {
        for (User user : users) {
            if (user.getEmail().equals(email)) {
                return user;
            }
        }
        return null;
    }

    public boolean isUsernameTaken(String username) {
        return getUserByUsername(username) != null;
    }

    public boolean isEmailTaken(String email) {
        return getUserByEmail(email) != null;
    }

    public java.util.ArrayList<User> getUsers() {
        return users;
    }

    public int getUserCount() {
        return users.size();
    }

    // Check database for email (only used during registration validation)
    public boolean isEmailTakenInDatabase(String email) {
        String query = "SELECT COUNT(*) FROM ContactInfo WHERE Email = ?";
        
        // DON'T use try-with-resources for Connection
        try {
            java.sql.Connection conn = DatabaseConnection.getConnection();
            java.sql.PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, email);
            java.sql.ResultSet rs = stmt.executeQuery();
            
            boolean result = rs.next() && rs.getInt(1) > 0;
            
            // Close only statement and result set
            rs.close();
            stmt.close();
            
            return result;
            
        } catch (java.sql.SQLException e) {
            System.err.println("Error checking email in database: " + e.getMessage());
            return false;
        }
    }

    // Get users by role
    public java.util.ArrayList<User> getUsersByRole(String role) {
        java.util.ArrayList<User> result = new java.util.ArrayList<>();
        for (User user : users) {
            if (user.getClass().getSimpleName().equalsIgnoreCase(role)) {
                result.add(user);
            }
        }
        return result;
    }

    // Remove user from catalog
    public boolean removeUser(String username) {
        User user = getUserByUsername(username);
        if (user != null) {
            users.remove(user);
            return true;
        }
        return false;
    }
    

    public boolean updateUser(User updatedUser) {
        // Find the existing user in the local list by UserID (not username)
        User existingUser = getUserByID(updatedUser.getUserID());
        if (existingUser == null) {
            System.out.println("User not found in catalog: " + updatedUser.getUserID());
            return false;
        }
    
        // Update the local user object
        existingUser.setName(updatedUser.getName());
        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPhoneNum(updatedUser.getPhoneNum());
    
        // Update in database
        return updateUserProfileInDatabase(updatedUser);
    }

    public boolean updateUserPassword(String userID, String newPassword) {
        // Find the existing user in the local list by UserID
        User existingUser = getUserByID(userID);
            if (existingUser == null) {
                System.out.println("User not found in catalog: " + userID);
            return false;
        }
    
        // Update the local user password
        existingUser.setPassword(newPassword);
    
        // Update in database (only password)
        return updatePasswordInDatabase(userID, newPassword);
    }

    // Add this helper method to UserCatalog to find user by ID
    public User getUserByID(String userID) {
        for (User user : users) {
            if (user.getUserID().equals(userID)) {
                return user;
            }
        }
        return null;
    }

    private boolean updateUserProfileInDatabase(User user) {
        Connection conn = null;
        PreparedStatement userStmt = null;
        PreparedStatement contactStmt = null;
    
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
        
            // First, get the ContactID for this user
            int contactId = getContactIdForUser(user.getUserID());
            if (contactId == -1) {
                System.out.println("ContactID not found for user: " + user.getUserID());
                return false;
            }
        
            // Update ContactInfo table
            String contactUpdate = "UPDATE ContactInfo SET Email = ?, PhoneNum = ? WHERE ContactID = ?";
            contactStmt = conn.prepareStatement(contactUpdate);
            contactStmt.setString(1, user.getEmail());
            contactStmt.setString(2, user.getPhoneNum());
            contactStmt.setInt(3, contactId);
        
            int contactRows = contactStmt.executeUpdate();
            System.out.println("ContactInfo rows updated: " + contactRows);
            
            // Then update Users table (only name and username, not password)
            String userUpdate = "UPDATE Users SET Name = ?, Username = ? WHERE UserID = ?";
            userStmt = conn.prepareStatement(userUpdate);
            userStmt.setString(1, user.getName());
            userStmt.setString(2, user.getUsername());
            userStmt.setString(3, user.getUserID());
        
            int userRows = userStmt.executeUpdate();
            System.out.println("Users rows updated: " + userRows);
        
            if (contactRows > 0 && userRows > 0) {
                conn.commit();
                System.out.println("User profile updated successfully in database: " + user.getUsername());
                return true;
            } else {
                conn.rollback();
                System.out.println("Failed to update user profile in database - rolling back");
                return false;
            }
        
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ex) {}
            }
            System.err.println("Error updating user profile in database: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try { if (contactStmt != null) contactStmt.close(); } catch (Exception e) {}
            try { if (userStmt != null) userStmt.close(); } catch (Exception e) {}
            try { if (conn != null) conn.setAutoCommit(true); } catch (Exception e) {}
        }
    }

    private int getContactIdForUser(String userID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
    
        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT ContactID FROM Users WHERE UserID = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, userID);
        
            rs = stmt.executeQuery();
            if (rs.next()) {
                int contactId = rs.getInt("ContactID");
                System.out.println("Found ContactID: " + contactId + " for UserID: " + userID);
                return contactId;
            }
            System.out.println("No ContactID found for UserID: " + userID);
            return -1;
        
        } catch (Exception e) {
            System.err.println("Error getting ContactID for user: " + e.getMessage());
            e.printStackTrace();
            return -1;
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (stmt != null) stmt.close(); } catch (Exception e) {}
        }
    }   
    private boolean updatePasswordInDatabase(String userID, String newPassword) {
        Connection conn = null;
        PreparedStatement stmt = null;
    
        try {
            conn = DatabaseConnection.getConnection();
            String updateQuery = "UPDATE Users SET Password = ? WHERE UserID = ?";
            stmt = conn.prepareStatement(updateQuery);
            stmt.setString(1, newPassword);
            stmt.setString(2, userID);
            
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Password updated in database. Rows affected: " + rowsAffected);
            return rowsAffected > 0;
        
        } catch (Exception e) {
            System.err.println("Error updating password in database: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try { if (stmt != null) stmt.close(); } catch (Exception e) {}
        }
    }
}