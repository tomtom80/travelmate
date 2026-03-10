package de.evia.travelmate.common.events.trips;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.DomainEvent;

public record InvitationCreated(
    UUID tenantId,
    UUID tripId,
    UUID invitationId,
    String inviteeEmail,
    String inviteeFirstName,
    String tripName,
    LocalDate tripStartDate,
    LocalDate tripEndDate,
    String inviterFirstName,
    String inviterLastName,
    LocalDate occurredOn
) implements DomainEvent {
}
