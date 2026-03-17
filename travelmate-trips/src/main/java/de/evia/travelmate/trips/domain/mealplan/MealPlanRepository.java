package de.evia.travelmate.trips.domain.mealplan;

import java.util.Optional;
import java.util.UUID;

import de.evia.travelmate.trips.domain.trip.TripId;

public interface MealPlanRepository {

    MealPlan save(MealPlan mealPlan);

    Optional<MealPlan> findByTripId(TripId tripId);

    boolean existsByTripId(TripId tripId);

    boolean existsSlotWithRecipe(UUID recipeId);
}
