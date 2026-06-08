package Services;

import models.Appointment;
import models.Doctor;
import models.Patient;
import models.User;
import utils.DBConnection;
import utils.Session;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AppointmentService {

    // ── Conflict check ────────────────────────────────────────

    public static boolean hasConflict(int doctorId, LocalDate date, LocalTime time) {
        String sql = "SELECT COUNT(*) FROM Appointment "
                   + "WHERE doctor_id = ? AND appointment_date = ? AND appointment_time = ? "
                   + "AND status NOT IN ('cancelled') AND is_archived = 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setDate(2, Date.valueOf(date));
            ps.setTime(3, Time.valueOf(time));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // ── CRUD ──────────────────────────────────────────────────

    /**
     * Book a new appointment.
     * Sends notifications to both the patient and the doctor immediately.
     */
    public static boolean book(Appointment appt) {
        if (hasConflict(appt.getDoctorId(), appt.getAppointmentDate(), appt.getAppointmentTime()))
            return false;

        String sql = "INSERT INTO Appointment "
                   + "(patient_id, doctor_id, appointment_date, appointment_time, status, reason) "
                   + "VALUES (?, ?, ?, ?, 'pending', ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appt.getPatientId());
            ps.setInt(2, appt.getDoctorId());
            ps.setDate(3, Date.valueOf(appt.getAppointmentDate()));
            ps.setTime(4, Time.valueOf(appt.getAppointmentTime()));
            ps.setString(5, truncate(appt.getReason(), 500));
            boolean ok = ps.executeUpdate() > 0;

            if (ok) sendBookingNotifications(appt);
            return ok;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean cancel(int appointmentId) {
        return updateStatus(appointmentId, "cancelled");
    }

    public static boolean confirm(int appointmentId) {
        return updateStatus(appointmentId, "confirmed");
    }

    public static boolean complete(int appointmentId) {
        return updateStatus(appointmentId, "completed");
    }

    private static boolean updateStatus(int appointmentId, String status) {
        String sql = "UPDATE Appointment SET status = ? WHERE appointment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, appointmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // ── Archive / Purge ───────────────────────────────────────

    /**
     * Archive all cancelled + completed appointments (and their medical records)
     * for the given group. They disappear from normal views but data is kept.
     * Returns count of archived appointments.
     */
    public static int archiveCompleted(int groupId) {
        String sql = "UPDATE Appointment a "
                   + "JOIN Doctor d ON a.doctor_id = d.doctor_id "
                   + "JOIN User u   ON d.user_id   = u.user_id "
                   + "SET a.is_archived = 1 "
                   + "WHERE u.group_id = ? "
                   + "AND a.status IN ('cancelled','completed') "
                   + "AND a.is_archived = 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            int count = ps.executeUpdate();
            // Also archive the linked medical records
            archiveRecordsForGroup(groupId);
            return count;
        } catch (SQLException e) { e.printStackTrace(); return 0; }
    }

    /** Archive all medical records whose appointment is archived. */
    private static void archiveRecordsForGroup(int groupId) {
        String sql = "UPDATE Medical_Record mr "
                   + "JOIN Appointment a ON mr.appointment_id = a.appointment_id "
                   + "JOIN Doctor d      ON a.doctor_id = d.doctor_id "
                   + "JOIN User u        ON d.user_id   = u.user_id "
                   + "SET mr.is_archived = 1 "
                   + "WHERE u.group_id = ? AND a.is_archived = 1 AND mr.is_archived = 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /**
     * Permanently delete all archived appointments (and their records) for a group.
     * Returns count deleted.
     */
    public static int purgeArchived(int groupId) {
        // Delete records first (FK constraint)
        String delRecords = "DELETE mr FROM Medical_Record mr "
                          + "JOIN Appointment a ON mr.appointment_id = a.appointment_id "
                          + "JOIN Doctor d      ON a.doctor_id = d.doctor_id "
                          + "JOIN User u        ON d.user_id   = u.user_id "
                          + "WHERE u.group_id = ? AND a.is_archived = 1";
        String delAppts   = "DELETE a FROM Appointment a "
                          + "JOIN Doctor d ON a.doctor_id = d.doctor_id "
                          + "JOIN User u   ON d.user_id   = u.user_id "
                          + "WHERE u.group_id = ? AND a.is_archived = 1";
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            int count;
            try (PreparedStatement ps = conn.prepareStatement(delRecords)) {
                ps.setInt(1, groupId); ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(delAppts)) {
                ps.setInt(1, groupId); count = ps.executeUpdate();
            }
            conn.commit();
            return count;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            e.printStackTrace();
            return 0;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); }
                              catch (SQLException ignored) {}
        }
    }

    // ── Queries ───────────────────────────────────────────────

    /** All active (non-archived) appointments for a group. */
    public static List<Appointment> getAll(int groupId) {
        return query(BASE_SQL + " WHERE u_doc.group_id = ? AND a.is_archived = 0"
                   + " ORDER BY a.appointment_date DESC, a.appointment_time DESC",
                   groupId);
    }

    /** All appointments including archived (admin archive view). */
    public static List<Appointment> getAllIncludingArchived(int groupId) {
        return query(BASE_SQL + " WHERE u_doc.group_id = ?"
                   + " ORDER BY a.appointment_date DESC, a.appointment_time DESC",
                   groupId);
    }

    /** Active appointments for a patient. */
    public static List<Appointment> getByPatient(int patientId) {
        return query(BASE_SQL + " WHERE a.patient_id = ? AND a.is_archived = 0"
                   + " ORDER BY a.appointment_date DESC, a.appointment_time DESC",
                   patientId);
    }

    /** Active appointments for a doctor. */
    public static List<Appointment> getByDoctor(int doctorId) {
        return query(BASE_SQL + " WHERE a.doctor_id = ? AND a.is_archived = 0"
                   + " ORDER BY a.appointment_date DESC, a.appointment_time DESC",
                   doctorId);
    }

    /** Upcoming (non-cancelled, non-archived) for a doctor. */
    public static List<Appointment> getUpcomingByDoctor(int doctorId) {
        return query(BASE_SQL + " WHERE a.doctor_id = ? AND a.appointment_date >= CURDATE()"
                   + " AND a.status != 'cancelled' AND a.is_archived = 0"
                   + " ORDER BY a.appointment_date, a.appointment_time",
                   doctorId);
    }

    /** Upcoming for the whole group. */
    public static List<Appointment> getUpcoming(int groupId) {
        return query(BASE_SQL + " WHERE u_doc.group_id = ? AND a.appointment_date >= CURDATE()"
                   + " AND a.status != 'cancelled' AND a.is_archived = 0"
                   + " ORDER BY a.appointment_date, a.appointment_time",
                   groupId);
    }

    public static List<Appointment> getAll() {
        User u = Session.getCurrentUser();
        return u != null ? getAll(u.getGroupId()) : new ArrayList<>();
    }

    // ── Internals ─────────────────────────────────────────────

    private static final String BASE_SQL =
        "SELECT a.appointment_id, a.patient_id, a.doctor_id, "
      + "a.appointment_date, a.appointment_time, a.status, a.reason, a.is_archived, "
      + "u_pat.name AS patient_name, u_doc.name AS doctor_name "
      + "FROM Appointment a "
      + "JOIN Patient p   ON a.patient_id = p.patient_id "
      + "JOIN User u_pat  ON p.user_id    = u_pat.user_id "
      + "JOIN Doctor d    ON a.doctor_id  = d.doctor_id "
      + "JOIN User u_doc  ON d.user_id    = u_doc.user_id";

    private static List<Appointment> query(String sql, int param) {
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private static Appointment map(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setAppointmentId(rs.getInt("appointment_id"));
        a.setPatientId(rs.getInt("patient_id"));
        a.setDoctorId(rs.getInt("doctor_id"));
        a.setAppointmentDate(rs.getDate("appointment_date").toLocalDate());
        a.setAppointmentTime(rs.getTime("appointment_time").toLocalTime());
        a.setStatus(rs.getString("status"));
        a.setReason(rs.getString("reason"));
        a.setArchived(rs.getInt("is_archived") == 1);
        a.setPatientName(rs.getString("patient_name"));
        a.setDoctorName(rs.getString("doctor_name"));
        return a;
    }

    // ── Notification helpers ──────────────────────────────────

    private static void sendBookingNotifications(Appointment appt) {
        String dateFmt = appt.getAppointmentDate()
                             .format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        String timeFmt = appt.getAppointmentTime()
                             .format(DateTimeFormatter.ofPattern("HH:mm"));

        // Resolve names and user_ids needed for notifications
        try {
            Patient patient = PatientService.getPatientById(appt.getPatientId());
            Doctor  doctor  = DoctorService.getDoctorById(appt.getDoctorId());
            if (patient == null || doctor == null) return;

            ReminderService.persistNotification(
                patient.getUserId(),
                "Appointment Booked",
                "Your appointment with Dr. " + doctor.getName()
                + " is scheduled for " + dateFmt + " at " + timeFmt
                + " — " + appt.getReason()
            );

            ReminderService.persistNotification(
                doctor.getUserId(),
                "New Appointment",
                "An appointment has been booked with patient " + patient.getName()
                + " on " + dateFmt + " at " + timeFmt
                + " — " + appt.getReason()
            );
        } catch (Exception e) {
            // Never let notification failure break the booking
            e.printStackTrace();
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}