package de.evia.travelmate.trips.adapters.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingListJpaRepository extends JpaRepository<ShoppingListJpaEntity, UUID> {

    Optional<ShoppingListJpaEntity> findByTripIdAndTenantId(UUID tripId, UUID tenantId);
}
