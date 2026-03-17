package de.evia.travelmate.trips.adapters.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipe_ingredient")
public class IngredientJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ingredient_id")
    private UUID ingredientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private RecipeJpaEntity recipe;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit", nullable = false)
    private String unit;

    protected IngredientJpaEntity() {
    }

    public IngredientJpaEntity(final RecipeJpaEntity recipe, final String name,
                               final BigDecimal quantity, final String unit) {
        this.recipe = recipe;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }

    public UUID getIngredientId() { return ingredientId; }
    public String getName() { return name; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
}
