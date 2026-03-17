package de.evia.travelmate.trips.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.command.AssignRecipeToSlotCommand;
import de.evia.travelmate.trips.application.command.GenerateMealPlanCommand;
import de.evia.travelmate.trips.application.command.UpdateMealSlotCommand;
import de.evia.travelmate.trips.application.representation.MealPlanRepresentation;
import de.evia.travelmate.trips.application.representation.MealSlotRepresentation;
import de.evia.travelmate.trips.application.representation.RecipeRepresentation;
import de.evia.travelmate.trips.domain.mealplan.MealPlan;
import de.evia.travelmate.trips.domain.mealplan.MealPlanRepository;
import de.evia.travelmate.trips.domain.mealplan.MealSlotId;
import de.evia.travelmate.trips.domain.mealplan.MealSlotStatus;
import de.evia.travelmate.trips.domain.recipe.RecipeId;
import de.evia.travelmate.trips.domain.recipe.RecipeRepository;
import de.evia.travelmate.trips.domain.trip.Trip;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.trip.TripRepository;

@Service
@Transactional
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final TripRepository tripRepository;
    private final RecipeRepository recipeRepository;

    public MealPlanService(final MealPlanRepository mealPlanRepository,
                           final TripRepository tripRepository,
                           final RecipeRepository recipeRepository) {
        this.mealPlanRepository = mealPlanRepository;
        this.tripRepository = tripRepository;
        this.recipeRepository = recipeRepository;
    }

    public MealPlanRepresentation generateMealPlan(final GenerateMealPlanCommand command) {
        final TripId tripId = new TripId(command.tripId());
        if (mealPlanRepository.existsByTripId(tripId)) {
            throw new IllegalStateException("A meal plan already exists for trip " + command.tripId());
        }
        final Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new EntityNotFoundException("Trip", command.tripId().toString()));
        final MealPlan mealPlan = MealPlan.generate(
            new TenantId(command.tenantId()),
            tripId,
            trip.dateRange()
        );
        mealPlanRepository.save(mealPlan);
        return new MealPlanRepresentation(mealPlan);
    }

    public void updateSlotStatus(final UpdateMealSlotCommand command) {
        final MealPlan mealPlan = findMealPlan(new TripId(command.tripId()));
        final MealSlotStatus status = MealSlotStatus.valueOf(command.status());
        mealPlan.markSlot(new MealSlotId(command.slotId()), status);
        mealPlanRepository.save(mealPlan);
    }

    public void assignRecipe(final AssignRecipeToSlotCommand command) {
        final MealPlan mealPlan = findMealPlan(new TripId(command.tripId()));
        recipeRepository.findById(new RecipeId(command.recipeId()))
            .orElseThrow(() -> new EntityNotFoundException("Recipe", command.recipeId().toString()));
        mealPlan.assignRecipe(new MealSlotId(command.slotId()), command.recipeId());
        mealPlanRepository.save(mealPlan);
    }

    public void clearRecipe(final TripId tripId, final MealSlotId slotId) {
        final MealPlan mealPlan = findMealPlan(tripId);
        mealPlan.clearRecipe(slotId);
        mealPlanRepository.save(mealPlan);
    }

    @Transactional(readOnly = true)
    public MealPlanRepresentation findByTripId(final TripId tripId, final TenantId tenantId) {
        final MealPlan mealPlan = findMealPlan(tripId);
        final MealPlanRepresentation repr = new MealPlanRepresentation(mealPlan);
        return enrichWithRecipeNames(repr, tenantId);
    }

    @Transactional(readOnly = true)
    public boolean existsByTripId(final TripId tripId) {
        return mealPlanRepository.existsByTripId(tripId);
    }

    @Transactional(readOnly = true)
    public boolean existsSlotWithRecipe(final UUID recipeId) {
        return mealPlanRepository.existsSlotWithRecipe(recipeId);
    }

    private MealPlan findMealPlan(final TripId tripId) {
        return mealPlanRepository.findByTripId(tripId)
            .orElseThrow(() -> new EntityNotFoundException("MealPlan", tripId.value().toString()));
    }

    private MealPlanRepresentation enrichWithRecipeNames(final MealPlanRepresentation repr,
                                                         final TenantId tenantId) {
        final Map<UUID, String> recipeNames = recipeRepository.findAllByTenantId(tenantId).stream()
            .collect(Collectors.toMap(
                r -> r.recipeId().value(),
                r -> r.name().value(),
                (a, b) -> a
            ));
        final List<MealSlotRepresentation> enriched = repr.slots().stream()
            .map(slot -> {
                if (slot.recipeId() != null) {
                    final String recipeName = recipeNames.get(slot.recipeId());
                    return slot.withRecipeName(recipeName != null ? recipeName : "[Geloeschtes Rezept]");
                }
                return slot;
            })
            .toList();
        return new MealPlanRepresentation(repr.mealPlanId(), repr.tenantId(), repr.tripId(), enriched);
    }
}
