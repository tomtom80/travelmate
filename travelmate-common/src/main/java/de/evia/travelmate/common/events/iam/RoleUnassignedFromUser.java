package de.evia.travelmate.common.events.iam;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.DomainEvent;

public record RoleUnassignedFromUser(
    UUID tenantId,
    String username,
    String roleName,
    LocalDate occurredOn
) implements DomainEvent {
}
