package de.evia.travelmate.trips.domain.mealplan;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record MealPlanId(UUID value) {

    public MealPlanId {
        argumentIsNotNull(value, "mealPlanId");
    }
}
