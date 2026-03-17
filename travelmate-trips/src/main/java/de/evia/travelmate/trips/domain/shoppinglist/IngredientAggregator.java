package de.evia.travelmate.trips.domain.shoppinglist;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.evia.travelmate.trips.domain.mealplan.MealPlan;
import de.evia.travelmate.trips.domain.mealplan.MealSlot;
import de.evia.travelmate.trips.domain.mealplan.MealSlotStatus;
import de.evia.travelmate.trips.domain.recipe.Ingredient;
import de.evia.travelmate.trips.domain.recipe.Recipe;

public final class IngredientAggregator {

    private IngredientAggregator() {
    }

    public static List<ShoppingItem> aggregate(final MealPlan mealPlan,
                                               final Map<java.util.UUID, Recipe> recipesById,
                                               final int participantCount) {
        argumentIsNotNull(mealPlan, "mealPlan");
        argumentIsNotNull(recipesById, "recipesById");
        argumentIsTrue(participantCount > 0, "Participant count must be greater than zero.");

        final Map<String, AggregatedEntry> aggregated = new LinkedHashMap<>();

        for (final MealSlot slot : mealPlan.slots()) {
            if (slot.status() != MealSlotStatus.PLANNED || slot.recipeId() == null) {
                continue;
            }

            final Recipe recipe = recipesById.get(slot.recipeId());
            if (recipe == null || recipe.servings().value() == 0) {
                continue;
            }

            final BigDecimal scaleFactor = BigDecimal.valueOf(participantCount)
                .divide(BigDecimal.valueOf(recipe.servings().value()), 10, RoundingMode.HALF_UP);

            for (final Ingredient ingredient : recipe.ingredients()) {
                final String key = ingredient.name().trim().toLowerCase()
                    + "||" + ingredient.unit().trim().toLowerCase();
                final BigDecimal scaled = ingredient.quantity().multiply(scaleFactor)
                    .setScale(2, RoundingMode.HALF_UP);

                aggregated.merge(key, new AggregatedEntry(ingredient.name().trim(),
                    scaled, ingredient.unit().trim()), AggregatedEntry::add);
            }
        }

        final List<ShoppingItem> result = new ArrayList<>();
        for (final AggregatedEntry entry : aggregated.values()) {
            result.add(new ShoppingItem(entry.name, entry.quantity, entry.unit, ItemSource.RECIPE));
        }
        return result;
    }

    private static final class AggregatedEntry {

        private final String name;
        private BigDecimal quantity;
        private final String unit;

        AggregatedEntry(final String name, final BigDecimal quantity, final String unit) {
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
        }

        AggregatedEntry add(final AggregatedEntry other) {
            this.quantity = this.quantity.add(other.quantity);
            return this;
        }
    }
}
