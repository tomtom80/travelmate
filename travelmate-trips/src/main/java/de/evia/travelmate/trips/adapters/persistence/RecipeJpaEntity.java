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
import jakarta.persistence.Table;

@Entity
@Table(name = "recipe")
public class RecipeJpaEntity {

    @Id
    @Column(name = "recipe_id")
    private UUID recipeId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "servings", nullable = false)
    private int servings;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<IngredientJpaEntity> ingredients = new ArrayList<>();

    protected RecipeJpaEntity() {
    }

    public RecipeJpaEntity(final UUID recipeId, final UUID tenantId, final String name, final int servings) {
        this.recipeId = recipeId;
        this.tenantId = tenantId;
        this.name = name;
        this.servings = servings;
    }

    public UUID getRecipeId() { return recipeId; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public void setName(final String name) { this.name = name; }
    public int getServings() { return servings; }
    public void setServings(final int servings) { this.servings = servings; }
    public List<IngredientJpaEntity> getIngredients() { return ingredients; }
}
