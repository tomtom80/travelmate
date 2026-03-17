package de.evia.travelmate.trips.domain.recipe;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

public record RecipeName(String value) {

    private static final int MAX_LENGTH = 200;

    public RecipeName {
        argumentIsNotBlank(value, "recipeName");
        argumentIsTrue(value.length() <= MAX_LENGTH,
            "Recipe name must not exceed " + MAX_LENGTH + " characters.");
    }
}
