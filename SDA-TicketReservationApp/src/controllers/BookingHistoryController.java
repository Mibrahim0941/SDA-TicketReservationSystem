package controllers;

import javafx.application.Platform;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import models.Booking;
import models.Customer;
import models.ETicket;
import models.Payment;
import config.DatabaseConfig;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class BookingHistoryController implements Initializable {

    // --- FXML Components ---
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
    @FXML private ProgressIndicator loadingIndicator;

    // --- State & Config ---
    private String currentUsername;
    private Customer currentCustomer;
    private List<Booking> userBookings;
    
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();
    
    private static final String ALL_BOOKINGS = "All Bookings";
    private static final String ALL_PAYMENTS = "All Payments";

    // --- Initialization & Navigation ---

    public static void show(Stage stage, String username, Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(BookingHistoryController.class.getResource("/ui/booking-history.fxml"));
            Parent root = loader.load();
            
            BookingHistoryController controller = loader.getController();
            controller.setUserData(username, customer);
            
            Scene scene = new Scene(root, 1000, 700);
            
            // Load CSS explicitly like MyBookings
            URL stylesheet = BookingHistoryController.class.getResource("/ui/booking-history.css");
            if (stylesheet != null) {
                scene.getStylesheets().add(stylesheet.toExternalForm());
            }
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Booking History");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            showErrorAlert("Failed to load Booking History: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilters();
        setupEventHandlers();
        initializeUIState();
    }

    public void setUserData(String username, Customer customer) {
        this.currentUsername = username;
        this.currentCustomer = customer;
        updateUserGreeting();
        loadUserBookings();
    }

    private void initializeUIState() {
        if(loadingIndicator != null) loadingIndicator.setVisible(false);
        if(noBookingsText != null) noBookingsText.setVisible(false);
    }

    private void setupFilters() {
        statusFilter.getItems().addAll(ALL_BOOKINGS, "Confirmed", "Pending", "Cancelled", "Completed", "Refunded");
        statusFilter.setValue(ALL_BOOKINGS);
        
        paymentFilter.getItems().addAll(ALL_PAYMENTS, "Paid", "Unpaid", "Pending Payment", "Refunded");
        paymentFilter.setValue(ALL_PAYMENTS);
        
        searchField.setPromptText("Search by booking ID, route...");
    }

    private void setupEventHandlers() {
        backButton.setOnAction(e -> handleBack());
        refreshButton.setOnAction(e -> loadUserBookings());
        
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        paymentFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void updateUserGreeting() {
        if (currentUsername != null && !currentUsername.isEmpty()) {
            String displayName = currentUsername.substring(0, 1).toUpperCase() + 
                               currentUsername.substring(1).toLowerCase();
            userGreeting.setText("Hello, " + displayName + "! 📜");
        }
    }

    // --- Data Loading Logic ---

    private void loadUserBookings() {
        setLoadingState(true);
        
        // Run database operation in background thread like MyBookings
        new Thread(() -> {
            try {
                List<Booking> bookings = fetchBookingsFromDatabase();
                
                // Update UI on JavaFX Application Thread
                Platform.runLater(() -> {
                    userBookings = bookings;
                    applyFilters();
                    setLoadingState(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Load Error", "Failed to load bookings: " + e.getMessage());
                    setLoadingState(false);
                });
            }
        }).start();
    }

    private List<Booking> fetchBookingsFromDatabase() {
        List<Booking> bookings = new ArrayList<>();
        
        String query = "SELECT " +
                       "    b.BookingID, b.CustomerID, b.BookingDateTime, b.TotalAmount, b.Status, " +
                       "    p.PaymentID, p.PaymentStatus, p.PaymentMethod, p.Amount as PaymentAmount, " +
                       "    r.RouteID, r.Source, r.Destination, r.BasePrice, " +
                       "    s.ScheduleID, s.Date, s.DepartureTime, s.ArrivalTime, s.Class, " +
                       "    res.ReservationID " +
                       "FROM Booking b " +
                       "LEFT JOIN Payment p ON b.PaymentID = p.PaymentID " +
                       "LEFT JOIN Reservation res ON b.ReservationID = res.ReservationID " +
                       "LEFT JOIN Route r ON res.RouteID = r.RouteID " +
                       "LEFT JOIN Schedule s ON res.ScheduleID = s.ScheduleID " +
                       "WHERE b.CustomerID = ? " +
                       "ORDER BY b.BookingDateTime DESC";
            
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
            System.err.println("Error fetching bookings: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> showAlert("Database Error", "Failed to load bookings: " + e.getMessage()));
        }
        
        return bookings;
    }

    private Booking createBookingFromResultSet(ResultSet rs) throws SQLException {
        try {
            // Reconstruct object graph from flat result set
            models.Route route = new models.Route(
                String.valueOf(rs.getInt("RouteID")),
                rs.getString("Source"),
                rs.getString("Destination"),
                rs.getDouble("BasePrice")
            );
            
            models.Schedule schedule = new models.Schedule(
                String.valueOf(rs.getInt("ScheduleID")),
                rs.getDate("Date").toLocalDate(),
                rs.getTime("DepartureTime").toLocalTime(),
                rs.getTime("ArrivalTime").toLocalTime(),
                rs.getString("Class")
            );
            
            models.Reservation reservation = new models.Reservation(
                String.valueOf(rs.getInt("ReservationID")),
                schedule,
                route,
                rs.getString("Class")
            );
            
            Booking booking = new Booking(
                "BK" + rs.getInt("BookingID"),
                rs.getString("CustomerID"),
                reservation,
                rs.getTimestamp("BookingDateTime")
            );
            
            booking.setTotalAmount(rs.getDouble("TotalAmount"));
            booking.setStatus(rs.getString("Status"));
            
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

    // --- UI Generation (Same Card System as MyBookings) ---

    private void applyFilters() {
        if (userBookings == null || userBookings.isEmpty()) {
            showNoBookings();
            return;
        }
        
        List<Booking> filteredBookings = new ArrayList<>(userBookings);
        
        // Status Filter
        String status = statusFilter.getValue();
        if (!ALL_BOOKINGS.equals(status)) {
            filteredBookings.removeIf(booking -> !booking.getStatus().equalsIgnoreCase(status));
        }
        
        // Payment Filter
        String paymentStatus = paymentFilter.getValue();
        switch (paymentStatus) {
            case "Paid": filteredBookings.removeIf(b -> !b.isPaid()); break;
            case "Unpaid": filteredBookings.removeIf(b -> b.hasPayment() && b.isPaid()); break;
            case "Pending Payment": filteredBookings.removeIf(b -> b.hasPayment() || b.isPaid()); break;
            case "Refunded": filteredBookings.removeIf(b -> !"Refunded".equals(b.getPaymentStatus())); break;
        }
        
        // Search Filter
        String searchTerm = searchField.getText().toLowerCase();
        if (!searchTerm.isEmpty()) {
            filteredBookings.removeIf(b -> 
                !b.getBookingID().toLowerCase().contains(searchTerm) &&
                !b.getReservation().getRoute().getSource().toLowerCase().contains(searchTerm) &&
                !b.getReservation().getRoute().getDestination().toLowerCase().contains(searchTerm)
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

    /**
     * Creates a card styled via CSS (.booking-card) - EXACT SAME as MyBookings
     */
    private VBox createBookingCard(Booking booking) {
        VBox card = new VBox(15);
        card.getStyleClass().add("booking-card"); // CRITICAL: Applies white bg and shadow
        
        models.Reservation reservation = booking.getReservation();
        models.Route route = reservation.getRoute();
        models.Schedule schedule = reservation.getSchedule();
        
        // 1. HEADER (ID + Status Badges) - EXACT SAME as MyBookings
        HBox header = new HBox(10); 
        header.getStyleClass().add("booking-header");
        
        Label bookingId = new Label("Booking #" + booking.getBookingID());
        bookingId.getStyleClass().add("booking-id");
        
        HBox statusBadges = new HBox(8); 
        statusBadges.getStyleClass().add("status-badges");
        
        Label bookingStatus = new Label(booking.getStatus());
        bookingStatus.getStyleClass().addAll("status", getBookingStatusStyleClass(booking.getStatus()));
        
        Label paymentStatus = new Label(getPaymentStatusText(booking));
        paymentStatus.getStyleClass().addAll("status", getPaymentStatusStyleClass(booking));
        
        statusBadges.getChildren().addAll(bookingStatus, paymentStatus);
        
        // Push badges to the right
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(bookingId, spacer, statusBadges);
        
        // 2. ROUTE INFO - EXACT SAME as MyBookings
        VBox routeInfo = new VBox(8); 
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
        
        // 3. FOOTER (Details + Actions) - EXACT SAME as MyBookings
        HBox details = new HBox(10); 
        details.getStyleClass().add("booking-details");
        details.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        VBox leftDetails = new VBox(5); 
        leftDetails.getStyleClass().add("details-left");
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm");
        Label bookingDate = new Label("Booked: " + dateFormat.format(booking.getBookingDateTime()));
        bookingDate.getStyleClass().add("booking-date");
        
        Label price = new Label("Total: PKR " + String.format("%.2f", booking.getTotalAmount()));
        price.getStyleClass().add("price");
        
        leftDetails.getChildren().addAll(bookingDate, price);
        
        // Action Buttons - For Booking History, we only show View actions
        HBox actionButtons = new HBox(10); 
        actionButtons.getStyleClass().add("action-buttons");
        
        Button viewTicketBtn = new Button("View E-Ticket");
        viewTicketBtn.getStyleClass().add("btn-primary");
        viewTicketBtn.setOnAction(e -> viewETicket(booking));
        
        Button viewDetailsBtn = new Button("View Details");
        viewDetailsBtn.getStyleClass().add("btn-secondary");
        viewDetailsBtn.setOnAction(e -> showBookingDetails(booking));
        
        // For Booking History, only show View actions (no payment or cancellation)
        if (booking.isPaid() && ("Confirmed".equals(booking.getStatus()) || "Completed".equals(booking.getStatus()))) {
            actionButtons.getChildren().addAll(viewTicketBtn, viewDetailsBtn);
        } else {
            actionButtons.getChildren().add(viewDetailsBtn);
        }
        
        HBox detailSpacer = new HBox();
        HBox.setHgrow(detailSpacer, Priority.ALWAYS);
        details.getChildren().addAll(leftDetails, detailSpacer, actionButtons);
        
        card.getChildren().addAll(header, routeInfo, details);
        return card;
    }

    // --- Helpers & Actions ---

    private String getPaymentStatusText(Booking booking) {
        if (!booking.hasPayment()) return "Unpaid";
        return booking.getPayment().getStatus();
    }

    private String getBookingStatusStyleClass(String status) {
        if(status == null) return "status-pending";
        switch (status.toLowerCase()) {
            case "confirmed": return "status-confirmed";
            case "pending": return "status-pending";
            case "cancelled": return "status-cancelled";
            case "completed": return "status-completed";
            case "refunded": return "status-refunded";
            default: return "status-pending";
        }
    }

    private String getPaymentStatusStyleClass(Booking booking) {
        if (!booking.hasPayment()) return "status-unpaid";
        String status = booking.getPayment().getStatus();
        if(status == null) return "status-unpaid";
        
        switch (status.toLowerCase()) {
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
        if(!bookingsContainer.getChildren().contains(noBookingsText)) {
            bookingsContainer.getChildren().add(noBookingsText);
        }
    }

    private void setLoadingState(boolean loading) {
        if(loadingIndicator != null) loadingIndicator.setVisible(loading);
        if(bookingsContainer != null) bookingsContainer.setVisible(!loading);
        if(filterContainer != null) filterContainer.setDisable(loading);
        if(refreshButton != null) refreshButton.setDisable(loading);
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
                "Route: " + booking.getReservation().getRoute().getSource() + " -> " + 
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
        details.append("Payment Status: ").append(getPaymentStatusText(booking)).append("\n");
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

    private void handleBack() {
        try {
            Stage currentStage = (Stage) backButton.getScene().getWindow();
            DashboardController.show(currentStage, currentUsername, currentCustomer);
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