package de.evia.travelmate.trips.adapters.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.mealplan.MealPlan;
import de.evia.travelmate.trips.domain.mealplan.MealPlanId;
import de.evia.travelmate.trips.domain.mealplan.MealPlanRepository;
import de.evia.travelmate.trips.domain.mealplan.MealSlot;
import de.evia.travelmate.trips.domain.mealplan.MealSlotId;
import de.evia.travelmate.trips.domain.mealplan.MealSlotStatus;
import de.evia.travelmate.trips.domain.mealplan.MealType;
import de.evia.travelmate.trips.domain.trip.TripId;

@Repository
public class MealPlanRepositoryAdapter implements MealPlanRepository {

    private final MealPlanJpaRepository jpaRepository;

    public MealPlanRepositoryAdapter(final MealPlanJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public MealPlan save(final MealPlan mealPlan) {
        final MealPlanJpaEntity entity = jpaRepository.findById(mealPlan.mealPlanId().value())
            .orElseGet(() -> new MealPlanJpaEntity(
                mealPlan.mealPlanId().value(),
                mealPlan.tenantId().value(),
                mealPlan.tripId().value()
            ));
        syncSlots(entity, mealPlan);
        jpaRepository.save(entity);
        return mealPlan;
    }

    @Override
    public Optional<MealPlan> findByTripId(final TripId tripId) {
        return jpaRepository.findByTripId(tripId.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByTripId(final TripId tripId) {
        return jpaRepository.existsByTripId(tripId.value());
    }

    @Override
    public boolean existsSlotWithRecipe(final UUID recipeId) {
        return jpaRepository.existsSlotWithRecipeId(recipeId);
    }

    private void syncSlots(final MealPlanJpaEntity entity, final MealPlan mealPlan) {
        for (final MealSlot slot : mealPlan.slots()) {
            final Optional<MealSlotJpaEntity> existing = entity.getSlots().stream()
                .filter(s -> s.getMealSlotId().equals(slot.mealSlotId().value()))
                .findFirst();
            if (existing.isPresent()) {
                existing.get().setStatus(slot.status().name());
                existing.get().setRecipeId(slot.recipeId());
            } else {
                entity.getSlots().add(new MealSlotJpaEntity(
                    slot.mealSlotId().value(), entity,
                    slot.date(), slot.mealType().name(),
                    slot.status().name(), slot.recipeId()
                ));
            }
        }
    }

    private MealPlan toDomain(final MealPlanJpaEntity entity) {
        final var slots = entity.getSlots().stream()
            .map(s -> new MealSlot(
                new MealSlotId(s.getMealSlotId()),
                s.getDate(),
                MealType.valueOf(s.getMealType()),
                MealSlotStatus.valueOf(s.getStatus()),
                s.getRecipeId()
            ))
            .toList();
        return new MealPlan(
            new MealPlanId(entity.getMealPlanId()),
            new TenantId(entity.getTenantId()),
            new TripId(entity.getTripId()),
            slots
        );
    }
}
