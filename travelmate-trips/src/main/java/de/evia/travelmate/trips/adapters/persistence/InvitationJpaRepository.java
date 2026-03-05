package de.evia.travelmate.trips.adapters.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationJpaRepository extends JpaRepository<InvitationJpaEntity, UUID> {

    List<InvitationJpaEntity> findByTripId(UUID tripId);

    boolean existsByTripIdAndInviteeId(UUID tripId, UUID inviteeId);
}
