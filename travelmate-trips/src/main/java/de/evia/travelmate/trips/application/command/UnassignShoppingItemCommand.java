package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record UnassignShoppingItemCommand(UUID tenantId, UUID tripId, UUID itemId,
                                          UUID requestingMemberId) {
}
