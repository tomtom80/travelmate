package de.evia.travelmate.trips.application.representation;

import java.math.BigDecimal;

import de.evia.travelmate.trips.domain.recipe.Ingredient;

public record IngredientRepresentation(
    String name,
    BigDecimal quantity,
    String unit
) {

    public IngredientRepresentation(final Ingredient ingredient) {
        this(ingredient.name(), ingredient.quantity(), ingredient.unit());
    }
}
