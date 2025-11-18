import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Initialize catalogs with sample data
            initializeSampleData();
            
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/mainpage.fxml"));
            Parent root = loader.load();
            
            // Set up the stage
            primaryStage.setTitle("TicketGenie - Login");
            primaryStage.setScene(new Scene(root, 900, 700));
            primaryStage.setResizable(true);
            
            // Apply CSS
            primaryStage.getScene().getStylesheets().add(getClass().getResource("/ui/mainpage.css").toExternalForm());
            
            // Show the stage
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeSampleData() {
        // Initialize some sample data for testing
        System.out.println("Initializing sample data...");
        
        // Sample routes, schedules, policies, etc.
        // This would typically be loaded from a database
    }

    public static void main(String[] args) {
        launch(args);
    }
}