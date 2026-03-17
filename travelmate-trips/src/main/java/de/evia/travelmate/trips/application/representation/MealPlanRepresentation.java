package de.evia.travelmate.trips.application.representation;

import java.util.List;
import java.util.UUID;

import de.evia.travelmate.trips.domain.mealplan.MealPlan;

public record MealPlanRepresentation(
    UUID mealPlanId,
    UUID tenantId,
    UUID tripId,
    List<MealSlotRepresentation> slots
) {

    public MealPlanRepresentation(final MealPlan mealPlan) {
        this(
            mealPlan.mealPlanId().value(),
            mealPlan.tenantId().value(),
            mealPlan.tripId().value(),
            mealPlan.slots().stream()
                .map(MealSlotRepresentation::new)
                .toList()
        );
    }
}
