package pl.domator.modules.ui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class DatabaseUpdateWindow {

    private final Stage stage;
    private final Label messageLabel;

    public DatabaseUpdateWindow() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Aktualizacja bazy danych");

        messageLabel = new Label("Trwa weryfikacja wersji bazy danych, proszę czekać...");
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(50, 50);

        VBox root = new VBox(20, messageLabel, progress);
        root.setAlignment(Pos.CENTER);
        root.setPrefSize(400, 150);

        stage.setScene(new Scene(root));
    }

    public void show() {
        stage.show();
    }

    public void close() {
        stage.close();
    }

    public void setMessage(String message) {
        javafx.application.Platform.runLater(() -> messageLabel.setText(message));
    }
}