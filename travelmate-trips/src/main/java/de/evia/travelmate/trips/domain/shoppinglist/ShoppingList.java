package de.evia.travelmate.trips.domain.shoppinglist;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import de.evia.travelmate.common.domain.AggregateRoot;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.TripId;

public class ShoppingList extends AggregateRoot {

    private final ShoppingListId shoppingListId;
    private final TenantId tenantId;
    private final TripId tripId;
    private final List<ShoppingItem> items;

    public ShoppingList(final ShoppingListId shoppingListId,
                        final TenantId tenantId,
                        final TripId tripId,
                        final List<ShoppingItem> items) {
        argumentIsNotNull(shoppingListId, "shoppingListId");
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(tripId, "tripId");
        argumentIsNotNull(items, "items");
        this.shoppingListId = shoppingListId;
        this.tenantId = tenantId;
        this.tripId = tripId;
        this.items = new ArrayList<>(items);
    }

    public static ShoppingList generate(final TenantId tenantId,
                                        final TripId tripId,
                                        final List<ShoppingItem> recipeItems) {
        argumentIsNotNull(recipeItems, "recipeItems");
        return new ShoppingList(
            new ShoppingListId(UUID.randomUUID()),
            tenantId,
            tripId,
            new ArrayList<>(recipeItems)
        );
    }

    public void regenerate(final List<ShoppingItem> newRecipeItems) {
        argumentIsNotNull(newRecipeItems, "newRecipeItems");
        items.removeIf(item -> item.source() == ItemSource.RECIPE);
        items.addAll(newRecipeItems);
    }

    public ShoppingItemId addManualItem(final String name,
                                        final BigDecimal quantity,
                                        final String unit) {
        final ShoppingItem item = new ShoppingItem(name, quantity, unit, ItemSource.MANUAL);
        items.add(item);
        return item.shoppingItemId();
    }

    public void removeItem(final ShoppingItemId itemId) {
        argumentIsNotNull(itemId, "itemId");
        final ShoppingItem item = findItem(itemId);
        argumentIsTrue(item.source() == ItemSource.MANUAL,
            "Only MANUAL items can be removed.");
        argumentIsTrue(item.status() != ShoppingItemStatus.PURCHASED,
            "PURCHASED items cannot be removed.");
        items.remove(item);
    }

    public void assignItem(final ShoppingItemId itemId, final UUID memberId) {
        argumentIsNotNull(itemId, "itemId");
        findItem(itemId).assignTo(memberId);
    }

    public void unassignItem(final ShoppingItemId itemId, final UUID requestingMemberId) {
        argumentIsNotNull(itemId, "itemId");
        findItem(itemId).unassign(requestingMemberId);
    }

    public void markPurchased(final ShoppingItemId itemId, final UUID requestingMemberId) {
        argumentIsNotNull(itemId, "itemId");
        findItem(itemId).markPurchased(requestingMemberId);
    }

    public void undoPurchase(final ShoppingItemId itemId) {
        argumentIsNotNull(itemId, "itemId");
        findItem(itemId).undoPurchase();
    }

    private ShoppingItem findItem(final ShoppingItemId itemId) {
        return items.stream()
            .filter(item -> item.shoppingItemId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "ShoppingItem " + itemId.value() + " not found in this shopping list."));
    }

    public ShoppingListId shoppingListId() {
        return shoppingListId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public TripId tripId() {
        return tripId;
    }

    public List<ShoppingItem> items() {
        return Collections.unmodifiableList(items);
    }
}
