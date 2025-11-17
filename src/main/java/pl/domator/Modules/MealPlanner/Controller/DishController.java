package pl.domator.Modules.MealPlanner.Controller;

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
import pl.domator.Modules.MealPlanner.Model.Dish;
import pl.domator.Modules.UI.DashboardController;

import java.io.IOException;
import java.sql.*;

public class DishController implements DashboardController.UserAware, CrudController {
    @FXML private VBox mainVBox;

    @FXML private TableView<Dish> dishTable;
    @FXML private TableColumn<Dish, String> colName;
    @FXML private TableColumn<Dish, String> colProtein;
    @FXML private TableColumn<Dish, String> colBase;
    @FXML private TableColumn<Dish, String> colMealType;
    @FXML private TableColumn<Dish, Integer> colPrepTime;

    @FXML private TextField nameField;
    @FXML private ComboBox<String> proteinBox;
    @FXML private ComboBox<String> baseBox;
    @FXML private ComboBox<String> mealTypeBox;
    @FXML private TextField prepTimeField;
    @FXML private TextArea descriptionArea;

    private CrudButtonsController crudButtons;

    private ObservableList<Dish> dishes = FXCollections.observableArrayList();
    private int userId;
    private Dish selectedDish = null;

    private final ObservableList<String> proteinNames = FXCollections.observableArrayList();
    private final ObservableList<String> baseNames = FXCollections.observableArrayList();
    private final ObservableList<String> mealTypeNames = FXCollections.observableArrayList();

    private enum Mode { NONE, ADD, EDIT }
    private Mode currentMode = Mode.NONE;

    @FXML
    private void initialize() {
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colProtein.setCellValueFactory(c -> new SimpleStringProperty(getProteinName(c.getValue().getProteinId())));
        colBase.setCellValueFactory(c -> new SimpleStringProperty(getBaseName(c.getValue().getBaseId())));
        colMealType.setCellValueFactory(c -> new SimpleStringProperty(getMealTypeName(c.getValue().getMealTypeId())));
        colPrepTime.setCellValueFactory(c -> new SimpleIntegerProperty(
                c.getValue().getPreparationTime() == null ? 0 : c.getValue().getPreparationTime()
        ).asObject());

        dishTable.setItems(dishes);

        dishTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selectedDish = newSel;
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
    public void setUserId(int id) {
        this.userId = id;
        loadTypes();
        loadDishes();
    }

    @Override
    public void onAdd() {
        currentMode = Mode.ADD;
        selectedDish = null;
        clearForm();
        setFormDisabled(false);

        nameField.requestFocus();
        crudButtons.enterEditingMode();
    }

    @Override
    public void onEdit() {
        if (selectedDish == null) {
            AlertUtils.showError("Brak wyboru", "Proszę wybrać danie do edycji.");
            return;
        }
        currentMode = Mode.EDIT;
        setFormDisabled(false);
        crudButtons.enterEditingMode();
    }

    @Override
    public void onDelete() {
        if (selectedDish == null) return;

        boolean confirmed = AlertUtils.showConfirmation(
                "Potwierdzenie",
                "Czy na pewno chcesz usunąć danie: " + selectedDish.getName() + "?"
        );

        if (confirmed) deleteDish(selectedDish.getId());
    }

    @Override
    public void onSave() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String desc = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
        Integer prepTime = parseIntegerOrNull(prepTimeField.getText() == null ? "" : prepTimeField.getText().trim());

        String protein = proteinBox.getValue();
        String base = baseBox.getValue();
        String meal = mealTypeBox.getValue();

        if (name.isEmpty()) {
            AlertUtils.showError("Błąd walidacji", "Pole 'Nazwa dania' jest wymagane.");
            return;
        }
        if (protein == null || protein.isEmpty()) {
            AlertUtils.showError("Błąd walidacji", "Pole 'Białko' jest wymagane.");
            return;
        }
        if (base == null || base.isEmpty()) {
            AlertUtils.showError("Błąd walidacji", "Pole 'Baza' jest wymagane.");
            return;
        }
        if (meal == null || meal.isEmpty()) {
            AlertUtils.showError("Błąd walidacji", "Pole 'Posiłek' jest wymagane.");
            return;
        }

        boolean confirmed = AlertUtils.showConfirmation(
                "Potwierdzenie zapisu",
                "Czy na pewno chcesz zapisać to danie?"
        );
        if (!confirmed) return;

        Integer proteinId = getProteinId(protein);
        Integer baseId = getBaseId(base);
        Integer mealTypeId = getMealTypeId(meal);

        try {
            if (currentMode == Mode.ADD) {
                insertDish(name, desc, prepTime, proteinId, baseId, mealTypeId);
            } else if (currentMode == Mode.EDIT && selectedDish != null) {
                updateDish(selectedDish.getId(), name, desc, prepTime, proteinId, baseId, mealTypeId);
            }

            currentMode = Mode.NONE;
            setFormDisabled(true);
            crudButtons.resetButtons();
            clearForm();
            loadDishes();

        } catch (Exception e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się zapisać dania.");
        }
    }

    @Override
    public void onCancel() {
        boolean confirmed = AlertUtils.showConfirmation(
                "Potwierdzenie",
                "Czy na pewno chcesz anulować edycję?"
        );
        if (!confirmed) return;

        currentMode = Mode.NONE;
        setFormDisabled(true);
        crudButtons.resetButtons();
        populateFormFromSelection();
    }

    private String getProteinName(Integer id) {
        return getNameById("dmt.protein_types", id);
    }

    private String getBaseName(Integer id) {
        return getNameById("dmt.base_types", id);
    }

    private String getMealTypeName(Integer id) {
        return getNameById("dmt.meal_types", id);
    }

    private String getNameById(String table, Integer id) {
        if (id == null) return "";
        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name FROM " + table + " WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (SQLException e) {
            LoggerUtils.logError(e);
        }
        return "";
    }

    private Integer getProteinId(String name) {
        return getIdByName("dmt.protein_types", name);
    }

    private Integer getBaseId(String name) {
        return getIdByName("dmt.base_types", name);
    }

    private Integer getMealTypeId(String name) {
        return getIdByName("dmt.meal_types", name);
    }

    private Integer getIdByName(String table, String name) {
        if (name == null || name.isEmpty()) return null;
        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM " + table + " WHERE name = ?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            LoggerUtils.logError(e);
        }
        return null;
    }

    private void loadTypes() {
        proteinNames.clear();
        baseNames.clear();
        mealTypeNames.clear();

        loadListFromDB("dmt.protein_types", proteinNames);
        loadListFromDB("dmt.base_types", baseNames);
        loadListFromDB("dmt.meal_types", mealTypeNames);

        proteinBox.setItems(proteinNames);
        baseBox.setItems(baseNames);
        mealTypeBox.setItems(mealTypeNames);
    }

    private void loadListFromDB(String tableName, ObservableList<String> list) {
        String sql = "SELECT name FROM " + tableName + " ORDER BY name ASC";
        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getString("name"));
        } catch (SQLException e) {
            LoggerUtils.logError(e);
        }
    }

    private void populateFormFromSelection() {
        if (selectedDish == null) {
            clearForm();
            return;
        }

        nameField.setText(selectedDish.getName());
        descriptionArea.setText(selectedDish.getDescription());
        prepTimeField.setText(selectedDish.getPreparationTime() == null ? "" : String.valueOf(selectedDish.getPreparationTime()));

        proteinBox.setValue(getProteinName(selectedDish.getProteinId()));
        baseBox.setValue(getBaseName(selectedDish.getBaseId()));
        mealTypeBox.setValue(getMealTypeName(selectedDish.getMealTypeId()));
    }

    private void clearForm() {
        nameField.clear();
        descriptionArea.clear();
        prepTimeField.clear();
        proteinBox.setValue(null);
        baseBox.setValue(null);
        mealTypeBox.setValue(null);
    }

    private void setFormDisabled(boolean disabled) {
        nameField.setDisable(disabled);
        descriptionArea.setDisable(disabled);
        prepTimeField.setDisable(disabled);
        proteinBox.setDisable(disabled);
        baseBox.setDisable(disabled);
        mealTypeBox.setDisable(disabled);
    }

    private Integer parseIntegerOrNull(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return null; }
    }

    private void loadDishes() {
        dishes.clear();
        String sql = "SELECT * FROM dmt.dishes WHERE user_id = ? ORDER BY id DESC";

        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Dish d = new Dish(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        (Integer) rs.getObject("preparation_time"),
                        (Integer) rs.getObject("protein_id"),
                        (Integer) rs.getObject("base_id"),
                        (Integer) rs.getObject("meal_type_id")
                );
                dishes.add(d);
            }

        } catch (SQLException e) {
            LoggerUtils.logError(e);
        }
    }

    private void insertDish(String name, String desc, Integer prep, Integer proteinId,
                            Integer baseId, Integer mealTypeId) {

        String sql = """
            INSERT INTO dmt.dishes (user_id, name, description, preparation_time,
                                    protein_id, base_id, meal_type_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setString(3, desc.isEmpty() ? null : desc);

            if (prep != null) ps.setInt(4, prep); else ps.setNull(4, Types.INTEGER);
            if (proteinId != null) ps.setInt(5, proteinId); else ps.setNull(5, Types.INTEGER);
            if (baseId != null) ps.setInt(6, baseId); else ps.setNull(6, Types.INTEGER);
            if (mealTypeId != null) ps.setInt(7, mealTypeId); else ps.setNull(7, Types.INTEGER);

            ps.executeUpdate();
        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się dodać dania.");
        }
    }

    private void updateDish(int id, String name, String desc, Integer prep, Integer proteinId,
                            Integer baseId, Integer mealTypeId) {

        String sql = """
            UPDATE dmt.dishes
            SET name=?, description=?, preparation_time=?, protein_id=?, base_id=?, meal_type_id=?
            WHERE id=? AND user_id=?
            """;

        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, desc.isEmpty() ? null : desc);

            if (prep != null) ps.setInt(3, prep); else ps.setNull(3, Types.INTEGER);
            if (proteinId != null) ps.setInt(4, proteinId); else ps.setNull(4, Types.INTEGER);
            if (baseId != null) ps.setInt(5, baseId); else ps.setNull(5, Types.INTEGER);
            if (mealTypeId != null) ps.setInt(6, mealTypeId); else ps.setNull(6, Types.INTEGER);

            ps.setInt(7, id);
            ps.setInt(8, userId);

            ps.executeUpdate();
        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się zaktualizować dania.");
        }
    }

    private void deleteDish(int id) {
        String sql = "DELETE FROM dmt.dishes WHERE id = ? AND user_id = ?";

        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();

            loadDishes();
            clearForm();
        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się usunąć dania.");
        }
    }
}