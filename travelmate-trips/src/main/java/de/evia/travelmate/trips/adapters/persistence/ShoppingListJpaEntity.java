package de.evia.travelmate.trips.adapters.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "shopping_list")
public class ShoppingListJpaEntity {

    @Id
    @Column(name = "shopping_list_id")
    private UUID shoppingListId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @OneToMany(mappedBy = "shoppingList", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("source ASC, name ASC")
    private List<ShoppingItemJpaEntity> items = new ArrayList<>();

    protected ShoppingListJpaEntity() {
    }

    public ShoppingListJpaEntity(final UUID shoppingListId, final UUID tenantId, final UUID tripId) {
        this.shoppingListId = shoppingListId;
        this.tenantId = tenantId;
        this.tripId = tripId;
    }

    public UUID getShoppingListId() { return shoppingListId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getTripId() { return tripId; }
    public List<ShoppingItemJpaEntity> getItems() { return items; }
}
