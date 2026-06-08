package models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MedicalRecord {
    private int           recordId;
    private int           appointmentId;
    private String        diagnosis;
    private String        prescription;
    private String        recordStatus;
    private LocalDateTime createdAt;
    private boolean       isArchived;

    // Display-only (populated by JOIN queries)
    private String    patientName;
    private String    doctorName;
    private LocalDate appointmentDate;

    public MedicalRecord() {}

    public int           getRecordId()                          { return recordId; }
    public void          setRecordId(int recordId)              { this.recordId = recordId; }
    public int           getAppointmentId()                     { return appointmentId; }
    public void          setAppointmentId(int appointmentId)    { this.appointmentId = appointmentId; }
    public String        getDiagnosis()                         { return diagnosis; }
    public void          setDiagnosis(String diagnosis)         { this.diagnosis = diagnosis; }
    public String        getPrescription()                      { return prescription; }
    public void          setPrescription(String prescription)   { this.prescription = prescription; }
    public String        getRecordStatus()                      { return recordStatus; }
    public void          setRecordStatus(String recordStatus)   { this.recordStatus = recordStatus; }
    public LocalDateTime getCreatedAt()                         { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt)  { this.createdAt = createdAt; }
    public boolean       isArchived()                           { return isArchived; }
    public void          setArchived(boolean archived)          { this.isArchived = archived; }
    public String        getPatientName()                       { return patientName; }
    public void          setPatientName(String patientName)     { this.patientName = patientName; }
    public String        getDoctorName()                        { return doctorName; }
    public void          setDoctorName(String doctorName)       { this.doctorName = doctorName; }
    public LocalDate     getAppointmentDate()                   { return appointmentDate; }
    public void          setAppointmentDate(LocalDate d)        { this.appointmentDate = d; }
}