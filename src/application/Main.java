package application;
import javafx.application.Application;
import javafx.stage.Stage;
import utils.SceneSwitcher;
import Services.ReminderService;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        // Minimum size prevents layout engine from fighting at tiny dimensions
        stage.setMinWidth(860);
        stage.setMinHeight(600);
        stage.setResizable(true);
        SceneSwitcher.setPrimaryStage(stage);
        SceneSwitcher.switchScene("/views/login.fxml", "Login — Health Assistance System");
    }

    @Override
    public void stop() {
        ReminderService.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}