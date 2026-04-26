package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record EditTripCommand(
    UUID tripId,
    String name,
    String description
) {
}
