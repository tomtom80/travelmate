package de.evia.travelmate.expense.domain.expense;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.time.LocalDate;
import java.util.UUID;

public class Receipt {

    private final ReceiptId receiptId;
    private final UUID paidBy;
    private final UUID submittedBy;
    private String description;
    private Amount amount;
    private LocalDate date;
    private ExpenseCategory category;
    private ReviewStatus reviewStatus;
    private UUID reviewerId;
    private String rejectionReason;

    public Receipt(final ReceiptId receiptId,
                   final String description,
                   final Amount amount,
                   final UUID paidBy,
                   final UUID submittedBy,
                   final LocalDate date,
                   final ExpenseCategory category,
                   final ReviewStatus reviewStatus,
                   final UUID reviewerId,
                   final String rejectionReason) {
        argumentIsNotNull(receiptId, "receiptId");
        argumentIsNotBlank(description, "description");
        argumentIsNotNull(amount, "amount");
        argumentIsNotNull(paidBy, "paidBy");
        argumentIsNotNull(submittedBy, "submittedBy");
        argumentIsNotNull(date, "date");
        argumentIsNotNull(reviewStatus, "reviewStatus");
        this.receiptId = receiptId;
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.submittedBy = submittedBy;
        this.date = date;
        this.category = category != null ? category : ExpenseCategory.OTHER;
        this.reviewStatus = reviewStatus;
        this.reviewerId = reviewerId;
        this.rejectionReason = rejectionReason;
    }

    public void approve(final UUID reviewerId) {
        argumentIsNotNull(reviewerId, "reviewerId");
        argumentIsTrue(!reviewerId.equals(submittedBy),
            "Reviewer must not be the submitter (four-eyes principle).");
        argumentIsTrue(reviewStatus == ReviewStatus.SUBMITTED,
            "Only SUBMITTED receipts can be approved.");
        this.reviewStatus = ReviewStatus.APPROVED;
        this.reviewerId = reviewerId;
        this.rejectionReason = null;
    }

    public void reject(final UUID reviewerId, final String reason) {
        argumentIsNotNull(reviewerId, "reviewerId");
        argumentIsNotBlank(reason, "reason");
        argumentIsTrue(!reviewerId.equals(submittedBy),
            "Reviewer must not be the submitter (four-eyes principle).");
        argumentIsTrue(reviewStatus == ReviewStatus.SUBMITTED,
            "Only SUBMITTED receipts can be rejected.");
        this.reviewStatus = ReviewStatus.REJECTED;
        this.reviewerId = reviewerId;
        this.rejectionReason = reason;
    }

    public void resubmit(final String description, final Amount amount,
                         final LocalDate date, final ExpenseCategory category) {
        argumentIsTrue(reviewStatus == ReviewStatus.REJECTED,
            "Only REJECTED receipts can be resubmitted.");
        argumentIsNotBlank(description, "description");
        argumentIsNotNull(amount, "amount");
        argumentIsNotNull(date, "date");
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.category = category != null ? category : ExpenseCategory.OTHER;
        this.reviewStatus = ReviewStatus.SUBMITTED;
        this.reviewerId = null;
        this.rejectionReason = null;
    }

    public ReceiptId receiptId() { return receiptId; }
    public String description() { return description; }
    public Amount amount() { return amount; }
    public UUID paidBy() { return paidBy; }
    public UUID submittedBy() { return submittedBy; }
    public LocalDate date() { return date; }
    public ExpenseCategory category() { return category; }
    public ReviewStatus reviewStatus() { return reviewStatus; }
    public UUID reviewerId() { return reviewerId; }
    public String rejectionReason() { return rejectionReason; }
}
