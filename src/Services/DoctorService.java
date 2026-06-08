package Services;

import models.Doctor;
import models.User;
import utils.DBConnection;
import utils.PasswordUtil;
import utils.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorService {

    /** Register a new doctor. Password is hashed before storage. */
    public static boolean register(Doctor doctor) {
        User creator = Session.getCurrentUser();
        if (creator == null) return false;

        String hashed = (doctor.getPasswordHash() == null || doctor.getPasswordHash().isBlank())
                        ? PasswordUtil.hash("temp_" + System.currentTimeMillis())
                        : PasswordUtil.hash(doctor.getPasswordHash());

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int userId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO User (name, email, password_hash, phone, role, created_by, group_id) "
                  + "VALUES (?, ?, ?, ?, 'doctor', ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, doctor.getName());
                ps.setString(2, doctor.getEmail());
                ps.setString(3, hashed);
                ps.setString(4, doctor.getPhone());
                ps.setInt(5, creator.getUserId());
                ps.setInt(6, creator.getGroupId());
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) { conn.rollback(); return false; }
                userId = keys.getInt(1);
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Doctor (user_id, specialty) VALUES (?, ?)")) {
                ps.setInt(1, userId);
                ps.setString(2, doctor.getSpecialty());
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

    /** All doctors for a group. */
    public static List<Doctor> getAllDoctors(int groupId) {
        List<Doctor> list = new ArrayList<>();
        String sql = "SELECT u.user_id, u.name, u.email, u.phone, d.doctor_id, d.specialty "
                   + "FROM Doctor d JOIN User u ON d.user_id = u.user_id "
                   + "WHERE u.group_id = ? ORDER BY u.name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapDoctor(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Doctor by user_id (used after login). */
    public static Doctor getDoctorByUserId(int userId) {
        String sql = "SELECT u.user_id, u.name, u.email, u.phone, d.doctor_id, d.specialty "
                   + "FROM Doctor d JOIN User u ON d.user_id = u.user_id WHERE u.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapDoctor(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    /** Doctor by doctor_id. */
    public static Doctor getDoctorById(int doctorId) {
        String sql = "SELECT u.user_id, u.name, u.email, u.phone, d.doctor_id, d.specialty "
                   + "FROM Doctor d JOIN User u ON d.user_id = u.user_id WHERE d.doctor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapDoctor(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    /** Update doctor name, phone, specialty. */
    public static boolean updateDoctor(Doctor doctor) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE User SET name = ?, phone = ? WHERE user_id = ?")) {
                ps.setString(1, doctor.getName());
                ps.setString(2, doctor.getPhone());
                ps.setInt(3, doctor.getUserId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Doctor SET specialty = ? WHERE doctor_id = ?")) {
                ps.setString(1, doctor.getSpecialty());
                ps.setInt(2, doctor.getDoctorId());
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

    /** Delete a doctor. Fails if doctor has appointments. */
    public static boolean delete(int doctorId) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int userId = -1;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT user_id FROM Doctor WHERE doctor_id = ?")) {
                ps.setInt(1, doctorId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) userId = rs.getInt(1);
                else { conn.rollback(); return false; }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Doctor WHERE doctor_id = ?")) {
                ps.setInt(1, doctorId);
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

    public static List<Doctor> getAllDoctors() {
        User u = Session.getCurrentUser();
        return u != null ? getAllDoctors(u.getGroupId()) : new ArrayList<>();
    }

    public static Doctor mapDoctor(ResultSet rs) throws SQLException {
        Doctor d = new Doctor();
        d.setUserId(rs.getInt("user_id"));
        d.setName(rs.getString("name"));
        d.setEmail(rs.getString("email"));
        d.setPhone(rs.getString("phone"));
        d.setDoctorId(rs.getInt("doctor_id"));
        d.setSpecialty(rs.getString("specialty"));
        return d;
    }
}