package de.evia.travelmate.common.events.iam;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.DomainEvent;

public record RoleAssignedToUser(
    UUID tenantId,
    String username,
    String roleName,
    String firstName,
    String familyName,
    String email,
    LocalDate occurredOn
) implements DomainEvent {
}
