package de.evia.travelmate.trips.adapters.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccommodationPollJpaRepository extends JpaRepository<AccommodationPollJpaEntity, UUID> {

    Optional<AccommodationPollJpaEntity> findByTenantIdAndAccommodationPollId(UUID tenantId, UUID accommodationPollId);

    List<AccommodationPollJpaEntity> findByTenantIdAndTripId(UUID tenantId, UUID tripId);
}
