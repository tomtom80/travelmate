package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record RemoveShoppingItemCommand(UUID tenantId, UUID tripId, UUID itemId) {
}
