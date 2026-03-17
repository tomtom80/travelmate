package de.evia.travelmate.trips.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record AddManualShoppingItemCommand(UUID tenantId, UUID tripId, String name,
                                           BigDecimal quantity, String unit) {
}
