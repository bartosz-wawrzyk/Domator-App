package pl.domator.Config;

import pl.domator.Core.LoggerUtils;
import pl.domator.Security.EncryptionUtils;

import java.io.FileInputStream;
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

        try (InputStream input = new FileInputStream("database.properties")) {
            props.load(input);
        } catch (IOException e) {
            LoggerUtils.logMessage("Nie można otworzyć pliku database.properties: " + e.getMessage());
            throw new SQLException("Brak konfiguracji połączenia.");
        }

        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String encryptedPassword = props.getProperty("db.password");
        String password = EncryptionUtils.decrypt(encryptedPassword);

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            LoggerUtils.logMessage("Brak sterownika PostgreSQL JDBC");
        }

        connection = DriverManager.getConnection(url, user, password);
        return connection;
    }

}