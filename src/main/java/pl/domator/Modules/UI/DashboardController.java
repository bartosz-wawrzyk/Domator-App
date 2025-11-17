package pl.domator.Modules.UI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pl.domator.Config.DBConnectionController;
import pl.domator.Core.LoggerUtils;
import pl.domator.Core.AlertUtils;
import pl.domator.Security.PasswordUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DashboardController {

    public Label upcomingPaymentsLabel;
    @FXML private StackPane mainContent;

    private int userId;

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @FXML
    private void initialize() {

    }

    private void openWindow(String fxmlPath, String title, String errorMessage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof UserAware) {
                ((UserAware) controller).setUserId(userId);
            }

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.setResizable(false);

            Stage ownerStage = (Stage) mainContent.getScene().getWindow();
            stage.initOwner(ownerStage);
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.showAndWait();

        } catch (Exception e) {
            LoggerUtils.logError(e);
            if (errorMessage != null) {
                AlertUtils.showError("Błąd", errorMessage);
            }
        }
    }

    @FXML
    private void openLoanVerification() {
        openWindow("/pl/domator/fxml/Finances/Credits.fxml",
                "Kredyty i historia wpłat",
                "Nie można otworzyć modułu szczegółów kredytów użytkownika");
    }

    @FXML
    private void openCardVehicle() {
        openWindow("/pl/domator/fxml/Vehicle/VehicleCard.fxml",
                "Karta pojazdu",
                "Nie można otworzyć karty pojazdu");
    }

    @FXML
    public void openVehicleTechnicalInspection() {
        openWindow("/pl/domator/fxml/Vehicle/VehicleTechnicalInspections.fxml",
                "Badanie techniczne",
                "Nie można otworzyć badań technicznych");
    }

    @FXML
    public void openVehicleInsurancePolicies() {
        openWindow("/pl/domator/fxml/Vehicle/VehicleInsurancePolicies.fxml",
                "Polisa OC",
                "Nie można otworzyć polis OC");
    }

    @FXML
    public void openVehicleServiceHistory() {
        openWindow("/pl/domator/fxml/Vehicle/VehicleServiceHistory.fxml",
                "Historia serwisów",
                "Nie można otworzyć historii serwisów");
    }

    @FXML
    private void openBillsManagement() {
        openWindow("/pl/domator/fxml/bills_management.fxml",
                "Zarządzanie rachunkami",
                "Nie można otworzyć modułu rachunków");
    }

    @FXML
    private void openAllDeadlines() {
        openWindow("/pl/domator/fxml/all_deadlines.fxml",
                "Wszystkie nadchodzące terminy",
                "Nie można otworzyć przeglądu terminów");
    }

    @FXML
    private void verifyAllDeadlines() {
        openAllDeadlines();
    }

    public void openAddDish() {
        openWindow("/pl/domator/fxml/DishView.fxml",
                "Moje dania",
                "Nie można otworzyć okna dodawania dań");
    }

    public void openCategoryManager() {
        openWindow("/pl/domator/fxml/MealPlanner/CategoryManager.fxml",
                "Zarządzanie kategoriami",
                "Nie można otworzyć zarządzania kategoriami");
    }

    @FXML
    public void changePassword() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Zmień hasło");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Nowe hasło");

        PasswordField passwordConfirmField = new PasswordField();
        passwordConfirmField.setPromptText("Potwierdź hasło");

        Label passwordError = new Label();
        passwordError.setStyle("-fx-text-fill: #c0392b;");

        Label passwordConfirmError = new Label();
        passwordConfirmError.setStyle("-fx-text-fill: #c0392b;");

        Button submit = new Button("Zmień hasło");
        submit.setOnAction(e -> {
            boolean valid = true;

            if (passwordField.getText().length() < 6) {
                setError(passwordField, passwordError, "Hasło musi mieć co najmniej 6 znaków.");
                valid = false;
            } else {
                clearError(passwordField, passwordError);
            }

            if (!passwordField.getText().equals(passwordConfirmField.getText())) {
                setError(passwordConfirmField, passwordConfirmError, "Hasła nie są zgodne.");
                valid = false;
            } else {
                clearError(passwordConfirmField, passwordConfirmError);
            }

            if (!valid) {
                AlertUtils.showError("Błąd", "Popraw błędy w formularzu.");
                return;
            }

            try (Connection conn = DBConnectionController.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE dmt.users SET password_hash=? WHERE id=?"
                );
                ps.setString(1, PasswordUtils.hashPassword(passwordField.getText()));
                ps.setInt(2, userId);
                ps.executeUpdate();

                AlertUtils.showInfo("Sukces", "Hasło zostało zmienione.");
                dialog.close();
            } catch (SQLException ex) {
                LoggerUtils.logError(ex);
                AlertUtils.showError("Błąd", "Nie udało się zmienić hasła.");
            }
        });

        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setStyle("-fx-padding:20;");
        grid.add(new Label("Nowe hasło:"), 0, 0);
        grid.add(passwordField, 1, 0);
        grid.add(passwordError, 1, 1);
        grid.add(new Label("Potwierdź hasło:"), 0, 2);
        grid.add(passwordConfirmField, 1, 2);
        grid.add(passwordConfirmError, 1, 3);
        grid.add(submit, 1, 4);

        Scene scene = new Scene(grid, 350, 200);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
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
    public void Logout() {
        boolean confirm = AlertUtils.showConfirmation("Wylogowanie", "Czy na pewno chcesz się wylogować?");
        if (confirm) {
            try {
                Stage stage = (Stage) mainContent.getScene().getWindow();
                stage.close();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/pl/domator/fxml/Login.fxml"));
                Parent root = loader.load();

                Stage loginStage = new Stage();
                loginStage.setScene(new Scene(root));
                loginStage.setTitle("Logowanie");
                loginStage.setResizable(false);
                loginStage.show();

            } catch (Exception e) {
                LoggerUtils.logError(e);
                AlertUtils.showError("Błąd", "Nie można otworzyć ekranu logowania.");
            }
        }
    }

    @FXML
    public void exitApp() {
        boolean confirm = AlertUtils.showConfirmation("Zamknij aplikację", "Czy na pewno chcesz zamknąć aplikację?");
        if (confirm) {
            System.exit(0);
        }
    }

    public interface UserAware {
        void setUserId(int userId);
    }
}
