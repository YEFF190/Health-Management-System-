package utils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * SearchablePickerDialog
 *
 * A popup that appears below a trigger node and shows a live-filtered
 * scrollable list of items. Replaces plain ComboBox for doctor/patient
 * selection in appointment booking.
 *
 * Usage example:
 *
 *   SearchablePickerDialog<Doctor> picker = new SearchablePickerDialog<>(
 *       doctors,
 *       d -> "Dr. " + d.getName() + " — " + d.getSpecialty(),
 *       selected -> {
 *           selectedDoctor = selected;
 *           doctorPickerBtn.setText("Dr. " + selected.getName());
 *       }
 *   );
 *   picker.show(doctorPickerBtn);
 */
public class SearchablePickerDialog<T> {

    private final Popup                popup      = new Popup();
    private final ObservableList<T>    allItems;
    private final FilteredList<T>      filtered;
    private final Function<T, String>  labelFn;
    private final Consumer<T>          onSelect;

    private final TextField  searchField = new TextField();
    private final ListView<T> listView   = new ListView<>();

    public SearchablePickerDialog(List<T> items,
                                  Function<T, String> labelFn,
                                  Consumer<T> onSelect) {
        this.allItems = FXCollections.observableArrayList(items);
        this.filtered = new FilteredList<>(this.allItems, p -> true);
        this.labelFn  = labelFn;
        this.onSelect = onSelect;
        build();
    }

    /** Replace the item list (e.g. after a refresh). */
    public void setItems(List<T> items) {
        allItems.setAll(items);
        searchField.clear();
        filtered.setPredicate(p -> true);
    }

    /** Show the popup anchored below the given node. */
    public void show(javafx.scene.Node anchor) {
        if (popup.isShowing()) { popup.hide(); return; }
        searchField.clear();
        filtered.setPredicate(p -> true);
        listView.scrollTo(0);

        Window window = anchor.getScene().getWindow();
        javafx.geometry.Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        popup.show(window, bounds.getMinX(), bounds.getMaxY() + 4);
        searchField.requestFocus();
    }

    public void hide() { popup.hide(); }

    // ── Build UI ──────────────────────────────────────────────

    private void build() {
        // Search field
        searchField.setPromptText("🔍  Type to search…");
        searchField.setStyle(
            "-fx-background-color:white; -fx-border-color:rgba(44,40,37,0.18);"
          + "-fx-border-radius:6 6 0 0; -fx-background-radius:6 6 0 0;"
          + "-fx-border-width:1; -fx-padding:9 12 9 12; -fx-font-size:13px;");
        searchField.textProperty().addListener((obs, old, query) -> {
            String q = query.trim().toLowerCase();
            filtered.setPredicate(item ->
                q.isEmpty() || labelFn.apply(item).toLowerCase().contains(q));
        });

        // List view
        listView.setItems(filtered);
        listView.setPrefHeight(220);
        listView.setPrefWidth(320);
        listView.setStyle(
            "-fx-background-color:white; -fx-border-color:rgba(44,40,37,0.18);"
          + "-fx-border-radius:0 0 8 8; -fx-background-radius:0 0 8 8;"
          + "-fx-border-width:0 1 1 1;");

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(labelFn.apply(item));
                    setStyle("-fx-padding:9 14 9 14; -fx-font-size:13px;");
                }
            }
        });

        listView.setOnMouseClicked(e -> {
            T selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                onSelect.accept(selected);
                popup.hide();
            }
        });

        // Keyboard: Enter selects, Escape closes
        listView.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> {
                    T sel = listView.getSelectionModel().getSelectedItem();
                    if (sel != null) { onSelect.accept(sel); popup.hide(); }
                }
                case ESCAPE -> popup.hide();
                default -> {}
            }
        });

        searchField.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case DOWN   -> listView.requestFocus();
                case ESCAPE -> popup.hide();
                case ENTER  -> {
                    if (!filtered.isEmpty()) {
                        onSelect.accept(filtered.get(0));
                        popup.hide();
                    }
                }
                default -> {}
            }
        });

        VBox container = new VBox(searchField, listView);
        container.setStyle(
            "-fx-effect:dropshadow(gaussian,rgba(44,40,37,0.18),14,0,0,4);"
          + "-fx-background-radius:8;");
        container.setPadding(Insets.EMPTY);

        popup.getContent().add(container);
        popup.setAutoHide(true);
    }
}