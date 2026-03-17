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
@Table(name = "meal_plan")
public class MealPlanJpaEntity {

    @Id
    @Column(name = "meal_plan_id")
    private UUID mealPlanId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("date ASC, mealType ASC")
    private List<MealSlotJpaEntity> slots = new ArrayList<>();

    protected MealPlanJpaEntity() {
    }

    public MealPlanJpaEntity(final UUID mealPlanId, final UUID tenantId, final UUID tripId) {
        this.mealPlanId = mealPlanId;
        this.tenantId = tenantId;
        this.tripId = tripId;
    }

    public UUID getMealPlanId() { return mealPlanId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getTripId() { return tripId; }
    public List<MealSlotJpaEntity> getSlots() { return slots; }
}
