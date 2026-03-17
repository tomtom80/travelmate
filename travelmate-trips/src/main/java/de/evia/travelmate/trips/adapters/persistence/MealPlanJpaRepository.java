package de.evia.travelmate.trips.adapters.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MealPlanJpaRepository extends JpaRepository<MealPlanJpaEntity, UUID> {

    Optional<MealPlanJpaEntity> findByTripId(UUID tripId);

    boolean existsByTripId(UUID tripId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM MealSlotJpaEntity s WHERE s.recipeId = :recipeId")
    boolean existsSlotWithRecipeId(@Param("recipeId") UUID recipeId);
}
