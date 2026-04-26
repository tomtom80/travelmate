package de.evia.travelmate.trips.adapters.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationJpaRepository extends JpaRepository<InvitationJpaEntity, UUID> {

    List<InvitationJpaEntity> findByTripId(UUID tripId);

    List<InvitationJpaEntity> findByInviteeIdAndStatus(UUID inviteeId, String status);

    List<InvitationJpaEntity> findByInviteeEmailAndStatus(String inviteeEmail, String status);

    boolean existsByTripIdAndTargetPartyTenantIdAndStatusIn(UUID tripId, UUID targetPartyTenantId, List<String> statuses);

    boolean existsByTripIdAndInviteeId(UUID tripId, UUID inviteeId);

    boolean existsByTripIdAndInviteeEmail(UUID tripId, String inviteeEmail);

    void deleteByTripId(UUID tripId);
}
