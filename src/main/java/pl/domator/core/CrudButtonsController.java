package pl.domator.core;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class CrudButtonsController {

    @FXML public Button addBtn;
    @FXML public Button editBtn;
    @FXML public Button deleteBtn;
    @FXML public Button saveBtn;
    @FXML public Button cancelBtn;

    private CrudController handler;

    public void setHandler(CrudController handler) {
        this.handler = handler;
        setupListeners();
    }

    private void setupListeners() {
        addBtn.setOnAction(e -> {
            if (handler != null) handler.onAdd();
            enterEditingMode();
        });

        editBtn.setOnAction(e -> {
            if (handler != null) handler.onEdit();
            enterEditingMode();
        });

        deleteBtn.setOnAction(e -> {
            if (handler != null) handler.onDelete();
        });

        saveBtn.setOnAction(e -> {
            if (handler != null) handler.onSave();
        });

        cancelBtn.setOnAction(e -> {
            if (handler != null) handler.onCancel();
        });
    }

    public void enterEditingMode() {
        addBtn.setDisable(true);
        editBtn.setDisable(true);
        deleteBtn.setDisable(true);
        saveBtn.setDisable(false);
        cancelBtn.setDisable(false);
    }

    public void resetButtons() {
        addBtn.setDisable(false);
        editBtn.setDisable(false);
        deleteBtn.setDisable(false);
        saveBtn.setDisable(true);
        cancelBtn.setDisable(true);
    }
}
