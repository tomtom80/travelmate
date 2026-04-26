package de.evia.travelmate.common.events.trips;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.DomainEvent;

public record OrganizerRoleRevoked(
    UUID tenantId,
    UUID tripId,
    UUID accountId,
    LocalDate occurredOn
) implements DomainEvent {
}
