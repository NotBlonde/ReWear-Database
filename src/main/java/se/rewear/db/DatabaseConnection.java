package se.rewear.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Simple connection factory for MySQL.
 * Reads from application.properties if present, else falls back to sane defaults.
 * Also respects environment variables DB_URL, DB_USER, DB_PASS if set.
 */
public class DatabaseConnection {

    private static final String DEFAULT_URL  =
            "jdbc:mysql://localhost:3306/rewear_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "Antligen150!";

    private static final Properties config = new Properties();

    static {
        // load application.properties if it exists
        try (InputStream in = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in != null) {
                config.load(in);
            }
        } catch (IOException ignored) { }
    }

    public static Connection getConnection() {
        try {
            String url  = getenvOrProp("DB_URL",  config.getProperty("db.url",  DEFAULT_URL));
            String user = getenvOrProp("DB_USER", config.getProperty("db.user", DEFAULT_USER));
            String pass = getenvOrProp("DB_PASS", config.getProperty("db.pass", DEFAULT_PASS));

            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", pass);
            props.setProperty("useSSL", "false");

            return DriverManager.getConnection(url, props);
        } catch (SQLException e) {
            System.err.println("DB connect failed: " + e.getMessage());
            return null;
        }
    }

    private static String getenvOrProp(String env, String fallback) {
        String v = System.getenv(env);
        return (v != null && !v.isBlank()) ? v : fallback;
    }
}
