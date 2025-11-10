package pl.domator.modules.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import pl.domator.core.LoggerUtils;

import java.net.URL;

public class DashboardController {

    @FXML private StackPane mainContent;
    @FXML private Label upcomingPaymentsLabel;

    private int userId;

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @FXML
    private void initialize() {

    }

    private void openWindow(String fxmlPath, String title, String errorMessage) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                showAlert("Menu niedostępne", "Ten moduł jest niedostępny w tym momencie.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof UserAware) {
                ((UserAware) controller).setUserId(userId);
            }

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.setResizable(false);
            stage.show();

        } catch (Exception e) {
            LoggerUtils.logError(e);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void openLoanVerification() {
        openWindow("/pl/domator/fxml/credits.fxml",
                "Kredyty i historia wpłat",
                "Nie można otworzyć modułu szczegółów kredytów użytkownika");
    }

    @FXML
    private void openCarMaintenance() {
        openWindow("/pl/domator/fxml/car_maintenance.fxml",
                "Przeglądy i ubezpieczenia samochodu",
                "Nie można otworzyć modułu samochodu");
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
    private void openSettings() {
        openWindow("/pl/domator/fxml/settings.fxml",
                "Ustawienia",
                "Nie można otworzyć ustawień");
    }

    @FXML
    private void openMealPlanner() {
        openWindow("/pl/domator/fxml/meal_planner.fxml",
                "Planer posiłków",
                "Nie można otworzyć planera posiłków");
    }

    @FXML
    private void verifyAllDeadlines() {
        openAllDeadlines();
    }

    public interface UserAware {
        void setUserId(int userId);
    }
}
