package de.evia.travelmate.common.events.trips;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.DomainEvent;

public record TripCreated(
    UUID tenantId,
    UUID tripId,
    String tripName,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate occurredOn
) implements DomainEvent {
}
