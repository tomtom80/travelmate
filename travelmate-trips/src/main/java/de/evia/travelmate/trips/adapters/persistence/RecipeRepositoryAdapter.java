package de.evia.travelmate.trips.adapters.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.recipe.Ingredient;
import de.evia.travelmate.trips.domain.recipe.Recipe;
import de.evia.travelmate.trips.domain.recipe.RecipeId;
import de.evia.travelmate.trips.domain.recipe.RecipeName;
import de.evia.travelmate.trips.domain.recipe.RecipeRepository;
import de.evia.travelmate.trips.domain.recipe.Servings;

@Repository
public class RecipeRepositoryAdapter implements RecipeRepository {

    private final RecipeJpaRepository jpaRepository;

    public RecipeRepositoryAdapter(final RecipeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Recipe save(final Recipe recipe) {
        final RecipeJpaEntity entity = jpaRepository.findById(recipe.recipeId().value())
            .orElseGet(() -> new RecipeJpaEntity(
                recipe.recipeId().value(),
                recipe.tenantId().value(),
                recipe.name().value(),
                recipe.servings().value()
            ));
        entity.setName(recipe.name().value());
        entity.setServings(recipe.servings().value());
        syncIngredients(entity, recipe);
        jpaRepository.save(entity);
        return recipe;
    }

    @Override
    public Optional<Recipe> findById(final RecipeId recipeId) {
        return jpaRepository.findById(recipeId.value()).map(this::toDomain);
    }

    @Override
    public List<Recipe> findAllByTenantId(final TenantId tenantId) {
        return jpaRepository.findAllByTenantId(tenantId.value()).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public void deleteById(final RecipeId recipeId) {
        jpaRepository.deleteById(recipeId.value());
    }

    private void syncIngredients(final RecipeJpaEntity entity, final Recipe recipe) {
        entity.getIngredients().clear();
        for (final Ingredient ingredient : recipe.ingredients()) {
            entity.getIngredients().add(new IngredientJpaEntity(
                entity, ingredient.name(), ingredient.quantity(), ingredient.unit()
            ));
        }
    }

    private Recipe toDomain(final RecipeJpaEntity entity) {
        final var ingredients = entity.getIngredients().stream()
            .map(i -> new Ingredient(i.getName(), i.getQuantity(), i.getUnit()))
            .toList();
        return new Recipe(
            new RecipeId(entity.getRecipeId()),
            new TenantId(entity.getTenantId()),
            new RecipeName(entity.getName()),
            new Servings(entity.getServings()),
            ingredients
        );
    }
}
