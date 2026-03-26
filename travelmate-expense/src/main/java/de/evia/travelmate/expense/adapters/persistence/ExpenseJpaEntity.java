package de.evia.travelmate.expense.adapters.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import de.evia.travelmate.expense.domain.expense.ExpenseStatus;

@Entity
@Table(name = "expense")
public class ExpenseJpaEntity {

    @Id
    @Column(name = "expense_id")
    private UUID expenseId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExpenseStatus status;

    @Column(name = "review_required", nullable = false)
    private boolean reviewRequired;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("date ASC, description ASC")
    private List<ReceiptJpaEntity> receipts = new ArrayList<>();

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("participantId ASC")
    private List<WeightingJpaEntity> weightings = new ArrayList<>();

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("partyName ASC")
    private List<AdvancePaymentJpaEntity> advancePayments = new ArrayList<>();

    protected ExpenseJpaEntity() {
    }

    public ExpenseJpaEntity(final UUID expenseId, final UUID tenantId,
                            final UUID tripId, final ExpenseStatus status,
                            final boolean reviewRequired) {
        this.expenseId = expenseId;
        this.tenantId = tenantId;
        this.tripId = tripId;
        this.status = status;
        this.reviewRequired = reviewRequired;
    }

    public UUID getExpenseId() { return expenseId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getTripId() { return tripId; }
    public ExpenseStatus getStatus() { return status; }
    public void setStatus(final ExpenseStatus status) { this.status = status; }
    public boolean isReviewRequired() { return reviewRequired; }
    public List<ReceiptJpaEntity> getReceipts() { return receipts; }
    public List<WeightingJpaEntity> getWeightings() { return weightings; }
    public List<AdvancePaymentJpaEntity> getAdvancePayments() { return advancePayments; }
}
