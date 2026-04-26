package de.evia.travelmate.trips.adapters.persistence;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "meal_slot")
public class MealSlotJpaEntity {

    @Id
    @Column(name = "meal_slot_id")
    private UUID mealSlotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlanJpaEntity mealPlan;

    @Column(name = "slot_date", nullable = false)
    private LocalDate date;

    @Column(name = "meal_type", nullable = false)
    private String mealType;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "recipe_id")
    private UUID recipeId;

    @Column(name = "kitchen_duty_participant_ids", columnDefinition = "text")
    private String kitchenDutyParticipantIdsJson;

    protected MealSlotJpaEntity() {
    }

    public MealSlotJpaEntity(final UUID mealSlotId, final MealPlanJpaEntity mealPlan,
                             final LocalDate date, final String mealType,
                             final String status, final UUID recipeId) {
        this.mealSlotId = mealSlotId;
        this.mealPlan = mealPlan;
        this.date = date;
        this.mealType = mealType;
        this.status = status;
        this.recipeId = recipeId;
    }

    public UUID getMealSlotId() { return mealSlotId; }
    public LocalDate getDate() { return date; }
    public String getMealType() { return mealType; }
    public String getStatus() { return status; }
    public void setStatus(final String status) { this.status = status; }
    public UUID getRecipeId() { return recipeId; }
    public void setRecipeId(final UUID recipeId) { this.recipeId = recipeId; }
    public String getKitchenDutyParticipantIdsJson() { return kitchenDutyParticipantIdsJson; }
    public void setKitchenDutyParticipantIdsJson(final String json) { this.kitchenDutyParticipantIdsJson = json; }
}
