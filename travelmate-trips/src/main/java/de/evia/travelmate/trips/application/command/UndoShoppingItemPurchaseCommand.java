package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record UndoShoppingItemPurchaseCommand(UUID tenantId, UUID tripId, UUID itemId) {
}
