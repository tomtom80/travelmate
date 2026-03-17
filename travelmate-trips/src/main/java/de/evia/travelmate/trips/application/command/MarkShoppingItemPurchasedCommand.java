package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record MarkShoppingItemPurchasedCommand(UUID tenantId, UUID tripId, UUID itemId,
                                               UUID requestingMemberId) {
}
