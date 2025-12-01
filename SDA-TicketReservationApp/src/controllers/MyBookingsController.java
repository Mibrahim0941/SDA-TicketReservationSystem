package controllers;

import javafx.application.Platform;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.Scene;
import javafx.scene.Parent;
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
import config.DatabaseConfig;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MyBookingsController implements Initializable {

    // --- FXML Components ---
    @FXML private Text pageTitle;
    @FXML private Text userGreeting;
    @FXML private Text noBookingsText;
    @FXML private VBox bookingsContainer; // This is inside the ScrollPane in FXML
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
            FXMLLoader loader = new FXMLLoader(MyBookingsController.class.getResource("/ui/myBookings.fxml"));
            Parent root = loader.load();
            
            MyBookingsController controller = loader.getController();
            controller.setUserData(username, customer);
            
            Scene scene = new Scene(root, 1000, 700);
            
            // Explicitly load CSS to ensure styling applies immediately
            URL stylesheet = MyBookingsController.class.getResource("/ui/MyBookings.css");
            if (stylesheet != null) {
                scene.getStylesheets().add(stylesheet.toExternalForm());
            }
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - My Bookings");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            showErrorAlert("Failed to load My Bookings: " + e.getMessage());
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
        statusFilter.getItems().addAll(ALL_BOOKINGS, "Confirmed", "Pending", "Cancelled", "Completed");
        statusFilter.setValue(ALL_BOOKINGS);
        
        paymentFilter.getItems().addAll(ALL_PAYMENTS, "Paid", "Unpaid", "Pending Payment");
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
            userGreeting.setText("Hello, " + displayName + "! 👋");
        }
    }

    // --- Data Loading Logic ---

    private void loadUserBookings() {
        setLoadingState(true);
        
        // Run database operation in background thread
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
        
        // Concatenated query compatible with older Java versions
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
                rs.getString("RouteID"),
                rs.getString("Source"),
                rs.getString("Destination"),
                rs.getDouble("BasePrice")
            );
            
            models.Schedule schedule = new models.Schedule(
                rs.getString("ScheduleID"),
                rs.getDate("Date").toLocalDate(),
                rs.getTime("DepartureTime").toLocalTime(),
                rs.getTime("ArrivalTime").toLocalTime(),
                rs.getString("Class")
            );
            
            models.Reservation reservation = new models.Reservation(
                rs.getString("ReservationID"),
                schedule,
                route,
                rs.getString("Class")
            );
            
            Booking booking = new Booking(
                rs.getString("BookingID"),
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

    // --- UI Generation (The Card System) ---

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
     * Creates a card styled via CSS (.booking-card) containing all booking info.
     */
    private VBox createBookingCard(Booking booking) {
        VBox card = new VBox(15);
        card.getStyleClass().add("booking-card"); // CRITICAL: Applies white bg and shadow
        
        models.Reservation reservation = booking.getReservation();
        models.Route route = reservation.getRoute();
        models.Schedule schedule = reservation.getSchedule();
        
        // 1. HEADER (ID + Status Badges)
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
        
        // 2. ROUTE INFO
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
        
        // 3. FOOTER (Details + Actions)
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
        
        // Action Buttons
        HBox actionButtons = new HBox(10); 
        actionButtons.getStyleClass().add("action-buttons");
        
        Button viewTicketBtn = new Button("View E-Ticket");
        viewTicketBtn.getStyleClass().add("btn-primary");
        viewTicketBtn.setOnAction(e -> viewETicket(booking));
        
        // Logic for which buttons to show
        if (!booking.isPaid()) {
            Button payBtn = new Button("Pay Now");
            payBtn.getStyleClass().add("btn-pay");
            payBtn.setOnAction(e -> proceedToPayment(booking));
            actionButtons.getChildren().addAll(viewTicketBtn, payBtn);
        } else {
            // Only show Cancel if Confirmed
            if ("Confirmed".equals(booking.getStatus())) {
                Button cancelBtn = new Button("Cancel Booking");
                cancelBtn.getStyleClass().add("btn-danger");
                cancelBtn.setOnAction(e -> cancelBooking(booking));
                actionButtons.getChildren().addAll(viewTicketBtn, cancelBtn);
            } else {
                actionButtons.getChildren().add(viewTicketBtn);
            }
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
            
            // --- Build Custom Ticket UI ---
            VBox ticketCard = new VBox(15);
            ticketCard.getStyleClass().add("ticket-card");
            ticketCard.setMinWidth(400);

            // 1. Ticket Header
            HBox header = new HBox();
            header.setAlignment(javafx.geometry.Pos.CENTER);
            Label title = new Label("E-TICKET CONFIRMATION");
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: #3F5F3C; -fx-font-size: 16px;");
            header.getChildren().add(title);

            // 2. Dashed Line Separator
            Separator sep1 = new Separator();
            sep1.setStyle("-fx-border-style: dashed; -fx-border-width: 1px 0 0 0; -fx-border-color: #ccc; -fx-background-color: transparent;");

            // 3. Route Info (Large)
            VBox routeBox = new VBox(5);
            routeBox.setAlignment(javafx.geometry.Pos.CENTER);
            Label routeLbl = new Label(
                booking.getReservation().getRoute().getSource() + " ➝ " + 
                booking.getReservation().getRoute().getDestination()
            );
            routeLbl.getStyleClass().add("route");
            routeLbl.setStyle("-fx-font-size: 22px;"); // Make it bigger for the ticket
            routeBox.getChildren().add(routeLbl);

            // 4. Details Grid (Date, Time, Class, IDs)
            GridPane detailsGrid = new GridPane();
            detailsGrid.setHgap(20);
            detailsGrid.setVgap(10);
            detailsGrid.setAlignment(javafx.geometry.Pos.CENTER);

            // Helper to add styled rows
            addTicketDetail(detailsGrid, "Date:", booking.getReservation().getSchedule().getDate().toString(), 0, 0);
            addTicketDetail(detailsGrid, "Time:", booking.getReservation().getSchedule().getDepartureTime() + " - " + booking.getReservation().getSchedule().getArrivalTime(), 0, 1);
            addTicketDetail(detailsGrid, "Class:", booking.getReservation().getSeatClass(), 1, 0);
            addTicketDetail(detailsGrid, "Price:", "PKR " + booking.getTotalAmount(), 1, 1);
            
            // 5. Footer (IDs)
            VBox footer = new VBox(5);
            footer.setAlignment(javafx.geometry.Pos.CENTER);
            footer.setPadding(new javafx.geometry.Insets(15, 0, 0, 0));
            Label ticketId = new Label("Ticket ID: " + eTicket.getTicketID());
            Label bookingId = new Label("Booking Ref: " + booking.getBookingID());
            ticketId.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");
            bookingId.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");
            footer.getChildren().addAll(ticketId, bookingId);

            // Combine all
            ticketCard.getChildren().addAll(header, sep1, routeBox, detailsGrid, new Separator(), footer);

            // --- Show in Dialog ---
            Alert ticketDialog = new Alert(Alert.AlertType.NONE);
            ticketDialog.setTitle("Your E-Ticket");
            ticketDialog.getDialogPane().setContent(ticketCard);
            ticketDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            
            // Remove default header/graphic to keep it clean
            ticketDialog.setHeaderText(null);
            ticketDialog.setGraphic(null);
            
            // Apply CSS to the dialog
            DialogPane dialogPane = ticketDialog.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/ui/MyBookings.css").toExternalForm());
            dialogPane.getStyleClass().add("ticket-dialog");

            ticketDialog.showAndWait();

        } catch (Exception e) {
            showAlert("Error", "Failed to generate E-Ticket: " + e.getMessage());
        }
    }

    // Helper method for the Grid layout
    private void addTicketDetail(GridPane grid, String label, String value, int col, int row) {
        VBox box = new VBox(2);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
        Label v = new Label(value);
        v.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        box.getChildren().addAll(l, v);
        grid.add(box, col, row);
    }

    private void proceedToPayment(Booking booking) {
        if (!booking.hasPayment()) {
            booking.proceedToPayment();
        }
        
        Alert paymentAlert = new Alert(Alert.AlertType.CONFIRMATION);
        paymentAlert.setTitle("Proceed to Payment");
        paymentAlert.setHeaderText("Payment Required");
        paymentAlert.setContentText("Proceed to payment for Booking #" + booking.getBookingID() + "?\n" +
            "Amount: PKR " + String.format("%.2f", booking.getTotalAmount()));
        
        paymentAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean paymentSuccess = simulatePayment(booking);
                if (paymentSuccess) {
                    booking.getPayment().confirmPayment();
                    savePaymentToDatabase(booking.getPayment());
                    showAlert("Payment Successful", "Payment completed for Booking #" + booking.getBookingID());
                    loadUserBookings(); // Refresh UI
                } else {
                    booking.getPayment().setStatus("Failed");
                    showAlert("Payment Failed", "Payment failed. Please try again.");
                }
            }
        });
    }

    private boolean simulatePayment(Booking booking) {
        return Math.random() > 0.1; // 90% success rate simulation
    }

    private void savePaymentToDatabase(Payment payment) {
        // Implementation would execute an UPDATE/INSERT SQL query here
        System.out.println("Saving payment to database: " + payment.getPaymentID() + " Status: " + payment.getStatus());
    }

    private void cancelBooking(Booking booking) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Cancel Booking");
        confirmAlert.setContentText("Are you sure you want to cancel booking #" + booking.getBookingID() + "?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (updateBookingStatus(booking.getBookingID(), "Cancelled")) {
                    booking.setStatus("Cancelled"); 
                    showAlert("Booking Cancelled", "Booking cancelled successfully.");
                    loadUserBookings();
                } else {
                    showAlert("Error", "Failed to cancel booking.");
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
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating status: " + e.getMessage());
            return false;
        }
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