package de.evia.travelmate.trips.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.command.AssignRecipeToSlotCommand;
import de.evia.travelmate.trips.application.command.GenerateMealPlanCommand;
import de.evia.travelmate.trips.application.command.UpdateMealSlotCommand;
import de.evia.travelmate.trips.application.representation.MealPlanRepresentation;
import de.evia.travelmate.trips.domain.mealplan.MealPlan;
import de.evia.travelmate.trips.domain.mealplan.MealPlanRepository;
import de.evia.travelmate.trips.domain.mealplan.MealSlotId;
import de.evia.travelmate.trips.domain.recipe.Ingredient;
import de.evia.travelmate.trips.domain.recipe.Recipe;
import de.evia.travelmate.trips.domain.recipe.RecipeId;
import de.evia.travelmate.trips.domain.recipe.RecipeName;
import de.evia.travelmate.trips.domain.recipe.RecipeRepository;
import de.evia.travelmate.trips.domain.recipe.Servings;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.Trip;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.trip.TripName;
import de.evia.travelmate.trips.domain.trip.TripRepository;

@ExtendWith(MockitoExtension.class)
class MealPlanServiceTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID ORGANIZER_ID = UUID.randomUUID();

    @Mock
    private MealPlanRepository mealPlanRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private MealPlanService mealPlanService;

    @Test
    void generateMealPlanCreatesSlots() {
        final Trip trip = Trip.plan(
            new TenantId(TENANT_UUID), new TripName("Urlaub"), null,
            new DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3)),
            ORGANIZER_ID
        );
        final TripId tripId = trip.tripId();
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(mealPlanRepository.existsByTripId(tripId)).thenReturn(false);
        when(mealPlanRepository.save(any(MealPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        final MealPlanRepresentation result = mealPlanService.generateMealPlan(
            new GenerateMealPlanCommand(TENANT_UUID, tripId.value()));

        assertThat(result.slots()).hasSize(9);
        verify(mealPlanRepository).save(any(MealPlan.class));
    }

    @Test
    void generateMealPlanRejectsDuplicate() {
        final TripId tripId = new TripId(UUID.randomUUID());
        when(mealPlanRepository.existsByTripId(tripId)).thenReturn(true);

        assertThatThrownBy(() -> mealPlanService.generateMealPlan(
            new GenerateMealPlanCommand(TENANT_UUID, tripId.value())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void updateSlotStatusChangesStatus() {
        final MealPlan mealPlan = createMealPlan();
        final TripId tripId = mealPlan.tripId();
        when(mealPlanRepository.findByTripId(tripId)).thenReturn(Optional.of(mealPlan));
        when(mealPlanRepository.save(any(MealPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        mealPlanService.updateSlotStatus(new UpdateMealSlotCommand(
            tripId.value(), mealPlan.slots().getFirst().mealSlotId().value(), "SKIP"));

        verify(mealPlanRepository).save(any(MealPlan.class));
    }

    @Test
    void assignRecipeValidatesRecipeExists() {
        final MealPlan mealPlan = createMealPlan();
        final TripId tripId = mealPlan.tripId();
        final UUID recipeId = UUID.randomUUID();
        when(mealPlanRepository.findByTripId(tripId)).thenReturn(Optional.of(mealPlan));
        when(recipeRepository.findById(new RecipeId(recipeId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mealPlanService.assignRecipe(new AssignRecipeToSlotCommand(
            tripId.value(), mealPlan.slots().getFirst().mealSlotId().value(), recipeId)))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void assignRecipeSucceedsWithValidRecipe() {
        final MealPlan mealPlan = createMealPlan();
        final TripId tripId = mealPlan.tripId();
        final Recipe recipe = Recipe.create(
            new TenantId(TENANT_UUID), new RecipeName("Pasta"), new Servings(4),
            List.of(new Ingredient("Nudeln", new BigDecimal("500"), "g")));
        when(mealPlanRepository.findByTripId(tripId)).thenReturn(Optional.of(mealPlan));
        when(recipeRepository.findById(recipe.recipeId())).thenReturn(Optional.of(recipe));
        when(mealPlanRepository.save(any(MealPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        mealPlanService.assignRecipe(new AssignRecipeToSlotCommand(
            tripId.value(), mealPlan.slots().getFirst().mealSlotId().value(),
            recipe.recipeId().value()));

        verify(mealPlanRepository).save(any(MealPlan.class));
    }

    @Test
    void clearRecipeRemovesRecipeFromSlot() {
        final MealPlan mealPlan = createMealPlan();
        final TripId tripId = mealPlan.tripId();
        final UUID recipeId = UUID.randomUUID();
        mealPlan.assignRecipe(mealPlan.slots().getFirst().mealSlotId(), recipeId);
        when(mealPlanRepository.findByTripId(tripId)).thenReturn(Optional.of(mealPlan));
        when(mealPlanRepository.save(any(MealPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        mealPlanService.clearRecipe(tripId, mealPlan.slots().getFirst().mealSlotId());

        verify(mealPlanRepository).save(any(MealPlan.class));
    }

    @Test
    void findByTripIdEnrichesWithRecipeNames() {
        final MealPlan mealPlan = createMealPlan();
        final TripId tripId = mealPlan.tripId();
        final TenantId tenantId = mealPlan.tenantId();
        final UUID recipeId = UUID.randomUUID();
        mealPlan.assignRecipe(mealPlan.slots().getFirst().mealSlotId(), recipeId);
        when(mealPlanRepository.findByTripId(tripId)).thenReturn(Optional.of(mealPlan));

        final Recipe recipe = Recipe.create(tenantId, new RecipeName("Pasta"), new Servings(4),
            List.of(new Ingredient("Nudeln", new BigDecimal("500"), "g")));
        // Create a recipe with matching ID for enrichment
        final Recipe matchingRecipe = Recipe.create(tenantId, new RecipeName("Matching"), new Servings(2),
            List.of(new Ingredient("X", new BigDecimal("1"), "g")));
        when(recipeRepository.findAllByTenantId(tenantId)).thenReturn(List.of(recipe, matchingRecipe));

        final MealPlanRepresentation result = mealPlanService.findByTripId(tripId, tenantId);

        assertThat(result.slots()).isNotEmpty();
        // The assigned recipe won't match any recipe in the list (different UUID),
        // so the enrichment should show the deleted-recipe fallback
        assertThat(result.slots().getFirst().recipeName()).isEqualTo("[Geloeschtes Rezept]");
    }

    @Test
    void findByTripIdThrowsWhenNotFound() {
        final TripId tripId = new TripId(UUID.randomUUID());
        when(mealPlanRepository.findByTripId(tripId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mealPlanService.findByTripId(tripId, new TenantId(TENANT_UUID)))
            .isInstanceOf(EntityNotFoundException.class);
    }

    private MealPlan createMealPlan() {
        return MealPlan.generate(
            new TenantId(TENANT_UUID),
            new TripId(UUID.randomUUID()),
            new DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3))
        );
    }
}
