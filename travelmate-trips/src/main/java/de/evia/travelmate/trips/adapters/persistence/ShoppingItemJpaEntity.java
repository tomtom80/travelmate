package de.evia.travelmate.trips.adapters.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "shopping_item")
public class ShoppingItemJpaEntity {

    @Id
    @Column(name = "shopping_item_id")
    private UUID shoppingItemId;

    @ManyToOne
    @JoinColumn(name = "shopping_list_id", nullable = false)
    private ShoppingListJpaEntity shoppingList;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "source", nullable = false, length = 10)
    private String source;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    protected ShoppingItemJpaEntity() {
    }

    public ShoppingItemJpaEntity(final UUID shoppingItemId,
                                 final ShoppingListJpaEntity shoppingList,
                                 final String name,
                                 final BigDecimal quantity,
                                 final String unit,
                                 final String source,
                                 final String status,
                                 final UUID assignedTo) {
        this.shoppingItemId = shoppingItemId;
        this.shoppingList = shoppingList;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.source = source;
        this.status = status;
        this.assignedTo = assignedTo;
    }

    public UUID getShoppingItemId() { return shoppingItemId; }
    public ShoppingListJpaEntity getShoppingList() { return shoppingList; }
    public String getName() { return name; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public String getSource() { return source; }
    public String getStatus() { return status; }
    public UUID getAssignedTo() { return assignedTo; }

    public void setName(final String name) { this.name = name; }
    public void setQuantity(final BigDecimal quantity) { this.quantity = quantity; }
    public void setUnit(final String unit) { this.unit = unit; }
    public void setStatus(final String status) { this.status = status; }
    public void setAssignedTo(final UUID assignedTo) { this.assignedTo = assignedTo; }
}
