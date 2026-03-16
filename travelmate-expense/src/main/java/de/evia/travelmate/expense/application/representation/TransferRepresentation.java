package de.evia.travelmate.expense.application.representation;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRepresentation(UUID from, UUID to, BigDecimal amount) {
}
