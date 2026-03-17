package de.evia.travelmate.trips.domain.shoppinglist;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.math.BigDecimal;
import java.util.UUID;

public class ShoppingItem {

    private final ShoppingItemId shoppingItemId;
    private final String name;
    private final BigDecimal quantity;
    private final String unit;
    private final ItemSource source;
    private ShoppingItemStatus status;
    private UUID assignedTo;

    public ShoppingItem(final ShoppingItemId shoppingItemId,
                        final String name,
                        final BigDecimal quantity,
                        final String unit,
                        final ItemSource source,
                        final ShoppingItemStatus status,
                        final UUID assignedTo) {
        argumentIsNotNull(shoppingItemId, "shoppingItemId");
        argumentIsNotBlank(name, "item name");
        argumentIsNotNull(quantity, "quantity");
        argumentIsTrue(quantity.compareTo(BigDecimal.ZERO) > 0, "Quantity must be greater than zero.");
        argumentIsNotBlank(unit, "unit");
        argumentIsNotNull(source, "source");
        argumentIsNotNull(status, "status");
        this.shoppingItemId = shoppingItemId;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.source = source;
        this.status = status;
        this.assignedTo = assignedTo;
    }

    public ShoppingItem(final String name,
                        final BigDecimal quantity,
                        final String unit,
                        final ItemSource source) {
        this(new ShoppingItemId(UUID.randomUUID()), name, quantity, unit, source,
            ShoppingItemStatus.OPEN, null);
    }

    void assignTo(final UUID memberId) {
        argumentIsNotNull(memberId, "memberId");
        argumentIsTrue(this.status == ShoppingItemStatus.OPEN,
            "Only OPEN items can be assigned.");
        this.status = ShoppingItemStatus.ASSIGNED;
        this.assignedTo = memberId;
    }

    void unassign(final UUID requestingMemberId) {
        argumentIsNotNull(requestingMemberId, "requestingMemberId");
        argumentIsTrue(this.status == ShoppingItemStatus.ASSIGNED,
            "Only ASSIGNED items can be unassigned.");
        argumentIsTrue(requestingMemberId.equals(this.assignedTo),
            "Only the assignee can unassign this item.");
        this.status = ShoppingItemStatus.OPEN;
        this.assignedTo = null;
    }

    void markPurchased(final UUID requestingMemberId) {
        argumentIsNotNull(requestingMemberId, "requestingMemberId");
        if (this.status == ShoppingItemStatus.OPEN) {
            this.assignedTo = requestingMemberId;
        } else {
            argumentIsTrue(this.status == ShoppingItemStatus.ASSIGNED,
                "Only OPEN or ASSIGNED items can be marked as purchased.");
            argumentIsTrue(requestingMemberId.equals(this.assignedTo),
                "Only the assignee can mark this item as purchased.");
        }
        this.status = ShoppingItemStatus.PURCHASED;
    }

    void undoPurchase() {
        argumentIsTrue(this.status == ShoppingItemStatus.PURCHASED,
            "Only PURCHASED items can be un-purchased.");
        this.status = ShoppingItemStatus.ASSIGNED;
    }

    public ShoppingItemId shoppingItemId() {
        return shoppingItemId;
    }

    public String name() {
        return name;
    }

    public BigDecimal quantity() {
        return quantity;
    }

    public String unit() {
        return unit;
    }

    public ItemSource source() {
        return source;
    }

    public ShoppingItemStatus status() {
        return status;
    }

    public UUID assignedTo() {
        return assignedTo;
    }
}
