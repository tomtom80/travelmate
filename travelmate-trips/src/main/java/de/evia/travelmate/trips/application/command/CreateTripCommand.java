package de.evia.travelmate.trips.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTripCommand(
    UUID tenantId,
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    UUID organizerId
) {
}
