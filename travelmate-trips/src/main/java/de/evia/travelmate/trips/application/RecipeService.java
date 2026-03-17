package de.evia.travelmate.trips.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.command.CreateRecipeCommand;
import de.evia.travelmate.trips.application.command.DeleteRecipeCommand;
import de.evia.travelmate.trips.application.command.IngredientCommand;
import de.evia.travelmate.trips.application.command.UpdateRecipeCommand;
import de.evia.travelmate.trips.application.representation.RecipeRepresentation;
import de.evia.travelmate.trips.domain.mealplan.MealPlanRepository;
import de.evia.travelmate.trips.domain.recipe.Ingredient;
import de.evia.travelmate.trips.domain.recipe.Recipe;
import de.evia.travelmate.trips.domain.recipe.RecipeId;
import de.evia.travelmate.trips.domain.recipe.RecipeName;
import de.evia.travelmate.trips.domain.recipe.RecipeRepository;
import de.evia.travelmate.trips.domain.recipe.Servings;

@Service
@Transactional
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final MealPlanRepository mealPlanRepository;

    public RecipeService(final RecipeRepository recipeRepository,
                         final MealPlanRepository mealPlanRepository) {
        this.recipeRepository = recipeRepository;
        this.mealPlanRepository = mealPlanRepository;
    }

    public RecipeRepresentation createRecipe(final CreateRecipeCommand command) {
        final List<Ingredient> ingredients = toIngredients(command.ingredients());
        final Recipe recipe = Recipe.create(
            new TenantId(command.tenantId()),
            new RecipeName(command.name()),
            new Servings(command.servings()),
            ingredients
        );
        recipeRepository.save(recipe);
        return new RecipeRepresentation(recipe);
    }

    public RecipeRepresentation updateRecipe(final UpdateRecipeCommand command) {
        final Recipe recipe = findRecipe(new RecipeId(command.recipeId()));
        final List<Ingredient> ingredients = toIngredients(command.ingredients());
        recipe.update(
            new RecipeName(command.name()),
            new Servings(command.servings()),
            ingredients
        );
        recipeRepository.save(recipe);
        return new RecipeRepresentation(recipe);
    }

    public void deleteRecipe(final DeleteRecipeCommand command) {
        final RecipeId recipeId = new RecipeId(command.recipeId());
        findRecipe(recipeId);
        if (mealPlanRepository.existsSlotWithRecipe(recipeId.value())) {
            throw new IllegalStateException(
                "Recipe " + recipeId.value() + " is still assigned to a meal plan slot.");
        }
        recipeRepository.deleteById(recipeId);
    }

    @Transactional(readOnly = true)
    public RecipeRepresentation findById(final RecipeId recipeId) {
        return new RecipeRepresentation(findRecipe(recipeId));
    }

    @Transactional(readOnly = true)
    public List<RecipeRepresentation> findAllByTenantId(final TenantId tenantId) {
        return recipeRepository.findAllByTenantId(tenantId).stream()
            .map(RecipeRepresentation::new)
            .toList();
    }

    private Recipe findRecipe(final RecipeId recipeId) {
        return recipeRepository.findById(recipeId)
            .orElseThrow(() -> new EntityNotFoundException("Recipe", recipeId.value().toString()));
    }

    private List<Ingredient> toIngredients(final List<IngredientCommand> commands) {
        return commands.stream()
            .map(ic -> new Ingredient(ic.name(), ic.quantity(), ic.unit()))
            .toList();
    }
}
