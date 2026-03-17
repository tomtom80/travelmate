package de.evia.travelmate.trips.application.representation;

import java.util.List;
import java.util.UUID;

public record ShoppingListRepresentation(
    UUID shoppingListId,
    UUID tripId,
    List<ShoppingItemRepresentation> recipeItems,
    List<ShoppingItemRepresentation> manualItems,
    int participantCount
) {
}
