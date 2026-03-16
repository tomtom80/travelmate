package de.evia.travelmate.expense.domain.expense;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.math.BigDecimal;
import java.util.UUID;

public record Transfer(UUID from, UUID to, BigDecimal amount) {

    public Transfer {
        argumentIsNotNull(from, "from");
        argumentIsNotNull(to, "to");
        argumentIsNotNull(amount, "amount");
        argumentIsTrue(amount.signum() > 0, "Transfer amount must be positive.");
    }
}
