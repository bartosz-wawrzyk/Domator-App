package pl.domator.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnectionController {

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return connection;
        }

        Properties props = new Properties();
        try (InputStream input = DBConnectionController.class.getResourceAsStream("/database.properties")) {
            if (input == null) {
                throw new SQLException("Nie znaleziono pliku database.properties w resources");
            }
            props.load(input);
        } catch (IOException e) {
            throw new SQLException("Błąd podczas ładowania konfiguracji bazy danych", e);
        }

        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Brak sterownika PostgreSQL JDBC w classpath", e);
        }

        connection = DriverManager.getConnection(url, user, password);
        return connection;
    }
}
