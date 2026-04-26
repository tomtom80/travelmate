package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record RevokeTripOrganizerCommand(
    UUID tripId,
    UUID accountId,
    UUID actorId
) {
}
