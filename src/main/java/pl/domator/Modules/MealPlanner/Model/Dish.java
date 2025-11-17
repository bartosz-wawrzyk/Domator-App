package pl.domator.Modules.MealPlanner.Model;

public class Dish {
    private final int id;
    private final int userId;
    private final String name;
    private final String description;
    private final Integer preparationTime;
    private final Integer proteinId;
    private final Integer baseId;
    private final Integer mealTypeId;

    public Dish(int id, int userId, String name, String description,
                Integer preparationTime, Integer proteinId,
                Integer baseId, Integer mealTypeId) {
        this.id = id;
        this.userId = userId;
        this.name = name == null ? "" : name;
        this.description = description == null ? "" : description;
        this.preparationTime = preparationTime;
        this.proteinId = proteinId;
        this.baseId = baseId;
        this.mealTypeId = mealTypeId;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getPreparationTime() { return preparationTime; }
    public Integer getProteinId() { return proteinId; }
    public Integer getBaseId() { return baseId; }
    public Integer getMealTypeId() { return mealTypeId; }

    @Override
    public String toString() {
        return name;
    }
}
