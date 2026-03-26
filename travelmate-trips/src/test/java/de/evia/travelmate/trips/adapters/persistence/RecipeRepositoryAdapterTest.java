package de.evia.travelmate.trips.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.recipe.Ingredient;
import de.evia.travelmate.trips.domain.recipe.Recipe;
import de.evia.travelmate.trips.domain.recipe.RecipeId;
import de.evia.travelmate.trips.domain.recipe.RecipeName;
import de.evia.travelmate.trips.domain.recipe.RecipeRepository;
import de.evia.travelmate.trips.domain.recipe.Servings;

@SpringBootTest
@ActiveProfiles("test")
class RecipeRepositoryAdapterTest {

    @Autowired
    private RecipeRepository repository;

    @Test
    void savesAndFindsRecipe() {
        final Recipe recipe = Recipe.createPersonal(
            new TenantId(UUID.randomUUID()),
            new RecipeName("Spaghetti Bolognese"),
            new Servings(4),
            List.of(new Ingredient("Spaghetti", new BigDecimal("500"), "g"))
        );

        repository.save(recipe);

        final Optional<Recipe> found = repository.findById(recipe.recipeId());
        assertThat(found).isPresent();
        assertThat(found.get().name().value()).isEqualTo("Spaghetti Bolognese");
        assertThat(found.get().servings().value()).isEqualTo(4);
        assertThat(found.get().ingredients()).hasSize(1);
    }

    @Test
    void savesRecipeWithMultipleIngredients() {
        final Recipe recipe = Recipe.createPersonal(
            new TenantId(UUID.randomUUID()),
            new RecipeName("Lasagne"),
            new Servings(6),
            List.of(
                new Ingredient("Lasagneplatten", new BigDecimal("250"), "g"),
                new Ingredient("Hackfleisch", new BigDecimal("500"), "g"),
                new Ingredient("Bechamelsauce", new BigDecimal("500"), "ml")
            )
        );

        repository.save(recipe);

        final Recipe found = repository.findById(recipe.recipeId()).orElseThrow();
        assertThat(found.ingredients()).hasSize(3);
    }

    @Test
    void updatesRecipeAndReplacesIngredients() {
        final Recipe recipe = Recipe.createPersonal(
            new TenantId(UUID.randomUUID()),
            new RecipeName("Original"),
            new Servings(2),
            List.of(new Ingredient("OldIngredient", new BigDecimal("100"), "g"))
        );
        repository.save(recipe);

        recipe.update(
            new RecipeName("Updated"),
            new Servings(8),
            List.of(
                new Ingredient("NewIngredient1", new BigDecimal("200"), "ml"),
                new Ingredient("NewIngredient2", new BigDecimal("300"), "g")
            )
        );
        repository.save(recipe);

        final Recipe found = repository.findById(recipe.recipeId()).orElseThrow();
        assertThat(found.name().value()).isEqualTo("Updated");
        assertThat(found.servings().value()).isEqualTo(8);
        assertThat(found.ingredients()).hasSize(2);
        assertThat(found.ingredients().getFirst().name()).isEqualTo("NewIngredient1");
    }

    @Test
    void findsAllByTenantId() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final Recipe r1 = Recipe.createPersonal(tenantId, new RecipeName("Recipe A"), new Servings(1),
            List.of(new Ingredient("A", new BigDecimal("1"), "g")));
        final Recipe r2 = Recipe.createPersonal(tenantId, new RecipeName("Recipe B"), new Servings(2),
            List.of(new Ingredient("B", new BigDecimal("2"), "ml")));
        repository.save(r1);
        repository.save(r2);

        final List<Recipe> found = repository.findAllPersonalByTenantId(tenantId);
        assertThat(found).hasSize(2);
    }

    @Test
    void deletesRecipe() {
        final Recipe recipe = Recipe.createPersonal(
            new TenantId(UUID.randomUUID()),
            new RecipeName("To Delete"),
            new Servings(1),
            List.of(new Ingredient("X", new BigDecimal("1"), "g"))
        );
        repository.save(recipe);

        repository.deleteById(recipe.recipeId());

        assertThat(repository.findById(recipe.recipeId())).isEmpty();
    }

    @Test
    void returnsEmptyForUnknownId() {
        final Optional<Recipe> found = repository.findById(new RecipeId(UUID.randomUUID()));
        assertThat(found).isEmpty();
    }
}
