package de.evia.travelmate.trips.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private MealPlanRepository mealPlanRepository;

    @InjectMocks
    private RecipeService recipeService;

    @Test
    void createRecipeReturnsRepresentation() {
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        final CreateRecipeCommand command = new CreateRecipeCommand(
            TENANT_UUID, "Spaghetti Bolognese", 4,
            List.of(new IngredientCommand("Spaghetti", new BigDecimal("500"), "g"))
        );

        final RecipeRepresentation result = recipeService.createRecipe(command);

        assertThat(result.name()).isEqualTo("Spaghetti Bolognese");
        assertThat(result.servings()).isEqualTo(4);
        assertThat(result.ingredients()).hasSize(1);
        verify(recipeRepository).save(any(Recipe.class));
    }

    @Test
    void createRecipeSavesAllIngredients() {
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        final CreateRecipeCommand command = new CreateRecipeCommand(
            TENANT_UUID, "Lasagne", 6,
            List.of(
                new IngredientCommand("Lasagneplatten", new BigDecimal("250"), "g"),
                new IngredientCommand("Hackfleisch", new BigDecimal("500"), "g"),
                new IngredientCommand("Bechamelsauce", new BigDecimal("500"), "ml")
            )
        );

        final RecipeRepresentation result = recipeService.createRecipe(command);

        final ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).save(captor.capture());
        assertThat(captor.getValue().ingredients()).hasSize(3);
    }

    @Test
    void updateRecipeChangesFields() {
        final Recipe existing = Recipe.create(
            new TenantId(TENANT_UUID), new RecipeName("Old Name"), new Servings(2),
            List.of(new Ingredient("Mehl", new BigDecimal("200"), "g"))
        );
        when(recipeRepository.findById(existing.recipeId())).thenReturn(Optional.of(existing));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        final UpdateRecipeCommand command = new UpdateRecipeCommand(
            existing.recipeId().value(), "New Name", 8,
            List.of(new IngredientCommand("Zucker", new BigDecimal("100"), "g"))
        );

        final RecipeRepresentation result = recipeService.updateRecipe(command);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.servings()).isEqualTo(8);
        assertThat(result.ingredients()).hasSize(1);
        assertThat(result.ingredients().getFirst().name()).isEqualTo("Zucker");
    }

    @Test
    void deleteRecipeRemovesFromRepository() {
        final Recipe existing = Recipe.create(
            new TenantId(TENANT_UUID), new RecipeName("To Delete"), new Servings(1),
            List.of(new Ingredient("X", new BigDecimal("1"), "g"))
        );
        when(recipeRepository.findById(existing.recipeId())).thenReturn(Optional.of(existing));
        when(mealPlanRepository.existsSlotWithRecipe(existing.recipeId().value())).thenReturn(false);

        recipeService.deleteRecipe(new DeleteRecipeCommand(existing.recipeId().value()));

        verify(recipeRepository).deleteById(existing.recipeId());
    }

    @Test
    void deleteRecipeThrowsWhenNotFound() {
        final UUID unknownId = UUID.randomUUID();
        when(recipeRepository.findById(new RecipeId(unknownId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.deleteRecipe(new DeleteRecipeCommand(unknownId)))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteRecipeRejectsWhenAssignedToMealSlot() {
        final Recipe existing = Recipe.create(
            new TenantId(TENANT_UUID), new RecipeName("In Use"), new Servings(2),
            List.of(new Ingredient("Y", new BigDecimal("100"), "g"))
        );
        when(recipeRepository.findById(existing.recipeId())).thenReturn(Optional.of(existing));
        when(mealPlanRepository.existsSlotWithRecipe(existing.recipeId().value())).thenReturn(true);

        assertThatThrownBy(() -> recipeService.deleteRecipe(new DeleteRecipeCommand(existing.recipeId().value())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("still assigned");
    }

    @Test
    void findByIdReturnsRepresentation() {
        final Recipe recipe = Recipe.create(
            new TenantId(TENANT_UUID), new RecipeName("Pancakes"), new Servings(4),
            List.of(new Ingredient("Mehl", new BigDecimal("200"), "g"))
        );
        when(recipeRepository.findById(recipe.recipeId())).thenReturn(Optional.of(recipe));

        final RecipeRepresentation result = recipeService.findById(recipe.recipeId());

        assertThat(result.name()).isEqualTo("Pancakes");
    }

    @Test
    void findAllByTenantIdReturnsRepresentations() {
        final TenantId tenantId = new TenantId(TENANT_UUID);
        final Recipe r1 = Recipe.create(tenantId, new RecipeName("A"), new Servings(1),
            List.of(new Ingredient("X", new BigDecimal("1"), "g")));
        final Recipe r2 = Recipe.create(tenantId, new RecipeName("B"), new Servings(2),
            List.of(new Ingredient("Y", new BigDecimal("2"), "ml")));
        when(recipeRepository.findAllByTenantId(tenantId)).thenReturn(List.of(r1, r2));

        final List<RecipeRepresentation> results = recipeService.findAllByTenantId(tenantId);

        assertThat(results).hasSize(2);
    }
}
