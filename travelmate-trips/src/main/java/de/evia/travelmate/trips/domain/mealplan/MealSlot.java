package de.evia.travelmate.trips.domain.mealplan;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import de.evia.travelmate.common.domain.BusinessRuleViolationException;

public class MealSlot {

    private final MealSlotId mealSlotId;
    private final LocalDate date;
    private final MealType mealType;
    private MealSlotStatus status;
    private UUID recipeId;
    private List<UUID> kitchenDutyParticipantIds;

    public MealSlot(final MealSlotId mealSlotId,
                    final LocalDate date,
                    final MealType mealType,
                    final MealSlotStatus status,
                    final UUID recipeId) {
        this(mealSlotId, date, mealType, status, recipeId, List.of());
    }

    public MealSlot(final MealSlotId mealSlotId,
                    final LocalDate date,
                    final MealType mealType,
                    final MealSlotStatus status,
                    final UUID recipeId,
                    final List<UUID> kitchenDutyParticipantIds) {
        argumentIsNotNull(mealSlotId, "mealSlotId");
        argumentIsNotNull(date, "date");
        argumentIsNotNull(mealType, "mealType");
        argumentIsNotNull(status, "status");
        this.mealSlotId = mealSlotId;
        this.date = date;
        this.mealType = mealType;
        this.status = status;
        this.recipeId = recipeId;
        this.kitchenDutyParticipantIds = kitchenDutyParticipantIds == null
            ? List.of()
            : List.copyOf(kitchenDutyParticipantIds);
    }

    public MealSlot(final LocalDate date, final MealType mealType) {
        this(new MealSlotId(UUID.randomUUID()), date, mealType, MealSlotStatus.PLANNED, null);
    }

    void markStatus(final MealSlotStatus newStatus) {
        this.status = newStatus;
        if (newStatus == MealSlotStatus.SKIP || newStatus == MealSlotStatus.EATING_OUT) {
            this.recipeId = null;
            this.kitchenDutyParticipantIds = List.of();
        }
    }

    void assignRecipe(final UUID recipeId) {
        this.status = MealSlotStatus.PLANNED;
        this.recipeId = recipeId;
    }

    void clearRecipe() {
        this.recipeId = null;
    }

    void assignKitchenDuty(final List<UUID> participantIds) {
        argumentIsNotNull(participantIds, "participantIds");
        if (status != MealSlotStatus.PLANNED) {
            throw new BusinessRuleViolationException("mealslot.error.kitchenDutyRequiresPlanned");
        }
        this.kitchenDutyParticipantIds = List.copyOf(participantIds);
    }

    void clearKitchenDuty() {
        this.kitchenDutyParticipantIds = List.of();
    }

    public MealSlotId mealSlotId() {
        return mealSlotId;
    }

    public LocalDate date() {
        return date;
    }

    public MealType mealType() {
        return mealType;
    }

    public MealSlotStatus status() {
        return status;
    }

    public UUID recipeId() {
        return recipeId;
    }

    public List<UUID> kitchenDutyParticipantIds() {
        return Collections.unmodifiableList(kitchenDutyParticipantIds);
    }
}
