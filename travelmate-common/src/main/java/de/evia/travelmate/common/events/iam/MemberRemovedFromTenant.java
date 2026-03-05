package de.evia.travelmate.common.events.iam;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.DomainEvent;

public record MemberRemovedFromTenant(
    UUID tenantId,
    UUID accountId,
    String email,
    LocalDate occurredOn
) implements DomainEvent {
}
