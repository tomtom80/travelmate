package de.evia.travelmate.trips.application.command;

import java.util.UUID;

public record GenerateMealPlanCommand(UUID tenantId, UUID tripId) {
}
