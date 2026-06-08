package utils;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Toast — floating notification popup shown in the bottom-right corner.
 *
 * Usage:
 *   Toast.show("Appointment Reminder", "Dr. Smith at 10:30", Toast.Type.WARNING);
 *   Toast.show("Saved", "Profile updated successfully.", Toast.Type.SUCCESS);
 */
public class Toast {

    public enum Type { SUCCESS, INFO, WARNING, ERROR }

    /**
     * Show a toast. Safe to call from any thread.
     * @param title   Bold headline (e.g. "Appointment Reminder")
     * @param message Body text
     * @param type    Controls the accent colour and icon
     */
    public static void show(String title, String message, Type type) {
        Platform.runLater(() -> display(title, message, type));
    }

    /** Convenience — INFO type */
    public static void info(String title, String message) {
        show(title, message, Type.INFO);
    }

    /** Convenience — SUCCESS type */
    public static void success(String title, String message) {
        show(title, message, Type.SUCCESS);
    }

    /** Convenience — WARNING type (used for appointment reminders) */
    public static void warning(String title, String message) {
        show(title, message, Type.WARNING);
    }

    /** Convenience — ERROR type */
    public static void error(String title, String message) {
        show(title, message, Type.ERROR);
    }

    // ── Internal rendering ────────────────────────────────────────────────────

    private static void display(String title, String message, Type type) {
        Stage toast = new Stage(StageStyle.TRANSPARENT);
        toast.setAlwaysOnTop(true);

        // ── Icon & accent colour ──
        String icon, accent, bg;
        switch (type) {
            case SUCCESS -> { icon = "✓"; accent = "#00b09b"; bg = "#f0fdf9"; }
            case WARNING -> { icon = "⏰"; accent = "#f59e0b"; bg = "#fffbeb"; }
            case ERROR   -> { icon = "✕"; accent = "#ef4444"; bg = "#fef2f2"; }
            default      -> { icon = "ℹ"; accent = "#3b82f6"; bg = "#eff6ff"; }
        }

        // ── Icon circle ──
        Label iconLabel = new Label(icon);
        iconLabel.setStyle(
            "-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:" + accent + ";" +
            "-fx-background-color:" + accent + "22;" +
            "-fx-background-radius:20; -fx-padding:8 10 8 10;");

        // ── Text block ──
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight:bold; -fx-font-size:13px; -fx-text-fill:#1a2332;");

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(260);
        msgLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#4a5568;");

        VBox textBox = new VBox(2, titleLabel, msgLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);

        // ── Close button ──
        Label closeBtn = new Label("×");
        closeBtn.setStyle("-fx-font-size:16px; -fx-text-fill:#9ca3af; -fx-cursor:hand; -fx-padding:0 0 0 8;");
        closeBtn.setOnMouseClicked(e -> toast.close());

        // ── Container ──
        HBox root = new HBox(12, iconLabel, textBox, closeBtn);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(14, 16, 14, 16));
        root.setStyle(
            "-fx-background-color:" + bg + ";" +
            "-fx-background-radius:12;" +
            "-fx-border-color:" + accent + "44;" +
            "-fx-border-radius:12;" +
            "-fx-border-width:1;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.18),16,0,0,4);");
        root.setPrefWidth(340);

        // Click anywhere to dismiss
        root.setOnMouseClicked(e -> toast.close());

        Scene scene = new Scene(root);
        scene.setFill(null);
        toast.setScene(scene);

        // ── Position: bottom-right of screen ──
        javafx.geometry.Rectangle2D screen =
                javafx.stage.Screen.getPrimary().getVisualBounds();
        toast.setX(screen.getMaxX() - 360);
        toast.setY(screen.getMaxY() - 100);

        // ── Animate: fade in ──
        root.setOpacity(0);
        toast.show();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        // ── Auto-dismiss after 5 s ──
        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), root);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> toast.close());

        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(e -> fadeOut.play());

        new SequentialTransition(fadeIn, pause, fadeOut).play();
    }
}
