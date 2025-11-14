package pl.domator.modules.auto.model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class ServiceRecord {

    private final SimpleIntegerProperty id;
    private final SimpleIntegerProperty vehicleId;
    private final ObjectProperty<LocalDate> serviceDate;
    private final SimpleIntegerProperty mileage;
    private final SimpleDoubleProperty cost;
    private final SimpleStringProperty description;

    public ServiceRecord(int id, int vehicleId, LocalDate serviceDate,
                         int mileage, double cost, String description) {

        this.id = new SimpleIntegerProperty(id);
        this.vehicleId = new SimpleIntegerProperty(vehicleId);
        this.serviceDate = new SimpleObjectProperty<>(serviceDate);
        this.mileage = new SimpleIntegerProperty(mileage);
        this.cost = new SimpleDoubleProperty(cost);
        this.description = new SimpleStringProperty(description == null ? "" : description);
    }

    public int getId() { return id.get(); }
    public SimpleIntegerProperty idProperty() { return id; }

    public int getVehicleId() { return vehicleId.get(); }
    public SimpleIntegerProperty vehicleIdProperty() { return vehicleId; }

    public LocalDate getServiceDate() { return serviceDate.get(); }
    public ObjectProperty<LocalDate> serviceDateProperty() { return serviceDate; }

    public int getMileage() { return mileage.get(); }
    public SimpleIntegerProperty mileageProperty() { return mileage; }

    public double getCost() { return cost.get(); }
    public SimpleDoubleProperty costProperty() { return cost; }

    public String getDescription() { return description.get(); }
    public SimpleStringProperty descriptionProperty() { return description; }
}