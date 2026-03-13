package de.evia.travelmate.expense.domain.expense;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record ReceiptId(UUID value) {

    public ReceiptId {
        argumentIsNotNull(value, "receiptId");
    }
}
