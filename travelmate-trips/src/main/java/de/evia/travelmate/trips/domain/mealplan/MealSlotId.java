package de.evia.travelmate.trips.domain.mealplan;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record MealSlotId(UUID value) {

    public MealSlotId {
        argumentIsNotNull(value, "mealSlotId");
    }
}
