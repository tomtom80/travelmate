package de.evia.travelmate.expense.application.representation;

import java.math.BigDecimal;

import de.evia.travelmate.expense.domain.expense.ExpenseCategory;

public record CategoryBreakdownRepresentation(
    ExpenseCategory category,
    BigDecimal total,
    int receiptCount
) {
}
