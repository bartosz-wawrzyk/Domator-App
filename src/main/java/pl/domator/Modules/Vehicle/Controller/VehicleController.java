package pl.domator.Modules.Vehicle.Controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
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
import pl.domator.Modules.Vehicle.Model.Vehicle;
import pl.domator.Modules.UI.DashboardController;

import java.io.IOException;
import java.sql.*;

public class VehicleController implements DashboardController.UserAware, CrudController {

    @FXML private VBox mainVBox;
    @FXML private TableView<Vehicle> vehicleTable;
    @FXML private TableColumn<Vehicle, String> colBrand;
    @FXML private TableColumn<Vehicle, String> colModel;
    @FXML private TableColumn<Vehicle, Integer> colYear;
    @FXML private TableColumn<Vehicle, String> colReg;
    @FXML private TableColumn<Vehicle, Integer> colMileage;

    @FXML private TextField brandField;
    @FXML private TextField modelField;
    @FXML private TextField yearField;
    @FXML private TextField registrationField;
    @FXML private TextField vinField;
    @FXML private TextField fuelTypeField;
    @FXML private TextField mileageField;
    @FXML private TextArea notesArea;

    private ObservableList<Vehicle> vehicles = FXCollections.observableArrayList();
    private int userId;
    private Vehicle selectedVehicle = null;

    private enum Mode { NONE, ADD, EDIT }
    private Mode currentMode = Mode.NONE;

    private CrudButtonsController crudButtons;

    @FXML
    private void initialize() {
        colBrand.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBrand()));
        colModel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getModel()));
        colYear.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getProductionYear()).asObject());
        colReg.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRegistrationNumber()));
        colMileage.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getCurrentMileage()).asObject());
        vehicleTable.setItems(vehicles);

        vehicleTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selectedVehicle = newSel;
            if (currentMode == Mode.NONE) populateFormFromSelection();
        });

        setFormDisabled(true);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pl/domator/fxml/Core/CrudButtons.fxml"));
            HBox crudBox = loader.load();
            crudButtons = loader.getController();
            crudButtons.setHandler(this);

            mainVBox.getChildren().add(2, crudBox);

        } catch (IOException e) {
            LoggerUtils.logError(e);
        }
    }

    @Override
    public void setUserId(int userId) {
        this.userId = userId;
        loadVehicles();
    }

    @Override
    public void onAdd() {
        currentMode = Mode.ADD;
        selectedVehicle = null;
        clearForm();
        setFormDisabled(false);
        brandField.requestFocus();
        if (crudButtons != null) crudButtons.enterEditingMode();
    }

    @Override
    public void onEdit() {
        if (selectedVehicle == null) {
            AlertUtils.showError("Brak wyboru", "Proszę wybrać pojazd do edycji.");
            return;
        }
        currentMode = Mode.EDIT;
        setFormDisabled(false);
        if (crudButtons != null) crudButtons.enterEditingMode();
    }

    @Override
    public void onDelete() {
        if (selectedVehicle == null) return;
        boolean confirmed = AlertUtils.showConfirmation("Potwierdzenie", "Czy na pewno chcesz usunąć pojazd: "
                + selectedVehicle.getBrand() + " " + selectedVehicle.getModel() + "?");
        if (confirmed) deleteVehicle(selectedVehicle.getId());
    }

    @Override
    public void onSave() {
        String brand = brandField.getText().trim();
        String model = modelField.getText().trim();
        String reg = registrationField.getText().trim();
        String vin = vinField.getText().trim();

        if (brand.isEmpty() || model.isEmpty()) {
            AlertUtils.showError("Błąd walidacji", "Pole Marka i Model są wymagane.");
            return;
        }

        if (!vin.isEmpty() && !vin.matches("^[A-HJ-NPR-Z0-9]{11,17}$")) {
            AlertUtils.showWarning("Niepoprawny VIN", "VIN powinien mieć 11–17 znaków i zawierać tylko litery i cyfry (bez O, I, Q).");
        }

        if (isVinOrRegExists(vin, reg, (currentMode == Mode.EDIT && selectedVehicle != null ? selectedVehicle.getId() : -1))) {
            AlertUtils.showError("Duplikat", "Pojazd z tym numerem VIN lub rejestracyjnym już istnieje.");
            return;
        }

        Integer year = parseIntegerOrNull(yearField.getText().trim());
        Integer mileage = parseIntegerOrNull(mileageField.getText().trim());
        String fuel = fuelTypeField.getText().trim();
        String notes = notesArea.getText().trim();

        try {
            if (currentMode == Mode.ADD) insertVehicle(brand, model, year, reg, vin, fuel, mileage, notes);
            else if (currentMode == Mode.EDIT && selectedVehicle != null)
                updateVehicle(selectedVehicle.getId(), brand, model, year, reg, vin, fuel, mileage, notes);

            currentMode = Mode.NONE;
            setFormDisabled(true);
            if (crudButtons != null) crudButtons.resetButtons();
            clearForm();
            loadVehicles();

        } catch (Exception e) {
            AlertUtils.showError("Błąd", "Nie udało się zapisać pojazdu.");
            LoggerUtils.logError(e);
        }
    }

    @Override
    public void onCancel() {
        currentMode = Mode.NONE;
        setFormDisabled(true);
        if (crudButtons != null) crudButtons.resetButtons();
        populateFormFromSelection();
    }

    private void populateFormFromSelection() {
        if (selectedVehicle == null) {
            clearForm();
            return;
        }
        brandField.setText(selectedVehicle.getBrand());
        modelField.setText(selectedVehicle.getModel());
        yearField.setText(selectedVehicle.getProductionYear() == 0 ? "" : String.valueOf(selectedVehicle.getProductionYear()));
        registrationField.setText(selectedVehicle.getRegistrationNumber());
        vinField.setText(selectedVehicle.getVin());
        fuelTypeField.setText(selectedVehicle.getFuelType());
        mileageField.setText(String.valueOf(selectedVehicle.getCurrentMileage()));
        notesArea.setText(selectedVehicle.getNotes());
    }

    private void clearForm() {
        brandField.clear();
        modelField.clear();
        yearField.clear();
        registrationField.clear();
        vinField.clear();
        fuelTypeField.clear();
        mileageField.clear();
        notesArea.clear();
    }

    private void setFormDisabled(boolean disabled) {
        brandField.setDisable(disabled);
        modelField.setDisable(disabled);
        yearField.setDisable(disabled);
        registrationField.setDisable(disabled);
        vinField.setDisable(disabled);
        fuelTypeField.setDisable(disabled);
        mileageField.setDisable(disabled);
        notesArea.setDisable(disabled);
    }

    private Integer parseIntegerOrNull(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }

    private boolean isVinOrRegExists(String vin, String reg, int excludeId) {
        String sql = "SELECT COUNT(*) FROM dmt.vehicles WHERE (vin = ? OR registration_number = ?) AND user_id = ?" +
                (excludeId > 0 ? " AND id <> ?" : "");
        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vin);
            ps.setString(2, reg);
            ps.setInt(3, userId);
            if (excludeId > 0) ps.setInt(4, excludeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            LoggerUtils.logError(e);
        }
        return false;
    }

    private void loadVehicles() {
        vehicles.clear();
        String sql = "SELECT * FROM dmt.vehicles WHERE user_id = ? ORDER BY id DESC";
        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
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
                vehicles.add(v);
            }
        } catch (SQLException e) {
            LoggerUtils.logError(e);
        }
    }

    private void deleteVehicle(int id) {
        String sql = "DELETE FROM dmt.vehicles WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
            loadVehicles();
            clearForm();
        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się usunąć pojazdu.");
        }
    }

    private void insertVehicle(String brand, String model, Integer year, String reg, String vin,
                               String fuel, Integer mileage, String notes) {
        String sql = "INSERT INTO dmt.vehicles (user_id, brand, model, production_year, registration_number, vin, fuel_type, current_mileage, notes) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, brand);
            ps.setString(3, model);
            if (year != null) ps.setInt(4, year); else ps.setNull(4, Types.INTEGER);
            ps.setString(5, reg.isEmpty() ? null : reg);
            ps.setString(6, vin.isEmpty() ? null : vin);
            ps.setString(7, fuel.isEmpty() ? null : fuel);
            if (mileage != null) ps.setInt(8, mileage); else ps.setNull(8, Types.INTEGER);
            ps.setString(9, notes.isEmpty() ? null : notes);
            ps.executeUpdate();
        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się dodać pojazdu.");
        }
    }

    private void updateVehicle(int id, String brand, String model, Integer year, String reg, String vin,
                               String fuel, Integer mileage, String notes) {
        String sql = "UPDATE dmt.vehicles SET brand=?, model=?, production_year=?, registration_number=?, vin=?, fuel_type=?, current_mileage=?, notes=? WHERE id=? AND user_id=?";
        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, brand);
            ps.setString(2, model);
            if (year != null) ps.setInt(3, year); else ps.setNull(3, Types.INTEGER);
            ps.setString(4, reg.isEmpty() ? null : reg);
            ps.setString(5, vin.isEmpty() ? null : vin);
            ps.setString(6, fuel.isEmpty() ? null : fuel);
            if (mileage != null) ps.setInt(7, mileage); else ps.setNull(7, Types.INTEGER);
            ps.setString(8, notes.isEmpty() ? null : notes);
            ps.setInt(9, id);
            ps.setInt(10, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się zaktualizować pojazdu.");
        }
    }
}
