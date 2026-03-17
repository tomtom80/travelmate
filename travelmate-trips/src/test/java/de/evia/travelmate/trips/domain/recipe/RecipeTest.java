package de.evia.travelmate.trips.domain.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.domain.TenantId;

class RecipeTest {

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final RecipeName NAME = new RecipeName("Spaghetti Bolognese");
    private static final Servings SERVINGS = new Servings(4);
    private static final List<Ingredient> INGREDIENTS = List.of(
        new Ingredient("Spaghetti", new BigDecimal("500"), "g"),
        new Ingredient("Hackfleisch", new BigDecimal("400"), "g")
    );

    @Test
    void createCreatesRecipeWithAllFields() {
        final Recipe recipe = Recipe.create(TENANT_ID, NAME, SERVINGS, INGREDIENTS);

        assertThat(recipe.recipeId()).isNotNull();
        assertThat(recipe.tenantId()).isEqualTo(TENANT_ID);
        assertThat(recipe.name()).isEqualTo(NAME);
        assertThat(recipe.servings()).isEqualTo(SERVINGS);
        assertThat(recipe.ingredients()).hasSize(2);
    }

    @Test
    void createRejectsEmptyIngredients() {
        assertThatThrownBy(() -> Recipe.create(TENANT_ID, NAME, SERVINGS, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one ingredient");
    }

    @Test
    void createRejectsNullName() {
        assertThatThrownBy(() -> Recipe.create(TENANT_ID, null, SERVINGS, INGREDIENTS))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateReplacesAllFields() {
        final Recipe recipe = Recipe.create(TENANT_ID, NAME, SERVINGS, INGREDIENTS);
        final RecipeName newName = new RecipeName("Lasagne");
        final Servings newServings = new Servings(6);
        final List<Ingredient> newIngredients = List.of(
            new Ingredient("Lasagneplatten", new BigDecimal("250"), "g"),
            new Ingredient("Bechamelsauce", new BigDecimal("500"), "ml"),
            new Ingredient("Hackfleisch", new BigDecimal("500"), "g")
        );

        recipe.update(newName, newServings, newIngredients);

        assertThat(recipe.name()).isEqualTo(newName);
        assertThat(recipe.servings()).isEqualTo(newServings);
        assertThat(recipe.ingredients()).hasSize(3);
        assertThat(recipe.ingredients().getFirst().name()).isEqualTo("Lasagneplatten");
    }

    @Test
    void updateRejectsEmptyIngredients() {
        final Recipe recipe = Recipe.create(TENANT_ID, NAME, SERVINGS, INGREDIENTS);

        assertThatThrownBy(() -> recipe.update(NAME, SERVINGS, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one ingredient");
    }

    @Test
    void ingredientsListIsUnmodifiable() {
        final Recipe recipe = Recipe.create(TENANT_ID, NAME, SERVINGS, INGREDIENTS);

        assertThatThrownBy(() -> recipe.ingredients().add(
            new Ingredient("Olivenoel", new BigDecimal("2"), "EL")))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
