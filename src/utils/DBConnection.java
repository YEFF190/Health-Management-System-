package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection — provides a raw JDBC connection.
 * Throws RuntimeException on failure so callers never receive null.
 *
 * For production consider replacing with HikariCP connection pool.
 */
public class DBConnection {

    private static final String URL      = "jdbc:mysql://localhost:3306/health_system"
                                         + "?useSSL=false&allowPublicKeyRetrieval=true"
                                         + "&serverTimezone=UTC";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot connect to database: " + e.getMessage(), e);
        }
    }
}
