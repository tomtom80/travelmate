package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record RemoveRoomAssignmentCommand(
    UUID tenantId,
    UUID tripId,
    UUID assignmentId
) {
}
