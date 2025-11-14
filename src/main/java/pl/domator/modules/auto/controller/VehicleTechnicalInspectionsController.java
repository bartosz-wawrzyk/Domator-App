package pl.domator.modules.auto.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pl.domator.config.DBConnectionController;
import pl.domator.core.AlertUtils;
import pl.domator.core.CrudButtonsController;
import pl.domator.core.CrudController;
import pl.domator.core.LoggerUtils;
import pl.domator.modules.auto.model.TechnicalInspection;
import pl.domator.modules.auto.model.Vehicle;
import pl.domator.modules.ui.DashboardController;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

public class VehicleTechnicalInspectionsController implements DashboardController.UserAware, CrudController {

    @FXML private VBox mainVBox;
    @FXML private ComboBox<Vehicle> vehicleComboBox;
    @FXML private TableView<TechnicalInspection> inspectionTable;
    @FXML private TableColumn<TechnicalInspection, LocalDate> colDate;
    @FXML private TableColumn<TechnicalInspection, LocalDate> colValidUntil;
    @FXML private TableColumn<TechnicalInspection, Integer> colMileage;
    @FXML private TableColumn<TechnicalInspection, Double> colCost;
    @FXML private TableColumn<TechnicalInspection, String> colNotes;

    @FXML private DatePicker inspectionDatePicker;
    @FXML private DatePicker validUntilPicker;
    @FXML private TextField mileageField;
    @FXML private TextField costField;
    @FXML private TextArea notesArea;

    private ObservableList<TechnicalInspection> inspections = FXCollections.observableArrayList();
    private Vehicle selectedVehicle = null;
    private TechnicalInspection selectedInspection = null;

    private enum Mode { NONE, ADD, EDIT }
    private Mode currentMode = Mode.NONE;

    private CrudButtonsController crudButtons;
    private int userId;

    @FXML
    private void initialize() {
        colDate.setCellValueFactory(c -> c.getValue().inspectionDateProperty());
        colValidUntil.setCellValueFactory(c -> c.getValue().validUntilProperty());
        colMileage.setCellValueFactory(c -> c.getValue().mileageProperty().asObject());
        colCost.setCellValueFactory(c -> c.getValue().costProperty().asObject());
        colNotes.setCellValueFactory(c -> c.getValue().notesProperty());
        inspectionTable.setItems(inspections);

        inspectionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selectedInspection = newSel;
            if (currentMode == Mode.NONE) populateFormFromSelection();
        });

        setFormDisabled(true);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pl/domator/fxml/CrudButtons.fxml"));
            HBox crudBox = loader.load();
            crudButtons = loader.getController();
            crudButtons.setHandler(this);

            mainVBox.getChildren().add(3, crudBox);
        } catch (IOException e) {
            LoggerUtils.logError(e);
        }

        vehicleComboBox.setOnAction(e -> loadInspectionsForSelectedVehicle());
    }

    public void setUserId(int userId) {
        this.userId = userId;
        loadVehicles();
    }

    private void loadVehicles() {
        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM dmt.vehicles WHERE user_id = ? ORDER BY id DESC")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            ObservableList<Vehicle> vehicleList = FXCollections.observableArrayList();
            while (rs.next()) {
                Vehicle v = new Vehicle(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("brand"),
                        rs.getString("model"),
                        rs.getInt("production_year"),
                        rs.getString("registration_number"),
                        rs.getString("vin"),
                        rs.getString("fuel_type"),
                        rs.getInt("current_mileage"),
                        rs.getString("notes")
                );
                vehicleList.add(v);
            }
            vehicleComboBox.setItems(vehicleList);
        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się wczytać pojazdów.");
        }
    }

    private void loadInspectionsForSelectedVehicle() {
        selectedVehicle = vehicleComboBox.getSelectionModel().getSelectedItem();
        inspections.clear();
        if (selectedVehicle == null) return;

        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM dmt.vehicle_technical_inspections WHERE vehicle_id=? ORDER BY inspection_date DESC")) {
            ps.setInt(1, selectedVehicle.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TechnicalInspection ti = new TechnicalInspection(
                        rs.getInt("id"),
                        rs.getInt("vehicle_id"),
                        rs.getDate("inspection_date").toLocalDate(),
                        rs.getDate("valid_until").toLocalDate(),
                        rs.getInt("mileage_at_inspection"),
                        rs.getDouble("cost"),
                        rs.getString("notes")
                );
                inspections.add(ti);
            }
        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się wczytać danych.");
        }
    }

    private void populateFormFromSelection() {
        if (selectedInspection == null) {
            clearForm();
            return;
        }
        inspectionDatePicker.setValue(selectedInspection.getInspectionDate());
        validUntilPicker.setValue(selectedInspection.getValidUntil());
        mileageField.setText(String.valueOf(selectedInspection.getMileage()));
        costField.setText(String.valueOf(selectedInspection.getCost()));
        notesArea.setText(selectedInspection.getNotes());
    }

    private void clearForm() {
        inspectionDatePicker.setValue(null);
        validUntilPicker.setValue(null);
        mileageField.clear();
        costField.clear();
        notesArea.clear();
    }

    private void setFormDisabled(boolean disabled) {
        inspectionDatePicker.setDisable(disabled);
        validUntilPicker.setDisable(disabled);
        mileageField.setDisable(disabled);
        costField.setDisable(disabled);
        notesArea.setDisable(disabled);
    }

    private boolean validateForm() {
        if (inspectionDatePicker.getValue() == null || validUntilPicker.getValue() == null) {
            AlertUtils.showError("Błąd walidacji", "Daty badania i ważności muszą być uzupełnione.");
            return false;
        }
        try {
            if (!mileageField.getText().isEmpty()) Integer.parseInt(mileageField.getText());
            if (!costField.getText().isEmpty()) Double.parseDouble(costField.getText());
        } catch (NumberFormatException e) {
            AlertUtils.showError("Błąd walidacji", "Przebieg i koszt muszą być liczbami.");
            return false;
        }
        return true;
    }

    @Override
    public void onAdd() {
        currentMode = Mode.ADD;
        selectedInspection = null;
        clearForm();
        setFormDisabled(false);
        inspectionDatePicker.requestFocus();
        if (crudButtons != null) crudButtons.enterEditingMode();
    }

    @Override
    public void onEdit() {
        if (selectedInspection == null) {
            AlertUtils.showError("Brak wyboru", "Proszę wybrać wpis do edycji.");
            return;
        }
        currentMode = Mode.EDIT;
        setFormDisabled(false);
        if (crudButtons != null) crudButtons.enterEditingMode();
    }

    @Override
    public void onDelete() {
        if (selectedInspection == null) return;
        boolean confirmed = AlertUtils.showConfirmation("Potwierdzenie", "Usunąć wybrany wpis?");
        if (!confirmed) return;
        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM dmt.vehicle_technical_inspections WHERE id=?")) {
            ps.setInt(1, selectedInspection.getId());
            ps.executeUpdate();
            loadInspectionsForSelectedVehicle();
            clearForm();
        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się usunąć wpisu.");
        }
    }

    @Override
    public void onSave() {
        if (!validateForm() || selectedVehicle == null) return;

        LocalDate date = inspectionDatePicker.getValue();
        LocalDate validUntil = validUntilPicker.getValue();
        int mileage = mileageField.getText().isEmpty() ? 0 : Integer.parseInt(mileageField.getText());
        double cost = costField.getText().isEmpty() ? 0 : Double.parseDouble(costField.getText());
        String notes = notesArea.getText();

        try (Connection conn = DBConnectionController.getConnection()) {
            if (currentMode == Mode.ADD) {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO dmt.vehicle_technical_inspections(vehicle_id, inspection_date, valid_until, mileage_at_inspection, cost, notes) VALUES (?,?,?,?,?,?)");
                ps.setInt(1, selectedVehicle.getId());
                ps.setDate(2, Date.valueOf(date));
                ps.setDate(3, Date.valueOf(validUntil));
                ps.setInt(4, mileage);
                ps.setDouble(5, cost);
                ps.setString(6, notes);
                ps.executeUpdate();
            } else if (currentMode == Mode.EDIT && selectedInspection != null) {
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE dmt.vehicle_technical_inspections SET inspection_date=?, valid_until=?, mileage_at_inspection=?, cost=?, notes=? WHERE id=?");
                ps.setDate(1, Date.valueOf(date));
                ps.setDate(2, Date.valueOf(validUntil));
                ps.setInt(3, mileage);
                ps.setDouble(4, cost);
                ps.setString(5, notes);
                ps.setInt(6, selectedInspection.getId());
                ps.executeUpdate();
            }

            currentMode = Mode.NONE;
            setFormDisabled(true);
            if (crudButtons != null) crudButtons.resetButtons();
            loadInspectionsForSelectedVehicle();
            clearForm();
        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się zapisać wpisu.");
        }
    }

    @Override
    public void onCancel() {
        currentMode = Mode.NONE;
        setFormDisabled(true);
        if (crudButtons != null) crudButtons.resetButtons();
        populateFormFromSelection();
    }
}
