package helpers;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for generating unique IDs for database insertion.
 * Supports multiple ID formats: UUID, numeric, and prefixed IDs.
 */
public class IDGenerator {

    private static final AtomicLong counter = new AtomicLong(System.currentTimeMillis());

    /**
     * Generates a UUID-based User ID.
     * Example: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
     */
    public static String generateUserID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a shorter UUID (first 8 characters) for simpler display.
     * Example: "a1b2c3d4"
     */
    public static String generateShortUserID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Generates a unique numeric ID.
     * Example: 1710674923847, 1710674923848 ...
     */
    public static long generateNumericID() {
        return counter.getAndIncrement();
    }

    /**
     * Generates a unique ID with a prefix.
     * Example: "USR-923847293847293"
     */
    public static String generateWithPrefix(String prefix) {
        return prefix + "-" + generateNumericID();
    }

    /**
     * Generates a UUID-based string.
     * Example: "ef98ad12-43b9-9f23-9cab-03d1f9021a11"
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Customer ID Helper Format: CUST-UUID
     * Example: "CUST-a1b2c3d4-e5f6-7890-abcd-ef1234567890"
     */
    public static String generateCustomerID() {
        return "CUST-" + generateUUID();
    }

    /**
     * Admin ID Helper Format: ADM-UUID
     * Example: "ADM-a1b2c3d4-e5f6-7890-abcd-ef1234567890"
     */
    public static String generateAdminID() {
        return "ADM-" + generateUUID();
    }

    /**
     * Support Staff ID Helper Format: SUP-UUID
     * Example: "SUP-a1b2c3d4-e5f6-7890-abcd-ef1234567890"
     */
    public static String generateSupportStaffID() {
        return "SUP-" + generateUUID();
    }

    /**
     * Booking ID Helper Format: BK-YYYY-NUMERIC
     * Example: "BK-2025-125151512151"
     */
    public static String generateBookingID() {
        return "BK-" + java.time.Year.now() + "-" + generateNumericID();
    }

    /**
     * Query ID Helper Format: QRY-UUID
     * Example: "QRY-a1b2c3d4-e5f6-7890-abcd-ef1234567890"
     */
    public static String generateQueryID() {
        return "QRY-" + generateShortUserID();
    }

    /**
     * Ticket ID Helper Format: TKT-UUID
     * Example: "TKT-a1b2c3d4-e5f6-7890-abcd-ef1234567890"
     */
    public static String generateTicketID() {
        return "TKT-" + generateShortUserID();
    }
}