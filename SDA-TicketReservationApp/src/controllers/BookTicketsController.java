package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import catalogs.BookingCatalog;
import catalogs.RouteCatalog;
import config.DatabaseConfig;
import models.*;
import database.DatabaseConnection;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class BookTicketsController implements Initializable {

    // --- FXML Injection ---
    @FXML private Text pageTitle;
    @FXML private Label subTitle;
    @FXML private Button backButton;

    // Search
    @FXML private HBox searchLayer;
    @FXML private TextField searchSource;
    @FXML private TextField searchDestination;
    @FXML private Button searchButton;
    @FXML private Button clearButton;

    // Layers
    @FXML private ScrollPane routeLayer;
    @FXML private ScrollPane scheduleLayer;
    @FXML private ScrollPane seatLayer;

    // Containers
    @FXML private VBox routesContainer;
    @FXML private VBox schedulesContainer;
    @FXML private VBox seatsContainer;

    // Summary
    @FXML private VBox summaryLayer;
    @FXML private Text lblSelectedSeats;
    @FXML private Text lblTotalPrice;
    @FXML private Button btnConfirm;

    // --- Data ---
    private Customer currentCustomer;
    private RouteCatalog routeCatalog;
    private BookingCatalog bookingCatalog;
    private Route selectedRoute;
    private Schedule selectedSchedule;
    private Set<String> selectedSeatIds = new HashSet<>();
    private Map<String, Seat> seatMap = new HashMap<>(); // Track seat objects by seat number
    
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        routeCatalog = RouteCatalog.getInstance();
        bookingCatalog = new BookingCatalog(); // Create instance of BookingCatalog
        setupEventHandlers();
        loadRoutes();
    }

    // --- Setters ---
    public void setUserData(String username, Customer customer) {
        this.currentCustomer = customer;
    }
    
    public void setUserData(Customer customer) {
        this.currentCustomer = customer;
    }

    private void setupEventHandlers() {
        if (searchButton != null) searchButton.setOnAction(e -> filterRoutes());
        if (clearButton != null) clearButton.setOnAction(e -> {
            searchSource.clear();
            searchDestination.clear();
            loadRoutes();
        });
        if (backButton != null) backButton.setOnAction(e -> handleBack());
        if (btnConfirm != null) btnConfirm.setOnAction(e -> handleBooking());
    }

    // =========================================================
    // LAYER 1: ROUTES
    // =========================================================

    private void loadRoutes() {
        showLayer("ROUTES");
        renderRoutes(routeCatalog.getAllRoutes());
    }

    private void filterRoutes() {
        String src = searchSource.getText().toLowerCase().trim();
        String dst = searchDestination.getText().toLowerCase().trim();
        
        List<Route> filtered = routeCatalog.getAllRoutes().stream()
            .filter(r -> (src.isEmpty() || r.getSource().toLowerCase().contains(src)) &&
                         (dst.isEmpty() || r.getDestination().toLowerCase().contains(dst)))
            .collect(Collectors.toList());
        
        renderRoutes(filtered);
    }

    private void renderRoutes(List<Route> routes) {
        routesContainer.getChildren().clear();
        
        if (routes.isEmpty()) {
            routesContainer.getChildren().add(new Label("No routes found."));
            return;
        }

        for (Route r : routes) {
            VBox card = new VBox(5);
            card.getStyleClass().add("item-card");
            
            HBox top = new HBox();
            Label title = new Label(r.getSource() + " ➝ " + r.getDestination());
            title.getStyleClass().add("card-title");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label price = new Label("PKR " + r.getBasePrice());
            price.getStyleClass().add("card-price");
            top.getChildren().addAll(title, spacer, price);
            
            Label details = new Label(r.getSchedules().size() + " Schedules Available");
            details.getStyleClass().add("card-detail");
            
            card.getChildren().addAll(top, details);
            card.setOnMouseClicked(e -> showSchedules(r));
            
            routesContainer.getChildren().add(card);
        }
    }

    // =========================================================
    // LAYER 2: SCHEDULES
    // =========================================================

    private void showSchedules(Route route) {
        this.selectedRoute = route;
        showLayer("SCHEDULES");
        subTitle.setText("Route: " + route.getSource() + " to " + route.getDestination());
        
        schedulesContainer.getChildren().clear();
        
        List<Schedule> validSchedules = route.getAllSchedules().stream()
            .filter(s -> !s.getDate().isBefore(LocalDate.now()))
            .sorted(Comparator.comparing(Schedule::getDate))
            .collect(Collectors.toList());

        if (validSchedules.isEmpty()) {
            schedulesContainer.getChildren().add(new Label("No upcoming schedules."));
            return;
        }

        for (Schedule s : validSchedules) {
            VBox card = new VBox(5);
            card.getStyleClass().add("item-card");
            
            HBox top = new HBox();
            Label date = new Label(s.getDate().format(DATE_FMT));
            date.getStyleClass().add("card-title");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label cls = new Label(s.getScheduleClass());
            cls.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-padding: 3 8; -fx-background-radius: 5; -fx-font-weight: bold;");
            top.getChildren().addAll(date, spacer, cls);
            
            Label time = new Label(s.getDepartureTime().format(TIME_FMT) + " - " + s.getArrivalTime().format(TIME_FMT));
            time.getStyleClass().add("card-detail");
            
            card.getChildren().addAll(top, time);
            card.setOnMouseClicked(e -> showSeats(s));
            
            schedulesContainer.getChildren().add(card);
        }
    }

    // =========================================================
    // LAYER 3: SEATS
    // =========================================================

    private void showSeats(Schedule schedule) {
        this.selectedSchedule = schedule;
        this.selectedSeatIds.clear();
        this.seatMap.clear();
        updateSummary();
        showLayer("SEATS");
        
        subTitle.setText("Select Seats (" + schedule.getScheduleClass() + ")");
        seatsContainer.getChildren().clear();
        
        if (schedule.getSeats().isEmpty()) {
            loadSeatsFromDB(schedule);
        }
        
        // Store seats in map for quick access
        for (Seat seat : schedule.getSeats()) {
            seatMap.put(seat.getSeatNo(), seat);
        }
        
        renderBusLayout(schedule.getSeats());
    }

    private void renderBusLayout(List<Seat> seats) {
        if (seats.isEmpty()) {
            seatsContainer.getChildren().add(new Label("Configuration unavailable."));
            return;
        }

        VBox busLayout = new VBox(15);
        busLayout.getStyleClass().add("bus-layout");
        busLayout.setAlignment(Pos.TOP_CENTER);
        
        Label front = new Label("🚌 FRONT");
        front.setStyle("-fx-text-fill: #AAA; -fx-font-weight: bold; -fx-padding: 0 0 10 0; -fx-border-style: dashed; -fx-border-color: #CCC; -fx-border-width: 0 0 1 0;");
        busLayout.getChildren().add(front);

        // Group Seats by Row Char
        Map<Character, List<Seat>> rows = seats.stream()
            .collect(Collectors.groupingBy(s -> s.getSeatNo().charAt(0)));
        
        List<Character> sortedRows = new ArrayList<>(rows.keySet());
        Collections.sort(sortedRows);

        for (Character rChar : sortedRows) {
            HBox rowBox = new HBox(15);
            rowBox.setAlignment(Pos.CENTER);
            
            List<Seat> rowSeats = rows.get(rChar);
            rowSeats.sort(Comparator.comparing(Seat::getSeatNo));
            
            for (Seat seat : rowSeats) {
                Button btn = new Button(seat.getSeatNo());
                btn.getStyleClass().add("seat-btn");
                
                if (!seat.isAvailability()) {
                    btn.getStyleClass().add("seat-booked");
                    btn.setDisable(true);
                } else {
                    btn.getStyleClass().add("seat-available");
                    btn.setOnAction(e -> toggleSeat(btn, seat));
                }
                rowBox.getChildren().add(btn);
            }
            busLayout.getChildren().add(rowBox);
        }
        
        seatsContainer.getChildren().add(busLayout);
    }

    private void toggleSeat(Button btn, Seat seat) {
        if (selectedSeatIds.contains(seat.getSeatNo())) {
            selectedSeatIds.remove(seat.getSeatNo());
            btn.getStyleClass().remove("seat-selected");
            btn.getStyleClass().add("seat-available");
        } else {
            selectedSeatIds.add(seat.getSeatNo());
            btn.getStyleClass().remove("seat-available");
            btn.getStyleClass().add("seat-selected");
        }
        updateSummary();
    }

    private void updateSummary() {
        int count = selectedSeatIds.size();
        if (count == 0) {
            lblSelectedSeats.setText("None");
            lblTotalPrice.setText("PKR 0.00");
            btnConfirm.setDisable(true);
        } else {
            lblSelectedSeats.setText(String.join(", ", selectedSeatIds));
            
            // Calculate total based on selected seats
            double total = selectedSeatIds.stream()
                .mapToDouble(seatNo -> seatMap.get(seatNo).getPrice())
                .sum();
            
            lblTotalPrice.setText("PKR " + String.format("%.2f", total));
            btnConfirm.setDisable(false);
        }
    }

    // =========================================================
    // STATE MANAGEMENT
    // =========================================================

    private void showLayer(String layer) {
        routeLayer.setVisible(false);
        scheduleLayer.setVisible(false);
        seatLayer.setVisible(false);
        summaryLayer.setVisible(false);
        searchLayer.setVisible(false);
        searchLayer.setManaged(false);
        
        switch (layer) {
            case "ROUTES":
                routeLayer.setVisible(true);
                searchLayer.setVisible(true);
                searchLayer.setManaged(true);
                backButton.setVisible(false);
                backButton.setManaged(false);
                subTitle.setText("Select a Route");
                break;
            case "SCHEDULES":
                scheduleLayer.setVisible(true);
                backButton.setVisible(true);
                backButton.setManaged(true);
                break;
            case "SEATS":
                seatLayer.setVisible(true);
                summaryLayer.setVisible(true);
                backButton.setVisible(true);
                backButton.setManaged(true);
                break;
        }
    }

    private void handleBack() {
        if (seatLayer.isVisible()) {
            showSchedules(selectedRoute);
        } else if (scheduleLayer.isVisible()) {
            loadRoutes();
        }
    }

    // =========================================================
    // DB UTILS
    // =========================================================

    private void loadSeatsFromDB(Schedule schedule) {
        String query = "SELECT * FROM Seat WHERE ScheduleID = ?";
        List<Seat> dbSeats = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(DatabaseConfig.getDbUrl(), DatabaseConfig.getDbUser(), DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, schedule.getScheduleID());
            ResultSet rs = stmt.executeQuery();
            
            while(rs.next()) {
                Seat s = new Seat(rs.getString("SeatNumber"), rs.getString("SeatType"), rs.getDouble("Price"));
                s.setAvailability(rs.getBoolean("Availability"));
                dbSeats.add(s);
            }
        } catch (Exception e) {
            System.err.println("Seat load error: " + e.getMessage());
            e.printStackTrace();
        }
        schedule.setSeats((ArrayList<Seat>) dbSeats);
    }

    private void handleBooking() {
        if(saveBookingUsingCatalog()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Booking Successful!");
            a.showAndWait();
            loadRoutes();
        } else {
            new Alert(Alert.AlertType.ERROR, "Booking Failed.").showAndWait();
        }
    }

   private boolean saveBookingUsingCatalog() {
        try {
            // 1. Generate unique IDs
            String reservationID = "RES" + System.currentTimeMillis();
            String bookingID = "B" + System.currentTimeMillis();
            
            // 2. Calculate total amount based on selected seats
            double totalAmount = selectedSeatIds.stream()
                .mapToDouble(seatNo -> seatMap.get(seatNo).getPrice())
                .sum();
            
            // 3. Create Reservation
            Reservation reservation = new Reservation(
                reservationID,
                selectedSchedule,
                selectedRoute,
                selectedSchedule.getScheduleClass()
            );
            
            // Add selected seats to reservation
            List<String> selectedSeatList = new ArrayList<>(selectedSeatIds);
            reservation.selectSeats(selectedSeatList);
            
            // 4. Create Booking WITHOUT payment initially
            Booking booking = new Booking(
                bookingID,
                currentCustomer.getUserID(),
                reservation,
                new java.util.Date()
            );
            booking.setTotalAmount(totalAmount);
            booking.setStatus("Confirmed");
            
            // 5. First, try to update seat availability
            if (!updateSeatAvailability()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Seat Unavailable");
                alert.setHeaderText("Selected seats are no longer available");
                alert.setContentText("Please select different seats.");
                alert.showAndWait();
                return false;
            }
            
            // 6. Save to database using BookingCatalog
            boolean bookingSaved = bookingCatalog.addBooking(booking);
            
            if (bookingSaved) {
                // Show success message
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Booking Confirmed");
                alert.setHeaderText("Booking Created Successfully!");
                alert.setContentText("Booking ID: " + bookingID + 
                                "\nTotal Amount: PKR " + String.format("%.2f", totalAmount) +
                                "\n\nYour booking is confirmed but payment is pending." +
                                "\nPlease complete payment from 'My Bookings' page.");
                alert.showAndWait();
                
                System.out.println("Booking saved successfully (payment pending): " + bookingID);
                return true;
            } else {
                // If booking failed, release the seats
                rollbackSeatAvailability();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Booking Failed");
                alert.setHeaderText("Failed to create booking");
                alert.setContentText("Please try again.");
                alert.showAndWait();
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Booking error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean updateSeatAvailability() {
    String updateSeatQuery = "UPDATE Seat SET Availability = 0 WHERE SeatNumber = ? AND ScheduleID = ? AND Availability = 1";
    
    Connection conn = null;
    try {
        conn = DatabaseConnection.getConnection();
        int updatedCount = 0;
        
        // Use batch update for efficiency
        try (PreparedStatement ps = conn.prepareStatement(updateSeatQuery)) {
            for (String seatNo : selectedSeatIds) {
                ps.setString(1, seatNo);
                ps.setString(2, selectedSchedule.getScheduleID());
                ps.addBatch();
            }
            
            int[] results = ps.executeBatch();
            updatedCount = Arrays.stream(results).sum();
        }
        
        // Check if all seats were updated (means they were available)
        return updatedCount == selectedSeatIds.size();
        
    } catch (Exception e) {
        System.err.println("Error updating seat availability: " + e.getMessage());
        e.printStackTrace();
        return false;
    } finally {
        // DO NOT close the connection here if it's a shared connection
        // Let DatabaseConnection manage it
    }
}

private void rollbackSeatAvailability() {
    String updateSeatQuery = "UPDATE Seat SET Availability = 1 WHERE SeatNumber = ? AND ScheduleID = ?";
    
    Connection conn = null;
    try {
        conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(updateSeatQuery)) {
            for (String seatNo : selectedSeatIds) {
                ps.setString(1, seatNo);
                ps.setString(2, selectedSchedule.getScheduleID());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        
    } catch (Exception e) {
        System.err.println("Error rolling back seat availability: " + e.getMessage());
        e.printStackTrace();
    } finally {
        // DO NOT close the connection here
    }
}

    // Optional: Get customer's booking history
    public List<Booking> getCustomerBookings() {
        if (currentCustomer != null) {
            return bookingCatalog.getBookingsByCustomer(currentCustomer.getUserID());
        }
        return new ArrayList<>();
    }

    // Optional: Check if seats are still available
    private boolean validateSeatAvailability() {
        for (String seatNo : selectedSeatIds) {
            Seat seat = seatMap.get(seatNo);
            if (seat == null || !seat.isAvailability()) {
                return false;
            }
        }
        return true;
    }

    // Optional: Clear selection and refresh
    private void clearSelection() {
        selectedSeatIds.clear();
        updateSummary();
        // Refresh seat view
        if (selectedSchedule != null) {
            showSeats(selectedSchedule);
        }
    }
}