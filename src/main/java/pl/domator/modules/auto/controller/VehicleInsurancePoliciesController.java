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
import pl.domator.modules.auto.model.InsurancePolicy;
import pl.domator.modules.auto.model.Vehicle;
import pl.domator.modules.ui.DashboardController;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

public class VehicleInsurancePoliciesController implements DashboardController.UserAware, CrudController {

    @FXML private VBox mainVBox;

    @FXML private ComboBox<Vehicle> vehicleComboBox;
    @FXML private TableView<InsurancePolicy> insuranceTable;
    @FXML private TableColumn<InsurancePolicy, String> colType;
    @FXML private TableColumn<InsurancePolicy, String> colInsurer;
    @FXML private TableColumn<InsurancePolicy, String> colNumber;
    @FXML private TableColumn<InsurancePolicy, LocalDate> colStartDate;
    @FXML private TableColumn<InsurancePolicy, LocalDate> colEndDate;
    @FXML private TableColumn<InsurancePolicy, Double> colCost;
    @FXML private TableColumn<InsurancePolicy, String> colNotes;

    @FXML private ComboBox<String> policyTypeComboBox;
    @FXML private TextField insurerField;
    @FXML private TextField policyNumberField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField costField;
    @FXML private TextArea notesArea;

    private ObservableList<InsurancePolicy> policies = FXCollections.observableArrayList();
    private Vehicle selectedVehicle = null;
    private InsurancePolicy selectedPolicy = null;

    private CrudButtonsController crudButtons;
    private int userId;

    private enum Mode { NONE, ADD, EDIT }
    private Mode currentMode = Mode.NONE;

    @FXML
    private void initialize() {
        colType.setCellValueFactory(c -> c.getValue().policyTypeProperty());
        colInsurer.setCellValueFactory(c -> c.getValue().insurerProperty());
        colNumber.setCellValueFactory(c -> c.getValue().policyNumberProperty());
        colStartDate.setCellValueFactory(c -> c.getValue().startDateProperty());
        colEndDate.setCellValueFactory(c -> c.getValue().endDateProperty());
        colCost.setCellValueFactory(c -> c.getValue().costProperty().asObject());
        colNotes.setCellValueFactory(c -> c.getValue().notesProperty());

        insuranceTable.setItems(policies);

        insuranceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selectedPolicy = newSel;
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

        vehicleComboBox.setOnAction(e -> loadPoliciesForSelectedVehicle());
    }

    @Override
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
            AlertUtils.showError("Błąd", "Nie udało się wczytać pojazdów.");
        }
    }

    private void loadPoliciesForSelectedVehicle() {
        selectedVehicle = vehicleComboBox.getSelectionModel().getSelectedItem();
        policies.clear();
        if (selectedVehicle == null) return;

        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM dmt.vehicle_insurance_policies WHERE vehicle_id=? ORDER BY start_date DESC")) {

            ps.setInt(1, selectedVehicle.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                policies.add(new InsurancePolicy(
                        rs.getInt("id"),
                        rs.getInt("vehicle_id"),
                        rs.getString("policy_type"),
                        rs.getString("insurer_name"),
                        rs.getString("policy_number"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        rs.getDouble("cost"),
                        rs.getString("notes")
                ));
            }

        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się wczytać polis.");
        }
    }

    private void populateFormFromSelection() {
        if (selectedPolicy == null) {
            clearForm();
            return;
        }

        policyTypeComboBox.setValue(selectedPolicy.getPolicyType());
        insurerField.setText(selectedPolicy.getInsurer());
        policyNumberField.setText(selectedPolicy.getPolicyNumber());
        startDatePicker.setValue(selectedPolicy.getStartDate());
        endDatePicker.setValue(selectedPolicy.getEndDate());
        costField.setText(String.valueOf(selectedPolicy.getCost()));
        notesArea.setText(selectedPolicy.getNotes());
    }

    private void clearForm() {
        policyTypeComboBox.setValue(null);
        insurerField.clear();
        policyNumberField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        costField.clear();
        notesArea.clear();
    }

    private void setFormDisabled(boolean disabled) {
        policyTypeComboBox.setDisable(disabled);
        insurerField.setDisable(disabled);
        policyNumberField.setDisable(disabled);
        startDatePicker.setDisable(disabled);
        endDatePicker.setDisable(disabled);
        costField.setDisable(disabled);
        notesArea.setDisable(disabled);
    }

    private boolean validateForm() {
        if (policyTypeComboBox.getValue() == null ||
                insurerField.getText().trim().isEmpty() ||
                startDatePicker.getValue() == null ||
                endDatePicker.getValue() == null) {

            AlertUtils.showError("Błąd walidacji", "Uzupełnij wymagane pola.");
            return false;
        }

        try {
            if (!costField.getText().isEmpty())
                Double.parseDouble(costField.getText());
        } catch (NumberFormatException e) {
            AlertUtils.showError("Błąd walidacji", "Koszt musi być liczbą.");
            return false;
        }

        return true;
    }

    @Override
    public void onAdd() {
        currentMode = Mode.ADD;
        selectedPolicy = null;

        clearForm();
        setFormDisabled(false);

        policyTypeComboBox.requestFocus();
        if (crudButtons != null) crudButtons.enterEditingMode();
    }

    @Override
    public void onEdit() {
        if (selectedPolicy == null) {
            AlertUtils.showError("Brak wyboru", "Proszę wybrać polisę.");
            return;
        }

        currentMode = Mode.EDIT;
        setFormDisabled(false);

        if (crudButtons != null) crudButtons.enterEditingMode();
    }

    @Override
    public void onDelete() {
        if (selectedPolicy == null) return;

        boolean ok = AlertUtils.showConfirmation("Potwierdzenie", "Usunąć wybraną polisę?");
        if (!ok) return;

        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM dmt.vehicle_insurance_policies WHERE id=?")) {

            ps.setInt(1, selectedPolicy.getId());
            ps.executeUpdate();

            loadPoliciesForSelectedVehicle();
            clearForm();

        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się usunąć polisy.");
        }
    }

    @Override
    public void onSave() {
        if (!validateForm() || selectedVehicle == null) return;

        String type = policyTypeComboBox.getValue();
        String insurer = insurerField.getText().trim();
        String number = policyNumberField.getText().trim();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        double cost = costField.getText().isEmpty() ? 0 : Double.parseDouble(costField.getText());
        String notes = notesArea.getText();

        try (Connection conn = DBConnectionController.getConnection()) {

            if (currentMode == Mode.ADD) {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO dmt.vehicle_insurance_policies(" +
                                "vehicle_id, policy_type, insurer_name, policy_number, start_date, end_date, cost, notes)" +
                                " VALUES (?,?,?,?,?,?,?,?)");

                ps.setInt(1, selectedVehicle.getId());
                ps.setString(2, type);
                ps.setString(3, insurer);
                ps.setString(4, number.isEmpty() ? null : number);
                ps.setDate(5, Date.valueOf(start));
                ps.setDate(6, Date.valueOf(end));
                ps.setDouble(7, cost);
                ps.setString(8, notes);

                ps.executeUpdate();
            }
            else if (currentMode == Mode.EDIT) {
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE dmt.vehicle_insurance_policies SET " +
                                "policy_type=?, insurer_name=?, policy_number=?, start_date=?, end_date=?, cost=?, notes=? " +
                                "WHERE id=?");

                ps.setString(1, type);
                ps.setString(2, insurer);
                ps.setString(3, number.isEmpty() ? null : number);
                ps.setDate(4, Date.valueOf(start));
                ps.setDate(5, Date.valueOf(end));
                ps.setDouble(6, cost);
                ps.setString(7, notes);
                ps.setInt(8, selectedPolicy.getId());

                ps.executeUpdate();
            }

            currentMode = Mode.NONE;
            setFormDisabled(true);
            if (crudButtons != null) crudButtons.resetButtons();
            loadPoliciesForSelectedVehicle();
            clearForm();

        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się zapisać polisy.");
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
