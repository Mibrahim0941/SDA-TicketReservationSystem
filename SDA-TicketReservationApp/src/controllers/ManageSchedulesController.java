package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Admin;
import models.Route;
import models.Schedule;
import catalogs.RouteCatalog;
import helpers.IDGenerator;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ManageSchedulesController {

    private RouteCatalog routeCatalog = RouteCatalog.getInstance();
    private String currentUsername;
    private Admin currentAdmin;

    @FXML private Text welcomeTitle;
    @FXML private Text userGreeting;
    
    @FXML private TableView<Schedule> schedulesTable;
    @FXML private TableColumn<Schedule, String> scheduleIdColumn;
    @FXML private TableColumn<Schedule, LocalDate> dateColumn;
    @FXML private TableColumn<Schedule, LocalTime> departureColumn;
    @FXML private TableColumn<Schedule, LocalTime> arrivalColumn;
    @FXML private TableColumn<Schedule, String> classColumn;
    
    @FXML private ComboBox<Route> routeComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TextField departureField;
    @FXML private TextField arrivalField;
    @FXML private ComboBox<String> classComboBox;
    
    @FXML private Button addScheduleButton;
    @FXML private Button updateScheduleButton;
    @FXML private Button deleteScheduleButton;
    @FXML private Button refreshButton;
    @FXML private Button clearButton;
    @FXML private Button backButton;
    
    @FXML private TextArea scheduleDetailsArea;

    public static void show(Stage stage, String username, Admin admin) {
        try {
            FXMLLoader loader = new FXMLLoader(ManageSchedulesController.class.getResource("/ui/manage-schedules.fxml"));
            Parent root = loader.load();
            
            ManageSchedulesController controller = loader.getController();
            controller.setAdminData(username, admin);
            
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(ManageSchedulesController.class.getResource("/ui/manage-schedules.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Manage Schedules");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to load schedule management page: " + e.getMessage());
        }
    }

    public void setAdminData(String username, Admin admin) {
        this.currentUsername = username;
        this.currentAdmin = admin;
        
        if (userGreeting != null) {
            userGreeting.setText("Manage Schedules - " + username);
        }
        
        Platform.runLater(this::loadData);
    }

    @FXML
    public void initialize() {
        System.out.println("ManageSchedulesController initialized");
        setupEventHandlers();
        initializeTable();
        initializeComboBoxes();
    }

    private void setupEventHandlers() {
        if (addScheduleButton != null) {
            addScheduleButton.setOnAction(e -> handleAddSchedule());
        }
        if (updateScheduleButton != null) {
            updateScheduleButton.setOnAction(e -> handleUpdateSchedule());
        }
        if (deleteScheduleButton != null) {
            deleteScheduleButton.setOnAction(e -> handleDeleteSchedule());
        }
        if (refreshButton != null) {
            refreshButton.setOnAction(e -> handleRefresh());
        }
        if (clearButton != null) {
            clearButton.setOnAction(e -> handleClear());
        }
        if (backButton != null) {
            backButton.setOnAction(e -> handleBack());
        }

        if (routeComboBox != null) {
            routeComboBox.setOnAction(e -> handleRouteSelection());
        }
    }

    private void initializeTable() {
        if (schedulesTable != null) {
            scheduleIdColumn.setCellValueFactory(new PropertyValueFactory<>("scheduleID"));
            dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
            departureColumn.setCellValueFactory(new PropertyValueFactory<>("departureTime"));
            arrivalColumn.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
            classColumn.setCellValueFactory(new PropertyValueFactory<>("scheduleClass"));
            
            schedulesTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> showScheduleDetails(newSelection)
            );
        }
    }

    private void initializeComboBoxes() {
        if (classComboBox != null) {
            classComboBox.getItems().addAll("Economy", "Business", "First Class");
        }
        
        if (routeComboBox != null) {
            routeComboBox.setCellFactory(param -> new ListCell<Route>() {
                @Override
                protected void updateItem(Route item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getSource() + " → " + item.getDestination() + " (PKR " + item.getBasePrice() + ")");
                    }
                }
            });

            routeComboBox.setButtonCell(new ListCell<Route>() {
                @Override
                protected void updateItem(Route item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getSource() + " → " + item.getDestination() + " (PKR " + item.getBasePrice() + ")");
                    }
                }
            });
        }
    }

    private void loadData() {
        if (routeCatalog != null && routeComboBox != null) {
            routeCatalog.refresh();
            routeComboBox.getItems().setAll(routeCatalog.getAllRoutes());
        }
    }

    private void handleRouteSelection() {
        Route selectedRoute = routeComboBox.getValue();
        if (selectedRoute != null) {
            refreshSchedulesTable(selectedRoute.getRouteID());
        } else {
            schedulesTable.getItems().clear();
        }
    }

    private void refreshSchedulesTable(String routeId) {
        if (routeCatalog != null) {
            List<Schedule> schedules = routeCatalog.getRouteSchedules(routeId);
            if (schedules != null) {
                schedulesTable.setItems(FXCollections.observableArrayList(schedules));
            } else {
                schedulesTable.getItems().clear();
            }
        }
    }

    private void showScheduleDetails(Schedule schedule) {
        if (schedule != null) {
            datePicker.setValue(schedule.getDate());
            departureField.setText(schedule.getDepartureTime().toString());
            arrivalField.setText(schedule.getArrivalTime().toString());
            classComboBox.setValue(schedule.getScheduleClass());
            
            if (scheduleDetailsArea != null) {
                StringBuilder details = new StringBuilder();
                details.append("=== SCHEDULE DETAILS ===\n\n");
                details.append("Schedule ID: ").append(schedule.getScheduleID()).append("\n");
                details.append("Date: ").append(schedule.getDate()).append("\n");
                details.append("Departure Time: ").append(schedule.getDepartureTime()).append("\n");
                details.append("Arrival Time: ").append(schedule.getArrivalTime()).append("\n");
                details.append("Class: ").append(schedule.getScheduleClass()).append("\n");
                details.append("Available Seats: ").append(schedule.getSeatCount()).append("\n");
                details.append("Type Percentage: ").append(schedule.getTypePercentage()).append("%");
                
                scheduleDetailsArea.setText(details.toString());
            }
        }
    }

    private void handleAddSchedule() {
        Route selectedRoute = routeComboBox.getValue();
        LocalDate date = datePicker.getValue();
        String departureText = departureField.getText().trim();
        String arrivalText = arrivalField.getText().trim();
        String scheduleClass = classComboBox.getValue();
        
        if (selectedRoute == null || date == null || departureText.isEmpty() || 
            arrivalText.isEmpty() || scheduleClass == null) {
            showError("Please fill in all fields");
            return;
        }
        
        try {
            LocalTime departureTime = LocalTime.parse(departureText);
            LocalTime arrivalTime = LocalTime.parse(arrivalText);
            
            if (arrivalTime.isBefore(departureTime)) {
                showError("Arrival time must be after departure time");
                return;
            }
            
            if (date.isBefore(LocalDate.now())) {
                showError("Schedule date cannot be in the past");
                return;
            }
            
            String scheduleID = IDGenerator.generateScheduleID();
            Schedule newSchedule = new Schedule(scheduleID, date, departureTime, arrivalTime, scheduleClass);
            
            boolean success = routeCatalog.addScheduleToRoute(selectedRoute.getRouteID(), newSchedule);
            
            if (success) {
                showSuccess("Schedule added successfully!\nSchedule ID: " + scheduleID);
                handleClear();
                refreshSchedulesTable(selectedRoute.getRouteID());
            } else {
                showError("Failed to add schedule");
            }
            
        } catch (Exception e) {
            showError("Invalid time format. Use HH:mm format (e.g., 14:30)");
        }
    }

    private void handleUpdateSchedule() {
        Schedule selectedSchedule = schedulesTable.getSelectionModel().getSelectedItem();
        Route selectedRoute = routeComboBox.getValue();
        
        if (selectedSchedule == null || selectedRoute == null) {
            showError("Please select a schedule to update");
            return;
        }
        
        LocalDate date = datePicker.getValue();
        String departureText = departureField.getText().trim();
        String arrivalText = arrivalField.getText().trim();
        String scheduleClass = classComboBox.getValue();
        
        if (date == null || departureText.isEmpty() || arrivalText.isEmpty() || scheduleClass == null) {
            showError("Please fill in all fields");
            return;
        }
        
        try {
            LocalTime departureTime = LocalTime.parse(departureText);
            LocalTime arrivalTime = LocalTime.parse(arrivalText);
            
            if (arrivalTime.isBefore(departureTime)) {
                showError("Arrival time must be after departure time");
                return;
            }
            
            if (date.isBefore(LocalDate.now())) {
                showError("Schedule date cannot be in the past");
                return;
            }
            
            selectedSchedule.setDate(date);
            selectedSchedule.setDepartureTime(departureTime);
            selectedSchedule.setArrivalTime(arrivalTime);
            selectedSchedule.setScheduleClass(scheduleClass);
            
            boolean success = routeCatalog.updateRoute(selectedRoute);
            
            if (success) {
                showSuccess("Schedule updated successfully!");
                refreshSchedulesTable(selectedRoute.getRouteID());
            } else {
                showError("Failed to update schedule");
            }
            
        } catch (Exception e) {
            showError("Invalid time format. Use HH:mm format (e.g., 14:30)");
        }
    }

    private void handleDeleteSchedule() {
        Schedule selectedSchedule = schedulesTable.getSelectionModel().getSelectedItem();
        Route selectedRoute = routeComboBox.getValue();
        
        if (selectedSchedule == null || selectedRoute == null) {
            showError("Please select a schedule to delete");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Schedule");
        alert.setContentText("Are you sure you want to delete this schedule?\n" + 
                            selectedSchedule.getDate() + " " + 
                            selectedSchedule.getDepartureTime() + " - " + 
                            selectedSchedule.getArrivalTime() + " (" + 
                            selectedSchedule.getScheduleClass() + ")");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                selectedRoute.getSchedules().remove(selectedSchedule);
                
                boolean success = routeCatalog.updateRoute(selectedRoute);
                
                if (success) {
                    showSuccess("Schedule deleted successfully!");
                    refreshSchedulesTable(selectedRoute.getRouteID());
                    handleClear();
                } else {
                    showError("Failed to delete schedule");
                }
            }
        });
    }

    private void handleRefresh() {
        loadData();
        Route selectedRoute = routeComboBox.getValue();
        if (selectedRoute != null) {
            refreshSchedulesTable(selectedRoute.getRouteID());
        }
        showSuccess("Data refreshed successfully");
    }

    private void handleClear() {
        datePicker.setValue(null);
        departureField.clear();
        arrivalField.clear();
        classComboBox.setValue(null);
        if (scheduleDetailsArea != null) {
            scheduleDetailsArea.clear();
        }
        schedulesTable.getSelectionModel().clearSelection();
    }

    private void handleBack() {
        try {
            Stage currentStage = (Stage) backButton.getScene().getWindow();
            AdminDashboardController.show(currentStage, currentUsername, currentAdmin);
        } catch (Exception e) {
            showError("Failed to go back: " + e.getMessage());
        }
    }

    private void showError(String message) {
        showAlert("Error", message, Alert.AlertType.ERROR);
    }

    private void showSuccess(String message) {
        showAlert("Success", message, Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private static void showErrorAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}