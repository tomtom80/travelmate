package de.evia.travelmate.expense.domain.expense;

import java.math.BigDecimal;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

public record CategoryShare(
    ExpenseCategory category,
    BigDecimal amount,
    BigDecimal percentage,
    int receiptCount
) {

    public CategoryShare {
        argumentIsNotNull(category, "category");
        argumentIsNotNull(amount, "amount");
        argumentIsNotNull(percentage, "percentage");
        argumentIsTrue(receiptCount >= 0, "receiptCount must not be negative.");
    }
}
