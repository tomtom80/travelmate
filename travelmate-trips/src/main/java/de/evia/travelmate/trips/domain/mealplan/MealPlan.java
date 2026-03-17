package de.evia.travelmate.trips.domain.mealplan;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import de.evia.travelmate.common.domain.AggregateRoot;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.TripId;

public class MealPlan extends AggregateRoot {

    private final MealPlanId mealPlanId;
    private final TenantId tenantId;
    private final TripId tripId;
    private final List<MealSlot> slots;

    public MealPlan(final MealPlanId mealPlanId,
                    final TenantId tenantId,
                    final TripId tripId,
                    final List<MealSlot> slots) {
        argumentIsNotNull(mealPlanId, "mealPlanId");
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(tripId, "tripId");
        argumentIsNotNull(slots, "slots");
        this.mealPlanId = mealPlanId;
        this.tenantId = tenantId;
        this.tripId = tripId;
        this.slots = new ArrayList<>(slots);
    }

    public static MealPlan generate(final TenantId tenantId,
                                    final TripId tripId,
                                    final DateRange dateRange) {
        argumentIsNotNull(dateRange, "dateRange");
        final List<MealSlot> slots = new ArrayList<>();
        LocalDate current = dateRange.startDate();
        while (!current.isAfter(dateRange.endDate())) {
            for (final MealType mealType : MealType.values()) {
                slots.add(new MealSlot(current, mealType));
            }
            current = current.plusDays(1);
        }
        return new MealPlan(
            new MealPlanId(UUID.randomUUID()),
            tenantId,
            tripId,
            slots
        );
    }

    public void markSlot(final MealSlotId slotId, final MealSlotStatus status) {
        argumentIsNotNull(slotId, "slotId");
        argumentIsNotNull(status, "status");
        findSlot(slotId).markStatus(status);
    }

    public void assignRecipe(final MealSlotId slotId, final UUID recipeId) {
        argumentIsNotNull(slotId, "slotId");
        argumentIsNotNull(recipeId, "recipeId");
        findSlot(slotId).assignRecipe(recipeId);
    }

    public void clearRecipe(final MealSlotId slotId) {
        argumentIsNotNull(slotId, "slotId");
        findSlot(slotId).clearRecipe();
    }

    private MealSlot findSlot(final MealSlotId slotId) {
        return slots.stream()
            .filter(s -> s.mealSlotId().equals(slotId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "MealSlot " + slotId.value() + " not found in this meal plan."));
    }

    public MealPlanId mealPlanId() {
        return mealPlanId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public TripId tripId() {
        return tripId;
    }

    public List<MealSlot> slots() {
        return Collections.unmodifiableList(slots);
    }
}
