package utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class SceneSwitcher {

    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void switchScene(String fxmlPath, String title) {
        try {
            URL fxmlUrl = SceneSwitcher.class.getResource(fxmlPath);
            if (fxmlUrl == null)
                throw new IllegalArgumentException("FXML not found: " + fxmlPath);

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Scene scene = new Scene(loader.load());

            URL cssUrl = SceneSwitcher.class.getResource("/application/application.css");
            if (cssUrl != null)
                scene.getStylesheets().add(cssUrl.toExternalForm());
            else
                System.err.println("WARNING: application.css not found — UI will be unstyled.");

            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scene: " + fxmlPath, e);
        }
    }
}