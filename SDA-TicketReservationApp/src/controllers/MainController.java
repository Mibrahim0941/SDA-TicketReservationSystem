package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;

public class MainController {

    @FXML
    private Button getStartedButton;

    @FXML
    private VBox adminCard;

    @FXML
    private VBox customerCard;

    @FXML
    private VBox staffCard;

    public static void show(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(MainController.class.getResource("/ui/mainpage.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 900, 700);
            scene.getStylesheets().add(MainController.class.getResource("/ui/style.css").toExternalForm());
            scene.getStylesheets().add(MainController.class.getResource("/ui/mainpage.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Welcome");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to load main page: " + e.getMessage());
        }
    }

    public static void showoptions(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(MainController.class.getResource("/ui/loginoptions.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 900, 700);
            scene.getStylesheets().add(MainController.class.getResource("/ui/style.css").toExternalForm());
            scene.getStylesheets().add(MainController.class.getResource("/ui/loginoptions.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Welcome");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to load main page: " + e.getMessage());
        }
    }

    public static void show(Stage stage, String username) {
        show(stage); 
    }

    @FXML
    private void handleGetStarted() {
        try {
            Stage currentStage = (Stage) getStartedButton.getScene().getWindow();
            loadLoginOptionsPage(currentStage);
        } catch (Exception e) {
            showError("Failed to load login options page");
        }
    }

    @FXML
    private void handleAdminLogin() {
        System.out.println("Admin login selected");
        try {
            Stage currentStage = (Stage) adminCard.getScene().getWindow();
            loadLoginPage(currentStage, "Admin");
        } catch (Exception e) {
            showError("Admin login page not implemented yet");
        }
    }

    @FXML
    private void handleCustomerLogin() {
        System.out.println("Customer login selected");
        try {
            Stage currentStage = (Stage) customerCard.getScene().getWindow();
            loadLoginPage(currentStage, "Customer");
        } catch (Exception e) {
            showError("Failed to load customer login page");
        }
    }

    @FXML
    private void handleStaffLogin() {
        System.out.println("Staff login selected");
        try {
            Stage currentStage = (Stage) staffCard.getScene().getWindow();
            loadLoginPage(currentStage, "Staff");
        } catch (Exception e) {
            showError("Staff login page not implemented yet");
        }
    }

    private void loadLoginOptionsPage(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/loginoptions.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 900, 700);
            scene.getStylesheets().add(getClass().getResource("/ui/style.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/ui/loginoptions.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("TicketGenie - Login Options");
            stage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load login options page: " + e.getMessage());
        }
    }

    private void loadLoginPage(Stage stage, String userType) {
        try {
            switch (userType) {
                case "Customer":
                    CustomerLoginController.show(stage);
                    break;
                    
                case "Admin":
                    AdminLoginController.show(stage);
                    break;
                    
                case "Staff":
                    StaffLoginController.show(stage);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load " + userType.toLowerCase() + " login page");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // private void showInfo(String title, String message) {
    //     Alert alert = new Alert(Alert.AlertType.INFORMATION);
    //     alert.setTitle(title);
    //     alert.setHeaderText(null);
    //     alert.setContentText(message);
    //     alert.showAndWait();
    // }

    private static void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}