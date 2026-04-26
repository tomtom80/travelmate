package de.evia.travelmate.trips.domain.mealplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.domain.BusinessRuleViolationException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.TripId;

class MealPlanTest {

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final TripId TRIP_ID = new TripId(UUID.randomUUID());
    private static final DateRange THREE_DAY_RANGE = new DateRange(
        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3)
    );

    @Test
    void generateCreatesThreeSlotsPerDay() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);

        assertThat(mealPlan.mealPlanId()).isNotNull();
        assertThat(mealPlan.tenantId()).isEqualTo(TENANT_ID);
        assertThat(mealPlan.tripId()).isEqualTo(TRIP_ID);
        assertThat(mealPlan.slots()).hasSize(9);
    }

    @Test
    void generateCreatesSlotsForEachMealType() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);

        final long breakfasts = mealPlan.slots().stream()
            .filter(s -> s.mealType() == MealType.BREAKFAST).count();
        final long lunches = mealPlan.slots().stream()
            .filter(s -> s.mealType() == MealType.LUNCH).count();
        final long dinners = mealPlan.slots().stream()
            .filter(s -> s.mealType() == MealType.DINNER).count();

        assertThat(breakfasts).isEqualTo(3);
        assertThat(lunches).isEqualTo(3);
        assertThat(dinners).isEqualTo(3);
    }

    @Test
    void allSlotsStartAsPlanned() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);

        assertThat(mealPlan.slots())
            .allMatch(s -> s.status() == MealSlotStatus.PLANNED);
    }

    @Test
    void singleDayCreatesThreeSlots() {
        final DateRange oneDay = new DateRange(
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1));

        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, oneDay);

        assertThat(mealPlan.slots()).hasSize(3);
    }

    @Test
    void markSlotChangesStatus() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);
        final MealSlotId slotId = mealPlan.slots().getFirst().mealSlotId();

        mealPlan.markSlot(slotId, MealSlotStatus.SKIP);

        assertThat(mealPlan.slots().getFirst().status()).isEqualTo(MealSlotStatus.SKIP);
    }

    @Test
    void markSlotSkipClearsRecipe() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);
        final MealSlotId slotId = mealPlan.slots().getFirst().mealSlotId();
        mealPlan.assignRecipe(slotId, UUID.randomUUID());

        mealPlan.markSlot(slotId, MealSlotStatus.SKIP);

        assertThat(mealPlan.slots().getFirst().recipeId()).isNull();
    }

    @Test
    void markSlotEatingOutClearsRecipe() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);
        final MealSlotId slotId = mealPlan.slots().getFirst().mealSlotId();
        mealPlan.assignRecipe(slotId, UUID.randomUUID());

        mealPlan.markSlot(slotId, MealSlotStatus.EATING_OUT);

        assertThat(mealPlan.slots().getFirst().recipeId()).isNull();
        assertThat(mealPlan.slots().getFirst().status()).isEqualTo(MealSlotStatus.EATING_OUT);
    }

    @Test
    void assignRecipeSetsRecipeAndPlannedStatus() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);
        final MealSlotId slotId = mealPlan.slots().getFirst().mealSlotId();
        final UUID recipeId = UUID.randomUUID();

        mealPlan.assignRecipe(slotId, recipeId);

        assertThat(mealPlan.slots().getFirst().recipeId()).isEqualTo(recipeId);
        assertThat(mealPlan.slots().getFirst().status()).isEqualTo(MealSlotStatus.PLANNED);
    }

    @Test
    void assignRecipeToSkippedSlotResetsToPlanned() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);
        final MealSlotId slotId = mealPlan.slots().getFirst().mealSlotId();
        mealPlan.markSlot(slotId, MealSlotStatus.SKIP);

        mealPlan.assignRecipe(slotId, UUID.randomUUID());

        assertThat(mealPlan.slots().getFirst().status()).isEqualTo(MealSlotStatus.PLANNED);
    }

    @Test
    void clearRecipeRemovesRecipeId() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);
        final MealSlotId slotId = mealPlan.slots().getFirst().mealSlotId();
        mealPlan.assignRecipe(slotId, UUID.randomUUID());

        mealPlan.clearRecipe(slotId);

        assertThat(mealPlan.slots().getFirst().recipeId()).isNull();
    }

    @Test
    void markSlotRejectsUnknownSlot() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);

        assertThatThrownBy(() -> mealPlan.markSlot(new MealSlotId(UUID.randomUUID()), MealSlotStatus.SKIP))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void assignRecipeRejectsUnknownSlot() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);

        assertThatThrownBy(() -> mealPlan.assignRecipe(new MealSlotId(UUID.randomUUID()), UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void slotsListIsUnmodifiable() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);

        assertThatThrownBy(() -> mealPlan.slots().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void assignKitchenDutyAddsParticipantsToPlannedSlot() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);
        final MealSlotId slotId = mealPlan.slots().getFirst().mealSlotId();
        final UUID alice = UUID.randomUUID();
        final UUID bob = UUID.randomUUID();

        mealPlan.assignKitchenDuty(slotId, List.of(alice, bob));

        assertThat(mealPlan.slots().getFirst().kitchenDutyParticipantIds())
            .containsExactly(alice, bob);
    }

    @Test
    void assignKitchenDutyRejectsSkippedSlot() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);
        final MealSlotId slotId = mealPlan.slots().getFirst().mealSlotId();
        mealPlan.markSlot(slotId, MealSlotStatus.SKIP);

        assertThatThrownBy(() -> mealPlan.assignKitchenDuty(slotId, List.of(UUID.randomUUID())))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("kitchenDutyRequiresPlanned");
    }

    @Test
    void assignKitchenDutyRejectsEatingOutSlot() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);
        final MealSlotId slotId = mealPlan.slots().getFirst().mealSlotId();
        mealPlan.markSlot(slotId, MealSlotStatus.EATING_OUT);

        assertThatThrownBy(() -> mealPlan.assignKitchenDuty(slotId, List.of(UUID.randomUUID())))
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void assignKitchenDutyWithEmptyListClearsExistingDuty() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);
        final MealSlotId slotId = mealPlan.slots().getFirst().mealSlotId();
        mealPlan.assignKitchenDuty(slotId, List.of(UUID.randomUUID()));

        mealPlan.assignKitchenDuty(slotId, List.of());

        assertThat(mealPlan.slots().getFirst().kitchenDutyParticipantIds()).isEmpty();
    }

    @Test
    void markSlotSkipClearsKitchenDuty() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);
        final MealSlotId slotId = mealPlan.slots().getFirst().mealSlotId();
        mealPlan.assignKitchenDuty(slotId, List.of(UUID.randomUUID(), UUID.randomUUID()));

        mealPlan.markSlot(slotId, MealSlotStatus.SKIP);

        assertThat(mealPlan.slots().getFirst().kitchenDutyParticipantIds()).isEmpty();
    }

    @Test
    void newSlotsHaveEmptyKitchenDutyByDefault() {
        final MealPlan mealPlan = MealPlan.generate(TENANT_ID, TRIP_ID, THREE_DAY_RANGE);

        assertThat(mealPlan.slots())
            .allMatch(s -> s.kitchenDutyParticipantIds().isEmpty());
    }
}
