package de.evia.travelmate.iam.adapters.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import de.evia.travelmate.webcommons.audit.AuditEvent;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @Column(name = "audit_id", nullable = false)
    private UUID auditId;

    @Column(name = "occurred_on", nullable = false)
    private Instant occurredOn;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "actor_account_id")
    private UUID actorAccountId;

    @Column(name = "actor_role", length = 64)
    private String actorRole;

    @Column(name = "action", length = 128, nullable = false)
    private String action;

    @Column(name = "resource_type", length = 128, nullable = false)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "outcome", length = 16, nullable = false)
    private String outcome;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    protected AuditLogEntity() {
    }

    private AuditLogEntity(final UUID auditId,
                           final Instant occurredOn,
                           final UUID tenantId,
                           final UUID actorAccountId,
                           final String actorRole,
                           final String action,
                           final String resourceType,
                           final UUID resourceId,
                           final String outcome,
                           final String reason) {
        this.auditId = auditId;
        this.occurredOn = occurredOn;
        this.tenantId = tenantId;
        this.actorAccountId = actorAccountId;
        this.actorRole = actorRole;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.outcome = outcome;
        this.reason = reason;
    }

    public static AuditLogEntity from(final AuditEvent event) {
        return new AuditLogEntity(
            event.auditId(),
            event.occurredOn(),
            event.tenantId(),
            event.actorAccountId(),
            event.actorRole(),
            event.action(),
            event.resourceType(),
            event.resourceId(),
            event.outcome().name(),
            event.reason()
        );
    }

    public UUID getAuditId() {
        return auditId;
    }

    public Instant getOccurredOn() {
        return occurredOn;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getActorAccountId() {
        return actorAccountId;
    }

    public String getActorRole() {
        return actorRole;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getReason() {
        return reason;
    }
}
