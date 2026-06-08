package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import models.*;
import Services.*;
import utils.*;

public class PatientController {

    // ── TabPane ──
    @FXML private TabPane mainTabPane;

    // ── Sidebar nav buttons ──
    @FXML private Button navMyInfo;
    @FXML private Button navAppointments;
    @FXML private Button navRecords;

    private static final int TAB_MY_INFO      = 0;
    private static final int TAB_APPOINTMENTS = 1;
    private static final int TAB_RECORDS      = 2;

    // ── Sidebar ──
    @FXML private Label welcomeLabel;
    @FXML private Label nextApptLabel;

    // ══ MY INFO TAB ══
    @FXML private Label infoNameLabel;
    @FXML private Label infoEmailLabel;
    @FXML private Label infoPhoneLabel;
    @FXML private Label infoGenderLabel;
    @FXML private Label infoDobLabel;
    @FXML private Label infoHeightLabel;
    @FXML private Label infoWeightLabel;

    @FXML private TextField  editNameField;
    @FXML private DatePicker editDobPicker;
    @FXML private Label      infoUpdateStatusLabel;

    @FXML private PasswordField currentPwField;
    @FXML private PasswordField newPwField;
    @FXML private PasswordField confirmPwField;
    @FXML private Label         pwStatusLabel;

    // ══ APPOINTMENTS TAB ══
    @FXML private TableView<Appointment>           appointmentsTable;
    @FXML private TableColumn<Appointment, String> colApptDoctor;
    @FXML private TableColumn<Appointment, String> colApptDate;
    @FXML private TableColumn<Appointment, String> colApptTime;
    @FXML private TableColumn<Appointment, String> colApptStatus;
    @FXML private TableColumn<Appointment, String> colApptReason;

    // ══ RECORDS TAB ══
    @FXML private TableView<MedicalRecord>           recordsTable;
    @FXML private TableColumn<MedicalRecord, String> colRecDiagnosis;
    @FXML private TableColumn<MedicalRecord, String> colRecPrescription;
    @FXML private TableColumn<MedicalRecord, String> colRecDoctor;
    @FXML private TableColumn<MedicalRecord, String> colRecDate;
    @FXML private TableColumn<MedicalRecord, String> colRecStatus;

    // ── Notification panel ──
    @FXML private NotificationController notifPanelController;

    private Patient currentPatient;

    // ────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        if (Session.getCurrentUser() == null) return;

        currentPatient = PatientService.getPatientByUserId(
            Session.getCurrentUser().getUserId());
        if (currentPatient == null) return;

        welcomeLabel.setText(currentPatient.getName());

        setupAppointmentTable();
        setupRecordsTable();
        loadAll();
        populateInfo();

        mainTabPane.getSelectionModel().selectedIndexProperty()
            .addListener((obs, old, idx) -> updateSidebarActive(idx.intValue()));

        ReminderService.startForPatient(currentPatient.getPatientId(),
            msg -> nextApptLabel.setText(msg));

        if (notifPanelController != null)
            notifPanelController.init(Session.getCurrentUser().getUserId());
    }

    // ══ SIDEBAR ══

    @FXML private void navToMyInfo()       { mainTabPane.getSelectionModel().select(TAB_MY_INFO); }
    @FXML private void navToAppointments() { mainTabPane.getSelectionModel().select(TAB_APPOINTMENTS); }
    @FXML private void navToRecords()      { mainTabPane.getSelectionModel().select(TAB_RECORDS); }

    private void updateSidebarActive(int index) {
        Button[] btns = {navMyInfo, navAppointments, navRecords};
        for (Button b : btns) {
            if (b == null) continue;
            b.getStyleClass().remove("nav-button-active");
            if (!b.getStyleClass().contains("nav-button")) b.getStyleClass().add("nav-button");
        }
        Button active = switch (index) {
            case TAB_MY_INFO      -> navMyInfo;
            case TAB_APPOINTMENTS -> navAppointments;
            case TAB_RECORDS      -> navRecords;
            default               -> null;
        };
        if (active != null) {
            active.getStyleClass().remove("nav-button");
            if (!active.getStyleClass().contains("nav-button-active"))
                active.getStyleClass().add("nav-button-active");
        }
    }

    // ══ TABLE SETUP ══

    private void setupAppointmentTable() {
        colApptDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        colApptDate.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getAppointmentDate().toString()));
        colApptTime.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getAppointmentTime().toString()));
        colApptStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colApptStatus.setCellFactory(StatusCellFactory.appointment());
        colApptReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
    }

    private void setupRecordsTable() {
        colRecDiagnosis.setCellValueFactory(new PropertyValueFactory<>("diagnosis"));
        colRecPrescription.setCellValueFactory(new PropertyValueFactory<>("prescription"));
        colRecDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        colRecDate.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getAppointmentDate() != null
                    ? d.getValue().getAppointmentDate().toString() : ""));
        colRecStatus.setCellValueFactory(new PropertyValueFactory<>("recordStatus"));
        colRecStatus.setCellFactory(StatusCellFactory.record());
    }

    // ══ DATA LOADING ══

    private void loadAll() {
        loadAppointments();
        loadRecords();
        updateNextAppt();
    }

    private void loadAppointments() {
        appointmentsTable.setItems(FXCollections.observableArrayList(
            AppointmentService.getByPatient(currentPatient.getPatientId())));
    }

    private void loadRecords() {
        recordsTable.setItems(FXCollections.observableArrayList(
            MedicalRecordService.getByPatient(currentPatient.getPatientId())));
    }

    private void updateNextAppt() {
        AppointmentService.getByPatient(currentPatient.getPatientId()).stream()
            .filter(a -> "confirmed".equals(a.getStatus())
                      && !a.getAppointmentDate().isBefore(java.time.LocalDate.now()))
            .findFirst()
            .ifPresentOrElse(
                a -> nextApptLabel.setText("Next: " + a.getAppointmentDate()
                        + " at " + a.getAppointmentTime()
                        + " — Dr. " + a.getDoctorName()),
                () -> nextApptLabel.setText("No upcoming appointments")
            );
    }

    private void populateInfo() {
        infoNameLabel.setText(currentPatient.getName());
        infoEmailLabel.setText(currentPatient.getEmail());
        infoPhoneLabel.setText(currentPatient.getPhone() != null
            ? currentPatient.getPhone() : "—");
        infoGenderLabel.setText(currentPatient.getGender() != null
            ? currentPatient.getGender() : "—");
        infoDobLabel.setText(currentPatient.getDob() != null
            ? currentPatient.getDob().toString() : "—");
        infoHeightLabel.setText(currentPatient.getHeight() != null
            ? currentPatient.getHeight() + " cm" : "—");
        infoWeightLabel.setText(currentPatient.getWeight() != null
            ? currentPatient.getWeight() + " kg" : "—");

        editNameField.setText(currentPatient.getName());
        editDobPicker.setValue(currentPatient.getDob());
        infoUpdateStatusLabel.setText("");
    }

    // ══ SELF-UPDATE ACTIONS ══

    @FXML
    private void handleUpdateInfo() {
        infoUpdateStatusLabel.setStyle("-fx-text-fill: red;");
        if (editNameField.getText().trim().isEmpty()) {
            infoUpdateStatusLabel.setText("Name is required."); return;
        }
        boolean ok = PatientService.selfUpdate(
            Session.getCurrentUser().getUserId(),
            currentPatient.getPatientId(),
            editNameField.getText().trim(),
            editDobPicker.getValue()
        );
        if (ok) {
            currentPatient = PatientService.getPatientByUserId(
                Session.getCurrentUser().getUserId());
            User refreshed = UserService.getById(Session.getCurrentUser().getUserId());
            if (refreshed != null) Session.setCurrentUser(refreshed);

            infoUpdateStatusLabel.setStyle("-fx-text-fill: #00b09b;");
            infoUpdateStatusLabel.setText("Information updated successfully.");
            welcomeLabel.setText(currentPatient.getName());
            populateInfo();
        } else infoUpdateStatusLabel.setText("Update failed.");
    }

    @FXML
    private void handleChangePassword() {
        pwStatusLabel.setStyle("-fx-text-fill: red;");
        if (currentPwField.getText().isEmpty() || newPwField.getText().isEmpty()) {
            pwStatusLabel.setText("Current and new password are required."); return;
        }
        if (!newPwField.getText().equals(confirmPwField.getText())) {
            pwStatusLabel.setText("New passwords do not match."); return;
        }
        if (newPwField.getText().length() < 6) {
            pwStatusLabel.setText("Password must be at least 6 characters."); return;
        }
        boolean ok = UserService.changePassword(
            Session.getCurrentUser().getUserId(),
            currentPwField.getText(),
            newPwField.getText()
        );
        if (ok) {
            pwStatusLabel.setStyle("-fx-text-fill: #00b09b;");
            pwStatusLabel.setText("Password changed successfully.");
            currentPwField.clear(); newPwField.clear(); confirmPwField.clear();
        } else pwStatusLabel.setText("Current password is incorrect.");
    }

    @FXML
    private void handleCancelAppointment() {
        Appointment sel = appointmentsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            warn("Select an appointment first."); return;
        }
        if ("cancelled".equals(sel.getStatus()) || "completed".equals(sel.getStatus())) {
            warn("This appointment cannot be cancelled."); return;
        }
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION,
            "Cancel your appointment with Dr. " + sel.getDoctorName()
            + " on " + sel.getAppointmentDate() + "?",
            ButtonType.YES, ButtonType.NO);
        dlg.setHeaderText("Confirm Cancellation");
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                if (AppointmentService.cancel(sel.getAppointmentId())) loadAll();
                else warn("Could not cancel appointment.");
            }
        });
    }

    // ══ GENERAL ══

    @FXML private void handleRefresh() { loadAll(); populateInfo(); }

    @FXML
    private void handleLogout() {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION,
            "Are you sure you want to log out?", ButtonType.YES, ButtonType.NO);
        dlg.setHeaderText("Confirm Logout");
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                ReminderService.stop();
                Session.logout();
                SceneSwitcher.switchScene("/views/login.fxml", "Login — HealthAssist");
            }
        });
    }

    private void warn(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }
}