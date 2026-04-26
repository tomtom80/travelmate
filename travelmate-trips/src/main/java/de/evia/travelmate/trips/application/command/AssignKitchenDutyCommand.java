package de.evia.travelmate.trips.application.command;

import java.util.List;
import java.util.UUID;

public record AssignKitchenDutyCommand(
    UUID tripId,
    UUID mealSlotId,
    List<UUID> participantIds
) {
}
