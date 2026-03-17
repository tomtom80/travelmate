package de.evia.travelmate.trips.domain.shoppinglist;

import java.util.Optional;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.TripId;

public interface ShoppingListRepository {

    ShoppingList save(ShoppingList shoppingList);

    Optional<ShoppingList> findByTripIdAndTenantId(TripId tripId, TenantId tenantId);

    void delete(ShoppingList shoppingList);
}
