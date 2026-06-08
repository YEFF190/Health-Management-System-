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

public class AdminController {

    // ── TabPane ──
    @FXML private TabPane mainTabPane;

    // ── Sidebar nav buttons ──
    @FXML private Button navAdmins;
    @FXML private Button navDoctors;
    @FXML private Button navPatients;
    @FXML private Button navAppointments;
    @FXML private Button navRecords;
    @FXML private Button navSpecialties;
    @FXML private Button navMyInfo;

    private static final int TAB_ADMINS       = 0;
    private static final int TAB_DOCTORS      = 1;
    private static final int TAB_PATIENTS     = 2;
    private static final int TAB_APPOINTMENTS = 3;
    private static final int TAB_RECORDS      = 4;
    private static final int TAB_SPECIALTIES  = 5;
    private static final int TAB_MY_INFO      = 6;

    // ── Topbar ──
    @FXML private Label welcomeLabel;
    @FXML private Label reminderLabel;

    // ── Stats ──
    @FXML private Label totalPatientsLabel;
    @FXML private Label totalDoctorsLabel;
    @FXML private Label totalAppointmentsLabel;
    @FXML private Label openRecordsLabel;

    // ══ ADMINS TAB ══
    @FXML private TableView<User>           adminsTable;
    @FXML private TableColumn<User,String>  colAdminName;
    @FXML private TableColumn<User,String>  colAdminEmail;
    @FXML private TableColumn<User,String>  colAdminPhone;

    @FXML private TextField     newAdminName;
    @FXML private TextField     newAdminEmail;
    @FXML private TextField     newAdminPhone;
    @FXML private PasswordField newAdminPassword;
    @FXML private Label         adminRegStatusLabel;

    // ══ DOCTORS TAB ══
    @FXML private TableView<Doctor>           doctorsTable;
    @FXML private TableColumn<Doctor,String>  colDocName;
    @FXML private TableColumn<Doctor,String>  colDocEmail;
    @FXML private TableColumn<Doctor,String>  colDocPhone;
    @FXML private TableColumn<Doctor,String>  colDocSpecialty;

    @FXML private TextField  docNameField;
    @FXML private TextField  docEmailField;
    @FXML private TextField  docPhoneField;
    @FXML private ComboBox<String> docSpecialtyCombo;   // ← was TextField
    @FXML private PasswordField docPasswordField;
    @FXML private Label      docStatusLabel;

    @FXML private TextField  editDocNameField;
    @FXML private TextField  editDocPhoneField;
    @FXML private ComboBox<String> editDocSpecialtyCombo; // ← was TextField
    @FXML private Label      editDocStatusLabel;

    // ══ PATIENTS TAB ══
    @FXML private TableView<Patient>            patientsTable;
    @FXML private TableColumn<Patient,String>   colPatName;
    @FXML private TableColumn<Patient,String>   colPatEmail;
    @FXML private TableColumn<Patient,String>   colPatPhone;
    @FXML private TableColumn<Patient,String>   colPatGender;
    @FXML private TableColumn<Patient,String>   colPatDob;

    @FXML private TextField     patNameField;
    @FXML private TextField     patEmailField;
    @FXML private TextField     patPhoneField;
    @FXML private ComboBox<String> patGenderCombo;
    @FXML private DatePicker    patDobPicker;
    @FXML private TextField     patHeightField;
    @FXML private TextField     patWeightField;
    @FXML private PasswordField patPasswordField;
    @FXML private Label         patStatusLabel;

    @FXML private TextField     editPatNameField;
    @FXML private TextField     editPatPhoneField;
    @FXML private TextField     editPatHeightField;
    @FXML private TextField     editPatWeightField;
    @FXML private ComboBox<String> editPatGenderCombo;
    @FXML private DatePicker    editPatDobPicker;
    @FXML private Label         editPatStatusLabel;

    // ══ APPOINTMENTS TAB ══
    @FXML private TableView<Appointment>            appointmentsTable;
    @FXML private TableColumn<Appointment,String>   colApptPatient;
    @FXML private TableColumn<Appointment,String>   colApptDoctor;
    @FXML private TableColumn<Appointment,String>   colApptDate;
    @FXML private TableColumn<Appointment,String>   colApptTime;
    @FXML private TableColumn<Appointment,String>   colApptStatus;
    @FXML private TableColumn<Appointment,String>   colApptReason;

    // Searchable picker trigger buttons (replace ComboBoxes)
    @FXML private Button apptDoctorPickerBtn;
    @FXML private Button apptPatientPickerBtn;
    @FXML private DatePicker apptDatePicker;
    @FXML private TextField  apptTimeField;
    @FXML private TextField  apptReasonField;
    @FXML private Label      apptStatusLabel;

    // Archive controls
    @FXML private Label archiveStatusLabel;

    // ══ RECORDS TAB ══
    @FXML private TableView<MedicalRecord>            recordsTable;
    @FXML private TableColumn<MedicalRecord,String>   colRecPatient;
    @FXML private TableColumn<MedicalRecord,String>   colRecDoctor;
    @FXML private TableColumn<MedicalRecord,String>   colRecDiagnosis;
    @FXML private TableColumn<MedicalRecord,String>   colRecDate;
    @FXML private TableColumn<MedicalRecord,String>   colRecStatus;

    // ══ SPECIALTIES TAB ══
    @FXML private TableView<Specialty>            specialtiesTable;
    @FXML private TableColumn<Specialty,String>   colSpecName;
    @FXML private TextField                       newSpecialtyField;
    @FXML private Label                           specialtyStatusLabel;

    // ══ MY INFO TAB ══
    @FXML private Label     infoNameLabel;
    @FXML private Label     infoEmailLabel;
    @FXML private Label     infoPhoneLabel;
    @FXML private Label     infoRoleLabel;
    @FXML private TextField editInfoNameField;
    @FXML private TextField editInfoPhoneField;
    @FXML private Label     infoUpdateStatusLabel;
    @FXML private PasswordField currentPwField;
    @FXML private PasswordField newPwField;
    @FXML private PasswordField confirmPwField;
    @FXML private Label         pwStatusLabel;

    // ── Notification panel ──
    @FXML private NotificationController notifPanelController;

    // ── Searchable pickers (stateful) ──
    private SearchablePickerDialog<Doctor>  doctorPicker;
    private SearchablePickerDialog<Patient> patientPicker;
    private Doctor  selectedDoctor  = null;
    private Patient selectedPatient = null;

    // ────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        if (Session.getCurrentUser() != null)
            welcomeLabel.setText("Welcome, " + Session.getCurrentUser().getName());

        setupAdminsTable();
        setupDoctorTable();
        setupPatientTable();
        setupAppointmentTable();
        setupRecordsTable();
        setupSpecialtiesTable();
        setupGenderCombos();
        loadStats();
        loadAllData();
        populateMyInfo();
        initSearchablePickers();

        mainTabPane.getSelectionModel().selectedIndexProperty()
            .addListener((obs, old, idx) -> updateSidebarActive(idx.intValue()));

        int gid = Session.getCurrentUser().getGroupId();
        ReminderService.start(gid, msg -> { reminderLabel.setText(msg); loadStats(); });
        if (notifPanelController != null)
            notifPanelController.init(Session.getCurrentUser().getUserId());
    }

    // ══ SIDEBAR ══

    @FXML private void navToAdmins()       { selectTab(TAB_ADMINS); }
    @FXML private void navToDoctors()      { selectTab(TAB_DOCTORS); }
    @FXML private void navToPatients()     { selectTab(TAB_PATIENTS); }
    @FXML private void navToAppointments() { selectTab(TAB_APPOINTMENTS); }
    @FXML private void navToRecords()      { selectTab(TAB_RECORDS); }
    @FXML private void navToSpecialties()  { selectTab(TAB_SPECIALTIES); }
    @FXML private void navToMyInfo()       { selectTab(TAB_MY_INFO); }

    private void selectTab(int index) {
        mainTabPane.getSelectionModel().select(index);
    }

    private void updateSidebarActive(int index) {
        Button[] btns = {navAdmins, navDoctors, navPatients,
                         navAppointments, navRecords, navSpecialties, navMyInfo};
        for (Button b : btns) {
            if (b == null) continue;
            b.getStyleClass().remove("nav-button-active");
            if (!b.getStyleClass().contains("nav-button")) b.getStyleClass().add("nav-button");
        }
        Button active = switch (index) {
            case TAB_ADMINS       -> navAdmins;
            case TAB_DOCTORS      -> navDoctors;
            case TAB_PATIENTS     -> navPatients;
            case TAB_APPOINTMENTS -> navAppointments;
            case TAB_RECORDS      -> navRecords;
            case TAB_SPECIALTIES  -> navSpecialties;
            case TAB_MY_INFO      -> navMyInfo;
            default               -> null;
        };
        if (active != null) {
            active.getStyleClass().remove("nav-button");
            if (!active.getStyleClass().contains("nav-button-active"))
                active.getStyleClass().add("nav-button-active");
        }
    }

    // ══ TABLE SETUP ══

    private void setupAdminsTable() {
        colAdminName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAdminEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colAdminPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
    }

    private void setupDoctorTable() {
        colDocName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDocEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colDocPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colDocSpecialty.setCellValueFactory(new PropertyValueFactory<>("specialty"));

        doctorsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                editDocNameField.setText(sel.getName());
                editDocPhoneField.setText(sel.getPhone() != null ? sel.getPhone() : "");
                editDocSpecialtyCombo.setValue(sel.getSpecialty());
                editDocStatusLabel.setText("");
            }
        });
    }

    private void setupPatientTable() {
        colPatName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPatEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPatPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colPatGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        colPatDob.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getDob() != null ? d.getValue().getDob().toString() : ""));

        patientsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                editPatNameField.setText(sel.getName());
                editPatPhoneField.setText(sel.getPhone() != null ? sel.getPhone() : "");
                editPatHeightField.setText(sel.getHeight() != null ? String.valueOf(sel.getHeight()) : "");
                editPatWeightField.setText(sel.getWeight() != null ? String.valueOf(sel.getWeight()) : "");
                editPatGenderCombo.setValue(sel.getGender());
                editPatDobPicker.setValue(sel.getDob());
                editPatStatusLabel.setText("");
            }
        });
    }

    private void setupAppointmentTable() {
        colApptPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
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
        colRecPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colRecDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        colRecDiagnosis.setCellValueFactory(new PropertyValueFactory<>("diagnosis"));
        colRecDate.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getAppointmentDate() != null
                    ? d.getValue().getAppointmentDate().toString() : ""));
        colRecStatus.setCellValueFactory(new PropertyValueFactory<>("recordStatus"));
        colRecStatus.setCellFactory(StatusCellFactory.record());
    }

    private void setupSpecialtiesTable() {
        colSpecName.setCellValueFactory(new PropertyValueFactory<>("name"));
    }

    private void setupGenderCombos() {
        var genders = FXCollections.observableArrayList("male", "female", "other");
        patGenderCombo.setItems(genders);
        editPatGenderCombo.setItems(genders);
    }

    // ══ SEARCHABLE PICKERS ══

    private void initSearchablePickers() {
        List<Doctor>  doctors  = DoctorService.getAllDoctors(groupId());
        List<Patient> patients = PatientService.getAllPatients(groupId());

        doctorPicker = new SearchablePickerDialog<>(
            doctors,
            d -> "Dr. " + d.getName() + " — " + d.getSpecialty(),
            d -> {
                selectedDoctor = d;
                apptDoctorPickerBtn.setText("Dr. " + d.getName() + " — " + d.getSpecialty());
                apptDoctorPickerBtn.setStyle(apptDoctorPickerBtn.getStyle()
                    + "-fx-text-fill:#1a3d2e;");
            }
        );

        patientPicker = new SearchablePickerDialog<>(
            patients,
            p -> p.getName(),
            p -> {
                selectedPatient = p;
                apptPatientPickerBtn.setText(p.getName());
                apptPatientPickerBtn.setStyle(apptPatientPickerBtn.getStyle()
                    + "-fx-text-fill:#1a3d2e;");
            }
        );
    }

    @FXML private void showDoctorPicker()  { doctorPicker.show(apptDoctorPickerBtn); }
    @FXML private void showPatientPicker() { patientPicker.show(apptPatientPickerBtn); }

    // ══ DATA LOADING ══

    private int groupId() { return Session.getCurrentUser().getGroupId(); }

    private void loadStats() {
        totalPatientsLabel.setText(String.valueOf(PatientService.getAllPatients(groupId()).size()));
        totalDoctorsLabel.setText(String.valueOf(DoctorService.getAllDoctors(groupId()).size()));
        totalAppointmentsLabel.setText(String.valueOf(AppointmentService.getAll(groupId()).size()));
        long open = MedicalRecordService.getAll(groupId()).stream()
                        .filter(r -> "active".equals(r.getRecordStatus())).count();
        openRecordsLabel.setText(String.valueOf(open));
    }

    private void loadAllData() {
        List<Doctor>  doctors  = DoctorService.getAllDoctors(groupId());
        List<Patient> patients = PatientService.getAllPatients(groupId());
        List<String>  specNames = SpecialtyService.getNames(groupId());

        adminsTable.setItems(FXCollections.observableArrayList(
                AdminService.getGroupAdmins(groupId())));
        doctorsTable.setItems(FXCollections.observableArrayList(doctors));
        patientsTable.setItems(FXCollections.observableArrayList(patients));
        appointmentsTable.setItems(FXCollections.observableArrayList(
                AppointmentService.getAll(groupId())));
        recordsTable.setItems(FXCollections.observableArrayList(
                MedicalRecordService.getAll(groupId())));
        specialtiesTable.setItems(FXCollections.observableArrayList(
                SpecialtyService.getAll(groupId())));

        // Specialty combos for doctor register/edit
        docSpecialtyCombo.setItems(FXCollections.observableArrayList(specNames));
        editDocSpecialtyCombo.setItems(FXCollections.observableArrayList(specNames));

        // Refresh searchable pickers
        if (doctorPicker  != null) doctorPicker.setItems(doctors);
        if (patientPicker != null) patientPicker.setItems(patients);
    }

    private void populateMyInfo() {
        User me = Session.getCurrentUser();
        infoNameLabel.setText(me.getName());
        infoEmailLabel.setText(me.getEmail());
        infoPhoneLabel.setText(me.getPhone() != null ? me.getPhone() : "—");
        infoRoleLabel.setText("Administrator");
        editInfoNameField.setText(me.getName());
        editInfoPhoneField.setText(me.getPhone() != null ? me.getPhone() : "");
    }

    // ══ ADMIN ACTIONS ══

    @FXML
    private void handleRegisterAdmin() {
        adminRegStatusLabel.setStyle("-fx-text-fill: red;");
        if (newAdminName.getText().trim().isEmpty() || newAdminEmail.getText().trim().isEmpty()) {
            adminRegStatusLabel.setText("Name and email are required."); return;
        }
        boolean ok = AdminService.register(
            newAdminName.getText().trim(),
            newAdminEmail.getText().trim(),
            newAdminPhone.getText().trim(),
            newAdminPassword.getText()
        );
        if (ok) {
            adminRegStatusLabel.setStyle("-fx-text-fill: #00b09b;");
            adminRegStatusLabel.setText("Admin registered successfully.");
            newAdminName.clear(); newAdminEmail.clear();
            newAdminPhone.clear(); newAdminPassword.clear();
            loadAllData();
        } else adminRegStatusLabel.setText("Failed. Email may already exist.");
    }

    @FXML
    private void handleDeleteAdmin() {
        User sel = adminsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Select an admin to delete."); return; }
        if (sel.getUserId() == Session.getCurrentUser().getUserId()) {
            showAlert("You cannot delete your own account."); return;
        }
        if (sel.getCreatedBy() == null) {
            showAlert("Cannot delete a root administrator."); return;
        }
        confirm("Delete admin " + sel.getName() + "?", () -> {
            if (AdminService.delete(sel.getUserId())) { showInfo("Admin deleted."); loadAllData(); }
            else showAlert("Could not delete admin.");
        });
    }

    // ══ DOCTOR ACTIONS ══

    @FXML
    private void handleRegisterDoctor() {
        docStatusLabel.setStyle("-fx-text-fill: red;");
        if (docNameField.getText().trim().isEmpty() || docEmailField.getText().trim().isEmpty()
                || docSpecialtyCombo.getValue() == null) {
            docStatusLabel.setText("Name, email and specialty are required."); return;
        }
        Doctor doc = new Doctor();
        doc.setName(docNameField.getText().trim());
        doc.setEmail(docEmailField.getText().trim());
        doc.setPhone(docPhoneField.getText().trim());
        doc.setSpecialty(docSpecialtyCombo.getValue());
        doc.setPasswordHash(docPasswordField.getText());

        if (DoctorService.register(doc)) {
            docStatusLabel.setStyle("-fx-text-fill: #00b09b;");
            docStatusLabel.setText("Doctor registered successfully.");
            docNameField.clear(); docEmailField.clear(); docPhoneField.clear();
            docSpecialtyCombo.setValue(null); docPasswordField.clear();
            loadAllData(); loadStats();
        } else docStatusLabel.setText("Failed. Email may already exist.");
    }

    @FXML
    private void handleUpdateDoctor() {
        editDocStatusLabel.setStyle("-fx-text-fill: red;");
        Doctor sel = doctorsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { editDocStatusLabel.setText("Select a doctor first."); return; }
        if (editDocNameField.getText().trim().isEmpty()
                || editDocSpecialtyCombo.getValue() == null) {
            editDocStatusLabel.setText("Name and specialty are required."); return;
        }
        sel.setName(editDocNameField.getText().trim());
        sel.setPhone(editDocPhoneField.getText().trim());
        sel.setSpecialty(editDocSpecialtyCombo.getValue());

        if (DoctorService.updateDoctor(sel)) {
            editDocStatusLabel.setStyle("-fx-text-fill: #00b09b;");
            editDocStatusLabel.setText("Doctor updated.");
            loadAllData(); loadStats();
        } else editDocStatusLabel.setText("Update failed.");
    }

    @FXML
    private void handleDeleteDoctor() {
        Doctor sel = doctorsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Select a doctor to delete."); return; }
        confirm("Delete Dr. " + sel.getName() + "?\nFails if doctor has appointments.", () -> {
            if (DoctorService.delete(sel.getDoctorId())) {
                showInfo("Doctor deleted."); loadAllData(); loadStats();
            } else showAlert("Could not delete. Doctor may have existing appointments.");
        });
    }

    // ══ PATIENT ACTIONS ══

    @FXML
    private void handleRegisterPatient() {
        patStatusLabel.setStyle("-fx-text-fill: red;");
        if (patNameField.getText().trim().isEmpty() || patEmailField.getText().trim().isEmpty()
                || patGenderCombo.getValue() == null) {
            patStatusLabel.setText("Name, email and gender are required."); return;
        }
        Patient pat = new Patient();
        pat.setName(patNameField.getText().trim());
        pat.setEmail(patEmailField.getText().trim());
        pat.setPhone(patPhoneField.getText().trim());
        pat.setGender(patGenderCombo.getValue());
        pat.setDob(patDobPicker.getValue());
        pat.setPasswordHash(patPasswordField.getText());
        try {
            if (!patHeightField.getText().trim().isEmpty())
                pat.setHeight(Double.parseDouble(patHeightField.getText().trim()));
            if (!patWeightField.getText().trim().isEmpty())
                pat.setWeight(Double.parseDouble(patWeightField.getText().trim()));
        } catch (NumberFormatException e) {
            patStatusLabel.setText("Height and weight must be numbers."); return;
        }
        if (PatientService.register(pat)) {
            patStatusLabel.setStyle("-fx-text-fill: #00b09b;");
            patStatusLabel.setText("Patient registered successfully.");
            patNameField.clear(); patEmailField.clear(); patPhoneField.clear();
            patHeightField.clear(); patWeightField.clear(); patPasswordField.clear();
            patGenderCombo.setValue(null); patDobPicker.setValue(null);
            loadAllData(); loadStats();
        } else patStatusLabel.setText("Failed. Email may already exist.");
    }

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
            loadAllData(); loadStats();
        } else editPatStatusLabel.setText("Update failed.");
    }

    @FXML
    private void handleDeletePatient() {
        Patient sel = patientsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Select a patient to delete."); return; }
        confirm("Delete patient " + sel.getName() + "?\nFails if patient has appointments.", () -> {
            if (PatientService.delete(sel.getPatientId())) {
                showInfo("Patient deleted."); loadAllData(); loadStats();
            } else showAlert("Could not delete. Patient may have existing appointments.");
        });
    }

    // ══ APPOINTMENT ACTIONS ══

    @FXML
    private void handleCreateAppointment() {
        apptStatusLabel.setStyle("-fx-text-fill: red;");
        if (selectedDoctor  == null) { apptStatusLabel.setText("Select a doctor.");  return; }
        if (selectedPatient == null) { apptStatusLabel.setText("Select a patient."); return; }
        if (apptDatePicker.getValue() == null) {
            apptStatusLabel.setText("Select a date."); return;
        }
        if (apptDatePicker.getValue().isBefore(LocalDate.now())) {
            apptStatusLabel.setText("Cannot book in the past."); return;
        }
        if (apptReasonField.getText().trim().isEmpty()) {
            apptStatusLabel.setText("Reason is required."); return;
        }
        if (apptReasonField.getText().length() > 500) {
            apptStatusLabel.setText("Reason must be under 500 characters."); return;
        }
        LocalTime time;
        try {
            time = LocalTime.parse(apptTimeField.getText().trim(),
                    DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            apptStatusLabel.setText("Enter time as HH:mm (e.g. 09:30)."); return;
        }

        Appointment appt = new Appointment();
        appt.setDoctorId(selectedDoctor.getDoctorId());
        appt.setPatientId(selectedPatient.getPatientId());
        appt.setAppointmentDate(apptDatePicker.getValue());
        appt.setAppointmentTime(time);
        appt.setReason(apptReasonField.getText().trim());

        if (AppointmentService.book(appt)) {
            apptStatusLabel.setStyle("-fx-text-fill: #00b09b;");
            apptStatusLabel.setText("Appointment created successfully.");
            selectedDoctor  = null; selectedPatient = null;
            apptDoctorPickerBtn.setText("Select doctor…");
            apptPatientPickerBtn.setText("Select patient…");
            apptDatePicker.setValue(null);
            apptTimeField.clear(); apptReasonField.clear();
            loadAllData(); loadStats();
        } else apptStatusLabel.setText("That time slot is already booked for this doctor.");
    }

    @FXML
    private void handleConfirmAppointment() {
        Appointment sel = appointmentsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Select an appointment first."); return; }
        if (!"pending".equals(sel.getStatus())) {
            showAlert("Only pending appointments can be confirmed."); return;
        }
        if (AppointmentService.confirm(sel.getAppointmentId())) {
            showInfo("Appointment confirmed."); loadAllData(); loadStats();
        } else showAlert("Could not confirm appointment.");
    }

    @FXML
    private void handleCancelAppointment() {
        Appointment sel = appointmentsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Select an appointment first."); return; }
        if ("cancelled".equals(sel.getStatus())) { showAlert("Already cancelled."); return; }
        if ("completed".equals(sel.getStatus())) {
            showAlert("Cannot cancel a completed appointment."); return;
        }
        if (AppointmentService.cancel(sel.getAppointmentId())) {
            showInfo("Appointment cancelled."); loadAllData(); loadStats();
        } else showAlert("Could not cancel appointment.");
    }

    // ══ ARCHIVE / PURGE ACTIONS ══

    @FXML
    private void handleArchiveHistory() {
        archiveStatusLabel.setStyle("-fx-text-fill: #c07a1a;");
        archiveStatusLabel.setText("");
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION,
            "Archive all completed and cancelled appointments?\n\n"
          + "They will disappear from normal views but data is preserved.\n"
          + "You can purge archived data separately.",
            ButtonType.YES, ButtonType.NO);
        dlg.setHeaderText("Archive History");
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                int count = AppointmentService.archiveCompleted(groupId());
                archiveStatusLabel.setStyle("-fx-text-fill: #2d6a4f;");
                archiveStatusLabel.setText(count + " appointment(s) archived.");
                loadAllData(); loadStats();
            }
        });
    }

    @FXML
    private void handlePurgeArchived() {
        archiveStatusLabel.setStyle("-fx-text-fill: red;");
        archiveStatusLabel.setText("");
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION,
            "⚠ PERMANENTLY DELETE all archived records?\n\n"
          + "This cannot be undone. Medical data will be lost forever.\n"
          + "Only proceed if you are certain this data is no longer needed.",
            ButtonType.YES, ButtonType.NO);
        dlg.setHeaderText("Permanent Purge — Are you sure?");
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                // Second confirmation
                Alert confirm2 = new Alert(Alert.AlertType.CONFIRMATION,
                    "Last chance — permanently delete all archived data?",
                    ButtonType.YES, ButtonType.NO);
                confirm2.setHeaderText("Confirm Permanent Delete");
                confirm2.showAndWait().ifPresent(btn2 -> {
                    if (btn2 == ButtonType.YES) {
                        int count = AppointmentService.purgeArchived(groupId());
                        archiveStatusLabel.setStyle("-fx-text-fill: #2d6a4f;");
                        archiveStatusLabel.setText(count + " record(s) permanently deleted.");
                        loadAllData(); loadStats();
                    }
                });
            }
        });
    }

    // ══ RECORDS ACTIONS ══

    @FXML
    private void handleResolveRecord() {
        MedicalRecord sel = recordsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Select a record first."); return; }
        if ("resolved".equals(sel.getRecordStatus())) { showAlert("Already resolved."); return; }
        if (MedicalRecordService.resolve(sel.getRecordId())) {
            showInfo("Record resolved."); loadAllData(); loadStats();
        } else showAlert("Could not resolve record.");
    }

    // ══ SPECIALTY ACTIONS ══

    @FXML
    private void handleAddSpecialty() {
        specialtyStatusLabel.setStyle("-fx-text-fill: red;");
        String name = newSpecialtyField.getText().trim();
        if (name.isEmpty()) {
            specialtyStatusLabel.setText("Enter a specialty name."); return;
        }
        if (name.length() > 100) {
            specialtyStatusLabel.setText("Name must be under 100 characters."); return;
        }
        if (SpecialtyService.add(name, groupId())) {
            specialtyStatusLabel.setStyle("-fx-text-fill: #00b09b;");
            specialtyStatusLabel.setText("Specialty added.");
            newSpecialtyField.clear();
            loadAllData();
        } else specialtyStatusLabel.setText("Already exists or could not be added.");
    }

    @FXML
    private void handleDeleteSpecialty() {
        Specialty sel = specialtiesTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Select a specialty to delete."); return; }
        confirm("Delete specialty \"" + sel.getName() + "\"?\n"
              + "Fails if a doctor is currently assigned to it.", () -> {
            if (SpecialtyService.delete(sel.getSpecialtyId(), groupId())) {
                showInfo("Specialty deleted."); loadAllData();
            } else showAlert("Cannot delete — a doctor is currently using this specialty.");
        });
    }

    // ══ MY INFO ACTIONS ══

    @FXML
    private void handleUpdateInfo() {
        infoUpdateStatusLabel.setStyle("-fx-text-fill: red;");
        if (editInfoNameField.getText().trim().isEmpty()) {
            infoUpdateStatusLabel.setText("Name is required."); return;
        }
        boolean ok = UserService.updateProfile(
            Session.getCurrentUser().getUserId(),
            editInfoNameField.getText().trim(),
            editInfoPhoneField.getText().trim()
        );
        if (ok) {
            User refreshed = UserService.getById(Session.getCurrentUser().getUserId());
            if (refreshed != null) Session.setCurrentUser(refreshed);
            infoUpdateStatusLabel.setStyle("-fx-text-fill: #00b09b;");
            infoUpdateStatusLabel.setText("Profile updated.");
            welcomeLabel.setText("Welcome, " + Session.getCurrentUser().getName());
            populateMyInfo();
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

    @FXML private void handleRefresh() { loadStats(); loadAllData(); }

    @FXML
    private void handleLogout() {
        confirm("Are you sure you want to log out?", () -> {
            ReminderService.stop();
            Session.logout();
            SceneSwitcher.switchScene("/views/login.fxml", "Login — HealthAssist");
        });
    }

    // ── Helpers ───────────────────────────────────────────────

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void confirm(String msg, Runnable onYes) {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        dlg.setHeaderText("Confirm");
        dlg.showAndWait().ifPresent(btn -> { if (btn == ButtonType.YES) onYes.run(); });
    }
}