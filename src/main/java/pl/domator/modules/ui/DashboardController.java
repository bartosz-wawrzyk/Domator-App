package pl.domator.modules.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pl.domator.core.LoggerUtils;
import pl.domator.core.AlertUtils;

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
        openWindow("/pl/domator/fxml/credits.fxml",
                "Kredyty i historia wpłat",
                "Nie można otworzyć modułu szczegółów kredytów użytkownika");
    }

    @FXML
    private void openCardVehicle() {
        openWindow("/pl/domator/fxml/car_card.fxml",
                "Karta pojazdu",
                "Nie można otworzyć karty pojazdu");
    }

    @FXML
    public void openVehicleTechnicalInspection() {
        openWindow("/pl/domator/fxml/VehicleTechnicalInspections.fxml",
                "Badanie techniczne",
                "Nie można otworzyć badań technicznych");
    }

    @FXML
    public void openVehicleInsurancePolicies() {
        openWindow("/pl/domator/fxml/VehicleInsurancePolicies.fxml",
                "Polisa OC",
                "Nie można otworzyć polis OC");
    }

    @FXML
    public void openVehicleServiceHistory() {
        openWindow("/pl/domator/fxml/VehicleServiceHistory.fxml",
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
