package de.evia.travelmate.trips.domain.recipe;

import java.util.List;
import java.util.Optional;

import de.evia.travelmate.common.domain.TenantId;

public interface RecipeRepository {

    Recipe save(Recipe recipe);

    Optional<Recipe> findById(RecipeId recipeId);

    List<Recipe> findAllByTenantId(TenantId tenantId);

    void deleteById(RecipeId recipeId);
}
