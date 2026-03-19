package de.evia.travelmate.expense.domain.expense;

import java.math.BigDecimal;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

public record ScannedLineItem(String name, BigDecimal amount) {

    public ScannedLineItem {
        argumentIsNotBlank(name, "name");
        argumentIsNotNull(amount, "amount");
    }
}
