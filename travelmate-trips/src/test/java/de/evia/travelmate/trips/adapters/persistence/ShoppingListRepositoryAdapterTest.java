package de.evia.travelmate.trips.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.shoppinglist.ItemSource;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingItem;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingItemStatus;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingList;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingListRepository;
import de.evia.travelmate.trips.domain.trip.TripId;

@SpringBootTest
@ActiveProfiles("test")
class ShoppingListRepositoryAdapterTest {

    @Autowired
    private ShoppingListRepository repository;

    @Test
    void savesAndFindsShoppingListByTripIdAndTenantId() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final TripId tripId = new TripId(UUID.randomUUID());
        final ShoppingList list = ShoppingList.generate(tenantId, tripId, List.of(
            new ShoppingItem("Spaghetti", new BigDecimal("1000"), "g", ItemSource.RECIPE),
            new ShoppingItem("Tomaten", new BigDecimal("800"), "g", ItemSource.RECIPE)
        ));
        list.addManualItem("Bier", new BigDecimal("6"), "Flaschen");

        repository.save(list);

        final Optional<ShoppingList> found = repository.findByTripIdAndTenantId(tripId, tenantId);
        assertThat(found).isPresent();
        assertThat(found.get().items()).hasSize(3);
        assertThat(found.get().shoppingListId()).isEqualTo(list.shoppingListId());
    }

    @Test
    void returnsEmptyWhenNotFound() {
        final Optional<ShoppingList> found = repository.findByTripIdAndTenantId(
            new TripId(UUID.randomUUID()), new TenantId(UUID.randomUUID()));

        assertThat(found).isEmpty();
    }

    @Test
    void updatesItemStatusOnSave() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final TripId tripId = new TripId(UUID.randomUUID());
        final ShoppingList list = ShoppingList.generate(tenantId, tripId, List.of(
            new ShoppingItem("Milch", new BigDecimal("2"), "l", ItemSource.RECIPE)
        ));
        repository.save(list);

        final UUID memberId = UUID.randomUUID();
        list.assignItem(list.items().getFirst().shoppingItemId(), memberId);
        repository.save(list);

        final ShoppingList found = repository.findByTripIdAndTenantId(tripId, tenantId).orElseThrow();
        assertThat(found.items().getFirst().status()).isEqualTo(ShoppingItemStatus.ASSIGNED);
        assertThat(found.items().getFirst().assignedTo()).isEqualTo(memberId);
    }

    @Test
    void regenerationRemovesRecipeItemsAndPreservesManual() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final TripId tripId = new TripId(UUID.randomUUID());
        final ShoppingList list = ShoppingList.generate(tenantId, tripId, List.of(
            new ShoppingItem("Mehl", new BigDecimal("500"), "g", ItemSource.RECIPE)
        ));
        list.addManualItem("Snacks", new BigDecimal("3"), "Packungen");
        repository.save(list);

        list.regenerate(List.of(
            new ShoppingItem("Reis", new BigDecimal("800"), "g", ItemSource.RECIPE)
        ));
        repository.save(list);

        final ShoppingList found = repository.findByTripIdAndTenantId(tripId, tenantId).orElseThrow();
        assertThat(found.items()).hasSize(2);
        assertThat(found.items().stream().filter(i -> i.source() == ItemSource.RECIPE).count()).isEqualTo(1);
        assertThat(found.items().stream().filter(i -> i.source() == ItemSource.MANUAL).count()).isEqualTo(1);
        assertThat(found.items().stream()
            .filter(i -> i.source() == ItemSource.RECIPE)
            .findFirst().orElseThrow().name()).isEqualTo("Reis");
    }

    @Test
    void deletesShoppingList() {
        final TenantId tenantId = new TenantId(UUID.randomUUID());
        final TripId tripId = new TripId(UUID.randomUUID());
        final ShoppingList list = ShoppingList.generate(tenantId, tripId, List.of());
        repository.save(list);

        repository.delete(list);

        assertThat(repository.findByTripIdAndTenantId(tripId, tenantId)).isEmpty();
    }
}
