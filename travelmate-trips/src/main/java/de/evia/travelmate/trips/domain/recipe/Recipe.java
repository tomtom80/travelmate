package de.evia.travelmate.trips.domain.recipe;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import de.evia.travelmate.common.domain.AggregateRoot;
import de.evia.travelmate.common.domain.TenantId;

public class Recipe extends AggregateRoot {

    private final RecipeId recipeId;
    private final TenantId tenantId;
    private RecipeName name;
    private Servings servings;
    private final List<Ingredient> ingredients;

    public Recipe(final RecipeId recipeId,
                  final TenantId tenantId,
                  final RecipeName name,
                  final Servings servings,
                  final List<Ingredient> ingredients) {
        argumentIsNotNull(recipeId, "recipeId");
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(name, "name");
        argumentIsNotNull(servings, "servings");
        argumentIsNotNull(ingredients, "ingredients");
        argumentIsTrue(!ingredients.isEmpty(), "A recipe must have at least one ingredient.");
        this.recipeId = recipeId;
        this.tenantId = tenantId;
        this.name = name;
        this.servings = servings;
        this.ingredients = new ArrayList<>(ingredients);
    }

    public static Recipe create(final TenantId tenantId,
                                final RecipeName name,
                                final Servings servings,
                                final List<Ingredient> ingredients) {
        return new Recipe(
            new RecipeId(UUID.randomUUID()),
            tenantId,
            name,
            servings,
            ingredients
        );
    }

    public void update(final RecipeName name,
                       final Servings servings,
                       final List<Ingredient> ingredients) {
        argumentIsNotNull(name, "name");
        argumentIsNotNull(servings, "servings");
        argumentIsNotNull(ingredients, "ingredients");
        argumentIsTrue(!ingredients.isEmpty(), "A recipe must have at least one ingredient.");
        this.name = name;
        this.servings = servings;
        this.ingredients.clear();
        this.ingredients.addAll(ingredients);
    }

    public RecipeId recipeId() {
        return recipeId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public RecipeName name() {
        return name;
    }

    public Servings servings() {
        return servings;
    }

    public List<Ingredient> ingredients() {
        return Collections.unmodifiableList(ingredients);
    }
}
