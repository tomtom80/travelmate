package de.evia.travelmate.expense.domain.expense;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.math.BigDecimal;

public record Amount(BigDecimal value) {

    public Amount {
        argumentIsNotNull(value, "amount");
        argumentIsTrue(value.compareTo(BigDecimal.ZERO) > 0,
            "Amount must be positive, but was: " + value);
    }
}
