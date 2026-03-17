package de.evia.travelmate.trips.application.representation;

import java.util.UUID;

import de.evia.travelmate.trips.domain.shoppinglist.ShoppingItem;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingItemStatus;

public record ShoppingItemRepresentation(
    UUID itemId,
    String name,
    String quantity,
    String unit,
    String source,
    ShoppingItemStatus status,
    String assigneeName,
    boolean assignedToCurrentUser,
    boolean manual
) {

    public static ShoppingItemRepresentation from(final ShoppingItem item,
                                                   final String assigneeName,
                                                   final UUID currentMemberId) {
        return new ShoppingItemRepresentation(
            item.shoppingItemId().value(),
            item.name(),
            item.quantity().stripTrailingZeros().toPlainString(),
            item.unit(),
            item.source().name(),
            item.status(),
            assigneeName,
            item.assignedTo() != null && item.assignedTo().equals(currentMemberId),
            item.source() == de.evia.travelmate.trips.domain.shoppinglist.ItemSource.MANUAL
        );
    }
}
