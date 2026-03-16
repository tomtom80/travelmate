package de.evia.travelmate.expense.adapters.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import de.evia.travelmate.expense.domain.expense.ExpenseCategory;
import de.evia.travelmate.expense.domain.expense.ReviewStatus;

@Entity
@Table(name = "receipt")
public class ReceiptJpaEntity {

    @Id
    @Column(name = "receipt_id")
    private UUID receiptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private ExpenseJpaEntity expense;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "paid_by", nullable = false)
    private UUID paidBy;

    @Column(name = "submitted_by", nullable = false)
    private UUID submittedBy;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ExpenseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    private ReviewStatus reviewStatus;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    protected ReceiptJpaEntity() {
    }

    public ReceiptJpaEntity(final UUID receiptId, final ExpenseJpaEntity expense,
                            final String description, final BigDecimal amount,
                            final UUID paidBy, final UUID submittedBy,
                            final LocalDate date, final ExpenseCategory category,
                            final ReviewStatus reviewStatus, final UUID reviewerId,
                            final String rejectionReason) {
        this.receiptId = receiptId;
        this.expense = expense;
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.submittedBy = submittedBy;
        this.date = date;
        this.category = category;
        this.reviewStatus = reviewStatus;
        this.reviewerId = reviewerId;
        this.rejectionReason = rejectionReason;
    }

    public void setDescription(final String description) { this.description = description; }
    public void setAmount(final BigDecimal amount) { this.amount = amount; }
    public void setDate(final LocalDate date) { this.date = date; }
    public void setCategory(final ExpenseCategory category) { this.category = category; }
    public void setReviewStatus(final ReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }
    public void setReviewerId(final UUID reviewerId) { this.reviewerId = reviewerId; }
    public void setRejectionReason(final String rejectionReason) { this.rejectionReason = rejectionReason; }

    public UUID getReceiptId() { return receiptId; }
    public ExpenseJpaEntity getExpense() { return expense; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public UUID getPaidBy() { return paidBy; }
    public UUID getSubmittedBy() { return submittedBy; }
    public LocalDate getDate() { return date; }
    public ExpenseCategory getCategory() { return category; }
    public ReviewStatus getReviewStatus() { return reviewStatus; }
    public UUID getReviewerId() { return reviewerId; }
    public String getRejectionReason() { return rejectionReason; }
}
