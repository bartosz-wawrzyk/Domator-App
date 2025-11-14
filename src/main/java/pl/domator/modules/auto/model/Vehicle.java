package pl.domator.modules.auto.model;

public class Vehicle {
    private final int id;
    private final int userId;
    private final String brand;
    private final String model;
    private final int productionYear;
    private final String registrationNumber;
    private final String vin;
    private final String fuelType;
    private final int currentMileage;
    private final String notes;

    public Vehicle(int id, int userId, String brand, String model, int productionYear,
                   String registrationNumber, String vin, String fuelType,
                   int currentMileage, String notes) {
        this.id = id;
        this.userId = userId;
        this.brand = brand == null ? "" : brand;
        this.model = model == null ? "" : model;
        this.productionYear = productionYear;
        this.registrationNumber = registrationNumber == null ? "" : registrationNumber;
        this.vin = vin == null ? "" : vin;
        this.fuelType = fuelType == null ? "" : fuelType;
        this.currentMileage = currentMileage;
        this.notes = notes == null ? "" : notes;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public int getProductionYear() { return productionYear; }
    public String getRegistrationNumber() { return registrationNumber; }
    public String getVin() { return vin; }
    public String getFuelType() { return fuelType; }
    public int getCurrentMileage() { return currentMileage; }
    public String getNotes() { return notes; }

    @Override
    public String toString() {
        return getBrand() + " " + getModel();
    }

}