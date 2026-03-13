package de.evia.travelmate.expense.domain.expense;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.time.LocalDate;
import java.util.UUID;

public class Receipt {

    private final ReceiptId receiptId;
    private final String description;
    private final Amount amount;
    private final UUID paidBy;
    private final LocalDate date;

    public Receipt(final ReceiptId receiptId,
                   final String description,
                   final Amount amount,
                   final UUID paidBy,
                   final LocalDate date) {
        argumentIsNotNull(receiptId, "receiptId");
        argumentIsNotBlank(description, "description");
        argumentIsNotNull(amount, "amount");
        argumentIsNotNull(paidBy, "paidBy");
        argumentIsNotNull(date, "date");
        this.receiptId = receiptId;
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.date = date;
    }

    public ReceiptId receiptId() {
        return receiptId;
    }

    public String description() {
        return description;
    }

    public Amount amount() {
        return amount;
    }

    public UUID paidBy() {
        return paidBy;
    }

    public LocalDate date() {
        return date;
    }
}
