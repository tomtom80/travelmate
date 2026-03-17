package de.evia.travelmate.trips.domain.shoppinglist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.mealplan.MealPlan;
import de.evia.travelmate.trips.domain.mealplan.MealSlot;
import de.evia.travelmate.trips.domain.mealplan.MealSlotId;
import de.evia.travelmate.trips.domain.mealplan.MealSlotStatus;
import de.evia.travelmate.trips.domain.mealplan.MealType;
import de.evia.travelmate.trips.domain.recipe.Ingredient;
import de.evia.travelmate.trips.domain.recipe.Recipe;
import de.evia.travelmate.trips.domain.recipe.RecipeName;
import de.evia.travelmate.trips.domain.recipe.Servings;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.TripId;

class IngredientAggregatorTest {

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final TripId TRIP_ID = new TripId(UUID.randomUUID());

    @Test
    void aggregatesAndScalesIngredients() {
        final Recipe pasta = Recipe.create(TENANT_ID, new RecipeName("Pasta"), new Servings(4), List.of(
            new Ingredient("Spaghetti", new BigDecimal("500"), "g"),
            new Ingredient("Tomaten", new BigDecimal("800"), "g")
        ));
        final MealPlan plan = createMealPlanWithRecipe(pasta.recipeId().value());

        final List<ShoppingItem> items = IngredientAggregator.aggregate(
            plan, Map.of(pasta.recipeId().value(), pasta), 8);

        assertThat(items).hasSize(2);
        final ShoppingItem spaghetti = findByName(items, "Spaghetti");
        assertThat(spaghetti.quantity()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(spaghetti.unit()).isEqualTo("g");
        assertThat(spaghetti.source()).isEqualTo(ItemSource.RECIPE);

        final ShoppingItem tomaten = findByName(items, "Tomaten");
        assertThat(tomaten.quantity()).isEqualByComparingTo(new BigDecimal("1600.00"));
    }

    @Test
    void aggregatesSameIngredientAcrossRecipes() {
        final Recipe pasta = Recipe.create(TENANT_ID, new RecipeName("Pasta"), new Servings(4), List.of(
            new Ingredient("Tomaten", new BigDecimal("500"), "g")
        ));
        final Recipe soup = Recipe.create(TENANT_ID, new RecipeName("Suppe"), new Servings(4), List.of(
            new Ingredient("Tomaten", new BigDecimal("600"), "g")
        ));
        final MealPlan plan = createMealPlanWithTwoRecipes(
            pasta.recipeId().value(), soup.recipeId().value());

        final List<ShoppingItem> items = IngredientAggregator.aggregate(
            plan,
            Map.of(pasta.recipeId().value(), pasta, soup.recipeId().value(), soup),
            4);

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().name()).isEqualTo("Tomaten");
        assertThat(items.getFirst().quantity()).isEqualByComparingTo(new BigDecimal("1100.00"));
    }

    @Test
    void caseInsensitiveIngredientMatching() {
        final Recipe recipe1 = Recipe.create(TENANT_ID, new RecipeName("R1"), new Servings(2), List.of(
            new Ingredient("tomaten", new BigDecimal("300"), "g")
        ));
        final Recipe recipe2 = Recipe.create(TENANT_ID, new RecipeName("R2"), new Servings(2), List.of(
            new Ingredient("Tomaten", new BigDecimal("400"), "G")
        ));
        final MealPlan plan = createMealPlanWithTwoRecipes(
            recipe1.recipeId().value(), recipe2.recipeId().value());

        final List<ShoppingItem> items = IngredientAggregator.aggregate(
            plan,
            Map.of(recipe1.recipeId().value(), recipe1, recipe2.recipeId().value(), recipe2),
            2);

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().quantity()).isEqualByComparingTo(new BigDecimal("700.00"));
    }

    @Test
    void differentUnitsAreNotAggregated() {
        final Recipe recipe = Recipe.create(TENANT_ID, new RecipeName("Mixed"), new Servings(2), List.of(
            new Ingredient("Salz", new BigDecimal("10"), "g"),
            new Ingredient("Salz", new BigDecimal("2"), "TL")
        ));
        final MealPlan plan = createMealPlanWithRecipe(recipe.recipeId().value());

        final List<ShoppingItem> items = IngredientAggregator.aggregate(
            plan, Map.of(recipe.recipeId().value(), recipe), 2);

        assertThat(items).hasSize(2);
    }

    @Test
    void excludesSkipSlots() {
        final Recipe recipe = Recipe.create(TENANT_ID, new RecipeName("Pasta"), new Servings(2), List.of(
            new Ingredient("Nudeln", new BigDecimal("250"), "g")
        ));
        final MealSlot skipSlot = new MealSlot(
            new MealSlotId(UUID.randomUUID()), LocalDate.of(2026, 7, 15),
            MealType.LUNCH, MealSlotStatus.SKIP, recipe.recipeId().value());
        final MealPlan plan = new MealPlan(
            new de.evia.travelmate.trips.domain.mealplan.MealPlanId(UUID.randomUUID()),
            TENANT_ID, TRIP_ID, List.of(skipSlot));

        final List<ShoppingItem> items = IngredientAggregator.aggregate(
            plan, Map.of(recipe.recipeId().value(), recipe), 4);

        assertThat(items).isEmpty();
    }

    @Test
    void excludesEatingOutSlots() {
        final Recipe recipe = Recipe.create(TENANT_ID, new RecipeName("Pasta"), new Servings(2), List.of(
            new Ingredient("Nudeln", new BigDecimal("250"), "g")
        ));
        final MealSlot eatingOutSlot = new MealSlot(
            new MealSlotId(UUID.randomUUID()), LocalDate.of(2026, 7, 15),
            MealType.DINNER, MealSlotStatus.EATING_OUT, recipe.recipeId().value());
        final MealPlan plan = new MealPlan(
            new de.evia.travelmate.trips.domain.mealplan.MealPlanId(UUID.randomUUID()),
            TENANT_ID, TRIP_ID, List.of(eatingOutSlot));

        final List<ShoppingItem> items = IngredientAggregator.aggregate(
            plan, Map.of(recipe.recipeId().value(), recipe), 4);

        assertThat(items).isEmpty();
    }

    @Test
    void excludesSlotsWithNoRecipeAssigned() {
        final MealSlot emptySlot = new MealSlot(LocalDate.of(2026, 7, 15), MealType.BREAKFAST);
        final MealPlan plan = new MealPlan(
            new de.evia.travelmate.trips.domain.mealplan.MealPlanId(UUID.randomUUID()),
            TENANT_ID, TRIP_ID, List.of(emptySlot));

        final List<ShoppingItem> items = IngredientAggregator.aggregate(
            plan, Map.of(), 4);

        assertThat(items).isEmpty();
    }

    @Test
    void scalesWithFractionalFactor() {
        final Recipe recipe = Recipe.create(TENANT_ID, new RecipeName("Cake"), new Servings(4), List.of(
            new Ingredient("Mehl", new BigDecimal("400"), "g")
        ));
        final MealPlan plan = createMealPlanWithRecipe(recipe.recipeId().value());

        final List<ShoppingItem> items = IngredientAggregator.aggregate(
            plan, Map.of(recipe.recipeId().value(), recipe), 3);

        assertThat(items).hasSize(1);
        // 400 * (3/4) = 400 * 0.75 = 300.00
        assertThat(items.getFirst().quantity()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void skipsRecipeNotFoundInMap() {
        final UUID unknownRecipeId = UUID.randomUUID();
        final MealSlot slot = new MealSlot(
            new MealSlotId(UUID.randomUUID()), LocalDate.of(2026, 7, 15),
            MealType.LUNCH, MealSlotStatus.PLANNED, unknownRecipeId);
        final MealPlan plan = new MealPlan(
            new de.evia.travelmate.trips.domain.mealplan.MealPlanId(UUID.randomUUID()),
            TENANT_ID, TRIP_ID, List.of(slot));

        final List<ShoppingItem> items = IngredientAggregator.aggregate(
            plan, Map.of(), 4);

        assertThat(items).isEmpty();
    }

    @Test
    void rejectsZeroParticipantCount() {
        final MealPlan plan = MealPlan.generate(TENANT_ID, TRIP_ID,
            new DateRange(LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 16)));

        assertThatThrownBy(() -> IngredientAggregator.aggregate(plan, Map.of(), 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("greater than zero");
    }

    private MealPlan createMealPlanWithRecipe(final UUID recipeId) {
        final MealSlot slot = new MealSlot(
            new MealSlotId(UUID.randomUUID()), LocalDate.of(2026, 7, 15),
            MealType.LUNCH, MealSlotStatus.PLANNED, recipeId);
        return new MealPlan(
            new de.evia.travelmate.trips.domain.mealplan.MealPlanId(UUID.randomUUID()),
            TENANT_ID, TRIP_ID, List.of(slot));
    }

    private MealPlan createMealPlanWithTwoRecipes(final UUID recipeId1, final UUID recipeId2) {
        final MealSlot slot1 = new MealSlot(
            new MealSlotId(UUID.randomUUID()), LocalDate.of(2026, 7, 15),
            MealType.LUNCH, MealSlotStatus.PLANNED, recipeId1);
        final MealSlot slot2 = new MealSlot(
            new MealSlotId(UUID.randomUUID()), LocalDate.of(2026, 7, 15),
            MealType.DINNER, MealSlotStatus.PLANNED, recipeId2);
        return new MealPlan(
            new de.evia.travelmate.trips.domain.mealplan.MealPlanId(UUID.randomUUID()),
            TENANT_ID, TRIP_ID, List.of(slot1, slot2));
    }

    private ShoppingItem findByName(final List<ShoppingItem> items, final String name) {
        return items.stream()
            .filter(i -> i.name().equalsIgnoreCase(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Item not found: " + name));
    }
}
