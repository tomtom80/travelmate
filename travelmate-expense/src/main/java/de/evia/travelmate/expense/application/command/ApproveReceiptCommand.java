package de.evia.travelmate.expense.application.command;

import java.util.UUID;

public record ApproveReceiptCommand(
    UUID tripId,
    UUID receiptId,
    UUID reviewerId
) {
}
