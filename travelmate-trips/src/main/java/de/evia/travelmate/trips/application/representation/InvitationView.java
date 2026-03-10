package de.evia.travelmate.trips.application.representation;

import java.util.UUID;

public record InvitationView(
    UUID invitationId,
    UUID tripId,
    UUID inviteeId,
    String inviteeName,
    String invitationType,
    String status
) {
}
