package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record GrantTripOrganizerCommand(
    UUID tripId,
    UUID participantId,
    UUID actorId
) {
}
