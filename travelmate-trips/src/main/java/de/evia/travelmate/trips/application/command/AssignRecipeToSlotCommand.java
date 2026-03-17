package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record AssignRecipeToSlotCommand(UUID tripId, UUID slotId, UUID recipeId) {
}
