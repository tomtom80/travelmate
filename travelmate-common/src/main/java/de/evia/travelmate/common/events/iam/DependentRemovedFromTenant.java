package de.evia.travelmate.common.events.iam;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.DomainEvent;

public record DependentRemovedFromTenant(
    UUID tenantId,
    UUID dependentId,
    LocalDate occurredOn
) implements DomainEvent {
}
