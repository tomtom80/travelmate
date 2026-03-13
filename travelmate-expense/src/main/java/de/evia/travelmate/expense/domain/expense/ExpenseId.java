package de.evia.travelmate.expense.domain.expense;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record ExpenseId(UUID value) {

    public ExpenseId {
        argumentIsNotNull(value, "expenseId");
    }
}
