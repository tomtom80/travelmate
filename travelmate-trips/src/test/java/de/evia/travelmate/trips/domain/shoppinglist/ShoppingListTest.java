package de.evia.travelmate.trips.domain.shoppinglist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.TripId;

class ShoppingListTest {

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final TripId TRIP_ID = new TripId(UUID.randomUUID());
    private static final UUID MEMBER_A = UUID.randomUUID();
    private static final UUID MEMBER_B = UUID.randomUUID();

    @Test
    void generateCreatesShoppingListWithRecipeItems() {
        final List<ShoppingItem> recipeItems = List.of(
            new ShoppingItem("Spaghetti", new BigDecimal("1000"), "g", ItemSource.RECIPE),
            new ShoppingItem("Tomaten", new BigDecimal("800"), "g", ItemSource.RECIPE)
        );

        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, recipeItems);

        assertThat(list.shoppingListId()).isNotNull();
        assertThat(list.tenantId()).isEqualTo(TENANT_ID);
        assertThat(list.tripId()).isEqualTo(TRIP_ID);
        assertThat(list.items()).hasSize(2);
        assertThat(list.items().getFirst().source()).isEqualTo(ItemSource.RECIPE);
        assertThat(list.items().getFirst().status()).isEqualTo(ShoppingItemStatus.OPEN);
    }

    @Test
    void generateWithEmptyRecipeItemsCreatesEmptyList() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of());

        assertThat(list.items()).isEmpty();
    }

    @Test
    void addManualItemAddsItemWithCorrectSource() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of());

        final ShoppingItemId itemId = list.addManualItem("Bier", new BigDecimal("12"), "Flaschen");

        assertThat(itemId).isNotNull();
        assertThat(list.items()).hasSize(1);
        final ShoppingItem item = list.items().getFirst();
        assertThat(item.name()).isEqualTo("Bier");
        assertThat(item.quantity()).isEqualByComparingTo(new BigDecimal("12"));
        assertThat(item.unit()).isEqualTo("Flaschen");
        assertThat(item.source()).isEqualTo(ItemSource.MANUAL);
        assertThat(item.status()).isEqualTo(ShoppingItemStatus.OPEN);
    }

    @Test
    void removeItemRemovesManualOpenItem() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of());
        final ShoppingItemId itemId = list.addManualItem("Chips", new BigDecimal("2"), "Tueten");

        list.removeItem(itemId);

        assertThat(list.items()).isEmpty();
    }

    @Test
    void removeItemRejectsRecipeItem() {
        final ShoppingItem recipeItem = new ShoppingItem("Mehl", new BigDecimal("500"), "g", ItemSource.RECIPE);
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of(recipeItem));

        assertThatThrownBy(() -> list.removeItem(recipeItem.shoppingItemId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("MANUAL");
    }

    @Test
    void removeItemRejectsPurchasedManualItem() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of());
        final ShoppingItemId itemId = list.addManualItem("Wein", new BigDecimal("3"), "Flaschen");
        list.assignItem(itemId, MEMBER_A);
        list.markPurchased(itemId, MEMBER_A);

        assertThatThrownBy(() -> list.removeItem(itemId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PURCHASED");
    }

    @Test
    void assignItemTransitionsOpenToAssigned() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of(
            new ShoppingItem("Milch", new BigDecimal("2"), "l", ItemSource.RECIPE)
        ));
        final ShoppingItemId itemId = list.items().getFirst().shoppingItemId();

        list.assignItem(itemId, MEMBER_A);

        final ShoppingItem item = list.items().getFirst();
        assertThat(item.status()).isEqualTo(ShoppingItemStatus.ASSIGNED);
        assertThat(item.assignedTo()).isEqualTo(MEMBER_A);
    }

    @Test
    void assignItemRejectsAlreadyAssignedItem() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of(
            new ShoppingItem("Milch", new BigDecimal("2"), "l", ItemSource.RECIPE)
        ));
        final ShoppingItemId itemId = list.items().getFirst().shoppingItemId();
        list.assignItem(itemId, MEMBER_A);

        assertThatThrownBy(() -> list.assignItem(itemId, MEMBER_B))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("OPEN");
    }

    @Test
    void unassignItemTransitionsAssignedToOpen() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of(
            new ShoppingItem("Butter", new BigDecimal("250"), "g", ItemSource.RECIPE)
        ));
        final ShoppingItemId itemId = list.items().getFirst().shoppingItemId();
        list.assignItem(itemId, MEMBER_A);

        list.unassignItem(itemId, MEMBER_A);

        final ShoppingItem item = list.items().getFirst();
        assertThat(item.status()).isEqualTo(ShoppingItemStatus.OPEN);
        assertThat(item.assignedTo()).isNull();
    }

    @Test
    void unassignItemRejectsIfNotAssignee() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of(
            new ShoppingItem("Butter", new BigDecimal("250"), "g", ItemSource.RECIPE)
        ));
        final ShoppingItemId itemId = list.items().getFirst().shoppingItemId();
        list.assignItem(itemId, MEMBER_A);

        assertThatThrownBy(() -> list.unassignItem(itemId, MEMBER_B))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("assignee");
    }

    @Test
    void markPurchasedTransitionsAssignedToPurchased() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of(
            new ShoppingItem("Eier", new BigDecimal("12"), "Stueck", ItemSource.RECIPE)
        ));
        final ShoppingItemId itemId = list.items().getFirst().shoppingItemId();
        list.assignItem(itemId, MEMBER_A);

        list.markPurchased(itemId, MEMBER_A);

        assertThat(list.items().getFirst().status()).isEqualTo(ShoppingItemStatus.PURCHASED);
    }

    @Test
    void markPurchasedAllowsDirectPurchaseFromOpen() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of(
            new ShoppingItem("Brot", new BigDecimal("1"), "Stueck", ItemSource.RECIPE)
        ));
        final ShoppingItemId itemId = list.items().getFirst().shoppingItemId();

        list.markPurchased(itemId, MEMBER_A);

        final ShoppingItem item = list.items().getFirst();
        assertThat(item.status()).isEqualTo(ShoppingItemStatus.PURCHASED);
        assertThat(item.assignedTo()).isEqualTo(MEMBER_A);
    }

    @Test
    void markPurchasedRejectsIfAssignedToOtherMember() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of(
            new ShoppingItem("Kaese", new BigDecimal("300"), "g", ItemSource.RECIPE)
        ));
        final ShoppingItemId itemId = list.items().getFirst().shoppingItemId();
        list.assignItem(itemId, MEMBER_A);

        assertThatThrownBy(() -> list.markPurchased(itemId, MEMBER_B))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("assignee");
    }

    @Test
    void undoPurchaseTransitionsPurchasedToAssigned() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of(
            new ShoppingItem("Sahne", new BigDecimal("500"), "ml", ItemSource.RECIPE)
        ));
        final ShoppingItemId itemId = list.items().getFirst().shoppingItemId();
        list.assignItem(itemId, MEMBER_A);
        list.markPurchased(itemId, MEMBER_A);

        list.undoPurchase(itemId);

        final ShoppingItem item = list.items().getFirst();
        assertThat(item.status()).isEqualTo(ShoppingItemStatus.ASSIGNED);
        assertThat(item.assignedTo()).isEqualTo(MEMBER_A);
    }

    @Test
    void undoPurchaseRejectsNonPurchasedItem() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of(
            new ShoppingItem("Sahne", new BigDecimal("500"), "ml", ItemSource.RECIPE)
        ));
        final ShoppingItemId itemId = list.items().getFirst().shoppingItemId();

        assertThatThrownBy(() -> list.undoPurchase(itemId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PURCHASED");
    }

    @Test
    void regenerateReplacesRecipeItemsAndPreservesManualItems() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of(
            new ShoppingItem("Mehl", new BigDecimal("500"), "g", ItemSource.RECIPE)
        ));
        list.addManualItem("Bier", new BigDecimal("6"), "Flaschen");
        assertThat(list.items()).hasSize(2);

        final List<ShoppingItem> newRecipeItems = List.of(
            new ShoppingItem("Reis", new BigDecimal("800"), "g", ItemSource.RECIPE),
            new ShoppingItem("Sojasauce", new BigDecimal("100"), "ml", ItemSource.RECIPE)
        );

        list.regenerate(newRecipeItems);

        assertThat(list.items()).hasSize(3);
        assertThat(list.items().stream().filter(i -> i.source() == ItemSource.MANUAL).count())
            .isEqualTo(1);
        assertThat(list.items().stream().filter(i -> i.source() == ItemSource.RECIPE).count())
            .isEqualTo(2);
        assertThat(list.items().stream()
            .filter(i -> i.source() == ItemSource.MANUAL)
            .findFirst().orElseThrow().name()).isEqualTo("Bier");
    }

    @Test
    void regeneratePreservesManualItemStatus() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of());
        final ShoppingItemId manualId = list.addManualItem("Sonnencreme", new BigDecimal("1"), "Tube");
        list.assignItem(manualId, MEMBER_A);
        list.markPurchased(manualId, MEMBER_A);

        list.regenerate(List.of(
            new ShoppingItem("Nudeln", new BigDecimal("500"), "g", ItemSource.RECIPE)
        ));

        final ShoppingItem manual = list.items().stream()
            .filter(i -> i.source() == ItemSource.MANUAL).findFirst().orElseThrow();
        assertThat(manual.status()).isEqualTo(ShoppingItemStatus.PURCHASED);
        assertThat(manual.assignedTo()).isEqualTo(MEMBER_A);
    }

    @Test
    void itemsListIsUnmodifiable() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of());

        assertThatThrownBy(() -> list.items().add(
            new ShoppingItem("Hack", new BigDecimal("500"), "g", ItemSource.RECIPE)))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void findItemThrowsForUnknownId() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of());
        final ShoppingItemId unknownId = new ShoppingItemId(UUID.randomUUID());

        assertThatThrownBy(() -> list.assignItem(unknownId, MEMBER_A))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void removeManualItemInAssignedStatusIsAllowed() {
        final ShoppingList list = ShoppingList.generate(TENANT_ID, TRIP_ID, List.of());
        final ShoppingItemId itemId = list.addManualItem("Salz", new BigDecimal("1"), "Packung");
        list.assignItem(itemId, MEMBER_A);

        list.removeItem(itemId);

        assertThat(list.items()).isEmpty();
    }
}
