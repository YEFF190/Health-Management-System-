package Services;

import utils.DBConnection;
import utils.Toast;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * ReminderService — background notification + reminder engine.
 *
 * Fixes vs original:
 *  - started flag resets on stop() so re-login restarts the scheduler
 *  - persistNotification() is now public (used by AppointmentService on booking)
 *  - ensureSchema() is called once at class load, not inside hot paths
 */
public class ReminderService {

    // ── Scheduler (lives for the JVM lifetime) ────────────────
    private static final ScheduledExecutorService SCHEDULER =
        Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "ReminderThread");
            t.setDaemon(true);
            return t;
        });

    // ── Per-session state (reset on stop()) ───────────────────
    private static volatile Consumer<String> adminCallback   = null;
    private static volatile Consumer<String> doctorCallback  = null;
    private static volatile Consumer<String> patientCallback = null;
    private static volatile int  activeGroupId   = -1;
    private static volatile int  activeDoctorId  = -1;
    private static volatile int  activePatientId = -1;
    private static volatile String activeRole    = "";
    private static volatile boolean started      = false;

    private static final java.util.Set<String> sentThisSession =
        java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    // Future handles so we can cancel tasks on stop()
    private static volatile ScheduledFuture<?> apptTask    = null;
    private static volatile ScheduledFuture<?> summaryTask = null;

    // Schema flag — initialised once
    private static volatile boolean schemaReady = false;

    static { ensureSchema(); }

    // ── Public API ────────────────────────────────────────────

    public static synchronized void start(int groupId, Consumer<String> callback) {
        adminCallback = callback;
        activeGroupId = groupId;
        activeRole    = "admin";
        ensureRunning();
    }

    public static synchronized void startForDoctor(int doctorId, Consumer<String> callback) {
        doctorCallback = callback;
        activeDoctorId = doctorId;
        activeRole     = "doctor";
        ensureRunning();
    }

    public static synchronized void startForPatient(int patientId, Consumer<String> callback) {
        patientCallback = callback;
        activePatientId = patientId;
        activeRole      = "patient";
        ensureRunning();
    }

    /** Hard-stop on JVM exit. */
    public static void shutdown() {
        stop();
        SCHEDULER.shutdownNow();
    }

    /** Stop reminders and reset state so next login restarts cleanly. */
    public static synchronized void stop() {
        if (apptTask    != null) { apptTask.cancel(false);    apptTask    = null; }
        if (summaryTask != null) { summaryTask.cancel(false); summaryTask = null; }
        adminCallback   = null;
        doctorCallback  = null;
        patientCallback = null;
        activeRole      = "";
        activeGroupId   = -1;
        activeDoctorId  = -1;
        activePatientId = -1;
        started         = false;          // ← KEY FIX: allows restart after re-login
        sentThisSession.clear();
    }

    // ── Scheduling ────────────────────────────────────────────

    private static synchronized void ensureRunning() {
        if (started) return;
        started     = true;
        apptTask    = SCHEDULER.scheduleAtFixedRate(
                          ReminderService::checkUpcomingAppointments, 0, 60, TimeUnit.SECONDS);
        summaryTask = SCHEDULER.scheduleAtFixedRate(
                          ReminderService::emitHourlySummary, 0, 3600, TimeUnit.SECONDS);
    }

    // ── Core reminder logic ───────────────────────────────────

    private static void checkUpcomingAppointments() {
        List<ReminderRow> rows = queryUpcoming();
        for (ReminderRow row : rows) {
            String key = "appt_" + row.appointmentId;
            if (!sentThisSession.add(key)) continue;

            persistNotification(row.patientUserId,
                "Appointment Reminder",
                "You have an appointment with Dr. " + row.doctorName
                + " today at " + row.time + " — " + row.reason);

            persistNotification(row.doctorUserId,
                "Appointment Reminder",
                "You have an appointment with " + row.patientName
                + " today at " + row.time + " — " + row.reason);

            for (int adminUid : getGroupAdminUserIds(row.groupId))
                persistNotification(adminUid,
                    "Appointment Reminder",
                    row.patientName + " ↔ Dr. " + row.doctorName
                    + " at " + row.time + " (" + row.reason + ")");

            markReminded(row.appointmentId);

            String adminMsg   = "⏰  " + row.patientName + " ↔ Dr. " + row.doctorName + " at " + row.time;
            String doctorMsg  = "⏰  Next: " + row.patientName + " at " + row.time + " — " + row.reason;
            String patientMsg = "⏰  Appointment with Dr. " + row.doctorName + " at " + row.time;

            if (adminCallback != null && activeGroupId == row.groupId)
                runOnFx(() -> {
                    adminCallback.accept(adminMsg);
                    if ("admin".equals(activeRole))
                        Toast.warning("Appointment Reminder", adminMsg.substring(3));
                });

            if (doctorCallback != null && activeDoctorId == row.doctorId)
                runOnFx(() -> {
                    doctorCallback.accept(doctorMsg);
                    if ("doctor".equals(activeRole))
                        Toast.warning("Appointment Reminder", doctorMsg.substring(3));
                });

            if (patientCallback != null && activePatientId == row.patientId)
                runOnFx(() -> {
                    patientCallback.accept(patientMsg);
                    if ("patient".equals(activeRole))
                        Toast.warning("Appointment Reminder", patientMsg.substring(3));
                });
        }
    }

    private static void emitHourlySummary() {
        if (adminCallback == null || activeGroupId < 0) return;
        int count = countTodayConfirmed(activeGroupId);
        String msg = count == 0
                   ? "All clear — no more appointments today."
                   : count + " confirmed appointment(s) remaining today.";
        runOnFx(() -> { if (adminCallback != null) adminCallback.accept(msg); });
    }

    // ── DB helpers ────────────────────────────────────────────

    private static List<ReminderRow> queryUpcoming() {
        List<ReminderRow> list = new ArrayList<>();
        String sql =
            "SELECT a.appointment_id, a.patient_id, a.doctor_id, "
          + "       a.appointment_time, a.reason, "
          + "       up.name AS patient_name, up.user_id AS patient_user_id, "
          + "       ud.name AS doctor_name,  ud.user_id AS doctor_user_id, "
          + "       ud.group_id "
          + "FROM   Appointment a "
          + "JOIN   Patient p   ON a.patient_id = p.patient_id "
          + "JOIN   User    up  ON p.user_id    = up.user_id "
          + "JOIN   Doctor  d   ON a.doctor_id  = d.doctor_id "
          + "JOIN   User    ud  ON d.user_id    = ud.user_id "
          + "WHERE  a.appointment_date = CURDATE() "
          + "AND    a.status           = 'confirmed' "
          + "AND    a.is_archived      = 0 "
          + "AND    (a.reminded IS NULL OR a.reminded = 0) "
          + "AND    a.appointment_time BETWEEN CURTIME() "
          + "                              AND ADDTIME(CURTIME(), '00:30:00')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ReminderRow row = new ReminderRow();
                row.appointmentId = rs.getInt("appointment_id");
                row.patientId     = rs.getInt("patient_id");
                row.doctorId      = rs.getInt("doctor_id");
                row.time          = rs.getTime("appointment_time").toLocalTime()
                                      .format(DateTimeFormatter.ofPattern("HH:mm"));
                row.reason        = rs.getString("reason");
                row.patientName   = rs.getString("patient_name");
                row.patientUserId = rs.getInt("patient_user_id");
                row.doctorName    = rs.getString("doctor_name");
                row.doctorUserId  = rs.getInt("doctor_user_id");
                row.groupId       = rs.getInt("group_id");
                list.add(row);
            }
        } catch (Exception ignored) {}
        return list;
    }

    /**
     * Persist a notification for a user.
     * PUBLIC so AppointmentService (and others) can send instant notifications.
     */
    public static void persistNotification(int userId, String title, String message) {
        String sql = "INSERT INTO Notification (user_id, title, message, is_read, created_at) "
                   + "VALUES (?, ?, ?, 0, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, message);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void markReminded(int appointmentId) {
        String sql = "UPDATE Appointment SET reminded = 1 WHERE appointment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private static List<Integer> getGroupAdminUserIds(int groupId) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT u.user_id FROM User u "
                   + "JOIN Admin a ON u.user_id = a.user_id WHERE u.group_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("user_id"));
        } catch (Exception ignored) {}
        return ids;
    }

    private static int countTodayConfirmed(int groupId) {
        String sql = "SELECT COUNT(*) FROM Appointment a "
                   + "JOIN Doctor d ON a.doctor_id = d.doctor_id "
                   + "JOIN User   u ON d.user_id   = u.user_id "
                   + "WHERE u.group_id = ? AND a.appointment_date = CURDATE() "
                   + "AND a.status = 'confirmed' AND a.is_archived = 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        return 0;
    }

    // ── Schema bootstrap ──────────────────────────────────────

    private static synchronized void ensureSchema() {
        if (schemaReady) return;
        schemaReady = true;
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Notification ("
              + "  notification_id INT AUTO_INCREMENT PRIMARY KEY, "
              + "  user_id INT NOT NULL, "
              + "  title VARCHAR(100) NOT NULL, "
              + "  message TEXT NOT NULL, "
              + "  is_read TINYINT(1) NOT NULL DEFAULT 0, "
              + "  created_at DATETIME NOT NULL, "
              + "  FOREIGN KEY (user_id) REFERENCES User(user_id) ON DELETE CASCADE"
              + ")"
            );

            // Add reminded column if missing (upgrade from v1)
            try (ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS "
                  + "WHERE TABLE_SCHEMA = DATABASE() "
                  + "AND TABLE_NAME='Appointment' AND COLUMN_NAME='reminded'")) {
                if (rs.next() && rs.getInt(1) == 0)
                    st.executeUpdate(
                        "ALTER TABLE Appointment ADD COLUMN reminded TINYINT(1) NOT NULL DEFAULT 0");
            }

            // Add is_archived to Appointment if missing
            try (ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS "
                  + "WHERE TABLE_SCHEMA = DATABASE() "
                  + "AND TABLE_NAME='Appointment' AND COLUMN_NAME='is_archived'")) {
                if (rs.next() && rs.getInt(1) == 0)
                    st.executeUpdate(
                        "ALTER TABLE Appointment ADD COLUMN is_archived TINYINT(1) NOT NULL DEFAULT 0");
            }

            // Add is_archived to Medical_Record if missing
            try (ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS "
                  + "WHERE TABLE_SCHEMA = DATABASE() "
                  + "AND TABLE_NAME='Medical_Record' AND COLUMN_NAME='is_archived'")) {
                if (rs.next() && rs.getInt(1) == 0)
                    st.executeUpdate(
                        "ALTER TABLE Medical_Record ADD COLUMN is_archived TINYINT(1) NOT NULL DEFAULT 0");
            }

            // Create Specialty table if missing
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Specialty ("
              + "  specialty_id INT AUTO_INCREMENT PRIMARY KEY, "
              + "  name VARCHAR(100) NOT NULL, "
              + "  group_id INT NOT NULL, "
              + "  UNIQUE KEY uq_specialty_group (name, group_id)"
              + ")"
            );

        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Notification API (used by NotificationController) ─────

    public static List<NotificationRow> getUnread(int userId, int limit) {
        List<NotificationRow> list = new ArrayList<>();
        String sql = "SELECT notification_id, title, message, created_at FROM Notification "
                   + "WHERE user_id = ? AND is_read = 0 ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                NotificationRow n = new NotificationRow();
                n.id        = rs.getInt("notification_id");
                n.title     = rs.getString("title");
                n.message   = rs.getString("message");
                n.createdAt = rs.getTimestamp("created_at").toLocalDateTime();
                list.add(n);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public static int countUnread(int userId) {
        String sql = "SELECT COUNT(*) FROM Notification WHERE user_id = ? AND is_read = 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        return 0;
    }

    public static void markAllRead(int userId) {
        String sql = "UPDATE Notification SET is_read = 1 WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    public static void markRead(int notificationId) {
        String sql = "UPDATE Notification SET is_read = 1 WHERE notification_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    // ── Helpers ───────────────────────────────────────────────

    private static void runOnFx(Runnable r) {
        try { javafx.application.Platform.runLater(r); }
        catch (IllegalStateException ignored) {}
    }

    // ── DTOs ──────────────────────────────────────────────────

    private static class ReminderRow {
        int appointmentId, patientId, doctorId, patientUserId, doctorUserId, groupId;
        String time, reason, patientName, doctorName;
    }

    public static class NotificationRow {
        public int           id;
        public String        title, message;
        public LocalDateTime createdAt;
    }
}