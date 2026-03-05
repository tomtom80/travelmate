package de.evia.travelmate.trips.domain.invitation;

import java.util.List;
import java.util.Optional;

import de.evia.travelmate.trips.domain.trip.TripId;

public interface InvitationRepository {

    Invitation save(Invitation invitation);

    Optional<Invitation> findById(InvitationId invitationId);

    List<Invitation> findByTripId(TripId tripId);

    boolean existsByTripIdAndInviteeId(TripId tripId, java.util.UUID inviteeId);
}
