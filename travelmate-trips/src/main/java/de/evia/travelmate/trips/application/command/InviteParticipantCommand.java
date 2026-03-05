package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record InviteParticipantCommand(
    UUID tenantId,
    UUID tripId,
    UUID inviteeId,
    UUID invitedBy
) {
}
