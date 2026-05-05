package de.evia.travelmate.webcommons.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
    UUID auditId,
    Instant occurredOn,
    UUID tenantId,
    UUID actorAccountId,
    String actorRole,
    String action,
    String resourceType,
    UUID resourceId,
    AuditOutcome outcome,
    String reason
) {
    public static AuditEvent success(final UUID tenantId,
                                     final UUID actorAccountId,
                                     final String actorRole,
                                     final String action,
                                     final String resourceType,
                                     final UUID resourceId) {
        return new AuditEvent(
            UUID.randomUUID(),
            Instant.now(),
            tenantId,
            actorAccountId,
            actorRole,
            action,
            resourceType,
            resourceId,
            AuditOutcome.SUCCESS,
            null
        );
    }

    public static AuditEvent failure(final UUID tenantId,
                                     final UUID actorAccountId,
                                     final String actorRole,
                                     final String action,
                                     final String resourceType,
                                     final UUID resourceId,
                                     final String reason) {
        return new AuditEvent(
            UUID.randomUUID(),
            Instant.now(),
            tenantId,
            actorAccountId,
            actorRole,
            action,
            resourceType,
            resourceId,
            AuditOutcome.FAILURE,
            reason
        );
    }
}
