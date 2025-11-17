package pl.domator.Modules.MealPlanner.Controller;

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
import pl.domator.Modules.MealPlanner.Model.BaseType;
import pl.domator.Modules.MealPlanner.Model.MealType;
import pl.domator.Modules.MealPlanner.Model.ProteinType;

import java.io.IOException;
import java.sql.*;

public class CategoryManagerController implements CrudController {
    @FXML private VBox mainVBox;

    @FXML private RadioButton rbProtein;
    @FXML private RadioButton rbBase;
    @FXML private RadioButton rbMeal;

    @FXML private TableView<Object> table;
    @FXML private TableColumn<Object, String> colName;
    @FXML private TableColumn<Object, String> colCategory;

    @FXML private TextField nameField;
    @FXML private TextField categoryField;
    @FXML private Label categoryLabel;
    @FXML private ToggleGroup categoryGroup;

    private CrudButtonsController crudButtons;

    private enum Mode { NONE, ADD, EDIT }
    private Mode currentMode = Mode.NONE;

    private ObservableList<Object> data = FXCollections.observableArrayList();
    private Object selectedItem = null;

    private enum TabType { PROTEIN, BASE, MEAL }
    private TabType currentTab = TabType.PROTEIN;


    @FXML
    private void initialize() {
        categoryGroup = new ToggleGroup();
        rbProtein.setToggleGroup(categoryGroup);
        rbBase.setToggleGroup(categoryGroup);
        rbMeal.setToggleGroup(categoryGroup);

        colName.setCellValueFactory(c -> new SimpleStringProperty(getNameValue(c.getValue())));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(getCategoryValue(c.getValue())));

        table.setItems(data);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selectedItem = newSel;
            if (currentMode == Mode.NONE) populateForm();
        });

        rbProtein.setOnAction(e -> switchTab(TabType.PROTEIN));
        rbBase.setOnAction(e -> switchTab(TabType.BASE));
        rbMeal.setOnAction(e -> switchTab(TabType.MEAL));

        setFormDisabled(true);

        loadCrudButtons();
        switchTab(TabType.PROTEIN);
    }


    private void loadCrudButtons() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pl/domator/fxml/Core/CrudButtons.fxml"));
            HBox crudBox = loader.load();
            crudButtons = loader.getController();
            crudButtons.setHandler(this);
            mainVBox.getChildren().add(3, crudBox);
        } catch (IOException e) {
            LoggerUtils.logError(e);
        }
    }

    private String getNameValue(Object obj) {
        if (obj instanceof ProteinType p) return p.getName();
        if (obj instanceof BaseType b) return b.getName();
        if (obj instanceof MealType m) return m.getName();
        return "";
    }

    private String getCategoryValue(Object obj) {
        if (obj instanceof ProteinType p) return p.getCategory();
        if (obj instanceof BaseType b) return b.getCategory();
        return "";
    }

    private void switchTab(TabType tab) {
        currentTab = tab;

        if (tab == TabType.MEAL) {
            categoryLabel.setVisible(false);
            categoryField.setVisible(false);
        } else {
            categoryLabel.setVisible(true);
            categoryField.setVisible(true);
        }

        loadData();
        clearForm();
        setFormDisabled(true);
        currentMode = Mode.NONE;
        crudButtons.resetButtons();
    }

    @Override
    public void onAdd() {

        currentMode = Mode.ADD;
        selectedItem = null;
        clearForm();
        setFormDisabled(true);

        nameField.setDisable(false);
        if (currentTab != TabType.MEAL) categoryField.setDisable(false);

        crudButtons.enterEditingMode();
    }

    @Override
    public void onEdit() {
        if (selectedItem == null) {
            AlertUtils.showError("Brak wyboru", "Wybierz element do edycji.");
            return;
        }

        currentMode = Mode.EDIT;
        setFormDisabled(false);
        if (currentTab == TabType.MEAL) categoryField.setDisable(true);

        crudButtons.enterEditingMode();
    }

    @Override
    public void onDelete() {
        if (selectedItem == null) return;

        boolean c = AlertUtils.showConfirmation(
                "Potwierdzenie",
                "Czy na pewno chcesz usunąć ten element?"
        );

        if (!c) return;

        deleteItem(getItemId(selectedItem));
    }

    @Override
    public void onSave() {
        String name = nameField.getText().trim();
        String cat = categoryField.getText().trim();

        if (name.isEmpty()) {
            AlertUtils.showError("Błąd", "Pole Nazwa jest wymagane.");
            return;
        }
        if (currentTab != TabType.MEAL && cat.isEmpty()) {
            AlertUtils.showError("Błąd", "Pole Kategoria jest wymagane.");
            return;
        }

        boolean confirmed = AlertUtils.showConfirmation(
                "Potwierdzenie zapisu",
                "Czy chcesz zapisać zmiany?"
        );
        if (!confirmed) return;

        if (currentMode == Mode.ADD)
            insertItem(name, cat);
        else if (currentMode == Mode.EDIT)
            updateItem(getItemId(selectedItem), name, cat);

        currentMode = Mode.NONE;
        setFormDisabled(true);
        crudButtons.resetButtons();
        loadData();
        clearForm();
    }

    @Override
    public void onCancel() {

        boolean c = AlertUtils.showConfirmation(
                "Potwierdzenie",
                "Czy chcesz anulować?"
        );
        if (!c) return;

        currentMode = Mode.NONE;
        setFormDisabled(true);
        crudButtons.resetButtons();
        populateForm();
    }

    private int getItemId(Object obj) {
        if (obj instanceof ProteinType p) return p.getId();
        if (obj instanceof BaseType b) return b.getId();
        if (obj instanceof MealType m) return m.getId();
        return -1;
    }

    private void populateForm() {
        if (selectedItem == null) {
            clearForm();
            return;
        }

        nameField.setText(getNameValue(selectedItem));
        categoryField.setText(getCategoryValue(selectedItem));
    }

    private void clearForm() {
        nameField.clear();
        categoryField.clear();
    }

    private void setFormDisabled(boolean disabled) {
        nameField.setDisable(disabled);
        categoryField.setDisable(disabled);
    }

    private void loadData() {
        data.clear();
        String sql = "";

        if (currentTab == TabType.PROTEIN) sql = "SELECT * FROM dmt.protein_types ORDER BY name";
        if (currentTab == TabType.BASE) sql = "SELECT * FROM dmt.base_types ORDER BY name";
        if (currentTab == TabType.MEAL) sql = "SELECT * FROM dmt.meal_types ORDER BY name";

        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                if (currentTab == TabType.PROTEIN)
                    data.add(new ProteinType(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("category")));

                else if (currentTab == TabType.BASE)
                    data.add(new BaseType(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("category")));

                else if (currentTab == TabType.MEAL)
                    data.add(new MealType(
                            rs.getInt("id"),
                            rs.getString("name")));
            }

        } catch (SQLException e) {
            LoggerUtils.logError(e);
        }
    }

    private void insertItem(String name, String cat) {
        String sql = "";

        if (currentTab == TabType.MEAL)
            sql = "INSERT INTO dmt.meal_types (name) VALUES (?)";
        else if (currentTab == TabType.PROTEIN)
            sql = "INSERT INTO dmt.protein_types (name, category) VALUES (?, ?)";
        else
            sql = "INSERT INTO dmt.base_types (name, category) VALUES (?, ?)";

        try (Connection c = DBConnectionController.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, name);
            if (currentTab != TabType.MEAL)
                ps.setString(2, cat);

            ps.executeUpdate();
        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się dodać.");
        }
    }

    private void updateItem(int id, String name, String cat) {
        String sql = "";

        if (currentTab == TabType.MEAL)
            sql = "UPDATE dmt.meal_types SET name=? WHERE id=?";
        else if (currentTab == TabType.PROTEIN)
            sql = "UPDATE dmt.protein_types SET name=?, category=? WHERE id=?";
        else
            sql = "UPDATE dmt.base_types SET name=?, category=? WHERE id=?";

        try (Connection c = DBConnectionController.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, name);

            if (currentTab == TabType.MEAL) {
                ps.setInt(2, id);
            } else {
                ps.setString(2, cat);
                ps.setInt(3, id);
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError("Błąd", "Nie udało się edytować.");
        }
    }

    private void deleteItem(int id) {

        String sql = "";

        if (currentTab == TabType.MEAL)
            sql = "DELETE FROM dmt.meal_types WHERE id=?";
        else if (currentTab == TabType.PROTEIN)
            sql = "DELETE FROM dmt.protein_types WHERE id=?";
        else
            sql = "DELETE FROM dmt.base_types WHERE id=?";

        try (Connection c = DBConnectionController.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

            loadData();

        } catch (SQLException e) {
            LoggerUtils.logError(e);
            AlertUtils.showError(
                    "Błąd",
                    "Nie można usunąć — ta kategoria może być używana w daniach."
            );
        }
    }
}