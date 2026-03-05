package de.evia.travelmate.trips.adapters.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TripJpaRepository extends JpaRepository<TripJpaEntity, UUID> {

    List<TripJpaEntity> findAllByTenantId(UUID tenantId);
}
