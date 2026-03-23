package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record AddParticipantToTripCommand(
    UUID tripId,
    UUID participantId,
    UUID actorId,
    UUID actorPartyTenantId
) {
}
