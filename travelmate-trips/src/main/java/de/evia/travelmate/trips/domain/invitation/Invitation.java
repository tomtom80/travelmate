package de.evia.travelmate.trips.domain.invitation;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

import de.evia.travelmate.common.domain.AggregateRoot;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.TripId;

public class Invitation extends AggregateRoot {

    private final InvitationId invitationId;
    private final TenantId tenantId;
    private final TripId tripId;
    private UUID inviteeId;
    private final UUID invitedBy;
    private final String inviteeEmail;
    private UUID targetPartyTenantId;
    private final InvitationType invitationType;
    private InvitationStatus status;

    public Invitation(final InvitationId invitationId,
                      final TenantId tenantId,
                      final TripId tripId,
                      final UUID inviteeId,
                      final UUID invitedBy,
                      final String inviteeEmail,
                      final UUID targetPartyTenantId,
                      final InvitationType invitationType,
                      final InvitationStatus status) {
        argumentIsNotNull(invitationId, "invitationId");
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(tripId, "tripId");
        argumentIsNotNull(invitedBy, "invitedBy");
        argumentIsNotNull(invitationType, "invitationType");
        argumentIsNotNull(status, "status");
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

    public static Invitation create(final TenantId tenantId,
                                    final TripId tripId,
                                    final UUID inviteeId,
                                    final UUID invitedBy,
                                    final UUID targetPartyTenantId) {
        argumentIsNotNull(inviteeId, "inviteeId");
        return new Invitation(
            new InvitationId(UUID.randomUUID()),
            tenantId, tripId, inviteeId, invitedBy,
            null, targetPartyTenantId, InvitationType.MEMBER, InvitationStatus.PENDING
        );
    }

    public static Invitation inviteExternal(final TenantId tenantId,
                                            final TripId tripId,
                                            final String inviteeEmail,
                                            final UUID invitedBy) {
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(tripId, "tripId");
        argumentIsNotBlank(inviteeEmail, "inviteeEmail");
        argumentIsNotNull(invitedBy, "invitedBy");
        return new Invitation(
            new InvitationId(UUID.randomUUID()),
            tenantId, tripId, null, invitedBy,
            inviteeEmail, null, InvitationType.EXTERNAL, InvitationStatus.AWAITING_REGISTRATION
        );
    }

    public void linkToMember(final UUID memberId, final UUID targetPartyTenantId) {
        argumentIsNotNull(memberId, "memberId");
        if (this.status != InvitationStatus.AWAITING_REGISTRATION) {
            throw new IllegalStateException(
                "Cannot link member to invitation in status " + this.status);
        }
        this.inviteeId = memberId;
        this.targetPartyTenantId = targetPartyTenantId;
        this.status = InvitationStatus.ACCEPTED;
    }

    public void accept() {
        assertPending("accept");
        this.status = InvitationStatus.ACCEPTED;
    }

    public void decline() {
        assertPending("decline");
        this.status = InvitationStatus.DECLINED;
    }

    private void assertPending(final String action) {
        if (this.status != InvitationStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot " + action + " invitation in status " + this.status);
        }
    }

    public InvitationId invitationId() {
        return invitationId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public TripId tripId() {
        return tripId;
    }

    public UUID inviteeId() {
        return inviteeId;
    }

    public UUID invitedBy() {
        return invitedBy;
    }

    public String inviteeEmail() {
        return inviteeEmail;
    }

    public UUID targetPartyTenantId() {
        return targetPartyTenantId;
    }

    public InvitationType invitationType() {
        return invitationType;
    }

    public InvitationStatus status() {
        return status;
    }
}
