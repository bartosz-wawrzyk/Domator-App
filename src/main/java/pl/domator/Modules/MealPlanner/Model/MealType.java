package pl.domator.Modules.MealPlanner.Model;

public class MealType {

    private final int id;
    private final String name;

    public MealType(int id, String name) {
        this.id = id;
        this.name = name == null ? "" : name;
    }

    public int getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() { return name; }
}
