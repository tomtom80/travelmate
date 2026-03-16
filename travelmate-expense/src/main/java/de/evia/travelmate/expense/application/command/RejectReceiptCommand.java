package de.evia.travelmate.expense.application.command;

import java.util.UUID;

public record RejectReceiptCommand(
    UUID tripId,
    UUID receiptId,
    UUID reviewerId,
    String reason
) {
}
