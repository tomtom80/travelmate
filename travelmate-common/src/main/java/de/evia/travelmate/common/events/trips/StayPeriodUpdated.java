package de.evia.travelmate.common.events.trips;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.DomainEvent;

public record StayPeriodUpdated(
    UUID tenantId,
    UUID tripId,
    UUID participantId,
    LocalDate arrivalDate,
    LocalDate departureDate,
    LocalDate occurredOn
) implements DomainEvent {
}
