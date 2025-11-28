package controllers;

import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.geometry.Pos;

import models.Route;
import models.Schedule;
import models.Seat;
import models.Customer;
import catalogs.RouteCatalog;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class BookTicketsController implements Initializable {

    @FXML private Text pageTitle;
    @FXML private StackPane contentArea; // Changed from VBox to StackPane
    @FXML private HBox searchBarContainer; // Reference to hide/show
    
    @FXML private TextField searchSource;
    @FXML private TextField searchDestination;
    @FXML private Button searchButton;
    @FXML private Button clearSearchButton;
    
    @FXML private VBox routesContainer;
    @FXML private VBox schedulesContainer;
    @FXML private VBox seatsContainer;
    
    @FXML private Text selectedRouteText;
    @FXML private Text selectedScheduleText;
    @FXML private Text selectedSeatsText;
    @FXML private Text totalPriceText;
    
    @FXML private Button bookNowButton;
    @FXML private Button backToRoutesButton;
    @FXML private Button backToSchedulesButton;

    private String currentUsername;
    private Customer currentCustomer;
    private RouteCatalog routeCatalog;
    
    private Route selectedRoute;
    private Schedule selectedSchedule;
    private List<Seat> selectedSeats;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        routeCatalog = RouteCatalog.getInstance();
        selectedSeats = new ArrayList<>();
        setupEventHandlers();
        loadAllRoutes();
    }

    public void setUserData(String username, Customer customer) {
        this.currentUsername = username;
        this.currentCustomer = customer;
        updateUI();
    }

    private void setupEventHandlers() {
        searchButton.setOnAction(e -> searchRoutes());
        clearSearchButton.setOnAction(e -> clearSearch());
        backToRoutesButton.setOnAction(e -> showRoutesView());
        backToSchedulesButton.setOnAction(e -> showSchedulesView());
        bookNowButton.setOnAction(e -> bookTickets());
    }

    private void updateUI() {
        if (currentUsername != null && !currentUsername.isEmpty()) {
            String displayName = currentUsername.substring(0, 1).toUpperCase() + 
                               currentUsername.substring(1).toLowerCase();
            pageTitle.setText("Book Tickets - Welcome, " + displayName + "!");
        }
    }

    private void loadAllRoutes() {
        showRoutesView();
        List<Route> routes = routeCatalog.getAllRoutes();
        displayRoutes(routes);
    }

    private void searchRoutes() {
        String source = searchSource.getText().trim();
        String destination = searchDestination.getText().trim();
        
        List<Route> filteredRoutes;
        
        if (source.isEmpty() && destination.isEmpty()) {
            filteredRoutes = routeCatalog.getAllRoutes();
        } else {
            filteredRoutes = routeCatalog.getAllRoutes().stream()
                .filter(route -> 
                    (source.isEmpty() || route.getSource().toLowerCase().contains(source.toLowerCase())) &&
                    (destination.isEmpty() || route.getDestination().toLowerCase().contains(destination.toLowerCase()))
                )
                .collect(Collectors.toList());
        }
        
        displayRoutes(filteredRoutes);
    }

    private void clearSearch() {
        searchSource.clear();
        searchDestination.clear();
        loadAllRoutes();
    }

    private void displayRoutes(List<Route> routes) {
        routesContainer.getChildren().clear();
        
        if (routes.isEmpty()) {
            Label noRoutesLabel = new Label("No routes found matching your search criteria.");
            noRoutesLabel.getStyleClass().add("no-data-text");
            routesContainer.getChildren().add(noRoutesLabel);
            return;
        }

        for (Route route : routes) {
            VBox routeCard = createRouteCard(route);
            routesContainer.getChildren().add(routeCard);
        }
    }

    private VBox createRouteCard(Route route) {
        VBox card = new VBox(10);
        card.getStyleClass().add("route-card");
        card.setOnMouseClicked(e -> selectRoute(route));

        // Route header
        HBox header = new HBox();
        header.getStyleClass().add("route-header");
        
        Label routeInfo = new Label(route.getSource() + " → " + route.getDestination());
        routeInfo.getStyleClass().add("route-title");
        
        Label priceLabel = new Label("PKR " + String.format("%.2f", route.getBasePrice()));
        priceLabel.getStyleClass().add("route-price");
        
        HBox.setHgrow(routeInfo, Priority.ALWAYS);
        header.getChildren().addAll(routeInfo, priceLabel);

        // Route details
        Label scheduleCount = new Label(route.getSchedules().size() + " available schedules");
        scheduleCount.getStyleClass().add("route-detail");

        Label routeId = new Label("Route ID: " + route.getRouteID());
        routeId.getStyleClass().add("route-detail");

        card.getChildren().addAll(header, scheduleCount, routeId);
        return card;
    }

    private void selectRoute(Route route) {
        this.selectedRoute = route;
        this.selectedSchedule = null;
        this.selectedSeats.clear();
        updateSelectionDisplay();
        showSchedulesView();
        displaySchedules(route.getSchedules());
    }

    private void displaySchedules(List<Schedule> schedules) {
        schedulesContainer.getChildren().clear();
        
        if (schedules.isEmpty()) {
            Label noSchedulesLabel = new Label("No schedules available for this route.");
            noSchedulesLabel.getStyleClass().add("no-data-text");
            schedulesContainer.getChildren().add(noSchedulesLabel);
            return;
        }

        // Filter schedules to only show future dates
        List<Schedule> futureSchedules = schedules.stream()
            .filter(schedule -> !schedule.getDate().isBefore(java.time.LocalDate.now()))
            .sorted((s1, s2) -> s1.getDate().compareTo(s2.getDate()))
            .collect(Collectors.toList());

        if (futureSchedules.isEmpty()) {
            Label noSchedulesLabel = new Label("No upcoming schedules available.");
            noSchedulesLabel.getStyleClass().add("no-data-text");
            schedulesContainer.getChildren().add(noSchedulesLabel);
            return;
        }

        for (Schedule schedule : futureSchedules) {
            VBox scheduleCard = createScheduleCard(schedule);
            schedulesContainer.getChildren().add(scheduleCard);
        }
    }

    private VBox createScheduleCard(Schedule schedule) {
        VBox card = new VBox(8);
        card.getStyleClass().add("schedule-card");
        card.setOnMouseClicked(e -> selectSchedule(schedule));

        // Schedule header
        HBox header = new HBox();
        header.getStyleClass().add("schedule-header");
        
        Label dateLabel = new Label(schedule.getDate().format(DATE_FORMATTER));
        dateLabel.getStyleClass().add("schedule-date");
        
        Label classLabel = new Label(schedule.getScheduleClass());
        classLabel.getStyleClass().add("schedule-class");
        
        HBox.setHgrow(dateLabel, Priority.ALWAYS);
        header.getChildren().addAll(dateLabel, classLabel);

        // Schedule timing
        Label timeLabel = new Label(
            schedule.getDepartureTime().format(TIME_FORMATTER) + " - " + 
            schedule.getArrivalTime().format(TIME_FORMATTER)
        );
        timeLabel.getStyleClass().add("schedule-time");

        // Available seats
        long availableSeats = schedule.getSeats().stream()
            .filter(Seat::isAvailability)
            .count();
        Label seatsLabel = new Label(availableSeats + " seats available");
        seatsLabel.getStyleClass().add("schedule-seats");

        card.getChildren().addAll(header, timeLabel, seatsLabel);
        return card;
    }

    private void selectSchedule(Schedule schedule) {
        this.selectedSchedule = schedule;
        this.selectedSeats.clear();
        updateSelectionDisplay();
        showSeatsView();
        displaySeats(schedule.getSeats());
    }

    private void displaySeats(List<Seat> seats) {
        seatsContainer.getChildren().clear();
        
        if (seats.isEmpty()) {
            Label noSeatsLabel = new Label("No seat configuration available for this schedule.");
            noSeatsLabel.getStyleClass().add("no-data-text");
            seatsContainer.getChildren().add(noSeatsLabel);
            return;
        }

        // Group seats by row
        Map<Character, List<Seat>> seatsByRow = seats.stream()
            .collect(Collectors.groupingBy(seat -> seat.getSeatNo().charAt(0)));

        // Create seat layout
        VBox seatLayout = new VBox(15);
        seatLayout.getStyleClass().add("seat-layout");
        seatLayout.setAlignment(Pos.TOP_CENTER); // Ensure bus visualization centers

        // Add bus front indicator
        Label busFront = new Label("🚌 BUS FRONT");
        busFront.getStyleClass().add("bus-front");
        seatLayout.getChildren().add(busFront);

        for (char row : seatsByRow.keySet().stream().sorted().collect(Collectors.toList())) {
            HBox seatRow = createSeatRow(seatsByRow.get(row), row);
            seatLayout.getChildren().add(seatRow);
        }

        // Wrap in a ScrollPane for scrolling within the card
        ScrollPane scrollPane = new ScrollPane(seatLayout);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("transparent-scroll"); // Important from CSS
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        seatsContainer.getChildren().add(scrollPane);
    }

    private HBox createSeatRow(List<Seat> rowSeats, char rowChar) {
        HBox seatRow = new HBox(10);
        seatRow.setAlignment(Pos.CENTER);
        seatRow.getStyleClass().add("seat-row");

        List<Seat> sortedSeats = rowSeats.stream()
            .sorted((s1, s2) -> s1.getSeatNo().compareTo(s2.getSeatNo()))
            .collect(Collectors.toList());

        for (Seat seat : sortedSeats) {
            Button seatButton = createSeatButton(seat);
            seatRow.getChildren().add(seatButton);
        }

        return seatRow;
    }

    private Button createSeatButton(Seat seat) {
        Button seatButton = new Button(seat.getSeatNo());
        seatButton.getStyleClass().add("seat-button");
        
        if (seat.isAvailability()) {
            seatButton.getStyleClass().add("seat-available");
            seatButton.setOnAction(e -> toggleSeatSelection(seat, seatButton));
        } else {
            seatButton.getStyleClass().add("seat-booked");
            seatButton.setDisable(true);
        }

        return seatButton;
    }

    private void toggleSeatSelection(Seat seat, Button seatButton) {
        if (selectedSeats.contains(seat)) {
            selectedSeats.remove(seat);
            seatButton.getStyleClass().remove("seat-selected");
            seatButton.getStyleClass().add("seat-available");
        } else {
            selectedSeats.add(seat);
            seatButton.getStyleClass().remove("seat-available");
            seatButton.getStyleClass().add("seat-selected");
        }
        updateSelectionDisplay();
    }

    private void updateSelectionDisplay() {
        if (selectedRoute != null) {
            selectedRouteText.setText("Route: " + selectedRoute.getSource() + " → " + selectedRoute.getDestination());
        } else {
            selectedRouteText.setText("Route: Not selected");
        }

        if (selectedSchedule != null) {
            selectedScheduleText.setText("Schedule: " + 
                selectedSchedule.getDate().format(DATE_FORMATTER) + " • " +
                selectedSchedule.getDepartureTime().format(TIME_FORMATTER));
        } else {
            selectedScheduleText.setText("Schedule: Not selected");
        }

        if (!selectedSeats.isEmpty()) {
            String seatsList = selectedSeats.stream()
                .map(Seat::getSeatNo)
                .collect(Collectors.joining(", "));
            selectedSeatsText.setText("Seats: " + seatsList);
            
            double totalPrice = selectedSeats.stream()
                .mapToDouble(Seat::getPrice)
                .sum();
            totalPriceText.setText("Total: PKR " + String.format("%.2f", totalPrice));
            
            bookNowButton.setDisable(false);
        } else {
            selectedSeatsText.setText("Seats: Not selected");
            totalPriceText.setText("Total: PKR 0.00");
            bookNowButton.setDisable(true);
        }
    }

    private void showRoutesView() {
        // Toggle visibility of scrollable containers in StackPane
        if (routesContainer.getParent() instanceof ScrollPane) {
             routesContainer.getParent().setVisible(true);
             schedulesContainer.getParent().setVisible(false);
        }
        seatsContainer.setVisible(false);
        
        // Buttons
        backToRoutesButton.setVisible(false);
        backToRoutesButton.setManaged(false);
        backToSchedulesButton.setVisible(false);
        backToSchedulesButton.setManaged(false);
        
        searchBarContainer.setVisible(true);
        searchBarContainer.setManaged(true);
    }

    private void showSchedulesView() {
        if (routesContainer.getParent() instanceof ScrollPane) {
             routesContainer.getParent().setVisible(false);
             schedulesContainer.getParent().setVisible(true);
        }
        seatsContainer.setVisible(false);
        
        backToRoutesButton.setVisible(true);
        backToRoutesButton.setManaged(true);
        backToSchedulesButton.setVisible(false);
        backToSchedulesButton.setManaged(false);
        
        searchBarContainer.setVisible(false);
        searchBarContainer.setManaged(false);
    }

    private void showSeatsView() {
        if (routesContainer.getParent() instanceof ScrollPane) {
             routesContainer.getParent().setVisible(false);
             schedulesContainer.getParent().setVisible(false);
        }
        seatsContainer.setVisible(true);
        
        backToRoutesButton.setVisible(true);
        backToRoutesButton.setManaged(true);
        backToSchedulesButton.setVisible(true);
        backToSchedulesButton.setManaged(true);
        
        searchBarContainer.setVisible(false);
        searchBarContainer.setManaged(false);
    }

    private void bookTickets() {
        if (selectedRoute == null || selectedSchedule == null || selectedSeats.isEmpty()) {
            showAlert("Selection Error", "Please select a route, schedule, and at least one seat.");
            return;
        }

        String confirmationMessage = String.format(
            "Confirm Booking:\n\n" +
            "Route: %s → %s\n" +
            "Date: %s\n" +
            "Time: %s\n" +
            "Class: %s\n" +
            "Seats: %s\n" +
            "Total Price: PKR %.2f\n\n" +
            "Proceed with payment?",
            selectedRoute.getSource(),
            selectedRoute.getDestination(),
            selectedSchedule.getDate().format(DATE_FORMATTER),
            selectedSchedule.getDepartureTime().format(TIME_FORMATTER),
            selectedSchedule.getScheduleClass(),
            selectedSeats.stream().map(Seat::getSeatNo).collect(Collectors.joining(", ")),
            selectedSeats.stream().mapToDouble(Seat::getPrice).sum()
        );

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Booking");
        confirmation.setHeaderText("Booking Summary");
        confirmation.setContentText(confirmationMessage);

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean bookingSuccess = processBooking();
            
            if (bookingSuccess) {
                showAlert("Booking Successful", 
                    "Your tickets have been booked successfully!\n\n" +
                    "Booking details have been sent to your email.\n" +
                    "You can view your tickets in 'My Bookings' section.");
                
                selectedRoute = null;
                selectedSchedule = null;
                selectedSeats.clear();
                showRoutesView();
                loadAllRoutes();
            } else {
                showAlert("Booking Failed", 
                    "Sorry, we couldn't process your booking at this time.\n" +
                    "Please try again or contact support.");
            }
        }
    }

    private boolean processBooking() {
        try {
            Thread.sleep(1000);
            return Math.random() > 0.1;
        } catch (InterruptedException e) {
            return false;
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}