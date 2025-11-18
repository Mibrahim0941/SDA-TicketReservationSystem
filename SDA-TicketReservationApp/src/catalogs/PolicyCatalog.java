package catalogs;

import models.CancellationPolicy;
import config.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;

public class PolicyCatalog {
    private ArrayList<CancellationPolicy> policies;
    private Connection connection;
    private boolean databaseAvailable;
    
    public PolicyCatalog() {
        this.policies = new ArrayList<>();
        this.databaseAvailable = false;
        
        try {
            String url = DatabaseConfig.getDbUrl();
            String user = DatabaseConfig.getDbUser();
            String password = DatabaseConfig.getDbPassword();
            
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            this.connection = DriverManager.getConnection(url, user, password);
            this.databaseAvailable = true;
            loadPoliciesFromDB();
            
        } catch (ClassNotFoundException e) {
            System.err.println("SQL Server JDBC driver not found: " + e.getMessage());
            System.out.println("Using in-memory storage instead");
            initializeDefaultPolicies();
        } catch (SQLException e) {
            System.err.println("Failed to initialize PolicyCatalog with database: " + e.getMessage());
            System.out.println("Using in-memory storage instead");
            initializeDefaultPolicies();
        }
    }
    
    private void initializeDefaultPolicies() {
        policies.add(new CancellationPolicy("POL001", 100.0, 24, "Full refund if cancelled 24+ hours before departure"));
        policies.add(new CancellationPolicy("POL002", 50.0, 12, "50% refund if cancelled 12-24 hours before departure"));
        policies.add(new CancellationPolicy("POL003", 0.0, 0, "No refund if cancelled less than 12 hours before departure"));
        policies.add(new CancellationPolicy("POL004", 75.0, 48, "75% refund if cancelled 48+ hours before departure"));
        System.out.println("Initialized with " + policies.size() + " default cancellation policies");
    }
    
    private void loadPoliciesFromDB() {
        if (!databaseAvailable) {
            System.out.println("Database not available, using in-memory data");
            return;
        }
        
        createTableIfNotExists();
        
        String query = "SELECT * FROM CancellationPolicies ORDER BY TimeBeforeDeparture DESC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            policies.clear();
            
            while (rs.next()) {
                String policyID = rs.getString("PolicyID");
                double refundAmount = rs.getDouble("RefundAmount");
                int timeBeforeDeparture = rs.getInt("TimeBeforeDeparture");
                String description = rs.getString("Description");
                
                CancellationPolicy policy = new CancellationPolicy(policyID, refundAmount, timeBeforeDeparture, description);
                policies.add(policy);
            }
            
            System.out.println("Loaded " + policies.size() + " cancellation policies from database");
            
        } catch (SQLException e) {
            System.err.println("Error loading cancellation policies from database: " + e.getMessage());
            e.printStackTrace();
            if (policies.isEmpty()) {
                initializeDefaultPolicies();
            }
        }
    }
    
    private void createTableIfNotExists() {
        if (!databaseAvailable) {
            return;
        }
        
        String createTableSQL = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='CancellationPolicies' AND xtype='U') " +
                               "CREATE TABLE CancellationPolicies (" +
                               "PolicyID VARCHAR(20) PRIMARY KEY, " +
                               "RefundAmount DECIMAL(5,2) NOT NULL, " +
                               "TimeBeforeDeparture INT NOT NULL, " +
                               "Description VARCHAR(500) NOT NULL, " +
                               "CreatedAt DATETIME DEFAULT GETDATE(), " +
                               "UpdatedAt DATETIME DEFAULT GETDATE(), " +
                               "UNIQUE(TimeBeforeDeparture)" +
                               ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("CancellationPolicies table ensured to exist");
        } catch (SQLException e) {
            System.err.println("Error creating CancellationPolicies table: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean addToCatalog(CancellationPolicy policy) {
        if (getPolicyByTimeFrame(policy.getTimeBeforeDeparture()) != null) {
            System.err.println("Policy already exists for time frame: " + policy.getTimeBeforeDeparture() + " hours");
            return false;
        }
        
        if (!validateData(policy)) {
            System.err.println("Invalid policy data");
            return false;
        }
        
        policies.add(policy);
        
        if (databaseAvailable) {
            String query = "INSERT INTO CancellationPolicies (PolicyID, RefundAmount, TimeBeforeDeparture, Description) VALUES (?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, policy.getPolicyID());
                pstmt.setDouble(2, policy.getAmountToBeRefunded());
                pstmt.setInt(3, policy.getTimeBeforeDeparture());
                pstmt.setString(4, policy.getDescription());
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("Policy added successfully to database: " + policy.getPolicyID());
                    return true;
                }
                
            } catch (SQLException e) {
                System.err.println("Error adding policy to database: " + e.getMessage());
                policies.remove(policy);
                return false;
            }
        }
        
        System.out.println("Policy added to in-memory storage: " + policy.getPolicyID());
        return true;
    }
    
    private CancellationPolicy getPolicyByTimeFrame(int timeBeforeDeparture) {
        return policies.stream()
                .filter(policy -> policy.getTimeBeforeDeparture() == timeBeforeDeparture)
                .findFirst()
                .orElse(null);
    }
    
    public boolean validateExistence(CancellationPolicy data) {
        return policies.stream().anyMatch(policy -> 
            policy.getTimeBeforeDeparture() == data.getTimeBeforeDeparture());
    }
    
    public boolean validateData(CancellationPolicy data) {
        return data != null && 
               data.getAmountToBeRefunded() >= 0 && 
               data.getAmountToBeRefunded() <= 100 &&
               data.getTimeBeforeDeparture() >= 0 &&
               data.getDescription() != null && !data.getDescription().isEmpty();
    }
    
    public ArrayList<CancellationPolicy> getAllPolicies() {
        return new ArrayList<>(policies);
    }
    
    public CancellationPolicy getPolicy(String policyID) {
        return policies.stream()
                .filter(policy -> policy.getPolicyID().equals(policyID))
                .findFirst()
                .orElse(null);
    }
    
    public CancellationPolicy getApplicablePolicy(int hoursBeforeDeparture) {
        for (CancellationPolicy policy : policies) {
            if (policy.match(hoursBeforeDeparture)) {
                return policy;
            }
        }
        return null;
    }
    
    public boolean updatePolicy(CancellationPolicy updatedPolicy) {
        if (!validateData(updatedPolicy)) {
            System.err.println("Invalid policy data for update");
            return false;
        }
        
        if (databaseAvailable) {
            String query = "UPDATE CancellationPolicies SET RefundAmount = ?, TimeBeforeDeparture = ?, Description = ?, UpdatedAt = GETDATE() WHERE PolicyID = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setDouble(1, updatedPolicy.getAmountToBeRefunded());
                pstmt.setInt(2, updatedPolicy.getTimeBeforeDeparture());
                pstmt.setString(3, updatedPolicy.getDescription());
                pstmt.setString(4, updatedPolicy.getPolicyID());
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    updateLocalPolicy(updatedPolicy);
                    System.out.println("Policy updated successfully in database: " + updatedPolicy.getPolicyID());
                    return true;
                }
                
            } catch (SQLException e) {
                System.err.println("Error updating policy in database: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        boolean updated = updateLocalPolicy(updatedPolicy);
        if (updated) {
            System.out.println("Policy updated in in-memory storage: " + updatedPolicy.getPolicyID());
        }
        return updated;
    }
    
    private boolean updateLocalPolicy(CancellationPolicy updatedPolicy) {
        for (int i = 0; i < policies.size(); i++) {
            if (policies.get(i).getPolicyID().equals(updatedPolicy.getPolicyID())) {
                policies.set(i, updatedPolicy);
                return true;
            }
        }
        return false;
    }
    
    public boolean deletePolicy(String policyID) {
        if (databaseAvailable) {
            String query = "DELETE FROM CancellationPolicies WHERE PolicyID = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, policyID);
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    policies.removeIf(policy -> policy.getPolicyID().equals(policyID));
                    System.out.println("Policy deleted successfully from database: " + policyID);
                    return true;
                }
                
            } catch (SQLException e) {
                System.err.println("Error deleting policy from database: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        boolean deleted = policies.removeIf(policy -> policy.getPolicyID().equals(policyID));
        if (deleted) {
            System.out.println("Policy deleted from in-memory storage: " + policyID);
        }
        return deleted;
    }
    
    public void refresh() {
        if (databaseAvailable) {
            loadPoliciesFromDB();
        }
    }
    
    public boolean isDatabaseAvailable() {
        return databaseAvailable;
    }
    
    public int getPolicyCount() {
        return policies.size();
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}