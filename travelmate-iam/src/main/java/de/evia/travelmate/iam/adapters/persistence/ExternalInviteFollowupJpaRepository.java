package de.evia.travelmate.iam.adapters.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalInviteFollowupJpaRepository
        extends JpaRepository<ExternalInviteFollowupJpaEntity, ExternalInviteFollowupJpaEntity.PK> {

    boolean existsByEmailAndTripId(String email, UUID tripId);
}
