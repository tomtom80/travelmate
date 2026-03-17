package de.evia.travelmate.trips.application.command;

import java.util.List;
import java.util.UUID;

public record UpdateRecipeCommand(
    UUID recipeId,
    String name,
    int servings,
    List<IngredientCommand> ingredients
) {
}
