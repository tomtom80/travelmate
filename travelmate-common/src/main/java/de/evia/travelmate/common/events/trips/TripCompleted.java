package de.evia.travelmate.common.events.trips;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.DomainEvent;

public record TripCompleted(
    UUID tenantId,
    UUID tripId,
    LocalDate occurredOn
) implements DomainEvent {
}
