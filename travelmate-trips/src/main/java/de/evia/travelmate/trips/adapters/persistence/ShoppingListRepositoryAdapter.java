package de.evia.travelmate.trips.adapters.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.shoppinglist.ItemSource;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingItem;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingItemId;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingItemStatus;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingList;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingListId;
import de.evia.travelmate.trips.domain.shoppinglist.ShoppingListRepository;
import de.evia.travelmate.trips.domain.trip.TripId;

@Repository
public class ShoppingListRepositoryAdapter implements ShoppingListRepository {

    private final ShoppingListJpaRepository jpaRepository;

    public ShoppingListRepositoryAdapter(final ShoppingListJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ShoppingList save(final ShoppingList shoppingList) {
        final ShoppingListJpaEntity entity = jpaRepository.findById(shoppingList.shoppingListId().value())
            .orElseGet(() -> new ShoppingListJpaEntity(
                shoppingList.shoppingListId().value(),
                shoppingList.tenantId().value(),
                shoppingList.tripId().value()
            ));
        syncItems(entity, shoppingList);
        jpaRepository.save(entity);
        return shoppingList;
    }

    @Override
    public Optional<ShoppingList> findByTripIdAndTenantId(final TripId tripId, final TenantId tenantId) {
        return jpaRepository.findByTripIdAndTenantId(tripId.value(), tenantId.value())
            .map(this::toDomain);
    }

    @Override
    public void delete(final ShoppingList shoppingList) {
        jpaRepository.deleteById(shoppingList.shoppingListId().value());
    }

    private void syncItems(final ShoppingListJpaEntity entity, final ShoppingList shoppingList) {
        // Remove items that no longer exist in domain
        entity.getItems().removeIf(jpaItem ->
            shoppingList.items().stream()
                .noneMatch(domainItem -> domainItem.shoppingItemId().value().equals(jpaItem.getShoppingItemId()))
        );

        // Update existing and add new items
        for (final ShoppingItem item : shoppingList.items()) {
            final Optional<ShoppingItemJpaEntity> existing = entity.getItems().stream()
                .filter(jpaItem -> jpaItem.getShoppingItemId().equals(item.shoppingItemId().value()))
                .findFirst();
            if (existing.isPresent()) {
                final ShoppingItemJpaEntity jpaItem = existing.get();
                jpaItem.setName(item.name());
                jpaItem.setQuantity(item.quantity());
                jpaItem.setUnit(item.unit());
                jpaItem.setStatus(item.status().name());
                jpaItem.setAssignedTo(item.assignedTo());
            } else {
                entity.getItems().add(new ShoppingItemJpaEntity(
                    item.shoppingItemId().value(),
                    entity,
                    item.name(),
                    item.quantity(),
                    item.unit(),
                    item.source().name(),
                    item.status().name(),
                    item.assignedTo()
                ));
            }
        }
    }

    private ShoppingList toDomain(final ShoppingListJpaEntity entity) {
        final var items = entity.getItems().stream()
            .map(jpaItem -> new ShoppingItem(
                new ShoppingItemId(jpaItem.getShoppingItemId()),
                jpaItem.getName(),
                jpaItem.getQuantity(),
                jpaItem.getUnit(),
                ItemSource.valueOf(jpaItem.getSource()),
                ShoppingItemStatus.valueOf(jpaItem.getStatus()),
                jpaItem.getAssignedTo()
            ))
            .toList();
        return new ShoppingList(
            new ShoppingListId(entity.getShoppingListId()),
            new TenantId(entity.getTenantId()),
            new TripId(entity.getTripId()),
            items
        );
    }
}
