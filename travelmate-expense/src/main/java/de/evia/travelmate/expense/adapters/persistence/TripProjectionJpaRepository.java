package de.evia.travelmate.expense.adapters.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TripProjectionJpaRepository extends JpaRepository<TripProjectionJpaEntity, UUID> {

    boolean existsByTripId(UUID tripId);
}
