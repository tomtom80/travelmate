package de.evia.travelmate.trips.domain.recipe;

import java.util.List;
import java.util.Optional;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.TripId;

public interface RecipeRepository {

    Recipe save(Recipe recipe);

    Optional<Recipe> findById(RecipeId recipeId);

    List<Recipe> findAllPersonalByTenantId(TenantId tenantId);

    List<Recipe> findAllByTripId(TripId tripId);

    void deleteById(RecipeId recipeId);
}
