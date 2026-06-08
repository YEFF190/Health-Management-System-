package controllers;

import Services.ReminderService;
import Services.ReminderService.NotificationRow;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Popup;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * NotificationController
 *
 * Manages the 🔔 bell + badge embedded in every dashboard via fx:include.
 * The dropdown is a JavaFX Popup so it floats above all layout nodes
 * and is never clipped by ScrollPanes, HBoxes, or z-order issues.
 *
 * How to embed in a dashboard FXML:
 *   <fx:include fx:id="notifPanel" source="notification_panel.fxml"/>
 *
 * In the dashboard controller:
 *   @FXML private NotificationController notifPanelController;
 *   // Inside initialize():
 *   if (notifPanelController != null)
 *       notifPanelController.init(Session.getCurrentUser().getUserId());
 */
public class NotificationController {

    // ── FXML bindings (notification_panel.fxml) ───────────────
    @FXML private Button bellButton;
    @FXML private Label  badgeLabel;

    // ── Popup dropdown (built in Java) ────────────────────────
    private final Popup        popup    = new Popup();
    private final ListView<NotificationRow> listView = new ListView<>();
    private final Label        badgeInHeader         = new Label();

    // ── State ─────────────────────────────────────────────────
    private int userId = -1;

    private static final ScheduledExecutorService POLLER =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NotifBadgePoller");
            t.setDaemon(true);
            return t;
        });

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd MMM HH:mm");

    // ── Public API ────────────────────────────────────────────

    /**
     * Call once after FXML injection, passing the logged-in user's id.
     * Builds the popup, starts badge polling.
     */
    public void init(int userId) {
        this.userId = userId;
        buildPopup();
        refreshBadge();
        POLLER.scheduleAtFixedRate(this::refreshBadge, 30, 30, TimeUnit.SECONDS);
    }

    // ── FXML handler ──────────────────────────────────────────

    @FXML
    private void toggleDropdown() {
        if (popup.isShowing()) {
            popup.hide();
        } else {
            loadNotifications();
            // Anchor popup below the bell button
            javafx.geometry.Bounds bounds =
                bellButton.localToScreen(bellButton.getBoundsInLocal());
            // Align right edge of popup with right edge of bell
            double popupWidth = 360;
            double x = bounds.getMaxX() - popupWidth;
            double y = bounds.getMaxY() + 6;
            popup.show(bellButton.getScene().getWindow(), x, y);
        }
    }

    // ── Build popup UI ────────────────────────────────────────

    private void buildPopup() {
        // ── Header ──
        Label title = new Label("Notifications");
        title.setStyle(
            "-fx-font-family:'Palatino Linotype',Georgia,serif;"
          + "-fx-font-weight:bold; -fx-font-size:13px; -fx-text-fill:#2c2825;");

        Button markAll = new Button("Mark all read");
        markAll.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#2d6a4f;"
          + "-fx-font-size:11px; -fx-cursor:hand; -fx-padding:0; -fx-border-width:0;");
        markAll.setOnAction(e -> {
            ReminderService.markAllRead(userId);
            loadNotifications();
            refreshBadge();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(8, title, spacer, markAll);
        header.setPadding(new Insets(13, 16, 11, 16));
        header.setStyle(
            "-fx-border-color:rgba(44,40,37,0.08); -fx-border-width:0 0 1 0;");
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // ── List ──
        listView.setPrefHeight(300);
        listView.setPrefWidth(358);
        listView.setStyle(
            "-fx-background-color:transparent; -fx-border-color:transparent;");
        listView.setPlaceholder(new Label("No new notifications"));

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(NotificationRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null); setText(null);
                    return;
                }

                VBox box = new VBox(3);
                box.setPadding(new Insets(9, 14, 9, 14));
                box.setStyle(
                    "-fx-border-color:transparent transparent #f5f1ed transparent;"
                  + "-fx-border-width:0 0 1 0;");

                Label titleLbl = new Label(item.title);
                titleLbl.setStyle(
                    "-fx-font-family:'Palatino Linotype',Georgia,serif;"
                  + "-fx-font-weight:bold; -fx-font-size:12.5px; -fx-text-fill:#2c2825;");

                Label msgLbl = new Label(item.message);
                msgLbl.setWrapText(true);
                msgLbl.setMaxWidth(320);
                msgLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#4a4240;");

                Label timeLbl = new Label(item.createdAt.format(FMT));
                timeLbl.setStyle(
                    "-fx-font-size:10px; -fx-text-fill:#b5aca6;"
                  + "-fx-font-family:'Consolas',monospace;");

                box.getChildren().addAll(titleLbl, msgLbl, timeLbl);
                setGraphic(box);
                setText(null);

                // Mark as read on click
                setOnMouseClicked(e -> {
                    ReminderService.markRead(item.id);
                    refreshBadge();
                    loadNotifications();
                });
            }
        });

        // ── Container ──
        VBox container = new VBox(header, listView);
        container.setStyle(
            "-fx-background-color:white;"
          + "-fx-background-radius:10;"
          + "-fx-border-color:rgba(44,40,37,0.13);"
          + "-fx-border-radius:10;"
          + "-fx-border-width:1;"
          + "-fx-effect:dropshadow(gaussian,rgba(44,40,37,0.18),20,0,0,6);");
        container.setPrefWidth(360);

        popup.getContent().add(container);
        popup.setAutoHide(true);
    }

    // ── Data helpers ──────────────────────────────────────────

    private void loadNotifications() {
        if (userId < 0) return;
        List<NotificationRow> rows = ReminderService.getUnread(userId, 20);
        Platform.runLater(() ->
            listView.setItems(FXCollections.observableArrayList(rows)));
    }

    private void refreshBadge() {
        if (userId < 0) return;
        int count = ReminderService.countUnread(userId);
        Platform.runLater(() -> {
            if (count > 0) {
                badgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
                badgeLabel.setVisible(true);
                badgeLabel.setManaged(true);
            } else {
                badgeLabel.setVisible(false);
                badgeLabel.setManaged(false);
            }
        });
    }
}