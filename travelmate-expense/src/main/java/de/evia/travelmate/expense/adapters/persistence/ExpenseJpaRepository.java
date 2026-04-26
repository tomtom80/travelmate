package de.evia.travelmate.expense.adapters.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseJpaRepository extends JpaRepository<ExpenseJpaEntity, UUID> {

    Optional<ExpenseJpaEntity> findByTenantIdAndTripId(UUID tenantId, UUID tripId);

    boolean existsByTripId(UUID tripId);

    void deleteByTripId(UUID tripId);
}
