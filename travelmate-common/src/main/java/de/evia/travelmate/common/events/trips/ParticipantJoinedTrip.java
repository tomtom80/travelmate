package de.evia.travelmate.common.events.trips;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.DomainEvent;

public record ParticipantJoinedTrip(
    UUID tenantId,
    UUID tripId,
    UUID participantId,
    String username,
    LocalDate occurredOn
) implements DomainEvent {
}
