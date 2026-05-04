package de.evia.travelmate.common.events.trips;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.DomainEvent;

public record ExternalUserInvitedToTrip(
    UUID tenantId,
    UUID tripId,
    UUID invitationId,
    String email,
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    String tripName,
    String inviterFirstName,
    String inviterLastName,
    LocalDate occurredOn
) implements DomainEvent {
}
