package pl.domator.config;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import pl.domator.core.DatabaseInitializer;
import pl.domator.security.EncryptionUtils;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DBConfigController {

    @FXML private TextArea currentConfigArea;
    @FXML private TextField serverField, portField, dbNameField, userField;
    @FXML private PasswordField passwordField;
    @FXML private Button saveButton, cancelButton;
    @FXML private Label statusLabel;

    private boolean editing = false;
    private Properties currentProps;

    private static final String FILE_PATH = "src/main/resources/database.properties";

    @FXML
    public void initialize() {
        loadConfig();
        updateCurrentView();
        enableFields(false);
    }

    private void loadConfig() {
        try (InputStream input = new FileInputStream(FILE_PATH)) {
            currentProps = new Properties();
            currentProps.load(input);
        } catch (IOException e) {
            currentProps = null;
        }
    }

    private void updateCurrentView() {
        if (currentProps == null) {
            currentConfigArea.setText("Brak zapisanej konfiguracji.");
            return;
        }

        String url = currentProps.getProperty("db.url", "");
        String user = currentProps.getProperty("db.user", "");

        try {
            String afterJdbc = url.substring("jdbc:postgresql://".length());
            String[] parts = afterJdbc.split("/");
            String hostPort = parts[0];
            String dbName = parts.length > 1 ? parts[1] : "";

            String host = hostPort.contains(":") ? hostPort.split(":")[0] : hostPort;
            String port = hostPort.contains(":") ? hostPort.split(":")[1] : "5432";

            currentConfigArea.setText(
                    "Serwer: " + host + "\n" +
                            "Port: " + port + "\n" +
                            "Baza: " + dbName + "\n" +
                            "Użytkownik: " + user + "\n" +
                            "Hasło: ********"
            );
        } catch (Exception e) {
            currentConfigArea.setText("Nieprawidłowa konfiguracja URL.");
        }
    }

    private void enableFields(boolean enable) {
        serverField.setDisable(!enable);
        portField.setDisable(!enable);
        dbNameField.setDisable(!enable);
        userField.setDisable(!enable);
        passwordField.setDisable(!enable);
        saveButton.setDisable(!enable);
        cancelButton.setDisable(!enable);
    }

    private void clearFields() {
        serverField.clear();
        portField.clear();
        dbNameField.clear();
        userField.clear();
        passwordField.clear();
    }

    @FXML
    public void onAdd() {
        enableFields(true);
        clearFields();
        portField.setText("5432");
        editing = true;
        statusLabel.setText("Tryb dodawania nowej konfiguracji.");
    }

    @FXML
    public void onEdit() {
        if (currentProps == null) {
            statusLabel.setText("Brak konfiguracji do edycji.");
            return;
        }

        enableFields(true);
        editing = true;

        String url = currentProps.getProperty("db.url", "");
        if (url.contains("//")) {
            String afterJdbc = url.substring(url.indexOf("//") + 2);
            String hostPart = afterJdbc.substring(0, afterJdbc.indexOf("/"));
            String db = afterJdbc.substring(afterJdbc.indexOf("/") + 1);

            if (hostPart.contains(":")) {
                String[] parts = hostPart.split(":");
                serverField.setText(parts[0]);
                portField.setText(parts[1]);
            } else {
                serverField.setText(hostPart);
                portField.setText("5432");
            }
            dbNameField.setText(db);
        }

        userField.setText(currentProps.getProperty("db.user", ""));
        passwordField.setText(EncryptionUtils.decrypt(currentProps.getProperty("db.password", "")));
        statusLabel.setText("Edycja konfiguracji.");
    }

    @FXML
    public void onSave() {
        try {
            String server = serverField.getText().trim();
            String port = portField.getText().trim();
            String db = dbNameField.getText().trim();
            String user = userField.getText().trim();
            String pass = passwordField.getText().trim();

            if (server.isEmpty() || db.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                statusLabel.setText("Wszystkie pola muszą być wypełnione.");
                return;
            }

            if (!server.matches("(localhost|\\d+\\.\\d+\\.\\d+\\.\\d+)")) {
                statusLabel.setText("Adres serwera musi być localhost lub IP.");
                return;
            }

            if (!port.matches("\\d{2,5}")) {
                statusLabel.setText("Nieprawidłowy port.");
                return;
            }

            String url = "jdbc:postgresql://" + server + ":" + port + "/" + db;

            Properties p = new Properties();
            p.setProperty("db.url", url);
            p.setProperty("db.user", user);
            p.setProperty("db.password", EncryptionUtils.encrypt(pass));

            try (OutputStream out = new FileOutputStream(FILE_PATH)) {
                p.store(out, "Database configuration");
            }

            currentProps = p;
            updateCurrentView();
            enableFields(false);
            editing = false;
            statusLabel.setText("Konfiguracja zapisana pomyślnie.");
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Błąd zapisu konfiguracji: " + e.getMessage());
        }
    }

    @FXML
    public void onCancel() {
        enableFields(false);
        clearFields();
        editing = false;
        statusLabel.setText("Anulowano edycję.");
    }

    @FXML
    public void onDelete() {
        File file = new File(FILE_PATH);
        if (file.exists()) file.delete();
        currentProps = null;
        updateCurrentView();
        statusLabel.setText("Konfiguracja usunięta.");
    }

    @FXML
    public void onTestConnection() {
        try {
            if (currentProps == null) {
                statusLabel.setText("Brak zapisanej konfiguracji.");
                return;
            }

            String url = currentProps.getProperty("db.url", "");
            String user = currentProps.getProperty("db.user", "");
            String pass = EncryptionUtils.decrypt(currentProps.getProperty("db.password", ""));

            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                if (conn != null && !conn.isClosed()) {
                    statusLabel.setText("✅ Połączenie z bazą OK!");
                } else {
                    statusLabel.setText("❌ Brak połączenia z bazą.");
                }
            }
        } catch (Exception e) {
            statusLabel.setText("❌ Błąd połączenia: " + e.getMessage());
        }
    }

    @FXML
    public void onInitDatabase() {
        try {
            DatabaseInitializer.initializeDatabase();
            statusLabel.setText("✅ Schemat i tabele utworzone.");
        } catch (Exception e) {
            statusLabel.setText("❌ Błąd inicjalizacji: " + e.getMessage());
        }
    }
}