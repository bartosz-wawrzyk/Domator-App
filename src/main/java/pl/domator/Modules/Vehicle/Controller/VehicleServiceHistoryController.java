package pl.domator.Modules.Vehicle.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pl.domator.Config.DBConnectionController;
import pl.domator.Core.AlertUtils;
import pl.domator.Core.CrudButtonsController;
import pl.domator.Core.CrudController;
import pl.domator.Core.LoggerUtils;
import pl.domator.Modules.Vehicle.Model.ServiceRecord;
import pl.domator.Modules.Vehicle.Model.Vehicle;
import pl.domator.Modules.UI.DashboardController;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

public class VehicleServiceHistoryController implements DashboardController.UserAware, CrudController {

    @FXML private VBox mainVBox;

    @FXML private ComboBox<Vehicle> vehicleComboBox;
    @FXML private TableView<ServiceRecord> serviceTable;
    @FXML private TableColumn<ServiceRecord, LocalDate> colServiceDate;
    @FXML private TableColumn<ServiceRecord, Integer> colMileage;
    @FXML private TableColumn<ServiceRecord, Double> colCost;
    @FXML private TableColumn<ServiceRecord, String> colDescription;

    @FXML private DatePicker serviceDatePicker;
    @FXML private TextField mileageField;
    @FXML private TextField costField;
    @FXML private TextArea descriptionArea;

    private CrudButtonsController crudButtons;

    private final ObservableList<ServiceRecord> records = FXCollections.observableArrayList();
    private Vehicle selectedVehicle;
    private ServiceRecord selectedRecord;

    private enum Mode { NONE, ADD, EDIT }
    private Mode currentMode = Mode.NONE;

    private int userId;

    @FXML
    private void initialize() {
        colServiceDate.setCellValueFactory(c -> c.getValue().serviceDateProperty());
        colMileage.setCellValueFactory(c -> c.getValue().mileageProperty().asObject());
        colCost.setCellValueFactory(c -> c.getValue().costProperty().asObject());
        colDescription.setCellValueFactory(c -> c.getValue().descriptionProperty());

        serviceTable.setItems(records);

        serviceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedRecord = newVal;
            if (currentMode == Mode.NONE) populateFormFromSelection();
        });

        setFormDisabled(true);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pl/domator/fxml/Core/CrudButtons.fxml"));
            HBox crudBox = loader.load();
            crudButtons = loader.getController();
            crudButtons.setHandler(this);
            mainVBox.getChildren().add(3, crudBox);
        } catch (IOException e) {
            LoggerUtils.logError(e);
        }

        vehicleComboBox.setOnAction(e -> loadRecords());
    }

    @Override
    public void setUserId(int userId) {
        this.userId = userId;
        loadVehicles();
    }

    private void loadVehicles() {
        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM dmt.vehicles WHERE user_id=? ORDER BY id DESC")) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            ObservableList<Vehicle> list = FXCollections.observableArrayList();
            while (rs.next()) {
                list.add(new Vehicle(
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
                ));
            }
            vehicleComboBox.setItems(list);

        } catch (SQLException e) {
            LoggerUtils.logError(e);
        }
    }

    private void loadRecords() {
        selectedVehicle = vehicleComboBox.getSelectionModel().getSelectedItem();
        records.clear();
        if (selectedVehicle == null) return;

        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM dmt.vehicle_service_history WHERE vehicle_id=? ORDER BY service_date DESC")) {

            ps.setInt(1, selectedVehicle.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                records.add(new ServiceRecord(
                        rs.getInt("id"),
                        rs.getInt("vehicle_id"),
                        rs.getDate("service_date").toLocalDate(),
                        rs.getInt("mileage_at_service"),
                        rs.getDouble("cost"),
                        rs.getString("description")
                ));
            }

        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się wczytać historii.");
        }
    }

    private void populateFormFromSelection() {
        if (selectedRecord == null) {
            clearForm();
            return;
        }

        serviceDatePicker.setValue(selectedRecord.getServiceDate());
        mileageField.setText(String.valueOf(selectedRecord.getMileage()));
        costField.setText(String.valueOf(selectedRecord.getCost()));
        descriptionArea.setText(selectedRecord.getDescription());
    }

    private void clearForm() {
        serviceDatePicker.setValue(null);
        mileageField.clear();
        costField.clear();
        descriptionArea.clear();
    }

    private void setFormDisabled(boolean disabled) {
        serviceDatePicker.setDisable(disabled);
        mileageField.setDisable(disabled);
        costField.setDisable(disabled);
        descriptionArea.setDisable(disabled);
    }

    private boolean validate() {
        if (serviceDatePicker.getValue() == null || descriptionArea.getText().trim().isEmpty()) {
            AlertUtils.showError("Błąd", "Data i opis są wymagane.");
            return false;
        }

        try {
            if (!mileageField.getText().isEmpty())
                Integer.parseInt(mileageField.getText());
            if (!costField.getText().isEmpty())
                Double.parseDouble(costField.getText());
        } catch (NumberFormatException e) {
            AlertUtils.showError("Błąd", "Przebieg i koszt muszą być liczbami.");
            return false;
        }

        return true;
    }

    @Override
    public void onAdd() {
        currentMode = Mode.ADD;
        selectedRecord = null;

        clearForm();
        setFormDisabled(false);
        serviceDatePicker.requestFocus();

        crudButtons.enterEditingMode();
    }

    @Override
    public void onEdit() {
        if (selectedRecord == null) {
            AlertUtils.showError("Brak wyboru", "Wybierz wpis.");
            return;
        }

        currentMode = Mode.EDIT;
        setFormDisabled(false);
        crudButtons.enterEditingMode();
    }

    @Override
    public void onDelete() {
        if (selectedRecord == null) return;

        boolean ok = AlertUtils.showConfirmation("Potwierdzenie", "Usunąć wpis?");
        if (!ok) return;

        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM dmt.vehicle_service_history WHERE id=?")) {

            ps.setInt(1, selectedRecord.getId());
            ps.executeUpdate();

            loadRecords();
            clearForm();

        } catch (SQLException e) {
            LoggerUtils.logError(e);
        }
    }

    @Override
    public void onSave() {
        if (!validate() || selectedVehicle == null) return;

        LocalDate date = serviceDatePicker.getValue();
        int mileage = mileageField.getText().isEmpty() ? 0 : Integer.parseInt(mileageField.getText());
        double cost = costField.getText().isEmpty() ? 0 : Double.parseDouble(costField.getText());
        String desc = descriptionArea.getText();

        try (Connection conn = DBConnectionController.getConnection()) {

            if (currentMode == Mode.ADD) {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO dmt.vehicle_service_history(vehicle_id, service_date, mileage_at_service, cost, description) VALUES (?,?,?,?,?)");

                ps.setInt(1, selectedVehicle.getId());
                ps.setDate(2, Date.valueOf(date));
                ps.setInt(3, mileage);
                ps.setDouble(4, cost);
                ps.setString(5, desc);
                ps.executeUpdate();
            }
            else if (currentMode == Mode.EDIT) {
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE dmt.vehicle_service_history SET service_date=?, mileage_at_service=?, cost=?, description=? WHERE id=?");

                ps.setDate(1, Date.valueOf(date));
                ps.setInt(2, mileage);
                ps.setDouble(3, cost);
                ps.setString(4, desc);
                ps.setInt(5, selectedRecord.getId());
                ps.executeUpdate();
            }

            currentMode = Mode.NONE;
            setFormDisabled(true);
            crudButtons.resetButtons();
            loadRecords();
            clearForm();

        } catch (SQLException e) {
            LoggerUtils.logError(e);
        }
    }

    @Override
    public void onCancel() {
        currentMode = Mode.NONE;
        setFormDisabled(true);
        crudButtons.resetButtons();
        populateFormFromSelection();
    }
}