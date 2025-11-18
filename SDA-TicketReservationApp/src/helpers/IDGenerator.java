package helpers;

import java.util.UUID;

public class IDGenerator {
    
    public static String generateCustomerID() {
        return "CUST-" + UUID.randomUUID().toString();
    }
    
    public static String generateAdminID() {
        return "ADM-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    public static String generateSupportStaffID() {
        return "SUP-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    public static String generateRouteID() {
        return "RT" + String.format("%03d", (int)(Math.random() * 1000));
    }
    
    public static String generateScheduleID() {
        return "SCH" + String.format("%04d", (int)(Math.random() * 10000));
    }
    
    public static String generateSeatID() {
        return "SEAT-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    public static String generateBookingID() {
        return "BK-" + System.currentTimeMillis();
    }
    
    public static String generatePolicyID() {
        return "POL" + String.format("%03d", (int)(Math.random() * 1000));
    }
    
    public static String generatePromoCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt((int)(Math.random() * chars.length())));
        }
        return code.toString();
    }
    
    public static String generateQueryID() {
        return "Q" + String.format("%04d", (int)(Math.random() * 10000));
    }
}