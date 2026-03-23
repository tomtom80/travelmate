package de.evia.travelmate.expense.domain.expense;

import java.math.BigDecimal;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

public record PartyAccountEntry(
    PartyAccountEntryType type,
    String label,
    BigDecimal amount
) {
    public PartyAccountEntry {
        argumentIsNotNull(type, "type");
        argumentIsNotNull(label, "label");
        argumentIsNotNull(amount, "amount");
    }
}
