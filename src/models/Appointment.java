package models;

import java.time.LocalDate;
import java.time.LocalTime;

public class Appointment {
    private int       appointmentId;
    private int       patientId;
    private int       doctorId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String    status;   // pending | confirmed | cancelled | completed
    private String    reason;
    private boolean   isArchived;

    // Display-only (populated by JOIN queries)
    private String patientName;
    private String doctorName;

    public Appointment() {}

    public int       getAppointmentId()                        { return appointmentId; }
    public void      setAppointmentId(int appointmentId)       { this.appointmentId = appointmentId; }
    public int       getPatientId()                            { return patientId; }
    public void      setPatientId(int patientId)               { this.patientId = patientId; }
    public int       getDoctorId()                             { return doctorId; }
    public void      setDoctorId(int doctorId)                 { this.doctorId = doctorId; }
    public LocalDate getAppointmentDate()                      { return appointmentDate; }
    public void      setAppointmentDate(LocalDate d)           { this.appointmentDate = d; }
    public LocalTime getAppointmentTime()                      { return appointmentTime; }
    public void      setAppointmentTime(LocalTime t)           { this.appointmentTime = t; }
    public String    getStatus()                               { return status; }
    public void      setStatus(String status)                  { this.status = status; }
    public String    getReason()                               { return reason; }
    public void      setReason(String reason)                  { this.reason = reason; }
    public boolean   isArchived()                              { return isArchived; }
    public void      setArchived(boolean archived)             { this.isArchived = archived; }
    public String    getPatientName()                          { return patientName; }
    public void      setPatientName(String patientName)        { this.patientName = patientName; }
    public String    getDoctorName()                           { return doctorName; }
    public void      setDoctorName(String doctorName)          { this.doctorName = doctorName; }

    @Override
    public String toString() {
        return appointmentDate + " " + appointmentTime + " — " + reason;
    }
}