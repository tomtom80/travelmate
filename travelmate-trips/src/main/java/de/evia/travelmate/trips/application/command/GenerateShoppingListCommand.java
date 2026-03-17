package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record GenerateShoppingListCommand(UUID tenantId, UUID tripId) {
}
