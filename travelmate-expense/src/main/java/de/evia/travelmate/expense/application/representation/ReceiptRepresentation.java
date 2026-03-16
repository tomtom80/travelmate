package de.evia.travelmate.expense.application.representation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.expense.domain.expense.ExpenseCategory;
import de.evia.travelmate.expense.domain.expense.ReviewStatus;

public record ReceiptRepresentation(
    UUID receiptId,
    String description,
    BigDecimal amount,
    UUID paidBy,
    UUID submittedBy,
    LocalDate date,
    ExpenseCategory category,
    ReviewStatus reviewStatus,
    UUID reviewerId,
    String rejectionReason
) {
}
