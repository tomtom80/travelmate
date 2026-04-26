package de.evia.travelmate.trips.adapters.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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

    @Override
    public void deleteByTripId(final TripId tripId) {
        jpaRepository.deleteByTripId(tripId.value());
    }

    private void syncSlots(final MealPlanJpaEntity entity, final MealPlan mealPlan) {
        for (final MealSlot slot : mealPlan.slots()) {
            final Optional<MealSlotJpaEntity> existing = entity.getSlots().stream()
                .filter(s -> s.getMealSlotId().equals(slot.mealSlotId().value()))
                .findFirst();
            if (existing.isPresent()) {
                existing.get().setStatus(slot.status().name());
                existing.get().setRecipeId(slot.recipeId());
                existing.get().setKitchenDutyParticipantIdsJson(serializeKitchenDuty(slot.kitchenDutyParticipantIds()));
            } else {
                final MealSlotJpaEntity newSlot = new MealSlotJpaEntity(
                    slot.mealSlotId().value(), entity,
                    slot.date(), slot.mealType().name(),
                    slot.status().name(), slot.recipeId()
                );
                newSlot.setKitchenDutyParticipantIdsJson(serializeKitchenDuty(slot.kitchenDutyParticipantIds()));
                entity.getSlots().add(newSlot);
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
                s.getRecipeId(),
                deserializeKitchenDuty(s.getKitchenDutyParticipantIdsJson())
            ))
            .toList();
        return new MealPlan(
            new MealPlanId(entity.getMealPlanId()),
            new TenantId(entity.getTenantId()),
            new TripId(entity.getTripId()),
            slots
        );
    }

    private String serializeKitchenDuty(final List<UUID> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(participantIds);
        } catch (final com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize kitchen duty participants", e);
        }
    }

    private List<UUID> deserializeKitchenDuty(final String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<UUID>>() {});
        } catch (final com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize kitchen duty participants", e);
        }
    }
}
