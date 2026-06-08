package Services;

import models.Patient;
import models.User;
import utils.DBConnection;
import utils.PasswordUtil;
import utils.Session;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PatientService {

    /** Register a new patient. Password is hashed before storage. */
    public static boolean register(Patient patient) {
        User creator = Session.getCurrentUser();
        if (creator == null) return false;

        String hashed = (patient.getPasswordHash() == null || patient.getPasswordHash().isBlank())
                        ? PasswordUtil.hash("temp_" + System.currentTimeMillis())
                        : PasswordUtil.hash(patient.getPasswordHash());

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int userId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO User (name, email, password_hash, phone, role, created_by, group_id) "
                  + "VALUES (?, ?, ?, ?, 'patient', ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, patient.getName());
                ps.setString(2, patient.getEmail());
                ps.setString(3, hashed);
                ps.setString(4, patient.getPhone());
                ps.setInt(5, creator.getUserId());
                ps.setInt(6, creator.getGroupId());
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) { conn.rollback(); return false; }
                userId = keys.getInt(1);
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Patient (user_id, height, weight, DOB, gender) "
                  + "VALUES (?, ?, ?, ?, ?)")) {
                ps.setInt(1, userId);
                ps.setObject(2, patient.getHeight());
                ps.setObject(3, patient.getWeight());
                ps.setDate(4, patient.getDob() != null ? Date.valueOf(patient.getDob()) : null);
                ps.setString(5, patient.getGender());
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); }
                              catch (SQLException ignored) {}
        }
    }

    /** All patients for a group. */
    public static List<Patient> getAllPatients(int groupId) {
        List<Patient> list = new ArrayList<>();
        String sql = "SELECT u.user_id, u.name, u.email, u.phone, "
                   + "p.patient_id, p.height, p.weight, p.DOB, p.gender "
                   + "FROM Patient p JOIN User u ON p.user_id = u.user_id "
                   + "WHERE u.group_id = ? ORDER BY u.name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapPatient(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Patients with appointments with a specific doctor. */
    public static List<Patient> getPatientsByDoctor(int doctorId) {
        List<Patient> list = new ArrayList<>();
        String sql = "SELECT DISTINCT u.user_id, u.name, u.email, u.phone, "
                   + "p.patient_id, p.height, p.weight, p.DOB, p.gender "
                   + "FROM Patient p JOIN User u ON p.user_id = u.user_id "
                   + "JOIN Appointment a ON a.patient_id = p.patient_id "
                   + "WHERE a.doctor_id = ? ORDER BY u.name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapPatient(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Patient by user_id. */
    public static Patient getPatientByUserId(int userId) {
        String sql = "SELECT u.user_id, u.name, u.email, u.phone, "
                   + "p.patient_id, p.height, p.weight, p.DOB, p.gender "
                   + "FROM Patient p JOIN User u ON p.user_id = u.user_id WHERE u.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapPatient(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    /** Patient by patient_id. */
    public static Patient getPatientById(int patientId) {
        String sql = "SELECT u.user_id, u.name, u.email, u.phone, "
                   + "p.patient_id, p.height, p.weight, p.DOB, p.gender "
                   + "FROM Patient p JOIN User u ON p.user_id = u.user_id WHERE p.patient_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapPatient(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    /** Update patient details (admin/doctor). */
    public static boolean update(Patient patient) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE User SET name = ?, phone = ? WHERE user_id = ?")) {
                ps.setString(1, patient.getName());
                ps.setString(2, patient.getPhone());
                ps.setInt(3, patient.getUserId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Patient SET height = ?, weight = ?, DOB = ?, gender = ? "
                  + "WHERE patient_id = ?")) {
                ps.setObject(1, patient.getHeight());
                ps.setObject(2, patient.getWeight());
                ps.setDate(3, patient.getDob() != null ? Date.valueOf(patient.getDob()) : null);
                ps.setString(4, patient.getGender());
                ps.setInt(5, patient.getPatientId());
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); }
                              catch (SQLException ignored) {}
        }
    }

    /** Patient self-update: only name and DOB. */
    public static boolean selfUpdate(int userId, int patientId, String name, LocalDate dob) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE User SET name = ? WHERE user_id = ?")) {
                ps.setString(1, name);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Patient SET DOB = ? WHERE patient_id = ?")) {
                ps.setDate(1, dob != null ? Date.valueOf(dob) : null);
                ps.setInt(2, patientId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); }
                              catch (SQLException ignored) {}
        }
    }

    /** Delete a patient. Fails if patient has appointments. */
    public static boolean delete(int patientId) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int userId = -1;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT user_id FROM Patient WHERE patient_id = ?")) {
                ps.setInt(1, patientId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) userId = rs.getInt(1);
                else { conn.rollback(); return false; }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Patient WHERE patient_id = ?")) {
                ps.setInt(1, patientId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM User WHERE user_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); }
                              catch (SQLException ignored) {}
        }
    }

    public static List<Patient> getAllPatients() {
        User u = Session.getCurrentUser();
        return u != null ? getAllPatients(u.getGroupId()) : new ArrayList<>();
    }

    public static Patient mapPatient(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.setUserId(rs.getInt("user_id"));
        p.setName(rs.getString("name"));
        p.setEmail(rs.getString("email"));
        p.setPhone(rs.getString("phone"));
        p.setPatientId(rs.getInt("patient_id"));
        double h = rs.getDouble("height"); if (!rs.wasNull()) p.setHeight(h);
        double w = rs.getDouble("weight"); if (!rs.wasNull()) p.setWeight(w);
        Date dob = rs.getDate("DOB");      if (dob != null)   p.setDob(dob.toLocalDate());
        p.setGender(rs.getString("gender"));
        return p;
    }
}