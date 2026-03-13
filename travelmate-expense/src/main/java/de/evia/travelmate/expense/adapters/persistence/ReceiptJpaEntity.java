package de.evia.travelmate.expense.adapters.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

    @Column(name = "date", nullable = false)
    private LocalDate date;

    protected ReceiptJpaEntity() {
    }

    public ReceiptJpaEntity(final UUID receiptId, final ExpenseJpaEntity expense,
                            final String description, final BigDecimal amount,
                            final UUID paidBy, final LocalDate date) {
        this.receiptId = receiptId;
        this.expense = expense;
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.date = date;
    }

    public UUID getReceiptId() { return receiptId; }
    public ExpenseJpaEntity getExpense() { return expense; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public UUID getPaidBy() { return paidBy; }
    public LocalDate getDate() { return date; }
}
