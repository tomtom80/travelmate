package de.evia.travelmate.expense.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AddReceiptCommand(
    UUID tripId,
    String description,
    BigDecimal amount,
    UUID paidBy,
    LocalDate date
) {
}
