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
import services.NotificationService;
import database.DatabaseConnection;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class BookTicketsController implements Initializable {
    @FXML private Text pageTitle;
    @FXML private Label subTitle;
    @FXML private Button backButton;

    @FXML private HBox searchLayer;
    @FXML private TextField searchSource;
    @FXML private TextField searchDestination;
    @FXML private Button searchButton;
    @FXML private Button clearButton;

    @FXML private ScrollPane routeLayer;
    @FXML private ScrollPane scheduleLayer;
    @FXML private ScrollPane seatLayer;

    @FXML private VBox routesContainer;
    @FXML private VBox schedulesContainer;
    @FXML private VBox seatsContainer;

    @FXML private VBox summaryLayer;
    @FXML private Text lblSelectedSeats;
    @FXML private Text lblTotalPrice;
    @FXML private Button btnConfirm;

    private Customer currentCustomer;
    private RouteCatalog routeCatalog;
    private BookingCatalog bookingCatalog;
    private Route selectedRoute;
    private Schedule selectedSchedule;
    private Set<Seat> selectedSeats = new HashSet<>(); 
    private Map<String, Seat> seatMap = new HashMap<>();
    
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        routeCatalog = RouteCatalog.getInstance();
        bookingCatalog = new BookingCatalog();
        setupEventHandlers();
        loadRoutes();
    }

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

    private void showSeats(Schedule schedule) {
        this.selectedSchedule = schedule;
        this.selectedSeats.clear();
        this.seatMap.clear();
        updateSummary();
        showLayer("SEATS");
        
        subTitle.setText("Select Seats (" + schedule.getScheduleClass() + ")");
        seatsContainer.getChildren().clear();
        loadSeatsFromDB(schedule);
        seatMap.clear();
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
        if (selectedSeats.contains(seat)) {
            selectedSeats.remove(seat);
            btn.getStyleClass().remove("seat-selected");
            btn.getStyleClass().add("seat-available");
        } else {
            selectedSeats.add(seat);
            btn.getStyleClass().remove("seat-available");
            btn.getStyleClass().add("seat-selected");
        }
        updateSummary();
    }

    private void updateSummary() {
        int count = selectedSeats.size();
        if (count == 0) {
            lblSelectedSeats.setText("None");
            lblTotalPrice.setText("PKR 0.00");
            btnConfirm.setDisable(true);
        } else {
            String seatNumbers = selectedSeats.stream()
                .map(Seat::getSeatNo)
                .collect(Collectors.joining(", "));
            lblSelectedSeats.setText(seatNumbers);
            double total = selectedSeats.stream()
                .mapToDouble(Seat::getPrice)
                .sum();
            
            lblTotalPrice.setText("PKR " + String.format("%.2f", total));
            btnConfirm.setDisable(false);
        }
    }

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

    private void loadSeatsFromDB(Schedule schedule) {
        String query = "SELECT * FROM Seat WHERE ScheduleID = ? ORDER BY SeatNumber";
        List<Seat> dbSeats = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(DatabaseConfig.getDbUrl(), 
                                                        DatabaseConfig.getDbUser(), 
                                                        DatabaseConfig.getDbPassword());
            PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, schedule.getScheduleID());
            ResultSet rs = stmt.executeQuery();
            schedule.getSeats().clear();
            
            while(rs.next()) {
                Seat s = new Seat(rs.getString("SeatNumber"), 
                                rs.getString("SeatType"), 
                                rs.getDouble("Price"));
                s.setAvailability(rs.getBoolean("Availability"));
                dbSeats.add(s);
            }
            schedule.setSeats((ArrayList<Seat>) dbSeats);
            
        } catch (Exception e) {
            System.err.println("Seat load error: " + e.getMessage());
            e.printStackTrace();
        }
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
            String reservationID = "RES" + System.currentTimeMillis();
            String bookingID = "B" + System.currentTimeMillis();
            double totalAmount = selectedSeats.stream().mapToDouble(Seat::getPrice).sum();
            Reservation reservation = new Reservation(
                reservationID,
                selectedSchedule,
                selectedRoute,
                selectedSchedule.getScheduleClass()
            );
            
            List<Seat> selectedSeatList = new ArrayList<>(selectedSeats);
            reservation.selectSeats(selectedSeatList);
            Booking booking = new Booking(
                bookingID,
                currentCustomer.getUserID(),
                reservation,
                new java.util.Date()
            );
            booking.setTotalAmount(totalAmount);
            booking.setStatus("Confirmed");    
            boolean bookingSaved = bookingCatalog.addBooking(booking);
            
            if (bookingSaved) {
                boolean seatsUpdated = updateSeatAvailability(reservationID, selectedSeatList);
                
                if (seatsUpdated) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Booking Confirmed");
                alert.setHeaderText(null);
                alert.setGraphic(null);
                DialogPane dialogPane = alert.getDialogPane();
                dialogPane.getStylesheets().add(getClass().getResource("/ui/MyBookings.css").toExternalForm());
                dialogPane.getStyleClass().add("ticket-dialog");
                VBox card = new VBox(15);
                card.getStyleClass().add("ticket-card");
                card.setMinWidth(400);
                card.setAlignment(javafx.geometry.Pos.TOP_CENTER);
                Label titleLbl = new Label("BOOKING SUCCESSFUL!");
                titleLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #3F5F3C; -fx-font-size: 18px;");

                Separator sep = new Separator();
                sep.setStyle("-fx-border-style: dashed; -fx-border-width: 1px 0 0 0; -fx-border-color: #ccc; -fx-background-color: transparent;");

                Label bookingIdLbl = new Label("Booking ID: #" + bookingID);
                bookingIdLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                GridPane grid = new GridPane();
                grid.setHgap(30); grid.setVgap(15);
                grid.setAlignment(javafx.geometry.Pos.CENTER);

                java.util.function.BiConsumer<String, String> addRow = (label, value) -> {
                    int row = grid.getRowCount();
                    VBox box = new VBox(2);
                    box.setAlignment(javafx.geometry.Pos.CENTER);
                    Label l = new Label(label); l.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
                    Label v = new Label(value); v.setStyle("-fx-font-weight: bold; -fx-text-fill: #333; -fx-font-size: 14px;");
                    box.getChildren().addAll(l, v);
                    grid.add(box, 0, row);
                };

                addRow.accept("Total Amount:", "PKR " + String.format("%.2f", totalAmount));
                addRow.accept("Selected Seats:", getSeatNumbersAsString(selectedSeatList));

                Label instructionLbl = new Label("Your booking is confirmed but payment is pending.\nPlease complete payment from the 'My Bookings' page.");
                instructionLbl.setWrapText(true);
                instructionLbl.setStyle("-fx-text-fill: #555; -fx-font-size: 13px; -fx-text-alignment: center; -fx-padding: 15 0 0 0;");

                card.getChildren().addAll(titleLbl, sep, bookingIdLbl, grid, instructionLbl);
                dialogPane.setContent(card);
                Button okBtn = (Button) dialogPane.lookupButton(ButtonType.OK);
                okBtn.setStyle("-fx-background-color: #3F5F3C; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5px;");

                alert.showAndWait();
                try {
                    NotificationService notificationService = NotificationService.getInstance();
                    notificationService.sendBookingSuccessNotification(
                        currentCustomer,
                        bookingID,
                        selectedRoute.getSource() + " to " + selectedRoute.getDestination()
                    );
                    System.out.println("✅ Notification sent for booking: " + bookingID);
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to send notification: " + e.getMessage());
                    e.printStackTrace();
                }
                System.out.println("Booking saved successfully (payment pending): " + bookingID);
                return true;
            } else {
                    bookingCatalog.cancelBooking(bookingID);
                    rollbackSeatAvailability(selectedSeatList);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Booking Failed");
                    alert.setHeaderText("Failed to reserve selected seats");
                    alert.setContentText("One or more seats are no longer available. Please try again.");
                    alert.showAndWait();
                    return false;
                }
            } else {
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

    private String getSeatNumbersAsString(List<Seat> seats) {
        return seats.stream()
            .map(Seat::getSeatNo)
            .collect(Collectors.joining(", "));
    }

    private boolean updateSeatAvailability(String reservationID, List<Seat> seats) {
        String updateSeatQuery = "UPDATE Seat SET Availability = 0, ReservationID = ? WHERE SeatNumber = ? AND ScheduleID = ? AND Availability = 1";
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            int updatedCount = 0;
            try (PreparedStatement ps = conn.prepareStatement(updateSeatQuery)) {
                for (Seat seat : seats) {
                    ps.setString(1, reservationID);
                    ps.setString(2, seat.getSeatNo());
                    ps.setString(3, selectedSchedule.getScheduleID());
                    ps.addBatch();
                }
                
                int[] results = ps.executeBatch();
                updatedCount = Arrays.stream(results).sum();
            }
            return updatedCount == seats.size();
            
        } catch (Exception e) {
            System.err.println("Error updating seat availability: " + e.getMessage());
            e.printStackTrace();
            return false;
        } 
    }

    private void rollbackSeatAvailability(List<Seat> seats) {
        String updateSeatQuery = "UPDATE Seat SET Availability = 1, ReservationID = NULL WHERE SeatNumber = ? AND ScheduleID = ?";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(updateSeatQuery)) {
                for (Seat seat : seats) {
                    ps.setString(1, seat.getSeatNo());
                    ps.setString(2, selectedSchedule.getScheduleID());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            
        } catch (Exception e) {
            System.err.println("Error rolling back seat availability: " + e.getMessage());
            e.printStackTrace();
        } 
    }

    public List<Booking> getCustomerBookings() {
        if (currentCustomer != null) {
            return bookingCatalog.getBookingsByCustomer(currentCustomer.getUserID());
        }
        return new ArrayList<>();
    }

    // private boolean validateSeatAvailability() {
    //     for (Seat seat : selectedSeats) {
    //         if (!seat.isAvailability()) {
    //             return false;
    //         }
    //     }
    //     return true;
    // }

    // private void clearSelection() {
    //     selectedSeats.clear();
    //     updateSummary();
    //     // Refresh seat view
    //     if (selectedSchedule != null) {
    //         showSeats(selectedSchedule);
    //     }
    // }
}