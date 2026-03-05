package de.evia.travelmate.trips.adapters.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "invitation")
public class InvitationJpaEntity {

    @Id
    @Column(name = "invitation_id")
    private UUID invitationId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "invitee_id", nullable = false)
    private UUID inviteeId;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Column(name = "status", nullable = false)
    private String status;

    protected InvitationJpaEntity() {
    }

    public InvitationJpaEntity(final UUID invitationId, final UUID tenantId, final UUID tripId,
                               final UUID inviteeId, final UUID invitedBy, final String status) {
        this.invitationId = invitationId;
        this.tenantId = tenantId;
        this.tripId = tripId;
        this.inviteeId = inviteeId;
        this.invitedBy = invitedBy;
        this.status = status;
    }

    public UUID getInvitationId() { return invitationId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getTripId() { return tripId; }
    public UUID getInviteeId() { return inviteeId; }
    public UUID getInvitedBy() { return invitedBy; }
    public String getStatus() { return status; }
    public void setStatus(final String status) { this.status = status; }
}
