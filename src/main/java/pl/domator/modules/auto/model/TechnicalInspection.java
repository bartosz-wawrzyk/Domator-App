package pl.domator.modules.auto.model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class TechnicalInspection {
    private final SimpleIntegerProperty id;
    private final SimpleIntegerProperty vehicleId;
    private final ObjectProperty<LocalDate> inspectionDate;
    private final ObjectProperty<LocalDate> validUntil;
    private final SimpleIntegerProperty mileage;
    private final SimpleDoubleProperty cost;
    private final SimpleStringProperty notes;

    public TechnicalInspection(int id, int vehicleId, LocalDate inspectionDate, LocalDate validUntil,
                               int mileage, double cost, String notes) {
        this.id = new SimpleIntegerProperty(id);
        this.vehicleId = new SimpleIntegerProperty(vehicleId);
        this.inspectionDate = new SimpleObjectProperty<>(inspectionDate);
        this.validUntil = new SimpleObjectProperty<>(validUntil);
        this.mileage = new SimpleIntegerProperty(mileage);
        this.cost = new SimpleDoubleProperty(cost);
        this.notes = new SimpleStringProperty(notes == null ? "" : notes);
    }

    public int getId() { return id.get(); }
    public SimpleIntegerProperty idProperty() { return id; }

    public int getVehicleId() { return vehicleId.get(); }
    public SimpleIntegerProperty vehicleIdProperty() { return vehicleId; }

    public LocalDate getInspectionDate() { return inspectionDate.get(); }
    public ObjectProperty<LocalDate> inspectionDateProperty() { return inspectionDate; }

    public LocalDate getValidUntil() { return validUntil.get(); }
    public ObjectProperty<LocalDate> validUntilProperty() { return validUntil; }

    public int getMileage() { return mileage.get(); }
    public SimpleIntegerProperty mileageProperty() { return mileage; }

    public double getCost() { return cost.get(); }
    public SimpleDoubleProperty costProperty() { return cost; }

    public String getNotes() { return notes.get(); }
    public SimpleStringProperty notesProperty() { return notes; }
}