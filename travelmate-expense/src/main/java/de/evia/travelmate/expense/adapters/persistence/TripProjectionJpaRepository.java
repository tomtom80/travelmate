package de.evia.travelmate.expense.adapters.persistence;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TripProjectionJpaRepository extends JpaRepository<TripProjectionJpaEntity, UUID> {

    boolean existsByTripId(UUID tripId);

    List<TripProjectionJpaEntity> findDistinctByParticipantsPartyTenantId(UUID partyTenantId);
}
