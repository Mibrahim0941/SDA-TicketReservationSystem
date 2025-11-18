package controllers;

import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.Scene;
import javafx.scene.Parent;

import java.io.IOException;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import models.Booking;
import models.Customer;
import models.ETicket;
import models.Payment;

import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import config.DatabaseConfig;

public class MyBookingsController implements Initializable {

    @FXML private Text pageTitle;
    @FXML private Text userGreeting;
    @FXML private Text noBookingsText;
    
    @FXML private VBox bookingsContainer;
    @FXML private HBox filterContainer;
    
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> paymentFilter;
    @FXML private TextField searchField;
    
    @FXML private Button backButton;
    @FXML private Button refreshButton;
    
    private String currentUsername;
    private Customer currentCustomer;
    private List<Booking> userBookings;

    // Database connection details
    String DB_URL = DatabaseConfig.getDbUrl();
    String DB_USER = DatabaseConfig.getDbUser();
    String DB_PASSWORD = DatabaseConfig.getDbPassword();

    public static void show(Stage stage, String username, Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(MyBookingsController.class.getResource("/ui/myBookings.fxml"));
            Parent root = loader.load();
            
            MyBookingsController controller = loader.getController();
            controller.setUserData(username, customer);
            
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(MyBookingsController.class.getResource("/ui/MyBookings.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - My Bookings");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to load My Bookings: " + e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("MyBookingsController initialized");
        setupFilters();
        setupEventHandlers();
    }
    
    public void setUserData(String username, Customer customer) {
        this.currentUsername = username;
        this.currentCustomer = customer;
        updateUserGreeting();
        loadUserBookingsFromDB();
    }
    
    private void updateUserGreeting() {
        if (currentUsername != null && !currentUsername.isEmpty()) {
            userGreeting.setText("Hello, " + currentUsername + "! 👋");
        }
    }
    
    private void setupFilters() {
        // Status filter options
        statusFilter.getItems().addAll("All Bookings", "Confirmed", "Pending", "Cancelled", "Completed");
        statusFilter.setValue("All Bookings");
        
        // Payment status filter
        paymentFilter.getItems().addAll("All Payments", "Paid", "Unpaid", "Pending Payment");
        paymentFilter.setValue("All Payments");
        
        // Search field placeholder
        searchField.setPromptText("Search by booking ID, route...");
    }
    
    private void setupEventHandlers() {
        backButton.setOnAction(e -> handleBack());
        refreshButton.setOnAction(e -> loadUserBookingsFromDB());
        
        // Filter change listeners
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        paymentFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }
    
    private void loadUserBookingsFromDB() {
        userBookings = fetchBookingsFromDatabase();
        applyFilters();
    }
    
    private List<Booking> fetchBookingsFromDatabase() {
        List<Booking> bookings = new ArrayList<>();
        
        String query = """
            SELECT 
                b.BookingID, b.CustomerID, b.BookingDateTime, b.TotalAmount, b.Status,
                p.PaymentID, p.PaymentStatus, p.PaymentMethod, p.Amount as PaymentAmount,
                r.RouteID, r.Source, r.Destination, r.BasePrice,
                s.ScheduleID, s.Date, s.DepartureTime, s.ArrivalTime, s.Class,
                res.ReservationID
            FROM Booking b
            LEFT JOIN Payment p ON b.PaymentID = p.PaymentID
            LEFT JOIN Reservation res ON b.ReservationID = res.ReservationID
            LEFT JOIN Route r ON res.RouteID = r.RouteID
            LEFT JOIN Schedule s ON res.ScheduleID = s.ScheduleID
            WHERE b.CustomerID = ?
            ORDER BY b.BookingDateTime DESC
            """;
            
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, currentCustomer.getUserID());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Booking booking = createBookingFromResultSet(rs);
                if (booking != null) {
                    bookings.add(booking);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching bookings from database: " + e.getMessage());
            e.printStackTrace();
            showAlert("Database Error", "Failed to load bookings: " + e.getMessage());
        }
        
        return bookings;
    }
    
    private Booking createBookingFromResultSet(ResultSet rs) throws SQLException {
        try {
            // Create Route
            models.Route route = new models.Route(
                String.valueOf(rs.getInt("RouteID")),
                rs.getString("Source"),
                rs.getString("Destination"),
                rs.getDouble("BasePrice")
            );
            
            // Create Schedule
            models.Schedule schedule = new models.Schedule(
                String.valueOf(rs.getInt("ScheduleID")),
                rs.getDate("Date").toLocalDate(),
                rs.getTime("DepartureTime").toLocalTime(),
                rs.getTime("ArrivalTime").toLocalTime(),
                rs.getString("Class")
            );
            
            // Create Reservation
            models.Reservation reservation = new models.Reservation(
                String.valueOf(rs.getInt("ReservationID")),
                schedule,
                route,
                rs.getString("Class")
            );
            
            // Create Booking
            Booking booking = new Booking(
                String.valueOf(rs.getInt("BookingID")),
                rs.getString("CustomerID"),
                reservation,
                rs.getTimestamp("BookingDateTime")
            );
            
            booking.setTotalAmount(rs.getDouble("TotalAmount"));
            booking.setStatus(rs.getString("Status"));
            
            // Create Payment if exists
            String paymentId = rs.getString("PaymentID");
            if (paymentId != null && !rs.wasNull()) {
                Payment payment = new Payment(
                    paymentId,
                    booking,
                    rs.getDouble("PaymentAmount"),
                    rs.getString("PaymentMethod")
                );
                payment.setStatus(rs.getString("PaymentStatus"));
                booking.setPayment(payment);
            }
            
            return booking;
            
        } catch (Exception e) {
            System.err.println("Error creating booking object: " + e.getMessage());
            return null;
        }
    }
    
    private void applyFilters() {
        if (userBookings == null || userBookings.isEmpty()) {
            showNoBookings();
            return;
        }
        
        List<Booking> filteredBookings = new ArrayList<>(userBookings);
        
        // Apply status filter
        String status = statusFilter.getValue();
        if (!"All Bookings".equals(status)) {
            filteredBookings.removeIf(booking -> !booking.getStatus().equalsIgnoreCase(status));
        }
        
        // Apply payment filter
        String paymentStatus = paymentFilter.getValue();
        switch (paymentStatus) {
            case "Paid":
                filteredBookings.removeIf(booking -> !booking.isPaid());
                break;
            case "Unpaid":
                filteredBookings.removeIf(booking -> booking.hasPayment() && !booking.isPaid());
                break;
            case "Pending Payment":
                filteredBookings.removeIf(booking -> !booking.hasPayment() || booking.isPaid());
                break;
        }
        
        // Apply search filter
        String searchTerm = searchField.getText().toLowerCase();
        if (!searchTerm.isEmpty()) {
            filteredBookings.removeIf(booking -> 
                !booking.getBookingID().toLowerCase().contains(searchTerm) &&
                !booking.getReservation().getRoute().getSource().toLowerCase().contains(searchTerm) &&
                !booking.getReservation().getRoute().getDestination().toLowerCase().contains(searchTerm)
            );
        }
        
        displayBookings(filteredBookings);
    }
    
    private void displayBookings(List<Booking> bookings) {
        bookingsContainer.getChildren().clear();
        
        if (bookings.isEmpty()) {
            showNoBookings();
            return;
        }
        
        noBookingsText.setVisible(false);
        
        for (Booking booking : bookings) {
            VBox bookingCard = createBookingCard(booking);
            bookingsContainer.getChildren().add(bookingCard);
        }
    }
    
    private VBox createBookingCard(Booking booking) {
        VBox card = new VBox();
        card.getStyleClass().add("booking-card");
        
        models.Reservation reservation = booking.getReservation();
        models.Route route = reservation.getRoute();
        models.Schedule schedule = reservation.getSchedule();
        
        // Header section
        HBox header = new HBox();
        header.getStyleClass().add("booking-header");
        
        Label bookingId = new Label("Booking #" + booking.getBookingID());
        bookingId.getStyleClass().add("booking-id");
        
        // Status badge
        HBox statusBadges = new HBox();
        statusBadges.getStyleClass().add("status-badges");
        
        Label bookingStatus = new Label(booking.getStatus());
        bookingStatus.getStyleClass().addAll("status", getBookingStatusStyleClass(booking.getStatus()));
        
        Label paymentStatus = new Label(getPaymentStatusText(booking));
        paymentStatus.getStyleClass().addAll("status", getPaymentStatusStyleClass(booking));
        
        statusBadges.getChildren().addAll(bookingStatus, paymentStatus);
        
        HBox.setHgrow(bookingId, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().addAll(bookingId, statusBadges);
        
        // Route information
        VBox routeInfo = new VBox();
        routeInfo.getStyleClass().add("route-info");
        
        Label routeText = new Label(route.getSource() + " → " + route.getDestination());
        routeText.getStyleClass().add("route");
        
        Label scheduleText = new Label(
            schedule.getDate() + " • " + 
            schedule.getDepartureTime() + " - " + schedule.getArrivalTime()
        );
        scheduleText.getStyleClass().add("schedule");
        
        Label classText = new Label("Class: " + reservation.getSeatClass());
        classText.getStyleClass().add("class");
        
        routeInfo.getChildren().addAll(routeText, scheduleText, classText);
        
        // Details section
        HBox details = new HBox();
        details.getStyleClass().add("booking-details");
        
        VBox leftDetails = new VBox();
        leftDetails.getStyleClass().add("details-left");
        
        Label bookingDate = new Label("Booked: " + 
            new java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm").format(booking.getBookingDateTime()));
        bookingDate.getStyleClass().add("booking-date");
        
        Label price = new Label("Total: PKR " + booking.getTotalAmount());
        price.getStyleClass().add("price");
        
        leftDetails.getChildren().addAll(bookingDate, price);
        
        // Action buttons
        HBox actionButtons = new HBox();
        actionButtons.getStyleClass().add("action-buttons");
        
        Button viewTicketBtn = new Button("View E-Ticket");
        viewTicketBtn.getStyleClass().add("btn-primary");
        viewTicketBtn.setOnAction(e -> viewETicket(booking));
        
        // Show appropriate buttons based on payment status
        if (!booking.isPaid()) {
            Button payBtn = new Button("Pay Now");
            payBtn.getStyleClass().add("btn-pay");
            payBtn.setOnAction(e -> proceedToPayment(booking));
            actionButtons.getChildren().addAll(viewTicketBtn, payBtn);
        } else {
            // For paid bookings, show cancel button if it's not completed/cancelled
            if ("Confirmed".equals(booking.getStatus())) {
                Button cancelBtn = new Button("Cancel Booking");
                cancelBtn.getStyleClass().add("btn-danger");
                cancelBtn.setOnAction(e -> cancelBooking(booking));
                actionButtons.getChildren().addAll(viewTicketBtn, cancelBtn);
            } else {
                actionButtons.getChildren().add(viewTicketBtn);
            }
        }
        
        HBox.setHgrow(leftDetails, javafx.scene.layout.Priority.ALWAYS);
        details.getChildren().addAll(leftDetails, actionButtons);
        
        card.getChildren().addAll(header, routeInfo, details);
        
        return card;
    }
    
    private String getPaymentStatusText(Booking booking) {
        if (!booking.hasPayment()) {
            return "Unpaid";
        }
        return booking.getPayment().getStatus();
    }
    
    private String getBookingStatusStyleClass(String status) {
        switch (status.toLowerCase()) {
            case "confirmed": return "status-confirmed";
            case "pending": return "status-pending";
            case "cancelled": return "status-cancelled";
            case "completed": return "status-completed";
            default: return "status-pending";
        }
    }
    
    private String getPaymentStatusStyleClass(Booking booking) {
        if (!booking.hasPayment()) {
            return "status-unpaid";
        }
        String paymentStatus = booking.getPayment().getStatus();
        switch (paymentStatus.toLowerCase()) {
            case "completed": return "status-paid";
            case "pending": 
            case "processing": return "status-pending-payment";
            case "failed": return "status-cancelled";
            default: return "status-unpaid";
        }
    }
    
    private void showNoBookings() {
        bookingsContainer.getChildren().clear();
        noBookingsText.setVisible(true);
    }
    
    private void viewETicket(Booking booking) {
        try {
            if (!booking.isPaid()) {
                showAlert("Payment Required", "Please complete payment to view your E-Ticket.");
                return;
            }
            
            ETicket eTicket = booking.generateETicket();
            showAlert("E-Ticket", 
                "E-Ticket Details:\n" +
                "Ticket ID: " + eTicket.getTicketID() + "\n" +
                "Booking ID: " + booking.getBookingID() + "\n" +
                "Route: " + booking.getReservation().getRoute().getSource() + " → " + 
                         booking.getReservation().getRoute().getDestination() + "\n" +
                "Date: " + booking.getReservation().getSchedule().getDate() + "\n" +
                "Time: " + booking.getReservation().getSchedule().getDepartureTime() + " - " + 
                         booking.getReservation().getSchedule().getArrivalTime());
            
        } catch (Exception e) {
            showAlert("Error", "Failed to generate E-Ticket: " + e.getMessage());
        }
    }
    
    private void proceedToPayment(Booking booking) {
        // Create payment if doesn't exist
        if (!booking.hasPayment()) {
            booking.proceedToPayment();
        }
        
        Alert paymentAlert = new Alert(Alert.AlertType.CONFIRMATION);
        paymentAlert.setTitle("Proceed to Payment");
        paymentAlert.setHeaderText("Payment Required");
        paymentAlert.setContentText(
            "Proceed to payment for Booking #" + booking.getBookingID() + "?\n" +
            "Amount: PKR " + booking.getTotalAmount() + "\n" +
            "Route: " + booking.getReservation().getRoute().getSource() + " → " + 
                     booking.getReservation().getRoute().getDestination() + "\n\n" +
            "Note: This is a simulation. In production, you would integrate with a payment gateway."
        );
        
        paymentAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Simulate payment processing
                boolean paymentSuccess = simulatePayment(booking);
                
                if (paymentSuccess) {
                    booking.getPayment().confirmPayment();
                    savePaymentToDatabase(booking.getPayment());
                    showAlert("Payment Successful", 
                        "Payment completed for Booking #" + booking.getBookingID() + "\n" +
                        "Your tickets have been confirmed!");
                    // Refresh bookings to update status
                    loadUserBookingsFromDB();
                } else {
                    booking.getPayment().setStatus("Failed");
                    showAlert("Payment Failed", 
                        "Payment failed for Booking #" + booking.getBookingID() + "\n" +
                        "Please try again or contact support.");
                }
            }
        });
    }
    
    private boolean simulatePayment(Booking booking) {
        // Simulate payment processing - 90% success rate
        return Math.random() > 0.1;
    }
    
    private void savePaymentToDatabase(Payment payment) {
        // Implementation to save payment to database
        // This would update the Payment table with the confirmed payment
        System.out.println("Saving payment to database: " + payment.getPaymentID());
    }
    
    private void cancelBooking(Booking booking) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Cancel Booking");
        confirmAlert.setHeaderText("Confirm Cancellation");
        confirmAlert.setContentText(
            "Are you sure you want to cancel booking #" + booking.getBookingID() + "?\n" +
            "Route: " + booking.getReservation().getRoute().getSource() + " → " + 
                     booking.getReservation().getRoute().getDestination() + "\n" +
            "Cancellation fees may apply based on our policy."
        );
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (updateBookingStatus(booking.getBookingID(), "Cancelled")) {
                    showAlert("Booking Cancelled", 
                        "Booking #" + booking.getBookingID() + " has been cancelled successfully.");
                    loadUserBookingsFromDB();
                } else {
                    showAlert("Error", "Failed to cancel booking. Please try again.");
                }
            }
        });
    }
    
    private boolean updateBookingStatus(String bookingId, String status) {
        String query = "UPDATE Booking SET Status = ? WHERE BookingID = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, status);
            stmt.setString(2, bookingId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating booking status: " + e.getMessage());
            return false;
        }
    }
    
    private void handleBack() {
        try {
            DashboardController.show((Stage) backButton.getScene().getWindow(), currentUsername, currentCustomer);
        } catch (Exception e) {
            System.err.println("Error navigating back: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private static void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}