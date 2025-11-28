package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import catalogs.RouteCatalog;
import config.DatabaseConfig;
import models.Customer;
import models.Route;
import models.Schedule;
import models.Seat;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class BookTicketsController implements Initializable {

    // --- FXML Injection ---
    @FXML private Text pageTitle;
    @FXML private Label subTitle;
    @FXML private Button backButton; // Single Back Button

    // Search
    @FXML private HBox searchLayer;
    @FXML private TextField searchSource;
    @FXML private TextField searchDestination;
    @FXML private Button searchButton;
    @FXML private Button clearButton;

    // Layers (StackPane children)
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
    private Route selectedRoute;
    private Schedule selectedSchedule;
    private Set<String> selectedSeatIds = new HashSet<>();
    
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // --- CRITICAL: DEBUG CHECKS ---
        if (backButton == null) System.err.println("CRITICAL: backButton not injected!");
        if (routeLayer == null) System.err.println("CRITICAL: routeLayer not injected!");
        
        routeCatalog = RouteCatalog.getInstance();
        setupEventHandlers();
        
        // Start by loading routes
        loadRoutes();
    }

    // --- Overloaded Setters for Compatibility ---
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
            card.setOnMouseClicked(e -> showSchedules(r)); // Interaction
            
            routesContainer.getChildren().add(card);
        }
    }

    // =========================================================
    // LAYER 2: SCHEDULES
    // =========================================================

    private void showSchedules(Route route) {
        this.selectedRoute = route;
        System.out.println(route.getRouteID());
        System.out.println(route.getSource());
        System.out.println(route.getDestination()); 
        System.out.println(route.getSchedules());
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
            card.setOnMouseClicked(e -> showSeats(s)); // Interaction
            
            schedulesContainer.getChildren().add(card);
        }
    }

    // =========================================================
    // LAYER 3: SEATS
    // =========================================================

    private void showSeats(Schedule schedule) {
        this.selectedSchedule = schedule;
        this.selectedSeatIds.clear();
        updateSummary();
        showLayer("SEATS");
        
        subTitle.setText("Select Seats (" + schedule.getScheduleClass() + ")");
        seatsContainer.getChildren().clear();
        
        if (schedule.getSeats().isEmpty()) {
            loadSeatsFromDB(schedule);
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

        // Group Seats by Row Char (A, B, C...)
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
            double total = count * selectedRoute.getBasePrice();
            lblTotalPrice.setText("PKR " + String.format("%.0f", total));
            btnConfirm.setDisable(false);
        }
    }

    // =========================================================
    // STATE MANAGEMENT
    // =========================================================

    private void showLayer(String layer) {
        // Hide all layers first
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
            System.out.println("Loading seats for ScheduleID: " + schedule.getScheduleID());
            stmt.setString(1, schedule.getScheduleID());
            ResultSet rs = stmt.executeQuery();
            System.out.println("Seats loaded:" + rs);
            while(rs.next()) {
                Seat s = new Seat(rs.getString("SeatNumber"), rs.getString("SeatType"), rs.getDouble("PriceAdjustment"));
                s.setAvailability(rs.getBoolean("Availability"));
                dbSeats.add(s);
            }
        } catch (Exception e) {
            System.err.println("Seat load error: " + e.getMessage());
        }
        schedule.setSeats((ArrayList<Seat>) dbSeats);
    }

    private void handleBooking() {
        if(saveBookingTransaction()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Booking Successful!");
            a.showAndWait();
            loadRoutes();
        } else {
            new Alert(Alert.AlertType.ERROR, "Booking Failed.").showAndWait();
        }
    }

    private boolean saveBookingTransaction() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DatabaseConfig.getDbUrl(), DatabaseConfig.getDbUser(), DatabaseConfig.getDbPassword());
            conn.setAutoCommit(false);

            String bID = "B" + System.currentTimeMillis();
            String insertB = "INSERT INTO Booking (BookingID, CustomerID, BookingDateTime, TotalAmount, Status) VALUES (?, ?, GETDATE(), ?, 'Confirmed')";
            try(PreparedStatement ps = conn.prepareStatement(insertB)){
                ps.setString(1, bID);
                ps.setString(2, currentCustomer.getUserID());
                ps.setDouble(3, Double.parseDouble(lblTotalPrice.getText().replace("PKR ", "")));
                ps.executeUpdate();
            }

            String updateS = "UPDATE Seat SET IsAvailable = 0 WHERE SeatNo = ? AND ScheduleID = ?";
            try(PreparedStatement ps = conn.prepareStatement(updateS)){
                for(String sid : selectedSeatIds) {
                    ps.setString(1, sid);
                    ps.setString(2, selectedSchedule.getScheduleID());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
            return true;
        } catch(Exception e) {
            if(conn!=null) try{conn.rollback();}catch(Exception ex){}
            e.printStackTrace();
            return false;
        }
    }
}