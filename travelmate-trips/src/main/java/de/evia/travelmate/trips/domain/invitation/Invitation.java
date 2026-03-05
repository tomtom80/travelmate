package de.evia.travelmate.trips.domain.invitation;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

import de.evia.travelmate.common.domain.AggregateRoot;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.TripId;

public class Invitation extends AggregateRoot {

    private final InvitationId invitationId;
    private final TenantId tenantId;
    private final TripId tripId;
    private final UUID inviteeId;
    private final UUID invitedBy;
    private InvitationStatus status;

    public Invitation(final InvitationId invitationId,
                      final TenantId tenantId,
                      final TripId tripId,
                      final UUID inviteeId,
                      final UUID invitedBy,
                      final InvitationStatus status) {
        argumentIsNotNull(invitationId, "invitationId");
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(tripId, "tripId");
        argumentIsNotNull(inviteeId, "inviteeId");
        argumentIsNotNull(invitedBy, "invitedBy");
        argumentIsNotNull(status, "status");
        this.invitationId = invitationId;
        this.tenantId = tenantId;
        this.tripId = tripId;
        this.inviteeId = inviteeId;
        this.invitedBy = invitedBy;
        this.status = status;
    }

    public static Invitation create(final TenantId tenantId,
                                    final TripId tripId,
                                    final UUID inviteeId,
                                    final UUID invitedBy) {
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(tripId, "tripId");
        argumentIsNotNull(inviteeId, "inviteeId");
        argumentIsNotNull(invitedBy, "invitedBy");
        return new Invitation(
            new InvitationId(UUID.randomUUID()),
            tenantId, tripId, inviteeId, invitedBy,
            InvitationStatus.PENDING
        );
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

    public InvitationStatus status() {
        return status;
    }
}
