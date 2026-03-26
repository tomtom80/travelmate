package de.evia.travelmate.trips.application.representation;

import java.util.List;
import java.util.UUID;

import de.evia.travelmate.trips.domain.recipe.Recipe;

public record RecipeRepresentation(
    UUID recipeId,
    UUID tenantId,
    UUID tripId,
    String contributedBy,
    String name,
    int servings,
    List<IngredientRepresentation> ingredients
) {

    public RecipeRepresentation(final Recipe recipe) {
        this(
            recipe.recipeId().value(),
            recipe.tenantId().value(),
            recipe.tripId() != null ? recipe.tripId().value() : null,
            recipe.contributedBy(),
            recipe.name().value(),
            recipe.servings().value(),
            recipe.ingredients().stream()
                .map(IngredientRepresentation::new)
                .toList()
        );
    }

    public boolean isPersonal() {
        return tripId == null;
    }
}
