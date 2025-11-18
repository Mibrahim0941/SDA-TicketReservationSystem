package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.Booking;
import models.Customer;
import models.ETicket;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class BookingHistoryController implements Initializable {

    @FXML private Text pageTitle;
    @FXML private Text pageSubtitle;
    
    @FXML private Label totalBookingsLabel;
    @FXML private Label completedTripsLabel;
    @FXML private Label cancelledBookingsLabel;
    @FXML private Label totalSpentLabel;
    
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TextField searchField;
    @FXML private Button applyFiltersButton;
    @FXML private Button clearFiltersButton;
    @FXML private Button exportButton;
    
    @FXML private TableView<Booking> bookingsTable;
    @FXML private TableColumn<Booking, String> bookingIdColumn;
    @FXML private TableColumn<Booking, String> routeColumn;
    @FXML private TableColumn<Booking, String> dateColumn;
    @FXML private TableColumn<Booking, String> bookingDateColumn;
    @FXML private TableColumn<Booking, String> seatsColumn;
    @FXML private TableColumn<Booking, String> classColumn;
    @FXML private TableColumn<Booking, String> amountColumn;
    @FXML private TableColumn<Booking, String> statusColumn;
    @FXML private TableColumn<Booking, String> actionsColumn;
    
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;
    @FXML private Label pageInfoLabel;
    @FXML private ComboBox<Integer> pageSizeCombo;
    
    @FXML private VBox detailsPanel;
    @FXML private Label detailBookingId;
    @FXML private Label detailStatus;
    @FXML private Label detailBookedOn;
    @FXML private Label detailRoute;
    @FXML private Label detailDeparture;
    @FXML private Label detailArrival;
    @FXML private Label detailSeats;
    @FXML private Label detailClass;
    @FXML private Label detailAmount;
    @FXML private Label detailPaymentStatus;
    @FXML private Label detailTravelDate;
    
    @FXML private Button viewTicketButton;
    @FXML private Button downloadTicketButton;
    @FXML private Button bookAgainButton;
    @FXML private Button provideFeedbackButton;
    
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    
    private Customer currentCustomer;
    private String currentUsername;
    private Stage primaryStage;
    
    private List<Booking> allBookings;
    private List<Booking> filteredBookings;
    private int currentPage = 1;
    private int pageSize = 10;
    private int totalPages = 1;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    
    // Static show method for navigation
    public static void show(Stage stage, String username, Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(BookingHistoryController.class.getResource("/ui/booking-history.fxml"));
            Parent root = loader.load();
            
            BookingHistoryController controller = loader.getController();
            controller.setUserData(username, stage, customer);
            
            Scene scene = new Scene(root, 1200, 800);
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
        initializeFilters();
        setupTableColumns();
        setupEventHandlers();
        setupPagination();
    }
    
    public void setUserData(String username, Stage stage, Customer customer) {
        this.currentUsername = username;
        this.primaryStage = stage;
        this.currentCustomer = customer;
        loadBookingsData();
    }
    
    private void initializeFilters() {
        // Initialize status filter options
        statusFilterCombo.getItems().addAll(
            "All Status",
            "Completed",
            "Confirmed", 
            "Cancelled",
            "Pending",
            "Refunded"
        );
        statusFilterCombo.setValue("All Status");
        
        // Initialize page size options
        pageSizeCombo.getItems().addAll(5, 10, 20, 50);
        pageSizeCombo.setValue(10);
        
        // Set default date range (last 6 months)
        fromDatePicker.setValue(LocalDate.now().minusMonths(6));
        toDatePicker.setValue(LocalDate.now());
    }
    
    private void setupTableColumns() {
        // Configure table columns
        bookingIdColumn.setCellValueFactory(cellData -> cellData.getValue().bookingIdProperty());
        routeColumn.setCellValueFactory(cellData -> cellData.getValue().routeInfoProperty());
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().travelDateProperty());
        bookingDateColumn.setCellValueFactory(cellData -> cellData.getValue().bookingDateProperty());
        seatsColumn.setCellValueFactory(cellData -> cellData.getValue().seatsProperty());
        classColumn.setCellValueFactory(cellData -> cellData.getValue().classTypeProperty());
        amountColumn.setCellValueFactory(cellData -> cellData.getValue().amountProperty());
        
        // Status column with styling
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusColumn.setCellFactory(column -> new TableCell<Booking, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status.toUpperCase()) {
                        case "COMPLETED":
                            setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #059669; -fx-background-radius: 8; -fx-padding: 2 6;");
                            break;
                        case "CONFIRMED":
                            setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-background-radius: 8; -fx-padding: 2 6;");
                            break;
                        case "CANCELLED":
                            setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-background-radius: 8; -fx-padding: 2 6;");
                            break;
                        case "PENDING":
                            setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #d97706; -fx-background-radius: 8; -fx-padding: 2 6;");
                            break;
                        case "REFUNDED":
                            setStyle("-fx-background-color: #f3e8ff; -fx-text-fill: #7c3aed; -fx-background-radius: 8; -fx-padding: 2 6;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        
        // Actions column
        actionsColumn.setCellFactory(column -> new TableCell<Booking, String>() {
            private final Button viewButton = new Button("👁 View");
            private final Button ticketButton = new Button("🎫 Ticket");
            
            {
                viewButton.setStyle("-fx-font-size: 10px; -fx-padding: 2px 6px;");
                ticketButton.setStyle("-fx-font-size: 10px; -fx-padding: 2px 6px;");
                
                viewButton.setOnAction(e -> {
                    Booking booking = getTableView().getItems().get(getIndex());
                    showBookingDetails(booking);
                });
                
                ticketButton.setOnAction(e -> {
                    Booking booking = getTableView().getItems().get(getIndex());
                    viewETicket(booking);
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5);
                    buttons.getChildren().addAll(viewButton, ticketButton);
                    setGraphic(buttons);
                }
            }
        });
    }
    
    private void setupEventHandlers() {
        // Filter buttons
        applyFiltersButton.setOnAction(e -> applyFilters());
        clearFiltersButton.setOnAction(e -> clearFilters());
        exportButton.setOnAction(e -> exportHistory());
        
        // Pagination
        prevPageButton.setOnAction(e -> previousPage());
        nextPageButton.setOnAction(e -> nextPage());
        pageSizeCombo.setOnAction(e -> changePageSize());
        
        // Detail buttons
        viewTicketButton.setOnAction(e -> viewSelectedTicket());
        downloadTicketButton.setOnAction(e -> downloadSelectedTicket());
        bookAgainButton.setOnAction(e -> bookAgain());
        provideFeedbackButton.setOnAction(e -> provideFeedback());
        
        // Navigation buttons
        refreshButton.setOnAction(e -> loadBookingsData());
        backButton.setOnAction(e -> navigateToDashboard());
        
        // Table selection listener
        bookingsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    showBookingDetails(newSelection);
                }
            });
    }
    
    private void setupPagination() {
        updatePaginationControls();
    }
    
    private void loadBookingsData() {
        // Load ALL bookings for customer (including completed and cancelled)
        allBookings = Booking.getAllBookingsForCustomer(currentCustomer.getCustomerID());
        applyFilters(); // This will apply current filters and pagination
        updateStatistics();
    }
    
    private void applyFilters() {
        if (allBookings == null) return;
        
        List<Booking> filtered = allBookings.stream()
            .filter(this::filterByDate)
            .filter(this::filterByStatus)
            .filter(this::filterBySearch)
            .collect(Collectors.toList());
        
        filteredBookings = filtered;
        currentPage = 1;
        updatePagination();
        displayCurrentPage();
    }
    
    private boolean filterByDate(Booking booking) {
        if (fromDatePicker.getValue() == null && toDatePicker.getValue() == null) {
            return true;
        }
        
        LocalDate bookingDate = booking.getBookingDate();
        if (bookingDate == null) return true;
        
        boolean afterFrom = fromDatePicker.getValue() == null || 
                           !bookingDate.isBefore(fromDatePicker.getValue());
        boolean beforeTo = toDatePicker.getValue() == null || 
                          !bookingDate.isAfter(toDatePicker.getValue());
        
        return afterFrom && beforeTo;
    }
    
    private boolean filterByStatus(Booking booking) {
        String selectedStatus = statusFilterCombo.getValue();
        if (selectedStatus == null || "All Status".equals(selectedStatus)) {
            return true;
        }
        return selectedStatus.equalsIgnoreCase(booking.getStatus());
    }
    
    private boolean filterBySearch(Booking booking) {
        String searchText = searchField.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            return true;
        }
        
        String searchLower = searchText.toLowerCase();
        return booking.getBookingID().toLowerCase().contains(searchLower) ||
               booking.getRouteInfo().toLowerCase().contains(searchLower);
    }
    
    private void clearFilters() {
        fromDatePicker.setValue(LocalDate.now().minusMonths(6));
        toDatePicker.setValue(LocalDate.now());
        statusFilterCombo.setValue("All Status");
        searchField.clear();
        applyFilters();
    }
    
    private void updatePagination() {
        if (filteredBookings == null) {
            totalPages = 1;
        } else {
            totalPages = (int) Math.ceil((double) filteredBookings.size() / pageSize);
        }
        updatePaginationControls();
    }
    
    private void updatePaginationControls() {
        pageInfoLabel.setText(String.format("Page %d of %d", currentPage, totalPages));
        prevPageButton.setDisable(currentPage <= 1);
        nextPageButton.setDisable(currentPage >= totalPages);
    }
    
    private void displayCurrentPage() {
        if (filteredBookings == null) {
            bookingsTable.getItems().clear();
            return;
        }
        
        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filteredBookings.size());
        
        if (fromIndex >= filteredBookings.size()) {
            bookingsTable.getItems().clear();
        } else {
            List<Booking> pageBookings = filteredBookings.subList(fromIndex, toIndex);
            bookingsTable.getItems().setAll(pageBookings);
        }
    }
    
    private void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            displayCurrentPage();
            updatePaginationControls();
        }
    }
    
    private void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            displayCurrentPage();
            updatePaginationControls();
        }
    }
    
    private void changePageSize() {
        pageSize = pageSizeCombo.getValue();
        currentPage = 1;
        updatePagination();
        displayCurrentPage();
    }
    
    private void updateStatistics() {
        if (allBookings == null) return;
        
        long total = allBookings.size();
        long completed = allBookings.stream().filter(b -> "COMPLETED".equals(b.getStatus())).count();
        long cancelled = allBookings.stream().filter(b -> "CANCELLED".equals(b.getStatus())).count();
        double totalSpent = allBookings.stream()
            .filter(b -> !"CANCELLED".equals(b.getStatus()))
            .mapToDouble(b -> Double.parseDouble(b.getAmount().replace("$", "")))
            .sum();
        
        totalBookingsLabel.setText(String.valueOf(total));
        completedTripsLabel.setText(String.valueOf(completed));
        cancelledBookingsLabel.setText(String.valueOf(cancelled));
        totalSpentLabel.setText(String.format("$%.2f", totalSpent));
    }
    
    private void showBookingDetails(Booking booking) {
        detailsPanel.setVisible(true);
        
        detailBookingId.setText(booking.getBookingID());
        detailStatus.setText(booking.getStatus());
        detailBookedOn.setText(booking.getBookingDateTime());
        detailRoute.setText(booking.getRouteInfo());
        detailDeparture.setText(booking.getDepartureTime());
        detailArrival.setText(booking.getArrivalTime());
        detailSeats.setText(booking.getSeats());
        detailClass.setText(booking.getClassType());
        detailAmount.setText(booking.getAmount());
        detailPaymentStatus.setText(booking.getPaymentStatus());
        detailTravelDate.setText(booking.getTravelDate());
        
        // Enable/disable buttons based on status
        boolean canViewTicket = "CONFIRMED".equals(booking.getStatus()) || "COMPLETED".equals(booking.getStatus());
        viewTicketButton.setDisable(!canViewTicket);
        downloadTicketButton.setDisable(!canViewTicket);
        provideFeedbackButton.setDisable(!"COMPLETED".equals(booking.getStatus()));
    }
    
    private void viewSelectedTicket() {
        Booking selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            viewETicket(selected);
        }
    }
    
    private void viewETicket(Booking booking) {
        ETicket ticket = booking.GetETicket();
        if (ticket != null) {
            // Show ticket in a dialog or new window
            showAlert("E-Ticket", "Displaying E-Ticket for booking: " + booking.getBookingID());
            // In real implementation, you'd show the ticket details
        } else {
            showAlert("Error", "No E-Ticket available for this booking.");
        }
    }
    
    private void downloadSelectedTicket() {
        Booking selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Generate and download PDF ticket
            boolean success = generateTicketPDF(selected);
            if (success) {
                showAlert("Success", "E-Ticket PDF downloaded successfully!");
            } else {
                showAlert("Error", "Failed to download E-Ticket.");
            }
        }
    }
    
    private boolean generateTicketPDF(Booking booking) {
        // Implement PDF generation logic here
        // This would use a PDF library to generate the ticket
        return true; // Placeholder
    }
    
    private void bookAgain() {
        Booking selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Navigate to booking page with pre-filled route information
            showAlert("Book Again", "Redirecting to booking page with route: " + selected.getRouteInfo());
            // In real implementation, you'd navigate to booking page with pre-filled data
        }
    }
    
    private void provideFeedback() {
        Booking selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Show feedback dialog
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Provide Feedback");
            dialog.setHeaderText("Feedback for booking: " + selected.getBookingID());
            dialog.setContentText("Please enter your feedback:");
            
            dialog.showAndWait().ifPresent(feedback -> {
                if (!feedback.trim().isEmpty()) {
                    // Save feedback to database
                    boolean success = saveFeedback(selected, feedback);
                    if (success) {
                        showAlert("Thank You", "Your feedback has been submitted!");
                    } else {
                        showAlert("Error", "Failed to submit feedback.");
                    }
                }
            });
        }
    }
    
    private boolean saveFeedback(Booking booking, String feedback) {
        // Implement feedback saving logic
        return true; // Placeholder
    }
    
    private void exportHistory() {
        // Export booking history to CSV or PDF
        boolean success = exportToCSV();
        if (success) {
            showAlert("Export Successful", "Booking history exported successfully!");
        } else {
            showAlert("Export Failed", "Failed to export booking history.");
        }
    }
    
    private boolean exportToCSV() {
        // Implement CSV export logic
        return true; // Placeholder
    }
    
    private void navigateToDashboard() {
        DashboardController.show(primaryStage, currentUsername, currentCustomer);
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