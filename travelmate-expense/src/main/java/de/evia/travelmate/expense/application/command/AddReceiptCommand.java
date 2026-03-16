package de.evia.travelmate.expense.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.expense.domain.expense.ExpenseCategory;

public record AddReceiptCommand(
    UUID tripId,
    String description,
    BigDecimal amount,
    UUID paidBy,
    UUID submittedBy,
    LocalDate date,
    ExpenseCategory category
) {
}
