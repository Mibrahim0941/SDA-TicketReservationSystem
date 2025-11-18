package catalogs;

import models.PromotionalCode;
import config.DatabaseConfig;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;

public class PromoCodeCatalog {
    private ArrayList<PromotionalCode> promoCodes;
    private Connection connection;
    private boolean databaseAvailable;
    
    public PromoCodeCatalog() {
        this.promoCodes = new ArrayList<>();
        this.databaseAvailable = false;
        
        try {
            String url = DatabaseConfig.getDbUrl();
            String user = DatabaseConfig.getDbUser();
            String password = DatabaseConfig.getDbPassword();
            
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            this.connection = DriverManager.getConnection(url, user, password);
            this.databaseAvailable = true;
            loadPromoCodesFromDB();
            
        } catch (ClassNotFoundException e) {
            System.err.println("SQL Server JDBC driver not found: " + e.getMessage());
            System.out.println("Using in-memory storage instead");
            initializeSamplePromoCodes();
        } catch (SQLException e) {
            System.err.println("Failed to initialize PromoCodeCatalog with database: " + e.getMessage());
            System.out.println("Using in-memory storage instead");
            initializeSamplePromoCodes();
        }
    }
    
    private void initializeSamplePromoCodes() {
        promoCodes.add(new PromotionalCode("WELCOME10", LocalDate.now().plusDays(30), 10.0));
        promoCodes.add(new PromotionalCode("SUMMER25", LocalDate.now().plusDays(60), 25.0));
        promoCodes.add(new PromotionalCode("FALL15", LocalDate.now().plusDays(45), 15.0));
        promoCodes.add(new PromotionalCode("NEWUSER20", LocalDate.now().plusDays(90), 20.0));
        System.out.println("Initialized with " + promoCodes.size() + " sample promotional codes");
    }
    
    private void loadPromoCodesFromDB() {
        if (!databaseAvailable) {
            System.out.println("Database not available, using in-memory data");
            return;
        }
        
        createTableIfNotExists();
        
        String query = "SELECT * FROM PromotionalCodes ORDER BY ValidityDate DESC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            promoCodes.clear();
            
            while (rs.next()) {
                String code = rs.getString("Code");
                double percentage = rs.getDouble("Percentage");
                LocalDate validity = rs.getDate("ValidityDate").toLocalDate();
                boolean isActive = rs.getBoolean("IsActive");
                
                PromotionalCode promoCode = new PromotionalCode(code, validity, percentage);
                promoCode.setActive(isActive);
                promoCodes.add(promoCode);
            }
            
            System.out.println("Loaded " + promoCodes.size() + " promotional codes from database");
            
        } catch (SQLException e) {
            System.err.println("Error loading promotional codes from database: " + e.getMessage());
            e.printStackTrace();
            if (promoCodes.isEmpty()) {
                initializeSamplePromoCodes();
            }
        }
    }
    
    private void createTableIfNotExists() {
        if (!databaseAvailable) {
            return;
        }
        
        String createTableSQL = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='PromotionalCodes' AND xtype='U') " +
                               "CREATE TABLE PromotionalCodes (" +
                               "Code VARCHAR(50) PRIMARY KEY, " +
                               "Percentage DECIMAL(5,2) NOT NULL, " +
                               "ValidityDate DATE NOT NULL, " +
                               "IsActive BIT DEFAULT 1, " +
                               "CreatedAt DATETIME DEFAULT GETDATE(), " +
                               "UpdatedAt DATETIME DEFAULT GETDATE()" +
                               ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("PromotionalCodes table ensured to exist");
        } catch (SQLException e) {
            System.err.println("Error creating PromotionalCodes table: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean addToCatalog(PromotionalCode promoCode) {
        if (getPromoCode(promoCode.getCode()) != null) {
            System.err.println("Promo code already exists: " + promoCode.getCode());
            return false;
        }
        
        promoCodes.add(promoCode);
        
        if (databaseAvailable) {
            String query = "INSERT INTO PromotionalCodes (Code, Percentage, ValidityDate, IsActive) VALUES (?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, promoCode.getCode());
                pstmt.setDouble(2, promoCode.getPercentage());
                pstmt.setDate(3, Date.valueOf(promoCode.getValidity()));
                pstmt.setBoolean(4, promoCode.isActive());
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("Promo code added successfully to database: " + promoCode.getCode());
                    return true;
                }
                
            } catch (SQLException e) {
                System.err.println("Error adding promo code to database: " + e.getMessage());
                promoCodes.remove(promoCode);
                return false;
            }
        }
        
        System.out.println("Promo code added to in-memory storage: " + promoCode.getCode());
        return true;
    }
    
    public PromotionalCode getPromoCode(String code) {
        for (PromotionalCode promoCode : promoCodes) {
            if (promoCode.getCode().equalsIgnoreCase(code)) {
                return promoCode;
            }
        }
        return null;
    }
    
    public ArrayList<PromotionalCode> getAllPromoCodes() {
        return new ArrayList<>(promoCodes);
    }
    
    public ArrayList<PromotionalCode> getActivePromoCodes() {
        ArrayList<PromotionalCode> activePromoCodes = new ArrayList<>();
        for (PromotionalCode promoCode : promoCodes) {
            if (promoCode.isActive() && promoCode.checkValidity()) {
                activePromoCodes.add(promoCode);
            }
        }
        return activePromoCodes;
    }
    
    public ArrayList<PromotionalCode> getExpiredPromoCodes() {
        ArrayList<PromotionalCode> expiredPromoCodes = new ArrayList<>();
        for (PromotionalCode promoCode : promoCodes) {
            if (!promoCode.checkValidity()) {
                expiredPromoCodes.add(promoCode);
            }
        }
        return expiredPromoCodes;
    }
    
    public boolean updatePromoCode(PromotionalCode updatedPromoCode) {
        if (databaseAvailable) {
            String query = "UPDATE PromotionalCodes SET Percentage = ?, ValidityDate = ?, IsActive = ?, UpdatedAt = GETDATE() WHERE Code = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setDouble(1, updatedPromoCode.getPercentage());
                pstmt.setDate(2, Date.valueOf(updatedPromoCode.getValidity()));
                pstmt.setBoolean(3, updatedPromoCode.isActive());
                pstmt.setString(4, updatedPromoCode.getCode());
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    updateLocalPromoCode(updatedPromoCode);
                    System.out.println("Promo code updated successfully in database: " + updatedPromoCode.getCode());
                    return true;
                }
                
            } catch (SQLException e) {
                System.err.println("Error updating promo code in database: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        boolean updated = updateLocalPromoCode(updatedPromoCode);
        if (updated) {
            System.out.println("Promo code updated in in-memory storage: " + updatedPromoCode.getCode());
        }
        return updated;
    }
    
    private boolean updateLocalPromoCode(PromotionalCode updatedPromoCode) {
        for (int i = 0; i < promoCodes.size(); i++) {
            if (promoCodes.get(i).getCode().equals(updatedPromoCode.getCode())) {
                promoCodes.set(i, updatedPromoCode);
                return true;
            }
        }
        return false;
    }
    
    public boolean deletePromoCode(String code) {
        if (databaseAvailable) {
            String query = "DELETE FROM PromotionalCodes WHERE Code = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, code);
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    promoCodes.removeIf(promoCode -> promoCode.getCode().equals(code));
                    System.out.println("Promo code deleted successfully from database: " + code);
                    return true;
                }
                
            } catch (SQLException e) {
                System.err.println("Error deleting promo code from database: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        boolean deleted = promoCodes.removeIf(promoCode -> promoCode.getCode().equals(code));
        if (deleted) {
            System.out.println("Promo code deleted from in-memory storage: " + code);
        }
        return deleted;
    }
    
    public boolean validatePromoCode(String code) {
        PromotionalCode promoCode = getPromoCode(code);
        return promoCode != null && promoCode.isActive() && promoCode.checkValidity();
    }
    
    public double applyPromoCode(String code, double originalPrice) {
        if (!validatePromoCode(code)) {
            System.out.println("Promo code " + code + " is invalid or expired");
            return originalPrice;
        }
        
        PromotionalCode promoCode = getPromoCode(code);
        double discount = originalPrice * (promoCode.getPercentage() / 100);
        double finalPrice = originalPrice - discount;
        
        System.out.println("Applied promo code " + code + ": " + originalPrice + " -> " + finalPrice + 
                          " (Discount: " + discount + ")");
        return finalPrice;
    }
    
    public void refresh() {
        if (databaseAvailable) {
            loadPromoCodesFromDB();
        }
    }
    
    public boolean isDatabaseAvailable() {
        return databaseAvailable;
    }
    
    public int getTotalPromoCodes() {
        return promoCodes.size();
    }
    
    public int getActivePromoCodesCount() {
        return getActivePromoCodes().size();
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