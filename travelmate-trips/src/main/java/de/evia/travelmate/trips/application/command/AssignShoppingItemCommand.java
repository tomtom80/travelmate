package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record AssignShoppingItemCommand(UUID tenantId, UUID tripId, UUID itemId, UUID memberId) {
}
