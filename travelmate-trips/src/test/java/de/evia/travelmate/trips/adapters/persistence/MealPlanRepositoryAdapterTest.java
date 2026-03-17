package de.evia.travelmate.trips.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.mealplan.MealPlan;
import de.evia.travelmate.trips.domain.mealplan.MealPlanRepository;
import de.evia.travelmate.trips.domain.mealplan.MealSlotStatus;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.TripId;

@SpringBootTest
@ActiveProfiles("test")
class MealPlanRepositoryAdapterTest {

    @Autowired
    private MealPlanRepository repository;

    @Test
    void savesAndFindsMealPlanByTripId() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final MealPlan mealPlan = MealPlan.generate(
            new TenantId(UUID.randomUUID()), tripId,
            new DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2))
        );

        repository.save(mealPlan);

        final Optional<MealPlan> found = repository.findByTripId(tripId);
        assertThat(found).isPresent();
        assertThat(found.get().slots()).hasSize(6);
    }

    @Test
    void existsByTripIdReturnsTrueWhenExists() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final MealPlan mealPlan = MealPlan.generate(
            new TenantId(UUID.randomUUID()), tripId,
            new DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1))
        );
        repository.save(mealPlan);

        assertThat(repository.existsByTripId(tripId)).isTrue();
    }

    @Test
    void existsByTripIdReturnsFalseWhenNotExists() {
        assertThat(repository.existsByTripId(new TripId(UUID.randomUUID()))).isFalse();
    }

    @Test
    void updatesSlotStatusAndRecipe() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final MealPlan mealPlan = MealPlan.generate(
            new TenantId(UUID.randomUUID()), tripId,
            new DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1))
        );
        repository.save(mealPlan);

        final MealPlan loaded = repository.findByTripId(tripId).orElseThrow();
        final UUID recipeId = UUID.randomUUID();
        loaded.assignRecipe(loaded.slots().getFirst().mealSlotId(), recipeId);
        loaded.markSlot(loaded.slots().get(1).mealSlotId(), MealSlotStatus.SKIP);
        repository.save(loaded);

        final MealPlan reloaded = repository.findByTripId(tripId).orElseThrow();
        assertThat(reloaded.slots().stream()
            .filter(s -> s.recipeId() != null && s.recipeId().equals(recipeId))
            .count()).isEqualTo(1);
        assertThat(reloaded.slots().stream()
            .filter(s -> s.status() == MealSlotStatus.SKIP)
            .count()).isEqualTo(1);
    }

    @Test
    void existsSlotWithRecipeReturnsTrueWhenAssigned() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final MealPlan mealPlan = MealPlan.generate(
            new TenantId(UUID.randomUUID()), tripId,
            new DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1))
        );
        final UUID recipeId = UUID.randomUUID();
        mealPlan.assignRecipe(mealPlan.slots().getFirst().mealSlotId(), recipeId);
        repository.save(mealPlan);

        assertThat(repository.existsSlotWithRecipe(recipeId)).isTrue();
    }

    @Test
    void existsSlotWithRecipeReturnsFalseWhenNotAssigned() {
        assertThat(repository.existsSlotWithRecipe(UUID.randomUUID())).isFalse();
    }
}
