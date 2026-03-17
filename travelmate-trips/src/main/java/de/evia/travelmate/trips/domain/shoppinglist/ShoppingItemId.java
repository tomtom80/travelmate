package de.evia.travelmate.trips.domain.shoppinglist;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record ShoppingItemId(UUID value) {

    public ShoppingItemId {
        argumentIsNotNull(value, "shoppingItemId");
    }
}
