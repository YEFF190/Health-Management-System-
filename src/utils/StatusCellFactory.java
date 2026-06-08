package utils;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 * Renders appointment/record status as a coloured badge in TableView columns.
 */
public class StatusCellFactory {

    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> appointment() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(status);
                String color = switch (status.toLowerCase()) {
                    case "confirmed"  -> "#00b09b";
                    case "pending"    -> "#f0a030";
                    case "cancelled"  -> "#e05050";
                    case "completed"  -> "#3b82f6";
                    default           -> "#888888";
                };
                setStyle("-fx-text-fill:" + color + "; -fx-font-weight:bold; -fx-font-size:11px;");
            }
        };
    }

    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> record() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(status);
                String color = "resolved".equalsIgnoreCase(status) ? "#3b82f6" : "#00b09b";
                setStyle("-fx-text-fill:" + color + "; -fx-font-weight:bold; -fx-font-size:11px;");
            }
        };
    }
}
