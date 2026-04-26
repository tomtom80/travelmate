package de.evia.travelmate.trips.application.representation;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import de.evia.travelmate.trips.domain.mealplan.MealSlot;

public record MealSlotRepresentation(
    UUID slotId,
    LocalDate date,
    String mealType,
    String status,
    UUID recipeId,
    String recipeName,
    List<UUID> kitchenDutyParticipantIds
) {

    public MealSlotRepresentation(final MealSlot slot) {
        this(slot.mealSlotId().value(), slot.date(), slot.mealType().name(),
            slot.status().name(), slot.recipeId(), null,
            List.copyOf(slot.kitchenDutyParticipantIds()));
    }

    public MealSlotRepresentation withRecipeName(final String name) {
        return new MealSlotRepresentation(slotId, date, mealType, status, recipeId, name,
            kitchenDutyParticipantIds);
    }
}
