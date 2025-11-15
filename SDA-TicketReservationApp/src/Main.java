import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
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

    public static void main(String[] args) {
        launch(args);
    }
}