package pl.domator.modules.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import pl.domator.config.DBConnectionController;
import pl.domator.core.AlertUtils;
import pl.domator.core.LoggerUtils;
import pl.domator.security.PasswordUtils;

import java.sql.*;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private Label usernameError;

    @FXML private PasswordField passwordField;
    @FXML private PasswordField passwordConfirmField;

    @FXML private Label passwordError;
    @FXML private Label passwordConfirmError;

    @FXML
    public void initialize() {
        usernameField.textProperty().addListener((obs, o, n) -> validateUsername());
        passwordField.textProperty().addListener((obs, o, n) -> validatePassword());
        passwordConfirmField.textProperty().addListener((obs, o, n) -> validateConfirmPassword());
    }

    private boolean validateUsername() {
        String u = usernameField.getText().trim();

        if (u.isEmpty()) {
            setError(usernameField, usernameError, "Login nie może być pusty.");
            return false;
        }

        if (u.contains(" ")) {
            setError(usernameField, usernameError, "Login nie może zawierać spacji.");
            return false;
        }

        clearError(usernameField, usernameError);
        return true;
    }

    private boolean validatePassword() {
        String p = passwordField.getText();

        if (p.length() < 6) {
            setError(passwordField, passwordError, "Hasło musi mieć co najmniej 6 znaków.");
            return false;
        }

        clearError(passwordField, passwordError);
        validateConfirmPassword();
        return true;
    }

    private boolean validateConfirmPassword() {
        String p1 = passwordField.getText();
        String p2 = passwordConfirmField.getText();

        if (!p1.equals(p2)) {
            setError(passwordConfirmField, passwordConfirmError, "Hasła nie są zgodne.");
            return false;
        }

        clearError(passwordConfirmField, passwordConfirmError);
        return true;
    }

    private void setError(Control control, Label label, String message) {
        control.setStyle("-fx-border-color: #c0392b;");
        label.setText(message);
    }

    private void clearError(Control control, Label label) {
        control.setStyle("-fx-border-color: #dcd8d3;");
        label.setText("");
    }

    @FXML
    public void handleRegister() {
        if (!validateUsername() | !validatePassword() | !validateConfirmPassword()) {
            AlertUtils.showError("Błąd", "Popraw błędy w formularzu.");
            return;
        }

        String username = usernameField.getText().trim();
        String pass = passwordField.getText();

        try (Connection conn = DBConnectionController.getConnection()) {
            PreparedStatement check = conn.prepareStatement(
                    "SELECT COUNT(*) FROM dmt.users WHERE username=?");
            check.setString(1, username);

            ResultSet rs = check.executeQuery();
            rs.next();

            if (rs.getInt(1) > 0) {
                AlertUtils.showError("Błąd", "Użytkownik o takim loginie już istnieje.");
                return;
            }

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO dmt.users (username, password_hash) VALUES (?, ?)");

            ps.setString(1, username);
            ps.setString(2, PasswordUtils.hashPassword(pass));
            ps.executeUpdate();

            AlertUtils.showInfo("Sukces", "Użytkownik został pomyślnie dodany.");

            closeWindow();

        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się zapisać użytkownika.");
        }
    }

    @FXML
    public void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }
}