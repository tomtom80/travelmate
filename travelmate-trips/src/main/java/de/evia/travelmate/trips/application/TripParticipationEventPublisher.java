package de.evia.travelmate.trips.application;

import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;

public interface TripParticipationEventPublisher {

    void publishParticipantJoinedAfterCommit(ParticipantJoinedTrip event);
}
