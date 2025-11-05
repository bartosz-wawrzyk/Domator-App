package pl.domator.modules.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.net.URL;

public class DashboardController {

    @FXML private StackPane mainContent;
    @FXML private Label dashboardTitle;
    @FXML private Label upcomingPaymentsLabel;

    @FXML
    public void initialize() {
        // Uruchom scheduler przypomnień przy starcie dashboardu
        //PaymentReminderScheduler.startScheduler();
        //loadUpcomingPaymentsSummary();
    }

    @FXML
    public void openLoanVerification() {
        openWindow("/pl/domator/fxml/loan_verification.fxml",
                "Weryfikacja terminów rat kredytowych",
                "Nie można otworzyć modułu weryfikacji rat kredytowych");
    }

    @FXML
    public void openPaymentHistory() {
        openWindow("/pl/domator/fxml/payment_history.fxml",
                "Historia wpłat",
                "Nie można otworzyć historii wpłat");
    }

    @FXML
    public void openCarMaintenance() {
        openWindow("/pl/domator/fxml/car_maintenance.fxml",
                "Przeglądy i ubezpieczenia samochodu",
                "Nie można otworzyć modułu samochodu");
    }

    @FXML
    public void openBillsManagement() {
        openWindow("/pl/domator/fxml/bills_management.fxml",
                "Zarządzanie rachunkami",
                "Nie można otworzyć modułu rachunków");
    }

    @FXML
    public void openAllDeadlines() {
        openWindow("/pl/domator/fxml/all_deadlines.fxml",
                "Wszystkie nadchodzące terminy",
                "Nie można otworzyć przeglądu terminów");
    }

    @FXML
    public void openSettings() {
        openWindow("/pl/domator/fxml/settings.fxml",
                "Ustawienia",
                "Nie można otworzyć ustawień");
    }

    @FXML
    public void openMealPlanner() {
        openWindow("/pl/domator/fxml/meal_planner.fxml",
                "Planer posiłków",
                "Nie można otworzyć planera posiłków");
    }

    @FXML
    public void verifyAllDeadlines() {
        /*try {
            DeadlineChecker checker = new DeadlineChecker();
            String summary = checker.getAllUpcomingDeadlinesSummary();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Weryfikacja wszystkich terminów");
            alert.setHeaderText("Najbliższe terminy płatności i obowiązków:");
            alert.setContentText(summary);
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Błąd", "Nie udało się sprawdzić terminów");
        }*/
    }

    private void loadUpcomingPaymentsSummary() {
        /*try {
            DeadlineChecker checker = new DeadlineChecker();
            String summary = checker.getDashboardSummary();
            upcomingPaymentsLabel.setText(summary);
        } catch (Exception e) {
            upcomingPaymentsLabel.setText("Błąd ładowania danych");
        }*/
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void openWindow(String fxmlPath, String title, String errorMessage) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                showAlert("Menu niedostępne", "Ten moduł jest niedostępny w tym momencie.");
                return;
            }

            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(resource);
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(title);
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Błąd", errorMessage);
        }
    }

}
