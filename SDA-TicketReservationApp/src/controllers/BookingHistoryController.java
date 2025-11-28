package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Booking;
import models.Customer;
import models.ETicket;
import models.Payment;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import config.DatabaseConfig;

public class BookingHistoryController implements Initializable {

    @FXML private Label pageTitle;
    @FXML private Label userGreeting;
    @FXML private Label noBookingsText;
    
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
            FXMLLoader loader = new FXMLLoader(BookingHistoryController.class.getResource("/ui/booking-history.fxml"));
            Parent root = loader.load();
            
            BookingHistoryController controller = loader.getController();
            controller.setUserData(username, customer);
            
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(BookingHistoryController.class.getResource("/ui/booking-history.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Booking History");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to load Booking History: " + e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("BookingHistoryController initialized");
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
            userGreeting.setText("Hello, " + currentUsername + "! 📜");
        }
    }
    
    private void setupFilters() {
        // Status filter options - including historical statuses
        statusFilter.getItems().addAll("All Bookings", "Confirmed", "Completed", "Cancelled", "Pending", "Refunded");
        statusFilter.setValue("All Bookings");
        
        // Payment status filter
        paymentFilter.getItems().addAll("All Payments", "Paid", "Unpaid", "Pending Payment", "Refunded");
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
                "BK" + rs.getInt("BookingID"), // Format booking ID
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
            case "Refunded":
                filteredBookings.removeIf(booking -> !"Refunded".equals(booking.getPaymentStatus()));
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
            new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm").format(booking.getBookingDateTime()));
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
        
        Button viewDetailsBtn = new Button("View Details");
        viewDetailsBtn.getStyleClass().add("btn-secondary");
        viewDetailsBtn.setOnAction(e -> showBookingDetails(booking));
        
        // Show appropriate buttons based on status
        if (booking.isPaid() && ("Confirmed".equals(booking.getStatus()) || "Completed".equals(booking.getStatus()))) {
            actionButtons.getChildren().addAll(viewTicketBtn, viewDetailsBtn);
        } else {
            actionButtons.getChildren().add(viewDetailsBtn);
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
            case "completed": return "status-completed";
            case "cancelled": return "status-cancelled";
            case "pending": return "status-pending";
            case "refunded": return "status-refunded";
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
            case "refunded": return "status-refunded";
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
    
    private void showBookingDetails(Booking booking) {
        StringBuilder details = new StringBuilder();
        details.append("📋 Booking Details\n\n");
        details.append("Booking ID: ").append(booking.getBookingID()).append("\n");
        details.append("Status: ").append(booking.getStatus()).append("\n");
        details.append("Payment Status: ").append(booking.getPaymentStatus()).append("\n");
        details.append("Booked On: ").append(new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm").format(booking.getBookingDateTime())).append("\n");
        details.append("Total Amount: PKR ").append(booking.getTotalAmount()).append("\n\n");
        
        if (booking.getReservation() != null) {
            details.append("🚌 Journey Details\n\n");
            details.append("Route: ").append(booking.getReservation().getRoute().getSource())
                   .append(" → ").append(booking.getReservation().getRoute().getDestination()).append("\n");
            details.append("Travel Date: ").append(booking.getReservation().getSchedule().getDate()).append("\n");
            details.append("Departure: ").append(booking.getReservation().getSchedule().getDepartureTime()).append("\n");
            details.append("Arrival: ").append(booking.getReservation().getSchedule().getArrivalTime()).append("\n");
            details.append("Class: ").append(booking.getReservation().getSeatClass()).append("\n");
        }
        
        Alert detailsAlert = new Alert(Alert.AlertType.INFORMATION);
        detailsAlert.setTitle("Booking Details");
        detailsAlert.setHeaderText("Complete Booking Information");
        detailsAlert.setContentText(details.toString());
        detailsAlert.showAndWait();
    }
    // Sample structure for creating booking cards in your controller
    private VBox createBookingCard(Booking booking) {
        VBox card = new VBox();
        card.getStyleClass().add("booking-card");
    
        // Header with booking ID and status
        HBox header = new HBox();
        header.getStyleClass().add("booking-header");
    
        VBox bookingInfo = new VBox();
        Label bookingId = new Label("Booking #" + booking.getId());
        bookingId.getStyleClass().add("booking-id");
    
        Label bookingDate = new Label("Booked on: " + booking.getBookingDate());
        bookingDate.getStyleClass().add("booking-date");
    
        bookingInfo.getChildren().addAll(bookingId, bookingDate);
    
        HBox statusBadges = new HBox();
        statusBadges.getStyleClass().add("status-badges");
    
        Label statusLabel = new Label(booking.getStatus());
        statusLabel.getStyleClass().addAll("status", "status-" + booking.getStatus().toLowerCase());
    
        Label paymentLabel = new Label(booking.getPaymentStatus());
        paymentLabel.getStyleClass().addAll("status", "status-" + booking.getPaymentStatus().toLowerCase().replace(" ", "-"));
    
        statusBadges.getChildren().addAll(statusLabel, paymentLabel);
    
        HBox.setHgrow(bookingInfo, Priority.ALWAYS);
        header.getChildren().addAll(bookingInfo, statusBadges);
    
        // Route information
        VBox routeSection = new VBox();
        routeSection.getStyleClass().add("route-section");
    
        Label route = new Label(booking.getDeparture() + " → " + booking.getDestination());
        route.getStyleClass().add("route");
    
        Label schedule = new Label(booking.getDepartureTime() + " - " + booking.getArrivalTime());
        schedule.getStyleClass().add("schedule");
    
        routeSection.getChildren().addAll(route, schedule);
    
        // Booking details grid
        GridPane detailsGrid = new GridPane();
        detailsGrid.getStyleClass().add("details-grid");
    
        // Add detail items (passengers, class, etc.)
        // ... details implementation
    
        // Price section
        VBox priceSection = new VBox();
        priceSection.getStyleClass().add("price-section");
    
        Label price = new Label("$" + booking.getTotalPrice());
        price.getStyleClass().add("price");
    
        priceSection.getChildren().add(price);
    
        // Action buttons
        HBox actionButtons = new HBox();
        actionButtons.getStyleClass().add("action-buttons");
    
        Button viewDetails = new Button("View Details");
        viewDetails.getStyleClass().add("btn-primary");
    
        Button cancelBooking = new Button("Cancel");
        cancelBooking.getStyleClass().add("btn-danger");
    
        actionButtons.getChildren().addAll(viewDetails, cancelBooking);
    
        card.getChildren().addAll(header, routeSection, detailsGrid, priceSection, actionButtons);
        return card;
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