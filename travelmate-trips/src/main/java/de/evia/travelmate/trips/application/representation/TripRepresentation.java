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
    List<UUID> participantIds,
    List<ParticipantDetail> participantDetails
) {

    public record ParticipantDetail(UUID participantId, LocalDate arrivalDate, LocalDate departureDate) {
    }

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
            trip.participants().stream().map(p -> p.participantId()).toList(),
            trip.participants().stream().map(p -> new ParticipantDetail(
                p.participantId(),
                p.stayPeriod() != null ? p.stayPeriod().arrivalDate() : null,
                p.stayPeriod() != null ? p.stayPeriod().departureDate() : null
            )).toList()
        );
    }

    public TripRepresentation(final UUID tripId, final UUID tenantId, final String name,
                              final String description, final LocalDate startDate, final LocalDate endDate,
                              final String status, final UUID organizerId, final List<UUID> participantIds) {
        this(tripId, tenantId, name, description, startDate, endDate, status, organizerId,
            participantIds, participantIds.stream().map(id -> new ParticipantDetail(id, null, null)).toList());
    }
}
