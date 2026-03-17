package de.evia.travelmate.trips.domain.recipe;

import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

public record Servings(int value) {

    public Servings {
        argumentIsTrue(value > 0, "Servings must be greater than zero.");
    }
}
