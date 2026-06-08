package Services;

import models.User;
import utils.DBConnection;
import utils.PasswordUtil;
import utils.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminService {

    /**
     * Register a new admin inside the current session's group.
     * Passwords are hashed before storage.
     */
    public static boolean register(String name, String email, String phone, String password) {
        User creator = Session.getCurrentUser();
        if (creator == null) return false;

        String hashed = (password == null || password.isBlank())
                        ? PasswordUtil.hash("temp_" + System.currentTimeMillis())
                        : PasswordUtil.hash(password);

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int userId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO User (name, email, password_hash, phone, role, created_by, group_id) "
                  + "VALUES (?, ?, ?, ?, 'admin', ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name.trim());
                ps.setString(2, email.trim());
                ps.setString(3, hashed);
                ps.setString(4, phone == null ? null : phone.trim());
                ps.setInt(5, creator.getUserId());
                ps.setInt(6, creator.getGroupId());
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) { conn.rollback(); return false; }
                userId = keys.getInt(1);
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Admin (user_id) VALUES (?)")) {
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

    /** All admins in the same group. */
    public static List<User> getGroupAdmins(int groupId) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.* FROM User u "
                   + "JOIN Admin a ON u.user_id = a.user_id "
                   + "WHERE u.group_id = ? ORDER BY u.name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(UserService.mapUser(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Delete a sub-admin (never deletes root admin). */
    public static boolean delete(int targetUserId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT created_by FROM User WHERE user_id = ?")) {
            ps.setInt(1, targetUserId);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getObject("created_by") == null) return false;
        } catch (SQLException e) { e.printStackTrace(); return false; }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Admin WHERE user_id = ?")) {
                ps.setInt(1, targetUserId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM User WHERE user_id = ?")) {
                ps.setInt(1, targetUserId);
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
}