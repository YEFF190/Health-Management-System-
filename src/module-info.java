module FXproj {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens application to javafx.graphics, javafx.fxml;
    opens controllers to javafx.graphics, javafx.fxml;
    opens models to javafx.base;
    opens views to javafx.fxml;
    opens utils to javafx.fxml;
    opens Services to javafx.fxml;
}