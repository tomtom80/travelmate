package de.evia.travelmate.common.events.iam;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.DomainEvent;

public record DependentAddedToTenant(
    UUID tenantId,
    UUID dependentId,
    UUID guardianAccountId,
    String firstName,
    String lastName,
    LocalDate occurredOn
) implements DomainEvent {
}
