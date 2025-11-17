package pl.domator.Modules.MealPlanner.Model;

public class ProteinType {

    private final int id;
    private final String name;
    private final String category;

    public ProteinType(int id, String name, String category) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.category = category == null ? "" : category;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }

    @Override
    public String toString() { return name; }
}