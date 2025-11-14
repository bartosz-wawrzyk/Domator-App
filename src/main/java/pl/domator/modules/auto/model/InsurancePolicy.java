package pl.domator.modules.auto.model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class InsurancePolicy {

    private final SimpleIntegerProperty id;
    private final SimpleIntegerProperty vehicleId;

    private final SimpleStringProperty policyType;
    private final SimpleStringProperty insurer;
    private final SimpleStringProperty policyNumber;

    private final ObjectProperty<LocalDate> startDate;
    private final ObjectProperty<LocalDate> endDate;

    private final SimpleDoubleProperty cost;
    private final SimpleStringProperty notes;

    public InsurancePolicy(int id,
                           int vehicleId,
                           String policyType,
                           String insurer,
                           String policyNumber,
                           LocalDate startDate,
                           LocalDate endDate,
                           double cost,
                           String notes) {

        this.id = new SimpleIntegerProperty(id);
        this.vehicleId = new SimpleIntegerProperty(vehicleId);

        this.policyType = new SimpleStringProperty(policyType == null ? "" : policyType);
        this.insurer = new SimpleStringProperty(insurer == null ? "" : insurer);
        this.policyNumber = new SimpleStringProperty(policyNumber == null ? "" : policyNumber);

        this.startDate = new SimpleObjectProperty<>(startDate);
        this.endDate = new SimpleObjectProperty<>(endDate);

        this.cost = new SimpleDoubleProperty(cost);
        this.notes = new SimpleStringProperty(notes == null ? "" : notes);
    }

    public int getId() { return id.get(); }
    public SimpleIntegerProperty idProperty() { return id; }

    public int getVehicleId() { return vehicleId.get(); }
    public SimpleIntegerProperty vehicleIdProperty() { return vehicleId; }

    public String getPolicyType() { return policyType.get(); }
    public SimpleStringProperty policyTypeProperty() { return policyType; }

    public String getInsurer() { return insurer.get(); }
    public SimpleStringProperty insurerProperty() { return insurer; }

    public String getPolicyNumber() { return policyNumber.get(); }
    public SimpleStringProperty policyNumberProperty() { return policyNumber; }

    public LocalDate getStartDate() { return startDate.get(); }
    public ObjectProperty<LocalDate> startDateProperty() { return startDate; }

    public LocalDate getEndDate() { return endDate.get(); }
    public ObjectProperty<LocalDate> endDateProperty() { return endDate; }

    public double getCost() { return cost.get(); }
    public SimpleDoubleProperty costProperty() { return cost; }

    public String getNotes() { return notes.get(); }
    public SimpleStringProperty notesProperty() { return notes; }
}
