package de.evia.travelmate.trips.application.representation;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import de.evia.travelmate.trips.domain.trip.Trip;

public record TripRepresentation(
    UUID tripId,
    UUID tenantId,
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    String status,
    UUID organizerId,
    List<UUID> participantIds
) {

    public TripRepresentation(final Trip trip) {
        this(
            trip.tripId().value(),
            trip.tenantId().value(),
            trip.name().value(),
            trip.description(),
            trip.dateRange().startDate(),
            trip.dateRange().endDate(),
            trip.status().name(),
            trip.organizerId(),
            trip.participants().stream().map(p -> p.participantId()).toList()
        );
    }
}
