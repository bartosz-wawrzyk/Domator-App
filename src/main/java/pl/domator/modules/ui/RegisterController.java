package pl.domator.modules.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import pl.domator.config.DBConnectionController;
import pl.domator.security.PasswordUtils;

import java.sql.*;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button registerButton;
    @FXML private Label statusLabel;

    @FXML
    public void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Login i hasło nie mogą być puste!");
            return;
        }

        try (Connection conn = DBConnectionController.getConnection()) {
            PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM dmt.users WHERE username=?");
            check.setString(1, username);
            ResultSet rs = check.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                statusLabel.setText("Użytkownik o takim loginie już istnieje!");
                return;
            }

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO dmt.users (username, password_hash) VALUES (?, ?)");
            ps.setString(1, username);
            ps.setString(2, PasswordUtils.hashPassword(password));
            ps.executeUpdate();

            statusLabel.setText("Użytkownik zarejestrowany pomyślnie.");

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Błąd przy rejestracji.");
        }
    }
}