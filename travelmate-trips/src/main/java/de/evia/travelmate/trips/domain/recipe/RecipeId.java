package de.evia.travelmate.trips.domain.recipe;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record RecipeId(UUID value) {

    public RecipeId {
        argumentIsNotNull(value, "recipeId");
    }
}
