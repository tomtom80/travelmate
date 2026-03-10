package de.evia.travelmate.trips.domain.invitation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.evia.travelmate.trips.domain.trip.TripId;

public interface InvitationRepository {

    Invitation save(Invitation invitation);

    Optional<Invitation> findById(InvitationId invitationId);

    List<Invitation> findByTripId(TripId tripId);

    List<Invitation> findByInviteeIdAndStatus(UUID inviteeId, InvitationStatus status);

    List<Invitation> findByInviteeEmailAndStatus(String inviteeEmail, InvitationStatus status);

    boolean existsByTripIdAndInviteeId(TripId tripId, UUID inviteeId);

    boolean existsByTripIdAndInviteeEmail(TripId tripId, String inviteeEmail);
}
