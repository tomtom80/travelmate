package de.evia.travelmate.iam.adapters.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.evia.travelmate.iam.adapters.messaging.ExternalInviteFollowupTracker;

@Component
@Profile("!test")
public class ExternalInviteFollowupTrackerAdapter implements ExternalInviteFollowupTracker {

    private final ExternalInviteFollowupJpaRepository jpaRepository;

    public ExternalInviteFollowupTrackerAdapter(final ExternalInviteFollowupJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean alreadyDispatched(final String email, final UUID tripId) {
        return jpaRepository.existsByEmailAndTripId(email, tripId);
    }

    @Override
    public void markDispatched(final String email, final UUID tripId, final String actionType) {
        jpaRepository.save(new ExternalInviteFollowupJpaEntity(email, tripId, actionType, Instant.now()));
    }
}
