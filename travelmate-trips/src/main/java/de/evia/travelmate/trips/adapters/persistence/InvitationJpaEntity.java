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

    @Column(name = "invitee_id")
    private UUID inviteeId;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Column(name = "invitee_email")
    private String inviteeEmail;

    @Column(name = "target_party_tenant_id")
    private UUID targetPartyTenantId;

    @Column(name = "invitation_type", nullable = false)
    private String invitationType;

    @Column(name = "status", nullable = false)
    private String status;

    protected InvitationJpaEntity() {
    }

    public InvitationJpaEntity(final UUID invitationId, final UUID tenantId, final UUID tripId,
                               final UUID inviteeId, final UUID invitedBy, final String inviteeEmail,
                               final UUID targetPartyTenantId, final String invitationType, final String status) {
        this.invitationId = invitationId;
        this.tenantId = tenantId;
        this.tripId = tripId;
        this.inviteeId = inviteeId;
        this.invitedBy = invitedBy;
        this.inviteeEmail = inviteeEmail;
        this.targetPartyTenantId = targetPartyTenantId;
        this.invitationType = invitationType;
        this.status = status;
    }

    public UUID getInvitationId() { return invitationId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getTripId() { return tripId; }
    public UUID getInviteeId() { return inviteeId; }
    public void setInviteeId(final UUID inviteeId) { this.inviteeId = inviteeId; }
    public UUID getInvitedBy() { return invitedBy; }
    public String getInviteeEmail() { return inviteeEmail; }
    public UUID getTargetPartyTenantId() { return targetPartyTenantId; }
    public void setTargetPartyTenantId(final UUID targetPartyTenantId) { this.targetPartyTenantId = targetPartyTenantId; }
    public String getInvitationType() { return invitationType; }
    public String getStatus() { return status; }
    public void setStatus(final String status) { this.status = status; }
}
