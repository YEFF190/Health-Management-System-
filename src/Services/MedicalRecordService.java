package Services;

import models.MedicalRecord;
import models.User;
import utils.DBConnection;
import utils.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicalRecordService {

    /** Add a medical record linked to an appointment. */
    public static boolean add(MedicalRecord record) {
        String sql = "INSERT INTO Medical_Record (appointment_id, diagnosis, prescription, record_status) "
                   + "VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, record.getAppointmentId());
            ps.setString(2, truncate(record.getDiagnosis(), 500));
            ps.setString(3, record.getPrescription());
            ps.setString(4, record.getRecordStatus() != null ? record.getRecordStatus() : "active");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /** Update an existing medical record. */
    public static boolean update(MedicalRecord record) {
        String sql = "UPDATE Medical_Record SET diagnosis = ?, prescription = ?, record_status = ? "
                   + "WHERE record_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, truncate(record.getDiagnosis(), 500));
            ps.setString(2, record.getPrescription());
            ps.setString(3, record.getRecordStatus());
            ps.setInt(4, record.getRecordId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /** Resolve a record. */
    public static boolean resolve(int recordId) {
        String sql = "UPDATE Medical_Record SET record_status = 'resolved' WHERE record_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recordId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /** All active (non-archived) records for a group. */
    public static List<MedicalRecord> getAll(int groupId) {
        return query(BASE_SQL + " WHERE u_doc.group_id = ? AND mr.is_archived = 0"
                   + " ORDER BY mr.created_at DESC", groupId);
    }

    /** All records including archived (admin archive view). */
    public static List<MedicalRecord> getAllIncludingArchived(int groupId) {
        return query(BASE_SQL + " WHERE u_doc.group_id = ?"
                   + " ORDER BY mr.created_at DESC", groupId);
    }

    /** Records for a specific doctor (active only). */
    public static List<MedicalRecord> getByDoctor(int doctorId) {
        return query(BASE_SQL + " WHERE a.doctor_id = ? AND mr.is_archived = 0"
                   + " ORDER BY mr.created_at DESC", doctorId);
    }

    /** Records for a specific patient (active only). */
    public static List<MedicalRecord> getByPatient(int patientId) {
        return query(BASE_SQL + " WHERE a.patient_id = ? AND mr.is_archived = 0"
                   + " ORDER BY mr.created_at DESC", patientId);
    }

    public static List<MedicalRecord> getAll() {
        User u = Session.getCurrentUser();
        return u != null ? getAll(u.getGroupId()) : new ArrayList<>();
    }

    // ── Internals ─────────────────────────────────────────────

    private static final String BASE_SQL =
        "SELECT mr.*, u_pat.name AS patient_name, u_doc.name AS doctor_name, a.appointment_date "
      + "FROM Medical_Record mr "
      + "JOIN Appointment a ON mr.appointment_id = a.appointment_id "
      + "JOIN Patient p     ON a.patient_id = p.patient_id "
      + "JOIN User u_pat    ON p.user_id    = u_pat.user_id "
      + "JOIN Doctor d      ON a.doctor_id  = d.doctor_id "
      + "JOIN User u_doc    ON d.user_id    = u_doc.user_id";

    private static List<MedicalRecord> query(String sql, int param) {
        List<MedicalRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapFull(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private static MedicalRecord mapFull(ResultSet rs) throws SQLException {
        MedicalRecord mr = new MedicalRecord();
        mr.setRecordId(rs.getInt("record_id"));
        mr.setAppointmentId(rs.getInt("appointment_id"));
        mr.setDiagnosis(rs.getString("diagnosis"));
        mr.setPrescription(rs.getString("prescription"));
        mr.setRecordStatus(rs.getString("record_status"));
        mr.setArchived(rs.getInt("is_archived") == 1);
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) mr.setCreatedAt(ts.toLocalDateTime());
        mr.setPatientName(rs.getString("patient_name"));
        mr.setDoctorName(rs.getString("doctor_name"));
        Date d = rs.getDate("appointment_date");
        if (d != null) mr.setAppointmentDate(d.toLocalDate());
        return mr;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}