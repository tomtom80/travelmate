package de.evia.travelmate.common.events.iam;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.DomainEvent;

public record AccountRegistered(
    UUID tenantId,
    UUID accountId,
    String username,
    String firstName,
    String lastName,
    String email,
    LocalDate dateOfBirth,
    LocalDate occurredOn
) implements DomainEvent {
}
