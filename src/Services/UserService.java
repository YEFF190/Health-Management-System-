package Services;

import models.User;
import utils.DBConnection;
import utils.PasswordUtil;

import java.sql.*;

public class UserService {

    /**
     * Self-register a root admin.
     * After insert, seeds the predefined specialty list for this new group.
     */
    public static boolean selfRegister(User user) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String hashed = PasswordUtil.hash(user.getPasswordHash());

            int userId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO User (name, email, password_hash, phone, role, created_by, group_id) "
                  + "VALUES (?, ?, ?, ?, 'admin', NULL, 0)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getName());
                ps.setString(2, user.getEmail());
                ps.setString(3, hashed);
                ps.setString(4, user.getPhone());
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) { conn.rollback(); return false; }
                userId = keys.getInt(1);
            }

            // group_id = own user_id
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE User SET group_id = ? WHERE user_id = ?")) {
                ps.setInt(1, userId);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Admin (user_id) VALUES (?)")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            conn.commit();

            // Seed predefined specialties for this new group (outside transaction is fine)
            SpecialtyService.seedDefaultsForGroup(userId);

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

    /**
     * Login — supports three stored formats:
     *   1. HASH:salt:hash  (current)
     *   2. salt:hash       (old, no prefix — upgrades on success)
     *   3. plain text      (original — upgrades on success)
     */
    public static User login(String email, String password) {
        String sql = "SELECT * FROM User WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            User user   = mapUser(rs);
            String stored = user.getPasswordHash();
            boolean ok  = false;

            if (stored == null) {
                ok = false;
            } else if (stored.startsWith("HASH:")) {
                ok = PasswordUtil.verify(password, stored);
            } else if (stored.contains(":") && stored.length() > 30) {
                ok = verifyOldFormat(password, stored);
                if (ok) rehashPassword(user.getUserId(), password);
            } else {
                ok = stored.equals(password);
                if (ok) rehashPassword(user.getUserId(), password);
            }

            return ok ? user : null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean updateProfile(int userId, String name, String phone) {
        String sql = "UPDATE User SET name = ?, phone = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean changePassword(int userId, String currentPlain, String newPlain) {
        String stored = null;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT password_hash FROM User WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) stored = rs.getString("password_hash");
        } catch (SQLException e) { e.printStackTrace(); return false; }

        boolean valid = false;
        if (stored == null) {
            valid = false;
        } else if (stored.startsWith("HASH:")) {
            valid = PasswordUtil.verify(currentPlain, stored);
        } else if (stored.contains(":") && stored.length() > 30) {
            valid = verifyOldFormat(currentPlain, stored);
        } else {
            valid = stored.equals(currentPlain);
        }
        if (!valid) return false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE User SET password_hash = ? WHERE user_id = ?")) {
            ps.setString(1, PasswordUtil.hash(newPlain));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static User getById(int userId) {
        String sql = "SELECT * FROM User WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // ── Internals ─────────────────────────────────────────────

    private static boolean verifyOldFormat(String plain, String stored) {
        try {
            String[] parts = stored.split(":", 2);
            if (parts.length != 2) return false;
            byte[] salt     = java.util.Base64.getDecoder().decode(parts[0]);
            byte[] expected = java.util.Base64.getDecoder().decode(parts[1]);
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            md.update(salt);
            md.update(plain.getBytes("UTF-8"));
            byte[] actual = md.digest();
            if (actual.length != expected.length) return false;
            int diff = 0;
            for (int i = 0; i < actual.length; i++) diff |= actual[i] ^ expected[i];
            return diff == 0;
        } catch (Exception e) { return false; }
    }

    private static void rehashPassword(int userId, String plain) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE User SET password_hash = ? WHERE user_id = ?")) {
            ps.setString(1, PasswordUtil.hash(plain));
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public static User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setPhone(rs.getString("phone"));
        u.setRole(rs.getString("role"));
        u.setGroupId(rs.getInt("group_id"));
        int cb = rs.getInt("created_by");
        if (!rs.wasNull()) u.setCreatedBy(cb);
        return u;
    }
}