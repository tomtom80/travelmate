package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record UpdateRoomCommand(
    UUID tenantId,
    UUID tripId,
    UUID roomId,
    String name,
    int bedCount
) {
}
