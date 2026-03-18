package de.evia.travelmate.trips.adapters.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccommodationJpaRepository extends JpaRepository<AccommodationJpaEntity, UUID> {

    Optional<AccommodationJpaEntity> findByTripId(UUID tripId);

    boolean existsByTripId(UUID tripId);

    void deleteByTripId(UUID tripId);
}
