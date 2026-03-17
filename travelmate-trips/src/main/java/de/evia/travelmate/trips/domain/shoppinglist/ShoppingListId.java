package de.evia.travelmate.trips.domain.shoppinglist;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record ShoppingListId(UUID value) {

    public ShoppingListId {
        argumentIsNotNull(value, "shoppingListId");
    }
}
