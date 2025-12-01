package catalogs;

import models.Booking;
import models.Reservation;
import models.Customer;
import models.Payment;
import models.Schedule;
import models.Route;
import database.DatabaseConnection;
import java.util.Date;
import java.sql.Timestamp;

public class BookingCatalog {
    private java.util.ArrayList<Booking> bookings;
    
    public BookingCatalog() {
        this.bookings = new java.util.ArrayList<>();
        loadBookingsFromDatabase();
    }

    private void loadBookingsFromDatabase() {
        String query = "SELECT " +
                  "b.BookingID, b.CustomerID, b.BookingDateTime, b.TotalAmount, b.Status, " +
                  "b.ReservationID, b.PaymentID, " +
                  "cust.Name as CustomerName, " +
                  "cust_contact.Email as CustomerEmail, " +
                  "cust_contact.PhoneNum as CustomerPhone, " +
                  "res.ScheduleID, res.RouteID, " +
                  "sch.Date as ScheduleDate, sch.DepartureTime, sch.ArrivalTime, sch.Class as ScheduleClass, " +
                  "r.Source, r.Destination, r.BasePrice, " +
                  "p.Amount as PaymentAmount, p.PaymentMethod, p.PaymentStatus, p.TransactionID, p.PaymentDate " +
                  "FROM Booking b " +
                  "LEFT JOIN Users cust ON b.CustomerID = cust.UserID " +
                  "LEFT JOIN ContactInfo cust_contact ON cust.ContactID = cust_contact.ContactID " +
                  "LEFT JOIN Reservation res ON b.ReservationID = res.ReservationID " +
                  "LEFT JOIN Schedule sch ON res.ScheduleID = sch.ScheduleID " +
                  "LEFT JOIN Route r ON res.RouteID = r.RouteID " +
                  "LEFT JOIN Payment p ON b.PaymentID = p.PaymentID " +
                  "ORDER BY b.BookingDateTime DESC";

        try {
            java.sql.Connection conn = DatabaseConnection.getConnection();
            java.sql.PreparedStatement stmt = conn.prepareStatement(query);
            java.sql.ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Booking booking = createBookingFromResultSet(rs);
                if (booking != null) {
                    bookings.add(booking);
                }
            }
            
            rs.close();
            stmt.close();
        
            
            System.out.println("Loaded " + bookings.size() + " bookings from database.");
            
        } catch (java.sql.SQLException e) {
            System.err.println("Error loading bookings from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean addBooking(Booking booking) {
        boolean persisted = persistBookingToDatabase(booking);
        
        if (persisted) {
            bookings.add(booking);
            return true;
        }
        return false;
    }

    private boolean persistBookingToDatabase(Booking booking) {
        java.sql.Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // First, ensure reservation exists
            Reservation reservation = booking.getReservation();
            if (reservation == null) {
                System.err.println("Cannot create booking without reservation");
                return false;
            }
            
            // Insert or update reservation first
            if (!persistReservation(reservation, conn)) {
                conn.rollback();
                return false;
            }
            
            // Check if booking already exists
            String checkQuery = "SELECT COUNT(*) FROM Booking WHERE BookingID = ?";
            java.sql.PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, booking.getBookingID());
            java.sql.ResultSet rs = checkStmt.executeQuery();
            rs.next();
            boolean exists = rs.getInt(1) > 0;
            checkStmt.close();
            rs.close();
            
            // Insert or update payment if exists
            if (booking.hasPayment()) {
                if (!persistPayment(booking.getPayment(), conn)) {
                    conn.rollback();
                    return false;
                }
            }
            
            String sql;
            if (exists) {
                sql = "UPDATE Booking SET CustomerID = ?, BookingDateTime = ?, TotalAmount = ?, " +
                      "Status = ?, ReservationID = ?, PaymentID = ? WHERE BookingID = ?";
            } else {
                sql = "INSERT INTO Booking (BookingID, CustomerID, BookingDateTime, TotalAmount, " +
                      "Status, ReservationID, PaymentID) VALUES (?, ?, ?, ?, ?, ?, ?)";
            }
            
            java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
            
            if (exists) {
                stmt.setString(1, booking.getCustomerID());
                stmt.setTimestamp(2, new Timestamp(booking.getBookingDateTime().getTime()));
                stmt.setDouble(3, booking.getTotalAmount());
                stmt.setString(4, booking.getStatus());
                stmt.setString(5, booking.getReservation().getReservationID());
                
                if (booking.hasPayment()) {
                    stmt.setString(6, booking.getPayment().getPaymentID());
                } else {
                    stmt.setNull(6, java.sql.Types.VARCHAR);
                }
                
                stmt.setString(7, booking.getBookingID());
            } else {
                stmt.setString(1, booking.getBookingID());
                stmt.setString(2, booking.getCustomerID());
                stmt.setTimestamp(3, new Timestamp(booking.getBookingDateTime().getTime()));
                stmt.setDouble(4, booking.getTotalAmount());
                stmt.setString(5, booking.getStatus());
                stmt.setString(6, booking.getReservation().getReservationID());
                
                if (booking.hasPayment()) {
                    stmt.setString(7, booking.getPayment().getPaymentID());
                } else {
                    stmt.setNull(7, java.sql.Types.VARCHAR);
                }
            }
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            if (rowsAffected > 0) {
                conn.commit();
                System.out.println("Booking " + (exists ? "updated" : "added") + " to database: " + booking.getBookingID());
                return true;
            } else {
                conn.rollback();
                return false;
            }
            
        } catch (java.sql.SQLException e) {
            if (conn != null) {
                try { 
                    conn.rollback(); 
                } catch (java.sql.SQLException ex) {
                    System.err.println("Error during rollback: " + ex.getMessage());
                }
            }
            System.err.println("Error persisting booking to database: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { 
                    conn.setAutoCommit(true);
                } catch (java.sql.SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    private boolean persistReservation(Reservation reservation, java.sql.Connection conn) throws java.sql.SQLException {
        // Check if reservation exists
        String checkQuery = "SELECT COUNT(*) FROM Reservation WHERE ReservationID = ?";
        java.sql.PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
        checkStmt.setString(1, reservation.getReservationID());
        java.sql.ResultSet rs = checkStmt.executeQuery();
        rs.next();
        boolean exists = rs.getInt(1) > 0;
        checkStmt.close();
        rs.close();
        
        String sql;
        if (exists) {
            sql = "UPDATE Reservation SET ScheduleID = ?, RouteID = ? WHERE ReservationID = ?";
        } else {
            sql = "INSERT INTO Reservation (ReservationID, ScheduleID, RouteID) VALUES (?, ?, ?)";
        }
        
        java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
        
        if (exists) {
            stmt.setString(1, reservation.getSchedule().getScheduleID());
            stmt.setString(2, reservation.getRoute().getRouteID());
            stmt.setString(3, reservation.getReservationID());
        } else {
            stmt.setString(1, reservation.getReservationID());
            stmt.setString(2, reservation.getSchedule().getScheduleID());
            stmt.setString(3, reservation.getRoute().getRouteID());
        }
        
        int rowsAffected = stmt.executeUpdate();
        stmt.close();
        
        return rowsAffected > 0;
    }

    private boolean persistPayment(Payment payment, java.sql.Connection conn) throws java.sql.SQLException {
        // Check if payment exists
        String checkQuery = "SELECT COUNT(*) FROM Payment WHERE PaymentID = ?";
        java.sql.PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
        checkStmt.setString(1, payment.getPaymentID());
        java.sql.ResultSet rs = checkStmt.executeQuery();
        rs.next();
        boolean exists = rs.getInt(1) > 0;
        checkStmt.close();
        rs.close();
        
        String sql;
        if (exists) {
            sql = "UPDATE Payment SET Amount = ?, PaymentMethod = ?, PaymentStatus = ?, " +
                  "TransactionID = ?, PaymentDate = ? WHERE PaymentID = ?";
        } else {
            sql = "INSERT INTO Payment (PaymentID, Amount, PaymentMethod, PaymentStatus, " +
                  "TransactionID, PaymentDate) VALUES (?, ?, ?, ?, ?, ?)";
        }
        
        java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
        
        if (exists) {
            stmt.setDouble(1, payment.getAmount());
            stmt.setString(2, payment.getPaymentMethod());
            stmt.setString(3, payment.getStatus());
            stmt.setString(4, payment.getTransactionID());
            stmt.setTimestamp(5, new Timestamp(payment.getPaymentDate().getTime()));
            stmt.setString(6, payment.getPaymentID());
        } else {
            stmt.setString(1, payment.getPaymentID());
            stmt.setDouble(2, payment.getAmount());
            stmt.setString(3, payment.getPaymentMethod());
            stmt.setString(4, payment.getStatus());
            stmt.setString(5, payment.getTransactionID());
            stmt.setTimestamp(6, new Timestamp(payment.getPaymentDate().getTime()));
        }
        
        int rowsAffected = stmt.executeUpdate();
        stmt.close();
        
        return rowsAffected > 0;
    }

    private Booking createBookingFromResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        try {
            String bookingID = rs.getString("BookingID");
            String customerID = rs.getString("CustomerID");
            Date bookingDateTime = new Date(rs.getTimestamp("BookingDateTime").getTime());
            double totalAmount = rs.getDouble("TotalAmount");
            String status = rs.getString("Status");
            
            // Create Customer object
            Customer customer = new Customer(
                customerID,
                rs.getString("CustomerName"),
                "", // password
                "", // username
                rs.getString("CustomerEmail"),
                rs.getString("CustomerPhone")
            );
            
            // Create Schedule object
            Schedule schedule = new Schedule(
                rs.getString("ScheduleID"),
                rs.getDate("ScheduleDate").toLocalDate(),
                rs.getTime("DepartureTime").toLocalTime(),
                rs.getTime("ArrivalTime").toLocalTime(),
                rs.getString("ScheduleClass")
            );
            
            // Create Route object
            Route route = new Route(
                rs.getString("RouteID"),
                rs.getString("Source"),
                rs.getString("Destination"),
                rs.getDouble("BasePrice")
            );
            
            // Create Reservation object
            Reservation reservation = new Reservation(
                rs.getString("ReservationID"),
                schedule,
                route,
                rs.getString("ScheduleClass") // Using schedule class as seat class
            );
            
            // Create Booking object
            Booking booking = new Booking(bookingID, customerID, reservation, bookingDateTime);
            booking.setStatus(status);
            booking.setTotalAmount(totalAmount);
            
            // Create Payment if exists
            String paymentID = rs.getString("PaymentID");
            if (paymentID != null && !rs.wasNull()) {
                Payment payment = new Payment(
                    paymentID,
                    booking,
                    rs.getDouble("PaymentAmount"),
                    rs.getString("PaymentMethod")
                );
                payment.setStatus(rs.getString("PaymentStatus"));
                payment.setTransactionID(rs.getString("TransactionID"));
                payment.setPaymentDate(new Date(rs.getTimestamp("PaymentDate").getTime()));
                booking.setPayment(payment);
            }
            
            return booking;
            
        } catch (Exception e) {
            System.err.println("Error creating Booking from ResultSet: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Booking findBooking(String bookingID) {
        for (Booking booking : bookings) {
            if (booking.getBookingID().equals(bookingID)) {
                return booking;
            }
        }
        return null;
    }

    public boolean updateBookingStatus(String bookingID, String status) {
        Booking booking = findBooking(bookingID);
        if (booking != null) {
            String oldStatus = booking.getStatus();
            booking.setStatus(status);
            
            boolean updated = updateBookingInDatabase(booking);
            if (updated) {
                return true;
            } else {
                booking.setStatus(oldStatus);
                return false;
            }
        }
        return false;
    }

    private boolean updateBookingInDatabase(Booking booking) {
        String updateQuery = "UPDATE Booking SET Status = ?, TotalAmount = ?, PaymentID = ? WHERE BookingID = ?";
        
        try {
            java.sql.Connection conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // Update payment if exists
            if (booking.hasPayment()) {
                if (!updatePaymentInDatabase(booking.getPayment(), conn)) {
                    conn.rollback();
                    return false;
                }
            }
            
            java.sql.PreparedStatement stmt = conn.prepareStatement(updateQuery);
            stmt.setString(1, booking.getStatus());
            stmt.setDouble(2, booking.getTotalAmount());
            
            if (booking.hasPayment()) {
                stmt.setString(3, booking.getPayment().getPaymentID());
            } else {
                stmt.setNull(3, java.sql.Types.VARCHAR);
            }
            
            stmt.setString(4, booking.getBookingID());
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            if (rowsAffected > 0) {
                conn.commit();
                return true;
            } else {
                conn.rollback();
                return false;
            }
            
        } catch (java.sql.SQLException e) {
            System.err.println("Error updating booking in database: " + e.getMessage());
            return false;
        }
    }

    private boolean updatePaymentInDatabase(Payment payment, java.sql.Connection conn) throws java.sql.SQLException {
        String updateQuery = "UPDATE Payment SET PaymentStatus = ?, TransactionID = ? WHERE PaymentID = ?";
        
        java.sql.PreparedStatement stmt = conn.prepareStatement(updateQuery);
        stmt.setString(1, payment.getStatus());
        stmt.setString(2, payment.getTransactionID());
        stmt.setString(3, payment.getPaymentID());
        
        int rowsAffected = stmt.executeUpdate();
        stmt.close();
        
        return rowsAffected > 0;
    }

    public java.util.ArrayList<Booking> getBookingsByCustomer(String customerID) {
        java.util.ArrayList<Booking> result = new java.util.ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.getCustomerID().equals(customerID)) {
                result.add(booking);
            }
        }
        return result;
    }

    public java.util.ArrayList<Booking> getBookingsByStatus(String status) {
        java.util.ArrayList<Booking> result = new java.util.ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.getStatus().equalsIgnoreCase(status)) {
                result.add(booking);
            }
        }
        return result;
    }

    public java.util.ArrayList<Booking> getBookingsByRoute(String routeID) {
        java.util.ArrayList<Booking> result = new java.util.ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.getReservation() != null && 
                booking.getReservation().getRoute().getRouteID().equals(routeID)) {
                result.add(booking);
            }
        }
        return result;
    }

    public java.util.ArrayList<Booking> getBookingsBySchedule(String scheduleID) {
        java.util.ArrayList<Booking> result = new java.util.ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.getReservation() != null && 
                booking.getReservation().getSchedule().getScheduleID().equals(scheduleID)) {
                result.add(booking);
            }
        }
        return result;
    }

    public java.util.ArrayList<Booking> getConfirmedBookings() {
        return getBookingsByStatus("Confirmed");
    }

    public java.util.ArrayList<Booking> getCancelledBookings() {
        return getBookingsByStatus("Cancelled");
    }

    public java.util.ArrayList<Booking> getPaidBookings() {
        java.util.ArrayList<Booking> result = new java.util.ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.isPaid()) {
                result.add(booking);
            }
        }
        return result;
    }

    public java.util.ArrayList<Booking> getUnpaidBookings() {
        java.util.ArrayList<Booking> result = new java.util.ArrayList<>();
        for (Booking booking : bookings) {
            if (!booking.isPaid() && !booking.getStatus().equals("Cancelled")) {
                result.add(booking);
            }
        }
        return result;
    }

    public java.util.ArrayList<Booking> getAllBookings() {
        return new java.util.ArrayList<>(bookings);
    }

    public int getBookingCount() {
        return bookings.size();
    }

    public int getBookingCountByStatus(String status) {
        int count = 0;
        for (Booking booking : bookings) {
            if (booking.getStatus().equalsIgnoreCase(status)) {
                count++;
            }
        }
        return count;
    }

    public boolean cancelBooking(String bookingID) {
        Booking booking = findBooking(bookingID);
        if (booking != null && booking.getStatus().equals("Confirmed")) {
            return updateBookingStatus(bookingID, "Cancelled");
        }
        return false;
    }

    public boolean completeBooking(String bookingID) {
        Booking booking = findBooking(bookingID);
        if (booking != null && booking.isPaid() && booking.getStatus().equals("Confirmed")) {
            return updateBookingStatus(bookingID, "Completed");
        }
        return false;
    }

    public boolean addPaymentToBooking(String bookingID, Payment payment) {
        Booking booking = findBooking(bookingID);
        if (booking != null) {
            booking.setPayment(payment);
            return updateBookingInDatabase(booking);
        }
        return false;
    }

    public boolean removeBooking(String bookingID) {
        Booking booking = findBooking(bookingID);
        if (booking != null) {
            if (removeBookingFromDatabase(bookingID)) {
                bookings.remove(booking);
                return true;
            }
        }
        return false;
    }

    private boolean removeBookingFromDatabase(String bookingID) {
        java.sql.Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // First get booking to check if it has payment
            Booking booking = findBooking(bookingID);
            if (booking != null && booking.hasPayment()) {
                // Remove payment first
                String deletePaymentQuery = "DELETE FROM Payment WHERE PaymentID = ?";
                java.sql.PreparedStatement paymentStmt = conn.prepareStatement(deletePaymentQuery);
                paymentStmt.setString(1, booking.getPayment().getPaymentID());
                paymentStmt.executeUpdate();
                paymentStmt.close();
            }
            
            // Remove booking
            String deleteQuery = "DELETE FROM Booking WHERE BookingID = ?";
            java.sql.PreparedStatement stmt = conn.prepareStatement(deleteQuery);
            stmt.setString(1, bookingID);
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            if (rowsAffected > 0) {
                conn.commit();
                return true;
            } else {
                conn.rollback();
                return false;
            }
            
        } catch (java.sql.SQLException e) {
            if (conn != null) {
                try { 
                    conn.rollback(); 
                } catch (java.sql.SQLException ex) {
                    System.err.println("Error during rollback: " + ex.getMessage());
                }
            }
            System.err.println("Error removing booking from database: " + e.getMessage());
            return false;
        } 
    }

    public double getTotalRevenue() {
        double total = 0.0;
        for (Booking booking : getPaidBookings()) {
            if (!booking.getStatus().equals("Cancelled")) {
                total += booking.getTotalAmount();
            }
        }
        return total;
    }

    public double getRevenueByPeriod(Date startDate, Date endDate) {
        double total = 0.0;
        for (Booking booking : getPaidBookings()) {
            if (!booking.getStatus().equals("Cancelled") &&
                booking.getBookingDateTime().after(startDate) &&
                booking.getBookingDateTime().before(endDate)) {
                total += booking.getTotalAmount();
            }
        }
        return total;
    }

    public java.util.ArrayList<Booking> getRecentBookings(int limit) {
        java.util.ArrayList<Booking> result = new java.util.ArrayList<>();
        int count = 0;
        for (Booking booking : bookings) {
            result.add(booking);
            count++;
            if (count >= limit) {
                break;
            }
        }
        return result;
    }

    public java.util.ArrayList<Booking> searchBookings(String searchTerm) {
        java.util.ArrayList<Booking> result = new java.util.ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.getBookingID().contains(searchTerm) ||
                booking.getCustomerID().contains(searchTerm) ||
                (booking.hasPayment() && booking.getPayment().getPaymentID().contains(searchTerm)) ||
                booking.getReservation().getReservationID().contains(searchTerm)) {
                result.add(booking);
            }
        }
        return result;
    }

    public void refreshFromDatabase() {
        bookings.clear();
        loadBookingsFromDatabase();
    }
}