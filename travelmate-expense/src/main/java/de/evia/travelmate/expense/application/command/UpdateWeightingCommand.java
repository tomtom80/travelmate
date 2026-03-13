package de.evia.travelmate.expense.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateWeightingCommand(
    UUID tripId,
    UUID participantId,
    BigDecimal weight
) {
}
