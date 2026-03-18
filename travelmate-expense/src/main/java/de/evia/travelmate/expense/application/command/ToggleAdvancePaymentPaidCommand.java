package de.evia.travelmate.expense.application.command;

import java.util.UUID;

public record ToggleAdvancePaymentPaidCommand(UUID tripId, UUID advancePaymentId) {
}
