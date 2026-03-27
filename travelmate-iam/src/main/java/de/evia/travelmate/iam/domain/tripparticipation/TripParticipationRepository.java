package de.evia.travelmate.iam.domain.tripparticipation;

import java.util.UUID;

public interface TripParticipationRepository {

    void add(UUID participantId, UUID tripId);

    void remove(UUID participantId, UUID tripId);

    boolean existsByParticipantId(UUID participantId);
}
