package pl.domator.Config;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pl.domator.Core.AlertUtils;
import pl.domator.Core.CrudButtonsController;
import pl.domator.Security.EncryptionUtils;
import pl.domator.Core.DatabaseInitializer;
import pl.domator.Core.LoggerUtils;

import java.io.*;
import java.sql.*;
import java.util.Properties;

public class DBConfigController {

    @FXML private VBox mainVBox;

    @FXML private TextArea currentConfigArea;
    @FXML private TextField serverField, portField, dbNameField, userField;
    @FXML private PasswordField passwordField;

    private CrudButtonsController crudButtons;

    private boolean editing = false;
    private Properties currentProps;

    private static final String FILE_PATH = "database.properties";

    @FXML
    public void initialize() {

        loadConfig();
        updateCurrentView();
        enableFields(false);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pl/domator/fxml/Core/CrudButtons.fxml"));
            HBox crudBox = loader.load();
            crudButtons = loader.getController();
            crudButtons.setHandler(new CrudHandler());
            mainVBox.getChildren().add(3, crudBox);
        } catch (IOException e) {
            LoggerUtils.logError(e);
        }
    }

    private class CrudHandler implements pl.domator.Core.CrudController {

        @Override
        public void onAdd() {
            enableFields(true);
            clearFields();
            portField.setText("5432");

            editing = true;
            crudButtons.enterEditingMode();
        }

        @Override
        public void onEdit() {
            if (currentProps == null) {
                AlertUtils.showWarning("Brak konfiguracji", "Nie znaleziono istniejącej konfiguracji.");
                return;
            }

            enableFields(true);
            loadPropsToFields();

            editing = true;
            crudButtons.enterEditingMode();
        }

        @Override
        public void onDelete() {
            boolean ok = AlertUtils.showConfirmation(
                    "Potwierdzenie",
                    "Czy na pewno chcesz usunąć konfigurację?"
            );

            if (!ok) return;

            File file = new File(FILE_PATH);
            if (file.exists()) file.delete();

            currentProps = null;
            updateCurrentView();
        }

        @Override
        public void onSave() {

            boolean ok = AlertUtils.showConfirmation(
                    "Potwierdzenie",
                    "Czy na pewno chcesz zapisać konfigurację?"
            );

            if (!ok) return;
            if (!saveConfiguration()) return;

            editing = false;
            enableFields(false);
            crudButtons.resetButtons();
            updateCurrentView();
        }

        @Override
        public void onCancel() {
            if (editing) {
                boolean ok = AlertUtils.showConfirmation(
                        "Potwierdzenie",
                        "Masz niezapisane zmiany. Czy na pewno chcesz anulować?"
                );
                if (!ok) return;
            }

            enableFields(false);
            clearFields();
            updateCurrentView();
            crudButtons.resetButtons();
            editing = false;
        }
    }

    private void loadPropsToFields() {
        try {
            String url = currentProps.getProperty("db.url", "");
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
            userField.setText(currentProps.getProperty("db.user", ""));
            passwordField.setText(EncryptionUtils.decrypt(currentProps.getProperty("db.password", "")));
        } catch (Exception e) {
            LoggerUtils.logError(e);
        }
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

        try {
            String url = currentProps.getProperty("db.url", "");
            String user = currentProps.getProperty("db.user", "");

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
            currentConfigArea.setText("Nieprawidłowa konfiguracja.");
        }
    }

    private void enableFields(boolean enable) {
        serverField.setDisable(!enable);
        portField.setDisable(!enable);
        dbNameField.setDisable(!enable);
        userField.setDisable(!enable);
        passwordField.setDisable(!enable);
    }

    private void clearFields() {
        serverField.clear();
        portField.clear();
        dbNameField.clear();
        userField.clear();
        passwordField.clear();
    }

    private boolean saveConfiguration() {
        try {
            String server = serverField.getText().trim();
            String port = portField.getText().trim();
            String db = dbNameField.getText().trim();
            String user = userField.getText().trim();
            String pass = passwordField.getText().trim();

            if (server.isEmpty() || db.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                AlertUtils.showError("Błąd danych", "Wszystkie pola muszą być wypełnione.");
                return false;
            }

            if (!server.matches("(localhost|\\d+\\.\\d+\\.\\d+\\.\\d+)")) {
                AlertUtils.showError("Błędny adres", "Adres serwera musi być localhost lub IP.");
                return false;
            }

            if (!port.matches("\\d{2,5}")) {
                AlertUtils.showError("Błędny port", "Nieprawidłowy port.");
                return false;
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
            return true;

        } catch (Exception e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się zapisać konfiguracji.");
            return false;
        }
    }

    @FXML
    public void onTestConnection() {
        try {
            if (currentProps == null) {
                AlertUtils.showWarning("Brak konfiguracji", "Najpierw zapisz konfigurację.");
                return;
            }

            String url = currentProps.getProperty("db.url", "");
            String user = currentProps.getProperty("db.user", "");
            String pass = EncryptionUtils.decrypt(currentProps.getProperty("db.password", ""));

            try (Connection conn = DriverManager.getConnection(url, user, pass)) {

                if (conn != null && !conn.isClosed()) {
                    AlertUtils.showInfo("Połączenie OK", "Połączenie z bazą danych działa.");
                } else {
                    AlertUtils.showError("Brak połączenia", "Połączenie nie powiodło się.");
                }
            }
        } catch (Exception e) {
            AlertUtils.showError("Błąd połączenia", e.getMessage());
        }
    }

    @FXML
    public void onInitDatabase() {

        if (currentProps == null) {
            AlertUtils.showWarning("Brak konfiguracji", "Najpierw zapisz konfigurację.");
            return;
        }

        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT schema_name FROM information_schema.schemata WHERE schema_name='dmt'")) {

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                AlertUtils.showInfo("Informacja", "Schemat już istnieje.");
                return;
            }

        } catch (Exception e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się sprawdzić schematu.");
            return;
        }

        try {
            DatabaseInitializer.initializeDatabase();
            AlertUtils.showInfo("Sukces", "Schemat i tabele utworzone.");
        } catch (Exception e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd inicjalizacji", e.getMessage());
        }
    }
}
