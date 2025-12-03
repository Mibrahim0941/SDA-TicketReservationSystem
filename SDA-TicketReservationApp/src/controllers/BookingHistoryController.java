package controllers;

import javafx.application.Platform;
import javafx.fxml.*;
import javafx.geometry.Insets;
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
import models.Payment;
import models.Seat;
import config.DatabaseConfig;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class BookingHistoryController implements Initializable {
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

    private String currentUsername;
    private Customer currentCustomer;
    private List<Booking> userBookings;
    
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();
    
    private static final String ALL_BOOKINGS = "All Bookings";
    private static final String ALL_PAYMENTS = "All Payments";

    public static void show(Stage stage, String username, Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(BookingHistoryController.class.getResource("/ui/booking-history.fxml"));
            Parent root = loader.load();
            
            BookingHistoryController controller = loader.getController();
            controller.setUserData(username, customer);
            
            Scene scene = new Scene(root, 1000, 700);
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

    private void loadUserBookings() {
        setLoadingState(true);
        new Thread(() -> {
            try {
                List<Booking> bookings = fetchBookingsFromDatabase();
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

        Map<String, Booking> bookingMap = new HashMap<>();
        Map<String, String> bookingReservationMap = new HashMap<>();
            
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, currentCustomer.getUserID());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Booking booking = createBookingFromResultSet(rs);
                if (booking != null) {
                    String bookingId = booking.getBookingID();
                    String reservationId = booking.getReservation().getReservationID();
                    
                    bookingMap.put(bookingId, booking);
                    bookingReservationMap.put(bookingId, reservationId);
                    bookings.add(booking);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching bookings: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> showAlert("Database Error", "Failed to load bookings: " + e.getMessage()));
            return bookings;
        }
        
        if (!bookingReservationMap.isEmpty()) {
            String seatQuery = 
                "SELECT ReservationID, SeatNumber, SeatType, Price, Availability " +
                "FROM Seat " +
                "WHERE ReservationID IN (" + 
                String.join(",", Collections.nCopies(bookingReservationMap.size(), "?")) + 
                ")";
            
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                PreparedStatement stmt = conn.prepareStatement(seatQuery)) {
                
                int i = 1;
                for (String reservationId : bookingReservationMap.values()) {
                    stmt.setString(i++, reservationId);
                }
                
                ResultSet rs = stmt.executeQuery();
                Map<String, List<Seat>> reservationSeatsMap = new HashMap<>();
                
                while (rs.next()) {
                    String reservationId = rs.getString("ReservationID");
                    Seat seat = new Seat(
                        rs.getString("SeatNumber"),
                        rs.getString("SeatType"),
                        rs.getDouble("Price")
                    );
                    seat.setAvailability(rs.getBoolean("Availability"));
                    
                    reservationSeatsMap
                        .computeIfAbsent(reservationId, k -> new ArrayList<>())
                        .add(seat);
                }

                for (Booking booking : bookings) {
                    String reservationId = booking.getReservation().getReservationID();
                    List<Seat> seats = reservationSeatsMap.get(reservationId);
                    
                    if (seats != null && !seats.isEmpty()) {
                        booking.getReservation().getSeats().addAll(seats);
                    }
                }
                
            } catch (SQLException e) {
                System.err.println("Error fetching seats: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return bookings;
    }

    private Booking createBookingFromResultSet(ResultSet rs) throws SQLException {
        try {
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

    private void applyFilters() {
        if (userBookings == null || userBookings.isEmpty()) {
            showNoBookings();
            return;
        }
        
        List<Booking> filteredBookings = new ArrayList<>(userBookings);
        String status = statusFilter.getValue();
        if (!ALL_BOOKINGS.equals(status)) {
            filteredBookings.removeIf(booking -> !booking.getStatus().equalsIgnoreCase(status));
        }
        
        String paymentStatus = paymentFilter.getValue();
        switch (paymentStatus) {
            case "Paid": filteredBookings.removeIf(b -> !b.isPaid()); break;
            case "Unpaid": filteredBookings.removeIf(b -> b.hasPayment() && b.isPaid()); break;
            case "Pending Payment": filteredBookings.removeIf(b -> b.hasPayment() || b.isPaid()); break;
            case "Refunded": filteredBookings.removeIf(b -> !"Refunded".equals(b.getPaymentStatus())); break;
        }
        
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
        card.getStyleClass().add("booking-card"); 
        
        models.Reservation reservation = booking.getReservation();
        models.Route route = reservation.getRoute();
        models.Schedule schedule = reservation.getSchedule();
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
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(bookingId, spacer, statusBadges);
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
        
        HBox actionButtons = new HBox(10); 
        actionButtons.getStyleClass().add("action-buttons");
        
        Button viewDetailsBtn = new Button("View Details");
        viewDetailsBtn.getStyleClass().add("btn-secondary");
        viewDetailsBtn.setOnAction(e -> showBookingDetails(booking));
     
        actionButtons.getChildren().add(viewDetailsBtn);
        
        HBox detailSpacer = new HBox();
        HBox.setHgrow(detailSpacer, Priority.ALWAYS);
        details.getChildren().addAll(leftDetails, detailSpacer, actionButtons);
        
        card.getChildren().addAll(header, routeInfo, details);
        return card;
    }

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

    private void addTicketDetail(GridPane grid, String label, String value, int col, int row) {
        VBox box = new VBox(2);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
        Label v = new Label(value);
        v.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        box.getChildren().addAll(l, v);
        grid.add(box, col, row);
    }

    private void showBookingDetails(Booking booking) {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Booking Details");
            dialog.setHeaderText(null);
            dialog.setGraphic(null);
            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/ui/MyBookings.css").toExternalForm());
            dialogPane.getStyleClass().add("ticket-dialog");
            VBox card = new VBox(15);
            card.getStyleClass().add("ticket-card");
            card.setMinWidth(400);
            card.setAlignment(javafx.geometry.Pos.TOP_CENTER);
            Label title = new Label("BOOKING DETAILS");
            title.getStyleClass().add("details-title"); 
            
            Separator sep1 = new Separator();
            sep1.getStyleClass().add("details-separator");

            String source = booking.getReservation().getRoute().getSource();
            String dest = booking.getReservation().getRoute().getDestination();
            Label routeLbl = new Label(source + " ➝ " + dest);
            routeLbl.getStyleClass().add("details-route"); 
            GridPane grid = new GridPane();
            grid.setHgap(30);
            grid.setVgap(15);
            grid.setAlignment(javafx.geometry.Pos.CENTER);

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");

            addTicketDetail(grid, "Booking ID:", "#" + booking.getBookingID(), 0, 0);
            addTicketDetail(grid, "Booked On:", dateFormat.format(booking.getBookingDateTime()), 1, 0);

            addTicketDetail(grid, "Travel Date:", booking.getReservation().getSchedule().getDate().toString(), 0, 1);
            addTicketDetail(grid, "Time:", booking.getReservation().getSchedule().getDepartureTime() + " - " + booking.getReservation().getSchedule().getArrivalTime(), 1, 1);

            addTicketDetail(grid, "Class:", booking.getReservation().getSeatClass(), 0, 2);
            addTicketDetail(grid, "Total Amount:", "PKR " + String.format("%.2f", booking.getTotalAmount()), 1, 2);

            HBox badgeBox = new HBox(10);
            badgeBox.setAlignment(javafx.geometry.Pos.CENTER);
            badgeBox.setPadding(new Insets(10, 0, 0, 0));

            Label statusBadge = new Label(booking.getStatus());
            statusBadge.getStyleClass().addAll("status", getBookingStatusStyleClass(booking.getStatus()));
            
            Label paymentBadge = new Label(getPaymentStatusText(booking));
            paymentBadge.getStyleClass().addAll("status", getPaymentStatusStyleClass(booking));
            
            badgeBox.getChildren().addAll(statusBadge, paymentBadge);

            Separator sep2 = new Separator();
            sep2.setVisible(false); 
            
            card.getChildren().addAll(title, sep1, routeLbl, grid, sep2, badgeBox);
            
            dialogPane.setContent(card);
            dialogPane.getButtonTypes().add(ButtonType.CLOSE);
            Button closeBtn = (Button) dialogPane.lookupButton(ButtonType.CLOSE);
            closeBtn.getStyleClass().add("btn-primary");

            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Could not open details: " + e.getMessage());
            alert.show();
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