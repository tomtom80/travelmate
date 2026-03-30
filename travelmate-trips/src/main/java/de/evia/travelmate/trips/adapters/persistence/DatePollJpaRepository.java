package de.evia.travelmate.trips.adapters.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DatePollJpaRepository extends JpaRepository<DatePollJpaEntity, UUID> {

    Optional<DatePollJpaEntity> findByTenantIdAndDatePollId(UUID tenantId, UUID datePollId);

    List<DatePollJpaEntity> findByTenantIdAndTripId(UUID tenantId, UUID tripId);
}
