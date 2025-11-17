package pl.domator.Core;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import java.util.Optional;

public class AlertUtils {

    public static void showError(String title, String message) {
        show(Alert.AlertType.ERROR, title, message, "OK");
    }

    public static void showInfo(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message, "OK");
    }

    public static void showWarning(String title, String message) {
        show(Alert.AlertType.WARNING, title, message, "OK");
    }

    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        ButtonType yes = new ButtonType("Tak", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Nie", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(yes, no);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yes;
    }

    private static void show(Alert.AlertType type, String title, String message, String buttonText) {
        Alert alert = new Alert(type, message, new ButtonType(buttonText, ButtonBar.ButtonData.OK_DONE));
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}