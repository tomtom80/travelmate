package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record CreateTripCommand(
    UUID tenantId,
    String name,
    String description,
    UUID organizerId
) {
}
