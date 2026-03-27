package de.evia.travelmate.trips.application.representation;

import java.util.UUID;

import de.evia.travelmate.trips.domain.invitation.Invitation;

public record InvitationRepresentation(
    UUID invitationId,
    UUID tenantId,
    UUID tripId,
    UUID inviteeId,
    UUID invitedBy,
    String inviteeEmail,
    UUID targetPartyTenantId,
    String invitationType,
    String status
) {

    public InvitationRepresentation(final Invitation invitation) {
        this(
            invitation.invitationId().value(),
            invitation.tenantId().value(),
            invitation.tripId().value(),
            invitation.inviteeId(),
            invitation.invitedBy(),
            invitation.inviteeEmail(),
            invitation.targetPartyTenantId(),
            invitation.invitationType().name(),
            invitation.status().name()
        );
    }
}
