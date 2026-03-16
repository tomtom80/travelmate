package de.evia.travelmate.expense.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.expense.domain.expense.ExpenseCategory;

public record ResubmitReceiptCommand(
    UUID tripId,
    UUID receiptId,
    String description,
    BigDecimal amount,
    LocalDate date,
    ExpenseCategory category
) {
}
