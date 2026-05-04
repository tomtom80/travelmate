package de.evia.travelmate.iam.adapters.messaging;

import java.util.UUID;

public interface ExternalInviteFollowupTracker {

    boolean alreadyDispatched(String email, UUID tripId);

    void markDispatched(String email, UUID tripId, String actionType);
}
