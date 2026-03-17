package de.evia.travelmate.trips.domain.mealplan;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.time.LocalDate;
import java.util.UUID;

public class MealSlot {

    private final MealSlotId mealSlotId;
    private final LocalDate date;
    private final MealType mealType;
    private MealSlotStatus status;
    private UUID recipeId;

    public MealSlot(final MealSlotId mealSlotId,
                    final LocalDate date,
                    final MealType mealType,
                    final MealSlotStatus status,
                    final UUID recipeId) {
        argumentIsNotNull(mealSlotId, "mealSlotId");
        argumentIsNotNull(date, "date");
        argumentIsNotNull(mealType, "mealType");
        argumentIsNotNull(status, "status");
        this.mealSlotId = mealSlotId;
        this.date = date;
        this.mealType = mealType;
        this.status = status;
        this.recipeId = recipeId;
    }

    public MealSlot(final LocalDate date, final MealType mealType) {
        this(new MealSlotId(UUID.randomUUID()), date, mealType, MealSlotStatus.PLANNED, null);
    }

    void markStatus(final MealSlotStatus newStatus) {
        this.status = newStatus;
        if (newStatus == MealSlotStatus.SKIP || newStatus == MealSlotStatus.EATING_OUT) {
            this.recipeId = null;
        }
    }

    void assignRecipe(final UUID recipeId) {
        this.status = MealSlotStatus.PLANNED;
        this.recipeId = recipeId;
    }

    void clearRecipe() {
        this.recipeId = null;
    }

    public MealSlotId mealSlotId() {
        return mealSlotId;
    }

    public LocalDate date() {
        return date;
    }

    public MealType mealType() {
        return mealType;
    }

    public MealSlotStatus status() {
        return status;
    }

    public UUID recipeId() {
        return recipeId;
    }
}
