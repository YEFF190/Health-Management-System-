package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import models.*;
import Services.*;
import utils.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class DoctorController {

    // ── TabPane ──
    @FXML private TabPane mainTabPane;

    // ── Sidebar nav buttons ──
    @FXML private Button navAppointments;
    @FXML private Button navRecords;
    @FXML private Button navPatients;
    @FXML private Button navProfile;

    private static final int TAB_APPOINTMENTS = 0;
    private static final int TAB_RECORDS      = 1;
    private static final int TAB_PATIENTS     = 2;
    private static final int TAB_PROFILE      = 3;

    // ── Sidebar ──
    @FXML private Label welcomeLabel;
    @FXML private Label specialtyLabel;
    @FXML private Label totalTodayLabel;

    // ══ APPOINTMENTS TAB ══
    @FXML private TableView<Appointment>           appointmentsTable;
    @FXML private TableColumn<Appointment, String> colApptPatient;
    @FXML private TableColumn<Appointment, String> colApptDate;
    @FXML private TableColumn<Appointment, String> colApptTime;
    @FXML private TableColumn<Appointment, String> colApptReason;
    @FXML private TableColumn<Appointment, String> colApptStatus;

    // Book new appointment — searchable picker
    @FXML private Button    bookPatientPickerBtn;
    @FXML private DatePicker bookDatePicker;
    @FXML private TextField  bookTimeField;
    @FXML private TextField  bookReasonField;
    @FXML private Label      bookStatusLabel;

    // ══ MEDICAL RECORDS TAB ══
    @FXML private TableView<MedicalRecord>           recordsTable;
    @FXML private TableColumn<MedicalRecord, String> colRecPatient;
    @FXML private TableColumn<MedicalRecord, String> colRecDiagnosis;
    @FXML private TableColumn<MedicalRecord, String> colRecPrescription;
    @FXML private TableColumn<MedicalRecord, String> colRecDate;
    @FXML private TableColumn<MedicalRecord, String> colRecStatus;

    @FXML private Label    selectedApptLabel;
    @FXML private TextField diagnosisField;
    @FXML private TextArea  prescriptionArea;
    @FXML private Label     addRecordStatusLabel;

    @FXML private TextField        editDiagnosisField;
    @FXML private TextArea         editPrescriptionArea;
    @FXML private ComboBox<String> editRecordStatusCombo;
    @FXML private Label            editRecordStatusLabel;

    // ══ PATIENTS TAB ══
    @FXML private TableView<Patient>             patientsTable;
    @FXML private TableColumn<Patient, String>   colPatName;
    @FXML private TableColumn<Patient, String>   colPatGender;
    @FXML private TableColumn<Patient, String>   colPatDob;
    @FXML private TableColumn<Patient, String>   colPatHeight;
    @FXML private TableColumn<Patient, String>   colPatWeight;
    @FXML private TableColumn<Patient, String>   colPatPhone;

    @FXML private TextField        editPatNameField;
    @FXML private TextField        editPatPhoneField;
    @FXML private TextField        editPatHeightField;
    @FXML private TextField        editPatWeightField;
    @FXML private ComboBox<String> editPatGenderCombo;
    @FXML private DatePicker       editPatDobPicker;
    @FXML private Label            editPatStatusLabel;

    // ══ PROFILE TAB ══
    @FXML private Label     profileNameLabel;
    @FXML private Label     profileEmailLabel;
    @FXML private Label     profilePhoneLabel;
    @FXML private Label     profileSpecialtyLabel;

    @FXML private TextField        editProfileNameField;
    @FXML private TextField        editProfilePhoneField;
    @FXML private ComboBox<String> editProfileSpecialtyCombo;  // ← was TextField
    @FXML private Label            profileStatusLabel;

    @FXML private PasswordField currentPwField;
    @FXML private PasswordField newPwField;
    @FXML private PasswordField confirmPwField;
    @FXML private Label         pwStatusLabel;

    // ── Notification panel ──
    @FXML private NotificationController notifPanelController;

    // ── State ──
    private Doctor  currentDoctor   = null;
    private Patient selectedPatient = null;

    private SearchablePickerDialog<Patient> patientPicker;

    // ────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        if (Session.getCurrentUser() == null) return;

        currentDoctor = DoctorService.getDoctorByUserId(Session.getCurrentUser().getUserId());
        if (currentDoctor == null) return;

        welcomeLabel.setText("Dr. " + currentDoctor.getName());
        specialtyLabel.setText(currentDoctor.getSpecialty());

        setupAppointmentTable();
        setupRecordsTable();
        setupPatientTable();
        setupCombos();
        loadAll();
        populateProfile();
        initPatientPicker();

        mainTabPane.getSelectionModel().selectedIndexProperty()
            .addListener((obs, old, idx) -> updateSidebarActive(idx.intValue()));

        ReminderService.startForDoctor(currentDoctor.getDoctorId(),
            msg -> specialtyLabel.setText(currentDoctor.getSpecialty() + "  |  " + msg));

        if (notifPanelController != null)
            notifPanelController.init(Session.getCurrentUser().getUserId());
    }

    // ══ SIDEBAR ══

    @FXML private void navToAppointments() { mainTabPane.getSelectionModel().select(TAB_APPOINTMENTS); }
    @FXML private void navToRecords()      { mainTabPane.getSelectionModel().select(TAB_RECORDS); }
    @FXML private void navToPatients()     { mainTabPane.getSelectionModel().select(TAB_PATIENTS); }
    @FXML private void navToProfile()      { mainTabPane.getSelectionModel().select(TAB_PROFILE); }

    private void updateSidebarActive(int index) {
        Button[] btns = {navAppointments, navRecords, navPatients, navProfile};
        for (Button b : btns) {
            if (b == null) continue;
            b.getStyleClass().remove("nav-button-active");
            if (!b.getStyleClass().contains("nav-button")) b.getStyleClass().add("nav-button");
        }
        Button active = switch (index) {
            case TAB_APPOINTMENTS -> navAppointments;
            case TAB_RECORDS      -> navRecords;
            case TAB_PATIENTS     -> navPatients;
            case TAB_PROFILE      -> navProfile;
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
        colApptPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colApptDate.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getAppointmentDate().toString()));
        colApptTime.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getAppointmentTime().toString()));
        colApptReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colApptStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colApptStatus.setCellFactory(StatusCellFactory.appointment());

        appointmentsTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, sel) -> {
                if (sel != null) {
                    selectedApptLabel.setText("Creating record for: "
                        + sel.getPatientName() + " on " + sel.getAppointmentDate());
                    addRecordStatusLabel.setText("");
                }
            });
    }

    private void setupRecordsTable() {
        colRecPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colRecDiagnosis.setCellValueFactory(new PropertyValueFactory<>("diagnosis"));
        colRecPrescription.setCellValueFactory(new PropertyValueFactory<>("prescription"));
        colRecDate.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getAppointmentDate() != null
                    ? d.getValue().getAppointmentDate().toString() : ""));
        colRecStatus.setCellValueFactory(new PropertyValueFactory<>("recordStatus"));
        colRecStatus.setCellFactory(StatusCellFactory.record());

        recordsTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, sel) -> {
                if (sel != null) {
                    editDiagnosisField.setText(sel.getDiagnosis());
                    editPrescriptionArea.setText(
                        sel.getPrescription() != null ? sel.getPrescription() : "");
                    editRecordStatusCombo.setValue(sel.getRecordStatus());
                    editRecordStatusLabel.setText("");
                }
            });
    }

    private void setupPatientTable() {
        colPatName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPatGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        colPatDob.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getDob() != null ? d.getValue().getDob().toString() : ""));
        colPatHeight.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getHeight() != null ? d.getValue().getHeight() + " cm" : ""));
        colPatWeight.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getWeight() != null ? d.getValue().getWeight() + " kg" : ""));
        colPatPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        patientsTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, sel) -> {
                if (sel != null) {
                    editPatNameField.setText(sel.getName());
                    editPatPhoneField.setText(sel.getPhone() != null ? sel.getPhone() : "");
                    editPatHeightField.setText(
                        sel.getHeight() != null ? String.valueOf(sel.getHeight()) : "");
                    editPatWeightField.setText(
                        sel.getWeight() != null ? String.valueOf(sel.getWeight()) : "");
                    editPatGenderCombo.setValue(sel.getGender());
                    editPatDobPicker.setValue(sel.getDob());
                    editPatStatusLabel.setText("");
                }
            });
    }

    private void setupCombos() {
        editPatGenderCombo.setItems(
            FXCollections.observableArrayList("male", "female", "other"));
        editRecordStatusCombo.setItems(
            FXCollections.observableArrayList("active", "resolved"));

        // Specialty combo for profile edit — from this group's specialty list
        int groupId = Session.getCurrentUser().getGroupId();
        editProfileSpecialtyCombo.setItems(
            FXCollections.observableArrayList(SpecialtyService.getNames(groupId)));
    }

    // ══ SEARCHABLE PATIENT PICKER ══

    private void initPatientPicker() {
        int groupId = Session.getCurrentUser().getGroupId();
        List<Patient> patients = PatientService.getAllPatients(groupId);

        patientPicker = new SearchablePickerDialog<>(
            patients,
            Patient::getName,
            p -> {
                selectedPatient = p;
                bookPatientPickerBtn.setText(p.getName());
                bookPatientPickerBtn.setStyle(
                    bookPatientPickerBtn.getStyle() + "-fx-text-fill:#1a3d2e;");
            }
        );
    }

    @FXML
    private void showPatientPicker() {
        patientPicker.show(bookPatientPickerBtn);
    }

    // ══ DATA LOADING ══

    private void loadAll() {
        loadAppointments();
        loadRecords();
        loadPatients();
        updateTodayCount();
        refreshSpecialtyCombo();
    }

    private void loadAppointments() {
        appointmentsTable.setItems(FXCollections.observableArrayList(
            AppointmentService.getByDoctor(currentDoctor.getDoctorId())));
    }

    private void loadRecords() {
        recordsTable.setItems(FXCollections.observableArrayList(
            MedicalRecordService.getByDoctor(currentDoctor.getDoctorId())));
    }

    private void loadPatients() {
        List<Patient> patients =
            PatientService.getPatientsByDoctor(currentDoctor.getDoctorId());
        patientsTable.setItems(FXCollections.observableArrayList(patients));
    }

    private void updateTodayCount() {
        long count = AppointmentService.getByDoctor(currentDoctor.getDoctorId()).stream()
            .filter(a -> a.getAppointmentDate().equals(LocalDate.now()))
            .count();
        totalTodayLabel.setText(count + " appointment(s) today");
    }

    private void refreshSpecialtyCombo() {
        int groupId = Session.getCurrentUser().getGroupId();
        editProfileSpecialtyCombo.setItems(
            FXCollections.observableArrayList(SpecialtyService.getNames(groupId)));
        editProfileSpecialtyCombo.setValue(currentDoctor.getSpecialty());

        // Also refresh patient picker list
        if (patientPicker != null)
            patientPicker.setItems(PatientService.getAllPatients(groupId));
    }

    private void populateProfile() {
        profileNameLabel.setText(currentDoctor.getName());
        profileEmailLabel.setText(currentDoctor.getEmail());
        profilePhoneLabel.setText(
            currentDoctor.getPhone() != null ? currentDoctor.getPhone() : "—");
        profileSpecialtyLabel.setText(currentDoctor.getSpecialty());
        editProfileNameField.setText(currentDoctor.getName());
        editProfilePhoneField.setText(
            currentDoctor.getPhone() != null ? currentDoctor.getPhone() : "");
        editProfileSpecialtyCombo.setValue(currentDoctor.getSpecialty());
        profileStatusLabel.setText("");
    }

    // ══ APPOINTMENT ACTIONS ══

    @FXML
    private void handleBookAppointment() {
        bookStatusLabel.setStyle("-fx-text-fill: red;");
        if (selectedPatient == null) {
            bookStatusLabel.setText("Select a patient."); return;
        }
        if (bookDatePicker.getValue() == null) {
            bookStatusLabel.setText("Select a date."); return;
        }
        if (bookDatePicker.getValue().isBefore(LocalDate.now())) {
            bookStatusLabel.setText("Cannot book in the past."); return;
        }
        if (bookReasonField.getText().trim().isEmpty()) {
            bookStatusLabel.setText("Reason is required."); return;
        }
        if (bookReasonField.getText().length() > 500) {
            bookStatusLabel.setText("Reason must be under 500 characters."); return;
        }

        LocalTime time;
        try {
            time = LocalTime.parse(bookTimeField.getText().trim(),
                    DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            bookStatusLabel.setText("Enter time as HH:mm (e.g. 09:30)."); return;
        }

        Appointment appt = new Appointment();
        appt.setDoctorId(currentDoctor.getDoctorId());
        appt.setPatientId(selectedPatient.getPatientId());
        appt.setAppointmentDate(bookDatePicker.getValue());
        appt.setAppointmentTime(time);
        appt.setReason(bookReasonField.getText().trim());

        if (AppointmentService.book(appt)) {
            bookStatusLabel.setStyle("-fx-text-fill: #00b09b;");
            bookStatusLabel.setText("Appointment booked successfully.");
            selectedPatient = null;
            bookPatientPickerBtn.setText("Select patient…");
            bookDatePicker.setValue(null);
            bookTimeField.clear();
            bookReasonField.clear();
            loadAll();
        } else {
            bookStatusLabel.setText("That time slot is already booked.");
        }
    }

    @FXML
    private void handleConfirmAppointment() {
        Appointment sel = appointmentsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { warn("Select an appointment first."); return; }
        if (!"pending".equals(sel.getStatus())) {
            warn("Only pending appointments can be confirmed."); return;
        }
        if (AppointmentService.confirm(sel.getAppointmentId())) loadAll();
        else warn("Could not confirm appointment.");
    }

    @FXML
    private void handleCancelAppointment() {
        Appointment sel = appointmentsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { warn("Select an appointment first."); return; }
        if ("cancelled".equals(sel.getStatus()) || "completed".equals(sel.getStatus())) {
            warn("This appointment cannot be cancelled."); return;
        }
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION,
            "Cancel appointment with " + sel.getPatientName()
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

    // ══ RECORD ACTIONS ══

    @FXML
    private void handleAddRecord() {
        addRecordStatusLabel.setStyle("-fx-text-fill: red;");
        Appointment sel = appointmentsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            addRecordStatusLabel.setText("Select an appointment first."); return;
        }
        if (diagnosisField.getText().trim().isEmpty()) {
            addRecordStatusLabel.setText("Diagnosis is required."); return;
        }
        if (diagnosisField.getText().length() > 500) {
            addRecordStatusLabel.setText("Diagnosis must be under 500 characters."); return;
        }

        MedicalRecord record = new MedicalRecord();
        record.setAppointmentId(sel.getAppointmentId());
        record.setDiagnosis(diagnosisField.getText().trim());
        record.setPrescription(prescriptionArea.getText().trim());
        record.setRecordStatus("active");

        if (MedicalRecordService.add(record)) {
            AppointmentService.complete(sel.getAppointmentId());
            addRecordStatusLabel.setStyle("-fx-text-fill: #00b09b;");
            addRecordStatusLabel.setText("Record saved. Appointment marked completed.");
            diagnosisField.clear();
            prescriptionArea.clear();
            selectedApptLabel.setText("Select an appointment from the table above");
            loadAll();
        } else {
            addRecordStatusLabel.setText(
                "Failed. A record may already exist for this appointment.");
        }
    }

    @FXML
    private void handleUpdateRecord() {
        editRecordStatusLabel.setStyle("-fx-text-fill: red;");
        MedicalRecord sel = recordsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            editRecordStatusLabel.setText("Select a record first."); return;
        }
        if (editDiagnosisField.getText().trim().isEmpty()) {
            editRecordStatusLabel.setText("Diagnosis is required."); return;
        }

        sel.setDiagnosis(editDiagnosisField.getText().trim());
        sel.setPrescription(editPrescriptionArea.getText().trim());
        sel.setRecordStatus(editRecordStatusCombo.getValue());

        if (MedicalRecordService.update(sel)) {
            editRecordStatusLabel.setStyle("-fx-text-fill: #00b09b;");
            editRecordStatusLabel.setText("Record updated.");
            loadRecords();
        } else editRecordStatusLabel.setText("Update failed.");
    }

    // ══ PATIENT ACTIONS ══

    @FXML
    private void handleUpdatePatient() {
        editPatStatusLabel.setStyle("-fx-text-fill: red;");
        Patient sel = patientsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { editPatStatusLabel.setText("Select a patient first."); return; }
        if (editPatNameField.getText().trim().isEmpty()) {
            editPatStatusLabel.setText("Name is required."); return;
        }
        try {
            sel.setName(editPatNameField.getText().trim());
            sel.setPhone(editPatPhoneField.getText().trim());
            sel.setGender(editPatGenderCombo.getValue());
            sel.setDob(editPatDobPicker.getValue());
            if (!editPatHeightField.getText().trim().isEmpty())
                sel.setHeight(Double.parseDouble(editPatHeightField.getText().trim()));
            if (!editPatWeightField.getText().trim().isEmpty())
                sel.setWeight(Double.parseDouble(editPatWeightField.getText().trim()));
        } catch (NumberFormatException e) {
            editPatStatusLabel.setText("Height and weight must be numbers."); return;
        }
        if (PatientService.update(sel)) {
            editPatStatusLabel.setStyle("-fx-text-fill: #00b09b;");
            editPatStatusLabel.setText("Patient updated.");
            loadPatients();
        } else editPatStatusLabel.setText("Update failed.");
    }

    // ══ PROFILE ACTIONS ══

    @FXML
    private void handleUpdateProfile() {
        profileStatusLabel.setStyle("-fx-text-fill: red;");
        if (editProfileNameField.getText().trim().isEmpty()
                || editProfileSpecialtyCombo.getValue() == null) {
            profileStatusLabel.setText("Name and specialty are required."); return;
        }
        currentDoctor.setName(editProfileNameField.getText().trim());
        currentDoctor.setPhone(editProfilePhoneField.getText().trim());
        currentDoctor.setSpecialty(editProfileSpecialtyCombo.getValue());

        if (DoctorService.updateDoctor(currentDoctor)) {
            profileStatusLabel.setStyle("-fx-text-fill: #00b09b;");
            profileStatusLabel.setText("Profile updated.");
            welcomeLabel.setText("Dr. " + currentDoctor.getName());
            specialtyLabel.setText(currentDoctor.getSpecialty());
            populateProfile();
        } else profileStatusLabel.setText("Update failed.");
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
            pwStatusLabel.setText("New password must be at least 6 characters."); return;
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

    // ══ GENERAL ══

    @FXML private void handleRefresh() { loadAll(); }

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