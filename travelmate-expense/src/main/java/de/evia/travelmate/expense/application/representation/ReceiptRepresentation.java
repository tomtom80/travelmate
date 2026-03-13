package de.evia.travelmate.expense.application.representation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReceiptRepresentation(
    UUID receiptId,
    String description,
    BigDecimal amount,
    UUID paidBy,
    LocalDate date
) {
}
