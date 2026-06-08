package Services;

import models.Specialty;
import utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SpecialtyService — manages the per-group specialty list.
 *
 * Each clinic group has its own specialties so a dental clinic
 * doesn't see Cardiology, etc.
 *
 * On first registration, seedDefaultsForGroup() is called to
 * populate a sensible starter list that the admin can then trim.
 */
public class SpecialtyService {

    // ── Predefined global list ────────────────────────────────
    public static final List<String> PREDEFINED = List.of(
        "Cardiology",
        "Dermatology",
        "Emergency Medicine",
        "Endocrinology",
        "Family Medicine / General Practice",
        "Gastroenterology",
        "Geriatrics",
        "Gynecology & Obstetrics",
        "Hematology",
        "Infectious Disease",
        "Internal Medicine",
        "Nephrology",
        "Neurology",
        "Neurosurgery",
        "Oncology",
        "Ophthalmology",
        "Orthopedics",
        "Otolaryngology (ENT)",
        "Pediatrics",
        "Plastic Surgery",
        "Psychiatry & Mental Health",
        "Pulmonology",
        "Radiology",
        "Rheumatology",
        "Sports Medicine",
        "Stomatology / Dentistry",
        "Surgery (General)",
        "Urology"
    );

    // ── Public API ────────────────────────────────────────────

    /** Get all specialties for a group, alphabetically. */
    public static List<Specialty> getAll(int groupId) {
        List<Specialty> list = new ArrayList<>();
        String sql = "SELECT specialty_id, name, group_id FROM Specialty "
                   + "WHERE group_id = ? ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Get just the names (for ComboBox). */
    public static List<String> getNames(int groupId) {
        List<String> names = new ArrayList<>();
        for (Specialty s : getAll(groupId)) names.add(s.getName());
        return names;
    }

    /** Add a new specialty to a group. Returns false if name already exists. */
    public static boolean add(String name, int groupId) {
        String sql = "INSERT INTO Specialty (name, group_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setInt(2, groupId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            // Duplicate entry → unique constraint violation
            if (e.getErrorCode() == 1062) return false;
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a specialty.
     * Returns false if any doctor in the group still uses this specialty name.
     */
    public static boolean delete(int specialtyId, int groupId) {
        // Find the name first
        String name = getNameById(specialtyId);
        if (name == null) return false;

        // Check if any doctor in this group uses it
        String check = "SELECT COUNT(*) FROM Doctor d "
                     + "JOIN User u ON d.user_id = u.user_id "
                     + "WHERE u.group_id = ? AND d.specialty = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setInt(1, groupId);
            ps.setString(2, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return false; // still in use
        } catch (SQLException e) { e.printStackTrace(); return false; }

        String sql = "DELETE FROM Specialty WHERE specialty_id = ? AND group_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, specialtyId);
            ps.setInt(2, groupId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /**
     * Seed the predefined list for a brand-new group.
     * Called once after a root admin self-registers.
     * Safe to call multiple times (ignores duplicates).
     */
    public static void seedDefaultsForGroup(int groupId) {
        String sql = "INSERT IGNORE INTO Specialty (name, group_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String name : PREDEFINED) {
                ps.setString(1, name);
                ps.setInt(2, groupId);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Internals ─────────────────────────────────────────────

    private static String getNameById(int specialtyId) {
        String sql = "SELECT name FROM Specialty WHERE specialty_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, specialtyId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    private static Specialty map(ResultSet rs) throws SQLException {
        return new Specialty(
            rs.getInt("specialty_id"),
            rs.getString("name"),
            rs.getInt("group_id")
        );
    }
}